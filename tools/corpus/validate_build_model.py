#!/usr/bin/env python3
"""Validate the reviewed all-build Java lineage and content model.

The mechanical provenance rows in ``builds.toml`` identify bytes.  This gate
adds the part needed by the Java reconstruction: every JAR is assigned to one
exact bytecode family, one source-layout lineage, one content profile, and one
honest preservation status.  All assignments are recomputed from verified JAR
members and classfiles; collection filenames never decide language or code
identity.
"""

from __future__ import annotations

import argparse
import hashlib
import sys
import tomllib
from collections import Counter
from pathlib import Path

import classify
import corpus


ROOT = corpus.REPO
MANIFEST = ROOT / "java" / "reconstruction" / "builds.toml"
MODEL = ROOT / "java" / "reconstruction" / "build-model.toml"
RECONSTRUCTION = ROOT / "java" / "reconstruction"

REVIEW_STATUSES = {
    "fingerprinted",
    "third-party-branded",
    "canonical-baseline",
    "semantic-reference",
}
THIRD_PARTY_MARKERS = (
    "binpda",
    "6x.to",
    "polick",
    "xsmart",
    "lavita",
    "cyberboy",
    "d@nilych",
    "stek12",
)
CONTENT_PROFILE_BY_CODES = {
    (): "embedded-or-monolingual",
    ("en",): "english-only",
    ("en", "fr"): "english-french",
    ("en", "es", "pt"): "english-spanish-portuguese",
    ("de", "en", "es", "fr", "it", "pt"): "six-language",
}


def set_sha256(values: list[str]) -> str:
    payload = "".join(f"{value}\n" for value in sorted(values))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def source_lineage(build: classify.BuildAnalysis) -> str:
    names = {item.internal_name for item in build.game_classes}
    if "sh/M" in names:
        return "split-runtime"
    if {"Ext", "ExtBase", "M", "MyCanvas", "s", "txt_consts"} <= names:
        return "source-named"
    return "obfuscated-engine"


def content_profile(build: classify.BuildAnalysis) -> str:
    try:
        return CONTENT_PROFILE_BY_CODES[build.locale_codes]
    except KeyError as error:
        raise ValueError(
            f"{build.build_id}: unreviewed locale-member set {build.locale_codes}"
        ) from error


def third_party_branded(row: dict) -> bool:
    manifest_branding = " ".join(
        str(row.get(key, "")) for key in ("midlet_name", "vendor", "repack_tag")
    ).casefold()
    return any(marker in manifest_branding for marker in THIRD_PARTY_MARKERS)


def expected_review_status(row: dict, baseline: str, reference: str) -> str:
    if row["id"] == baseline:
        return "canonical-baseline"
    if row["id"] == reference:
        return "semantic-reference"
    if third_party_branded(row):
        return "third-party-branded"
    return "fingerprinted"


def family_rows(
    analyses: list[classify.BuildAnalysis],
) -> tuple[dict[str, str], list[dict[str, object]]]:
    by_id = {build.build_id: build for build in analyses}
    groups = classify.code_families(analyses)
    family_of: dict[str, str] = {}
    rows: list[dict[str, object]] = []
    for ordinal, (representative, members) in enumerate(sorted(groups.items()), 1):
        family_id = f"f{ordinal:02d}"
        for build_id in members:
            family_of[build_id] = family_id
        build = by_id[representative]
        rows.append(
            {
                "id": family_id,
                "representative": representative,
                "representative_sha256": build.sha256,
                "source_lineage": source_lineage(build),
                "member_count": len(members),
                "members_sha256": set_sha256(members),
                "game_class_count": len(build.game_classes),
                "game_method_count": build.game_method_count(),
                "game_code_bytes": build.game_code_size(),
                "class_shape_set_sha256": set_sha256(
                    [item.shape_sha256 for item in build.game_classes]
                ),
            }
        )
    return family_of, rows


