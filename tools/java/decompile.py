#!/usr/bin/env python3
"""Regenerate JADX and CFR evidence for a verified build.

Decompiler trees are ignored, disposable evidence. The selected JAR is resolved
by its builds.toml content hash, never by a collected filename.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))

import corpus  # noqa: E402


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tool_version(command: list[str]) -> str:
    result = subprocess.run(command, check=True, text=True, capture_output=True)
    return (result.stdout or result.stderr).strip().splitlines()[0]


def selected_build(build_id: str):
    wanted = build_id or corpus.load_manifest().get("baseline", "")
    matches = [build for build in corpus.builds() if build.build_id == wanted]
    if len(matches) != 1:
        raise SystemExit(f"build {wanted!r} does not resolve to exactly one verified JAR")
    return matches[0]


def source_inventory(root: Path) -> list[dict]:
    return [
        {
            "path": path.relative_to(root).as_posix(),
            "sha256": digest(path),
            "bytes": path.stat().st_size,
        }
        for path in sorted(root.rglob("*.java"))
    ]


def generate(build_id: str, out_root: Path) -> None:
    build = selected_build(build_id)
    target = out_root / build.build_id
    temporary = Path(tempfile.mkdtemp(prefix="orphan-decompile-", dir=out_root))
    try:
        jar = temporary / "baseline.jar"
        jar.write_bytes(build.payload)
        jadx_out = temporary / "jadx"
        cfr_out = temporary / "cfr"
        subprocess.run(
            ["jadx", "--no-res", "--show-bad-code", "-d", str(jadx_out), str(jar)],
            check=True,
        )
        subprocess.run(
            ["cfr", str(jar), "--outputdir", str(cfr_out), "--silent", "true"],
            check=True,
        )
        jar.unlink()
        index = {
            "build_id": build.build_id,
            "payload_sha256": build.sha256,
            "jadx_version": tool_version(["jadx", "--version"]),
            "cfr_version": tool_version(["cfr", "--version"]),
            "jadx_sources": source_inventory(jadx_out),
            "cfr_sources": source_inventory(cfr_out),
        }
        if not index["jadx_sources"] or not index["cfr_sources"]:
            raise SystemExit("a decompiler emitted no Java sources")
        (temporary / "index.json").write_text(
            json.dumps(index, sort_keys=True, indent=2) + "\n", encoding="utf-8"
        )
        if target.exists():
            shutil.rmtree(target)
        temporary.rename(target)
    except BaseException:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
    print(
        f"Decompiled {build.build_id}: {len(index['jadx_sources'])} JADX and "
        f"{len(index['cfr_sources'])} CFR sources under {target}"
    )


def check(build_id: str, out_root: Path) -> None:
    build = selected_build(build_id)
    target = out_root / build.build_id
    index_path = target / "index.json"
    if not index_path.is_file():
        raise SystemExit(f"missing {index_path}; run the decompile target first")
    index = json.loads(index_path.read_text(encoding="utf-8"))
    errors: list[str] = []
    if index.get("payload_sha256") != build.sha256:
        errors.append("payload hash changed")
    for key, subdir in (("jadx_sources", "jadx"), ("cfr_sources", "cfr")):
        actual = source_inventory(target / subdir)
        if actual != index.get(key):
            errors.append(f"{subdir} source inventory changed")
    if errors:
        raise SystemExit("decompile evidence is stale: " + "; ".join(errors))
    print(
        f"decompile evidence OK: {len(index['jadx_sources'])} JADX and "
        f"{len(index['cfr_sources'])} CFR sources for {build.build_id}"
    )


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("build", nargs="?", default="")
    parser.add_argument("--out-root", type=Path, default=ROOT / "_reference" / "decompiled")
    parser.add_argument("--check", action="store_true")
    arguments = parser.parse_args(argv)
    arguments.out_root.mkdir(parents=True, exist_ok=True)
    if arguments.check:
        check(arguments.build, arguments.out_root)
    else:
        generate(arguments.build, arguments.out_root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
