#!/usr/bin/env python3
"""Materialize surviving in-JAR Java editor backups as regenerable evidence."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))

import corpus  # noqa: E402


def backups(build: corpus.Build) -> list[tuple[str, bytes]]:
    with zipfile.ZipFile(io.BytesIO(build.payload)) as archive:
        return [
            (name, archive.read(name))
            for name in sorted(archive.namelist())
            if name.lower().endswith(".java~")
        ]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("build_id")
    parser.add_argument(
        "--out", type=Path, default=ROOT / "_reference" / "source-backups"
    )
    arguments = parser.parse_args()
    matches = [build for build in corpus.builds() if build.build_id == arguments.build_id]
    if len(matches) != 1:
        raise SystemExit(f"build {arguments.build_id!r} does not resolve uniquely")
    build = matches[0]
    entries = backups(build)
    if not entries:
        raise SystemExit(f"build {build.build_id!r} contains no .java~ backups")

    destination = arguments.out / build.build_id
    destination.mkdir(parents=True, exist_ok=True)
    inventory = []
    for name, data in entries:
        relative = Path(name)
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        inventory.append(
            {
                "member": name,
                "bytes": len(data),
                "sha256": hashlib.sha256(data).hexdigest(),
            }
        )
    (destination / "inventory.json").write_text(
        json.dumps(
            {
                "build_id": build.build_id,
                "payload_sha256": build.sha256,
                "backups": inventory,
            },
            indent=2,
            sort_keys=True,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"extracted {len(entries)} source backups to {destination}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
