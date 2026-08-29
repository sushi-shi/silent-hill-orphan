# Canonical baseline decision

The canonical code/content baseline is
`silent_hill_orphan_j2me_en_v010_4_zip_silent_hill_2014_konam_106`, payload
SHA-256 `ca9a874ce7c7fb11ed444701b65e967919d3d7c0bcf73f80a9fcf825fb6a4be7`.
The independent semantic naming reference is
`silent_hill_orphan_j2me_en_v010_9_zip_silent_hill_2007_konam_41`, payload
SHA-256 `9f638bcd52b8d3314014ced473a1b510279b7453819ff9387647524e2152b7ea`.

This is a quality/completeness decision, not a chronology claim. The enclosing
collection calls its wrapper “2014”, while the payload manifest says 0.1.0 and
does not establish a trustworthy date.

## Evidence

- Complete campaign carrier: `chunks/0.bin` through `chunks/f.bin`.
- Six atomic locale pairs: `de`, `en`, `es`, `fr`, `it`, and `pt`, each with a
  UI `localization/*.properties` table and narrative `sh/lan/*.lan` table.
- 13 default-package application classes, 350 methods, and 62,197 bytes of
  method code. Bundled vendor adapters do not inflate these counts.
- Semantic class identities survive in bytecode: `M`, `MyCanvas`, `Ext`,
  `ExtBase`, `Menu`, `Resource`, `RoomObject`, `Script`, `ScriptThread`,
  `LoadRequest`, `Cheat`, `s`, and `txt_consts`.
- Every baseline field and method name+descriptor recurs under those same 13
  owners in the naming reference. Its five additional methods and eight
  additional fields are treated as variant policy, not canonical declarations.

The mechanically largest candidate has 355 methods but only eleven campaign
chunks (`0` through `a`), so it is comparison evidence rather than the playable
baseline. Version 1.5.3 carries all sixteen chunks but only three locale pairs
and a more device-specific obfuscated class tree; it remains a later-lineage
cross-check.

The `*.java~` backups found in other builds are strong semantic/preprocessor
evidence. Their hashes and occurrences belong in the generated resource
catalog; their literal contents remain outside tracked CC0 source.
