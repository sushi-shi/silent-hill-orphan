#!/usr/bin/env python3
"""Reject raw and cross-domain IDs in the canonical Java source.

The original classfiles contain many unrelated integer domains with the same
values.  Decompilers sometimes substitute any visible same-valued constant,
which produces compilable but misleading source.  This gate requires the two
recovered protocol dictionaries at every statically known text, Ink command,
and Ink event use site and locks their reviewed consumer denominator.
"""

from __future__ import annotations

import argparse
import re
import shutil
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_ROOT = ROOT / "java" / "src" / "main" / "java" / "defpackage"

EXPECTED_TEXT_ID_USES = 229
EXPECTED_COMMAND_USES = 72
EXPECTED_EVENT_USES = 105
EXPECTED_TEXT_CONSTANTS = 186
EXPECTED_INK_CONSTANTS = 114

ALIAS_COMMENT = re.compile(r"/\*\s*-?\d+\s*\*/")
TEXT_USE = re.compile(r"\bTextId\.[A-Z][A-Z0-9_]*")
COMMAND_USE = re.compile(r"\bInkCodes\.COMMAND_[A-Z0-9_]+")
EVENT_USE = re.compile(r"\bInkCodes\.EVENT_[A-Z0-9_]+")
GET_STRING = re.compile(r"Application\.getString\(([^)\n]+)\)")
DIRECT_PROTOCOL_CALL = re.compile(
    r"\b(?:[A-Za-z_$][\w$]*\.)?(hasEvent|hasCommand|executeEvent)\(\s*([^,\n)]+)"
)
STATIC_EVENT_CALL = re.compile(
    r"\bInkScript\.executeEvent\(\s*[^,\n]+,\s*([^,\n)]+)"
)
RAW_INTEGER = re.compile(r"-?(?:0|[1-9]\d*)$")
CROSS_DOMAIN = re.compile(
    r"^(?:(?:InkEngine|SilentHillGame|GameCanvas)\.)?"
    r"(?:ACTION_KEY|CHOICE_|KEY_|MENU_|SETUP_|INV_|RECOIL_|ZONE_)[A-Z0-9_]*$"
)
CONSTANT_DEFINITION = re.compile(r"^\s*static\s+final\s+int\s+[A-Z][A-Z0-9_]*\s*=", re.M)


def source_texts(source_root: Path) -> dict[str, str]:
    return {
        path.name: path.read_text(encoding="utf-8")
        for path in sorted(source_root.glob("*.java"))
    }


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def protocol_argument_problem(kind: str, argument: str) -> bool:
    argument = argument.strip()
    if kind == "hasCommand":
        return RAW_INTEGER.fullmatch(argument) is not None or (
            CROSS_DOMAIN.fullmatch(argument) is not None
        )
    return (
        RAW_INTEGER.fullmatch(argument) is not None
        or CROSS_DOMAIN.fullmatch(argument) is not None
        or argument == "InkCodes.EVENT_COUNT"
    )


def problems(source_root: Path = SOURCE_ROOT) -> list[str]:
    texts = source_texts(source_root)
    failures: list[str] = []

    for file_name, source in texts.items():
        if file_name not in {"InkCodes.java", "TextId.java"}:
            for match in ALIAS_COMMENT.finditer(source):
                failures.append(
                    f"{file_name}:{line_number(source, match.start())}: "
                    "decompiler same-value alias comment"
                )

        for match in GET_STRING.finditer(source):
            argument = match.group(1).strip()
            if not (
                argument.startswith("TextId.")
                or argument.startswith("Application.language_text_ids[")
            ):
                failures.append(
                    f"{file_name}:{line_number(source, match.start())}: "
                    f"text lookup is not a TextId: {argument}"
                )

        for match in DIRECT_PROTOCOL_CALL.finditer(source):
            kind, argument = match.groups()
            # Method declarations and calls through dynamic event arrays remain
            # valid.  Only a statically known raw/cross-domain value is wrong.
            if argument.lstrip().startswith("int "):
                continue
            if protocol_argument_problem(kind, argument):
                failures.append(
                    f"{file_name}:{line_number(source, match.start())}: "
                    f"{kind} uses raw/cross-domain ID {argument.strip()}"
                )

        for match in STATIC_EVENT_CALL.finditer(source):
            argument = match.group(1).strip()
            if protocol_argument_problem("executeEvent", argument):
                failures.append(
                    f"{file_name}:{line_number(source, match.start())}: "
                    f"static executeEvent uses raw/cross-domain ID {argument}"
                )

    consumers = {
        name: text
        for name, text in texts.items()
        if name not in {"InkCodes.java", "TextId.java"}
    }
    joined = "\n".join(consumers.values())
    denominators = {
        "TextId consumers": (len(TEXT_USE.findall(joined)), EXPECTED_TEXT_ID_USES),
        "Ink command consumers": (
            len(COMMAND_USE.findall(joined)),
            EXPECTED_COMMAND_USES,
        ),
        "Ink event consumers": (len(EVENT_USE.findall(joined)), EXPECTED_EVENT_USES),
        "TextId constants": (
            len(CONSTANT_DEFINITION.findall(texts["TextId.java"])),
            EXPECTED_TEXT_CONSTANTS,
        ),
        "InkCodes constants": (
            len(CONSTANT_DEFINITION.findall(texts["InkCodes.java"])),
            EXPECTED_INK_CONSTANTS,
        ),
    }
    for label, (actual, expected) in denominators.items():
        if actual != expected:
            failures.append(f"{label} {actual} != reviewed denominator {expected}")

    interpreter = texts["InkInterpreter.java"]
    if "case InkEngine." in interpreter or re.search(r"^\s*case\s+-?\d+\s*:", interpreter, re.M):
        failures.append("InkInterpreter command dispatch retains a raw/cross-domain case")
    return failures


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()

    if arguments.self_test:
        with tempfile.TemporaryDirectory(prefix="orphan-semantic-constant-self-test-") as raw:
            copied = Path(raw) / "defpackage"
            shutil.copytree(SOURCE_ROOT, copied)
            application = copied / "Application.java"
            source = application.read_text(encoding="utf-8")
            marker = "    static boolean loading() {\n"
            if marker not in source:
                raise SystemExit("self-test injection point disappeared")
            application.write_text(
                source.replace(
                    marker,
                    marker + "        String injected = Application.getString(154);\n",
                    1,
                ),
                encoding="utf-8",
                newline="",
            )
            failures = problems(copied)
        if not any("text lookup is not a TextId: 154" in item for item in failures):
            raise SystemExit("self-test failed: raw text ID was accepted")
        print("self-test OK: injected raw text ID was rejected")
        return 0

    failures = problems()
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        return 1
    print(
        "semantic constants OK: "
        f"{EXPECTED_TEXT_ID_USES} TextId, {EXPECTED_COMMAND_USES} command, and "
        f"{EXPECTED_EVENT_USES} event consumers; zero raw/cross-domain IDs"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
