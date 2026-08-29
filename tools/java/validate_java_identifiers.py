#!/usr/bin/env python3
"""Reject decompiler-generated identifiers in the canonical Java AST."""

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import tempfile
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "java" / "src" / "main" / "java"
INVENTORY_SOURCE = ROOT / "tools" / "java" / "JavaIdentifierInventory.java"

# These are JADX/CFR synthetic-name families, not short-name style opinions.
# Structural for-loop counters and catch variables are classified separately by
# the javac tree walker and remain legitimate where the source really uses them.
DECOMPILER_NAME = re.compile(
    r"^(?:"
    r"(?:i|z|str|obj|bArr|iArr|objArr|strArr)[0-9]*"
    r"|(?:bArr|iArr|objArr|strArr|i|j|z|str|obj|c)[A-Z_].*"
    r"|menuMenuCreate[0-9]*"
    r"|inputStream(?:ResourceGet|Open).*"
    r"|outputStreamOpen.*|recordStoreOpen.*|threadCurrent.*"
    r"|imageCreate.*|httpConnectionOpen.*"
    r"|(?:stringBuffer|menu|current|length|size|script|itemName|lowerCase|utf|g)[0-9]+"
    r")$"
)
EXPECTED_TOTAL = 2492
EXPECTED_ROLES = {
    "field": 1075,
    "parameter": 349,
    "local": 815,
    "counter": 142,
    "iteration": 2,
    "catch": 109,
}


def inventory(sources: list[Path]) -> list[tuple[str, ...]]:
    with tempfile.TemporaryDirectory(prefix="orphan-java-identifiers-") as raw:
        classes = Path(raw) / "classes"
        classes.mkdir()
        subprocess.run(
            ["javac", "-proc:none", "-d", str(classes), str(INVENTORY_SOURCE)],
            check=True,
        )
        completed = subprocess.run(
            [
                "java",
                "-cp",
                str(classes),
                "JavaIdentifierInventory",
                *map(str, sources),
            ],
            check=True,
            text=True,
            stdout=subprocess.PIPE,
        )
    rows = []
    for line in completed.stdout.splitlines():
        columns = tuple(line.split("\t"))
        if len(columns) != 7:
            raise ValueError(f"malformed identifier inventory row: {line!r}")
        rows.append(columns)
    return rows


def problems(rows: list[tuple[str, ...]]) -> list[str]:
    result = []
    roles = Counter(row[4] for row in rows)
    if len(rows) != EXPECTED_TOTAL:
        result.append(f"declaration denominator {len(rows)} != {EXPECTED_TOTAL}")
    if dict(roles) != EXPECTED_ROLES:
        result.append(f"declaration roles {dict(roles)} != {EXPECTED_ROLES}")
    for file_name, line, owner, method, role, name, _type_name in rows:
        if role not in {"counter", "catch"} and DECOMPILER_NAME.fullmatch(name):
            result.append(
                f"{file_name}:{line}: {owner}.{method} retains synthetic {role} {name}"
            )
    return result


def canonical_sources(root: Path = SOURCE_ROOT) -> list[Path]:
    return sorted(root.rglob("*.java"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()

    if arguments.self_test:
        with tempfile.TemporaryDirectory(prefix="orphan-identifier-self-test-") as raw:
            copied_root = Path(raw) / "java"
            shutil.copytree(SOURCE_ROOT, copied_root)
            application = copied_root / "defpackage" / "Application.java"
            text = application.read_text(encoding="utf-8")
            marker = "    static boolean loading() {\n"
            if marker not in text:
                raise SystemExit("self-test injection point disappeared")
            application.write_text(
                text.replace(marker, marker + "        int i2 = 0;\n", 1),
                encoding="utf-8",
                newline="",
            )
            failures = problems(inventory(canonical_sources(copied_root)))
        if not any("retains synthetic local i2" in item for item in failures):
            raise SystemExit("self-test failed: injected synthetic local was accepted")
        print("self-test OK: injected synthetic local i2 was rejected")
        return 0

    failures = problems(inventory(canonical_sources()))
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        return 1
    print(
        f"canonical identifier AST OK: {EXPECTED_TOTAL} declarations; "
        "zero decompiler-generated names"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
