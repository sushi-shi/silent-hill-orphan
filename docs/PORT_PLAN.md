# Port plan — Silent Hill: Orphan

The preserved class files are the authority; the checked-in Java application is
the readable behavioral reference and exact `javac` AST input; strict Rust is an
executable spec. A maintainable modern game may follow only after that spec can
be compared frame-for-frame.

## Evidence and content

- Resolve builds by SHA-256, never collection filename. The reviewed baseline is
  the full named 16-chunk build in `BASELINE.md`; all 115 JARs are assigned to
  49 exact bytecode families, three source lineages, and five decoded-content
  profiles. Every other build remains available for code/device deltas and
  language/resource evidence.
- A language is integrated only when decoded content and slot shape agree. The
  all-build locale closure gate admits a useful later-build language but rejects
  filename-only claims. Original bytes stay private; public ledgers store member
  paths, hashes, counts, and provenance.
- All custom byte formats live in `orphan-formats`, `no_std` + `alloc`, over
  borrowed slices. Filesystem/archive walking is test/tool code. Standard image
  and audio containers stay out of the core codec.

## Java recovery and transliteration

- Keep all 13 Java classes buildable as one real stub-free MIDP application JAR.
  Before Rust work, require exact original class/field/method surface and no
  unresolved decompiler markers. Require recovered semantic constants at every
  statically known text/INK protocol site so a decompiler's same-valued constant
  from an unrelated integer domain cannot masquerade as a meaningful name.
- For each Rust body, record original code/opcode hashes, complete canonical
  `javac` AST + nodes, complete Rust `syn` AST + nodes, semantic adaptations, and
  executable oracle evidence. Coverage is a numerator over all 350 original
  bodies; helpers and adapters receive an explicit classification.
- Work leaf-first: arithmetic/string/array helpers, resource and INK execution,
  rooms, graphics, menus, persistence/audio, then lifecycle/main loop. Preserve
  Java widths, overflow, evaluation order, exception boundaries, and known bugs.

## Runtime and hosts

- `orphan-jvm`: exact Java primitives/collections/clock/RNG pieces as demanded.
- `orphan-me`: MIDP Graphics2D image/font/input serial queue, RMS, MMAPI event
  model, and deterministic CPU ARGB framebuffer. These runtime and translation
  layers use ordinary Rust; `no_std` is reserved for serialization codecs.
- `orphan-game-xlat`: statics collected into explicit game state, otherwise a
  strict structural translation. It does not ingest the modern format crate in
  production; independent parser comparisons are test-only.
- Milestones: first real pixel, first in-game frame, player movement, thin Linux
  presenter, thin browser presenter, then deterministic frame/audio/state oracle.
  Native/web shells are intentionally deferred until a real game frame exists so
  a placeholder window cannot masquerade as port progress.

## Optional modern pass

After the complete transliteration runs, an idiomatic data-driven engine may own
the shipped game. It must remain production-independent of the transliteration
and match it frame/state/audio-decision-for-decision on shared clock, seed, input,
save, content, and language before replacement is accepted.
