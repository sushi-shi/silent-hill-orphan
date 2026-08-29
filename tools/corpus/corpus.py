#!/usr/bin/env python3
"""Read every verified Silent Hill: Orphan payload by content identity.

The tracked provenance ledger is the authority; filenames and archive member
names are only locations.  This module deliberately includes rows still marked
for Phase-1 review so classification can be the evidence used to archive them.
"""

from __future__ import annotations

import hashlib
import io
import zipfile
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover
    import tomli as tomllib  # type: ignore

REPO = Path(__file__).resolve().parents[2]
BUILDS_TOML = REPO / "java" / "reconstruction" / "builds.toml"
ORIGINALS = REPO / "_originals"


class CorpusError(RuntimeError):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


@dataclass(frozen=True)
class Build:
    build_id: str
    sha256: str
    payload: bytes
    manifest_version: str
    declared_language: str
    official: object
    archived: bool
    collected_as: tuple[str, ...]

    @property
    def size(self) -> int:
        return len(self.payload)


def load_manifest() -> dict:
    with BUILDS_TOML.open("rb") as handle:
        return tomllib.load(handle)


@lru_cache(maxsize=1)
def _payload_index() -> dict[str, bytes]:
    if not ORIGINALS.is_dir() or ORIGINALS.is_symlink():
        raise CorpusError(
            "_originals must be a materialized real directory; run "
            "`just bootstrap <silent-hill-orphan-resources>`"
        )
    found: dict[str, bytes] = {}
    for path in sorted(p for p in ORIGINALS.iterdir() if p.is_file()):
        data = path.read_bytes()
        found.setdefault(sha256(data), data)
        if not zipfile.is_zipfile(io.BytesIO(data)):
            continue
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            for member in sorted(n for n in archive.namelist() if not n.endswith("/")):
                nested = archive.read(member)
                found.setdefault(sha256(nested), nested)
    return found


def builds(*, include_archived: bool = True) -> list[Build]:
    manifest = load_manifest()
    rows = [(entry, False) for entry in manifest.get("payload", [])]
    if include_archived:
        rows.extend((entry, True) for entry in manifest.get("archived", []))
    index = _payload_index()
    result: list[Build] = []
    for entry, archived in rows:
        if entry.get("kind") != "jar":
            continue
        want = entry["sha256"]
        payload = index.get(want)
        if payload is None:
            raise CorpusError(
                f"payload {entry['id']} ({want[:12]}) is absent from _originals"
            )
        if len(payload) != entry["bytes"]:
            raise CorpusError(f"payload {entry['id']} has an unexpected byte length")
        result.append(
            Build(
                build_id=entry["id"],
                sha256=want,
                payload=payload,
                manifest_version=entry.get("midlet_version", ""),
                declared_language=entry.get("declared_language", "unknown"),
                official=entry.get("official", "unreviewed"),
                archived=archived,
                collected_as=tuple(entry.get("collected_as", [])),
            )
        )
    if not result:
        raise CorpusError("no JAR payloads are recorded in builds.toml")
    return result


def jar_members(payload: bytes) -> list[tuple[str, bytes]]:
    with zipfile.ZipFile(io.BytesIO(payload)) as jar:
        return [
            (name, jar.read(name))
            for name in sorted(n for n in jar.namelist() if not n.endswith("/"))
        ]


def locale_members(payload: bytes) -> dict[str, tuple[str, ...]]:
    """Return locale-looking members without claiming their decoded language."""
    result: dict[str, list[str]] = {}
    for name, _ in jar_members(payload):
        lower = name.lower()
        if not (
            (lower.startswith("localization/") and lower.endswith(".properties"))
            or (lower.startswith("sh/lan/") and lower.endswith(".lan"))
        ):
            continue
        code = Path(lower).stem
        result.setdefault(code, []).append(name)
    return {code: tuple(sorted(names)) for code, names in sorted(result.items())}
