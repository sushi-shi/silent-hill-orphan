# Source-named layout

Two payloads preserve all thirteen author class names: `M`, `MyCanvas`, `Ext`,
`ExtBase`, `Cheat`, `LoadRequest`, `Menu`, `Resource`, `RoomObject`, `Script`,
`ScriptThread`, `s`, and `txt_consts`.

The 350-method payload is the canonical baseline. The 355-method semantic
reference adds vibration persistence/device calls and a sprite-based
`drawRegion` fallback, while retaining every baseline member name and
descriptor. `source-named-variants.toml` locks the exact common and varying
method sets.
