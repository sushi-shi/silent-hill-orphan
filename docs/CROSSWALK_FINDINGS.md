# AST-crosswalk findings

The method crosswalk is not a coverage ornament. For every reviewed body it
hash-locks the verified original bytecode, walks the complete canonical
`javac` body, walks every participating Rust `syn` body, and requires each
Java and Rust node to belong exactly once to either a shared semantic operation
or a reasoned one-sided adaptation. Behavioral oracles are independent evidence,
but they cannot enumerate every malformed state, exception edge, evaluation
order, overload, or switch target.

This log follows the sibling Gothic port's discipline: `O-*` records defects in
this port or its audit machinery; `A-*` records deliberate representation
changes. A reviewed body is not allowed to hide an unresolved divergence in an
adaptation.

## O-1 — statement-position Rust macros were absent from the syn denominator

Gothic finding G-12 applies directly to the inherited AST emitter. `syn` stores
`panic!(...);`, `unimplemented!(...);`, and similar statement-position macros
as `Stmt::Macro`, not `Expr::Macro`. The original Orphan walker visited only
the latter, so a future loud subsystem boundary could have contributed zero
semantic nodes while its containing method was called fully crosswalked.

The walker now records the complete token payload of every `Stmt::Macro`. A
unit regression constructs a statement-position `panic!` and requires a
`MACRO` node. At the time of the fix, none of the then-reviewed 63 production
bodies contained such a macro, so their ratcheted node counts remained unchanged.

**Lesson.** Exhaustiveness is only as strong as the auditor's node taxonomy.
Whenever the port starts using a new Rust syntactic position, prove the walker
can see it before accepting the first crosswalk that contains it.

## A-1 — Java strings stay UTF-16 at the strict boundary

`M.charToString` returns `[u16; 1]`, and `ExtBase.actionKeyIdConvert` accepts a
borrowed UTF-16 slice. This deliberately avoids Rust `char`/`str`: a Java
`char` is one code unit and can be an isolated surrogate. The oracle exhausts
all 65,536 `char` values and includes arbitrary isolated-surrogate action-key
inputs.

**Lesson.** A convenient UTF-8 conversion can erase real Java states before an
oracle sees them. Preserve the source representation first; decode for a host
only at an explicit adapter.

## A-2 — implicit JVM null faults are explicit Rust results

The byte-array comparator and both action-key lookups dereference Java arrays or
strings without null guards. Rust receives `Option` and returns
`NullPointerException` explicitly. Those extra Rust nodes are one-sided
adaptations, and null cases execute through both recovered JARs, canonical
Java, and Rust.

**Lesson.** Never turn an unconditional Java dereference into a silent Rust
`None` path. Exception behavior and the position of the throwing read are part
of the body.

## A-3 — application, resource, menu, script, and interpreter fields have shaped owners

The 149 mutable fields reached by the reviewed methods—including application timing,
resource/language/Ink-server state, menu, input, and sound state, script/interpreter
state, and room-object/battle-panel state—live in `ApplicationState`,
`ResourceRequestState`, `InkEngineState`, `GameCanvasState`, `MenuState`,
`MenuStatics`, `InkInterpreterState`, `InkInterpreterStatics`, `InkScriptState`,
`InkScriptStatics`, `RoomObjectState`, `RoomObjectStatics`, `GameResourceState`,
`GameResourceStatics`, `CheatControllerStatics`, and `SilentHillGameStatics`. Each Rust field has a direct
original-field and canonical-`javac` declaration claim with complete
non-overlapping node ownership. The sixteen structural owner ASTs are hash-locked
separately, and the reverse `syn` inventory rejects any unreviewed function,
value declaration, or container. Opaque handles retain Thread/Menu/Resource
identity where the current methods observe only nullness, identity, or stack
position, keeping the strict boundary explicit. The oracle writes the real
recovered/canonical fields before each call, constructs the corresponding Rust
owner, and compares return values, exceptions, identity, and mutations.

**Lesson.** A method oracle that compares only the return value misses static
stores, byte truncation, and read/write order. Seed every field the method can
read and compare every field it can write.

## A-4 — instance arrays and mutable class indices have separate owners

`RoomObject.battlePanel` and `battlePanelID` belong to each object, while the
five `BATTLE_PANEL_*` fields are mutable class statics. They therefore live in
separate `RoomObjectState` and `RoomObjectStatics` owners; folding them into one
per-instance struct would silently duplicate Java's static authority. The
`orphan-jvm` compatibility layer performs signed array bounds checks and distinguishes null,
out-of-bounds, and negative-allocation failures. `battlePanelNew` deliberately
stores the new ID before allocation so a negative length preserves that write
while leaving the previous array untouched.

The oracle mutates the private static indices through reflection in both real
JARs and canonical Java. It covers null and short panels, negative and extreme
indices, negative allocation lengths, every successful mutation, and every
field that must remain unchanged when an exception occurs.

**Lesson.** Java array syntax hides observable exception and side-effect order.
An in-range happy-path comparison cannot establish a transliteration, and a
single state struct cannot represent both object fields and mutable statics.

## A-5 — object handles are separate from resolved script state

`InkInterpreter.script` is a nullable opaque handle in `InkInterpreterState`;
the strict read bodies receive the handle's resolved `InkScriptState` as a
separate borrow. This retains shared-object identity without nesting or copying
the script into every interpreter. Crucially, `read()` checks the nullable
handle before accessing the resolved script data, then increments `offset`
before the array load. Thus a null script leaves the cursor unchanged, while a
nonnull script with null data or a bad index leaves the increment visible.

The oracle distinguishes all three cases and checks partial cursor progress
through multi-byte reads. A future heap adapter must resolve the same handle;
the borrowed state is not permission to bypass or reorder the original field
dereference.

**Lesson.** Separating a Java reference from its Rust storage is safe only when
the crosswalk fixes the original dereference point. Resolving too early changes
which mutations survive an NPE.

## A-6 — StringBuffer paths use explicit UTF-16 appenders

The three `Application.loadRequest_getResourcePath` overloads and
`ResourceRequest.getResourcePath` construct resource
names with `StringBuffer.append(String)` and `append(int)`. The strict Rust body
keeps the result as `Vec<u16>` and implements three local appenders: ASCII
literals, nullable UTF-16 strings, and signed decimal integers. This preserves
Java's observable `null` text for a null `gameId` or string ID, every isolated
surrogate, and the complete signed `i32` decimal domain without round-tripping
exact Java code units through a host Unicode string.

Those appenders contribute 67 Rust AST nodes with no one-to-one Java syntax.
They are therefore explicit Rust-side adaptations, not invisible helper
machinery. The five switch cases remain separate shared operations; the MIDI
case records that Java's six literal appends are coalesced into `/sfx/mid/` and
`.mid`. The oracle crosses recognized and unknown types, null and adversarial
UTF-16 inputs, and signed integer edges against both recovered JARs and
canonical Java.

The object overload also preserves its unusual exception schedule: it first
casts and unboxes an `Integer`, catches any resulting `Exception`, and then
casts to `String`. Consequently null takes the string path, while an unrelated
object escapes as `ClassCastException`. Its nominal transform argument is
ignored and both branches pass zero. `ResourceRequest.getResourcePath` reuses
the crosswalked Rust builder, and `toString` remains a separately audited
delegation rather than receiving behavioral credit transitively.

The matching object constructor has the same cast schedule and also ignores
its transform argument, leaving `imageTransform` at the JVM allocation default
of zero. Its Rust constructor writes every default explicitly. `getID` keeps
the returned Integer-versus-String/null kind explicit at the oracle boundary,
and `equals` maps its caught cast/null failures to an absent typed candidate
while retaining the original short-circuit field order.

**Lesson.** An oracle can show that generated paths agree, but only the AST
crosswalk proves that helper logic, default arms, and literal coalescing have
all been reviewed instead of falling outside the claimed translation.

## A-7 — native arraycopy semantics are explicit and overlap-safe

`Application.arrayCopyString` is ten Java AST nodes because all null checks,
signed bounds checks, exception selection, and overlap handling are hidden
inside `System.arraycopy`. The strict Rust body expands that one native call
to 156 visible nodes. It validates both arrays and every signed range before
mutation, snapshots the source window, and then writes the destination so
forward and backward same-array overlaps have Java memmove behavior.

The strict boundary uses nullable opaque integer handles for String references.
That retains null slots, duplicates, and exact reference placement without
claiming that Rust owns or decodes the String objects. The oracle covers null
arrays, negative/extreme positions and lengths, zero-length boundary calls,
distinct arrays, and every small overlapping window.

**Lesson.** A tiny Java AST can hide a large native semantic contract. Exact
node ownership on the Rust side prevents the bounds/error/overlap machinery
from being waved through merely because the Java source contains one call.

## O-2 — an eager conditional helper underflowed outside the chosen branch

The first Rust draft of Java decimal-digit recognition used
`predicate.then_some(unit - zero)`. Rust evaluates the `then_some` argument
eagerly, so a code unit below a later Unicode digit range underflowed even when
the predicate was false. The all-code-unit oracle panicked immediately. An
explicit `if` now guards the subtraction, and its control-flow nodes are part
of the accepted `toInt` crosswalk.

**Lesson.** Source that reads like a conditional is not necessarily lazy.
Cross-language evaluation order must be demonstrated by adversarial execution
and then made visible in the AST claim.

## A-8 — Java Object categories and parseInt stay explicit in typed Rust

`Application.toInt` and `toBoolean` distinguish null, boxed Integer, String,
and every other Object. `JavaObject` represents those four categories without
host reflection; the enum and each variant are separately hash-locked Rust-only
declarations, so representation helpers cannot enter outside the reverse
inventory. Strings remain borrowed UTF-16.

The Rust parser implements Java's ASCII sign handling, all BMP decimal-digit
families accepted by `Integer.parseInt`, and exact signed-32-bit overflow
rejection. The oracle covers every single UTF-16 code unit, sign and overflow
boundaries, all object categories, and randomized multi-unit strings.
`toBoolean` is factored through the proven identity `toInt(value) >= 1`, but it
retains its own complete Java/Rust body crosswalk.

**Lesson.** A typed boundary can replace runtime `instanceof` only when every
source category, fallback value, parse failure, and helper declaration remains
independently visible to the audit.

## A-9 — Language text IDs use the numeric value of `$`: radix 36

`Application.getGameText(String)` passes the character constant
`TEXT_IDENTIFICATOR = '$'` as `Integer.parseInt`'s radix. Java promotes that
character to its numeric value, 36. The strict Rust path therefore implements
Java sign, Unicode-digit, ASCII/fullwidth letter, and i32 overflow rules for
radix 36 explicitly, and catches both parse failures and delegated lookup
failures.

The first Rust reconstruction and its prose incorrectly called this radix 35.
An exhaustive one-code-unit oracle still missed the error because its table also
had only 35 slots: Java parsed `z` as index 35 and returned the bounds fallback,
while Rust rejected `z` and returned the parse fallback, producing the same
`???`. The per-operation literal audit forced a review of the claimed helper
expansion against the original `bipush 36`. The oracle now uses a nullable
36-slot table, making `z` select a distinct value in slot 35.

**Lesson.** Language-pack integration includes the game's ID grammar and fallback
control flow, not only decoding the `.lan` bytes. Exhaustive inputs are not
enough when the observation fixture collapses two different paths to one value.

## A-10 — Hashtable mutation order is observable state

`Application.inkServerSetVariable` writes the variables table before the hints
table and sets `gameChangedSinceLastSave` only after both writes succeed.
`inkServerUnsetVariable` has the same variables-then-hints ordering. A null
second table or null hint can therefore leave the first table changed while the
dirty flag retains its old value. `resetVariableSystem` likewise clears the
variables table before dereferencing the hints table and never touches the dirty
flag. The strict Rust representation keeps both tables as separately nullable owners,
and the oracle compares their complete sorted UTF-16 contents plus the flag after
both success and failure.

The room-history and inventory writers reuse those exact mutators. Their
crosswalks additionally lock null StringBuffer append (`inv-null`), the
last-room lookup before `id.equals`, signed history-size wrapping, the second
history-size read during removal, and the order of the room-N and room-size
writes.

