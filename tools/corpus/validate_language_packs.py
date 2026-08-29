#!/usr/bin/env python3
"""Validate integrated language packs against all content-proven corpus locales."""

from __future__ import annotations

import argparse
import hashlib
import io
import sys
import tomllib
import zipfile

import corpus

ROOT = corpus.REPO
LEDGER = ROOT / "java" / "reconstruction" / "language-packs.toml"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def ui_records(data: bytes) -> list[bytes]:
    last = max(data.rfind(b"\r"), data.rfind(b"\n"))
    if last < 0:
        return []
    records: list[bytes] = []
    current = bytearray()
    for byte in data[: last + 1]:
        if byte in (10, 13):
            if current:
                records.append(bytes(current))
                current.clear()
        else:
            current.append(byte)
    return records


def narrative_records(data: bytes) -> tuple[list[bytes], bytes]:
    if len(data) < 2:
        raise ValueError("truncated narrative count")
    count = int.from_bytes(data[:2], "big")
    position = 2
    records = []
    for _ in range(count):
        if position + 2 > len(data):
            raise ValueError("truncated narrative length")
        size = int.from_bytes(data[position : position + 2], "big")
        position += 2
        end = position + size
        if end > len(data):
            raise ValueError("truncated narrative record")
        records.append(data[position:end])
        position = end
    return records, data[position:]


def baseline_jar(payload_sha256: str) -> bytes:
    matches = [build.payload for build in corpus.builds() if build.sha256 == payload_sha256]
    if len(matches) != 1:
        raise ValueError("language ledger baseline_sha256 does not resolve exactly once")
    return matches[0]


def validate(*, mutate: bool) -> list[str]:
    ledger = tomllib.loads(LEDGER.read_text(encoding="utf-8"))
    errors: list[str] = []
    entries = ledger.get("language", [])
    codes = [entry.get("code") for entry in entries]
    if len(codes) != len(set(codes)):
        errors.append("language codes are not unique")
    corpus_codes = {
        code
        for build in corpus.builds()
        for code in corpus.locale_members(build.payload)
    }
    if set(codes) != corpus_codes:
        errors.append(
            f"ledger locale set {sorted(codes)} differs from content-proven corpus set "
            f"{sorted(corpus_codes)}"
        )
    try:
        payload = baseline_jar(ledger["baseline_sha256"])
    except (KeyError, ValueError) as error:
        return [str(error)]
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        for index, entry in enumerate(entries):
            label = f"language {entry.get('code')!r}"
            try:
                ui = archive.read(entry["ui_member"])
                narrative = archive.read(entry["narrative_member"])
            except (KeyError, RuntimeError) as error:
                errors.append(f"{label}: missing baseline member: {error}")
                continue
            if mutate and index == 0:
                ui = bytes([ui[0] ^ 1]) + ui[1:]
            if sha256(ui) != entry.get("ui_sha256"):
                errors.append(f"{label}: UI content hash changed")
            if sha256(narrative) != entry.get("narrative_sha256"):
                errors.append(f"{label}: narrative content hash changed")
            ui_count = len(ui_records(ui))
            if ui_count != ledger.get("ui_slot_count"):
                errors.append(f"{label}: UI has {ui_count} slots")
            try:
                records, trailing = narrative_records(narrative)
            except ValueError as error:
                errors.append(f"{label}: {error}")
                continue
            if len(records) != ledger.get("narrative_slot_count"):
                errors.append(f"{label}: narrative has {len(records)} slots")
            if trailing not in (b"", b"\n"):
                errors.append(f"{label}: unexpected trailing narrative bytes")
    if "zh" not in ledger.get("rejected_claims", {}):
        errors.append("the contradicted ZH filename claim is not recorded")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    errors = validate(mutate=arguments.self_test)
    if arguments.self_test:
        if errors != ["language 'de': UI content hash changed"]:
            print(f"SELF-TEST FAILED: expected one hash error, found {errors}")
            return 3
        print("self-test OK: a one-byte selected language mutation was rejected (R3)")
        return 0
    if errors:
        print("language-pack validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    print("language packs OK: 6 content-proven locales, 185 UI + 714 narrative slots each")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
