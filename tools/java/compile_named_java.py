#!/usr/bin/env python3
"""Compile the canonical named Java application against declaration-only stubs."""

from __future__ import annotations

import argparse
import hashlib
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "java" / "src" / "main" / "java"
STUB_ROOT = ROOT / "java" / "api-stubs"


def java_sources(root: Path) -> list[Path]:
    sources = sorted(root.rglob("*.java"))
    if not sources:
        raise SystemExit(f"no Java sources found under {root}")
    return sources


def run_javac(sources: list[Path], destination: Path, class_path: Path | None = None) -> None:
    command = [
        "javac",
        "-encoding",
        "UTF-8",
        "--release",
        "8",
        "-Xlint:-options",
        "-d",
        str(destination),
    ]
    if class_path is not None:
        command.extend(["-classpath", str(class_path)])
    command.extend(str(source) for source in sources)
    subprocess.run(command, check=True)


def compile_source(emit_classes: Path | None = None) -> None:
    with tempfile.TemporaryDirectory(prefix="orphan-java-build-") as temporary:
        root = Path(temporary)
        stub_classes = root / "stub-classes"
        application_classes = root / "application-classes"
        stub_classes.mkdir()
        application_classes.mkdir()
        run_javac(java_sources(STUB_ROOT), stub_classes)
        run_javac(java_sources(SOURCE_ROOT), application_classes, stub_classes)
        if emit_classes is not None:
            if emit_classes.exists():
                shutil.rmtree(emit_classes)
            shutil.copytree(application_classes, emit_classes)


def write_jar(classes_root: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".tmp")
    if temporary.exists():
        temporary.unlink()
    with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        manifest = zipfile.ZipInfo("META-INF/MANIFEST.MF", date_time=(1980, 1, 1, 0, 0, 0))
        manifest.external_attr = 0o100644 << 16
        archive.writestr(
            manifest,
            b"Manifest-Version: 1.0\r\n"
            b"MIDlet-Name: Silent Hill: Orphan\r\n"
            b"MIDlet-Version: 0.1.0\r\n"
            b"MIDlet-Vendor: J2ME Preservation Project\r\n"
            b"MIDlet-1: Silent Hill: Orphan,,defpackage.Application\r\n"
            b"MicroEdition-Profile: MIDP-2.0\r\n"
            b"MicroEdition-Configuration: CLDC-1.1\r\n\r\n",
        )
        for source in sorted(classes_root.rglob("*.class")):
            entry = zipfile.ZipInfo(
                source.relative_to(classes_root).as_posix(),
                date_time=(1980, 1, 1, 0, 0, 0),
            )
            entry.external_attr = 0o100644 << 16
            archive.writestr(entry, source.read_bytes())
    temporary.replace(destination)


def build_jar(destination: Path) -> None:
    with tempfile.TemporaryDirectory(prefix="orphan-java-jar-") as temporary:
        classes = Path(temporary) / "classes"
        compile_source(classes)
        write_jar(classes, destination)


def jar_inventory(path: Path) -> list[str]:
    with zipfile.ZipFile(path) as archive:
        return sorted(archive.namelist())


def reproducible_jar(*, inject_defect: bool) -> int:
    with tempfile.TemporaryDirectory(prefix="orphan-java-repro-") as temporary:
        root = Path(temporary)
        first = root / "first.jar"
        second = root / "second.jar"
        build_jar(first)
        build_jar(second)
        left = first.read_bytes()
        right = second.read_bytes()
        inventory = jar_inventory(first)
        expected_classes = len(java_sources(SOURCE_ROOT))
        class_entries = [name for name in inventory if name.endswith(".class")]
        if len(class_entries) != expected_classes or any(
            not name.startswith("defpackage/") for name in class_entries
        ):
            raise SystemExit("application JAR class inventory is incomplete or contains stubs")
        if inject_defect:
            right = right[:-1] + bytes([right[-1] ^ 1])
        if inject_defect:
            if left == right:
                print("SELF-TEST FAILED: a one-byte application JAR mutation was invisible")
                return 3
            print("self-test OK: a one-byte application JAR mutation was rejected (R3)")
            return 0
        if left != right:
            raise SystemExit("two clean application JAR builds were not byte-identical")
        print(
            f"reproducible Java app OK: {expected_classes} classes, "
            f"sha256 {hashlib.sha256(left).hexdigest()}"
        )
        return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--check-reproducible", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    if arguments.check_reproducible or arguments.self_test:
        if arguments.jar is not None:
            parser.error("--jar cannot be combined with a reproducibility check")
        return reproducible_jar(inject_defect=arguments.self_test)
    if arguments.jar is None:
        compile_source()
        print(f"typecheck OK: {len(java_sources(SOURCE_ROOT))} canonical sources")
        return 0
    build_jar(arguments.jar)
    print(f"wrote deterministic application JAR {arguments.jar}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
