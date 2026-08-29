# Obfuscated engine layout

Eighty-eight payloads keep `M` as the MIDlet and distribute the engine over
ten to twelve game classes. The large `ExtBase`, `Ext`, resource, room, menu,
script, interpreter, and canvas roles remain structurally visible, while
constant holders and small helpers may be folded or removed by optimization.

This lineage intentionally does not import its short names into canonical
Java. It supplies device/content variants and corroborating behavior; the
source-named baseline remains the exact AST authority.