**Lesson.** A final-state-only oracle misses partially committed Java state.
Seed every independently nullable owner, inject failure at each dereference, and
compare every mutation that survives the exception.

## A-11 — typed variable reads preserve three different Java failures

`InkScript.getVariable` is not just a map lookup. A present value with no hint
fails with `NullPointerException`; a present empty hint fails at `charAt(0)`
with `StringIndexOutOfBoundsException`; and malformed text under hint `I`
fails with `NumberFormatException`. `getVariableAsInteger` propagates that last
failure when it comes from an `I` hint, but catches the same numeric parse
failure when the text came through hint `S` and returns one. Unknown nonempty
hints instead become null and therefore integer zero.

The strict port uses a three-variant `InkVariableError` rather than flattening
these paths. Its Java-compatible parser is a separately inventoried Rust body
owned by the `getVariable` crosswalk. All of its nodes are paired to the single
`Integer.parseInt` Java subtree, including every BMP decimal-digit family and
signed overflow handling; callers then retain the original catch boundary.
The differential exhausts all 65,536 one-code-unit UTF-16 values beneath both
`S` and `I` hints, in addition to missing/empty/unknown hints and randomized
tables.

**Lesson.** The AST walk matters even after a broad oracle is green: it makes
both `charAt(0)` calls, the parser body, and the exact try/catch boundary
impossible to silently coalesce into a convenient Rust `Option` or one generic
error.

## A-12 — command discovery is a stateful parser, not a byte search

`InkInterpreter.hasCommand` first reads and masks a command byte, but a match
returns immediately before its arity byte or payload is consumed. A miss must
then interpret the high two bits, including the extended-arity byte; skip
integer literals by their encoded one/two/four-byte widths; perform the String
pool lookup even though its result is discarded; stop at `COMMAND_END`; or
recursively visit child expressions in order. Consequently, both the boolean
answer and the interpreter's post-call offset—and its offset after a truncated
read—are observable evidence.

`InkScript.hasCommand` adds a second short-circuit layer over event codes 1
through 56. It reads each `eventOffsets` slot in order and only constructs a
scanner for values other than minus one. A match in an early event therefore
prevents later short-array or malformed-bytecode failures that would otherwise
occur. The Rust wrapper uses a nonnull opaque script handle when constructing
the temporary interpreter, preserving Java's nonnull `this` relationship while
the actual script storage remains explicitly borrowed.

The differential covers all 256 command headers against every target command,
compact and extended trees, every truncation prefix, String-array failures,
hostile offsets, randomized trees, and first/last/short event tables. The AST
crosswalk separately owns all 129 Java/208 Rust scanner nodes and all 45
Java/74 Rust wrapper nodes.

**Lesson.** A recursive bytecode walker must be verified as a cursor-mutating
parser. Searching raw bytes or checking only final booleans cannot prove operand
widths, ignored-but-throwing lookups, or early-return fault suppression.

## A-13 — build variants scope an oracle; they do not weaken the baseline crosswalk

The naming-reference build's `GameCanvas.keyPressed` and `keyReleased` bodies
are real `input-timing-policy` variants. In addition to the baseline's loading,
load-bar, and dissolve guards, that lineage checks logo state and wall-clock
timing. Its `Application.repaintCanvasIfPossible` is separately a
`rendering-policy` variant: it omits `serviceRepaints` and adds key-flag
behavior. Treating both JARs as interchangeable made correct baseline Rust
bodies look wrong; simply dropping the second authority would instead have
hidden the reasons for the disagreements.

The oracle now validates those three exact member signatures against the
hash-locked source-named variant ledger before excluding only their 6,215
requests from the naming-reference comparison. The recovered baseline,
canonical Java, and Rust still compare on all 991,477 cases. The
naming-reference JAR still compares on the remaining 985,262 cases, and
`Application.setKeyStatus` remains a four-authority comparison because its body
is common. Complete post-state checks prove the sticky new-key latch, current
pressed state, last-key write, and signed-byte scroll reset—not merely the
return path.

**Lesson.** A second build is an independent authority only within its reviewed
equivalence scope. Every exclusion must name an exact member, require a live
variant-ledger classification, and leave the selected production baseline's
bytecode → canonical `javac` → Rust `syn` crosswalk and oracle intact.

## O-3 — declaration claims conflated raw and canonical field names

The first constant-field crosswalk exposed a real naming boundary: the baseline
classfile spells its 30-column bound `M.COLLUM_WIDTH:I`, while canonical Java
correctly calls it `CODED_STRING_COLUMN_WIDTH`. The declaration validator had
required `java_item == <field:java_name>`, so it could express only fields whose
raw and semantic names happened to be identical. That would either block a
legitimate semantic rename or tempt a reviewer to put the wrong name on one
side merely to satisfy the schema.

The field schema now permits an explicit `canonical_name`, defaulting to the raw
name for all existing claims. Original existence is still checked by raw
`(owner, name, descriptor)`; the javac AST item is independently checked by the
canonical name; both complete declaration ASTs remain node-crosswalked. The new
claim therefore binds `M.COLLUM_WIDTH:I` →
`Application.CODED_STRING_COLUMN_WIDTH` → Rust
`CODED_STRING_COLUMN_WIDTH` without erasing the retail misspelling from the
evidence chain.

**Lesson.** Naming recovery is a mapping, not an in-place spelling assertion.
Audit schemas must identify the raw and semantic members separately or the
first useful rename breaks provenance exactly where naming adds value.

## A-14 — the two lowercase-hex paths have different padding boundaries

`Application.resourceURLEncode` and `codedString` both delegate digit rendering
to `Integer.toHexString`, but their leading-zero tests are not the same. URL
encoding uses `character < 15`; U+000E becomes `%0e`, while U+000F becomes the
nonstandard `%f`. The byte dump uses `c <= 15`, so byte `0x0f` is rendered as
`0f `. Coalescing them behind a conventional two-digit percent encoder would
silently fix one shipped bug and change server request bodies.

The strict Rust bodies keep UTF-16 code units and unsigned bytes directly and
render lowercase hexadecimal without a host formatter. The URL oracle exhausts
all 65,536 singleton code units plus randomized strings. The dump oracle covers
all 256 byte values, full/partial 30-column lines, and the 100-line boundary:
2,999 bytes retain a padded last line, 3,000 produce exactly 100 full lines, and
all bytes after the first 3,000 are ignored.

**Lesson.** Similar formatting helpers are not interchangeable until their
comparison operators and width rules are checked in bytecode. Exhaustive small
domains are especially valuable here: one boundary glyph can distinguish two
otherwise identical-looking implementations.

## A-15 — key initialization preserves ten settings reads and ten ordered stores

`GameCanvas.keyInit` is ten superficially identical assignments, but the field
and key paired at each position are behavior: `KEY_SEND` is the third write,
`KEY_END` feeds `keyReturn`, the thumbstick setting feeds the center softkey,
and `KEY_CLEAR` is the final write. Every assignment independently reads
`InkEngine.settingsHash`, calls the already crosswalked `Application.toInt`,
then publishes one static binding. A null table faults at the first `get` before
any binding changes; a missing key becomes null and therefore zero; Integer,
String, and every other nonnull Object retain the three `toInt` categories.

The first Rust draft hoisted `settings_hash` before the ten assignments. Its
happy-path and null-table oracle results were identical, but the AST walk made
the changed read schedule explicit—the same optimization class Gothic G-10
warns about. The final strict body performs the owner dereference inside the
lookup closure on every call, so its execution schedule still contains ten
field reads. The explicit state borrow rules out concurrent owner replacement;
the typed `JavaOwnedObject` representation retains exact Integer and UTF-16
String payloads plus the single observationally equivalent Other category.

The 4,285-case tranche seeds and compares all ten bindings for null, empty,
complete, sparse, irrelevant, duplicate, and 4,096 randomized tables. The
independent node ledger owns all 111 Java and 207 Rust body nodes, including
each key literal and destination field separately; the table declaration owns
all 3 Java and 6 Rust nodes.

**Lesson.** Repeated-looking configuration assignments are wiring and ordering
tests, not one generic map operation. Keep each source key, destination, read,
conversion, and store individually visible in the crosswalk even when Rust
factors their common lookup mechanics.

## A-16 — nullable Vector appends and clears retain partial commits

`Application.resetLoad` does not validate its download Vector before changing
the other loader state. It first clears `loadThread`, then stores loading mode
minus one, and only then calls `resourcesToDownload.removeAllElements()`. A null
Vector therefore throws after both scalar/reference stores survive. The Rust
owner uses an outer `Option` and an explicit null error at the `clear` site;
1,184 complete-state cases prove the ordering for nullable, empty, populated,
nullable-entry, and randomized Vectors.

`MenuModel.addChoice(Object,String)` has a second partial-commit boundary. It
appends the ID to `choiceIDs`, appends the text to `choiceTexts`, and only then
sets `updateBodyLines` and `updateMenu`. If the first Vector is null nothing
changes. If only the second is null, the ID append remains but neither flag is
set. The int overload constructs a nonnull boxed Integer and delegates into the
same sequence. The Rust representation keeps nullable opaque IDs and nullable
UTF-16 texts in separately nullable owners; the int overload uses `Some(i32)`
as the observational equivalent of the boxed value.

The two overloads each have 7,648 four-authority cases spanning independent
Vector nullness, old flags, null elements and arguments, signed integer edges,
isolated surrogates, and randomized contents. Their ledgers account for all
23/37 and 8/13 Java/Rust body nodes separately; `resetLoad` accounts for all
13/26.

**Lesson.** Multiple container mutations followed by dirty flags form a commit
protocol even without an explicit transaction. Do not pre-validate every owner
or combine the mutations into one convenient Rust operation: place each null
fault at its original dereference and compare the state that survives it.

## A-17 — the string codec widens signed bytes and never accepts EOF

`Application.readString` initially looked like an ordinary NUL-terminated byte
decoder. The node walk exposed two incompatible assumptions in the first Rust
draft. Retail calls `DataInputStream.readByte`, not `readUnsignedByte`, and then
casts the signed result to `char`; bytes `0x80..0xff` therefore become
`U+ff80..U+ffff`. More dangerously, its empty `catch (Exception)` is inside
`while (true)`. EOF, a null stream, or any permanently failing read is retried
forever. Only a successfully read zero byte exits.

The strict transliteration now widens through `i8` and keeps the retry
loop, using `spin_loop` only as a host execution hint. This deliberately differs
from the safe bounded codecs in `orphan-formats`: those validate untrusted host
data, while this body records the shipped game behavior. Its 7,142 terminating
oracle cases cover every byte value, embedded NULs, unread tails, selected
pairs, and randomized strings. The 41/54 Java/Rust node ledger owns the
nontermination path that cannot safely be invoked by an in-process oracle.

`ResourceRequest.createFromInputStream` inherits that contract only after its
first unsigned type read. Its type-2 path reads the signed-byte string before
requiring the transform; type 3 reads unsigned ID then transform; all other
unsigned types read a string. The factory's outer catch returns null for faults
outside `readString`, but cannot intercept the inner infinite retry. A further
shipped quirk is now locked down: type 2 and 3 parse a transform and pass it to
the Object constructor, which discards it, leaving `imageTransform == 0`.
11,716 full-state-plus-cursor cases and all 57/111 Java/Rust nodes verify those
boundaries.

**Lesson.** Exception placement is part of a codec grammar. Before converting a
read loop to a bounded parser, crosswalk the signed read primitive, the catch
scope, the only terminating edge, caller-side catches, and the cursor left for
the next field. A conventional EOF error here would be safer but not faithful.

## A-18 — stream helpers are defined by cursor and attempted side effects

`Application.find` always returns zero. Success is observable only because the
input cursor stops immediately after the first matching window; failure and any
exception consume through the failing read and also return zero. Its window is
pre-filled with one ASCII space per target code unit, so an empty target or a
target made entirely of spaces can match without reading at all. The strict
body retains the literal sliding window rather than replacing it with a host
substring search. Across 69,716 cases, all UTF-16 singleton targets, every
unsigned byte, overlapping markers, absent non-byte code units, randomized
streams, and null owners agree by return value plus remaining input. All 58/61
Java/Rust nodes have independent owners.

