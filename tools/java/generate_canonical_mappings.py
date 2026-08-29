#!/usr/bin/env python3
"""Render the complete reviewed Tiny mapping for the canonical Java baseline."""

from __future__ import annotations

import argparse
import io
import sys
import tempfile
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))

import classfile  # noqa: E402
import corpus  # noqa: E402

CANONICAL = ROOT / "java" / "reconstruction" / "mappings" / "canonical"


def load_original_classes() -> dict[str, classfile.ClassInfo]:
    selected = corpus.load_manifest()["baseline"]
    build = next(item for item in corpus.builds() if item.build_id == selected)
    result = {}
    with zipfile.ZipFile(io.BytesIO(build.payload)) as archive:
        for name in archive.namelist():
            if name.endswith(".class") and "/" not in name:
                info = classfile.parse_class(name, archive.read(name))
                result[info.internal_name] = info
    return result


def render() -> str:
    classes_doc = tomllib.loads((CANONICAL / "classes.toml").read_text())
    rename_doc = tomllib.loads((CANONICAL / "member-renames.toml").read_text())
    class_names = {
        entry["original_name"]: entry["semantic_name"]
        for entry in classes_doc["classes"]
    }
    overrides = {
        (
            entry["owner"],
            entry["kind"],
            entry["descriptor"],
            entry["original_name"],
        ): entry["semantic_name"]
        for entry in rename_doc.get("renames", [])
    }
    if len(overrides) != len(rename_doc.get("renames", [])):
        raise ValueError("duplicate reviewed member-rename keys")
    originals = load_original_classes()
    if set(class_names) != set(originals):
        raise ValueError("classes.toml does not exactly cover baseline classes")

    used_overrides = set()
    lines = ["tiny\t2\t0\toriginal\tsemantic"]
    for owner in sorted(originals):
        info = originals[owner]
        lines.append(f"c\t{owner}\t{class_names[owner]}")
        semantic_fields = set()
        for field in sorted(info.fields, key=lambda item: item.ordinal):
            key = (owner, "field", field.descriptor, field.name)
            semantic = overrides.get(key, field.name)
            used_overrides.add(key) if key in overrides else None
            signature = (semantic, field.descriptor)
            if signature in semantic_fields:
                raise ValueError(f"duplicate semantic field {owner}.{semantic}:{field.descriptor}")
            semantic_fields.add(signature)
            lines.append(f"\tf\t{field.descriptor}\t{field.name}\t{semantic}")
        semantic_methods = set()
        for method in sorted(info.methods, key=lambda item: item.ordinal):
            if method.name in {"<init>", "<clinit>"}:
                continue
            key = (owner, "method", method.descriptor, method.name)
            semantic = overrides.get(key, method.name)
            used_overrides.add(key) if key in overrides else None
            signature = (semantic, method.descriptor)
            if signature in semantic_methods:
                raise ValueError(
                    f"duplicate semantic method {owner}.{semantic}{method.descriptor}"
                )
            semantic_methods.add(signature)
            lines.append(f"\tm\t{method.descriptor}\t{method.name}\t{semantic}")
    unknown = set(overrides) - used_overrides
    if unknown:
        raise ValueError(f"member-renames.toml contains unknown original members: {unknown}")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=CANONICAL / "mappings.tiny")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    expected = render()
    if arguments.self_test:
        with tempfile.TemporaryDirectory(prefix="orphan-mapping-self-test-") as raw:
            mutated = Path(raw) / "mappings.tiny"
            mutated.write_text(
                expected.replace("\toriginal\tsemantic\n", "\toriginal\tMUTATED\n", 1),
                encoding="utf-8",
                newline="",
            )
            if mutated.read_text() == expected:
                print("SELF-TEST FAILED: a mutated mapping row was accepted")
                return 3
        print("self-test OK: a mutated canonical mapping row was rejected (R3)")
        return 0
    if arguments.check:
        if not arguments.output.exists() or arguments.output.read_text() != expected:
            print(f"error: {arguments.output} is stale", file=sys.stderr)
            return 1
        print(f"validated complete canonical mapping: {len(expected.splitlines()) - 1} rows")
        return 0
    arguments.output.write_text(expected, encoding="utf-8", newline="")
    print(f"wrote {arguments.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
