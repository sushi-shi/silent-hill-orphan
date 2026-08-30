set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

default:
    @just --list

# --- Fresh clone -------------------------------------------------------------

# Fresh clone to verified: materialize the corpus, then reconcile it. The
# resource location is passed explicitly; it is never baked into the repo (R1).
bootstrap resources:
    nix run .#fetch-resources -- {{quote(resources)}}
    just originals-verify
    just classify
    just catalog

# Verify the materialized _originals against builds.toml's sha256/bytes table.
originals-verify:
    python3 tools/originals/verify.py

# Prove the originals-verify gate can fail (playbook R3). Must exit 0.
originals-verify-canfail:
    python3 tools/originals/verify.py --self-test

# Regenerate builds.toml provenance from a resources dir (mechanical facts only;
# the judgment calls stay flagged for Phase 1 — see the file header).
gen-builds resources match:
    python3 tools/originals/gen_builds.py \
        --resources {{quote(resources)}} --match {{quote(match)}} \
        --slug "$(python3 -c 'import tomllib;print(tomllib.load(open("game.toml","rb"))["slug"])')" \
        --title "$(python3 -c 'import tomllib;print(tomllib.load(open("game.toml","rb"))["title"])')" \
        --out java/reconstruction/builds.toml

# --- Corpus evidence ---------------------------------------------------------

# Fingerprint all classes/methods in every unique JAR directly from bytecode.
classify:
    python3 tools/corpus/classify.py

# Prove class identities and deterministic output notice a one-byte mutation.
classify-canfail:
    python3 tools/corpus/classify.py --self-test

# Content-address every non-class resource across all unique JARs.
catalog:
    python3 tools/corpus/catalog_resources.py

# Prove resource deduplication notices and splits a one-byte mutation.
catalog-canfail:
    python3 tools/corpus/catalog_resources.py --self-test

# Validate selected packs against every locale code actually in the corpus.
language-packs:
    python3 tools/corpus/validate_language_packs.py

language-packs-canfail:
    python3 tools/corpus/validate_language_packs.py --self-test

# Recompute every JAR's exact bytecode family, source lineage, content profile,
# and evidence-scoped review status from the verified payloads.
java-builds-check:
    python3 tools/corpus/validate_build_model.py

java-builds-canfail:
    python3 tools/corpus/validate_build_model.py --self-test

# Verify the no_std codec crate does not accidentally acquire a std feature.
formats-no-std:
    cargo check -p orphan-formats --no-default-features

# Only serialization/content codecs are deliberately constrained to no_std.
codec-no-std:
    cargo check -p orphan-formats --no-default-features
    cargo check -p orphan-content --no-default-features

# --- Java recovery -----------------------------------------------------------

# Regenerate JADX and CFR evidence for the reviewed baseline (or an explicit id).
decompile build="":
    python3 tools/java/decompile.py {{quote(build)}}

# Verify the generated source inventories still match their recorded hashes.
decompile-check build="":
    python3 tools/java/decompile.py {{quote(build)}} --check

# Type-check the complete named application against Java ME declarations.
java-typecheck:
    python3 tools/java/compile_named_java.py

# Build a deterministic application-only JAR; stubs and assets are excluded.
java-build:
    python3 tools/java/compile_named_java.py --jar target/java/orphan-java.jar

java-build-check:
    python3 tools/java/compile_named_java.py --check-reproducible

java-build-canfail:
    python3 tools/java/compile_named_java.py --self-test

# Compare every canonical class/field/method signature with the original JAR.
java-named-check:
    python3 tools/java/validate_named_java.py

# Prove the surface gate rejects one mutated original member signature.
java-named-canfail:
    python3 tools/java/validate_named_java.py --self-test

# Regenerate and compare the complete reviewed original-to-semantic Tiny map.
java-mappings-check:
    python3 tools/java/generate_canonical_mappings.py --check

java-mappings-canfail:
    python3 tools/java/generate_canonical_mappings.py --self-test

# Bind the author editor-backup sources to exact files in the verified corpus.
java-source-backups-check:
    python3 tools/java/validate_source_backups.py

java-source-backups-canfail:
    python3 tools/java/validate_source_backups.py --self-test