The first `writeString` oracle run rejected the canonical Java reconstruction,
not Rust. The recovered exception table covers bytecode offsets 0–31 with one
`catch (Exception)`: `value.length`, `charAt`, every character write, and the
terminator write are all inside it. The earlier nested-try source incorrectly
let a null value escape. Canonical Java was corrected to the single retail
scope and its complete javac inventory was reratcheted from 51,831 to 51,823
nodes before the Rust claim was admitted.

Retail truncates every UTF-16 code unit to a signed byte before calling
`DataOutputStream.write(int)`, stops after the first failed character write,
and attempts one zero terminator only after all characters succeed. A
closure-based typed output boundary exposes those exact attempted signed ints
independently of a concrete I/O backend. The 69,694-case oracle exhausts all
UTF-16 singleton values and compares every attempted write and committed low
byte under null owners and injected failures; the ledger owns all 37/38
Java/Rust nodes.

**Lesson.** A canonical AST is an authority only while it remains bound to
bytecode and executable oracles. When an oracle contradicts reconstructed
source, inspect the exception table, correct the Java app, and deliberately
reratchet the whole AST inventory—never bend Rust to a convenient decompilation.

## A-19 — `readUTF` expands into visible host-independent helper nodes

`Application.readStringList` publishes its nullable `String[]` immediately
after reading an unsigned one-byte count, then fills it in place. Its one outer
`catch (Exception)` returns null when the count cannot be read and returns the
already allocated, partially filled array after any later failure. Localization
is also ordered: a nonempty dollar-prefixed value is replaced through
`getGameText(String)` before it is stored, and the stored slot is dereferenced
again for the `savePoint` test. A null localized result therefore faults at
`equals`, leaving that null in the returned partial array. `saveIsPossible` is
set only when the stored value equals `savePoint` and `loadingMode == 0`.

Java exposes the wire decoder as the single library call
`DataInputStream.readUTF`; no application AST contains its implementation. The
strict Rust translation must nevertheless implement Java modified UTF exactly.
Its private helper therefore contributes 237 Rust nodes recorded as eight
Rust-only runtime adaptations: the big-endian byte length, complete payload
read before validation, UTF-16 allocation/cursor, one-, two-, and three-byte
forms, invalid-lead rejection, and successful return. Keeping those nodes in
the denominator matters: a conventional UTF-8 decoder would reject/normalize
different inputs, and streaming validation would leave the cursor earlier than
Java's `readFully` on malformed payloads.

The 72,119-case tranche exercises null/truncated inputs, every UTF-16 singleton
encoded with Java modified UTF, nullable and valid localization, both loading
modes and prior flag states, malformed lead/continuation classes, declared
lengths through 65,535, unread tails, and 4,096 randomized lists. All 92 Java
application nodes and 132 Rust body nodes have shared semantic ownership; every
one of the 237 runtime-expansion nodes has a reasoned one-sided owner.

**Lesson.** Exact AST comparison does not mean forcing unequal source trees
into fake one-to-one syntax. It means every node on both sides is visible and
owned exactly once: shared game behavior is paired, while library/runtime
expansions are explicitly categorized and independently justified.

## A-20 — closing the current menu is not a stack pop

`MenuModel.closeCurrent` first obtains the last menu object through
`getCurrent`, then passes that object to `Vector.removeElement`. The latter
removes the first equal occurrence, not necessarily the last slot. Duplicate
references make the distinction observable: `[A, B, A]` becomes `[B, A]`, not
`[A, B]`. The method then calls `active()` again, re-reads the new last object
through `getCurrent`, and sets that object's `isCurrent` flag true. A singleton
stack becomes empty and no flag is written; a reflected null stack faults at
the first `active()` call.

The Rust owner retains ordered opaque handles and accepts a callback for the
final object-field write, keeping object resolution outside the strict game
state without copying menu instances into the static stack. Its 2,920-case
oracle exhausts every stack of length zero through five over three identities,
all duplicate arrangements, every initial three-flag combination, and null
stacks. All 23 Java and 79 Rust nodes have one semantic owner.

**Lesson.** A collection method name is not a semantic summary. Pair the exact
receiver, argument-producing call, library operation, and post-mutation reads;
otherwise `removeElement(getCurrent())` is easily but incorrectly simplified
to `pop()`.

## O-4 — a new oracle tranche advanced the shared RNG and replaced old cases

The first `GameResource.equals` generator used the oracle builder's existing
random-number stream. Although its new comparisons were green, inserting those
draws before older randomized tranches changed all their subsequent samples:
the total corpus appeared to grow, but some previously established requests had
quietly disappeared. Comparing the expected additive count exposed the drift
before the method was admitted.

The tranche now owns a fixed, method-specific RNG seed. Its 5,290 unique
requests append to the prior 963,896 requests exactly, producing 969,186; later
generators receive the same stream state as before.

**Lesson.** Oracle coverage must be monotonic, not merely deterministic. Give
each randomized method tranche an independent seed (or persist its exact
requests), and ratchet the additive per-command counts so inserting a new
method cannot rewrite old evidence.

## A-21 — resource equality is an ordered virtual call, not a tuple comparison

`GameResource.equals` rejects null/wrong-class candidates first, compares the
candidate's `type` before touching either identifier, then invokes
`candidate.ID.equals(this.ID)`, and compares `imageTransform` only if that
virtual call returns true. A null candidate ID therefore throws NPE when types
match, but the same null is never dereferenced after a type mismatch. A null
`this.ID` is merely the right operand and makes ordinary String/Integer/Object
equality false.

The Rust owner keeps all eight instance fields. `JavaResourceId` distinguishes
boxed-Integer value equality, exact UTF-16 String equality, and identity-only
opaque objects; `Option` retains null. The constructor explicitly supplies the
JVM allocation defaults for image and four geometry fields. The oracle has 300
constructor cases and 4,990 equality cases across the two recovered JARs,
canonical Java, and Rust. Every one of the constructor's 17/23 and equality's
40/89 Java/Rust nodes is owned, along with all eight declarations and the typed
ID variants.

**Lesson.** Do not replace chained Java equality with a derived Rust tuple
comparison. Receiver direction, virtual equality category, null-fault location,
and short-circuit field-read order are all observable behavior.

## A-22 — a one-line draw wrapper still has an ordered fault boundary

`GameResource.paintSimple` is only one Java statement, but the original
bytecode fixes seven semantically relevant steps: load the nullable `Graphics`
receiver, read `this.image`, load `x`, `y`, and `anchor`, invoke exactly
`Graphics.drawImage(Image,III)`, and return. The receiver's null check happens
at `invokevirtual`, after all arguments have been evaluated, and an exception
raised by `drawImage` propagates uncaught. Neither a null image nor an unusual
anchor is filtered by the game body; both cross the MIDP boundary unchanged.

The strict Rust transliteration keeps image and graphics as opaque handles and takes
one draw callback. It snapshots the image before making the implicit receiver
fault explicit, then passes all five values without coordinate arithmetic or
anchor interpretation. The 2,024-case oracle crosses null/nonnull graphics,
null/nonnull image, the full cube of five signed edges for every integer
argument, success, injected draw-time NPE, and 1,024 independently seeded
random cases. Its output records whether the call occurred and all arguments,
so a null-receiver failure cannot masquerade as a callee failure. All 10 Java
and 25 Rust nodes have exactly one semantic owner.

**Lesson.** AST exhaustiveness matters most when source looks too small to
deserve scrutiny. A call expression contains receiver evaluation, argument
evaluation, descriptor-selected dispatch, and exception propagation; a return-
value oracle or a graphics mock that merely counts calls cannot prove those
parts independently.

## A-23 — the region-table fault occurs before the null Graphics fault

`GameResource.paint` begins with a hard image-null return: that one guard
suppresses both geometry calls, the transform-table access, and every Graphics
fault. With an image present it computes `left` and `top` before branching.
Transform zero calls `drawImage`; every other signed value enters `drawRegion`
and indexes the live `GameCanvas.transformTable` directly—there is no clamp or
default transform.

The nonzero arm's operand stack exposes the subtle order. It loads the Graphics
receiver, image, source origin, width, height, then evaluates
`transformTable[transform]`; only after loading the remaining arguments does
`invokevirtual` check whether Graphics is null. Thus an invalid transform and a
null Graphics together throw `ArrayIndexOutOfBoundsException`, not NPE. The
Rust body deliberately checks the signed eight-slot array before converting
the nullable graphics handle into an explicit error. Both draw callbacks retain
uncaught failure propagation.

The table is a `static final int[]`, so only its reference is final; its eight
elements remain mutable. `GameCanvasState` is the single mutable owner, while a
separately inventoried constant supplies the exact `{0,2,5,7,3,1,6,4}` class-
initializer contents. The 6,400-case oracle overwrites all eight elements before
each invocation and records the complete draw call. It crosses null owners,
both draw arms, simultaneous array/receiver faults, injected callee faults,
wrapping geometry, every valid and invalid transform class, and 4,096 isolated-
seed random states. All 76 Java and 136 Rust body nodes, plus all 13 Java and
12 Rust declaration/initializer nodes, have exactly one owner.

**Lesson.** A null receiver is not necessarily the first failing part of a Java
method call: the JVM evaluates every argument before `invokevirtual` performs
the receiver check. Crosswalk the operand-producing subtree in bytecode order,
especially when an argument contains an array access or another throwing call.

## A-24 — resume clears shared pause state before nested execution can fail

`InkInterpreter.execute` is a result-carrying state machine, not a conventional
callback loop. It writes RUNNING first, publishes the prior command result on
each iteration before inspecting status, invokes `executeCommand` only while
status remains 1, and publishes `this` to `pausedThread` only after a normal
loop exit with status 4 or 5. An exception skips that final publication and
retains any paused-thread identity that existed before a direct `execute` call.

`resume` changes that exceptional post-state: its first operation clears
`pausedThread`, and only then does it delegate to `execute(null)`. A null script
therefore leaves status RUNNING and the pause slot null without advancing the
cursor; null data advances the cursor once before NPE; a bad index advances it
once before AIOOBE. Moving the clear into a success-only cleanup or folding it
into `execute` would agree on ordinary returns while corrupting every failing
resume.

The 1,944-case tranche runs the recovered and canonical implementations' real
`executeCommand` over END, CHOOSE, WAIT, all signed integer widths, string
lookup, RETURN, and multi-command programs. It crosses initial Object values,
prior null/self/other pause identities, null/truncated scripts, and signed
cursor edges while observing result, status, offset, exception kind/index, and
pause identity. All 52 Java/61 Rust `execute` nodes and all 9 Java/20 Rust
`resume` nodes have exactly one semantic owner; Rust's explicit `Result` edge
is recorded as an adaptation instead of disappearing inside the oracle.

**Lesson.** Nested calls do not make wrapper statements unimportant. Crosswalk
and oracle the state immediately before delegation as well as the final return,
especially when exceptions can bypass the callee's normal epilogue.

## A-25 — an explicit state owner can bind a repeated Java static read

`InkScript.resume` first reads `InkInterpreter.pausedThread` for its null test,
then the original bytecode reads the static field again to obtain the
`invokevirtual` receiver. The game/runtime Rust boundary cannot expose a Java
object reference, so it represents the shared slot as an optional opaque handle
inside `InkInterpreterStatics` and passes the bound handle to a resume callback.

That is a deliberate single-read adaptation, not an accidental loss of a Java
AST subtree. The Rust function holds an exclusive borrow of the complete
statics owner across the adjacent test and callback dispatch; no alias can
change the slot between the source-level reads. The crosswalk assigns the
second Java field-select nodes to this representation argument, while the
explicit Rust `?` owns nested exception propagation and `Ok(())` owns normal
void return.

The 162-case oracle proves both sides of the guard. Null pause state suppresses
even deliberately broken scripts. Present state executes the same real command
programs used for interpreter resume, discards every returned Object, propagates
NPE/AIOOBE with exact cursor progress, and observes whether CHOOSE/WAIT
republishes the interpreter. All 13 Java and 20 Rust nodes are owned exactly
once.

