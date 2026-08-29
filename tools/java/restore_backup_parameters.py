#!/usr/bin/env python3
"""Restore author parameter names from verified .java~ source backups.

This is a review/refactoring helper, not a gate. It only applies a rename when
owner, original method name, arity, and parameter types select one author name
tuple. Conditional-preprocessor duplicates must agree or the method is skipped.
"""

from __future__ import annotations

import argparse
import re
import tomllib
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CANONICAL = ROOT / "java" / "reconstruction" / "mappings" / "canonical"
SOURCE_ROOT = ROOT / "java" / "src" / "main" / "java" / "defpackage"
BACKUP_ROOT = (
    ROOT
    / "_reference"
    / "source-backups"
    / "silent_hill_orphan_j2me_en_v010_114"
    / "daydream"
    / "ink"
)

DECLARATION = re.compile(
    r"(?m)^[ \t]*"
    r"(?P<mods>(?:(?:public|protected|private|static|final|abstract|synchronized)\s+)*)"
    r"(?:(?P<return>[A-Za-z_$][\w.$<>?]*(?:\[\])*)\s+)?"
    r"(?P<name>[A-Za-z_$][\w$]*)\s*"
    r"\((?P<parameters>[^()]*)\)\s*"
    r"(?:throws\s+[^{]+)?\{"
)
IDENTIFIER = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*")


@dataclass(frozen=True)
class Parameter:
    type_name: str
    name: str


@dataclass(frozen=True)
class Method:
    name: str
    parameters: tuple[Parameter, ...]
    start: int
    end: int


def split_parameters(text: str) -> tuple[Parameter, ...] | None:
    if not text.strip():
        return ()
    result = []
    for raw in text.split(","):
        cleaned = re.sub(r"\bfinal\s+", "", raw.strip())
        match = re.fullmatch(
            r"(?P<type>[A-Za-z_$][\w.$<>?]*(?:\[\])*)\s+"
            r"(?P<name>[A-Za-z_$][\w$]*)(?P<suffix>(?:\[\])*)",
            cleaned,
        )
        if match is None:
            return None
        result.append(
            Parameter(match.group("type") + match.group("suffix"), match.group("name"))
        )
    return tuple(result)


def closing_brace(text: str, opening: int) -> int | None:
    depth = 0
    state = "code"
    index = opening
    while index < len(text):
        char = text[index]
        following = text[index + 1] if index + 1 < len(text) else ""
        if state == "code":
            if char == "/" and following == "/":
                state = "line-comment"
                index += 2
                continue
            if char == "/" and following == "*":
                state = "block-comment"
                index += 2
                continue
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    return index + 1
        elif state == "line-comment":
            if char in "\r\n":
                state = "code"
        elif state == "block-comment":
            if char == "*" and following == "/":
                state = "code"
                index += 2
                continue
        elif state in {"string", "char"}:
            if char == "\\":
                index += 2
                continue
            if (state == "string" and char == '"') or (state == "char" and char == "'"):
                state = "code"
        index += 1
    return None


def methods(text: str, class_name: str, allowed: set[str] | None) -> list[Method]:
    result = []
    for match in DECLARATION.finditer(text):
        name = match.group("name")
        # A missing return type is only legal for a constructor. This also
        # prevents line-leading if/for/catch constructs from looking like methods.
        if match.group("return") is None and name != class_name:
            continue
        if allowed is not None and name not in allowed and name != class_name:
            continue
        parameters = split_parameters(match.group("parameters"))
        if parameters is None:
            continue
        end = closing_brace(text, match.end() - 1)
        if end is not None:
            result.append(Method(name, parameters, match.start(), end))
    return result


def normalize_type(type_name: str, reverse_classes: dict[str, str]) -> str:
    arrays = ""
    while type_name.endswith("[]"):
        arrays += "[]"
        type_name = type_name[:-2]
    simple = type_name.rsplit(".", 1)[-1]
    return reverse_classes.get(simple, simple) + arrays