# Cover every field and method delta between the two source-named builds.
java-method-variants-check:
    python3 tools/java/validate_source_named_variants.py

java-method-variants-canfail:
    python3 tools/java/validate_source_named_variants.py --self-test

# Reject JADX/CFR synthetic identifier families outside real loop/catch roles.
java-identifiers-check:
    python3 tools/java/validate_java_identifiers.py

java-identifiers-canfail:
    python3 tools/java/validate_java_identifiers.py --self-test

# Compare every mapped method's arithmetic/conversion opcode sequence.
java-numeric-shape-check:
    python3 tools/java/validate_java_numeric_shape.py

java-numeric-shape-canfail:
    python3 tools/java/validate_java_numeric_shape.py --self-test

# Require an evidence-backed semantic domain for every raw executable literal.
java-literals-check:
    python3 tools/java/validate_java_literals.py

java-literals-canfail:
    python3 tools/java/validate_java_literals.py --self-test

# Prevent same-valued constants from an unrelated integer domain from being
# accepted as readable source by the compiler.
java-semantic-constants-check:
    python3 tools/java/validate_java_semantic_constants.py

java-semantic-constants-canfail:
    python3 tools/java/validate_java_semantic_constants.py --self-test

# Gate the exact per-class semantic-member denominator (excluding <init>/<clinit>).
java-coverage-check:
    python3 tools/java/report_named_java_coverage.py --summary-only

java-coverage-canfail:
    python3 tools/java/report_named_java_coverage.py --self-test

# Execute canonical Java and both verified source-named original JARs on the
# same edge-case stream. This is the Java-only half of the Rust differential.
java-original-oracle:
    python3 tools/transliteration/compare_pure_oracle.py --java-only

java-original-oracle-canfail:
    python3 tools/transliteration/compare_pure_oracle.py --java-only --self-test

java-test:
    just java-original-oracle

java-check:
    just java-builds-check
    just java-builds-canfail
    just java-typecheck
    just java-build-check
    just java-build-canfail
    just java-named-check
    just java-named-canfail
    just java-mappings-check
    just java-mappings-canfail
    just java-source-backups-check
    just java-source-backups-canfail
    just java-method-variants-check
    just java-method-variants-canfail
    just java-identifiers-check
    just java-identifiers-canfail
    just java-numeric-shape-check
    just java-numeric-shape-canfail
    just java-literals-check
    just java-literals-canfail
    just java-semantic-constants-check
    just java-semantic-constants-canfail
    just java-coverage-check
    just java-coverage-canfail
    just java-ast-check
    just java-ast-canfail
    just java-original-oracle
    just java-original-oracle-canfail

# Hash-lock the complete javac tree and node denominator to baseline bytecode.
java-ast-check:
    python3 tools/transliteration/validate_ast_authority.py

# Prove that changing one canonical AST item invalidates the authority digest.
java-ast-canfail:
    python3 tools/transliteration/validate_ast_authority.py --self-test

# Emit exact syn AST evidence for production transliteration sources.
rust-ast-dump:
    cargo run -q -p orphan-ast-audit -- --production-only \
        $(find transliteration/crates/orphan-game-xlat/src -name '*.rs' -type f | sort)

# Execute recovered JARs, canonical Java, and Rust for every admitted method.
pure-oracle:
    python3 tools/transliteration/compare_pure_oracle.py

# Prove the differential catches one changed Rust result.
pure-oracle-canfail:
    python3 tools/transliteration/compare_pure_oracle.py --self-test

# Validate bytecode + javac + syn evidence for every reviewed Rust body.
method-audit-check:
    python3 tools/transliteration/validate_method_audit.py

method-audit-canfail:
    python3 tools/transliteration/validate_method_audit.py --self-test

# Prove that every javac/syn node must have exactly one semantic owner.
method-crosswalk-canfail:
    python3 tools/transliteration/validate_method_audit.py --self-test-crosswalk

