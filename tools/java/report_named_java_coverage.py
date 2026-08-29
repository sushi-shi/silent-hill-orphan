#!/usr/bin/env python3
"""Report and gate complete semantic-member coverage of the Java baseline."""

from __future__ import annotations

import argparse
import sys
import tomllib
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "java"))

import generate_canonical_mappings  # noqa: E402
import validate_named_java  # noqa: E402


CANONICAL = ROOT / "java" / "reconstruction" / "mappings" / "canonical"
EXPECTED_CLASSES = 13
EXPECTED_FIELDS = 1075
EXPECTED_METHODS = 326  # Constructors and <clinit> are binary-surface-only.


@dataclass(frozen=True)
class ClassCoverage:
    original_name: str
    semantic_name: str
    fields: int
    reviewed_fields: int
    methods: int
    reviewed_methods: int


def coverage(*, mutate: bool = False) -> tuple[list[ClassCoverage], list[str]]:
    mapping = validate_named_java.read_mappings(CANONICAL / "mappings.tiny")
    if mutate:
        victim = min(mapping.members)
        del mapping.members[victim]
    originals = generate_canonical_mappings.load_original_classes()
    class_document = tomllib.loads((CANONICAL / "classes.toml").read_text())
    class_rows = {entry["original_name"]: entry for entry in class_document["classes"]}
    failures: list[str] = []
    if set(class_rows) != set(originals) or set(mapping.classes) != set(originals):
        failures.append("class coverage differs from the verified baseline")

    result: list[ClassCoverage] = []
    for owner, info in sorted(originals.items()):
        row = class_rows.get(owner, {})
        if not row.get("role") or not row.get("evidence"):
            failures.append(f"{owner}: missing reviewed semantic role/evidence")
        fields = [
            (owner, "field", field.descriptor, field.name) for field in info.fields
        ]
        methods = [
            (owner, "method", method.descriptor, method.name)
            for method in info.methods
            if method.name not in {"<init>", "<clinit>"}
        ]
        reviewed_fields = sum(key in mapping.members for key in fields)
        reviewed_methods = sum(key in mapping.members for key in methods)
        result.append(
            ClassCoverage(
                owner,
                mapping.classes.get(owner, "<missing>"),
                len(fields),
                reviewed_fields,
                len(methods),
                reviewed_methods,
            )
        )
        if reviewed_fields != len(fields) or reviewed_methods != len(methods):
            failures.append(
                f"{owner}: reviewed {reviewed_fields}/{len(fields)} fields and "
                f"{reviewed_methods}/{len(methods)} methods"
            )

    totals = (
        len(result),
        sum(item.fields for item in result),
        sum(item.methods for item in result),
        sum(item.reviewed_fields for item in result),
        sum(item.reviewed_methods for item in result),
    )
    expected = (
        EXPECTED_CLASSES,
        EXPECTED_FIELDS,
        EXPECTED_METHODS,
        EXPECTED_FIELDS,
        EXPECTED_METHODS,
    )
    if totals != expected:
        failures.append(f"coverage denominator {totals} != reviewed {expected}")
    return result, failures


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--summary-only", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    rows, failures = coverage(mutate=arguments.self_test)

    if arguments.self_test:
        if len(failures) != 2 or not any("reviewed" in item for item in failures):
            print(f"SELF-TEST FAILED: deleted mapping row produced {failures}")
            return 3
        print("self-test OK: a missing semantic member mapping was rejected (R3)")
        return 0

    if not arguments.summary_only:
        print("| Class | Fields | Methods |")
        print("|---|---:|---:|")
        for row in sorted(rows, key=lambda item: item.semantic_name.casefold()):
            print(
                f"| {row.semantic_name} (`{row.original_name}`) "
                f"| {row.reviewed_fields}/{row.fields} "
                f"| {row.reviewed_methods}/{row.methods} |"
            )
        print()
    print(
        f"Reviewed semantic coverage: {len(rows)} classes, "
        f"{sum(item.reviewed_fields for item in rows)}/{sum(item.fields for item in rows)} fields, "
        f"{sum(item.reviewed_methods for item in rows)}/{sum(item.methods for item in rows)} methods "
        "(constructors and class initializers excluded)."
    )
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