def rewrite_identifiers(segment: str, renames: dict[str, str]) -> str:
    output = []
    state = "code"
    index = 0
    while index < len(segment):
        char = segment[index]
        following = segment[index + 1] if index + 1 < len(segment) else ""
        if state == "code" and char == "/" and following == "/":
            output.append("//")
            index += 2
            state = "line-comment"
            continue
        if state == "code" and char == "/" and following == "*":
            output.append("/*")
            index += 2
            state = "block-comment"
            continue
        if state == "code" and char in {'"', "'"}:
            output.append(char)
            state = "string" if char == '"' else "char"
            index += 1
            continue
        if state == "code":
            match = IDENTIFIER.match(segment, index)
            if match:
                token = match.group(0)
                output.append(renames.get(token, token))
                index = match.end()
                continue
        elif state == "line-comment" and char in "\r\n":
            state = "code"
        elif state == "block-comment" and char == "*" and following == "/":
            output.append("*/")
            index += 2
            state = "code"
            continue
        elif state in {"string", "char"}:
            if char == "\\" and following:
                output.append(char + following)
                index += 2
                continue
            if (state == "string" and char == '"') or (state == "char" and char == "'"):
                state = "code"
        output.append(char)
        index += 1
    return "".join(output)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    arguments = parser.parse_args()
    class_doc = tomllib.loads((CANONICAL / "classes.toml").read_text())
    class_map = {
        entry["original_name"]: entry["semantic_name"] for entry in class_doc["classes"]
    }
    reverse_classes = {semantic: original for original, semantic in class_map.items()}
    rename_doc = tomllib.loads((CANONICAL / "member-renames.toml").read_text())
    reverse_methods = {
        (class_map[entry["owner"]], entry["semantic_name"]): entry["original_name"]
        for entry in rename_doc.get("renames", [])
        if entry["kind"] == "method"
    }
    restored = 0
    skipped = []
    for original_owner in ("M", "MyCanvas", "Ext", "ExtBase"):
        semantic_owner = class_map[original_owner]
        source_path = SOURCE_ROOT / f"{semantic_owner}.java"
        backup_path = BACKUP_ROOT / f"{original_owner}.java~"
        source = source_path.read_text(encoding="utf-8")
        backup = backup_path.read_text(encoding="latin-1")
        canonical_methods = methods(source, semantic_owner, None)
        original_names = {
            reverse_methods.get((semantic_owner, method.name), method.name)
            if method.name != semantic_owner
            else original_owner
            for method in canonical_methods
        }
        backup_methods = methods(backup, original_owner, original_names)
        by_key: dict[tuple[str, tuple[str, ...]], set[tuple[str, ...]]] = {}
        for method in backup_methods:
            key = (
                method.name,
                tuple(normalize_type(item.type_name, {}) for item in method.parameters),
            )
            by_key.setdefault(key, set()).add(tuple(item.name for item in method.parameters))

        replacements = []
        for method in canonical_methods:
            original_name = (
                original_owner
                if method.name == semantic_owner
                else reverse_methods.get((semantic_owner, method.name), method.name)
            )
            key = (
                original_name,
                tuple(
                    normalize_type(item.type_name, reverse_classes)
                    for item in method.parameters
                ),
            )
            candidates = by_key.get(key, set())
            if len(candidates) != 1:
                if method.parameters:
                    skipped.append(f"{semantic_owner}.{method.name}{key[1]}: {len(candidates)} candidates")
                continue
            author_names = next(iter(candidates))
            renames = {
                parameter.name: author
                for parameter, author in zip(method.parameters, author_names, strict=True)
                if parameter.name != author
            }
            if renames:
                replacements.append((method.start, method.end, renames, method.name))
                restored += len(renames)
        for start, end, renames, _name in sorted(replacements, reverse=True):
            source = source[:start] + rewrite_identifiers(source[start:end], renames) + source[end:]
        if arguments.apply:
            source_path.write_text(source, encoding="utf-8", newline="")
        print(f"{semantic_owner}: {sum(len(item[2]) for item in replacements)} parameter renames")
    for item in skipped:
        print(f"skipped: {item}")
    print(f"{'applied' if arguments.apply else 'would apply'} {restored} author parameter names")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
