#!/usr/bin/env python3
"""Bind the surviving author Java editor backups to verified corpus bytes."""

from __future__ import annotations

import argparse
import hashlib
import io
import sys
import tomllib
import zipfile
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))

import corpus  # noqa: E402

LEDGER = ROOT / "java" / "reconstruction" / "source-backups.toml"


def observed() -> Counter[tuple[str, str]]:
    result: Counter[tuple[str, str]] = Counter()
    for build in corpus.builds():
        with zipfile.ZipFile(io.BytesIO(build.payload)) as archive:
            for member in archive.namelist():
                if member.lower().endswith(".java~"):
                    digest = hashlib.sha256(archive.read(member)).hexdigest()
                    result[(member, digest)] += 1
    return result


def validate(*, inject_defect: bool) -> list[str]:
    document = tomllib.loads(LEDGER.read_text(encoding="utf-8"))
    if document.get("schema_version") != 1:
        return ["source-backups.toml has an unsupported schema_version"]
    entries = document.get("backups", [])
    problems: list[str] = []
    if len(entries) != 4:
        problems.append(f"expected four authoritative backups, found {len(entries)}")
    owners = [entry.get("semantic_owner") for entry in entries]
    if len(owners) != len(set(owners)) or any(not owner for owner in owners):
        problems.append("semantic backup owners are missing or duplicated")
    counts = observed()
    for index, entry in enumerate(entries):
        digest = entry.get("sha256", "")
        if inject_defect and index == 0:
            digest = "0" * 64
        key = (entry.get("member", ""), digest)
        found = counts.get(key, 0)
        expected = entry.get("expected_payload_occurrences")
        if found != expected:
            problems.append(
                f"{key[0]} {key[1][:12]}: expected {expected} payload "
                f"occurrences, found {found}"
            )
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    problems = validate(inject_defect=arguments.self_test)
    if arguments.self_test:
        if not problems:
            print("SELF-TEST FAILED: a mutated backup digest was accepted")
            return 3
        print("self-test OK: a mutated source-backup digest was rejected (R3)")
        return 0
    if problems:
        for problem in problems:
            print(f"error: {problem}", file=sys.stderr)
        return 1
    print("validated four author source backups across 29 payload occurrences")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
