#!/usr/bin/env python3
"""Validate the complete reviewed mapping and canonical Java binary surface.

The selected original JAR remains the authority. This gate applies the checked-
in Tiny class/member mapping to its declarations, compiles the semantic source,
and compares every hierarchy edge, field, constructor, initializer, and method
signature. An independent original build must also contain every baseline
author symbol with the same descriptor; this makes retained names cross-build
evidence instead of a decompiler convention.
"""

from __future__ import annotations

import argparse
import io
import re
import sys
import tempfile
import tomllib
import zipfile
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))
sys.path.insert(0, str(ROOT / "tools" / "java"))

import classfile  # noqa: E402
import compile_named_java  # noqa: E402
import corpus  # noqa: E402

SOURCE_ROOT = ROOT / "java" / "src" / "main" / "java"
SOURCE_PACKAGE = "defpackage/"
EXPECTED_CLASSES = 13
EXPECTED_METHODS = 350
SYMBOLS = ROOT / "java" / "reconstruction" / "symbols.toml"
CANONICAL = ROOT / "java" / "reconstruction" / "mappings" / "canonical"
CLASS_MAP = CANONICAL / "classes.toml"
MAPPINGS = CANONICAL / "mappings.tiny"

# These all signal that a decompiler failure was checked in as if it were a
# reviewed body.  Ordinary explanatory comments remain allowed.
FORBIDDEN_SOURCE = (
    "JADX WARN",
    "JADX ERROR",
    "Method not decompiled",
    "NOT YET RECOVERED",
    "UnsupportedOperationException(\"Method not decompiled",
)


@dataclass(frozen=True, order=True)
class Member:
    owner: str
    kind: str
    name: str
    descriptor: str
    access: int


@dataclass(frozen=True, order=True)
class TypeSurface:
    name: str
    access: int
    super_name: str | None
    interfaces: tuple[str, ...]


@dataclass(frozen=True)
class Mapping:
    classes: dict[str, str]
    members: dict[tuple[str, str, str, str], str]


def read_mappings(path: Path) -> Mapping:
    classes: dict[str, str] = {}
    members: dict[tuple[str, str, str, str], str] = {}
    owner: str | None = None
    for line_number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if line_number == 1:
            if raw != "tiny\t2\t0\toriginal\tsemantic":
                raise ValueError(f"{path}: unsupported Tiny header")
            continue
        columns = raw.split("\t")
        if len(columns) == 3 and columns[0] == "c":
            owner = columns[1]
            if owner in classes:
                raise ValueError(f"{path}:{line_number}: duplicate class")
            classes[owner] = columns[2]
        elif len(columns) == 5 and columns[0] == "" and columns[1] in {"f", "m"}:
            if owner is None:
                raise ValueError(f"{path}:{line_number}: member without owner")
            kind = "field" if columns[1] == "f" else "method"
            key = (owner, kind, columns[2], columns[3])
            if key in members:
                raise ValueError(f"{path}:{line_number}: duplicate member")
            members[key] = columns[4]
        else:
            raise ValueError(f"{path}:{line_number}: invalid Tiny row")
    return Mapping(classes, members)


def selected_payload(key: str) -> tuple[str, str, bytes]:
    selected = corpus.load_manifest().get(key, "")
    matches = [build for build in corpus.builds() if build.build_id == selected]
    if len(matches) != 1:
        raise RuntimeError(f"{key} {selected!r} does not resolve to one payload")
    return matches[0].build_id, matches[0].sha256, matches[0].payload


def original_classes(payload: bytes) -> list[classfile.ClassInfo]:
    result = []
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        for name in sorted(archive.namelist()):
            if not name.endswith(".class") or "/" in name:
                continue
            result.append(classfile.parse_class(name, archive.read(name)))
    return result


def compiled_classes(root: Path) -> list[classfile.ClassInfo]:
    result = []
    for path in sorted(root.rglob("*.class")):
        relative = path.relative_to(root).as_posix()
        if not relative.startswith(SOURCE_PACKAGE):
            raise RuntimeError(f"compiled application escaped {SOURCE_PACKAGE}: {relative}")
        result.append(classfile.parse_class(relative, path.read_bytes()))
    return result


def canonical_name(name: str | None, class_names: dict[str, str]) -> str | None:
    if name is None:
        return None
    if name.startswith(SOURCE_PACKAGE):
        return name[len(SOURCE_PACKAGE) :]
    return class_names.get(name, name)


def canonical_descriptor(descriptor: str, class_names: dict[str, str]) -> str:
    return re.sub(
        r"L(?:defpackage/)?([^;]+);",
        lambda match: f"L{class_names.get(match.group(1), match.group(1))};",
        descriptor,
    )


