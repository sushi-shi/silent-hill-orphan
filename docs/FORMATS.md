# Original resource formats — Silent Hill: Orphan

Status: **Phase 1 recovered and gated.** Every custom format below has a Rust
parser in the `*-formats` crate that **rejects malformed/truncated input with a
typed error without panicking** and is exercised against **every applicable unique blob**
(playbook R1/R2/R3/R10). Decode each format from its **consumer** (the bytecode
that reads it), not by guessing; state each multi-byte field's endianness.

## Language classification (declared vs. actual) — R10/R11

The filename and the internal language code both lie in the sibling ports; the
**authoritative** key is the decoded string-table content.

The selected baseline contains complete, slot-compatible `de`, `en`, `es`,
`fr`, `it`, and `pt` UI/narrative pairs: 185 UI slots and 714 narrative slots
each. They are integrated by content hash in
`java/reconstruction/language-packs.toml` and decoded by `orphan-content`.

The collected `ZH`/Motorola A1000 filename is contradicted by its bytes: that
payload contains English/French resources and no Chinese table. It is therefore
not presented as a Chinese language pack.

## Formats present in the corpus

The roles, endianness, layouts, and consumers below come from bytecode review
and `catalog_resources.py`. Text containers are modeled from their consumers,
not assumed to be Java properties or ordinary UTF-8 (R11).

- `.png` — standard sprites/tiles/UI; slot numbering maps to a role.
- `.mid` / `.wav` — audio; playback via the host's MMAPI model (R9).
- `localization/*.properties` — line-indexed UI strings (not key/value Java
  properties); zero-based line position is the wire identity. The codec matches
  `M.loadLanguage`: CR/LF terminate a non-empty record, empty records are
  suppressed, and an unterminated final fragment is ignored.
- `sh/lan/*.lan` — big-endian `u16` record count followed by big-endian `u16`
  byte lengths and exactly that many encoded bytes. There are no per-string NUL
  terminators; an early corpus test deliberately made that mistake and failed
  on the German table. Exactly two archived Spanish occurrences append one LF
  after the declared 714 records; retail stops at the count, and the codec
  accepts and reports only that specific anomaly rather than arbitrary trailing
  data. Each record is validated with Java `DataInputStream.readUTF`'s modified
  UTF-8 byte grammar; strings remain borrowed encoded bytes so UTF-16/control
  semantics can be recovered without lossy early conversion.
- `chunks/*.bin` — INK compact resource archives. `M.resourceGetNamesAndData`
  and `M.resourceGetFromBytes` establish the exact framing: `u8` entry count,
  then `u8` name length + name bytes + big-endian `u16` payload length + exact
  payload bytes. `ChunkArchive` borrows all names/data, rejects truncation,
  empty names, and trailing bytes, and is exercised over every corpus chunk.
  `ink::ScriptResource` and `ink::RoomResource` then reproduce the canonical
  consumer order for modified-UTF string slots, typed reference groups, script
  graphics/event tables + bytecode, and all six room-object record types. They
  retain borrowed encoded strings and reject invalid indices, event IDs,
  offsets, object types, truncation, and trailing data across the full corpus.
- `*.ini` — textual client/index/settings/splash configuration.

`orphan-formats` and `orphan-content` are `#![no_std]` by default and use only
`core` + `alloc` where allocation is required. JVM semantics, MIDP, the strict
transliteration, filesystem/JAR traversal, PNG, audio, windowing, AST tooling,
and packaging use ordinary Rust.
