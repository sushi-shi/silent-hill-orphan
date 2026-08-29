# Repository structure — Silent Hill: Orphan

This repository is **resource-free** and dedicated to the public domain (CC0).
It contains only recovered-by-hand reconstruction and our own code — never
literal copied binaries. See the j2me home's `PLAYBOOK.md` for the method.

## Resource storage contract (three layers)

1. **Authored sources, in this repo** — engine/game/tooling code, transcribed
   data, and the reconstruction ledgers (`java/reconstruction/`).
2. **Binary resources, in a private resources location** — the surviving
   distributions. `tools/originals/fetch.py <path-or-url>` (or the env var named
   in `game.toml`) copies them into git-ignored `_originals/` and runs
   `verify.py`. The location is never baked into the repo (R1).
3. **Derived, regenerable** — `_reference/` (catalogs, fingerprints, decompiles)
   and web outputs, rebuilt from layers 1–2, never committed.

## Boundary rules

- The root `Cargo.toml` is the only workspace manifest; one `target/`, one lock.
- `transliteration/` is the executable spec, not a future modern shipped engine.
  `orphan-jvm`, `orphan-me`, and `orphan-game-xlat` use ordinary Rust; only the
  serialization/content codecs are deliberately `no_std`.
  Production modern code must not depend on the transliteration at runtime;
  test-only differential oracles may (R12).
- `crates/` holds the shipped engine libraries (2D or 3D per `game.toml`'s
  `fork`); `apps/` the frontends; `web/` page composition only.
- Repository-owned ignored directories begin with `_`; `.gitignore` matches them
  by name (no trailing-slash globs) so symlinks are covered too (R2).
- `_originals` must be a **real directory, never a symlink** (R2).

## Layout

```text
_originals/            immutable surviving jars/zips (git-ignored, sha256-verified)
_reference/            generated catalogs & fingerprints (git-ignored, regenerable)
java/reconstruction/   provenance, baseline, language, AST authority ledgers
java/src/              complete canonical named-Java MIDP application
transliteration/       orphan-jvm, orphan-me, orphan-game-xlat, AST audit + ledgers
crates/                no_std orphan-formats + orphan-content (engine later)
apps/                  native/browser presenters at first real frame
tools/                 originals/, corpus/, java/, transliteration/ workflows
docs/                  REPOSITORY_STRUCTURE, GATES, FORMATS, STATUS (+ more)
game.toml, Justfile, flake.nix, Cargo.toml
```
