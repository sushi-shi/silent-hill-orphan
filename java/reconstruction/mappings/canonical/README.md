# Canonical semantic mapping

These reviewed inputs bind the Java reconstruction to the selected original
bytecode and to an independent build that retains the same author symbol names.

- `classes.toml` accounts for every original class and records its role.
- `mappings.tiny` accounts for every original field and non-initializer method.
- `literals.toml` explains every non-structural executable numeric value; its
  unexplained budget is zero.
- `numeric-shape.toml` records reviewed compiler-level arithmetic differences.

`tools/java/validate_java_identifiers.py` walks the real `javac` AST and locks
the declaration/role denominator while rejecting both short and compound JADX
identifier families. Loop counters and catch variables are structural roles,
not an exemption for synthetic locals elsewhere.

The mapping is evidence consumed by validators. It is not a generated Java
source tree and it does not claim that a retained author name is perfect English.
