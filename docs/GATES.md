# Gates and the can-fail discipline (R3) — Silent Hill: Orphan

> No gate is trusted until it has been shown to go red on an injected defect.

The full discipline (the can-fail rule, the four vacuous-gate shapes, the
independent two-implementation oracle pattern, and the anti-bog protocol) lives
in the j2me home's `docs/GATES.md`. This file is this game's live gate ledger:
every gate the project has today, with its command and its can-fail proof. Add a
row when you add a gate.

## Current gates

| Gate | Command | Can-fail proof |
| --- | --- | --- |
| Originals provenance | `just originals-verify` | `just originals-verify-canfail` (proven RED on a one-byte payload corruption) |
| Class/method fingerprints | `just classify` | `just classify-canfail` (one-byte class perturbation is rejected or changes the deterministic inventory) |
| Resource catalog | `just catalog` | `just catalog-canfail` (one-byte resource perturbation splits content-addressed dedup) |
| Integrated language packs | `just language-packs` | `just language-packs-canfail` (one selected UI-table byte is changed and its content identity is rejected) |
| Serialization codecs are `no_std` | `just codec-no-std` | Compiles formats and content with default features disabled; JVM/MIDP/runtime/transliteration code is intentionally ordinary Rust. |
| Reviewed all-build Java model | `just java-builds-check` | `just java-builds-canfail` (one JAR is assigned to a false exact-code family and rejected) |
| Canonical Java binary surface | `just java-named-check` | `just java-named-canfail` (an in-memory mutation of one original member signature is rejected) |
| Complete semantic mapping | `just java-mappings-check` | `just java-mappings-canfail` (one generated Tiny mapping row is changed and rejected) |
| Author Java backup provenance | `just java-source-backups-check` | `just java-source-backups-canfail` (one recorded backup digest is changed and rejected across the verified corpus) |
| Complete source-named variants | `just java-method-variants-check` | `just java-method-variants-canfail` (one exact-common method digest is changed and rejected) |
| Semantic Java identifiers | `just java-identifiers-check` | `just java-identifiers-canfail` (a synthetic `i2` local is injected into a copied canonical source and rejected by the javac AST inventory) |
| Original/canonical numeric shape | `just java-numeric-shape-check` | `just java-numeric-shape-canfail` (one arithmetic opcode is injected into a copied method inventory and rejected) |
| Reviewed Java literal domains | `just java-literals-check` | `just java-literals-canfail` (an unexplained `2147483646` local is injected into a copied source and rejected; unexplained budget is zero) |
| Semantic text/command/event constants | `just java-semantic-constants-check` | `just java-semantic-constants-canfail` (a raw text-table index is injected and rejected; cross-domain same-value aliases are also forbidden) |
| Exact semantic-member denominator | `just java-coverage-check` | `just java-coverage-canfail` (one explicit member mapping is removed and both per-class and fixed-total checks fail) |
| Reproducible Java application | `just java-build-check` | `just java-build-canfail` (one built-JAR byte is changed and rejected) |
| Canonical javac AST authority | `just java-ast-check` | `just java-ast-canfail` (one complete AST item is mutated and changes the inventory digest) |
| Recovered-JAR/canonical Java behavior | `just java-original-oracle` | `just java-original-oracle-canfail` (one recovered-JAR result is changed; exactly one case diverges) |
| Pure-method Java/Rust differential | `just pure-oracle` | `just pure-oracle-canfail` (both recovered JARs and canonical Java are independent authorities; one Rust result is changed and exactly one case diverges) |
| Three-authority method/declaration audit | `just method-audit-check` | `just method-audit-canfail` changes one reviewed original opcode digest; `just method-crosswalk-canfail` and `just declaration-crosswalk-canfail` each leave one `javac` node without a semantic owner; all are rejected, and the AST-walker unit test requires statement-position boundary macros to be visible (finding O-1) |
| Production Rust ownership scope | `just method-audit-check` | `just method-ownership-canfail` injects an unowned production constant; the reverse `syn` inventory rejects it and separately accounts for every function, value declaration, and owner container. The first 179/1075 Java fields map exhaustively to 146 Rust fields in sixteen hash-locked owner structs, thirty-three typed scalar constants, and one mutable-array initializer template. |

## Content-addressed affected-gate loop

`just check-affected` hashes every input declared for each group in
`tools/gates/gates.toml`, including the group command definition. A fingerprint
is cached only after all commands in that group pass, so a failure is retried on
the next run. `just watch-affected` watches the same surface and reruns groups
after a file create/write/remove. The router does not consult Git and therefore
works during the large uncommitted reconstruction phase.

For the normal method-admission edit set, changes to the strict Rust body select
`xlat-rust`, `differential-oracle`, and `method-audit`; oracle-adapter changes
select the differential; manifest changes select both the method audit and Java
AST authority because the latter reads the former's reviewed-body ratchet.
Every transitive file read by a validator must be declared in that gate's input
surface, even when it is not named on the command line; otherwise a method-only
admission could reuse a stale authority fingerprint. Corpus, language, codec,
and unrelated Java-recovery gates remain clean by hash. This is an
iteration accelerator only: milestone and final completion still use
`just check`, whose last successful step synchronizes every group fingerprint.

## Rules (restated)

1. Every gate ships with a can-fail proof (`--self-test` / an in-test negative
   control), proven RED by a one-unit perturbation you then reverse (never
   `git checkout`).
2. Ban the four vacuous shapes: comparing against a quantity the tool never
   returns; an assertion whose subject can vanish while it holds; a skip that
   reads as a pass; a ratio of a set against itself.
3. Build the quantity you assert on yourself (pixel masks, sample counts) — never
   parse an image/audio tool's stdout numerically.
4. Corpus-dependent tests fail loudly when `_originals` is absent — never skip to
   green.
5. Retired/ignored tests carry an honest header and run by a named target.