def surfaces(
    classes: list[classfile.ClassInfo],
    mapping: Mapping,
) -> tuple[set[TypeSurface], set[Member]]:
    types: set[TypeSurface] = set()
    members: set[Member] = set()
    for info in classes:
        original_owner = (
            info.internal_name[len(SOURCE_PACKAGE) :]
            if info.internal_name.startswith(SOURCE_PACKAGE)
            else info.internal_name
        )
        owner = canonical_name(info.internal_name, mapping.classes)
        assert owner is not None
        # ACC_SUPER is a class-file invocation bit, not a Java declaration.
        types.add(
            TypeSurface(
                owner,
                info.access_flags & 0x7DDF,
                canonical_name(info.super_name, mapping.classes),
                tuple(
                    sorted(
                        canonical_name(value, mapping.classes) or ""
                        for value in info.interfaces
                    )
                ),
            )
        )
        for field in info.fields:
            members.add(
                Member(
                    owner,
                    "field",
                    mapping.members.get(
                        (original_owner, "field", field.descriptor, field.name),
                        field.name,
                    ),
                    canonical_descriptor(field.descriptor, mapping.classes),
                    field.access_flags & 0x50DF,
                )
            )
        for method in info.methods:
            members.add(
                Member(
                    owner,
                    "method",
                    mapping.members.get(
                        (original_owner, "method", method.descriptor, method.name),
                        method.name,
                    ),
                    canonical_descriptor(method.descriptor, mapping.classes),
                    method.access_flags & 0x1DFF,
                )
            )
    return types, members


def source_problems(mapping: Mapping) -> list[str]:
    problems: list[str] = []
    sources = compile_named_java.java_sources(SOURCE_ROOT)
    expected_names = set(mapping.classes.values())
    actual_names = {path.stem for path in sources}
    if len(sources) != EXPECTED_CLASSES:
        problems.append(f"expected {EXPECTED_CLASSES} source files, found {len(sources)}")
    for path in sources:
        text = path.read_text(encoding="utf-8")
        original = next(
            (raw for raw, semantic in mapping.classes.items() if semantic == path.stem),
            None,
        )
        if original is None:
            problems.append(f"{path.relative_to(ROOT)} is not mapped from an original class")
        elif f"original class {{@code {original}}}" not in text:
            problems.append(f"{path.relative_to(ROOT)} lacks original class provenance")
        for marker in FORBIDDEN_SOURCE:
            if marker in text:
                problems.append(f"{path.relative_to(ROOT)} contains {marker!r}")
    if actual_names != expected_names:
        problems.append(
            f"canonical source coverage mismatch: missing={sorted(expected_names - actual_names)}, "
            f"extra={sorted(actual_names - expected_names)}"
        )
    return problems


def describe_delta(label: str, expected: set, actual: set) -> list[str]:
    problems: list[str] = []
    missing = sorted(expected - actual)
    extra = sorted(actual - expected)
    if missing:
        problems.append(f"missing {label}: " + "; ".join(map(str, missing[:12])))
    if extra:
        problems.append(f"extra {label}: " + "; ".join(map(str, extra[:12])))
    return problems


def compare(
    originals: list[classfile.ClassInfo],
    compiled: list[classfile.ClassInfo],
    mapping: Mapping,
    *,
    inject_signature_defect: bool = False,
) -> list[str]:
    expected_types, expected_members = surfaces(originals, mapping)
    actual_types, actual_members = surfaces(compiled, Mapping({}, {}))
    if inject_signature_defect:
        victim = min(expected_members)
        expected_members.remove(victim)
        expected_members.add(
            Member(victim.owner, victim.kind, victim.name + "_MUTATED", victim.descriptor, victim.access)
        )
    problems = describe_delta("types", expected_types, actual_types)
    problems.extend(describe_delta("members", expected_members, actual_members))
    original_methods = sum(len(item.methods) for item in originals)
    if len(originals) != EXPECTED_CLASSES:
        problems.append(
            f"baseline invariant changed: {len(originals)} classes, expected {EXPECTED_CLASSES}"
        )
    if original_methods != EXPECTED_METHODS:
        problems.append(
            f"baseline invariant changed: {original_methods} methods, expected {EXPECTED_METHODS}"
        )
    return problems


def mapping_problems(originals: list[classfile.ClassInfo], mapping: Mapping) -> list[str]:
    problems: list[str] = []
    original_names = {item.internal_name for item in originals}
    if set(mapping.classes) != original_names:
        problems.append("mappings.tiny class coverage differs from baseline bytecode")
    expected_members = {
        (info.internal_name, "field", field.descriptor, field.name)
        for info in originals
        for field in info.fields
    } | {
        (info.internal_name, "method", method.descriptor, method.name)
        for info in originals
        for method in info.methods
        if method.name not in {"<init>", "<clinit>"}
    }
    if set(mapping.members) != expected_members:
        missing = sorted(expected_members - set(mapping.members))
        extra = sorted(set(mapping.members) - expected_members)
        problems.append(
            f"mappings.tiny member coverage mismatch: missing={missing[:8]}, extra={extra[:8]}"
        )
    if len(set(mapping.classes.values())) != len(mapping.classes):
        problems.append("mappings.tiny semantic class names are not unique")
    return problems


