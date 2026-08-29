#!/usr/bin/env python3
"""Require every non-structural canonical Java literal to have evidence."""

from __future__ import annotations

import argparse
import re
import shutil
import sys
import tempfile
import tomllib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from java_literal_scan import STRUCTURAL_VALUES, Literal, scan_tree  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "java" / "src" / "main" / "java" / "defpackage"
LEDGER = ROOT / "java" / "reconstruction" / "mappings" / "canonical" / "literals.toml"
CONSTANT_HOLDERS = {"InkCodes.java", "TextId.java"}
NAMED_CONSTANT_DEFINITION = re.compile(
    r"^(?:public\s+|protected\s+|private\s+)?static\s+final\s+[^=]+\s+"
    r"[A-Z][A-Z0-9_]*\s*="
)


class Rule:
    def __init__(self, raw: dict, index: int):
        self.id = raw.get("id")
        if not self.id:
            raise ValueError(f"rule {index}: missing id")
        for field in ("domain", "meaning"):
            if not raw.get(field):
                raise ValueError(f"{self.id}: missing {field}")
        if not raw.get("evidence"):
            raise ValueError(f"{self.id}: evidence is required")
        self.sources = set(raw.get("sources", []))
        self.members = set(raw.get("members", []))
        self.member_contains = list(raw.get("member_contains", []))
        self.roles = set(raw.get("roles", []))
        self.values = {str(value) for value in raw.get("values", [])}
        value_range = raw.get("value_range")
        if value_range is not None and len(value_range) != 2:
            raise ValueError(f"{self.id}: value_range needs two bounds")
        self.value_range = tuple(value_range) if value_range else None
        self.matched = 0

    def matches(self, literal: Literal) -> bool:
        if self.sources and literal.source not in self.sources:
            return False
        if self.members and literal.member not in self.members:
            return False
        if self.member_contains and not any(part in literal.member for part in self.member_contains):
            return False
        if self.roles and literal.role not in self.roles:
            return False
        if self.values and literal.value not in self.values:
            return False
        if self.value_range is not None:
            try:
                value = int(literal.value, 0)
            except ValueError:
                return False
            if not self.value_range[0] <= value <= self.value_range[1]:
                return False
        return True


def load_rules() -> list[Rule]:
    document = tomllib.loads(LEDGER.read_text(encoding="utf-8"))
    if document.get("schema_version") != 1:
        raise ValueError("literal ledger schema_version must be 1")
    rules = [Rule(raw, index) for index, raw in enumerate(document.get("rules", []))]
    ids = [rule.id for rule in rules]
    if len(ids) != len(set(ids)):
        raise ValueError("literal ledger contains duplicate rule ids")
    return rules


def classify(literals: list[Literal], rules: list[Rule]) -> tuple[list[Literal], int]:
    unexplained = []
    explained = 0
    for literal in literals:
        if literal.source in CONSTANT_HOLDERS or literal.value in STRUCTURAL_VALUES:
            continue
        # A descriptive constant declaration is itself the explanation for its
        # defining value. Uses of that constant remain named in the Java AST;
        # only raw literals in executable code need a ledger rule.
        if NAMED_CONSTANT_DEFINITION.match(literal.context):
            continue
        for rule in rules:
            if rule.matches(literal):
                rule.matched += 1
                explained += 1
                break
        else:
            unexplained.append(literal)
    return unexplained, explained


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    source_root = SOURCE_ROOT
    if arguments.self_test:
        temporary = tempfile.TemporaryDirectory(prefix="orphan-literal-self-test-")
        source_root = Path(temporary.name) / "defpackage"
        shutil.copytree(SOURCE_ROOT, source_root)
        application = source_root / "Application.java"
        text = application.read_text(encoding="utf-8")
        marker = "    static boolean loading() {\n"
        application.write_text(
            text.replace(marker, marker + "        int injected = 2147483646;\n", 1),
            encoding="utf-8",
            newline="",
        )
    rules = load_rules()
    unexplained, explained = classify(scan_tree(source_root), rules)
    unused = [rule.id for rule in rules if rule.matched == 0]
    if arguments.list:
        for literal in unexplained:
            print(
                f"{literal.source}:{literal.line} {literal.member} "
                f"[{literal.role}] {literal.value}  {literal.context}"
            )
        return 0
    if arguments.self_test:
        temporary.cleanup()
        if not any(literal.value == "2147483646" for literal in unexplained):
            raise SystemExit("self-test failed: injected magic literal was accepted")
        print("self-test OK: injected magic literal was rejected")
        return 0
    failures = []
    if unexplained:
        failures.append(f"{len(unexplained)} unexplained non-structural literals")
    if unused:
        failures.append(f"unused literal rules: {', '.join(unused)}")
    if failures:
        for failure in failures:
            print(f"error: {failure}", file=sys.stderr)
        print("run with --list for exact sites", file=sys.stderr)
        return 1
    print(f"canonical literal ledger OK: {explained} explained uses across {len(rules)} rules")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
