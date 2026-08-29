#!/usr/bin/env python3
"""Hash-lock the complete javac AST denominator to the original bytecode.

This is the source half of every future transliteration crosswalk. It records
all declaration/body ASTs and their full pre-order node inventories. The Rust
half is emitted by `orphan-ast-audit`; a method may only be marked reviewed when
the audit manifest pairs every node on both sides and records the original
method's bytecode/opcode authority.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import io
import subprocess
import sys
import tempfile
import tomllib
import zipfile
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CANONICAL = ROOT / "java" / "src" / "main" / "java" / "defpackage"
DUMPER = ROOT / "tools" / "transliteration" / "JavaAstAuditDump.java"
MANIFEST = ROOT / "java" / "reconstruction" / "ast-authority.toml"
sys.path.insert(0, str(ROOT / "tools" / "corpus"))

import classfile  # noqa: E402
import corpus  # noqa: E402


@dataclass(frozen=True)
class AstEvidence:
    ast: str
    nodes: tuple[str, ...]


def digest(value: str | bytes) -> str:
    if isinstance(value, str):
        value = value.encode("utf-8")
    return hashlib.sha256(value).hexdigest()


def java_asts() -> dict[tuple[str, str], AstEvidence]:
    sources = sorted(CANONICAL.glob("*.java"))
    if not sources:
        raise RuntimeError("canonical Java tree is empty")
    with tempfile.TemporaryDirectory(prefix="orphan-java-ast-") as directory:
        subprocess.run(
            ["javac", "-d", directory, str(DUMPER)],
            check=True,
            capture_output=True,
            text=True,
        )
        output = subprocess.run(
            ["java", "-cp", directory, "JavaAstAuditDump", *map(str, sources)],
            check=True,
            capture_output=True,
            text=True,
        ).stdout
    result: dict[tuple[str, str], AstEvidence] = {}
    for line in output.splitlines():
        _source, owner, item, encoded_ast, encoded_nodes = line.split("\t", 4)
        key = (owner, item)
        if key in result:
            raise RuntimeError(f"duplicate javac AST item {owner}.{item}")
        raw_nodes = base64.b64decode(encoded_nodes).decode("utf-8")
        result[key] = AstEvidence(
            base64.b64decode(encoded_ast).decode("utf-8"),
            tuple(raw_nodes.splitlines()) if raw_nodes else (),
        )
    return result


def baseline_classes() -> tuple[str, list[classfile.ClassInfo]]:
    selected = corpus.load_manifest()["baseline"]
    build = next(item for item in corpus.builds() if item.build_id == selected)
    classes = []
    with zipfile.ZipFile(io.BytesIO(build.payload)) as archive:
        for name in sorted(archive.namelist()):
            if name.endswith(".class") and "/" not in name:
                classes.append(classfile.parse_class(name, archive.read(name)))
    return build.sha256, classes


def ast_inventory_digest(items: dict[tuple[str, str], AstEvidence]) -> str:
    return digest(
        "\n".join(
            f"{owner}\t{item}\t{digest(evidence.ast)}\t{digest(chr(10).join(evidence.nodes))}"
            for (owner, item), evidence in sorted(items.items())
        )
    )


def bytecode_inventory_digest(classes: list[classfile.ClassInfo]) -> str:
    return digest(
        "\n".join(
            "\t".join(
                [
                    info.internal_name,
                    str(method.ordinal),
                    method.name,
                    method.descriptor,
                    method.code_sha256 or "no-code",
                    method.opcode_sha256 or "no-code",
                ]
            )
            for info in sorted(classes, key=lambda value: value.internal_name)
            for method in sorted(info.methods, key=lambda value: value.ordinal)
        )
    )


def facts(items: dict[tuple[str, str], AstEvidence]) -> dict[str, int | str]:
    return {
        "java_item_count": len(items),
        "java_field_count": sum(item.startswith("<field:") for _, item in items),
        "java_executable_count": sum(not item.startswith("<field:") for _, item in items),
        "java_node_count": sum(len(evidence.nodes) for evidence in items.values()),
        "java_ast_inventory_sha256": ast_inventory_digest(items),
    }


def print_manifest() -> None:
    items = java_asts()
    baseline_sha256, classes = baseline_classes()
    values = facts(items)
    print("schema_version = 1")
    print(f'baseline_sha256 = "{baseline_sha256}"')
    print(f'bytecode_inventory_sha256 = "{bytecode_inventory_digest(classes)}"')
    for name, value in values.items():
        rendered = f'"{value}"' if isinstance(value, str) else str(value)
        print(f"{name} = {rendered}")
    print("reviewed_crosswalk_count = 0")


def validate(*, self_test: bool) -> int:
    expected = tomllib.loads(MANIFEST.read_text(encoding="utf-8"))
    items = java_asts()
    baseline_sha256, classes = baseline_classes()
    actual = facts(items)
    actual["baseline_sha256"] = baseline_sha256
    actual["bytecode_inventory_sha256"] = bytecode_inventory_digest(classes)
    audit_path = ROOT / "transliteration" / "audits" / "method-audit.toml"
    if audit_path.is_file():
        audit = tomllib.loads(audit_path.read_text(encoding="utf-8"))
        actual["reviewed_crosswalk_count"] = audit.get("reviewed_body_count")
    errors = [
        f"{name}: expected {expected.get(name)!r}, found {value!r}"
        for name, value in actual.items()
        if expected.get(name) != value
    ]
    if errors:
        print("AST authority validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    if self_test:
        mutated = dict(items)
        key = min(mutated)
        evidence = mutated[key]
        mutated[key] = AstEvidence(evidence.ast + " MUTATED", evidence.nodes)
        if ast_inventory_digest(mutated) == expected["java_ast_inventory_sha256"]:
            print("SELF-TEST FAILED: a javac AST mutation was invisible")
            return 3
        print("self-test OK: a one-node javac AST mutation was rejected (R3)")
        return 0
    print(
        "javac AST authority OK: "
        f"{actual['java_item_count']} items / {actual['java_node_count']} nodes; "
        f"{sum(len(info.methods) for info in classes)} original methods"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--print-manifest", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    if arguments.print_manifest:
        print_manifest()
        return 0
    return validate(self_test=arguments.self_test)


if __name__ == "__main__":
    raise SystemExit(main())