**Lesson.** AST comparison makes representation-driven read coalescing visible.
When two source reads become one bound Rust value, record why intervening
mutation is impossible and oracle both the suppressed and executed paths.

## A-26 — absent events suppress allocation, data access, and every command fault

The three-argument `InkScript.executeEvent` is exactly a forwarding overload:
it supplies `false` for language debug and changes nothing else. The core
four-argument overload first calls `hasEvent`. A minus-one event slot returns
the original nullable Object immediately; only a present slot causes a fresh
interpreter to be constructed, assigned the debug flag, and executed.

The AST makes two details non-optional. First, the absent return precedes the
constructor subtree, so even null or truncated bytecode must remain untouched.
Second, a present path reads `eventOffsets[eventCode]` again for the constructor
after `hasEvent` already read it. The strict Rust body retains both operations
instead of treating the boolean query as permission to cache an offset. Runtime
allocation is explicit: the caller supplies storage and an opaque identity for
the fresh interpreter, allowing a later CHOOSE/WAIT pause to refer to the same
owner without choosing a host allocator or thread model at this typed boundary.

The 1,152-case tranche crosses null/short/canonical event tables, absent and
hostile offsets, broken and valid scripts, both debug values, room identity,
prior pause identity, all supported oracle command programs, and every initial
Object category. Paused programs expose the constructed interpreter's script,
room, debug, status, and offset; absent events prove all those operations were
suppressed. The forwarding overload owns 8 Java/23 Rust nodes and the core owns
31 Java/65 Rust nodes, each exactly once.

**Lesson.** A guard that appears to be a convenience query may be an allocation
and fault boundary. Crosswalk the guarded constructor subtree separately, and
do not infer that an earlier lookup licenses removal of a later source read.

## A-27 — the raw script registry must retain wrong-class and null states

The static `InkScript.executeEvent(String, …)` overload reads the public raw
`Hashtable list`, casts the selected value to `InkScript`, returns null on a
missing key, and otherwise delegates to the four-argument instance overload
with language-debug mode false. A typed Rust map from IDs directly to scripts
would erase three retail states before the body could observe them: reflection
can set the registry itself to null, `Hashtable.get(null)` throws, and raw Java
code can store a non-script value whose cast throws `ClassCastException`.

The Rust owner therefore stores an optional vector of exact UTF-16 keys and a
two-category value enum: opaque script identity or other Object. Lookup faults,
cast faults, and arbitrary nested execution failures remain distinct in a
generic error enum. An absent key returns null before event-table or bytecode
access and leaves the prior paused interpreter untouched; a valid entry passes
the original Object and room identities to the already crosswalked instance
executor and always supplies false.

The 528-case oracle tranche uses the real static field in both recovered JARs
and canonical Java. Its 144 lookup cases cross null/empty/populated registries,
null/empty/NUL/surrogate/text IDs, missing and matching keys, valid and
wrong-class values, and both pause seeds. Its 384 delegation cases reuse the
complete event/data/value/room/pause matrix. All 5 Java/6 Rust declaration
nodes and all 26 Java/75 Rust body nodes have exactly one semantic owner.

**Lesson.** Do not let a safer host type silently prove away states admitted by
a public raw Java collection. First inventory the source collection's null,
key, value, cast, and mutation behaviors; then make every erased runtime check
an explicit typed branch in the AST crosswalk and oracle it through the real
field.

## A-28 — a one-call lifecycle wrapper still needs its own proof

`Application.destroyApp(boolean)` ignores its boolean argument and consists
only of a call to `exit()`. The production Rust body expresses that unported
lifecycle dependency as a generic callback and transparently returns its error,
but the wrapper remains a separately admitted original method rather than
receiving credit from either the callback or the future `exit` transliteration.

The four-case oracle crosses both flag values with present and null MIDlet
state while running the real Java `exit`. Its complete post-state proves the
call occurred: `runtime` is cleared and `appInited` becomes false before either
normal return or the null MIDlet's propagated second `notifyDestroyed` NPE.
Identical results for true and false prove the flag is unused. All four Java and
four Rust body nodes are paired exactly once.

**Lesson.** Tiny forwarding methods are call-graph and exception edges. Hash,
crosswalk, and oracle them independently; otherwise an omitted call and a
correct callee can coexist while aggregate coverage still looks healthy.

## A-29 — javac initializer nodes are not automatically runtime bytecode

The canonical `InkScript.<clinit>` AST contains five nodes: construction of the
empty script `Hashtable`, followed by the literals 0, 1, and 2 from the three
private `GFX_TYPE_*` declarations. Retail bytecode contains only the Hashtable
construction and `putstatic list`; the three constants use classfile
`ConstantValue` attributes and have no runtime stores.

The Rust initializer therefore publishes only `Some(Vec::new())`. Each GFX tag
is independently declaration-crosswalked to a typed i32 constant, while its
synthetic `<clinit>` AST occurrence is a Java-only, bytecode-proven adaptation.
The initializer oracle is deliberately the first process request and observes
the class's real pre-mutation registry as nonnull and empty. All 5 Java and 11
Rust initializer nodes have exactly one owner.

**Lesson.** A source-AST initializer inventory and an executable bytecode
initializer inventory overlap but are not identical. For every constant
literal in `<clinit>`, inspect whether retail has a store or a ConstantValue;
never add fake runtime work merely to obtain a one-to-one AST shape.

## A-30 — `getItemName` has two distinct cast boundaries

`InkScript.getItemName` forwards a nullable ID through the static registry
dispatcher using semantic event `EVENT_GETNAME`, the one-code-unit UTF-16
fallback `"?"`, a null room, and debug false, then casts the returned Object to
String. The registry lookup can throw its own `ClassCastException` for a
wrong-class table value; a valid script can independently return an Integer and
fail the final String cast.

The Rust body calls the already crosswalked static dispatcher and wraps its
complete error domain separately from the final typed cast. Its 182-case oracle
crosses registry states and key shapes, null/short/hostile event tables, every
reviewed execution program, and both pause seeds. It observes returned UTF-16,
both cast sites, nested NPE/AIOOBE, and the full paused-interpreter state. All 13
Java and 55 Rust nodes are owned exactly once, including the statement-position
`vec!` macro used to construct the question-mark fallback.

**Lesson.** Same exception class does not mean same semantic edge. Preserve the
layer at which each dynamic cast occurs so later callers and diagnostics cannot
conflate corrupt registry contents with a script returning the wrong value
type.

## A-31 — tiny lifecycle and popup wrappers retain real call edges

`Application.pauseApp`, `Application.appStart`, and the two-argument
`InkEngine.popupCreate` contain no local game algorithm, but they are not
no-ops. They respectively forward exact `true`, invoke `Thread.start` on the
current static receiver, and forward nullable UTF-16 text plus the signed
recovery code with maximum-time sentinel `-1`. Each invocation can propagate a
callee failure.

The Rust pause/start bodies expose their not-yet-admitted callees as typed
callbacks, while the popup wrapper now calls the separately crosswalked live
three-argument state machine. This keeps each wrapper independently testable
without smuggling larger unreviewed bodies into its coverage claim. The
113-case addition observes
both hidden states; null, successful, and throwing thread starts including
attempt count; and popup text/recovery/timeout state over nullable and
adversarial UTF-16, signed integer edges, and empty/last/full queue positions.
All 17 Java and 19 Rust nodes have exactly one semantic owner.

**Lesson.** A callback boundary is a representation adaptation, not permission
to erase a call. Its arguments, receiver fault, invocation count, side effects,
and exception propagation still belong to the wrapper's AST and oracle proof.

## A-32 — a synthetic default constructor is still executable evidence

The six remaining five-byte default constructors—for `CheatController`,
`SilentHillGame`, `InkEngine`, `Application`, `InkCodes`, and `TextId`—all have
the same retail opcode shape: load `this`, invoke the no-argument superclass
constructor, return. Their canonical sources omit explicit constructors, so the
`javac` authority deliberately emits one `SYNTHETIC_SUPER_CONSTRUCTOR` node for
each body.

Rust represents that one body operation as a typed superclass-constructor
adapter. This is not object allocation: JVM allocation occurs at the caller's
`new` instruction, and these classes add no instance-field writes here. Real
objects are constructed in both recovered JARs and canonical Java; the abstract
`InkEngine` constructor is exercised through `SilentHillGame`. All six Java
nodes and 24 Rust nodes are owned, and each original body remains independently
hash-bound even though their byte sequences are identical.

**Lesson.** Do not drop compiler-synthesized AST nodes merely because the source
has no written declaration. Conversely, do not invent Rust object state for an
`<init>` body whose bytecode only forwards to its superclass.

## A-33 — popup creation publishes partial state in a strict order

The three-argument `InkEngine.popupCreate` first calls `wrapString`, then stores
the returned String array, recovery code, and maximum time into three separate
arrays. Only after all three stores succeed does it reset `popup_choice`. For an
inactive queue it then publishes `popupCurrent` before re-reading
`popupMaxTime`; deadline publication precedes `popupActive = true`, and the
signed popup count increments last.

The Rust body uses checked nullable arrays and preserves that exact schedule.
In particular, Java evaluates `wrapString` before `aastore` performs its null or
bounds check, so a bad `popupText` still executes the wrapping callee. A bad
recovery array retains the text store; a bad time array retains both earlier
stores; a bad inactive-current read also retains the choice and current-index
writes. The typed error keeps delegated wrapping failure separate from queue
NPE/AIOOBE. The 1,280-case direct oracle compares every scalar and complete
array across null/short/full shapes, hostile indices, both active states,
nullable/adversarial UTF-16, signed code/time edges, sentinel and timed paths.
All 82 Java and 174 Rust method nodes plus nine field declarations have exactly
one owner.

**Lesson.** Array assignment syntax hides its evaluation and fault schedule.
Crosswalk the bytecode stack order and compare partial state after every
throwing store; an equal final happy path cannot prove the queue transaction.

## A-34 — popup advance has two clock reads separated by a throwing array edge

`InkEngine.popupSetNext` first post-increments `popupCurrent` with Java `int`
wrapping. Retirement then clears the count and active flag and returns without
touching either the clock or `popupMaxTime`. A continuing popup instead writes
`popupMinTimeEnds = currentTimeMillis() + 500` before reading the nullable,
possibly short maximum-time array. The `-1` element publishes an indefinite end
time after one clock read; every other value triggers a second, independent
clock read and a wrapping Java `long` addition.

The Rust body keeps the clock as a callback and the array access checked, so
the read schedule and partial-failure state remain observable. An NPE or AIOOBE
retains the incremented current index and newly published minimum deadline but
leaves the old end deadline intact. The 864-case oracle crosses wrapping index
edges, count edges, both active states, and null/empty/short/sentinel/timed
arrays; it compares all queue scalars, the complete time array, normalized
deadlines, and zero/one/two clock-call reachability. All 53 Java and 90 Rust
method nodes, plus the new deadline field's declaration nodes, have exactly one
owner.

**Lesson.** Calls surrounding a possibly throwing read cannot be coalesced.
Only the exhaustive AST pairing makes the first clock call, array exception
edge, sentinel branch, and optional second clock call separately reviewable;
happy-path output alone cannot prove that schedule.

## A-35 — source initializer nodes are broader than retail `<clinit>` bytecode

`CheatController` declares four compile-time integer constants followed by the
mutable `lastKey = INIT_VALUE`. The complete `javac` initializer walk therefore
contains five nodes: literals `0`, `1`, `2`, `-123`, and the `INIT_VALUE`
reference. Retail's six-byte `<clinit>`, however, contains only `bipush -123`,
`putstatic lastKey`, and `return`; the four final constants live in classfile
`ConstantValue` attributes and execute no stores.

The strict transliteration adds one explicit `CheatControllerStatics` owner and
one initializer that replaces any seed with `-123`. The crosswalk maps the
actual mutable initializer and explicitly classifies the four source-only
constant nodes, while the declaration crosswalk owns `lastKey` separately. A
first-request oracle observes the class-init value in both recovered JARs,
canonical Java, and Rust.

