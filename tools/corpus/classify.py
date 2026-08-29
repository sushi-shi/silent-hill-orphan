#!/usr/bin/env python3
"""Fingerprint every Silent Hill: Orphan JAR directly from class bytes.

Outputs are deterministic, regenerable evidence under ``_reference``.  No
decompiler text and no filename-derived device claim participates in identity.
"""

from __future__ import annotations

import argparse
import io
import json
import sys
import zipfile
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

import classfile
import corpus

REPO = corpus.REPO
DEFAULT_OUT = REPO / "_reference"


def is_game_class(info: classfile.ClassInfo) -> bool:
    """Separate game classes from bundled handset and repack adapters.

    Most builds keep the game in the default package.  Twenty-five compact
    builds instead package the MIDlet as ``sh/M`` while leaving the other two
    game classes in the default package.  That class extends MIDlet and owns
    the same runtime/resource/persistence surface as default-package ``M``;
    excluding it used to erase 69--78 real methods from those build trees.

    The remaining packaged classes are Nokia/Motorola/Alcatel compatibility
    adapters or a third-party demo overlay and are deliberately excluded.
    """
    return "/" not in info.internal_name or info.internal_name == "sh/M"


@dataclass
class BuildAnalysis:
    build_id: str
    sha256: str
    size: int
    version: str
    declared_language: str
    locale_codes: tuple[str, ...]
    source_backups: tuple[str, ...]
    official: object
    archived: bool
    classes: list[classfile.ClassInfo]

    @property
    def game_classes(self) -> list[classfile.ClassInfo]:
        return [item for item in self.classes if is_game_class(item)]

    @property
    def game_shape_set(self) -> tuple[str, ...]:
        return tuple(sorted(item.shape_sha256 for item in self.game_classes))

    def method_count(self) -> int:
        return sum(len(item.methods) for item in self.classes)

    def game_method_count(self) -> int:
        return sum(len(item.methods) for item in self.game_classes)

    def game_code_size(self) -> int:
        return sum(
            method.code_size
            for item in self.game_classes
            for method in item.methods
        )


def analyze(*, corrupt: tuple[str, str] | None = None) -> list[BuildAnalysis]:
    analyses: list[BuildAnalysis] = []
    for build in corpus.builds():
        with zipfile.ZipFile(io.BytesIO(build.payload)) as jar:
            members = sorted(name for name in jar.namelist() if name.endswith(".class"))
            classes: list[classfile.ClassInfo] = []
            for member in members:
                data = jar.read(member)
                if corrupt == (build.build_id, member) and data:
                    at = len(data) // 2
                    data = data[:at] + bytes([data[at] ^ 1]) + data[at + 1 :]
                classes.append(classfile.parse_class(member, data))
            backups = tuple(
                sorted(name for name in jar.namelist() if name.lower().endswith(".java~"))
            )
        analyses.append(
            BuildAnalysis(
                build_id=build.build_id,
                sha256=build.sha256,
                size=build.size,
                version=build.manifest_version,
                declared_language=build.declared_language,
                locale_codes=tuple(corpus.locale_members(build.payload)),
                source_backups=backups,
                official=build.official,
                archived=build.archived,
                classes=classes,
            )
        )
    return analyses


def multiset(classes: list[classfile.ClassInfo], key: str) -> Counter:
    return Counter(getattr(method, key) for item in classes for method in item.methods)


def dice(left: Counter, right: Counter) -> float:
    total = sum(left.values()) + sum(right.values())
    return 1.0 if total == 0 else 2.0 * sum((left & right).values()) / total


def code_families(analyses: list[BuildAnalysis]) -> dict[str, list[str]]:
    groups: dict[tuple[str, ...], list[str]] = {}
    for build in analyses:
        groups.setdefault(build.game_shape_set, []).append(build.build_id)
    return {
        min(members): sorted(members)
        for members in sorted(groups.values(), key=lambda values: min(values))
    }


def baseline_rank(build: BuildAnalysis) -> tuple:
    """Mechanical candidate ordering, not the final curatorial decision."""
    return (
        int(build.archived),
        -build.game_code_size(),
        -build.game_method_count(),
        -len(build.locale_codes),
        build.build_id,
    )


def selected_baseline(analyses: list[BuildAnalysis]) -> BuildAnalysis:
    selected = corpus.load_manifest().get("baseline", "")
    matches = [build for build in analyses if build.build_id == selected]
    if len(matches) != 1:
        raise corpus.CorpusError(
            f"builds.toml baseline {selected!r} does not select exactly one JAR"
        )
    return matches[0]


def pairwise(base: BuildAnalysis, other: BuildAnalysis) -> dict:
    code_left = multiset(base.game_classes, "code_sha256")
    code_right = multiset(other.game_classes, "code_sha256")
    opcode_left = multiset(base.game_classes, "opcode_sha256")
    opcode_right = multiset(other.game_classes, "opcode_sha256")
    shape_left = multiset(base.game_classes, "shape_sha256")
    shape_right = multiset(other.game_classes, "shape_sha256")
    return {
        "base": base.build_id,
        "other": other.build_id,
        "base_game_methods": base.game_method_count(),
        "other_game_methods": other.game_method_count(),
        "game_methods_code_identical": sum((code_left & code_right).values()),
        "game_methods_shape_shared": sum((shape_left & shape_right).values()),
        "game_method_code_dice": round(dice(code_left, code_right), 6),
        "game_method_opcode_dice": round(dice(opcode_left, opcode_right), 6),
        "game_method_shape_dice": round(dice(shape_left, shape_right), 6),
    }


