# Status — Silent Hill: Orphan

Living record of what is recovered and verified. Newest first.

## Phase 3 — strict transliteration started

- The workspace has JVM, MIDP, strict game-translation, content, and format
  layers. Only the bounded serialization/content codecs are `#![no_std]`;
  JVM/MIDP/transliteration code uses ordinary Rust, and filesystem/JAR traversal,
  compilers, windows, audio devices, and packaging remain in tools/hosts.
- `orphan-jvm` fixes Java wrapping, division/remainder, and shift semantics.
  `orphan-me` starts the deterministic MIDP subset with a clipped CPU ARGB
  framebuffer. These are device/runtime implementations, not transliterated
  game bodies.
- The first 158/350 game methods (`M.min`, `max`, `abs`, `dir`, `toInt`,
  `toBoolean`, `getLeft`,
  `getTop`, `resourceExit`, `destroyApp`, `pauseApp`, `appStart`, `resourceURLEncode`, `codedString`, `charToString`,
  the default constructors for `Application`, `CheatController`, `SilentHillGame`,
  `InkEngine`, `InkCodes`, and `TextId`, `SilentHillGame.menuResetIngameValues`, `appInit`,
  `CheatController.<clinit>`,
  `resourceMergeSortCmp`, the one-argument `printArray`,
  `resourceRestartImportants`, `resetLoad`, `find`, `readString`, `writeString`, `readStringList`,
  `tickBasedTime`, `tickBasedTimeUpdate`, `tickBasedTimeReset`, `getGameLangPath`,
  both `getGameText` overloads, `getPosInLanguageSelectionList`,
  `txtStringReplace`, `removeStringPrefix`, both Ink-server getters and mutators,
  `resetVariableSystem`, `roomRepaintRun`, `clearAllRMS`, `freeMemory`, `setDisplay`, `paint`, `rmsDelete`, `saveChunkINI`, `resourceMakeSubChunk`, `roomGetCurrent`, `roomSetCurrent`, `roomGetHistorySize`,
  `roomGetLastInRoomHistory`, `roomAddToRoomHistory`, `roomRemoveLastInRoomHistory`,
  `resourceIsOnHeap`, `random`, `arrayCopyString`,
  both `LoadRequest` constructors, `createFromInputStream`, `getID`, `getDescription`, `equals`,
  `getResourcePath`, `toString`,
  `GameResource`'s constructor, `<clinit>`, `equals`, `paint`, and `paintSimple`,
  `ExtBase.actionKeyIdConvert`, both `popupCreate` overloads, `popupSetNext`,
  `actionKeyKeycodeToActionkey`, `actionKeyGetScriptId`,
  `actionKeyUnsetAllKeys`, `actionKeyInitSystem`, `isMenuScrollAllowed`,
  `inventoryEquipUnequipHandling`, `splashMoreExists`, the two-argument `wrapString`,
  `M.loading`, all three `loadRequest_getResourcePath` overloads,
  `M.setKeyStatus`, and `MyCanvas.<init>`, `paint`, `showNotify`, `keyInit`, `keyJadEntryAsInt`, `keyConvertToKeyId`, `keyPressed`, `keyReleased`,
  both `Menu.addChoice` overloads, `getChoiceNr`, `countChoices`,
  `getChoiceID`, `nextChoice`, `previousChoice`, `setPosition`, `setCurrent`,
  `scrollIncrease`, `scrollDecrease`, `setTop`,
  `setSoftkeyOptions`, `setInvItemResource`, `active`, `closeAll`, `MenuModel.<clinit>`, `closeCurrent`, `getCurrent`,
  `InkScript`'s stream constructor, `getString`, `hasEvent`, both instance `executeEvent` overloads,
  the static script-ID `executeEvent` dispatcher, `getItemName`, and `<clinit>`,
  `hasCommand`, `setVariable`, `getVariable`,
  `getVariableAsInteger`, `setInventory`, `removeInventory`,
  `getInventorySize`, `isWaiting`, `resume`, `stop`, `InkInterpreter`'s constructor, `execute`, `resume`,
  `read()`, `read(int)`, `readSigned`, `hasCommand`, `integerArgument`, and `RoomObject`'s `isOver`,
  stream constructor, class initializer, `battlePanelNew`, four indexed battle-panel setters, `executeEvent`, `getName`,
  `getMoveDir`, and `enterHover`) are strict
  Rust translations. Each is hash-bound to original
  bytecode and opcode streams, complete `javac` and `syn` ASTs, a written
  per-node semantic crosswalk, and a live 991,338-case oracle in which the
  recovered baseline, canonical Java, and Rust agree. The naming-reference JAR
  agrees on all 985,142 non-variant cases; its 6,196 excluded requests cover two
  extra input-timing policies scoped by live validation of the variant ledger.
  Coverage stays an explicit ratchet; the other 192 bodies are not claimed.
