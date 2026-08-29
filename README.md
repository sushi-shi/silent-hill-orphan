# Silent Hill: Orphan — J2ME recovery & native Rust port

A game-preservation project: recover Silent Hill: Orphan from its surviving J2ME builds and
reimplement it as a maintainable native Rust game for Linux and the browser,
following the shared method in `../PLAYBOOK.md` (the j2me home).

This repository is **resource-free** and dedicated to the public domain (CC0): it
contains only recovered-by-hand reconstruction and our own code. The original
game binaries are never committed — they live in a private resources location and
are materialized locally by an explicit fetch.

## Getting started

```sh
# materialize + verify the corpus from the private resources location
python3 tools/originals/fetch.py <resources-checkout-or-its-originals-dir>
#   or set the env var named in game.toml (resources_env)

# reconcile the materialized corpus against builds.toml, and prove the gate bites
python3 tools/originals/verify.py
python3 tools/originals/verify.py --self-test
```

With `nix` + `just`:

```sh
nix develop
just bootstrap <resources>
just check-affected       # only content-hash-invalidated gate groups
just watch-affected       # rerun affected groups while editing
just check
```

The affected-gate runner is Git-independent: `tools/gates/gates.toml` declares
each group’s real inputs, and only a successfully checked content fingerprint is
cached. A failed or newly changed hash reruns immediately. `just check` remains
the milestone/final umbrella and refreshes every cached fingerprint only after
all gates and can-fail proofs pass.

Reusable discoveries are maintained in the local J2ME Preservation Kit at
`../_template` and copied into this standalone game repository deliberately.
Only Silent Hill build evidence, canonical source, variants, oracle vectors, and
node crosswalks remain game-specific.

## Status

Phases 1–2 are operational and Phase 3 has started: the complete canonical Java
application, content-proven six-language integration, bounded `no_std` codecs,
reviewed 115-build family/lineage/content model, exact `javac`/`syn` audit
pipeline, and the first 160 oracle-verified Rust methods are in-tree.
The first 183 Java fields also have exhaustive declaration crosswalks: 149 mutable
state fields in sixteen explicit instance/static owners, thirty-four scalar Rust
constants, and one separately inventoried mutable-array initializer template.
The real MIDP JAR exists as an AST/oracle
authority but is not the production runtime. The game is not playable yet;
the strict boundary also has hash-locked typed Java `Object` and variable-error
representations.
Coverage is deliberately reported as 160/350 bodies and 183/1,075 fields. See
`docs/STATUS.md`, `docs/GATES.md`, `docs/CROSSWALK_FINDINGS.md`, and the provenance authority
`java/reconstruction/builds.toml`.