# Run the reusable schema-2 verifier over live Silent Hill bytecode/javac/syn
# evidence. The game-specific declaration and reverse-inventory checks remain in
# method-audit-check.
crosswalk-check:
    python3 tools/transliteration/validate_method_audit.py --crosswalk-coverage
    python3 tools/ast/validate_crosswalk.py \
        tools/ast/fixtures/paint_radio_row.crosswalk.toml \
        --evidence tools/ast/fixtures/paint_radio_row.evidence.toml --strict
    python3 tools/ast/validate_crosswalk.py \
        tools/ast/fixtures/temporal_interleave.crosswalk.toml \
        --evidence tools/ast/fixtures/temporal_interleave.evidence.toml --strict

# Report the reusable schema-2 burn-down over the live Silent Hill evidence.
crosswalk-coverage:
    python3 tools/transliteration/validate_method_audit.py --crosswalk-coverage

# Prove the generic per-node gate rejects missing decisions, coarse blankets,
# digest drift, literal/index substitutions, and unexplained A-B-A-B ownership.
crosswalk-canfail:
    python3 tools/ast/validate_crosswalk.py --self-test

# Prove the fixture bug rows go red as recorded: coarse blanket, div-vs-sm() call
# (operator parity), entity_row[13]-vs-[10] (literal/index parity), wrong/stale
# literal-delta locks, and A-B-A-B ownership whose exact interleave owner group
# was removed.
crosswalk-fixture-canfail:
    python3 -m unittest \
        tools.tests.test_crosswalk_validator.CrosswalkValidatorTests.test_coarse_blanket_is_rejected \
        tools.tests.test_crosswalk_validator.CrosswalkValidatorTests.test_operator_parity_catches_div_vs_call \
        tools.tests.test_crosswalk_validator.CrosswalkValidatorTests.test_literal_index_parity_catches_wrong_column \
        tools.tests.test_crosswalk_validator.CrosswalkValidatorTests.test_literal_delta_must_match_the_exact_multiset \
        tools.tests.test_crosswalk_validator.CrosswalkValidatorTests.test_stale_literal_delta_is_rejected \
        tools.tests.test_crosswalk_validator.CrosswalkValidatorTests.test_temporal_interleave_waiver_removal_is_rejected

# Prove that Java/Rust state declarations use the same complete node ownership
# rule as executable bodies.
declaration-crosswalk-canfail:
    python3 tools/transliteration/validate_method_audit.py --self-test-declaration-crosswalk

# Prove that constants/types/state cannot enter the strict game crate before an
# explicit Java-field ownership review exists for them.
method-ownership-canfail:
    python3 tools/transliteration/validate_method_audit.py --self-test-ownership

# --- Test batteries ----------------------------------------------------------

# Hash every declared gate input and execute only groups whose exact current
# fingerprint has not already passed. This does not depend on Git cleanliness.
check-affected:
    python3 tools/gates/check_changed.py

# Show which hash-invalidated groups and commands would run.
check-affected-dry:
    python3 tools/gates/check_changed.py --dry-run

# Continuously rerun newly affected groups while transliterating.
watch-affected interval="0.5":
    python3 tools/gates/check_changed.py --watch --interval {{quote(interval)}}

test:
    if [ -d tools/tests ]; then python3 -m unittest discover -s tools/tests; fi
    if [ -f Cargo.toml ]; then cargo test --workspace; fi

# Every gate the project has today. Grows as phases land; every gate cited here
# must exist and be proven able to fail (playbook R3, R14).
check:
    just originals-verify
    just originals-verify-canfail
    just classify
    just classify-canfail
    just catalog
    just catalog-canfail
    just language-packs
    just language-packs-canfail
    just codec-no-std
    just java-check
    just pure-oracle
    just pure-oracle-canfail
    just method-audit-check
    just method-audit-canfail
    just method-crosswalk-canfail
    just crosswalk-canfail
    just crosswalk-fixture-canfail
    just declaration-crosswalk-canfail
    just method-ownership-canfail
    if [ -d tools/tests ]; then python3 -m unittest discover -s tools/tests; fi
    if [ -f Cargo.toml ]; then cargo fmt --all --check; fi
    if [ -f Cargo.toml ]; then cargo clippy --workspace --all-targets -- -D warnings; fi
    if [ -f Cargo.toml ]; then cargo test --workspace; fi
    python3 tools/gates/check_changed.py --record-all