**Lesson.** Exhaustive AST comparison does not mean forcing every source node
into invented runtime work. It means every node receives a reviewed owner or a
precise one-sided classification, checked against the bytecode's actual
execution surface.

## A-36 — lazy room-script resolution is an observable publication boundary

`RoomObject.executeEvent` consults the global script registry only while its
cached `script` field is null. A simultaneously null `scriptID` bypasses both
lookup and execution and returns the initial Object unchanged. Otherwise the
method dereferences the registry, looks up the exact UTF-16 key, dynamically
casts a hit to `InkScript`, and publishes that handle into the room before
executing it. A missing entry therefore proceeds from an already-null cache to
the later virtual-call NPE; a wrong-class entry throws CCE before publication;
and a successfully resolved script remains cached even when its execution
fails.

The Rust owner represents `scriptID` as nullable UTF-16 and `script` as a
nullable opaque handle. Its typed registry search, cast, publication, and
executor callback retain the Java order rather than pre-validating the whole
operation. `getName` then dispatches event 1 with the exact one-code-unit `?`
fallback, while `getMoveDir` dispatches semantic event 30 with null; both pass
false for debug and perform their own final nullable-String cast.

The 2,304-case RoomObject matrix crosses cached and lazy paths, null/empty/NUL/
surrogate/matching/missing IDs, absent/matching/wrong-class registries,
malformed and returning scripts, Object result types, debug flags, and prior
pause state. It compares result or exception, cached-handle publication,
interpreter script/room/debug/status/offset, and pause identity after every
failure. The crosswalk owns all 45/99 `executeEvent`, 10/54 `getName`, and 10/48
`getMoveDir` Java/Rust nodes exactly once. Seven canonical Java nodes belonging
to a decompiler-produced empty `if (this.script == null) {}` are explicitly
classified as source-only: retail and `javac` bytecode contain no operation to
transliterate.

**Lesson.** A return-value oracle cannot prove a lazy object boundary. The AST
walk identifies lookup, cast, publication, call, and wrapper-cast edges; an
independent stateful oracle must then prove their exact failure order and the
partial state each edge leaves behind.

## A-37 — hover entry probes only the cached script and returns identity after dispatch

Retail names the method `hooverIn` and its event field `EVENT_HOOVERIN`; the
canonical application records both raw spellings while using the reviewed
semantic names `enterHover` and `EVENT_HOVER_IN`. The behavior is importantly
different from `RoomObject.executeEvent`: if the cached script is null,
`enterHover` returns null immediately even when `scriptID` and the global
registry could resolve a valid script. It never performs lazy lookup.

With a cached script, the method first calls `hasEvent(54)`. A null or short
event table therefore throws before an interpreter is created, while an absent
event returns null. Only a present event proceeds to `executeEvent(54, null,
false)`. The callback's Object result is ignored; after successful execution
the method returns the exact same RoomObject receiver, while any execution
failure prevents that return.

The Rust body keeps the room identity as an opaque handle and uses a typed
error boundary to distinguish failure in the presence probe from failure in
dispatch. Its 304-case oracle crosses null and cached scripts, every hostile
registry shape, null/empty/short/absent/present/extreme event tables, the
existing malformed and returning interpreter programs, and prior pause state.
It compares exception, null-or-this identity, cached script, and complete
interpreter/paused state. All 27 Java and 60 Rust nodes, plus the four-to-one
event-constant declaration nodes, have exactly one semantic owner.

**Lesson.** Adjacent methods can deliberately use different resolution
policies. Reusing a convenient lazy dispatcher before checking the AST would
make null-cache hover entry observably wrong; return identity and the point at
which it becomes reachable also belong in the oracle.

## A-38 — an expired wait is cleared before resume, but resume may schedule another wait

`InkScript.isWaiting` reads the clock only when `waitStop` is positive. A
strictly future deadline returns true unchanged; a nonpositive deadline returns
false without a clock read or resume. A positive expired deadline instead
publishes zero first, invokes `InkScript.resume`, and returns false only if that
call completes.

That apparent three-branch leaf contained a call-graph interaction the first
oracle run exposed. A resumed interpreter can execute `COMMAND_WAIT`, which
sets a fresh clock-based `waitStop`. The initial Rust oracle modeled only the
interpreter's waiting status and incorrectly left the deadline at zero; both
recovered JARs and canonical Java retained a new absolute deadline. The fixed
resume boundary permits nested script-static mutation, and the oracle
normalizes only that inherently clock-based new deadline while still comparing
every stable numeric deadline exactly. Conversely, an NPE or AIOOBE before a
new wait command leaves the earlier zero store observable.

The 810-case matrix crosses signed-long minimum, negative, zero, definitely
expired, and definitely future deadlines with null/present paused interpreters,
nullable scripts, every existing interpreter program, signed offset edges,
stop/return/choose/wait commands, and nested NPE/AIOOBE. It compares boolean or
error, deadline post-state, interpreter status/offset, and paused identity. All
32 Java and 50 Rust body nodes and all 3/2 Java/Rust `waitStop` declaration
nodes have exactly one owner.

**Lesson.** Write-before-call order is not enough when the callee can write the
same location. Preserve the shared owner across the callback, compare
post-failure state, and let an independent oracle reveal reentrant mutations
that a locally plausible translation would erase.

## A-39 — inventory equip closes every menu before resolving the item script

`InkEngine.inventoryEquipUnequipHandling` contains only two source statements,
but their order is the user-visible transaction. `MenuModel.closeAll` replaces
the global stack with a new empty Vector first. Only then does the method call
the static script-ID dispatcher with `InkScript.itemID`, the signed choice ID as
the event code, null initial Object, and null RoomObject. The default dispatcher
also supplies false debug. Its successful Object result is discarded.

Consequently, null registry storage, null item ID, a wrong-class registry hit,
or malformed script execution all occur after the menu stack has been replaced.
A missing registry entry is a successful no-op after that close. The strict Rust
body composes the already admitted `menu_close_all` and
`ink_script_execute_event_by_id` functions around the same `MenuStatics` and
`InkScriptStatics` owners, rather than pre-validating the script and accidentally
leaving the menu open on failure.

The 960-case oracle crosses null/empty/nonempty stacks; null, empty, NUL,
surrogate, matching, and missing IDs; null/empty/matching/wrong-class
registries; signed event and event-table edges; malformed, stopping, waiting,
returning, and choosing scripts; and both prior pause states. It compares the
stack after every OK/NPE/CCE/AIOOBE result plus complete interpreter and paused
state. All 18 Java and 28 Rust method nodes and all 3/4 `itemID` declaration
nodes have exactly one owner.

**Lesson.** Tiny forwarding methods can be transaction boundaries. Crosswalk
each call as a separate operation and compare shared state after downstream
failure; otherwise a reordered implementation can look equivalent on every
successful return.

## A-40 — the InkScript constructor is a caught, partially publishing parser

`InkScript(DataInputStream,String[])` catches every `Exception`, so malformed
input does not mean “no script.” It means a successfully allocated script whose
fields expose every store completed before the fault. The nullable string table
is published before the first input dereference. The 57-entry event table is
published and filled with minus one before the event count is read. The data
array is published as zero-filled storage before `readFully` starts, and a short
stream leaves its successfully read prefix followed by the original zero suffix.

The read schedule matters independently of that field state. Each unsigned
short consumes its high byte before attempting its low byte. More subtly, an
event record evaluates the array, reads the unsigned event code, reads the full
two-byte offset, and only then performs the array-index check for the store.
Thus an event code above 56 consumes all three record bytes before the caught
`ArrayIndexOutOfBoundsException`. Using the format crate's atomic `u16_be`
reader, pre-validating the event code, or parsing into locals and publishing a
finished value would each change retail behavior.

The strict transliteration therefore composes each short from two `Reader::u8`
calls and fills the already-published data array byte by byte. Its 1,122-case
oracle includes null input, nullable/hostile string tables, all 256 graphics
discriminators, every one-byte string index, representative unsigned Integer
IDs, all 256 event codes, record-count boundaries through 255, every selected
truncation point, the maximum 65,535-byte declared data length, and randomized
streams. It compares the graphics value, full event table, entire partial data
array, retained string table, and remaining input count. Both recovered JARs,
canonical Java, and Rust agree. All 119 Java and 305 Rust constructor nodes and
all 3/3 `gfxID` declaration nodes have exactly one owner.

Retail's caught `printStackTrace` is explicitly one-sided: it is diagnostic
stderr, while the production transliteration keeps that diagnostic side effect
outside its typed game-state boundary. The real Java oracles still execute it;
only the game-state owner omits the host output.

**Lesson.** For a constructor with a broad catch, the oracle is the partially
initialized object plus the input cursor—not merely success versus failure.
Translate byte consumption, bounds-check timing, and publication order before
considering a higher-level decoder reusable.

## A-41 — RoomObject construction publishes a usable partial object after any parse fault

`RoomObject(DataInputStream,String[])` first runs three source field
initializers—white `DEFAULT_COLOR`, null text, and paused-animation time minus
one—then enters an empty whole-parser `catch (Exception)`. Inside that boundary
it publishes idle time zero, visibility true, and activity false before the
first input dereference. Null input therefore still returns a non-null object
with those values; a truncated record retains every completed field store.

The outer type switch has two payload shapes. Graphics type one reads signed x
and y shorts, an unsigned transform, and a nested graphics-ID discriminator;
kind one performs a one-based nullable String-array lookup, kind two constructs
an Integer from an unsigned short, and every other kind consumes no ID payload.
Types two through six share signed x/y plus unsigned width/height. Every other
outer type consumes no payload, so the byte immediately after the type is the
script-string index. Index zero deliberately suppresses any dereference of the
possibly null string table; a nonzero invalid index is caught after its byte is
consumed.

The strict translation models all thirty original instance fields in one
`RoomObjectState`, including fields untouched by this constructor, so early
failure has the same reflection-visible zero/false/null defaults as JVM
allocation. Signed and unsigned shorts are consumed as two individual byte
reads and published only after the second succeeds. That choice is deliberate:
an atomic codec helper would retain the same remaining slice on success but the
wrong cursor after EOF, exactly the read-order class highlighted by Gothic's
crosswalk findings.

The 1,454-case constructor matrix covers null input, all 256 outer selectors,
all 256 graphics-ID selectors, signed-short and unsigned-byte edges,
nullable/empty/sparse/hostile UTF-16 tables, every truncation point of three
representative records, and 512 deterministic malformed streams. Both recovered
JARs, canonical Java, and Rust agree on all thirty fields and exact remaining
bytes. The AST ledger independently owns all 153 Java nodes and all 441 Rust
nodes, including the three source initializers, empty catch, explicit JVM
defaults, switch defaults, and checked Rust adaptations.

**Lesson.** A complete constructor comparison must observe allocation defaults,
source-initializer order, branch payload width, the cursor at failure, and the
partially initialized object. Naming fields or matching successful records does
not establish any of those properties; exhaustive AST ownership tells the
oracle which intermediate states must be made observable.

## A-42 — a Java class initializer mixes executable stores with non-executable constants

The canonical `RoomObject.<clinit>` AST contains 37 initializer-expression
nodes, but the retail bytecode executes stores for only fourteen of them. The
first 23 source nodes belong to `static final int` ConstantValue fields: object
types, graphics-ID kinds, animation slots and layout offsets, default color,
and default text-zone width. `javac` records their source initializers, while
the class file exposes them as field attributes and emits no `putstatic` for
them. The remaining nodes initialize mutable class state in exact order:
painting time minus one, the vibration latch true, panel IDs one through seven,
then panel indices zero through four.

The Rust port keeps those models separate. Fourteen newly admitted typed Rust
constants join the nine constructor constants, preventing equal-valued IDs and
offsets from unrelated domains from satisfying later translations. The nine
new mutable values extend the single `RoomObjectStatics` owner; the five
previously admitted mutable panel indices remain in that same owner. None of the
non-final Java fields was incorrectly promoted to a Rust constant.

