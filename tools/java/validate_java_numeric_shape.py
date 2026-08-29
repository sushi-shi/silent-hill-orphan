#!/usr/bin/env python3
"""Compare arithmetic/conversion opcode order with every original Java body."""

from __future__ import annotations

import argparse
import collections
import hashlib
import io
import sys
import tempfile
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))
sys.path.insert(0, str(ROOT / "tools" / "java"))

import classfile  # noqa: E402
import compile_named_java  # noqa: E402
import corpus  # noqa: E402
from validate_named_java import (  # noqa: E402
    MAPPINGS,
    Mapping,
    canonical_descriptor,
    compiled_classes,
    original_classes,
    read_mappings,
    selected_payload,
)

RATCHET = (
    ROOT
    / "java"
    / "reconstruction"
    / "mappings"
    / "canonical"
    / "numeric-shape.toml"
)
RESOLUTIONS = RATCHET.with_name("numeric-shape-resolutions.toml")


def method_key(
    owner: str,
    method: classfile.MethodSymbol,
    mapping: Mapping,
    original_owner: str,
) -> tuple[str, str, str]:
    name = mapping.members.get(
        (original_owner, "method", method.descriptor, method.name), method.name
    )
    return owner, name, canonical_descriptor(method.descriptor, mapping.classes)


def inventory(
    classes: list[classfile.ClassInfo], mapping: Mapping, *, compiled: bool
) -> dict[tuple[str, str, str], list[str]]:
    result = {}
    for info in classes:
        original_owner = (
            info.internal_name.removeprefix("defpackage/") if compiled else info.internal_name
        )
        owner = original_owner if compiled else mapping.classes[original_owner]
        active_mapping = Mapping({}, {}) if compiled else mapping
        for method in info.methods:
            key = method_key(owner, method, active_mapping, original_owner)
            if key in result:
                raise ValueError(f"duplicate method key {key}")
            result[key] = method.numeric_opcodes
    return result


def label(key: tuple[str, str, str]) -> str:
    return f"{key[0]}.{key[1]}{key[2]}"


def sequence_hash(opcodes: list[str]) -> str:
    return hashlib.sha256("\n".join(opcodes).encode()).hexdigest()


def load_resolutions() -> dict[str, dict]:
    document = tomllib.loads(RESOLUTIONS.read_text())
    result = {}
    for entry in document.get("resolved", []):
        name = entry.get("method")
        if not name or name in result or not entry.get("evidence"):
            raise ValueError(f"invalid or duplicate numeric resolution {name!r}")
        result[name] = entry
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    mapping = read_mappings(MAPPINGS)
    _baseline_id, _baseline_sha, payload = selected_payload("baseline")
    originals = original_classes(payload)
    with tempfile.TemporaryDirectory(prefix="orphan-java-numeric-") as temporary:
        output = Path(temporary) / "classes"
        compile_named_java.compile_source(output)
        canonical = compiled_classes(output)
    expected = inventory(originals, mapping, compiled=False)
    actual = inventory(canonical, mapping, compiled=True)
    if set(expected) != set(actual):
        print("error: numeric method inventory differs from mapped surface", file=sys.stderr)
        return 1
    if arguments.self_test:
        victim = next(key for key in sorted(actual) if actual[key])
        actual[victim] = [*actual[victim], "iadd"]

    reordered = []
    different = []
    for key in sorted(expected):
        left = expected[key]
        right = actual[key]
        if left == right:
            continue
        if collections.Counter(left) == collections.Counter(right):
            reordered.append(label(key))
        else:
            different.append((label(key), left, right))

    ratchet = tomllib.loads(RATCHET.read_text()) if RATCHET.exists() else {}
    accepted_reordered = set(ratchet.get("reordered", []))
    found_reordered = set(reordered)
    failures = []
    if found_reordered != accepted_reordered:
        failures.append(
            "reordered ratchet differs: "
            f"new={sorted(found_reordered - accepted_reordered)}, "
            f"gone={sorted(accepted_reordered - found_reordered)}"
        )
    resolutions = load_resolutions()
    resolved = []
    unresolved = []
    for name, left, right in different:
        entry = resolutions.get(name)
        missing = list((collections.Counter(left) - collections.Counter(right)).elements())
        extra = list((collections.Counter(right) - collections.Counter(left)).elements())
        if entry is not None and all(
            (
                entry.get("original_sha256") == sequence_hash(left),
                entry.get("canonical_sha256") == sequence_hash(right),
                collections.Counter(entry.get("missing", [])) == collections.Counter(missing),
                collections.Counter(entry.get("extra", [])) == collections.Counter(extra),
            )
        ):
            resolved.append(name)
        else:
            unresolved.append((name, left, right))
    stale_resolutions = set(resolutions) - set(resolved)
    if unresolved:
        failures.append(f"{len(unresolved)} methods have unresolved numeric opcode multisets")
    if stale_resolutions:
        failures.append(f"stale numeric resolutions: {sorted(stale_resolutions)}")
    if arguments.verbose or failures:
        for name, left, right in unresolved:
            print(f"different {name}\n  original: {left}\n  canonical: {right}")
        for name in reordered:
            print(f"reordered {name}")
    if arguments.self_test:
        if not failures:
            print("SELF-TEST FAILED: an injected numeric opcode was accepted")
            return 3
        print("self-test OK: an injected numeric opcode was rejected (R3)")
        return 0
    if failures:
        for failure in failures:
            print(f"error: {failure}", file=sys.stderr)
        return 1
    print(
        f"validated numeric shape for {len(expected)} methods; "
        f"{len(reordered)} reviewed reorderings, {len(resolved)} hash-locked "
        "compiler-equivalent resolutions"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
