# `builds.toml` schema — the provenance authority (playbook R2)

`builds.toml` is the **single tracked owner** of build provenance. The binary
payloads themselves are never committed — they live in the private resources
location and are materialized into git-ignored `_originals/` by `fetch.py`.
`verify.py` reconciles every materialized payload against the `sha256` + `bytes`
table here.

`gen_builds.py` fills in the **mechanical** fields automatically. The checked-in
Silent Hill ledger has completed its first review: `build-model.toml` and
`validate_build_model.py` now recompute every judgment field from the verified
corpus. Regenerating this file produces a candidate and must not silently erase
those reviewed annotations.

## Top-level keys

| key | meaning |
|---|---|
| `title` | human title of the game |
| `slug` | repo slug |
| `fork` | `"2d"` (Graphics2D) or `"3d"` (M3G) — from the M3G probe |
| `baseline` | reviewed id of the quality/completeness baseline build |
| `naming_reference` | reviewed id of the independent source-named semantic reference |

## `[[payload]]` — one row per UNIQUE payload (deduped by sha256)

A payload is a single JAR (`kind = "jar"`) or JAD (`kind = "jad"`). The **same**
payload appearing under several names / in several containers is **one row** with
multiple `collected_as` + `containers` entries (meaningful aliases, kept).

| field | kind | meaning |
|---|---|---|
| `id` | mechanical | human-readable, uniqueness-gated identifier — **never** a hash slug that can alias (R2) |
| `sha256` | mechanical | the payload's content hash — **identity is this, never the filename** |
| `bytes` | mechanical | exact byte length (verified) |
| `kind` | mechanical | `"jar"` or `"jad"` |
| `is_3d` | mechanical | M3G probe result for this payload |
| `class_count` | mechanical | number of `.class` entries |
| `midlet_name` / `midlet_version` / `vendor` / `cldc` | mechanical | from `MANIFEST.MF` (strings, may lie about language) |
| `resolution` / `device` | mechanical | derived from the filename (a hint; confirm from canvas dims) |
| `declared_language` | **HINT** | from the filename/manifest — **verify from decoded content, never trust it** (R10) |
| `review_status` | reviewed | `canonical-baseline`, `semantic-reference`, `fingerprinted`, or evidence-scoped `third-party-branded`; this deliberately does not claim an unverifiable official release |
| `repack_tag` | mechanical | a detected fan-repack signature (`by X`, a modder domain) — a hint to archive |
| `code_family` | reviewed/mechanical | one of the 49 exact game-class shape families, locked by `build-model.toml` |
| `source_lineage` | reviewed | `source-named`, `obfuscated-engine`, or `split-runtime`, derived from class ownership/layout |
| `content_profile` | reviewed | actual locale-member profile derived from the JAR, never its filename |
| `collected_as` | mechanical | every original distribution name this payload was seen under |
| `containers` | mechanical | `_originals/<file>` (top-level) and/or `inside _originals/<zip>` (nested) |
| `companion_jad` / `companion_of` | mechanical | link a JAR to its JAD and vice-versa |
| `notes` | curatorial | aliases, evidence, anything a reviewer must know |

## `[[archived]]` — same schema, for proven altered payloads

Preserved and verified (evidence is never discarded), but **not** recovery
targets. Third-party branding alone does not prove altered code, so those JARs
remain in `[[payload]]` with `review_status = "third-party-branded"`. Move one
to `[[archived]]` only when byte/content evidence establishes that policy.

## `[[container]]` — immutable zip wrappers

| field | meaning |
|---|---|
| `id`, `sha256`, `bytes` | identity of the zip |
| `collected_as` | the zip's distribution name |
| `holds` | sha256 of every member (jars, jads, loose files) — the provenance link |

## Rules (R2)

- Never modify the contents of a surviving archive. Filenames are curatorial;
  contents are immutable. **Identity is the `sha256`**, never the id or filename.
- Delete only a cryptographically proven accidental duplicate; **keep meaningful
  aliases** (same payload under two names) and document them.
- Assign a semantic field (review status, lineage, true language, baseline) only
  when bytecode / decoded content / cross-build evidence supports it.