The startup oracle is intentionally first in every process. Both recovered
JVMs and canonical Java expose their real class-load state once. Rust begins
with hostile maximum/minimum/false values and runs `room_object_initialize`, so
an omitted store remains observable even where a friendly default would have
hidden it. All four authorities agree on the fourteen-value state. The
bytecode/opcode hashes prove the actual `putstatic` sequence, and the crosswalk
owns every one of the 37 Java and 72 Rust nodes, classifying all 23 source-only
ConstantValue nodes rather than inventing runtime operations.

**Lesson.** A source AST is deliberately broader than executable bytecode around
class initialization. Keep both authorities: bytecode determines which stores
run and in what order; the AST requires every compile-time initializer to be
seen, classified, and tied to a typed declaration instead of disappearing from
the proof denominator.

## A-43 — empty startup collections still require allocation and identity evidence

`MenuModel.<clinit>` allocates one empty menu stack. `GameResource.<clinit>`
allocates the evictable-image cache first and a distinct important-image vector
second. An oracle that reported only their zero lengths would admit null-friendly
substitutes, aliasing, or a missing allocation. The Rust oracle therefore starts
all three owners at hostile `None`, runs the Rust initializers, and reports both
length and identity; the Java oracles read the actual class fields. All four
authorities observe the two resource vectors as nonnull, empty, and distinct.

The AST crosswalk supplies the structural proof that the output alone cannot.
It owns both Java nodes and all eleven Rust nodes for the menu initializer, then
all six Java nodes and all twenty-one Rust nodes for the resource initializer.
The resource source tree also contains the `TYPE_GFX` and `TYPE_SFX` literals,
while the retail initializer bytecode contains no corresponding stores because
both are `ConstantValue` fields. Those source-only nodes are explicitly classified
and their declarations map to separate typed Rust constants.

**Lesson.** Even a zero-sized startup value has observable allocation, identity,
publication order, and source/bytecode structure. Use the oracle to expose the
runtime state and exhaustive AST ownership to prove how that state was produced.

## A-44 — body coverage is an AST-to-bytecode join, not an AST-shaped count

The javac walker exposes 353 executable-shaped source items, while the selected
retail classfiles contain 350 methods. Ranking unreviewed ASTs by size initially
put `ResourceRequest.<clinit>` first with five literal nodes. The retail
`LoadRequest.class`, however, has no `<clinit>` method at all: those nodes are
the source initializers of five `static final int` fields represented by
`ConstantValue` attributes. There is therefore no original code hash, opcode
hash, or executable body to transliterate.

The audit denominator must join canonical owner/signature to an actual original
method before a body can be admitted. The five literals remain real source
evidence and must be owned when the corresponding constant declarations are
crosswalked, but counting their synthetic source grouping as a translated method
would overstate executable coverage.

**Lesson.** AST completeness and bytecode completeness constrain different
sets. Never select or count a body from syntax alone; require the original
method join first, then route source-only initializer nodes to declaration
coverage.

## A-45 — a forwarding wrapper must not inherit its callee's coverage

`GameCanvas.paint(Graphics)` contains exactly one operation: forward the same
nullable graphics reference to `Application.paint`. Its Rust translation uses
an injected callback boundary, and the exhaustive crosswalk pairs all six javac
nodes with all six syn nodes. That boundary was intentional: it proved the
one-call wrapper without prematurely claiming the then-unreviewed
`Application.paint` body. Now that the callee is admitted independently, the
same boundary composes the two real Rust bodies while each crosswalk continues
to own only its own nodes.

Six cross-build-stable oracle cases cover both prior painting states, the open
and closed fade guard, nonnull reference identity, and null with the delegate
guard closed. A delegated null argument also exposed a downstream difference:
the selected baseline/canonical path faults in `InkEngine.paint`, while the
naming-reference build reaches its loading-bar exit. The wrapper bytecode is
not the variant, so the oracle excludes only that callee-dependent input instead
of weakening or mislabelling the wrapper claim.

The same boundary applies to `InkEngine.wrapString(String,int)`: all seven Java
nodes and all ten Rust nodes prove one call forwarding the exact text, signed
length, and class-owned current font. Forty-two four-authority cases exercise
null, empty, NUL, isolated-surrogate, plus-bearing, and plain strings. The
three-argument renderer is a reviewed cross-build rendering-policy variant and
remains wholly unclaimed; the Rust callback reproduces only the common
single-line oracle fixture, not that renderer's implementation.

**Lesson.** Treat every unreviewed call as an explicit opaque boundary. Oracle
the wrapper's call count, argument identity, return/failure propagation, and
observable effects only where its downstream authorities agree; leave the
callee's AST denominator untouched until its own tranche.

## A-46 — a print prefix is not observable until its entire Java expression succeeds

`Application.printArray(byte[][])` forwards `(data, 0, data.length)` to its
three-argument overload. The wrapper's null contract comes from evaluating
`data.length` before `invokestatic`; Rust therefore resolves the outer Option
and returns its typed null fault before invoking the callback. All eight javac
nodes and twenty-two syn nodes are exhaustively owned without claiming the
overload.

The first Rust oracle fixture exposed a subtler ordering bug inside that opaque
callee. It appended `"index: "` to the captured stream and then called the
already-reviewed `codedString`. Java instead evaluates the complete
`new StringBuffer().append(index).append(": ").append(codedString(...)).toString()`
argument before `System.out.print` is invoked. If an inner byte array is null,
the failing index contributes no prefix at all. Moving the fixture's capture
write after `codedString` made all 142 new cases agree across both recovered
JARs, canonical Java, and Rust.

**Lesson.** Statement-level similarity is insufficient for side-effecting
expressions. Preserve Java operand evaluation through the call boundary and
make partial output/state visible in the oracle; AST node order tells us which
apparently harmless Rust statement split is actually wrong.

## A-47 — cleanup after a call is not a finally block

`Application.roomRepaintRun()` calls `roomRepaint()` and only then assigns null
to `roomRepaintThread`. The Rust owner now includes that nullable thread handle,
and the translation uses `room_repaint(state)?` before the assignment. All
eight Java body nodes, seventeen Rust body nodes, and both three-node field
declarations have exact ownership; the changed `ApplicationState` container
hash was also ratcheted independently.

The oracle drives the actual nested Java repaint through two controlled paths.
With nonnull Graphics/Image owners and an empty room-object array, repaint
completes and the thread becomes null. With null room Graphics, repaint first
sets its nested repainting flag and then throws; the preexisting thread handle
survives and repaint-needed remains set. Crossing both paths with null and
nonnull initial handles produces the same four observations in both recovered
JARs, canonical Java, and Rust.

**Lesson.** A store after a throwing call is success-only publication, not
unconditional cleanup. Use a hostile preexisting handle and a failing delegate
to distinguish ordinary sequential bytecode from `finally`-style behavior.

## A-48 — a lifecycle callback is an ordered composition, not a merged policy

`GameCanvas.showNotify()` has only two statements: call `Application.setHide(false)`,
then call `resumeSound()`. The Rust transliteration keeps both unreviewed callees
as callbacks and uses `?` on the first. Its nine `syn` nodes crosswalk all nine
`javac` nodes without importing either menu-unhide policy or MMAPI behavior into
the wrapper's coverage claim.

The twelve oracle cases cross both incoming hidden states, enabled/disabled
sound, and loop counts minus one, zero, and one. Clearing `hiddenCanvas` observes
the first call. With sound enabled and loop count minus one, the real Java
`resumeSound` enters `playSound`, publishes loop count zero, and then catches the
deliberately induced null-ID failure; that makes the second call independently
visible without requiring an audio host. Both recovered JARs, canonical Java,
and Rust agree. A focused Rust test also injects failure at each opaque boundary
and proves the second call is skipped if the first fails.

**Lesson.** Small wrappers still need node-complete admission. Model each
unreviewed callee as its own ordered, fallible boundary; do not collapse a
lifecycle wrapper into a new combined host policy that cannot preserve Java's
exception cut points.

## A-49 — “clear all” is sequential and partially committing, not atomic

`Application.clearAllRMS()` first calls `resourceClear()` and only after that
returns clears `InkScript.list`. The Rust transliteration retains
`resourceClear` as an opaque fallible callback: that proves this wrapper's call
order without claiming the still-unreviewed resource-clearing implementation.
All nine `javac` nodes and twenty-four `syn` nodes are exhaustively owned,
including the explicit Rust errors for callback failure and a null script
registry.

Six four-authority cases cross resource success/failure with a null, empty, or
two-entry script registry. The deliberately failing resource path publishes its
heap-derived resource state before it faults and leaves the script registry
untouched. On the successful resource path those resource updates remain
visible, then a null script registry faults or a nonnull registry is cleared.
The focused Rust test independently proves that callback failure skips the
clear and that the registry dereference happens only after callback success.

**Lesson.** A name such as “clear all” does not imply a transaction. Preserve
partial state produced by the first call and the exact failure cut before the
second mutation; an oracle must expose both owners, while the AST crosswalk
keeps the unreviewed callee outside the coverage claim.

## A-50 — nondeterministic native results make structural evidence indispensable

`Application.freeMemory()` first calls `System.gc()`, then resolves the nullable
class-owned `Runtime`, invokes `freeMemory()`, and returns its `long`. Raw free
heap bytes cannot be expected to match across three separate JVM processes and
a Rust host: process layout, prior allocations, and collector timing are all
independent. The two four-authority cases therefore compare only the stable
observable contract—a null Runtime faults and a real Runtime reports a
nonnegative value.

That normalization does not weaken the structural proof. The exact original
code/opcode hashes and exhaustive nine-node `javac`/twenty-node `syn` crosswalk
lock the collection-before-field-read schedule. Rust owns the Runtime as a
nullable opaque handle and exposes collection and sampling as two callbacks. A
focused test records their order, proves null skips only sampling, checks handle
identity, and forwards `i64::MIN` unchanged, independently establishing that the
game body does not clamp or reinterpret the returned Java `long`.

**Lesson.** Normalize only an intrinsically nondeterministic observation, and
replace the lost comparison strength with stronger orthogonal evidence. Here
the live oracle proves stable outcomes, while bytecode and every AST node prove
that `System.gc()` was neither removed nor moved after the heap measurement.

## A-51 — a nested platform-call expression has two independent failure cuts

`Application.setDisplay(Displayable)` is one Java expression but two ordered
MIDP calls: read the nullable class-owned MIDlet and call
`Display.getDisplay(midlet)`, then invoke `setCurrent(displayable)` on the exact
returned Display. Rust keeps both platform methods as callbacks and owns the
MIDlet as an opaque nullable handle; a typed three-way error distinguishes a
throwing lookup, a null returned receiver, and a throwing publication call.

The instrumented declaration stub records both call counts and the identities
of the MIDlet, returned Display receiver, and nullable Displayable. Twenty-four
four-authority cases cross null/non-null arguments, successful, null-returning,
and throwing lookup modes, and successful/throwing publication. A failed
`setCurrent` still exposes its attempted receiver and argument, while either
lookup failure leaves the second call unreachable. All nine `javac` and
thirty-one `syn` body nodes plus both three-node MIDlet declarations are owned.

**Lesson.** Do not translate a chained call as one convenient host operation.
Split it at the same receiver-producing boundary as the JVM, retain exact
identity across that boundary, and make each failure cut observable without
claiming either platform implementation as translated game code.

## A-52 — constructor allocation and constructor-body effects are separate evidence

`GameCanvas.<init>()` does not allocate the canvas. Its eleven-byte retail body
first invokes the Java ME `GameCanvas(boolean)` superclass constructor with
`false`, then calls `setFullScreenMode(true)` on the same `this`. Rust therefore
accepts an already allocated opaque canvas identity and exposes the two platform
operations as ordered callbacks instead of inventing a Rust-owned canvas object.

Four hostile cases inject failure independently at both calls. Instrumented
Java ME stubs and the Rust oracle record both boolean arguments, both call
counts, and receiver identity. A superclass failure prevents the fullscreen
call; a fullscreen failure occurs only after successful superclass construction.
The oracle also resets injected stub state after each request, because failure
hooks leaking into unrelated later cases would make the harness—not the game
body—the source of behavior. Both recovered JARs, canonical Java, and Rust agree
on all four cases.

