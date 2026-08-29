# Common recovered engine

All three source layouts implement the same INK adventure-game domains:
application lifecycle, canvas/input/audio, room objects, resources, menus,
scripts, and the interpreter. This layer is a semantic composition model, not
a claim that differently optimized classfiles came from one byte-identical
source checkout.

The source-named builds preserve the domain boundaries directly. The
obfuscated builds preserve them through class surfaces and calls, while the
split-runtime builds merge most of them into `sh/M`, `a`, and `b`.
