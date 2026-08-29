#!/usr/bin/env python3
"""Content-address every non-class resource in every recorded JAR."""

from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

import corpus

REPO = corpus.REPO
DEFAULT_OUT = REPO / "_reference"

EXT_CATEGORY = {
    "amr": "audio",
    "bin": "chunk",
    "ini": "config",
    "jad": "descriptor",
    "java~": "source-evidence",
    "lan": "narrative-localization",
    "mid": "midi",
    "midi": "midi",
    "png": "image",
    "properties": "ui-localization",
    "wav": "audio",
}


def category(member: str) -> str:
    if member.upper() == "META-INF/MANIFEST.MF":
        return "manifest"
    base = member.rsplit("/", 1)[-1]
    lower = base.lower()
    for extension, kind in EXT_CATEGORY.items():
        if lower.endswith("." + extension):
            return kind
    return "pack" if "." not in base else "other"


@dataclass
class Blob:
    sha256: str
    size: int
    category: str
    occurrences: list[tuple[str, str]] = field(default_factory=list)


def analyze(*, corrupt: tuple[str, str] | None = None) -> dict[str, Blob]:
    blobs: dict[str, Blob] = {}
    for build in corpus.builds():
        for member, original in corpus.jar_members(build.payload):
            if member.endswith(".class"):
                continue
            data = original
            if corrupt == (build.build_id, member) and data:
                data = bytes([data[0] ^ 1]) + data[1:]
            digest = corpus.sha256(data)
            kind = category(member)
            blob = blobs.setdefault(digest, Blob(digest, len(data), kind))
            # Empty/default resources are occasionally reused under unrelated
            # filenames. Content identity still wins; retain that fact as a
            # mixed-role blob instead of duplicating it by name.
            if blob.category != kind:
                blob.category = "mixed"
            blob.occurrences.append((build.build_id, member))
    for blob in blobs.values():
        blob.occurrences.sort()
    return blobs


def carriers(blob: Blob) -> list[str]:
    return sorted({build for build, _ in blob.occurrences})


def member_names(blob: Blob) -> list[str]:
    return sorted({member for _, member in blob.occurrences})


def sort_key(blob: Blob) -> tuple:
    return (blob.category, member_names(blob)[0], blob.sha256)


def resources_json(blobs: dict[str, Blob]) -> str:
    ordered = sorted(blobs.values(), key=sort_key)
    per_category: dict[str, int] = defaultdict(int)
    per_build: dict[str, int] = defaultdict(int)
    for blob in ordered:
        per_category[blob.category] += 1
        for build in carriers(blob):
            per_build[build] += 1
    document = {
        "unique_blob_count": len(ordered),
        "total_occurrences": sum(len(blob.occurrences) for blob in ordered),
        "unique_by_category": dict(sorted(per_category.items())),
        "unique_blobs_per_build": dict(sorted(per_build.items())),
        "blobs": [
            {
                "sha256": blob.sha256,
                "size": blob.size,
                "category": blob.category,
                "member_names": member_names(blob),
                "carriers": carriers(blob),
                "occurrences": [
                    {"build": build, "member": member}
                    for build, member in blob.occurrences
                ],
            }
            for blob in ordered
        ],
    }
    return json.dumps(document, sort_keys=True, indent=2, ensure_ascii=True) + "\n"


def resources_tsv(blobs: dict[str, Blob]) -> str:
    lines = ["\t".join([
        "sha256_12", "size", "category", "n_builds", "n_occurrences",
        "representative_name", "carriers",
    ])]
    for blob in sorted(blobs.values(), key=sort_key):
        lines.append("\t".join([
            blob.sha256[:12],
            str(blob.size),
            blob.category,
            str(len(carriers(blob))),
            str(len(blob.occurrences)),
            member_names(blob)[0],
            ",".join(carriers(blob)),
        ]))
    return "\n".join(lines) + "\n"


def write_outputs(blobs: dict[str, Blob], out_dir: Path) -> dict[str, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    files = {
        "resources.json": resources_json(blobs),
        "resources.tsv": resources_tsv(blobs),
    }
    result: dict[str, Path] = {}
    for name, contents in files.items():
        path = out_dir / name
        path.write_text(contents, encoding="utf-8", newline="")
        result[name] = path
    return result


def self_test() -> int:
    clean = analyze()
    if resources_json(clean) != resources_json(analyze()):
        print("SELF-TEST FAILED: catalog regeneration is not byte-identical")
        return 3
    shared = [blob for blob in clean.values() if len(carriers(blob)) >= 2 and blob.size]
    if not shared:
        print("SELF-TEST FAILED: no cross-build shared resource exists")
        return 3
    target = min(shared, key=lambda blob: blob.sha256)
    build, member = target.occurrences[0]
    dirty = analyze(corrupt=(build, member))
    if resources_json(clean) == resources_json(dirty):
        print("SELF-TEST FAILED: a one-byte resource perturbation was invisible")
        return 3
    remaining = dirty.get(target.sha256)
    if remaining is None or len(carriers(remaining)) >= len(carriers(target)):
        print("SELF-TEST FAILED: content-addressed dedup did not split the mutation")
        return 3
    print("self-test OK: deterministic catalog split a one-byte mutation (R3)")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args(argv)
    if arguments.self_test:
        return self_test()
    blobs = analyze()
    written = write_outputs(blobs, arguments.out_dir)
    counts: dict[str, int] = defaultdict(int)
    for blob in blobs.values():
        counts[blob.category] += 1
    print(
        f"Cataloged {len(blobs)} unique resources from "
        f"{sum(len(blob.occurrences) for blob in blobs.values())} occurrences."
    )
    for kind, count in sorted(counts.items()):
        print(f"  {kind:24s} {count}")
    for path in written.values():
        print(f"  {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