The original code and opcode digests lock the two `invokespecial`/`invokevirtual`
edges. All nine `javac` and twenty-five `syn` nodes have exactly one semantic
owner or an explicit Rust-only `Result` adaptation; the two error variants and
their enum container are independently hash-locked by the reverse declaration
inventory.

**Lesson.** Treat object allocation as caller work when transliterating a JVM
constructor. Preserve the constructor body's exact receiver, argument, order,
and failure cuts, and ensure hostile oracle instrumentation is isolated between
requests.

## A-53 — a thin wrapper still owns call-before-store ordering

`SilentHillGame.menuResetIngameValues()` first delegates to the larger, not-yet-
admitted `InkEngine.menuResetIngameValues()`, then writes the HUD ammunition
width sentinel `-1`, then raises the update-needed latch. Rust keeps the callee
as an explicit callback and introduces `SilentHillGameStatics` as the sole owner
of the two game-level fields; it does not absorb the engine reset into this
twelve-byte wrapper.

Twenty cases run through all four authorities, seeding the width across signed-int edges and the latch
with both booleans. A hostile engine-margin marker becomes exactly four, proving
the delegated reset ran, while both wrapper fields end at their exact constants.
A focused Rust fault case complements that live comparison: if the delegated
callee fails, neither later store occurs. Thus the partial-state boundary is
locked even though the full engine reset has not yet entered the transliteration
ratchet.

All thirteen `javac` and twenty `syn` body nodes have exactly one owner or a
reasoned `Result` adaptation. Both three-node Java field declarations map
exhaustively to their two-node Rust fields, and the new owner container is
independently hash-locked.

**Lesson.** Do not inline or prematurely claim a large unreviewed callee merely
because a leaf wrapper invokes it. Preserve the call edge and exact subsequent
stores now; replace the callback with the separately admitted callee when its own
body reaches the frontier.

## A-54 — observe only stable callee facts when admitting a wrapper

`SilentHillGame.appInit()` loads `gfx/menu_logo.png`, publishes the nullable
image into `INK_menu_logo`, and only then enters the much larger
`InkEngine.appInit()`. Rust represents the image as a nullable opaque handle and
passes the exact seventeen UTF-16 code units to a loader callback; the engine
initializer remains a second callback and therefore outside this wrapper's
semantic claim.

The two old-logo cases prove exact load count, normalized requested path, new
image identity, and publication before the engine callback. For a controlled
failure cut, both Java harnesses force a null MIDlet, so the engine initializer
starts and then faults after its initial canvas setup; Rust injects the same
post-publication failure. The focused Rust test also covers successful engine
completion and a loader returning null.

This exercise exposed a genuine build difference inside the unreviewed callee:
the selected baseline writes canvas width 130, while canonical/source-named Java
writes 240. The wrapper oracle intentionally compares only the stable fact that
the hostile width changed, not its variant value. Original appInit hashes still
prove the exact load-store-call schedule, and all twelve `javac`, thirty-five
`syn`, three Java field, and three Rust field nodes are exhaustively owned.

**Lesson.** A wrapper's oracle may use a callee side effect to prove the call
edge, but it must not silently promote variant callee internals into the
wrapper's contract. Normalize the smallest explicit invariant and leave the
callee for its own bytecode- and AST-bound admission.

## A-55 — a catch-all leaf is better expressed as explicit branch ownership

`GameCanvas.keyJadEntryAsInt(String)` dereferences the class-owned MIDlet,
forwards the nullable key to `getAppProperty`, parses the nullable property with
Java's decimal `Integer.parseInt`, and catches every `Exception` from that
entire chain as zero. Rust makes the implicit fault sites explicit: a missing
MIDlet or failed lookup returns zero immediately, and the already admitted
`parse_java_i32` helper maps null, malformed, and overflowing values to the same
zero fallback.

Two hundred four-authority cases cross null/non-null MIDlets; null, empty, NUL,
ordinary, and isolated-surrogate keys; successful and throwing lookups; and
null, empty, signed-bound, overflow, whitespace, plus-prefixed, and Arabic-
Indic-decimal property values. The Java MIDlet stub and Rust callback also
record the exact receiver and key, while a focused test proves null receiver
suppresses the platform call.

All nineteen `javac` and thirty-one `syn` nodes are owned exactly once. The
Java-only try/block wrapper is documented as the structural counterpart of the
Rust Option/Result pattern branches; the successful lookup/parse path and every
zero-return path are paired separately. The parse helper's own 193-node AST is
not double-claimed—it remains under its earlier method admission.

**Lesson.** Transliterate broad Java catches by enumerating the actual fault
sites and their common fallback. Reusing an already verified helper is sound,
but the new body's audit should own only its call edge, never the helper AST a
second time.

## A-56 — admitted wrappers compose without merging their AST claims

`Application.paint(Graphics)` reads the mutable signed `FADE_FRAMES` and
`DEMO_FRAMES` fields and delegates to `InkEngine.paint` exactly when the first
is less than or equal to the second. The Rust body keeps that inclusive signed
comparison visible and forwards the exact graphics value through an injected
paint callback. Its focused test covers equality, the skipped greater-than
case, exact argument identity, and delegated failure propagation.

The six existing `GameCanvas.paint` oracle cases now execute
`game_canvas_paint` and `application_paint` compositionally. They still compare
both recovered JARs, canonical Java, and Rust on the wrapper's observable
contract, but the audit does not merge coverage: the six wrapper nodes remain
owned by the wrapper entry and the twelve Java/twenty Rust callee nodes remain
owned by the callee entry. `FADE_FRAMES` and `DEMO_FRAMES` additionally receive
complete four-to-two declaration crosswalks in the same admission.

**Lesson.** Once a callback-delimited callee is admitted, route the wrapper's
oracle through the real callee body. Runtime composition strengthens the
evidence while separate manifests preserve exact per-body AST ownership.

## A-57 — catch subtype order is part of a boolean wrapper's contract

`Application.rmsDelete(String)` forwards the nullable UTF-16 name once to
`RecordStore.deleteRecordStore`. A successful deletion returns true; the first
catch treats `RecordStoreNotFoundException` as the same successful result,
while the following `RecordStoreException` catch returns false. Rust represents
those two caught categories separately and retains an explicit generic arm for
unchecked failures that Java lets escape.

Thirty-two four-authority cases cross null, empty, NUL, isolated-surrogate,
noncharacter, ordinary, path-like, and multilingual names with success,
not-found, generic RMS failure, and an uncaught `NullPointerException`. The Java
stub records reference identity and call count, and the focused Rust test uses
pointer identity to prove the same forwarding rather than mere string equality.
All twenty-four `javac` and twenty-seven `syn` nodes are owned exactly once.

**Lesson.** Do not flatten an exception hierarchy into one host error. Catch
order can change an ordinary return value, and errors outside the declared
catches must remain observable rather than being silently converted.

## A-58 — a swallowed wrapper still has an observable call schedule

`Application.saveChunkINI(DataInputStream)` first passes the nullable stream to
`inkServerGetBytes`, then passes that exact nullable byte-array result with the
fixed UTF-16 name `RMS_chunkINI` to `rmsSet`. Its outer catch swallows any
`Exception` from either step and the boolean returned by `rmsSet` is discarded.
The Rust body uses an explicit `Result` boundary for each unreviewed callee,
returns after collection failure, and deliberately ignores publication result
or failure.

Ten four-authority cases cross null, empty, and three-byte streams, failures
from `available` and `read`, and successful or failing record-store opens. The
instrumented RMS stub records the exact name, create flag, byte content,
offset, and length. Focused Rust tests additionally prove stream and byte-slice
identity, the get-before-set order, forwarding of a null byte result even when
the setter returns false, suppression of the setter after collection failure,
and swallowed setter failure. All fifteen `javac` and thirty-one `syn` nodes
are owned exactly once.

**Lesson.** An empty catch does not make a wrapper untestable. Record the
downstream attempts and partial effects, then use structural evidence for call
edges whose callee intentionally leaves no external trace.

## A-59 — allocation and array copy have separate failure cut points

`Application.resourceMakeSubChunk()` reads `resourceSCCurrentSize` twice: once
to allocate a distinct zero-filled destination and again as the final
`System.arraycopy` argument. A negative first read therefore raises
`NegativeArraySizeException` before the source is evaluated. After allocation,
a null `resourceSCData` raises `NullPointerException` even when the copy length
is zero, while a length outside either array raises
`IndexOutOfBoundsException`. Success returns only the copied source prefix in a
new array, with no aliasing or source mutation.

The original bytecode evaluates `resourceSCData` before the second size read,
then performs the null and bounds checks inside `System.arraycopy`. The Rust
translation retains that order and copies into `subchunk[..copyLength]`, so an
interleaved smaller second size leaves the remainder of the allocation zero
instead of requiring both whole arrays to have equal lengths.

Twenty-eight four-authority cases cross null and differently sized sources with
negative, zero, in-bounds, exact-bound, and oversized lengths. They distinguish
all three failures, returned-array identity, copied bytes, and unchanged source
state. The crosswalk owns all nineteen `javac` and 108 `syn` body nodes exactly
once, including Rust's explicit JVM allocation-fault adaptation, as well as both
new field declarations.

**Lesson.** Preserve repeated static reads and the boundary between allocation
and a following platform primitive; otherwise exception order and identity can
look correct on ordinary inputs while diverging on hostile states.

## A-60 — a repaint guard does not make synchronous callbacks atomic

`Application.repaintCanvasIfPossible()` reads the mutable `painting` guard and
returns without touching `canvas` when it is already true. Otherwise it stores
true before the first canvas read, calls `repaint`, then reads `canvas` again
before calling `serviceRepaints`. The two reads are deliberately distinct:
synchronous repaint reentrancy may clear or replace the class field between
them, so the second call can receive a different canvas identity or fail on a
newly null reference.

Rust exposes four exact failure boundaries: null before repaint, repaint
failure, null before service-repaints, and service-repaints failure. Neither
callback-triggered changes to `painting` nor canvas replacement are restored or
rechecked by this body. In particular, publishing the guard survives every
first-call failure, a repaint failure suppresses the second field read, and a
successful repaint can alter both state fields before the second operation.

Nineteen baseline/canonical/Rust cases cover the pre-set guard, first and second
nulls, both callback failures, clear/replacement reentrancy, callback guard
changes, and guarded recursive invocation. The naming-reference result is
excluded for only this command through its hash-locked baseline-only
`rendering-policy` row, because that build omits `serviceRepaints` and adds
key-flag behavior. Exact receiver traces and final guard/canvas state make the
call schedule observable. All eighteen `javac` and fifty-eight `syn` body nodes
plus both field declarations are owned exactly once.

**Lesson.** A reentrancy flag is an ordered store, not an atomic section. Preserve
repeated static reads and every synchronous callback cut point; callback writes
remain visible unless the Java body explicitly restores them.

## A-61 — a short-circuit wrapper can expose a pending callee without claiming it

`GameCanvas.resumeSound()` reads `Application.curSoundMode` first and touches no
canvas sound state when that guard is false. When enabled, it reads `loopCount`
for an exact comparison with the GameCanvas-owned `KEY_UP` value minus one. Only
inside that branch does it read `soundID`, re-read `loopCount`, and call
`playSound` once. Rust keeps both static reads visible, borrows the nullable
UTF-16 ID without transcoding or copying, and propagates a callback failure
because this wrapper has no catch.

The direct oracle invokes the actual canonical and recovered `resumeSound`
methods. Its 120-case Cartesian matrix crosses both sound modes, six signed loop
values from `Integer.MIN_VALUE` through minus one to `Integer.MAX_VALUE`, both
`FirstLoad` states, and null, empty, `ls`, case-near-miss `lS`, plus an isolated
surrogate ID. The already stable but still-pending `playSound` body supplies
observable entry markers: its `FirstLoad && id.equals("ls")` branch retains the
sound fields, null can fault before its internal catch, and its normal path
publishes null/zero before swallowing later media/resource failures. No naming-
reference exclusion is needed.