def class_row(info: classfile.ClassInfo) -> dict:
    methods = [
        {
            "ordinal": method.ordinal,
            "name": method.name,
            "descriptor": method.descriptor,
            "access_flags": method.access_flags,
            "code_size": method.code_size,
            "opcode_count": method.opcode_count,
            "code_sha256": method.code_sha256,
            "opcode_sha256": method.opcode_sha256,
            "shape_sha256": method.shape_sha256,
            "calls": method.calls,
        }
        for method in sorted(info.methods, key=lambda value: value.ordinal)
    ]
    external_apis = sorted(
        {
            call.split(".", 1)[0]
            for method in info.methods
            for call in method.calls
            if "/" in call.split(".", 1)[0]
        }
    )
    return {
        "member_path": info.member_path,
        "internal_name": info.internal_name,
        "is_game_class": is_game_class(info),
        "class_sha256": info.class_sha256,
        "shape_sha256": info.shape_sha256,
        "super_name": info.super_name,
        "interfaces": sorted(info.interfaces),
        "external_apis": external_apis,
        "major_version": info.major_version,
        "fields": [
            {
                "ordinal": field.ordinal,
                "name": field.name,
                "descriptor": field.descriptor,
                "access_flags": field.access_flags,
            }
            for field in sorted(info.fields, key=lambda value: value.ordinal)
        ],
        "methods": methods,
    }


def inventory_json(analyses: list[BuildAnalysis]) -> str:
    document = {
        "builds": [
            {
                "build_id": build.build_id,
                "sha256": build.sha256,
                "size": build.size,
                "version": build.version,
                "declared_language": build.declared_language,
                "locale_codes": list(build.locale_codes),
                "source_backups": list(build.source_backups),
                "official": build.official,
                "archived": build.archived,
                "class_count": len(build.classes),
                "game_class_count": len(build.game_classes),
                "method_count": build.method_count(),
                "game_method_count": build.game_method_count(),
                "game_code_size": build.game_code_size(),
                "classes": [class_row(item) for item in sorted(build.classes, key=lambda x: x.member_path)],
            }
            for build in sorted(analyses, key=lambda value: value.build_id)
        ]
    }
    return json.dumps(document, sort_keys=True, indent=2, ensure_ascii=True) + "\n"


def delta_json(analyses: list[BuildAnalysis]) -> str:
    candidate = selected_baseline(analyses)
    document = {
        "comparison_anchor": candidate.build_id,
        "comparison_anchor_is_reviewed_baseline": True,
        "code_families": code_families(analyses),
        "pairwise_vs_anchor": [
            pairwise(candidate, other)
            for other in sorted(analyses, key=lambda value: value.build_id)
            if other.build_id != candidate.build_id
        ],
    }
    return json.dumps(document, sort_keys=True, indent=2, ensure_ascii=True) + "\n"


def builds_tsv(analyses: list[BuildAnalysis]) -> str:
    family_of = {
        build_id: family
        for family, members in code_families(analyses).items()
        for build_id in members
    }
    lines = ["\t".join([
        "build_id", "sha256_12", "version", "official", "archived",
        "declared_language", "locale_codes", "source_backups", "class_count",
        "game_class_count", "game_method_count", "game_code_size", "code_family",
    ])]
    for build in sorted(analyses, key=baseline_rank):
        lines.append("\t".join([
            build.build_id,
            build.sha256[:12],
            build.version,
            str(build.official),
            str(build.archived),
            build.declared_language,
            ",".join(build.locale_codes),
            str(len(build.source_backups)),
            str(len(build.classes)),
            str(len(build.game_classes)),
            str(build.game_method_count()),
            str(build.game_code_size()),
            family_of[build.build_id],
        ]))
    return "\n".join(lines) + "\n"


def write_outputs(analyses: list[BuildAnalysis], out_dir: Path) -> dict[str, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    files = {
        "class-inventory.json": inventory_json(analyses),
        "class-delta.json": delta_json(analyses),
        "builds.tsv": builds_tsv(analyses),
    }
    result: dict[str, Path] = {}
    for name, contents in files.items():
        path = out_dir / name
        path.write_text(contents, encoding="utf-8", newline="")
        result[name] = path
    return result


def self_test() -> int:
    clean = analyze()
    rendered = inventory_json(clean)
    if rendered != inventory_json(analyze()):
        print("SELF-TEST FAILED: a second classification was not byte-identical")
        return 3
    target = selected_baseline(clean)
    member = min(item.member_path for item in target.game_classes)
    try:
        dirty = analyze(corrupt=(target.build_id, member))
    except classfile.ClassFormatError:
        print("self-test OK: a one-byte class perturbation was rejected (R3)")
        return 0
    if inventory_json(dirty) == rendered:
        print("SELF-TEST FAILED: a one-byte class perturbation was invisible")
        return 3
    print("self-test OK: deterministic output changed after a one-byte class perturbation (R3)")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args(argv)
    if arguments.self_test:
        return self_test()
    analyses = analyze()
    written = write_outputs(analyses, arguments.out_dir)
    candidate = selected_baseline(analyses)
    print(
        f"Classified {len(analyses)} JARs into {len(code_families(analyses))} "
        f"exact game-class families."
    )
    print(
        f"Reviewed comparison baseline: {candidate.build_id} "
        f"({candidate.game_method_count()} game methods, {candidate.game_code_size()} code bytes)."
    )
    for path in written.values():
        print(f"  {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