def reference_problems(
    originals: list[classfile.ClassInfo], reference: list[classfile.ClassInfo]
) -> list[str]:
    problems: list[str] = []
    by_name = {info.internal_name: info for info in reference}
    for original in originals:
        other = by_name.get(original.internal_name)
        if other is None:
            problems.append(f"semantic reference lacks class {original.internal_name}")
            continue
        original_fields = {(item.name, item.descriptor) for item in original.fields}
        reference_fields = {(item.name, item.descriptor) for item in other.fields}
        original_methods = {(item.name, item.descriptor) for item in original.methods}
        reference_methods = {(item.name, item.descriptor) for item in other.methods}
        if not original_fields <= reference_fields:
            problems.append(
                f"semantic reference lacks fields in {original.internal_name}: "
                f"{sorted(original_fields - reference_fields)[:8]}"
            )
        if not original_methods <= reference_methods:
            problems.append(
                f"semantic reference lacks methods in {original.internal_name}: "
                f"{sorted(original_methods - reference_methods)[:8]}"
            )
    return problems


def run(*, self_test: bool) -> int:
    mapping = read_mappings(MAPPINGS)
    problems = source_problems(mapping)
    baseline_id, baseline_sha256, payload = selected_payload("baseline")
    reference_id, reference_sha256, reference_payload = selected_payload("naming_reference")
    originals = original_classes(payload)
    reference = original_classes(reference_payload)
    problems.extend(mapping_problems(originals, mapping))
    problems.extend(reference_problems(originals, reference))
    class_map = tomllib.loads(CLASS_MAP.read_text(encoding="utf-8"))
    if class_map.get("baseline_build") != baseline_id or class_map.get("baseline_sha256") != baseline_sha256:
        problems.append("classes.toml baseline identity changed")
    if class_map.get("semantic_reference_build") != reference_id or class_map.get("semantic_reference_sha256") != reference_sha256:
        problems.append("classes.toml semantic-reference identity changed")
    symbols = tomllib.loads(SYMBOLS.read_text(encoding="utf-8"))
    if symbols.get("baseline_sha256") != baseline_sha256:
        problems.append("symbols.toml baseline identity changed")
    if symbols.get("semantic_reference_sha256") != reference_sha256:
        problems.append("symbols.toml semantic-reference identity changed")
    if symbols.get("mapping") != "reviewed-tiny-v2":
        problems.append("symbols.toml no longer points to the reviewed Tiny mapping")
    coverage = symbols.get("coverage", {})
    expected_coverage = {
        "classes": len(originals),
        "fields": sum(len(item.fields) for item in originals),
        "methods": sum(len(item.methods) for item in originals),
        "mapped_non_initializer_methods": sum(
            method.name not in {"<init>", "<clinit>"}
            for item in originals
            for method in item.methods
        ),
        "mapped_members": len(mapping.members),
        "semantic_member_renames": sum(
            semantic != original
            for (_owner, _kind, _descriptor, original), semantic in mapping.members.items()
        ),
        "cross_build_fields": sum(len(item.fields) for item in originals),
        "cross_build_methods": sum(len(item.methods) for item in originals),
    }
    if coverage != expected_coverage:
        problems.append(f"symbols.toml coverage changed: expected {expected_coverage}, found {coverage}")
    with tempfile.TemporaryDirectory(prefix="orphan-java-surface-") as temporary:
        output = Path(temporary) / "classes"
        compile_named_java.compile_source(output)
        compiled = compiled_classes(output)
        clean = compare(originals, compiled, mapping)
        problems.extend(clean)
        if self_test and not problems:
            injected = compare(
                originals, compiled, mapping, inject_signature_defect=True
            )
            if not injected:
                print("SELF-TEST FAILED: a member-signature mutation was invisible")
                return 3
            print(
                "self-test OK: an in-memory original member-signature mutation "
                "was rejected (R3)"
            )
            return 0
    if problems:
        print("canonical Java validation failed:", file=sys.stderr)
        for problem in problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    field_count = sum(len(item.fields) for item in originals)
    print(
        f"canonical Java surface OK: {len(originals)} classes, {field_count} fields, "
        f"{EXPECTED_METHODS} methods; baseline {baseline_sha256[:12]}"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    return run(self_test=arguments.self_test)


if __name__ == "__main__":
    raise SystemExit(main())