The oracle adapter models those callee-owned final statics separately from the
wrapper's immutable borrowed input; that borrow proves the argument identity
without pretending the callee body has entered coverage. A focused Rust test
independently proves both guard suppressions, null and hostile UTF-16 forwarding,
one callback, the second loop read, and uncaught failure propagation. All fifteen
`javac` and thirty-four `syn` body nodes are owned exactly once, together with
the three mutable fields and the typed, GameCanvas-scoped constant.

**Lesson.** Use stable entry-visible effects of a pending callee to prove a
wrapper's call edge, but keep the callback boundary and declaration ownership
explicit so oracle evidence cannot silently promote the callee's AST.

## A-62 — an empty catch is not a `finally` block

`Application.rmsGet(String)` initializes its result to null, opens the named
record store with `create=false`, fetches record ID one, publishes that exact
byte-array reference, and only then closes the store. One `Exception` handler
covers all three calls. Consequently an open failure returns null without get
or close, a get failure returns null without close, and a caught close failure
returns the already-published array. The close is ordinary code inside the try,
not cleanup guaranteed by a `finally` block. Failures outside the caught
`Exception` domain still propagate at each boundary and a close-time `Error`
therefore suppresses the otherwise ready return.

The Rust seam represents three caught `Exception` categories separately from an
uncaught failure, borrows the store for `getRecord`, and consumes it for the
conditional close. Its successful data path moves the exact `Vec` returned by
the callback without copying. The 3,000-case four-authority matrix crosses eight
nullable and hostile UTF-16 names, success, two RMS exceptions,
`AssertionError`, and `NullPointerException` independently at open, get, and
close, plus null, empty, or binary record data. It compares callback
order and counts, exact name and data identity, the fixed false flag and record
ID one, returned bytes, and propagated status. All thirty-four `javac` and
ninety `syn` body nodes are owned exactly once, together with the four variants
and container of the typed callback-error enum.

**Lesson.** Recover the exception-table range before translating resource code.
Replacing this shape with unconditional cleanup changes the observable call
schedule, while assigning the result after close loses a successfully fetched
array when close throws a caught exception.

## A-63 — a wrapper may discard a result without discarding its call edge

`InkEngine.removeSavedGameFromRMS(String)` builds a new name by appending the
nullable game ID to the exact UTF-16 prefix `RMS_variables_`, calls the already
admitted `Application.rmsDelete` once, and discards its boolean. Java
`StringBuffer.append(String)` contributes the literal code units `null` for a
null reference; empty and hostile UTF-16 inputs are otherwise copied unchanged.
The following `toString` materializes a distinct nonnull `String`, so the
downstream argument cannot alias even a nonnull input game ID.

Rust factors the transient `StringBuffer` and final `String` allocation into one
owned `Vec<u16>` initialized from an InkEngine-scoped fourteen-unit constant.
The four-authority oracle composes the real admitted Rust `application_rms_delete`
body beneath this wrapper. Its forty-five cases cross nine nullable and hostile
IDs with success, not-found, another RMS failure, `NullPointerException`, and
`AssertionError`. Every case observes the complete constructed name, exactly one
delete attempt, and distinct argument identity. Both true and false callee
returns complete normally, while failures outside the callee's typed catches
propagate. All fifteen `javac` and forty-three `syn` body nodes are owned exactly
once, together with all four Java and fifteen Rust prefix-declaration nodes.

**Lesson.** Discarding a return value is not permission to replace or omit the
call. Compose an admitted callee in the oracle, then separately prove argument
construction, allocation identity, call count, and uncaught-failure propagation
at the wrapper boundary.

## A-64 — two reads of one static field are two observable operations

`InkEngine.menuPaintCurrentIngame()` first reads `Menu.stack`, invokes `size()`,
and saves that signed result. Only when the saved value is positive does it read
the static field again, invoke `elementAt(savedSize - 1)` on that second receiver,
cast the returned value, and call `menuPaintIngame`. The second field read is not
an optimization detail: a hostile `Vector.size()` override can clear the first
receiver or replace the static with null or another vector before returning.

The twenty-case four-authority oracle records the size receiver, mutation or
replacement, saved value, second receiver, requested index, returned identity,
whether downstream painting was entered, and final static identity. It covers
nonpositive suppression, a null replacement that fails before `elementAt`, NPE
and Error at each callable boundary, invalid saved indices, wrong-class CCE,
null surviving `checkcast`, and selection of the exact last object from a
replacement stack. A following initialization request proves the Java oracle
restores the stack and graphics statics it temporarily replaces. Rust therefore passes the mutable
`MenuStatics` owner separately to the size and element adapters and carries the
saved `i32`; it does not cache a stack borrow or recompute its length. All twenty-seven
`javac` and thirty-seven `syn` nodes have one atomic owner, while explicit try
and successful-unit nodes remain categorized representation adapters.

**Lesson.** When bytecode repeats `getstatic` around an overridable call, model
both reads. A convenient retained borrow silently erases mutations that Java can
observe between them; the oracle must make receiver replacement part of the
result, not merely compare the ordinary final value.

## A-65 — wrapper truth may depend on reference nullness, not payload length

`InkEngine.savedGameExistsInRMS(String)` constructs the saved-game record name
with the same ordered `StringBuffer` operations as the deletion wrapper, then
calls the already admitted `Application.rmsGet` exactly once. Its result is the
reference comparison `record != null`: an empty byte array means that the saved
game exists, while only a null reference means that it does not. The wrapper has
no catch of its own, so an `AssertionError` escaping open, get, or close must
still propagate.

Rust retains the distinct final Java `String` as a newly owned UTF-16 vector and
tests `Option<Vec<u8>>::is_some`, never its length. The 3,375-case four-authority
matrix crosses nine nullable and hostile game IDs with every combination of the
five reviewed open, get, and close outcomes and null, empty, or binary record
data. It observes the exact `RMS_variables_` name, non-aliasing from the input,
`create=false`, record ID one, all three call counts, the returned boolean, and
propagated status. This also proves that caught open/get failures yield false,
caught close failures retain a fetched result, and an error suppresses any
otherwise-ready result. All seventeen `javac` and forty-two `syn` nodes have one
atomic decision under the generic crosswalk gate.

**Lesson.** Do not translate Java reference existence as container non-emptiness.
Compose the admitted callee, keep allocation identity observable, and include an
empty-but-nonnull result in the oracle so `is_some()` cannot regress into
`!is_empty()`.

## A-66 — a catch does not protect the retry executed by its handler

`Application.exit()` clears `runtime`, clears `appInited`, calls the admitted
empty `resourceExit`, reads `midlet`, and invokes `notifyDestroyed()` inside one
region protected by `catch (Exception)`. If that first attempt raises an
`Exception`, the handler discards it, reads `midlet` again, and invokes the same
method once more. The handler's retry is outside its own protected range: an
Exception from the second attempt propagates, while an `Error` from the first
attempt bypasses the handler and prevents any retry.

Rust owns `appInited` beside the already mapped runtime and MIDlet handles and
uses a typed three-way exit error: null receiver, catchable notification
Exception, or uncaught notification failure. Its second receiver is resolved
from live state after the first callback returns. The 160-case direct oracle
crosses both initial runtime and initialized states, null/present MIDlet, five
first/second failure schedules, and hooks that replace or clear the MIDlet or
restore hostile lifecycle values. It compares both entered receiver identities,
call count, final MIDlet identity, final lifecycle state, and propagated status.
This proves first-error suppression, first-Error non-retry, retry propagation,
and the absence of any post-callback state restoration. The existing
`destroyApp(boolean)` oracle now composes this admitted body beneath its
forced-flag-ignoring wrapper. All twenty-seven `javac` and seventy-three `syn`
nodes have one atomic decision.

**Lesson.** Reconstruct the exception-table interval, not just source indentation.
Code written inside a catch handler is not recursively covered by that handler,
and a callback between two static reads can change the retry receiver and every
state value that the method wrote earlier.

## A-67 — `readBoolean` is a one-byte zero/nonzero test with a caught empty-input path

`Application.loadSoundMode()` calls the already admitted `rmsGet` once with the
interned literal `soundRecordStore`. A null result leaves `curSoundMode`
unchanged. For a nonnull result the method wraps the exact returned array, reads
one Java boolean, assigns the flag, and closes the byte-array stream.

The important boundary is the empty array: `DataInputStream.readBoolean()`
throws `EOFException`, the enclosing `IOException` catch swallows it, and the
old flag survives. A present first byte of zero clears the flag; every other
unsigned byte value sets it, and trailing bytes are never read. Ordinary RMS
failures already swallowed by `rmsGet` therefore behave like a null result,
including open/get failures. A caught close failure inside `rmsGet` retains the
fetched array and it is still decoded. An `Error` at any RMS boundary bypasses
both methods' `Exception`/`IOException` catches and reaches the caller before
the sound flag is assigned.

The direct oracle crosses both initial flag values with all five open, get, and
close outcomes and eight null/empty/tail-bearing payloads, then adds every
remaining first-byte value. Its 2,506 cases record the exact opened name,
create flag, record ID, call counts, final mode, and propagated status. The AST
crosswalk separately locks the Java string literal to the exact sixteen UTF-16
code units in Rust; all thirty-nine `javac` and sixty `syn` nodes have one
decision.

**Lesson.** Do not translate `readBoolean` as “data exists” or “first byte is
one.” Preserve the empty-stream exception path and the full nonzero byte domain,
and keep a wrapper's catch type narrower than the callee's unchecked failures.

## A-68 — modified UTF has a nullable failure path distinct from an empty string

`Application.getLanguage()` initializes its result to null, calls the admitted
`rmsGet` with the interned `languageRecordStore` literal, and only constructs an
input view for a nonnull byte array. It then reads one Java modified-UTF record.
A missing record therefore returns null, while the valid two-byte length prefix
`00 00` returns a distinct empty string.

Truncated length prefixes, payloads shorter than the declared unsigned length,
invalid leading bytes, and malformed continuation bytes make `readUTF` throw an
`IOException`; the method catches it and returns the still-null result. Valid
NUL, surrogate, and noncharacter code units remain exact UTF-16 values. Bytes
after the first complete record are ignored. As with `loadSoundMode`, caught
open/get RMS failures look like a null record, a caught close failure retains
the fetched bytes, and an `Error` at any RMS boundary propagates before decode.

The existing `read_modified_utf` helper already has its own exhaustive AST
ownership and 72,119-case decoder tranche. The wrapper adds 68,739 direct cases:
all five outcomes at each RMS phase over representative records, every one-code-
unit UTF-16 value, all 2,048 leading-byte/continuation-edge payloads, and declared
length truncations through 65,535. The oracle also records exact RMS name,
create flag, record ID, call counts, returned UTF-16 units, and propagated status.
All forty-five wrapper `javac` and fifty-two wrapper `syn` nodes have one decision.

**Lesson.** Preserve null, empty, malformed, and valid-empty as separate states.
Reusing an admitted decoder is legitimate composition, but its call node and the
caller's narrower catch/result policy still need their own AST ownership and
direct original-JAR oracle.

## Verified clean so far

- 167/350 bodies are bytecode-bound and have complete, non-overlapping `javac`
  and `syn` node ownership.
- 185/1,075 Java fields have complete declaration-node ownership: 150 map into
  sixteen hash-locked Rust owner containers, thirty-five map to typed scalar constants,
  and one mutable array also owns a separately inventoried initializer template.
- The differential currently runs 1,069,323 cases against the recovered baseline,
  canonical Java, and Rust; the naming-reference JAR agrees on all 1,063,108
  cases, with 6,215 requests excluded only by its two ledger-reviewed
  input-timing variants and one ledger-reviewed rendering-policy variant.
- Exhaustive subdomains include all Java `char` values, every pair of singleton
  unsigned comparator bytes, all 256 signed menu-scroll counter states, and all
  256 Ink command-header bytes against every target command.
- The key converters are tested with null, empty, duplicate, randomized, and
  collision-heavy mutable mappings so branch precedence is observable.