def validate(*, mutate: bool = False) -> list[str]:
    manifest_text = MANIFEST.read_text(encoding="utf-8")
    manifest = tomllib.loads(manifest_text)
    model = tomllib.loads(MODEL.read_text(encoding="utf-8"))
    errors: list[str] = []

    if model.get("schema_version") != 1:
        errors.append("build-model.toml: unsupported schema_version")
    if "TODO Phase 1" in manifest_text:
        errors.append("builds.toml still contains unresolved Phase-1 judgments")

    jar_rows = [
        row
        for section in ("payload", "archived")
        for row in manifest.get(section, [])
        if row.get("kind") == "jar"
    ]
    ids = [row.get("id") for row in jar_rows]
    if len(ids) != len(set(ids)):
        errors.append("builds.toml: JAR ids are missing or duplicated")
    row_by_id = {row["id"]: row for row in jar_rows}

    analyses = classify.analyze()
    analysis_by_id = {build.build_id: build for build in analyses}
    if set(row_by_id) != set(analysis_by_id):
        errors.append("builds.toml JAR coverage differs from the verified corpus")
        return errors

    family_of, expected_families = family_rows(analyses)
    if mutate:
        first_id = sorted(row_by_id)[0]
        row_by_id[first_id] = dict(row_by_id[first_id], code_family="f00")

    baseline = manifest.get("baseline")
    reference = manifest.get("naming_reference")
    for build_id, row in sorted(row_by_id.items()):
        build = analysis_by_id[build_id]
        if row.get("sha256") != build.sha256 or row.get("bytes") != build.size:
            errors.append(f"{build_id}: provenance differs from verified payload")
        if row.get("class_count") != len(build.classes):
            errors.append(f"{build_id}: class_count differs from classfile catalog")
        if "official" in row:
            errors.append(
                f"{build_id}: unsupported official claim; use evidence-scoped review_status"
            )
        status = row.get("review_status")
        if status not in REVIEW_STATUSES:
            errors.append(f"{build_id}: invalid review_status {status!r}")
        expected_status = expected_review_status(row, baseline, reference)
        if status != expected_status:
            errors.append(
                f"{build_id}: review_status {status!r}, expected {expected_status!r}"
            )
        expected_annotations = {
            "code_family": family_of[build_id],
            "source_lineage": source_lineage(build),
            "content_profile": content_profile(build),
        }
        for key, expected in expected_annotations.items():
            if row.get(key) != expected:
                errors.append(
                    f"{build_id}: {key} {row.get(key)!r}, expected {expected!r}"
                )

    if len(analyses) != model.get("expected_jar_payloads"):
        errors.append("build-model.toml: expected_jar_payloads changed")
    if len(expected_families) != model.get("expected_exact_code_families"):
        errors.append("build-model.toml: exact family count changed")
    if sum("sh/M" in {c.internal_name for c in build.game_classes} for build in analyses) != model.get(
        "expected_packaged_midlets"
    ):
        errors.append("build-model.toml: packaged sh/M MIDlet count changed")

    declared_lineages = {entry.get("id"): entry for entry in model.get("source_lineages", [])}
    actual_lineage_members: dict[str, list[str]] = {}
    for build in analyses:
        actual_lineage_members.setdefault(source_lineage(build), []).append(build.build_id)
    if set(declared_lineages) != set(actual_lineage_members):
        errors.append("build-model.toml: source-lineage set changed")
    else:
        for lineage_id, members in sorted(actual_lineage_members.items()):
            entry = declared_lineages[lineage_id]
            if entry.get("expected_builds") != len(members):
                errors.append(f"{lineage_id}: expected_builds changed")
            if entry.get("build_ids_sha256") != set_sha256(members):
                errors.append(f"{lineage_id}: build membership changed")
            if not entry.get("evidence"):
                errors.append(f"{lineage_id}: missing lineage evidence")
            layers = entry.get("source_layers", [])
            if not layers or layers[0] != "common":
                errors.append(f"{lineage_id}: source layers must begin with common")
            for layer in layers:
                if not (RECONSTRUCTION / "lineages" / layer).is_dir():
                    errors.append(f"{lineage_id}: missing source-layer notes {layer}")

    declared_profiles = {entry.get("id"): entry for entry in model.get("content_profiles", [])}
    actual_profile_members: dict[str, list[str]] = {}
    codes_by_profile: dict[str, tuple[str, ...]] = {}
    for build in analyses:
        profile = content_profile(build)
        actual_profile_members.setdefault(profile, []).append(build.build_id)
        codes_by_profile[profile] = build.locale_codes
    if set(declared_profiles) != set(actual_profile_members):
        errors.append("build-model.toml: content-profile set changed")
    else:
        for profile_id, members in sorted(actual_profile_members.items()):
            entry = declared_profiles[profile_id]
            if tuple(entry.get("locale_codes", [])) != codes_by_profile[profile_id]:
                errors.append(f"{profile_id}: locale-code set changed")
            if entry.get("expected_builds") != len(members):
                errors.append(f"{profile_id}: expected_builds changed")
            if entry.get("build_ids_sha256") != set_sha256(members):
                errors.append(f"{profile_id}: build membership changed")
            if not entry.get("evidence"):
                errors.append(f"{profile_id}: missing content evidence")

    declared_families = model.get("code_families", [])
    if declared_families != expected_families:
        errors.append("build-model.toml: exact code-family ledger is stale")

    status_counts = Counter(row.get("review_status") for row in jar_rows)
    expected_status_counts = model.get("review_status_counts", {})
    if dict(sorted(status_counts.items())) != dict(sorted(expected_status_counts.items())):
        errors.append("build-model.toml: review-status counts changed")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    errors = validate(mutate=arguments.self_test)
    if arguments.self_test:
        analyses = classify.analyze()
        family_of, _ = family_rows(analyses)
        first_id = min(build.build_id for build in analyses)
        expected = [f"{first_id}: code_family 'f00', expected {family_of[first_id]!r}"]
        if errors != expected:
            print(f"SELF-TEST FAILED: expected {expected}, found {errors}")
            return 3
        print("self-test OK: a changed build-family assignment was rejected (R3)")
        return 0
    if errors:
        print("reviewed build-model validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    model = tomllib.loads(MODEL.read_text(encoding="utf-8"))
    print(
        f"reviewed build model OK: {model['expected_jar_payloads']} JARs, "
        f"{model['expected_exact_code_families']} exact families, "
        f"{len(model['source_lineages'])} source lineages, and "
        f"{len(model['content_profiles'])} content profiles"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
