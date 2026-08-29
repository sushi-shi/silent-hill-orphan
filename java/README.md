# Canonical named-Java application

`src/main/java/defpackage/` is the complete, readable Java reconstruction of
the selected baseline. Original bytecode is authoritative; JADX is the primary
draft and CFR plus instruction-level inspection resolve every disagreement.

`api-stubs/` contains declaration-only Java ME APIs. Stubs compile separately
and are never packaged in `target/java/orphan-java.jar`, so the artifact is a
real MIDP application JAR with `defpackage.Application` as its MIDlet entry
rather than a desktop emulator bundle. The port does not use this JAR for production
execution: its source is the reviewed behavioral reference and exact `javac`
AST input for Java-to-Rust crosswalks.

The initial checked-in bodies are a baseline draft. A body becomes reviewed
only when its original `Code` hash, complete `javac` AST, and corresponding
Rust `syn` AST are exhaustively crosswalked in the transliteration audit.

```sh
just java-typecheck
just java-build
just java-build-check
just java-named-check
just java-mappings-check
just java-source-backups-check
just java-builds-check
just java-method-variants-check
just java-identifiers-check
just java-numeric-shape-check
just java-literals-check
just java-semantic-constants-check
just java-coverage-check
just java-test
```

The surface gate recompiles the sources and compares every class hierarchy,
field, and method declaration directly with the selected original class files.
It also rejects unresolved decompiler markers. This prevents an incomplete or
placeholder Java tree from becoming the apparent AST authority.

`tools/transliteration/JavaAstAuditDump.java` walks the real `javac` source tree,
including initializer order and synthetic constructor prologues. Its 1,428-item
/ 51,823-node denominator is hash-locked to the selected original's complete
bytecode inventory in `reconstruction/ast-authority.toml`. The identifier gate
also locks the 2,492-declaration denominator and rejects short, typed-prefix,
compound-call, and numbered-generic decompiler-name families; the zero-budget
literal ledger explains all 1,195 remaining non-structural raw uses in
executable code. A separate domain gate requires 229 `TextId`, 72 Ink-command,
and 105 Ink-event consumers and rejects same-valued constants borrowed from an
unrelated domain. The corresponding Rust walker uses `syn`; reviewed
crosswalk coverage is ratcheted by the method audit and currently covers the
first 160 method bodies plus 183 Java fields they reach. Of those, 149 mutable
fields map to sixteen instance/static Rust owner structs and thirty-four final
fields map to typed Rust constants; one mutable array also owns a separately
inventoried initializer template. The
reverse `syn` inventory separately rejects unreviewed functions, value
declarations, and owner containers.

“Named” has a precise limit here. The verified baseline contains no
`LocalVariableTable`, `LineNumberTable`, or `SourceFile` attributes, so the
author's exact parameter/local spellings do not survive in the classfiles. All
1,075 fields and 326 non-initializer methods have explicit original-to-semantic
mapping rows; all 1,418 parameter/local/counter/iteration/catch declarations in
the reconstructed source have reviewed role-based names and zero known
decompiler-generated residue. The latter are semantic reconstructions, not a
claim that their spelling is the lost author's spelling.

The all-build model recomputes 49 exact code families, three source lineages,
and five content profiles from all 115 verified JARs. The complete two-build
source-named delta ledger accounts for 304 byte-identical common methods, 51
reviewed method variants, five reference-only methods, and eight reference-only
fields. `just java-test` executes the recovered baseline, canonical Java, and
strict Rust on the same 888,857 edge cases for the 104 reviewed leaf methods.
The naming-reference JAR is also compared on all 882,661 cases outside
its two ledger-reviewed input-timing variants.

The surviving `M.java~`, `MyCanvas.java~`, `Ext.java~`, and `ExtBase.java~`
editor backups provide independent author vocabulary and control-flow evidence.
They are not blindly substituted: the selected original classfiles decide the
shipped behavior. CFR and those backups repair two loops that JADX collapsed
(delimiter tiling and RMS rollback), while baseline-only behavior such as
`enemyAnimUpdate` writing the constant `"0"` is retained despite a later backup
using its parameter.
