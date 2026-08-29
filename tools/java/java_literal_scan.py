#!/usr/bin/env python3
"""Extract numeric literals with syntactic context from canonical Java."""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

NUMERIC = re.compile(r"(?<![\w.])(?:0[xX][0-9a-fA-F]+|\d+)(?![\w.])")
METHOD_DECLARATION = re.compile(
    r"^\s{4}(?:@\w+\s+)*(?:public|private|protected|static|final|abstract|synchronized|native|\s)*"
    r"[\w\[\]<>., ]+?\s+(\w+)\s*\([^;]*\)\s*(?:throws [\w., ]+)?\{"
)
FIELD_DECLARATION = re.compile(
    r"^\s{4}(?:public|private|protected|static|final|volatile|transient|\s)+"
    r"[\w\[\]<>., ]+\s+(\w+)\s*(?:=|;)"
)
STRUCTURAL_VALUES = {"0", "-0", "1", "2", "-1"}


@dataclass(frozen=True)
class Literal:
    source: str
    line: int
    column: int
    value: str
    role: str
    context: str
    member: str


def strip_noise(text: str) -> str:
    output = list(text)
    index = 0
    while index < len(text):
        character = text[index]
        following = text[index + 1] if index + 1 < len(text) else ""
        if character == "/" and following == "/":
            while index < len(text) and text[index] != "\n":
                output[index] = " "
                index += 1
        elif character == "/" and following == "*":
            while index < len(text) and not (
                text[index] == "*" and index + 1 < len(text) and text[index + 1] == "/"
            ):
                if text[index] != "\n":
                    output[index] = " "
                index += 1
            for offset in range(index, min(index + 2, len(text))):
                output[offset] = " "
            index += 2
        elif character in "\"'":
            quote = character
            output[index] = " "
            index += 1
            while index < len(text) and text[index] != quote:
                if text[index] == "\\":
                    output[index] = " "
                    index += 1
                    if index < len(text):
                        output[index] = " "
                        index += 1
                    continue
                if text[index] != "\n":
                    output[index] = " "
                index += 1
            if index < len(text):
                output[index] = " "
                index += 1
        else:
            index += 1
    return "".join(output)


def classify(line: str, start: int, end: int) -> tuple[str, str]:
    before = line[:start]
    after = line[end:]
    stripped = line.strip()
    if re.search(r"\bcase\s+$", before) or re.search(r"\bcase\s+-$", before):
        return "case-label", stripped
    if re.search(r"=\s*$", before) and re.search(r"^\s*;", after):
        return "field-initializer", stripped
    if re.search(r"[=!<>]=\s*\(?\s*(?:byte|short|int|long|char)?\s*\)?\s*-?$", before):
        return "comparison", stripped
    if re.search(r"[<>]\s*\(?\s*(?:byte|short|int|long|char)?\s*\)?\s*-?$", before):
        return "comparison", stripped
    if re.search(r"\(\s*(?:byte|short|long|float|double)\s*\)\s*-?$", before):
        return "narrowing-cast", stripped
    if re.search(r"\[\s*-?$", before) and re.search(r"^\s*\]", after):
        return "array-index", stripped
    if re.search(r"[,(]\s*-?$", before):
        return "call-argument", stripped
    if re.search(r"[-+*/%]\s*-?$", before):
        return "arithmetic", stripped
    if re.search(r"\{\s*-?$", before) or re.search(r",\s*-?$", before):
        return "array-element", stripped
    return "other", stripped


def scan_source(path: Path) -> list[Literal]:
    text = strip_noise(path.read_text(encoding="utf-8"))
    literals = []
    member = "<class>"
    brace_depth = 0
    for line_number, line in enumerate(text.splitlines(), 1):
        declaration = METHOD_DECLARATION.match(line)
        if declaration and brace_depth <= 1:
            member = declaration.group(1)
        elif brace_depth <= 1:
            field = FIELD_DECLARATION.match(line)
            if field:
                member = field.group(1)
        for match in NUMERIC.finditer(line):
            role, context = classify(line, match.start(), match.end())
            value = match.group(0)
            if role in {"case-label", "comparison", "narrowing-cast", "arithmetic"}:
                if line[: match.start()].rstrip().endswith("-"):
                    value = f"-{value}"
            literals.append(
                Literal(path.name, line_number, match.start() + 1, value, role, context[:160], member)
            )
        brace_depth += line.count("{") - line.count("}")
    return literals


def scan_tree(source_root: Path) -> list[Literal]:
    return [literal for path in sorted(source_root.rglob("*.java")) for literal in scan_source(path)]
