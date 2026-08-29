#!/usr/bin/env python3
"""Validate complete field/method variation between the two named builds."""

from __future__ import annotations

import argparse
import hashlib
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "corpus"))

import classify  # noqa: E402
import corpus  # noqa: E402


LEDGER = ROOT / "java" / "reconstruction" / "variants" / "source-named.toml"
CLASSIFICATIONS = {
    "vibration-policy",
    "rendering-policy",
    "input-timing-policy",
    "repaint-policy",
    "diagnostic-policy",
    "lifecycle-policy",
    "device-ui-policy",
}


def digest_lines(values: list[str]) -> str:
    return hashlib.sha256("".join(f"{value}\n" for value in sorted(values)).encode()).hexdigest()


def calls_digest(calls: list[str]) -> str:
    return digest_lines(calls)


def validate(*, mutate: bool = False) -> list[str]:
    ledger = tomllib.loads(LEDGER.read_text(encoding="utf-8"))
    errors: list[str] = []
    if ledger.get("schema_version") != 1:
        errors.append("unsupported source-named variant schema")

    analyses = {build.build_id: build for build in classify.analyze()}
    baseline_id = ledger.get("baseline_build")
    reference_id = ledger.get("reference_build")
    if baseline_id not in analyses or reference_id not in analyses:
        return ["variant build identity is absent from the verified corpus"]
    baseline = analyses[baseline_id]
    reference = analyses[reference_id]
    if baseline.sha256 != ledger.get("baseline_sha256"):
        errors.append("baseline payload hash changed")
    if reference.sha256 != ledger.get("reference_sha256"):
        errors.append("reference payload hash changed")

    baseline_classes = {item.internal_name: item for item in baseline.game_classes}
    reference_classes = {item.internal_name: item for item in reference.game_classes}
    if set(baseline_classes) != set(reference_classes):
        errors.append("source-named class sets differ")
        return errors

    class_entries = {entry.get("owner"): entry for entry in ledger.get("classes", [])}
    if set(class_entries) != set(baseline_classes):
        errors.append("variant ledger class coverage differs from source-named builds")
        return errors

    if mutate:
        first_owner = min(class_entries)
        class_entries[first_owner] = dict(class_entries[first_owner])
        class_entries[first_owner]["common_methods_sha256"] = "0" * 64

    total_common = 0
    total_varying = 0
    total_reference_only_methods = 0
    total_reference_only_fields = 0
    for owner in sorted(baseline_classes):
        entry = class_entries[owner]
        left = baseline_classes[owner]
        right = reference_classes[owner]
        if not entry.get("canonical_owner") or not entry.get("evidence"):
            errors.append(f"{owner}: missing semantic owner/evidence")

        left_fields = {(field.name, field.descriptor): field for field in left.fields}
        right_fields = {(field.name, field.descriptor): field for field in right.fields}
        if set(left_fields) - set(right_fields):
            errors.append(f"{owner}: baseline fields are absent from reference")
        reference_only_fields = [
            {
                "name": name,
                "descriptor": descriptor,
                "access_flags": right_fields[(name, descriptor)].access_flags,
            }
            for name, descriptor in sorted(set(right_fields) - set(left_fields))
        ]
        if entry.get("baseline_field_count") != len(left_fields):
            errors.append(f"{owner}: baseline_field_count changed")
        if entry.get("reference_field_count") != len(right_fields):
            errors.append(f"{owner}: reference_field_count changed")
        if entry.get("reference_only_fields", []) != reference_only_fields:
            errors.append(f"{owner}: reference-only field set changed")
        total_reference_only_fields += len(reference_only_fields)

        left_methods = {(method.name, method.descriptor): method for method in left.methods}
        right_methods = {(method.name, method.descriptor): method for method in right.methods}
        if set(left_methods) - set(right_methods):
            errors.append(f"{owner}: baseline methods are absent from reference")
        common_exact = [
            (name, descriptor, left_methods[(name, descriptor)].shape_sha256)
            for name, descriptor in sorted(set(left_methods) & set(right_methods))
            if left_methods[(name, descriptor)].shape_sha256
            == right_methods[(name, descriptor)].shape_sha256
        ]
        common_digest = digest_lines(
            [f"{name}\t{descriptor}\t{shape}" for name, descriptor, shape in common_exact]
        )
        if entry.get("baseline_method_count") != len(left_methods):
            errors.append(f"{owner}: baseline_method_count changed")
        if entry.get("reference_method_count") != len(right_methods):
            errors.append(f"{owner}: reference_method_count changed")
        if entry.get("common_exact_methods") != len(common_exact):
            errors.append(f"{owner}: common_exact_methods changed")
        if entry.get("common_methods_sha256") != common_digest:
            errors.append(f"{owner}: common exact method digest changed")
        total_common += len(common_exact)

        actual_variants = {
            key
            for key in set(left_methods) | set(right_methods)
            if key not in {(name, descriptor) for name, descriptor, _shape in common_exact}
        }
        variants = entry.get("variants", [])
        declared_keys = [(item.get("name"), item.get("descriptor")) for item in variants]
        if len(declared_keys) != len(set(declared_keys)):
            errors.append(f"{owner}: duplicate variant signature")
        if set(declared_keys) != actual_variants:
            errors.append(f"{owner}: variant signature coverage changed")
            continue
        for variant in variants:
            key = (variant["name"], variant["descriptor"])
            base_method = left_methods.get(key)
            ref_method = right_methods.get(key)
            expected_presence = "both" if base_method is not None else "reference-only"
            if variant.get("presence") != expected_presence:
                errors.append(f"{owner}.{key}: presence changed")
            if variant.get("classification") not in CLASSIFICATIONS:
                errors.append(f"{owner}.{key}: invalid classification")
            if not variant.get("policy") or not variant.get("evidence"):
                errors.append(f"{owner}.{key}: missing reviewed policy evidence")
            facts = {
                "baseline_shape_sha256": base_method.shape_sha256 if base_method else None,
                "reference_shape_sha256": ref_method.shape_sha256 if ref_method else None,
                "baseline_code_bytes": base_method.code_size if base_method else None,
                "reference_code_bytes": ref_method.code_size if ref_method else None,
                "baseline_calls_sha256": calls_digest(base_method.calls) if base_method else None,
                "reference_calls_sha256": calls_digest(ref_method.calls) if ref_method else None,
            }
            for field, expected in facts.items():
                if expected is None:
                    if field in variant:
                        errors.append(f"{owner}.{key}: unexpected {field}")
                elif variant.get(field) != expected:
                    errors.append(f"{owner}.{key}: {field} changed")
            total_varying += 1
            total_reference_only_methods += base_method is None

    expected_totals = {
        "common_exact_methods": total_common,
        "varying_signatures": total_varying,
        "reference_only_methods": total_reference_only_methods,
        "reference_only_fields": total_reference_only_fields,
    }
    if ledger.get("totals") != expected_totals:
        errors.append(f"variant ledger totals changed: expected {expected_totals}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    errors = validate(mutate=arguments.self_test)
    if arguments.self_test:
        expected = ["Cheat: common exact method digest changed"]
        if errors != expected:
            print(f"SELF-TEST FAILED: expected {expected}, found {errors}")
            return 3
        print("self-test OK: a changed common-method digest was rejected (R3)")
        return 0
    if errors:
        print("source-named variant validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    ledger = tomllib.loads(LEDGER.read_text(encoding="utf-8"))
    totals = ledger["totals"]
    print(
        "source-named variants OK: "
        f"{totals['common_exact_methods']} exact-common methods, "
        f"{totals['varying_signatures']} reviewed variants, "
        f"{totals['reference_only_methods']} reference-only methods, and "
        f"{totals['reference_only_fields']} reference-only fields"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
