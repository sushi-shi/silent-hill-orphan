# Split packaged-runtime layout

Twenty-five payloads put the real MIDlet in `sh/M.class` and keep two game
classes in the default package. `sh/M` extends `MIDlet` and owns runtime,
resource, persistence, sound, and display behavior; it is game code, not a
bundled handset adapter.

Other packaged classes in these JARs are Nokia, Motorola, or Alcatel platform
compatibility layers. The class classifier explicitly includes `sh/M` and
excludes those adapters, preventing 69--78 game methods per build from falling
out of the preservation denominator again.