- The 177 Java fields reached by those methods are exhaustively mapped: 144 mutable
  fields become 144 Rust fields in `CheatControllerStatics`, `SilentHillGameStatics`, `ApplicationState`, `ResourceRequestState`, `GameResourceState`, `GameResourceStatics`, `InkEngineState`, `GameCanvasState`,
  `MenuState`, `MenuStatics`, `InkInterpreterState`, `InkInterpreterStatics`,
  `InkScriptState`, `InkScriptStatics`, `RoomObjectState`, and `RoomObjectStatics`; thirty-three final
  coded-string/Ink constants become typed Rust constants, and the mutable transform
  table's eight class-initializer values have their own constant template. Each declaration
  has complete `javac`/`syn` node ownership; all sixteen state-container ASTs are
  hash-locked. Typed `JavaObject`, `JavaOwnedObject`, `JavaResourceId`,
  `InkVariableError`, `InkScriptRegistryValue`, `InkScriptExecuteEventError`,
  `InkScriptGetItemNameError`, `InkEnginePopupCreateError`, `ApplicationSetDisplayError`,
  `ApplicationResourceMakeSubChunkError`, and
  `RoomObjectStringEventError` and `RoomObjectEnterHoverError` enums and their variants are independently claimed
  as Rust-only adaptations.
  The reverse `syn` inventory permits only the 160 reviewed functions, 216
  reviewed value declarations, and 31 reviewed containers (407 total
  declarations). Eighteen focused Rust tests exercise the admitted bodies, and the
  injected unowned-constant proof goes red.

## Phase 2 — canonical Java application and AST authority

- `java/src/main/java/defpackage/` contains all 13 application classes. It
  type-checks against declaration-only Java ME stubs and builds a deterministic,
  stub-free MIDP JAR whose entry is `defpackage.Application`. The JAR is
  evidence/tooling, not the production runtime.
- Its binary surface exactly matches the selected original: 13 classes, 1,075
  fields, and 350 methods through a complete 1,414-row original-to-semantic
  mapping. Four surviving author editor backups are SHA-256-bound across 29
  verified payload occurrences.
- The exact `javac` identifier inventory contains 2,492 declarations: 1,075
  fields, 349 parameters, 815 locals, 142 loop counters, 2 iteration variables,
  and 109 catch variables. Short, typed-prefix, compound-call, and numbered
  decompiler-name families have zero residue and an injected-failure gate. The
  original JAR contains no local-variable, line-number, or source-file debug
  tables: parameter/local names are reviewed semantic reconstructions, not a
  claim to have recovered lost author spellings.
- Exact semantic coverage is 13/13 classes, 1,075/1,075 fields, and 326/326
  non-initializer methods. The zero-budget literal ledger explains all 1,195
  remaining non-structural raw uses in executable Java across 36 reviewed
  domains. The semantic-domain gate separately locks 229 `TextId`, 72 Ink
  command, and 105 Ink event consumers and rejects raw or same-valued
  cross-domain substitutions. Numeric/conversion shape is compared for all 350
  methods; 17 reorderings and 7 compiler-equivalent differences are explicitly
  ratcheted.
- The real `javac` walker inventories 1,428 source items and 51,823 semantic
  nodes, including initializer order and implicit constructor prologues. The
  inventory is hash-locked to the complete original bytecode denominator.
  `orphan-ast-audit` provides the corresponding complete `syn` walk.
- CFR plus the author backups repaired the JADX-collapsed delimiter tiling and
  RMS rollback loops; original bytecode remains authoritative where the later
  author backup differs. The fixed AST is now hash-locked.

## Phase 1 — corpus, baseline, formats, languages

- All 115 unique JAR payloads are parsed directly from bytes and grouped into
  49 exact game-code families. The reviewed build model assigns every JAR to
  one of three source lineages and five content profiles, includes the real
  packaged `sh/M` MIDlet in all 25 split-runtime builds, and scopes 19
  third-party-branding observations without inventing an unverifiable
  “official” status. The resource catalog contains 384 unique blobs across
  4,719 occurrences.
- The reviewed baseline is
  `silent_hill_orphan_j2me_en_v010_4_zip_silent_hill_2014_konam_106`
  (`ca9a874…`), with all 16 chunks and six co-shipped language pairs. The
  independent naming reference is build `…konam_41` (`9f638bc…`); every
  baseline field and method name/descriptor recurs there. See
  `java/reconstruction/BASELINE.md`.
- The complete source-named variant ledger accounts for all members of both
  builds: 304 exact-common methods, 51 reviewed varying signatures (including
  five reference-only methods), and eight reference-only fields. Each variant
  records exact shape, code-size, call-set, device-policy, and evidence facts.
- The resource-free `orphan-formats` codec uses bounded slice readers for the
  chunk archive, modified-UTF language/string tables, embedded INK scripts, and
  rooms. Corpus tests cover every applicable occurrence and reject malformed
  input. PNG/MIDI remain standard container concerns outside this codec.
- German, English, Spanish, French, Italian, and Portuguese are integrated by
  content hash, with exactly 185 UI and 714 narrative slots apiece. Every locale
  code found in any build must appear in the ledger. The filename-declared
  Chinese/A1000 build actually contains English/French resources, so no false
  Chinese option is exposed.

## Phase 0 — resource-free foundation

- The repository is CC0 and contains no original game bytes. Immutable builds
  live in a private resources repository and materialize only into ignored
  `_originals/`, verified against `builds.toml` by SHA-256.
- Originals, class/resource inventories, language selection, codecs, Java
  surface/JAR/ASTs, method audits, and the differential oracle all have named
  can-fail proofs in `docs/GATES.md`.
- Fork: pure 2D / Graphics2D.

## Next executable milestones

1. Continue the bytecode-small leaf tranche, then move call-graph inward through
   string/array helpers, INK script execution, rooms, menus, and the main loop.
2. Expand `orphan-me` only when a translated caller needs an exact MIDP/MMAPI/RMS
   behavior; add independent format oracles and then a frame oracle at first loop.
3. Add thin Linux and browser presenters at first real frame, then reach first
   in-game frame and player movement before any optional modern-engine rewrite.

During Phase 3, `just check-affected`/`just watch-affected` use the hash-declared
dependency graph in `tools/gates/gates.toml` for the fast inner loop. The full
`just check` battery remains mandatory at milestones and final completion.
