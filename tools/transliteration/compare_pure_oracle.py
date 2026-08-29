#!/usr/bin/env python3
"""Compare 156 methods across recovered JARs, canonical Java, and Rust."""

from __future__ import annotations

import argparse
import itertools
import random
import subprocess
import sys
import tempfile
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "java"))

import compile_named_java  # noqa: E402

sys.path.insert(0, str(ROOT / "tools" / "corpus"))
import corpus  # noqa: E402

HARNESS = ROOT / "tools" / "transliteration" / "OrphanJavaPureOracle.java"
ORIGINAL_HARNESS = (
    ROOT / "tools" / "transliteration" / "OrphanOriginalPureOracle.java"
)
RUST_BINARY = ROOT / "target" / "debug" / "orphan-pure-oracle"

# These methods are explicitly classified as input-timing-policy variants in
# java/reconstruction/variants/source-named.toml. The production transliteration
# targets the selected baseline; the naming-reference JAR has extra logo/time
# guards and therefore remains an authority only for the non-variant requests.
BASELINE_ONLY_METHODS = {
    "key-pressed": ("GameCanvas", "keyPressed", "(I)V", "input-timing-policy"),
    "key-released": ("GameCanvas", "keyReleased", "(I)V", "input-timing-policy"),
}
BASELINE_ONLY_COMMANDS = frozenset(BASELINE_ONLY_METHODS)


def validate_baseline_only_scopes() -> None:
    ledger_path = ROOT / "java" / "reconstruction" / "variants" / "source-named.toml"
    ledger = tomllib.loads(ledger_path.read_text(encoding="utf-8"))
    reviewed = {
        (
            owner["canonical_owner"],
            variant["name"],
            variant["descriptor"],
            variant["classification"],
        )
        for owner in ledger["classes"]
        for variant in owner.get("variants", [])
    }
    missing = set(BASELINE_ONLY_METHODS.values()) - reviewed
    if missing:
        raise RuntimeError(
            f"baseline-only oracle scopes lack reviewed variant rows: {sorted(missing)}"
        )


def requests() -> list[str]:
    edges = [
        -(2**31),
        -(2**31) + 1,
        -65536,
        -1,
        0,
        1,
        20,
        32767,
        65536,
        2**31 - 1,
    ]
    result: list[str] = [
        "cheat-init",
        "ink-script-init",
        "room-object-init",
        "menu-init",
        "game-resource-init",
    ]
    for left in edges:
        for right in edges:
            result.append(f"min {left} {right}")
            result.append(f"max {left} {right}")
    for value in edges:
        result.append(f"abs {value}")
        result.append(f"dir {value}")
    rng = random.Random(0x53484F)
    geometry = [*edges, *(rng.randint(-(2**31), 2**31 - 1) for _ in range(96))]
    for index in range(512):
        values = [geometry[(index * step + step) % len(geometry)] for step in range(1, 6)]
        transform = (index % 12) - 2
        arguments = " ".join(map(str, [*values, transform]))
        result.append(f"left {arguments}")
        result.append(f"top {arguments}")

    result.extend(["resource-exit", "description"])
    result.extend(
        f"destroy-app {forced} {midlet_present}"
        for forced in (0, 1)
        for midlet_present in (0, 1)
    )
    result.extend(f"pause-app {hidden}" for hidden in (0, 1))
    result.extend(f"app-start {mode}" for mode in (0, 1, 2))
    result.extend(
        f"resource-restart-importants {length}"
        for length in (-1, 0, 1, 12, 127)
    )
    result.extend(f"char {value}" for value in range(1 << 16))

    def byte_token(value: bytes | None) -> str:
        if value is None:
            return "null"
        return "-" if not value else f"h{value.hex()}"

    def byte_arrays_token(values: list[bytes | None] | None) -> str:
        if values is None:
            return "null"
        if not values:
            return "-"
        return "a" + ",".join(byte_token(value) for value in values)

    byte_values = [bytes([value]) for value in range(1 << 8)]
    for left in byte_values:
        for right in byte_values:
            result.append(f"cmp {byte_token(left)} {byte_token(right)}")
    comparator_edges: list[bytes | None] = [
        None,
        b"",
        b"\x00",
        b"\xff",
        b"\x00\x00",
        b"\x00\xff",
        b"\xff\x00",
        b"\xff\xff",
        b"abc",
        b"abcd",
        b"abd",
    ]
    for left in comparator_edges:
        for right in comparator_edges:
            result.append(f"cmp {byte_token(left)} {byte_token(right)}")
    for _ in range(1024):
        left = bytes(rng.randrange(256) for _ in range(rng.randrange(33)))
        right = bytes(rng.randrange(256) for _ in range(rng.randrange(33)))
        result.append(f"cmp {byte_token(left)} {byte_token(right)}")

    def object_array_token(values: list[int | None] | None) -> str:
        if values is None:
            return "null"
        return "-" if not values else "o" + ",".join(
            "n" if value is None else str(value) for value in values
        )

    object_arrays: list[list[int | None] | None] = [
        None,
        [],
        [None],
        [1],
        [1, 2, 3],
        [1, None, 2],
        [1, 1, 2],
        [0, 1, 2, 3, 4, 5],
    ]
    integer_extremes = [-(2**31), -1, 0, 1, 2, 2**31 - 1]
    for source_index, source in enumerate(object_arrays):
        for target_index, target in enumerate(object_arrays):
            for pattern in range(20):
                source_start = integer_extremes[
                    (pattern + source_index) % len(integer_extremes)
                ]
                target_start = integer_extremes[
                    (pattern * 2 + target_index) % len(integer_extremes)
                ]
                size = integer_extremes[(pattern * 3) % len(integer_extremes)]
                result.append(
                    "array-copy-string "
                    f"{object_array_token(source)} {source_start} "
                    f"{object_array_token(target)} {target_start} {size} 0"
                )
    for target in object_arrays:
        length = -1 if target is None else len(target)
        positions = [-(2**31), -1, 0, 1, max(0, length - 1), length, length + 1, 2**31 - 1]
        sizes = [-1, 0, 1, max(0, length - 1), length, length + 1]
        for source_start in positions:
            for target_start in positions:
                for size in sizes:
                    result.append(
                        "array-copy-string "
                        f"null {source_start} {object_array_token(target)} "
                        f"{target_start} {size} 1"
                    )

    def utf16_token(units: list[int] | None) -> str:
        if units is None:
            return "null"
        return "-" if not units else "u" + "".join(f"{unit:04x}" for unit in units)

    def ascii_token(value: str) -> str:
        return utf16_token([ord(character) for character in value])

    def popup_int_array_token(values: list[int] | None) -> str:
        if values is None:
            return "null"
        return "-" if not values else "i" + ",".join(map(str, values))

    popup_texts = [
        None,
        [],
        [0],
        [0x003f],
        [0xD800],
        [ord(character) for character in "Silent Hill"],
    ]
    result.extend(
        f"popup-create {utf16_token(text)} {recovery_code} {popup_number}"
        for text in popup_texts
        for recovery_code in (-(2**31), -1, 0, 1, 9, 2**31 - 1)
        for popup_number in (0, 3, 4)
    )
    popup_canvas_widths = (16, 128, -(2**31), 2**31 - 1)
    result.extend(
        "popup-create-max "
        f"{utf16_token(text)} {recovery_code} {maximum_time} {popup_number} "
        f"{active} 5 5 5 {popup_canvas_widths[(text_index + popup_number) % 4]}"
        for text_index, text in enumerate(popup_texts)
        for recovery_code in (-(2**31), -1, 0, 9, 2**31 - 1)
        for maximum_time in (-(2**31), -1, 0, 1, 2**31 - 1)
        for popup_number in (-1, 0, 3, 4)
        for active in (0, 1)
    )
    popup_array_shapes = (
        (-1, 5, 5),
        (0, 5, 5),
        (3, 5, 5),
        (5, -1, 5),
        (5, 0, 5),
        (5, 3, 5),
        (5, 5, -1),
        (5, 5, 0),
        (5, 5, 3),
        (5, 5, 5),
    )
    result.extend(
        "popup-create-max u0058006c0061 7 "
        f"{maximum_time} {popup_number} {active} "
        f"{text_length} {recovery_length} {maximum_length} 128"
        for text_length, recovery_length, maximum_length in popup_array_shapes
        for maximum_time in (-1, 0)
        for popup_number in (0, 3)
        for active in (0, 1)
    )
    popup_maximum_time_arrays: tuple[list[int] | None, ...] = (
        None,
        [],
        [-1],
        [0],
        [-(2**31), -1, 0, 1, 2**31 - 1],
        [10, 20, 30],
    )
    result.extend(
        "popup-set-next "
        f"{current} {number} {active} {popup_int_array_token(maximum_times)}"
        for current in (-(2**31), -2, -1, 0, 1, 2, 3, 2**31 - 1)
        for number in (-(2**31), -1, 0, 1, 2, 3, 4, 5, 2**31 - 1)
        for active in (0, 1)
        for maximum_times in popup_maximum_time_arrays
    )
    result.extend(
        f"default-constructor {owner}"
        for owner in ("cheat", "game", "engine", "application", "codes", "text-id")
    )
    result.extend(
        f"game-canvas-new {super_fails} {full_screen_fails}"
        for super_fails in (0, 1)
        for full_screen_fails in (0, 1)
    )
    result.extend(
        f"menu-reset-ingame-values {width} {update_needed}"
        for width in edges
        for update_needed in (0, 1)
    )
    result.extend(f"app-init {logo_present}" for logo_present in (0, 1))
    jad_keys: tuple[list[int] | None, ...] = (
        None,
        [],
        [0],
        [0xD800],
        [ord(character) for character in "key"],
    )
    jad_values: tuple[list[int] | None, ...] = (
        None,
        [],
        [ord(character) for character in "0"],
        [ord(character) for character in "-1"],
        [ord(character) for character in "+1"],
        [ord(character) for character in "2147483647"],
        [ord(character) for character in "-2147483648"],
        [ord(character) for character in "2147483648"],
        [ord(character) for character in " 1"],
        [0x0661, 0x0662],
    )
    result.extend(
        "key-jad-entry "
        f"{midlet_present} {utf16_token(key)} {lookup_fails} {utf16_token(property_value)}"
        for midlet_present in (0, 1)
        for key in jad_keys
        for lookup_fails in (0, 1)
        for property_value in jad_values
    )

    result.extend(("url-encode null", "url-encode -"))
    result.extend(
        f"url-encode {utf16_token([unit])}" for unit in range(1 << 16)
    )
    url_strings: list[list[int]] = [
        [0, 14, 15, 16],
        [ord(character) for character in "Az09-._~ /%"],
        [0xD7FF, 0xD800, 0xDBFF, 0xDC00, 0xDFFF, 0xE000, 0xFFFF],
        list(range(128)),
    ]
    url_rng = random.Random(0x55524C)
    url_strings.extend(
        [url_rng.randrange(1 << 16) for _ in range(url_rng.randrange(65))]
        for _ in range(2048)
    )
    result.extend(f"url-encode {utf16_token(value)}" for value in url_strings)

    result.extend(("coded-string null", "coded-string -"))
    result.extend(
        f"coded-string {byte_token(bytes([value]))}" for value in range(1 << 8)
    )
    coded_lengths = [
        2,
        14,
        15,
        16,
        29,
        30,
        31,
        59,
        60,
        61,
        2999,
        3000,
        3001,
        3037,
    ]
    for length in coded_lengths:
        result.append(
            f"coded-string {byte_token(bytes((index * 73 + 15) & 255 for index in range(length)))}"
        )
    for repeated in (0, 14, 15, 16, 31, 32, 127, 128, 255):
        for length in (29, 30, 31, 3000, 3001):
            result.append(f"coded-string {byte_token(bytes([repeated]) * length)}")
    coded_rng = random.Random(0x434F4445)
    for _ in range(1024):
        value = bytes(
            coded_rng.randrange(256) for _ in range(coded_rng.randrange(129))
        )
        result.append(f"coded-string {byte_token(value)}")

    print_arrays: list[list[bytes | None] | None] = [
        None,
        [],
        [b""],
        [b"A"],
        [b"A", b"\x00 "],
        [None],
        [b"A", None, b"B"],
        [bytes(range(16))],
        [bytes(range(17))],
    ]
    for length in (2, 15, 16, 17, 31):
        print_arrays.append([bytes(((index * 37 + 32) & 127,)) for index in range(length)])
    print_rng = random.Random(0x5052494E54415252)
    for _ in range(128):
        values: list[bytes | None] = []
        for _ in range(print_rng.randrange(18)):
            if print_rng.randrange(13) == 0:
                values.append(None)
            else:
                values.append(bytes(
                    print_rng.randrange(128) for _ in range(print_rng.randrange(18))
                ))
        print_arrays.append(values)
    result.extend(
        f"print-array {byte_arrays_token(values)}" for values in print_arrays
    )
    result.extend(
        f"room-repaint-run {delegate_succeeds} {thread_present}"
        for delegate_succeeds in (0, 1)
        for thread_present in (0, 1)
    )
    result.extend(
        f"clear-all-rms {resource_succeeds} {script_count}"
        for resource_succeeds in (0, 1)
        for script_count in (-1, 0, 2)
    )
    result.extend(f"free-memory {runtime_present}" for runtime_present in (0, 1))
    result.extend(
        f"set-display {midlet_present} {displayable_present} {get_mode} {set_mode}"
        for midlet_present in (0, 1)
        for displayable_present in (0, 1)
        for get_mode in (0, 1, 2)
        for set_mode in (0, 1)
    )
    rms_names: tuple[list[int] | None, ...] = (
        None,
        [],
        [0],
        [0xD800],
        [0xFFFF],
        [ord(character) for character in "save"],
        [ord(character) for character in "a/b"],
        [0x00E9, 0x4E2D],
    )
    result.extend(
        f"rms-delete {utf16_token(name)} {mode}"
        for name in rms_names
        for mode in (0, 1, 2, 3)
    )

    path_strings: list[list[int] | None] = [
        None,
        [],
        [0],
        [0xD800],
        [0xFFFF],
        [ord(character) for character in "null"],
        [ord(character) for character in "sh"],
        [ord(character) for character in "a/b"],
        [0x00E9, 0x4E2D],
    ]
    path_types = [-(2**31), -1, 0, 1, 2, 3, 4, 5, 6, 2**31 - 1]
    result.extend(
        f"game-language-path {utf16_token(game_id)}"
        for game_id in path_strings
    )
    game_text_arrays: list[list[list[int] | None] | None] = [
        None,
        [],
        [None],
        [[]],
        [[ord("a")]],
        [None, [0], [0xD800], [0xFFFF], [ord("o"), ord("k")]],
    ]
    for texts in game_text_arrays:
        token = (
            "null"
            if texts is None
            else "-"
            if not texts
            else "s" + ",".join(utf16_token(text) for text in texts)
        )
        length = -1 if texts is None else len(texts)
        for index in sorted({-(2**31), -1, 0, 1, max(0, length - 1), length, length + 1, 2**31 - 1}):
            result.append(f"game-text {token} {index}")
    indexed_game_texts: list[list[int] | None] = [
        None if index == 17 else [ord(character) for character in f"v{index}"]
        for index in range(35)
    ]
    indexed_game_texts_token = "s" + ",".join(
        utf16_token(text) for text in indexed_game_texts
    )
    for unit in range(1 << 16):
        result.append(
            f"game-text-string {indexed_game_texts_token} {utf16_token([unit])}"
        )
    radix_strings = [
        None,
        [],
        [ord("+")],
        [ord("-")],
        [ord(character) for character in "0"],
        [ord(character) for character in "+0"],
        [ord(character) for character in "-1"],
        [ord(character) for character in "y"],
        [ord(character) for character in "z"],
        [ord(character) for character in "zik0zj"],
        [ord(character) for character in "zik0zk"],
        [ord(character) for character in "-zik0zj"],
        [ord(character) for character in "-zik0zk"],
        [0x0661, 0x0041],
        [0xff11, 0xff21],
        [0x005a],
        [0xff5a],
        [0],
        [0xd800, 0xdc00],
    ]
    radix_strings.extend(
        [rng.randrange(1 << 16) for _ in range(rng.randrange(24))]
        for _ in range(4096)
    )
    result.extend(
        f"game-text-string {indexed_game_texts_token} {utf16_token(index)}"
        for index in radix_strings
    )
    replace_strings = [
        None,
        [],
        [0],
        [0xd800],
        [0xffff],
        [ord("a")],
        [ord("a"), ord("a")],
        [ord("a"), ord("b"), ord("a")],
        [ord("n"), ord("u"), ord("l"), ord("l")],
        [0x00e9, 0x4e2d],
    ]
    for haystack in replace_strings:
        for needle in replace_strings:
            for replacement in replace_strings:
                result.append(
                    "text-replace "
                    f"{utf16_token(haystack)} {utf16_token(needle)} "
                    f"{utf16_token(replacement)}"
                )
    for _ in range(4096):
        haystack = [rng.randrange(1 << 16) for _ in range(rng.randrange(33))]
        needle_start = rng.randrange(len(haystack) + 1)
        needle_stop = rng.randrange(needle_start, len(haystack) + 1)
        needle = (
            haystack[needle_start:needle_stop]
            if rng.randrange(2) == 0
            else [rng.randrange(1 << 16) for _ in range(rng.randrange(9))]
        )
        replacement = [rng.randrange(1 << 16) for _ in range(rng.randrange(17))]
        result.append(
            "text-replace "
            f"{utf16_token(haystack)} {utf16_token(needle)} "
            f"{utf16_token(replacement)}"
        )
    prefix_arrays: list[list[list[int] | None] | None] = [
        None,
        [],
        [None],
        [[]],
        [[ord("a")]],
        [[ord("p"), ord("r"), ord("e")], [ord("x")], [ord("p"), ord("r"), ord("e"), ord("x")]],
        [[0xd800, ord("x")], [0xd800], [0xffff]],
        [None, [ord("p")]],
    ]
    prefix_values = [None, [], [ord("p")], [ord("p"), ord("r"), ord("e")], [0xd800], [0xffff]]
    for strings in prefix_arrays:
        strings_token = (
            "null"
            if strings is None
            else "-"
            if not strings
            else "s" + ",".join(utf16_token(string) for string in strings)
        )
        for prefix in prefix_values:
            result.append(
                f"remove-string-prefix {strings_token} {utf16_token(prefix)}"
            )
    for _ in range(4096):
        prefix = [rng.randrange(1 << 16) for _ in range(rng.randrange(6))]
        strings = []
        for _ in range(rng.randrange(12)):
            tail = [rng.randrange(1 << 16) for _ in range(rng.randrange(12))]
            strings.append(prefix + tail if rng.randrange(2) == 0 else tail)
        strings_token = (
            "-"
            if not strings
            else "s" + ",".join(utf16_token(string) for string in strings)
        )
        result.append(
            f"remove-string-prefix {strings_token} {utf16_token(prefix)}"
        )
    language_arrays: list[list[list[int] | None] | None] = [
        None,
        [],
        [None],
        [[]],
        [[ord("e"), ord("n")]],
        [[ord("e"), ord("n")], [ord("d"), ord("e")], [ord("e"), ord("n")]],
        [None, [0xd800], [0xffff]],
    ]
    language_values = [
        None,
        [],
        [ord("e"), ord("n")],
        [ord("d"), ord("e")],
        [0xd800],
        [0xffff],
    ]
    for languages in language_arrays:
        languages_token = (
            "null"
            if languages is None
            else "-"
            if not languages
            else "s" + ",".join(utf16_token(language) for language in languages)
        )
        for language in language_values:
            result.append(
                f"language-position {languages_token} {utf16_token(language)}"
            )
    for _ in range(4096):
        languages = [
            [rng.randrange(1 << 16) for _ in range(rng.randrange(9))]
            for _ in range(rng.randrange(17))
        ]
        language = (
            languages[rng.randrange(len(languages))]
            if languages and rng.randrange(2) == 0
            else [rng.randrange(1 << 16) for _ in range(rng.randrange(9))]
        )
        languages_token = (
            "-"
            if not languages
            else "s" + ",".join(utf16_token(value) for value in languages)
        )
        result.append(
            f"language-position {languages_token} {utf16_token(language)}"
        )
    heap_source_arrays: list[list[int] | None] = [
        None,
        [],
        [-(2**31)],
        [-1],
        [0],
        [1],
        [2**31 - 1],
        [1, 2, 3],
    ]
    for heap_sources in heap_source_arrays:
        token = (
            "null"
            if heap_sources is None
            else "-"
            if not heap_sources
            else "i" + ",".join(map(str, heap_sources))
        )
        for source in edges:
            result.append(f"resource-heap-index {source} {token}")
    for _ in range(4096):
        heap_sources = [
            rng.randint(-(2**31), 2**31 - 1) for _ in range(rng.randrange(1, 9))
        ]
        source = (
            heap_sources[0]
            if rng.randrange(2) == 0
            else rng.randint(-(2**31), 2**31 - 1)
        )
        result.append(
            f"resource-heap-index {source} i{','.join(map(str, heap_sources))}"
        )
    random_edges = [
        -(2**31),
        -(2**31) + 1,
        -65536,
        -1,
        0,
        1,
        65535,
        65536,
        2**31 - 2,
        2**31 - 1,
    ]
    for next_value in random_edges:
        for scale in random_edges:
            result.append(f"random-scaled fixed {next_value} {scale}")
    for scale in random_edges:
        result.append(f"random-scaled null 0 {scale}")
    for _ in range(4096):
        result.append(
            "random-scaled fixed "
            f"{rng.randint(-(2**31), 2**31 - 1)} "
            f"{rng.randint(-(2**31), 2**31 - 1)}"
        )
    ink_tables: list[list[tuple[list[int], list[int]]] | None] = [
        None,
        [],
        [([], [])],
        [([ord("x")], [ord("1")])],
        [([0xd800], [0xffff]), ([0xffff], [0xd800])],
        [([ord("x")], [ord("1")]), ([ord("x")], [ord("2")])],
        [([ord("a")], [ord("1")]), ([ord("b")], [ord("2")]), ([ord("c")], [ord("3")])],
    ]
    ink_names = [None, [], [ord("x")], [ord("a")], [ord("z")], [0xd800], [0xffff]]

    def table_token(entries: list[tuple[list[int], list[int]]] | None) -> str:
        if entries is None:
            return "null"
        if not entries:
            return "-"
        return "m" + ",".join(
            f"{utf16_token(key)}={utf16_token(value)}" for key, value in entries
        )

    for table in ink_tables:
        for name in ink_names:
            for kind in ("variable", "hint"):
                result.append(
                    f"ink-get {kind} {table_token(table)} {utf16_token(name)}"
                )
    for _ in range(4096):
        entries = [
            (
                [rng.randrange(1 << 16) for _ in range(rng.randrange(9))],
                [rng.randrange(1 << 16) for _ in range(rng.randrange(17))],
            )
            for _ in range(rng.randrange(17))
        ]
        name = (
            entries[rng.randrange(len(entries))][0]
            if entries and rng.randrange(2) == 0
            else [rng.randrange(1 << 16) for _ in range(rng.randrange(9))]
        )
        for kind in ("variable", "hint"):
            result.append(
                f"ink-get {kind} {table_token(entries)} {utf16_token(name)}"
            )
    room_size_key = [ord(character) for character in "room-size"]
    mutation_tables = [
        None,
        [],
        [([ord("x")], [ord("1")])],
        [([ord("a")], [ord("1")]), ([ord("b")], [ord("2")])],
    ]
    mutation_names = [None, [], [ord("x")], [ord("a")], [ord("z")], [0xd800], [0xffff]]
    mutation_values = [None, [], [ord("0")], [ord("n"), ord("e"), ord("w")], [0xd800], [0xffff]]
    mutation_hints = [None, [], [ord("R")], [ord("V")], [0xd800], [0xffff]]
    for variables in mutation_tables:
        for hints in mutation_tables:
            for changed in (0, 1):
                result.append(
                    f"reset-variables {table_token(variables)} {table_token(hints)} {changed}"
                )
                for name in mutation_names:
                    result.append(
                        "ink-unset "
                        f"{table_token(variables)} {table_token(hints)} {changed} "
                        f"{utf16_token(name)}"
                    )
                    for new_value in mutation_values:
                        for hint in mutation_hints:
                            result.append(
                                "ink-set "
                                f"{table_token(variables)} {table_token(hints)} {changed} "
                                f"{utf16_token(name)} {utf16_token(new_value)} "
                                f"{utf16_token(hint)}"
                            )

    room_mutation_tables = [
        *mutation_tables,
        [(room_size_key, [ord("0")])],
        [(room_size_key, [ord("1")]), ([ord(character) for character in "room-1"], [ord("a")])],
        [(room_size_key, [ord(character) for character in "2147483647"])],
        [(room_size_key, [ord("x")])],
    ]
    mutation_ids = [None, [], [ord("a")], [ord("b")], [0xd800], [0xffff]]
    mutation_amounts = [-(2**31), -1, 0, 1, 2**31 - 1]
    for variables in room_mutation_tables:
        for hints in mutation_tables:
            for changed in (0, 1):
                result.append(
                    f"room-remove {table_token(variables)} {table_token(hints)} {changed}"
                )
                for item_id in mutation_ids:
                    result.append(
                        "room-set "
                        f"{table_token(variables)} {table_token(hints)} {changed} "
                        f"{utf16_token(item_id)}"
                    )
                    result.append(
                        "room-add "
                        f"{table_token(variables)} {table_token(hints)} {changed} "
                        f"{utf16_token(item_id)}"
                    )
                    result.append(
                        "inventory-remove "
                        f"{table_token(variables)} {table_token(hints)} {changed} "
                        f"{utf16_token(item_id)}"
                    )
                    for amount in mutation_amounts:
                        result.append(
                            "inventory-set "
                            f"{table_token(variables)} {table_token(hints)} {changed} "
                            f"{utf16_token(item_id)} {amount}"
                        )

    for _ in range(2048):
        variables = [
            (
                [rng.randrange(1 << 16) for _ in range(rng.randrange(7))],
                [rng.randrange(1 << 16) for _ in range(rng.randrange(11))],
            )
            for _ in range(rng.randrange(8))
        ]
        hints = [
            (key.copy(), [rng.randrange(1 << 16) for _ in range(rng.randrange(4))])
            for key, _ in variables
        ]
        name = [rng.randrange(1 << 16) for _ in range(rng.randrange(7))]
        new_value = [rng.randrange(1 << 16) for _ in range(rng.randrange(11))]
        hint = [rng.randrange(1 << 16) for _ in range(rng.randrange(4))]
        changed = rng.randrange(2)
        variable_token = table_token(variables)
        hint_token = table_token(hints)
        name_token = utf16_token(name)
        result.append(
            f"ink-set {variable_token} {hint_token} {changed} {name_token} "
            f"{utf16_token(new_value)} {utf16_token(hint)}"
        )
        result.append(f"ink-unset {variable_token} {hint_token} {changed} {name_token}")
        result.append(f"room-set {variable_token} {hint_token} {changed} {name_token}")
        result.append(f"inventory-remove {variable_token} {hint_token} {changed} {name_token}")
        result.append(
            f"inventory-set {variable_token} {hint_token} {changed} {name_token} "
            f"{rng.randint(-(2**31), 2**31 - 1)}"
        )
    result.append("room-history-size null")
    result.append("room-history-size -")
    result.append(
        f"room-history-size {table_token([([ord('x')], [ord('1')])])}"
    )
    server_number_strings = [
        [],
        [ord(character) for character in "0"],
        [ord(character) for character in "1"],
        [ord(character) for character in "-1"],
        [ord(character) for character in "2147483647"],
        [ord(character) for character in "2147483648"],
        [ord(character) for character in "-2147483648"],
        [ord(character) for character in "-2147483649"],
        [0x0661, 0x0662],
        [0xff11, 0xff12],
        [0],
        [0xd800, 0xdc00],
    ]
    server_number_strings.extend(
        [rng.randrange(1 << 16) for _ in range(rng.randrange(24))]
        for _ in range(4096)
    )

    typed_integer_edges = [-(2**31), -1, 0, 1, 2**31 - 1]
    for variables in mutation_tables:
        for hints in mutation_tables:
            for changed in (0, 1):
                for variable_id in mutation_names:
                    prefix = (
                        f"script-set-variable {table_token(variables)} "
                        f"{table_token(hints)} {changed} {utf16_token(variable_id)}"
                    )
                    result.append(f"{prefix} null -")
                    result.append(f"{prefix} other -")
                    for integer in typed_integer_edges:
                        result.append(f"{prefix} integer {integer}")
                    for string_value in mutation_values:
                        if string_value is not None:
                            result.append(
                                f"{prefix} string {utf16_token(string_value)}"
                            )

    typed_keys = [[], [ord("x")], [ord("a")], [0xd800], [0xffff]]
    typed_hints = [[], [ord("S")], [ord("I")], [ord("X")], [ord("I"), ord("t")]]
    typed_values = server_number_strings[:16] + [
        [ord("t"), ord("e"), ord("x"), ord("t")],
        [0xd800],
        [0xffff],
    ]
    for key in typed_keys:
        other_key = [ord("z")] if key != [ord("z")] else [ord("y")]
        for stored_value in typed_values:
            variable_states = [
                None,
                [],
                [(other_key, stored_value)],
                [(key, stored_value)],
            ]
            hint_states = [None, [], [(other_key, [ord("S")])]] + [
                [(key, hint)] for hint in typed_hints
            ]
            for variables in variable_states:
                for hints in hint_states:
                    for command in ("script-get-variable", "script-get-variable-int"):
                        result.append(
                            f"{command} {table_token(variables)} {table_token(hints)} "
                            f"{utf16_token(key)}"
                        )
    for null_table in (None, []):
        for hints in (None, []):
            for command in ("script-get-variable", "script-get-variable-int"):
                result.append(
                    f"{command} {table_token(null_table)} {table_token(hints)} null"
                )

    typed_key = [ord("x")]
    integer_hint_table = [(typed_key, [ord("I")])]
    string_hint_table = [(typed_key, [ord("S")])]
    for unit in range(1 << 16):
        variables = [(typed_key, [unit])]
        variable_token = table_token(variables)
        key_token = utf16_token(typed_key)
        result.append(
            f"script-get-variable {variable_token} "
            f"{table_token(integer_hint_table)} {key_token}"
        )
        result.append(
            f"script-get-variable-int {variable_token} "
            f"{table_token(integer_hint_table)} {key_token}"
        )
        result.append(
            f"script-get-variable-int {variable_token} "
            f"{table_token(string_hint_table)} {key_token}"
        )

    for _ in range(4096):
        key = [rng.randrange(1 << 16) for _ in range(rng.randrange(8))]
        stored_value = [rng.randrange(1 << 16) for _ in range(rng.randrange(20))]
        hint = [rng.randrange(1 << 16) for _ in range(rng.randrange(4))]
        variable_token = table_token([(key, stored_value)])
        hint_token = table_token([(key, hint)])
        key_token = utf16_token(key)
        result.append(f"script-get-variable {variable_token} {hint_token} {key_token}")
        result.append(
            f"script-get-variable-int {variable_token} {hint_token} {key_token}"
        )
        integer = rng.randint(-(2**31), 2**31 - 1)
        result.append(
            f"script-set-variable {variable_token} {hint_token} {rng.randrange(2)} "
            f"{key_token} integer {integer}"
        )
        result.append(
            f"script-set-variable {variable_token} {hint_token} {rng.randrange(2)} "
            f"{key_token} string {utf16_token(stored_value)}"
        )
    for value_units in server_number_strings:
        result.append(
            f"room-history-size {table_token([(room_size_key, value_units)])}"
        )
    inventory_item_ids = [None, [], [0], [0xd800], [0xffff], [ord("x")], [ord("n"), ord("u"), ord("l"), ord("l")]]
    result.append("inventory-size null null")
    result.append("inventory-size - null")
    for item_id in inventory_item_ids:
        key = [ord(character) for character in "inv-"] + (
            [ord(character) for character in "null"] if item_id is None else item_id
        )
        for value_units in server_number_strings[:32]:
            result.append(
                f"inventory-size {table_token([(key, value_units)])} {utf16_token(item_id)}"
            )
    for _ in range(4096):
        item_id = [rng.randrange(1 << 16) for _ in range(rng.randrange(13))]
        key = [ord(character) for character in "inv-"] + item_id
        value_units = server_number_strings[rng.randrange(len(server_number_strings))]
        entries = [(key, value_units)] if rng.randrange(3) != 0 else []
        result.append(
            f"inventory-size {table_token(entries)} {utf16_token(item_id)}"
        )
    current_room_key = [ord(character) for character in "cur-curRoom"]
    result.append("room-current null")
    result.append("room-current -")
    for room in path_strings:
        if room is not None:
            result.append(
                f"room-current {table_token([(current_room_key, room)])}"
            )
    for _ in range(4096):
        room = [rng.randrange(1 << 16) for _ in range(rng.randrange(25))]
        entries = [(current_room_key, room)] if rng.randrange(3) != 0 else []
        result.append(f"room-current {table_token(entries)}")
    result.append("room-last null")
    result.append("room-last -")
    history_sizes = [-(2**31), -1, 0, 1, 2, 10, 2**31 - 1]
    for history_size in history_sizes:
        size_text = [ord(character) for character in str(history_size)]
        room_key = [ord(character) for character in f"room-{history_size}"]
        for room in path_strings:
            if room is not None:
                result.append(
                    f"room-last {table_token([(room_size_key, size_text), (room_key, room)])}"
                )
        result.append(f"room-last {table_token([(room_size_key, size_text)])}")
    for malformed in ([], [ord("x")], [ord(character) for character in "2147483648"]):
        result.append(
            "room-last "
            f"{table_token([(room_size_key, malformed), ([ord(character) for character in 'room-1'], [ord('f')])])}"
        )
    for _ in range(4096):
        history_size = rng.randrange(-1000, 1001)
        size_text = [ord(character) for character in str(history_size)]
        room_key = [ord(character) for character in f"room-{history_size}"]
        room = [rng.randrange(1 << 16) for _ in range(rng.randrange(25))]
        entries = [(room_size_key, size_text)]
        if rng.randrange(3) != 0:
            entries.append((room_key, room))
        result.append(f"room-last {table_token(entries)}")
    for type_index, resource_type in enumerate(path_types):
        for string_index, string_id in enumerate(path_strings):
            for game_index, game_id in enumerate(path_strings):
                integer_id = edges[(type_index + string_index) % len(edges)]
                image_transform = edges[(type_index + game_index) % len(edges)]
                result.append(
                    "resource-path "
                    f"{resource_type} {integer_id} {utf16_token(string_id)} "
                    f"{image_transform} {utf16_token(game_id)}"
                )
                result.append(
                    "resource-path-string "
                    f"{resource_type} {utf16_token(string_id)} {utf16_token(game_id)}"
                )
                result.append(
                    "request-resource-path "
                    f"{resource_type} {integer_id} {utf16_token(string_id)} "
                    f"{image_transform} {utf16_token(game_id)}"
                )
    for integer_id in edges:
        for image_transform in edges:
            result.append(
                "resource-path "
                f"3 {integer_id} {ascii_token('ignored')} {image_transform} "
                f"{ascii_token('game')}"
            )
            result.append(
                "resource-path "
                f"2 {integer_id} {ascii_token('sprite')} {image_transform} "
                f"{ascii_token('game')}"
            )
            result.append(
                "request-resource-path "
                f"3 {integer_id} {ascii_token('ignored')} {image_transform} "
                f"{ascii_token('game')}"
            )
            result.append(
                "request-resource-path "
                f"2 {integer_id} {ascii_token('sprite')} {image_transform} "
                f"{ascii_token('game')}"
            )
    for game_id in path_strings:
        for image_transform in edges:
            for integer_id in edges:
                result.append(
                    "resource-path-object "
                    f"integer {integer_id} null {image_transform} {utf16_token(game_id)}"
                )
            for string_id in path_strings:
                result.append(
                    "resource-path-object "
                    f"string 0 {utf16_token(string_id)} {image_transform} "
                    f"{utf16_token(game_id)}"
                )
            result.append(
                "resource-path-object "
                f"null 0 null {image_transform} {utf16_token(game_id)}"
            )
            result.append(
                "resource-path-object "
                f"other 0 null {image_transform} {utf16_token(game_id)}"
            )

    for resource_type in path_types:
        for string_id in path_strings:
            result.append(
                f"request-new-string {resource_type} {utf16_token(string_id)}"
            )
        for edge_index, integer_id in enumerate(edges):
            string_id = path_strings[edge_index % len(path_strings)]
            image_transform = edges[-1 - edge_index]
            result.append(
                "request-get-id "
                f"{resource_type} {integer_id} {utf16_token(string_id)} "
                f"{image_transform}"
            )
    for image_transform in edges:
        for integer_id in edges:
            result.append(
                "request-new-object "
                f"integer {integer_id} null {image_transform}"
            )
        for string_id in path_strings:
            result.append(
                "request-new-object "
                f"string 0 {utf16_token(string_id)} {image_transform}"
            )
        result.append(f"request-new-object null 0 null {image_transform}")
        result.append(f"request-new-object other 0 null {image_transform}")

    request_states: list[tuple[int, int, list[int] | None, int]] = []
    equality_types = [-1, 2, 3, 5]
    equality_integers = [-(2**31), -1, 0, 2**31 - 1]
    equality_strings = [None, [], [0xD800], [ord("x")], [ord("s"), ord("a"), ord("m"), ord("e")]]
    equality_transforms = [-1, 0, 2**31 - 1]
    for index in range(60):
        request_states.append(
            (
                equality_types[index % len(equality_types)],
                equality_integers[(index // 2) % len(equality_integers)],
                equality_strings[(index // 3) % len(equality_strings)],
                equality_transforms[(index // 5) % len(equality_transforms)],
            )
        )
    for left_type, left_integer, left_string, left_transform in request_states:
        left = (
            f"{left_type} {left_integer} {utf16_token(left_string)} "
            f"{left_transform}"
        )
        result.append(f"request-equals {left} same 0 0 null 0")
        result.append(f"request-equals {left} null 0 0 null 0")
        result.append(f"request-equals {left} other 0 0 null 0")
        for right_type, right_integer, right_string, right_transform in request_states:
            right = (
                f"{right_type} {right_integer} {utf16_token(right_string)} "
                f"{right_transform}"
            )
            result.append(f"request-equals {left} request {right}")

    game_resource_ints = [-(2**31), -1, 0, 1, 2**31 - 1]
    game_resource_ids = [
        "n",
        *(f"i{value}" for value in game_resource_ints),
        "s-",
        f"s{utf16_token([0])}",
        f"s{utf16_token([0xD800])}",
        f"s{utf16_token([ord(character) for character in 'same'])}",
        "o0",
        "o1",
    ]
    for resource_type in game_resource_ints:
        for resource_id in game_resource_ids:
            for transform in game_resource_ints:
                result.append(
                    f"game-resource-new {resource_type} {resource_id} {transform}"
                )
                result.append(
                    "game-resource-equals "
                    f"{resource_type} {resource_id} {transform} null 0 n 0"
                )
                result.append(
                    "game-resource-equals "
                    f"{resource_type} {resource_id} {transform} other 0 n 0"
                )
    for left_type in game_resource_ints:
        for right_type in game_resource_ints:
            for transform in game_resource_ints:
                result.append(
                    "game-resource-equals "
                    f"{left_type} n {transform} resource {right_type} n {transform}"
                )
    for left_id in game_resource_ids:
        for right_id in game_resource_ids:
            result.append(
                f"game-resource-equals 0 {left_id} 0 resource 0 {right_id} 0"
            )
    for left_transform in game_resource_ints:
        for right_transform in game_resource_ints:
            result.append(
                "game-resource-equals "
                f"0 i7 {left_transform} resource 0 i7 {right_transform}"
            )
    game_resource_rng = random.Random(0x47524553)
    for _ in range(4096):
        left_type = game_resource_rng.choice(game_resource_ints)
        left_id = game_resource_rng.choice(game_resource_ids)
        left_transform = game_resource_rng.choice(game_resource_ints)
        candidate_kind = game_resource_rng.choice(("null", "other", "resource", "resource"))
        right_type = game_resource_rng.choice(game_resource_ints)
        right_id = game_resource_rng.choice(game_resource_ids)
        right_transform = game_resource_rng.choice(game_resource_ints)
        result.append(
            "game-resource-equals "
            f"{left_type} {left_id} {left_transform} {candidate_kind} "
            f"{right_type} {right_id} {right_transform}"
        )
    paint_simple_values = [-(2**31), -1, 0, 1, 2**31 - 1]
    for graphics_present in (0, 1):
        for image_present in (0, 1):
            for x in paint_simple_values:
                for y in paint_simple_values:
                    for anchor in paint_simple_values:
                        for fail in (0, 1):
                            result.append(
                                "game-resource-paint-simple "
                                f"{graphics_present} {image_present} {x} {y} {anchor} {fail}"
                            )
    paint_simple_rng = random.Random(0x5041494E54)
    for _ in range(1024):
        result.append(
            "game-resource-paint-simple "
            f"{paint_simple_rng.randrange(2)} {paint_simple_rng.randrange(2)} "
            f"{paint_simple_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_simple_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_simple_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_simple_rng.randrange(2)}"
        )
    paint_transforms = [-(2**31), -1, *range(9), 2**31 - 1]
    paint_geometry = [
        (-(2**31), -(2**31), -(2**31), -(2**31), -(2**31), -(2**31)),
        (2**31 - 1, 2**31 - 1, 2**31 - 1, 2**31 - 1, 2**31 - 1, 2**31 - 1),
        (0, 0, 0, 0, 0, 0),
        (-1, 1, -1, 1, -1, 1),
        (1, -1, 1, -1, 1, -1),
        (-(2**31), 2**31 - 1, 0, 1, -1, 2**31 - 1),
        (2**31 - 1, -(2**31), 1, 0, 2**31 - 1, -1),
        (20, 36, 127, 255, 17, 19),
    ]
    paint_tables = [
        [0, 2, 5, 7, 3, 1, 6, 4],
        [-(2**31), -1, 0, 1, 2, 3, 4, 2**31 - 1],
        [77, 66, 55, 44, 33, 22, 11, 0],
    ]

    def paint_table_token(values: list[int]) -> str:
        return "i" + ",".join(map(str, values))

    for graphics_present in (0, 1):
        for image_present in (0, 1):
            for fail in (0, 1):
                for transform in paint_transforms:
                    for x, y, width, height, registration_x, registration_y in paint_geometry:
                        for table in paint_tables:
                            result.append(
                                "game-resource-paint "
                                f"{graphics_present} {image_present} {x} {y} {transform} "
                                f"{width} {height} {registration_x} {registration_y} {fail} "
                                f"{paint_table_token(table)}"
                            )
    paint_rng = random.Random(0x5041494E54524547)
    for _ in range(4096):
        table = [paint_rng.randint(-(2**31), 2**31 - 1) for _ in range(8)]
        result.append(
            "game-resource-paint "
            f"{paint_rng.randrange(2)} {paint_rng.randrange(2)} "
            f"{paint_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_rng.choice(paint_transforms)} "
            f"{paint_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_rng.randint(-(2**31), 2**31 - 1)} "
            f"{paint_rng.randrange(2)} {paint_table_token(table)}"
        )

    result.append("object-convert null 0 null")
    result.append("object-convert other 0 null")
    for integer_value in [*edges, *(rng.randint(-(2**31), 2**31 - 1) for _ in range(256))]:
        result.append(f"object-convert integer {integer_value} null")
    for unit in range(1 << 16):
        result.append(f"object-convert string 0 {utf16_token([unit])}")
    conversion_strings = [
        [],
        [ord("+")],
        [ord("-")],
        [ord(character) for character in "0"],
        [ord(character) for character in "+0"],
        [ord(character) for character in "-0"],
        [ord(character) for character in "2147483647"],
        [ord(character) for character in "2147483648"],
        [ord(character) for character in "-2147483648"],
        [ord(character) for character in "-2147483649"],
        [ord(character) for character in "000000000000000000000000000000000000001"],
        [ord(character) for character in " 1"],
        [ord(character) for character in "1 "],
        [0],
        [0xD800, 0xDC00],
        [0x0661, 0x0662],
        [0x06F1, 0x06F2],
        [0xFF11, 0xFF12],
        [0x0661, ord("2")],
    ]
    conversion_strings.extend(
        [rng.randrange(1 << 16) for _ in range(rng.randrange(24))]
        for _ in range(4096)
    )
    result.extend(
        f"object-convert string 0 {utf16_token(value)}"
        for value in conversion_strings
    )

    action_ids = ["actionkey_star", "actionkey_pound"]
    action_ids.extend(f"actionkey_num{digit}" for digit in range(10))
    action_edges = [
        None,
        [],
        [0],
        [0xD800],
        [0xFFFF],
        *([ord(character) for character in value] for value in action_ids),
        *(
            [ord(character) for character in value]
            for value in [
                "ACTIONKEY_STAR",
                "actionkey_star0",
                "actionkey_num",
                "actionkey_num00",
                "actionkey_num:",
                " actionkey_num0",
            ]
        ),
    ]
    result.extend(f"action {utf16_token(value)}" for value in action_edges)
    for _ in range(1024):
        units = [rng.randrange(1 << 16) for _ in range(rng.randrange(21))]
        result.append(f"action {utf16_token(units)}")

    for value in edges:
        result.append(f"tick-get {value}")
        result.append(f"tick-update {value}")
        result.append(f"tick-reset {value}")
    result.extend(("loading 0", "loading 1"))
    result.extend(f"scroll {value}" for value in range(-128, 128))

    def int_array_token(values: list[int] | None) -> str:
        if values is None:
            return "null"
        return "-" if not values else "i" + ",".join(map(str, values))

    keycode_arrays: list[list[int] | None] = [
        None,
        [],
        [42, 35, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57],
        [7, 7, 7],
        [-(2**31), 0, 2**31 - 1],
    ]
    keycode_arrays.extend(
        [rng.randint(-(2**31), 2**31 - 1) for _ in range(rng.randrange(25))]
        for _ in range(128)
    )
    for values in keycode_arrays:
        probes = [*edges, 35, 42, 48, 53, 57]
        if values:
            probes.extend(values)
        for keycode in probes:
            result.append(f"action-code {keycode} {int_array_token(values)}")
    result.extend(f"action-unset {length}" for length in (-1, 0, 1, 11, 12, 13, 127))

    def script_array_token(values: list[list[int] | None] | None) -> str:
        if values is None:
            return "null"
        return "-" if not values else "s" + ",".join(utf16_token(value) for value in values)

    def ink_script_blob(
        gfx_type: int,
        gfx_payload: bytes,
        events: list[tuple[int, int]],
        data: bytes,
    ) -> bytes:
        assert 0 <= gfx_type <= 255
        assert len(events) <= 255
        assert len(data) <= 65535
        encoded = bytearray([gfx_type])
        encoded.extend(gfx_payload)
        encoded.append(len(events))
        for event_code, event_offset in events:
            encoded.append(event_code)
            encoded.extend(event_offset.to_bytes(2, "big"))
        encoded.extend(len(data).to_bytes(2, "big"))
        encoded.extend(data)
        return bytes(encoded)

    constructor_strings: list[list[list[int] | None] | None] = [
        None,
        [],
        [None],
        [[]],
        [[0]],
        [[0xD800]],
        [[0xFFFF]],
        [[ord("a")], None, [ord("b"), 0, 0xDFFF]],
    ]
    result.extend(
        f"ink-script-new null {script_array_token(strings)}"
        for strings in constructor_strings
    )
    for gfx_type in range(256):
        gfx_payload = b"\x01" if gfx_type == 1 else b"\xff\xff" if gfx_type == 2 else b""
        result.append(
            "ink-script-new "
            f"{byte_token(ink_script_blob(gfx_type, gfx_payload, [], b''))} "
            "su0078"
        )
    for strings in constructor_strings:
        for string_index in (0, 1, 2, 3, 255):
            result.append(
                "ink-script-new "
                f"{byte_token(ink_script_blob(1, bytes([string_index]), [], b''))} "
                f"{script_array_token(strings)}"
            )
    for integer_gfx in (0, 1, 255, 256, 32767, 32768, 65535):
        result.append(
            "ink-script-new "
            f"{byte_token(ink_script_blob(2, integer_gfx.to_bytes(2, 'big'), [], b''))} "
            "null"
        )
    for event_code in range(256):
        result.append(
            "ink-script-new "
            f"{byte_token(ink_script_blob(0, b'', [(event_code, 0xBEEF)], b''))} "
            "-"
        )
    for event_count in (0, 1, 2, 56, 57, 255):
        events = [(index % 57, (index * 257) & 0xFFFF) for index in range(event_count)]
        result.append(
            "ink-script-new "
            f"{byte_token(ink_script_blob(0, b'', events, bytes([0, 255])))} "
            "-"
        )
    for data_length in (0, 1, 2, 255, 256, 257):
        data = bytes((index * 73 + 19) & 255 for index in range(data_length))
        blob = ink_script_blob(0, b"", [(0, 0), (56, 65535)], data)
        result.append(f"ink-script-new {byte_token(blob)} -")
        for retained in dict.fromkeys((0, 1, 2, len(blob) // 2, len(blob) - 1)):
            result.append(f"ink-script-new {byte_token(blob[:retained])} -")
    maximum_length_header = bytes([0, 0, 255, 255])
    result.append(f"ink-script-new {byte_token(maximum_length_header)} -")

    constructor_rng = random.Random(0x494E4B534352495054)
    for _ in range(512):
        gfx_type = constructor_rng.randrange(256)
        if gfx_type == 1:
            gfx_payload = bytes([constructor_rng.randrange(256)])
        elif gfx_type == 2:
            gfx_payload = bytes([constructor_rng.randrange(256), constructor_rng.randrange(256)])
        else:
            gfx_payload = b""
        events = [
            (constructor_rng.randrange(256), constructor_rng.randrange(65536))
            for _ in range(constructor_rng.randrange(12))
        ]
        data = bytes(constructor_rng.randrange(256) for _ in range(constructor_rng.randrange(33)))
        blob = ink_script_blob(gfx_type, gfx_payload, events, data)
        if constructor_rng.randrange(2) == 0:
            blob = blob[:constructor_rng.randrange(len(blob) + 1)]
        result.append(
            f"ink-script-new {byte_token(blob)} "
            f"{script_array_token(constructor_rng.choice(constructor_strings))}"
        )

    def room_object_blob(object_type: int, payload: bytes, script_index: int) -> bytes:
        assert 0 <= object_type <= 255
        assert 0 <= script_index <= 255
        return bytes([object_type]) + payload + bytes([script_index])

    def signed_short(value: int) -> bytes:
        return value.to_bytes(2, "big", signed=True)

    result.extend(
        f"room-object-new null {script_array_token(strings)}"
        for strings in constructor_strings
    )

    # Every outer switch selector, with a complete payload for the recognized
    # types. Unknown types fall straight through and consume the following byte
    # as the script-string index.
    for object_type in range(256):
        if object_type == 1:
            payload = (signed_short(-32768) + signed_short(32767)
                       + bytes([255, 2, 0xBE, 0xEF]))
        elif 2 <= object_type <= 6:
            payload = (signed_short(-32768) + signed_short(32767)
                       + bytes([0, 255]))
        else:
            payload = b""
        result.append(
            "room-object-new "
            f"{byte_token(room_object_blob(object_type, payload, 0))} su0078"
        )

    # Every nested graphics-ID selector and both recognized operand shapes.
    for gfx_type in range(256):
        gfx_payload = b"\x01" if gfx_type == 1 else b"\xbe\xef" if gfx_type == 2 else b""
        payload = signed_short(-1) + signed_short(1) + bytes([7, gfx_type]) + gfx_payload
        result.append(
            "room-object-new "
            f"{byte_token(room_object_blob(1, payload, 0))} su0078"
        )

    for strings in constructor_strings:
        for string_index in (0, 1, 2, 3, 255):
            payload = (signed_short(-32768) + signed_short(32767)
                       + bytes([255, 1, string_index]))
            result.append(
                "room-object-new "
                f"{byte_token(room_object_blob(1, payload, 0))} "
                f"{script_array_token(strings)}"
            )

    for integer_gfx in (0, 1, 255, 256, 32767, 32768, 65535):
        payload = (signed_short(-32768) + signed_short(32767) + bytes([255, 2])
                   + integer_gfx.to_bytes(2, "big"))
        result.append(
            "room-object-new "
            f"{byte_token(room_object_blob(1, payload, 0))} null"
        )

    signed_short_edges = (-32768, -32767, -1, 0, 1, 32767)
    for object_type in range(2, 7):
        for x in signed_short_edges:
            for y in signed_short_edges:
                payload = signed_short(x) + signed_short(y) + bytes([0, 255])
                result.append(
                    "room-object-new "
                    f"{byte_token(room_object_blob(object_type, payload, 0))} -"
                )
        for width in (0, 1, 255):
            for height in (0, 1, 255):
                payload = signed_short(-1) + signed_short(1) + bytes([width, height])
                result.append(
                    "room-object-new "
                    f"{byte_token(room_object_blob(object_type, payload, 0))} -"
                )

    # Script lookup happens after the type-specific payload and index zero must
    # not dereference the strings array.
    for strings in constructor_strings:
        for script_index in (0, 1, 2, 3, 255):
            for object_type, payload in (
                (0, b""),
                (1, signed_short(-1) + signed_short(1) + bytes([0, 2, 0, 1])),
                (2, signed_short(-1) + signed_short(1) + bytes([1, 255])),
            ):
                result.append(
                    "room-object-new "
                    f"{byte_token(room_object_blob(object_type, payload, script_index))} "
                    f"{script_array_token(strings)}"
                )

    # Every truncation point proves partial field publication and exact cursor
    # advancement through readShort/readUnsignedShort.
    representative_room_objects = (
        room_object_blob(
            1,
            signed_short(-32768) + signed_short(32767) + bytes([255, 1, 1]),
            1,
        ),
        room_object_blob(
            1,
            signed_short(32767) + signed_short(-32768) + bytes([1, 2, 0xBE, 0xEF]),
            0,
        ),
        room_object_blob(
            6,
            signed_short(-32768) + signed_short(32767) + bytes([0, 255]),
            1,
        ),
    )
    for blob in representative_room_objects:
        for retained in range(len(blob) + 1):
            result.append(
                f"room-object-new {byte_token(blob[:retained])} "
                "su0078,u0079"
            )

    room_constructor_rng = random.Random(0x524F4F4D4F424A45)
    for _ in range(512):
        object_type = room_constructor_rng.randrange(256)
        if object_type == 1:
            gfx_type = room_constructor_rng.randrange(256)
            if gfx_type == 1:
                gfx_payload = bytes([room_constructor_rng.randrange(256)])
            elif gfx_type == 2:
                gfx_payload = bytes([
                    room_constructor_rng.randrange(256),
                    room_constructor_rng.randrange(256),
                ])
            else:
                gfx_payload = b""
            payload = (
                bytes(room_constructor_rng.randrange(256) for _ in range(5))
                + bytes([gfx_type])
                + gfx_payload
            )
        elif 2 <= object_type <= 6:
            payload = bytes(room_constructor_rng.randrange(256) for _ in range(6))
        else:
            payload = b""
        blob = room_object_blob(object_type, payload, room_constructor_rng.randrange(256))
        if room_constructor_rng.randrange(2) == 0:
            blob = blob[:room_constructor_rng.randrange(len(blob) + 1)]
        result.append(
            f"room-object-new {byte_token(blob)} "
            f"{script_array_token(room_constructor_rng.choice(constructor_strings))}"
        )

    script_code_arrays: list[list[int] | None] = [
        None,
        [],
        [7],
        [7, 8],
        [7, 7, 9],
        [42, 35, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57],
    ]
    script_id_arrays: list[list[list[int] | None] | None] = [
        None,
        [],
        [None],
        [[]],
        [[ord("a")]],
        [None, [0xD800]],
        [[ord(str(index % 10))] for index in range(12)],
    ]
    for key_codes in script_code_arrays:
        for script_values in script_id_arrays:
            probes = [*edges, 7, 8, 9, 35, 42, 57]
            for keycode in probes:
                result.append(
                    "action-script "
                    f"{keycode} {int_array_token(key_codes)} "
                    f"{script_array_token(script_values)}"
                )
    for key_codes in script_code_arrays:
        for script_values in script_id_arrays:
            result.append(
                "action-init "
                f"{int_array_token(key_codes)} {script_array_token(script_values)}"
            )

    for current_splash in edges:
        for number_of_splashes in edges:
            result.append(f"splash-more {current_splash} {number_of_splashes}")
    for _ in range(128):
        result.append(
            "splash-more "
            f"{rng.randint(-(2**31), 2**31 - 1)} "
            f"{rng.randint(-(2**31), 2**31 - 1)}"
        )

    setting_keys = [
        "KEY_SOFTKEY_LEFT",
        "KEY_SOFTKEY_RIGHT",
        "KEY_SEND",
        "KEY_END",
        "KEY_SOFTKEY_THUMBSTICK",
        "KEY_UP_ARROW",
        "KEY_DOWN_ARROW",
        "KEY_LEFT_ARROW",
        "KEY_RIGHT_ARROW",
        "KEY_CLEAR",
    ]
    setting_key_units = {
        key: [ord(character) for character in key] for key in setting_keys
    }

    def setting_value_token(setting: tuple[str, int | list[int] | None]) -> str:
        kind, value = setting
        if kind == "integer":
            assert isinstance(value, int)
            return f"i{value}"
        if kind == "string":
            assert isinstance(value, list)
            return "s" + utf16_token(value)
        assert kind == "other" and value is None
        return "o"

    def settings_token(
        entries: list[tuple[list[int], tuple[str, int | list[int] | None]]] | None,
    ) -> str:
        if entries is None:
            return "null"
        if not entries:
            return "-"
        return "q" + ",".join(
            f"{utf16_token(key)}={setting_value_token(setting)}"
            for key, setting in entries
        )

    def add_key_init(
        entries: list[tuple[list[int], tuple[str, int | list[int] | None]]] | None,
        seeds: list[int],
    ) -> None:
        result.append(f"key-init {settings_token(entries)} {' '.join(map(str, seeds))}")

    key_init_seeds = [
        [0] * 10,
        [*range(1, 11)],
        [edges[index % len(edges)] for index in range(10)],
    ]
    for entries in (None, []):
        for seeds in key_init_seeds:
            add_key_init(entries, seeds)

    add_key_init(
        [
            (setting_key_units[key], ("integer", 101 + index))
            for index, key in enumerate(setting_keys)
        ],
        key_init_seeds[1],
    )
    targeted_settings: list[tuple[str, int | list[int] | None]] = [
        *(("integer", value) for value in edges),
        ("string", []),
        ("string", [ord(character) for character in "0"]),
        ("string", [ord(character) for character in "-2147483648"]),
        ("string", [ord(character) for character in "2147483647"]),
        ("string", [ord(character) for character in "2147483648"]),
        ("string", [0x0661, 0x0662, 0x0663]),
        ("string", [0xD800]),
        ("other", None),
    ]
    for key_index, key in enumerate(setting_keys):
        for setting_index, setting in enumerate(targeted_settings):
            seeds = [
                edges[(key_index + setting_index + index) % len(edges)]
                for index in range(10)
            ]
            add_key_init([(setting_key_units[key], setting)], seeds)
    add_key_init(
        [([ord(character) for character in "IRRELEVANT_KEY"], ("integer", 777))],
        key_init_seeds[2],
    )
    add_key_init(
        [
            (setting_key_units["KEY_SOFTKEY_LEFT"], ("integer", 11)),
            (
                setting_key_units["KEY_SOFTKEY_LEFT"],
                ("string", [ord("2"), ord("2")]),
            ),
            (setting_key_units["KEY_SEND"], ("other", None)),
            (setting_key_units["KEY_SEND"], ("integer", 33)),
        ],
        key_init_seeds[0],
    )
    key_init_rng = random.Random(0x4B4559494E4954)
    random_setting_keys = [*setting_keys, "IRRELEVANT_KEY", "KEY_softkey_left"]
    random_string_settings = [
        [],
        [ord(character) for character in "-1"],
        [ord(character) for character in "+42"],
        [ord(character) for character in "2147483648"],
        [0x0661, 0x0662, 0x0663],
        [0xD800],
    ]
    for _ in range(4096):
        entries = []
        for _ in range(key_init_rng.randrange(18)):
            key = key_init_rng.choice(random_setting_keys)
            kind = key_init_rng.randrange(3)
            if kind == 0:
                setting: tuple[str, int | list[int] | None] = (
                    "integer",
                    key_init_rng.randint(-(2**31), 2**31 - 1),
                )
            elif kind == 1:
                setting = ("string", key_init_rng.choice(random_string_settings))
            else:
                setting = ("other", None)
            key_units = setting_key_units.get(key, [ord(character) for character in key])
            entries.append((key_units, setting))
        add_key_init(
            entries,
            [key_init_rng.randint(-(2**31), 2**31 - 1) for _ in range(10)],
        )

    key_literals = [
        0,
        35,
        42,
        48,
        50,
        52,
        53,
        54,
        56,
        57,
        98,
        100,
        102,
        106,
        107,
        116,
        118,
        121,
    ]
    key_configs = [
        [0] * 10,
        [-6, -7, -5, -1, -2, -3, -4, -11, -8, -10],
        [53] * 10,
    ]
    key_configs.extend(
        [rng.randint(-256, 256) for _ in range(10)] for _ in range(128)
    )
    for config in key_configs:
        probes = [*edges, *key_literals, *config]
        for keycode in probes:
            arguments = " ".join(map(str, [keycode, *config]))
            result.append(f"key-convert {arguments}")

    scroll_states = [-128, -1, 0, 1, 127]
    for old_new in (0, 1):
        for old_pressed in (0, 1):
            for old_last in edges:
                for old_scroll in scroll_states:
                    for keycode in edges:
                        for status in (0, 1):
                            result.append(
                                "set-key-status "
                                f"{old_new} {old_pressed} {old_last} {old_scroll} "
                                f"{keycode} {status}"
                            )

    def add_key_event(
        command: str,
        loading_mode: int,
        load_bar_active: bool,
        dissolve_counter: int,
        old_new: bool,
        old_pressed: bool,
        old_last: int,
        old_scroll: int,
        keycode: int,
        config: list[int],
    ) -> None:
        arguments = [
            loading_mode,
            int(load_bar_active),
            dissolve_counter,
            int(old_new),
            int(old_pressed),
            old_last,
            old_scroll,
            keycode,
            *config,
        ]
        result.append(f"{command} {' '.join(map(str, arguments))}")

    guard_states = [
        (loading_mode, load_bar_active, dissolve_counter)
        for loading_mode in [-(2**31), -2, -1, 0, 1, 2**31 - 1]
        for load_bar_active in (False, True)
        for dissolve_counter in [-(2**31), -4, -3, -2, 0, 2**31 - 1]
    ]
    for index, guard_state in enumerate(guard_states):
        config = key_configs[index % len(key_configs)]
        for command in ("key-pressed", "key-released"):
            add_key_event(
                command,
                *guard_state,
                bool(index & 1),
                bool(index & 2),
                edges[index % len(edges)],
                scroll_states[index % len(scroll_states)],
                key_literals[index % len(key_literals)],
                config,
            )

    event_configs = [*key_configs[:3], *key_configs[3:19]]
    for config_index, config in enumerate(event_configs):
        probes = [*edges, *key_literals, *config]
        for probe_index, keycode in enumerate(probes):
            for command in ("key-pressed", "key-released"):
                add_key_event(
                    command,
                    -1,
                    False,
                    [-4, -3, -(2**31)][probe_index % 3],
                    bool((config_index + probe_index) & 1),
                    bool((config_index + probe_index) & 2),
                    edges[(config_index + probe_index) % len(edges)],
                    scroll_states[probe_index % len(scroll_states)],
                    keycode,
                    config,
                )

    standard_key_config = key_configs[1]
    for old_scroll in range(-128, 128):
        for command in ("key-pressed", "key-released"):
            add_key_event(
                command,
                -1,
                False,
                -3,
                bool(old_scroll & 1),
                bool(old_scroll & 2),
                old_scroll * 65537,
                old_scroll,
                53,
                standard_key_config,
            )

    for index in range(4096):
        config = [rng.randint(-(2**31), 2**31 - 1) for _ in range(10)]
        add_key_event(
            "key-pressed" if index & 1 else "key-released",
            rng.choice([-(2**31), -2, -1, 0, 1, 2**31 - 1]),
            bool(rng.randrange(2)),
            rng.choice([-(2**31), -4, -3, -2, 0, 2**31 - 1]),
            bool(rng.randrange(2)),
            bool(rng.randrange(2)),
            rng.randint(-(2**31), 2**31 - 1),
            rng.randint(-128, 127),
            rng.randint(-(2**31), 2**31 - 1),
            config,
        )
    result.extend(
        f"canvas-paint {graphics_present} {delegate_enabled} {painting}"
        for graphics_present in (0, 1)
        for delegate_enabled in (0, 1)
        for painting in (0, 1)
        if graphics_present or not delegate_enabled
    )
    result.extend(
        f"canvas-show-notify {hidden} {sound_enabled} {loop_count}"
        for hidden in (0, 1)
        for sound_enabled in (0, 1)
        for loop_count in (-1, 0, 1)
    )
    wrap_texts: list[list[int] | None] = [
        None,
        [],
        [0],
        [0xD800],
        [ord("+")],
        [ord(character) for character in "a+b"],
        [ord(character) for character in "plain"],
    ]
    result.extend(
        f"wrap-default {utf16_token(text)} {maximum_length} {font_present}"
        for text in wrap_texts
        for maximum_length in (0, 1, 2**31 - 1)
        for font_present in (0, 1)
    )
    result.extend(f"menu-choice {value}" for value in edges)

    def choice_array_token(values: list[int | None] | None) -> str:
        if values is None:
            return "null"
        if not values:
            return "-"
        return "o" + ",".join("n" if value is None else str(value) for value in values)

    choice_arrays: list[list[int | None] | None] = [
        None,
        [],
        [None],
        [0],
        [-(2**31)],
        [0, None, 2],
        [9, 9],
        list(range(12)),
    ]
    for choices in choice_arrays:
        token = choice_array_token(choices)
        result.append(f"menu-count {token}")
        selected_values = [*edges, -2, 2]
        if choices is not None:
            selected_values.extend((len(choices) - 1, len(choices), len(choices) + 1))
        for selected in selected_values:
            result.append(f"menu-get-id {token} {selected}")
            for scroll in edges:
                for update_menu in (0, 1):
                    result.append(f"menu-next {token} {selected} {scroll} {update_menu}")
                    result.append(
                        f"menu-previous {token} {selected} {scroll} {update_menu}"
                    )
    for index, x in enumerate(edges):
        y = edges[-index - 1]
        result.append(f"menu-position {y} {x} {x} {y}")
    for _ in range(128):
        old_x, old_y, x, y = (
            rng.randint(-(2**31), 2**31 - 1) for _ in range(4)
        )
        result.append(f"menu-position {old_x} {old_y} {x} {y}")
    for previous in (0, 1):
        for current in (0, 1):
            result.append(f"menu-current {previous} {current}")
    for scroll in edges:
        for text_scrolling in (0, 1):
            for update_menu in (0, 1):
                result.append(
                    f"menu-scroll-increase {scroll} {text_scrolling} {update_menu}"
                )
                result.append(
                    f"menu-scroll-decrease {scroll} {text_scrolling} {update_menu}"
                )

    string_edges: list[list[int] | None] = [
        None,
        [],
        [0],
        [0xD800],
        [0xDC00],
        [0xFFFF],
        [ord(character) for character in "left"],
        [ord(character) for character in "right"],
    ]
    for old_text in string_edges:
        for new_text in string_edges:
            for update_menu in (0, 1):
                for update_top_lines in (0, 1):
                    result.append(
                        "menu-top "
                        f"{utf16_token(old_text)} {update_menu} {update_top_lines} "
                        f"{utf16_token(new_text)}"
                    )
    for old_left in string_edges:
        for old_right in string_edges:
            for new_left in string_edges:
                for new_right in string_edges:
                    result.append(
                        "menu-softkeys "
                        f"{utf16_token(old_left)} {utf16_token(old_right)} "
                        f"{utf16_token(new_left)} {utf16_token(new_right)}"
                    )
    resource_handles: list[int | None] = [None, -(2**31), -1, 0, 2**31 - 1]
    for old_resource in resource_handles:
        for new_resource in resource_handles:
            old_token = "n" if old_resource is None else str(old_resource)
            new_token = "n" if new_resource is None else str(new_resource)
            result.append(f"menu-resource {old_token} {new_token}")
    for stack_length in (-1, 0, 1, 2, 17):
        result.append(f"menu-active {stack_length}")
        result.append(f"menu-close-all {stack_length}")
        result.append(f"menu-get-current {stack_length}")

    for flags in itertools.product("01", repeat=3):
        flag_token = "b" + "".join(flags)
        result.append(f"menu-close-current null {flag_token}")
        for stack_length in range(6):
            for handles in itertools.product(range(3), repeat=stack_length):
                stack_token = "-" if not handles else "m" + ",".join(map(str, handles))
                result.append(f"menu-close-current {stack_token} {flag_token}")

    for strings in script_id_arrays:
        probes = [*edges, -2, 2, 3, 12, 13]
        if strings is not None:
            probes.extend((len(strings), len(strings) + 1, len(strings) + 2))
        for string_index in probes:
            result.append(
                f"ink-get-string {script_array_token(strings)} {string_index}"
            )

    event_offset_arrays: list[list[int] | None] = [
        None,
        [],
        [-1],
        [0],
        [-(2**31), -1, 0, 1, 2**31 - 1],
        [-1] * 57,
        list(range(57)),
    ]
    event_offset_arrays.extend(
        [rng.choice(edges) for _ in range(rng.randrange(65))] for _ in range(64)
    )
    for offsets in event_offset_arrays:
        probes = [*edges, -2, 2, 56, 57]
        if offsets is not None:
            probes.extend((len(offsets) - 1, len(offsets), len(offsets) + 1))
        for event_code in probes:
            result.append(f"ink-has-event {int_array_token(offsets)} {event_code}")

    for script_present in (0, 1):
        for event_offset in edges:
            for room_present in (0, 1):
                result.append(
                    f"ink-new {script_present} {event_offset} {room_present}"
                )

    interpreter_data: list[bytes | None] = [
        None,
        b"",
        b"\x00",
        b"\x7f",
        b"\x80",
        b"\xff",
        b"\x00\x01\x7f\x80\xff",
    ]
    interpreter_data.extend(
        bytes(rng.randrange(256) for _ in range(rng.randrange(10)))
        for _ in range(16)
    )
    script_data_cases = [(0, None), *((1, data) for data in interpreter_data)]
    for script_present, data in script_data_cases:
        data_length = 0 if data is None else len(data)
        offsets = list(
            dict.fromkeys(
                [*edges, -2, 2, data_length - 1, data_length, data_length + 1]
            )
        )
        for offset in offsets:
            token = byte_token(data)
            result.append(f"ink-read {script_present} {token} {offset}")
            for byte_count in edges:
                result.append(
                    f"ink-read-n {script_present} {token} {offset} {byte_count}"
                )
                result.append(
                    f"ink-read-signed {script_present} {token} {offset} {byte_count}"
                )

    execution_scripts: list[tuple[int, bytes | None]] = [
        (0, None),
        (1, None),
        (1, b""),
        (1, bytes([2])),
        (1, bytes([25])),
        (1, bytes([0x5D, 5, 0])),
        (1, bytes([5, 7, 2])),
        (1, bytes([0x41, 5, 7])),
        (1, bytes([5, 7, 0x41, 5, 9])),
        (1, bytes([0x41, 6, 1])),
        (1, bytes([6, 1, 2])),
        (1, bytes([0x41, 5, 0x80])),
        (1, bytes([0x41, 0x45, 0x80, 1])),
        (1, bytes([0x41, 0x85, 0x80, 0, 0, 1])),
    ]
    execution_values = [
        ("n", "0"),
        ("i", str(-(2**31))),
        ("i", str(2**31 - 1)),
        ("s", "u0073006500650064"),
    ]
    wait_deadlines = [-(2**63), -1, 0, 1, 2**63 - 1]
    for script_present, data in execution_scripts:
        data_length = 0 if data is None else len(data)
        offsets = list(
            dict.fromkeys(
                [-(2**31), -1, 0, data_length, data_length + 1, 2**31 - 1]
            )
        )
        for mode in ("execute", "resume"):
            for offset in offsets:
                for paused in range(3):
                    for initial_kind, initial_value in execution_values:
                        result.append(
                            "ink-execute "
                            f"{mode} {script_present} {byte_token(data)} {offset} "
                            f"{paused} {initial_kind} {initial_value}"
                        )
        for offset in offsets:
            for paused in (0, 1):
                result.append(
                    "ink-script-resume "
                    f"{paused} {script_present} {byte_token(data)} {offset}"
                )
                for wait_deadline in wait_deadlines:
                    result.append(
                        "ink-script-wait "
                        f"{wait_deadline} {paused} {script_present} "
                        f"{byte_token(data)} {offset}"
                    )

    absent_event_offsets = [-1] * 57
    present_event_offsets = absent_event_offsets.copy()
    present_event_offsets[1] = 0
    event_execution_cases: list[tuple[bytes | None, list[int] | None, int]] = [
        *((data, [0], 0) for data in dict.fromkeys(data for _, data in execution_scripts)),
        (bytes([2]), None, 0),
        (bytes([2]), [], 0),
        (bytes([2]), [-1], 0),
        (bytes([2]), [0], 1),
        (bytes([2]), [-(2**31)], 0),
        (bytes([2]), [-2], 0),
        (bytes([2]), [2**31 - 1], 0),
        (bytes([2]), absent_event_offsets, 1),
        (bytes([2]), absent_event_offsets, -1),
        (bytes([2]), absent_event_offsets, 57),
        (bytes([2]), present_event_offsets, 1),
    ]
    for data, event_offsets, event_code in event_execution_cases:
        for mode, debug in (("default", 0), ("debug", 0), ("debug", 1)):
            for initial_kind, initial_value in execution_values:
                for room_present in (0, 1):
                    for paused in (0, 1):
                        result.append(
                            "ink-script-execute "
                            f"{mode} {byte_token(data)} {int_array_token(event_offsets)} "
                            f"{event_code} {initial_kind} {initial_value} "
                            f"{room_present} {debug} {paused}"
                        )

    registry_script_id = ascii_token("script")
    for data, event_offsets, event_code in event_execution_cases:
        for initial_kind, initial_value in execution_values:
            for room_present in (0, 1):
                for paused in (0, 1):
                    result.append(
                        "ink-script-execute-id "
                        f"s{registry_script_id} {registry_script_id} "
                        f"{byte_token(data)} {int_array_token(event_offsets)} "
                        f"{event_code} {initial_kind} {initial_value} "
                        f"{room_present} {paused}"
                    )

    registry_ids = [
        None,
        [],
        [0],
        [0xD800],
        [ord(character) for character in "script"],
        [ord(character) for character in "other"],
    ]
    registry_tokens = ["null", "-"]
    for registry_id in registry_ids[1:]:
        encoded = utf16_token(registry_id)
        registry_tokens.extend((f"s{encoded}", f"o{encoded}"))
    for script_id in registry_ids:
        encoded_script_id = utf16_token(script_id)
        for registry in registry_tokens:
            for paused in (0, 1):
                result.append(
                    "ink-script-execute-id "
                    f"{registry} {encoded_script_id} h02 i-1 0 i 7 1 {paused}"
                )

    inventory_item_id = ascii_token("item")
    inventory_stack_lengths = (-1, 0, 1, 4, 17)
    for data, event_offsets, event_code in event_execution_cases:
        for stack_length in inventory_stack_lengths:
            for paused in (0, 1):
                result.append(
                    f"inventory-equip {stack_length} {inventory_item_id} "
                    f"s{inventory_item_id} {byte_token(data)} "
                    f"{int_array_token(event_offsets)} {event_code} {paused}"
                )
    for item_id in registry_ids:
        encoded_item_id = utf16_token(item_id)
        for registry in registry_tokens:
            for stack_length in inventory_stack_lengths:
                for paused in (0, 1):
                    result.append(
                        f"inventory-equip {stack_length} {encoded_item_id} {registry} "
                        f"h02 i0 0 {paused}"
                    )

    for data, event_offsets, event_code in event_execution_cases:
        for initial_kind, initial_value in execution_values:
            for debug in (0, 1):
                for paused in (0, 1):
                    result.append(
                        "room-event execute 1 null null "
                        f"{byte_token(data)} {int_array_token(event_offsets)} "
                        f"{event_code} {initial_kind} {initial_value} {debug} {paused}"
                    )
                    result.append(
                        f"room-event execute 0 {registry_script_id} s{registry_script_id} "
                        f"{byte_token(data)} {int_array_token(event_offsets)} "
                        f"{event_code} {initial_kind} {initial_value} {debug} {paused}"
                    )
    for script_id in registry_ids:
        encoded_script_id = utf16_token(script_id)
        for registry in registry_tokens:
            for initial_kind, initial_value in execution_values:
                for debug in (0, 1):
                    for paused in (0, 1):
                        result.append(
                            "room-event execute 0 "
                            f"{encoded_script_id} {registry} h02 i0 0 "
                            f"{initial_kind} {initial_value} {debug} {paused}"
                        )

    room_wrapper_programs: list[tuple[bytes | None, list[int] | None]] = []
    for wrapper_event_code in (1, 30):
        missing_offsets = [-1] * 31
        present_offsets = missing_offsets.copy()
        present_offsets[wrapper_event_code] = 0
        room_wrapper_programs.extend(
            (
                (bytes([2]), missing_offsets),
                (bytes([2]), present_offsets),
                (bytes([0x41, 6, 0]), present_offsets),
                (bytes([0x41, 5, 7]), present_offsets),
                (b"", present_offsets),
                (None, present_offsets),
            )
        )
    for mode, wrapper_event_code in (("name", 1), ("move", 30)):
        for data, event_offsets in room_wrapper_programs:
            for initial_script in (0, 1):
                for paused in (0, 1):
                    result.append(
                        f"room-event {mode} {initial_script} {registry_script_id} "
                        f"s{registry_script_id} {byte_token(data)} "
                        f"{int_array_token(event_offsets)} {wrapper_event_code} "
                        f"n 0 1 {paused}"
                    )
        for script_id in registry_ids:
            encoded_script_id = utf16_token(script_id)
            for registry in registry_tokens:
                for paused in (0, 1):
                    result.append(
                        f"room-event {mode} 0 {encoded_script_id} {registry} h02 "
                        f"{int_array_token([-1] * 31)} {wrapper_event_code} "
                        f"n 0 1 {paused}"
                    )

    hover_absent_offsets = [-1] * 55
    hover_present_offsets = hover_absent_offsets.copy()
    hover_present_offsets[54] = 0
    hover_negative_offset = hover_absent_offsets.copy()
    hover_negative_offset[54] = -2
    hover_maximum_offset = hover_absent_offsets.copy()
    hover_maximum_offset[54] = 2**31 - 1
    hover_programs: list[tuple[bytes | None, list[int] | None]] = [
        (data, hover_present_offsets)
        for data in dict.fromkeys(data for _, data in execution_scripts)
    ]
    hover_programs.extend(
        (
            (bytes([2]), None),
            (bytes([2]), []),
            (bytes([2]), [-1]),
            (bytes([2]), [-1] * 54),
            (bytes([2]), hover_absent_offsets),
            (bytes([2]), hover_negative_offset),
            (bytes([2]), hover_maximum_offset),
        )
    )
    for data, event_offsets in hover_programs:
        for registry in (
            "null",
            "-",
            f"s{registry_script_id}",
            f"o{registry_script_id}",
        ):
            for paused in (0, 1):
                result.append(
                    f"room-event hover 1 {registry_script_id} {registry} "
                    f"{byte_token(data)} {int_array_token(event_offsets)} 54 n 0 0 {paused}"
                )
    for script_id in registry_ids:
        encoded_script_id = utf16_token(script_id)
        for registry in registry_tokens:
            for paused in (0, 1):
                result.append(
                    f"room-event hover 0 {encoded_script_id} {registry} h02 "
                    f"{int_array_token(hover_present_offsets)} 54 n 0 0 {paused}"
                )

    item_script_id = ascii_token("item")
    for data in dict.fromkeys(data for _, data in execution_scripts):
        for paused in (0, 1):
            result.append(
                "ink-script-item-name "
                f"s{item_script_id} {item_script_id} {byte_token(data)} i-1,0 {paused}"
            )
    for event_offsets in (None, [], [-1], [-1, -1], [-1, -(2**31)], [-1, 2**31 - 1]):
        for paused in (0, 1):
            result.append(
                "ink-script-item-name "
                f"s{item_script_id} {item_script_id} h02 "
                f"{int_array_token(event_offsets)} {paused}"
            )
    for item_id in registry_ids:
        encoded_item_id = utf16_token(item_id)
        for registry in registry_tokens:
            for paused in (0, 1):
                result.append(
                    "ink-script-item-name "
                    f"{registry} {encoded_item_id} h02 i-1,-1 {paused}"
                )

    def compact_ink_node(command: int, children: list[bytes]) -> bytes:
        assert 0 <= command <= 63
        assert len(children) <= 2
        return bytes([(len(children) << 6) | command]) + b"".join(children)

    def extended_ink_node(command: int, children: list[bytes]) -> bytes:
        assert 0 <= command <= 63
        assert len(children) <= 255
        return bytes([0xC0 | command, len(children)]) + b"".join(children)

    ink_end = bytes([2])
    ink_integer_8 = bytes([5, 0x80])
    ink_integer_16 = bytes([0x45, 0x80, 0x01])
    ink_integer_32 = bytes([0x85, 0x80, 0x00, 0x00, 0x01])
    ink_integer_extended = bytes([0xC5, 7, 0x7F, 0xFF, 0xFF, 0xFF])
    ink_string = bytes([6, 1])
    one_string = script_array_token([[ord("x")]])

    # Every possible command byte and target command. Non-matching cases carry
    # a valid payload for the width/arity selected by the byte's high bits.
    for command_byte in range(256):
        command = command_byte & 63
        argument_count = command_byte >> 6
        payload = bytearray([command_byte])
        if command == 5:
            payload.extend(bytes([0x7F]) if argument_count == 0 else
                           bytes([0x80, 0x01]) if argument_count == 1 else
                           bytes([0x80, 0x00, 0x00, 0x01]))
        elif command == 6:
            payload.append(1)
        elif command != 2:
            if argument_count == 3:
                payload.append(0)
            else:
                payload.extend(ink_end * argument_count)
        token = byte_token(bytes(payload))
        for target_command in range(64):
            result.append(
                f"ink-has-command 1 {token} {one_string} 0 {target_command}"
            )

    nested_ink_trees = [
        ink_end,
        ink_integer_8,
        ink_integer_16,
        ink_integer_32,
        ink_integer_extended,
        ink_string,
        compact_ink_node(7, [ink_end]),
        compact_ink_node(8, [ink_integer_8, ink_string]),
        extended_ink_node(9, []),
        extended_ink_node(10, [ink_end]),
        extended_ink_node(11, [ink_integer_16, ink_string, ink_end]),
        compact_ink_node(
            12,
            [
                compact_ink_node(13, [ink_integer_32]),
                extended_ink_node(14, [ink_string, compact_ink_node(15, [ink_end])]),
            ],
        ),
    ]
    for tree in nested_ink_trees:
        for target_command in [*range(64), -1, 64, 2**31 - 1]:
            result.append(
                "ink-has-command "
                f"1 {byte_token(tree)} {one_string} 0 {target_command}"
            )
        for prefix_length in range(len(tree)):
            result.append(
                "ink-has-command "
                f"1 {byte_token(tree[:prefix_length])} {one_string} 0 64"
            )

    for script_present in (0, 1):
        for data in (None, b"", nested_ink_trees[-1]):
            data_length = 0 if data is None else len(data)
            for offset in [
                -(2**31), -2, -1, 0, 1, data_length - 1,
                data_length, data_length + 1, 2**31 - 1,
            ]:
                result.append(
                    "ink-has-command "
                    f"{script_present} {byte_token(data)} {one_string} {offset} 64"
                )
    for string_index in (0, 1, 2, 255):
        data = bytes([6, string_index])
        for strings in (None, [], [None], [[]], [[ord("x")]]):
            result.append(
                "ink-has-command "
                f"1 {byte_token(data)} {script_array_token(strings)} 0 64"
            )

    def random_ink_tree(depth: int) -> bytes:
        if depth == 0 or rng.randrange(4) == 0:
            return rng.choice(
                [ink_end, ink_integer_8, ink_integer_16, ink_integer_32, ink_string]
            )
        command = rng.choice([1, 3, 4, 7, 8, 9, 10, 11, 12, 13, 63])
        child_count = rng.randrange(4)
        children = [random_ink_tree(depth - 1) for _ in range(child_count)]
        return (
            compact_ink_node(command, children)
            if child_count <= 2 and rng.randrange(2) == 0
            else extended_ink_node(command, children)
        )

    random_ink_trees = [random_ink_tree(4) for _ in range(2048)]
    for tree in random_ink_trees:
        target_command = rng.choice([*range(64), -1, 64])
        result.append(
            "ink-has-command "
            f"1 {byte_token(tree)} {one_string} 0 {target_command}"
        )
        if tree:
            prefix_length = rng.randrange(len(tree))
            result.append(
                "ink-has-command "
                f"1 {byte_token(tree[:prefix_length])} {one_string} 0 64"
            )

    absent_events = [-1] * 57
    event_one = absent_events.copy()
    event_one[1] = 0
    event_last = absent_events.copy()
    event_last[56] = 0
    short_early_event = [-1, 0]
    for data, strings, offsets, target_command in [
        (None, None, None, 1),
        (None, None, [], 1),
        (None, None, [-1], 1),
        (None, None, absent_events, 1),
        (nested_ink_trees[-1], [[ord("x")]], event_one, 15),
        (nested_ink_trees[-1], [[ord("x")]], event_one, 62),
        (nested_ink_trees[-1], [[ord("x")]], event_last, 15),
        (nested_ink_trees[-1], [[ord("x")]], short_early_event, 12),
        (nested_ink_trees[-1], [[ord("x")]], short_early_event, 62),
        (ink_string, None, event_one, 63),
        (b"", [[ord("x")]], event_one, 63),
    ]:
        result.append(
            "ink-script-has-command "
            f"{byte_token(data)} {script_array_token(strings)} "
            f"{int_array_token(offsets)} {target_command}"
        )

    for tree in random_ink_trees[:1024]:
        offsets = absent_events.copy()
        event_code = rng.randrange(1, 57)
        offsets[event_code] = 0
        target_command = rng.choice([*range(64), -1, 64])
        result.append(
            "ink-script-has-command "
            f"{byte_token(tree)} {one_string} {int_array_token(offsets)} "
            f"{target_command}"
        )

    result.extend(("ink-stop 0", "ink-stop 1"))

    panel_arrays: list[list[int] | None] = [
        None,
        [],
        [0],
        [-(2**31), -1, 0, 2**31 - 1],
        [11, 22, 33, 44, 55],
    ]
    for old_panel in panel_arrays:
        for size in (-3, -1, 0, 1, 4, 8):
            for old_id, panel_id in ((0, 1), (2**31 - 1, -(2**31)), (-9, -9)):
                result.append(
                    "panel-new "
                    f"{old_id} {int_array_token(old_panel)} {panel_id} {size}"
                )
    for command in ("panel-max", "panel-health", "panel-bar", "panel-time"):
        for panel in panel_arrays:
            for index in (-(2**31), -1, 0, 1, 3, 4, 2**31 - 1):
                for new_value in edges:
                    result.append(
                        f"{command} 77 {int_array_token(panel)} {index} {new_value}"
                    )
    rectangles = [
        (-(2**31), 2**31 - 1, -(2**31), 2**31 - 1),
        (0, 0, 0, 0),
        (-1, 1, -1, 1),
        (2**31 - 1, -(2**31), 2**31 - 1, -(2**31)),
        (-(2**31), -(2**31), 2**31 - 1, 2**31 - 1),
        (2**31 - 1, 2**31 - 1, -(2**31), -(2**31)),
    ]
    for left, right, top, bottom in rectangles:
        x_probes = [left, right, -(2**31), 2**31 - 1, -1, 0, 1]
        y_probes = [top, bottom, -(2**31), 2**31 - 1, -1, 0, 1]
        for visible in (0, 1):
            for active in (0, 1):
                for x in x_probes:
                    for y in y_probes:
                        result.append(
                            "room-is-over "
                            f"{visible} {active} {left} {right} {top} {bottom} {x} {y}"
                        )
    for _ in range(128):
        left, right, top, bottom, x, y = (
            rng.randint(-(2**31), 2**31 - 1) for _ in range(6)
        )
        for visible in (0, 1):
            for active in (0, 1):
                result.append(
                    "room-is-over "
                    f"{visible} {active} {left} {right} {top} {bottom} {x} {y}"
                )

    reset_rng = random.Random(0x52455345544C4F4144)
    for resources in choice_arrays:
        for thread_present in (0, 1):
            for loading_mode in edges:
                result.append(
                    "reset-load "
                    f"{thread_present} {loading_mode} {choice_array_token(resources)}"
                )
    for _ in range(1024):
        resources = (
            None
            if reset_rng.randrange(8) == 0
            else [
                None
                if reset_rng.randrange(5) == 0
                else reset_rng.randint(-(2**31), 2**31 - 1)
                for _ in range(reset_rng.randrange(17))
            ]
        )
        result.append(
            "reset-load "
            f"{reset_rng.randrange(2)} "
            f"{reset_rng.randint(-(2**31), 2**31 - 1)} "
            f"{choice_array_token(resources)}"
        )

    menu_add_ids: list[int | None] = [None, -(2**31), -1, 0, 2**31 - 1]
    menu_add_ints = [-(2**31), -1, 0, 1, 2**31 - 1]
    menu_add_texts: list[list[int] | None] = [
        None,
        [],
        [0],
        [0xD800],
        [ord(character) for character in "choice"],
    ]
    for ids in choice_arrays:
        for texts in script_id_arrays:
            for update_body_lines in (0, 1):
                for update_menu in (0, 1):
                    for choice_id in menu_add_ids:
                        for choice_text in menu_add_texts:
                            result.append(
                                "menu-add-object "
                                f"{choice_array_token(ids)} {script_array_token(texts)} "
                                f"{update_body_lines} {update_menu} "
                                f"{'n' if choice_id is None else choice_id} "
                                f"{utf16_token(choice_text)}"
                            )
                    for choice_id in menu_add_ints:
                        for choice_text in menu_add_texts:
                            result.append(
                                "menu-add-int "
                                f"{choice_array_token(ids)} {script_array_token(texts)} "
                                f"{update_body_lines} {update_menu} {choice_id} "
                                f"{utf16_token(choice_text)}"
                            )
    menu_add_rng = random.Random(0x4D454E55414444)
    for index in range(4096):
        ids = (
            None
            if menu_add_rng.randrange(8) == 0
            else [
                None
                if menu_add_rng.randrange(5) == 0
                else menu_add_rng.randint(-(2**31), 2**31 - 1)
                for _ in range(menu_add_rng.randrange(13))
            ]
        )
        texts = (
            None
            if menu_add_rng.randrange(8) == 0
            else [
                None
                if menu_add_rng.randrange(5) == 0
                else [
                    menu_add_rng.randrange(1 << 16)
                    for _ in range(menu_add_rng.randrange(9))
                ]
                for _ in range(menu_add_rng.randrange(13))
            ]
        )
        choice_text = (
            None
            if menu_add_rng.randrange(6) == 0
            else [
                menu_add_rng.randrange(1 << 16)
                for _ in range(menu_add_rng.randrange(17))
            ]
        )
        command = "menu-add-object" if index & 1 else "menu-add-int"
        choice_id = (
            "n"
            if command == "menu-add-object" and menu_add_rng.randrange(5) == 0
            else str(menu_add_rng.randint(-(2**31), 2**31 - 1))
        )
        result.append(
            f"{command} {choice_array_token(ids)} {script_array_token(texts)} "
            f"{menu_add_rng.randrange(2)} {menu_add_rng.randrange(2)} "
            f"{choice_id} {utf16_token(choice_text)}"
        )

    result.extend(
        (
            "request-from-input null",
            "request-from-input -",
            "request-from-input h03",
            "request-from-input h0301",
            "request-from-input h0200",
        )
    )
    stream_byte_edges = (0, 1, 2, 15, 16, 127, 128, 254, 255)
    result.extend(
        f"read-string h{first:02x}00"
        for first in range(1 << 8)
    )
    for first in range(1 << 8):
        second_values = set(stream_byte_edges)
        second_values.update((first, first ^ 0xFF, (first + 1) & 0xFF))
        for second in sorted(second_values):
            token = f"h{first:02x}{second:02x}00a5"
            result.append(f"read-string {token}")
    for resource_id in range(1 << 8):
        transform_values = set(stream_byte_edges)
        transform_values.update(
            (resource_id, resource_id ^ 0xFF, (resource_id + 1) & 0xFF)
        )
        for image_transform in sorted(transform_values):
            if resource_id == 0:
                encoded = bytes((2, 0, image_transform, 0xA5))
            else:
                encoded = bytes((2, resource_id, 0, image_transform, 0xA5))
            result.append(f"request-from-input {byte_token(encoded)}")
            encoded = bytes((3, resource_id, image_transform, 0xA5))
            result.append(f"request-from-input {byte_token(encoded)}")
    for request_type in range(1 << 8):
        if request_type not in (2, 3):
            for payload in (b"\0", b"a\0", b"a\0\xA5", b"\x80\0", b"\xFF\0\xA5"):
                result.append(
                    f"request-from-input {byte_token(bytes((request_type,)) + payload)}"
                )
    stream_rng = random.Random(0x53545245414D)
    for _ in range(4096):
        string_bytes = bytes(
            stream_rng.randrange(1 << 8)
            for _ in range(stream_rng.randrange(32))
        )
        trailing = bytes(
            stream_rng.randrange(1 << 8)
            for _ in range(stream_rng.randrange(5))
        )
        result.append(
            f"read-string {byte_token(string_bytes + bytes((0,)) + trailing)}"
        )
        request_type = stream_rng.randrange(1 << 8)
        if request_type == 2:
            encoded = bytes((2,)) + string_bytes + bytes(
                (0, stream_rng.randrange(1 << 8))
            ) + trailing
        elif request_type == 3:
            encoded = bytes(
                (3, stream_rng.randrange(1 << 8), stream_rng.randrange(1 << 8))
            ) + trailing
        else:
            encoded = bytes((request_type,)) + string_bytes + bytes((0,)) + trailing
        result.append(f"request-from-input {byte_token(encoded)}")

    find_inputs: list[bytes | None] = [
        None,
        b"",
        b"\0",
        b" ",
        b"abc",
        b"xxabc\xA5",
        bytes(range(1 << 8)),
    ]
    find_targets: list[list[int] | None] = [
        None,
        [],
        [0],
        [0x20],
        [0x7F],
        [0x80],
        [0xFF],
        [0x100],
        [0xD800],
        [0xFFFF],
        [ord(character) for character in "abc"],
        [ord(character) for character in "aab"],
    ]
    for data in find_inputs:
        for target in find_targets:
            result.append(f"find {byte_token(data)} {utf16_token(target)}")
    for unit in range(1 << 16):
        data = bytes((unit, 0xA5)) if unit <= 0xFF else b"\0"
        result.append(f"find {byte_token(data)} {utf16_token([unit])}")
    find_rng = random.Random(0x46494E44)
    for _ in range(4096):
        data = bytes(
            find_rng.randrange(1 << 8) for _ in range(find_rng.randrange(65))
        )
        if data and find_rng.randrange(2) == 0:
            start = find_rng.randrange(len(data))
            end = find_rng.randrange(start, len(data) + 1)
            target = list(data[start:end])
        else:
            target = [
                find_rng.randrange(1 << 16)
                for _ in range(find_rng.randrange(9))
            ]
        result.append(f"find {byte_token(data)} {utf16_token(target)}")

    for unit in range(1 << 16):
        result.append(f"write-string present -1 {utf16_token([unit])}")
    write_values: list[list[int] | None] = [
        None,
        [],
        [0],
        [0x7F, 0x80, 0xFF, 0x100, 0xD800, 0xFFFF],
        [ord(character) for character in "savePoint"],
    ]
    for string_value in write_values:
        length = 0 if string_value is None else len(string_value)
        for output_kind in ("null", "present"):
            for fail_at in (-1, *range(length + 2)):
                result.append(
                    f"write-string {output_kind} {fail_at} "
                    f"{utf16_token(string_value)}"
                )
    write_rng = random.Random(0x5752495445)
    for _ in range(4096):
        string_value = [
            write_rng.randrange(1 << 16)
            for _ in range(write_rng.randrange(33))
        ]
        fail_at = write_rng.randrange(-1, len(string_value) + 2)
        result.append(
            f"write-string present {fail_at} {utf16_token(string_value)}"
        )

    def modified_utf_payload(units: list[int]) -> bytes:
        encoded = bytearray()
        for unit in units:
            if 0x0001 <= unit <= 0x007F:
                encoded.append(unit)
            elif unit <= 0x07FF:
                encoded.extend((0xC0 | (unit >> 6), 0x80 | (unit & 0x3F)))
            else:
                encoded.extend(
                    (
                        0xE0 | (unit >> 12),
                        0x80 | ((unit >> 6) & 0x3F),
                        0x80 | (unit & 0x3F),
                    )
                )
        return bytes(encoded)

    def modified_utf_record(units: list[int]) -> bytes:
        payload = modified_utf_payload(units)
        assert len(payload) <= 0xFFFF
        return len(payload).to_bytes(2, "big") + payload

    def string_list_blob(values: list[list[int]]) -> bytes:
        assert len(values) <= 0xFF
        return bytes((len(values),)) + b"".join(
            modified_utf_record(value) for value in values
        )

    result.extend(
        (
            "read-string-list null null -1 0",
            "read-string-list - null -1 1",
        )
    )
    for unit in range(1 << 16):
        result.append(
            "read-string-list "
            f"{byte_token(string_list_blob([[unit]]))} - -1 0"
        )

    list_values: list[list[list[int]]] = [
        [],
        [[]],
        [[0]],
        [[0x7F, 0x80, 0x7FF, 0x800, 0xD800, 0xFFFF]],
        [[ord(character) for character in "savePoint"]],
        [[ord(character) for character in "$0"]],
        [[ord(character) for character in "$1"]],
        [[ord(character) for character in "$-1"]],
        [
            [ord(character) for character in "first"],
            [ord(character) for character in "savePoint"],
            [ord(character) for character in "$0"],
            [ord(character) for character in "last"],
        ],
    ]
    list_game_texts: list[list[list[int] | None] | None] = [
        None,
        [],
        [None],
        [[]],
        [[ord(character) for character in "localized"]],
        [None, [ord(character) for character in "one"]],
    ]
    for values in list_values:
        blob = string_list_blob(values)
        for game_texts in list_game_texts:
            for loading_mode in (-1, 0, 1):
                for old_save in (0, 1):
                    result.append(
                        "read-string-list "
                        f"{byte_token(blob)} {script_array_token(game_texts)} "
                        f"{loading_mode} {old_save}"
                    )
        for truncation in range(len(blob)):
            result.append(
                "read-string-list "
                f"{byte_token(blob[:truncation])} - 0 0"
            )

    malformed_payloads = set()
    continuation_edges = (0x00, 0x3F, 0x7F, 0x80, 0xBF, 0xC0, 0xFF)
    for first in range(1 << 8):
        malformed_payloads.add(bytes((first,)))
        for second in continuation_edges:
            malformed_payloads.add(bytes((first, second)))
    for payload in sorted(malformed_payloads):
        record = len(payload).to_bytes(2, "big") + payload
        result.append(
            f"read-string-list {byte_token(bytes((1,)) + record + b'\xA5')} - -1 0"
        )
    for declared_length in (1, 2, 3, 15, 255, 0xFFFF):
        for available in (0, 1, 2, 3, 7):
            payload = bytes((index * 73 + 0x80) & 0xFF for index in range(available))
            blob = bytes((1,)) + declared_length.to_bytes(2, "big") + payload
            result.append(f"read-string-list {byte_token(blob)} - 0 0")

    list_rng = random.Random(0x4C495354)
    for _ in range(4096):
        values = [
            [
                list_rng.randrange(1 << 16)
                for _ in range(list_rng.randrange(17))
            ]
            for _ in range(list_rng.randrange(9))
        ]
        if values and list_rng.randrange(4) == 0:
            values[list_rng.randrange(len(values))] = [
                ord(character) for character in ("savePoint" if list_rng.randrange(2) else "$0")
            ]
        game_texts = (
            None
            if list_rng.randrange(5) == 0
            else [
                None
                if list_rng.randrange(5) == 0
                else [
                    list_rng.randrange(1 << 16)
                    for _ in range(list_rng.randrange(9))
                ]
                for _ in range(list_rng.randrange(5))
            ]
        )
        result.append(
            "read-string-list "
            f"{byte_token(string_list_blob(values))} "
            f"{script_array_token(game_texts)} "
            f"{list_rng.choice((-1, 0, 1))} {list_rng.randrange(2)}"
        )
    return result


def java_command(classes: Path) -> list[str]:
    stub_sources = compile_named_java.java_sources(compile_named_java.STUB_ROOT)
    app_sources = compile_named_java.java_sources(compile_named_java.SOURCE_ROOT)
    compile_named_java.run_javac(stub_sources, classes)
    compile_named_java.run_javac([*app_sources, HARNESS], classes, classes)
    return ["java", "-cp", str(classes), "defpackage.OrphanJavaPureOracle"]


def original_java_command(classes: Path, jar: Path) -> list[str]:
    """Compile the reflection harness and call the real recovered classfile."""
    compile_named_java.run_javac([ORIGINAL_HARNESS], classes, classes)
    return [
        "java",
        "-cp",
        f"{classes}:{jar}",
        "defpackage.OrphanOriginalPureOracle",
    ]


def original_builds() -> list[corpus.Build]:
    manifest = corpus.load_manifest()
    wanted = [manifest["baseline"], manifest["naming_reference"]]
    indexed = {build.build_id: build for build in corpus.builds()}
    missing = [build_id for build_id in wanted if build_id not in indexed]
    if missing:
        raise RuntimeError(f"oracle builds are absent from the corpus: {missing}")
    return [indexed[build_id] for build_id in wanted]


def output(command: list[str], payload: str) -> list[str]:
    result = subprocess.run(
        command,
        input=payload,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"oracle command failed ({' '.join(command)}):\n{result.stderr}"
        )
    return result.stdout.splitlines()


def run(*, self_test: bool, java_only: bool) -> int:
    validate_baseline_only_scopes()
    cases = requests()
    payload = "\n".join(cases) + "\n"
    if not java_only:
        subprocess.run(
            ["cargo", "build", "-q", "-p", "orphan-pure-oracle"],
            cwd=ROOT,
            check=True,
        )
    with tempfile.TemporaryDirectory(prefix="orphan-pure-java-") as temporary:
        temporary_path = Path(temporary)
        classes = temporary_path / "classes"
        classes.mkdir()
        java = output(java_command(classes), payload)
        originals: list[tuple[str, list[str]]] = []
        oracle_builds = original_builds()
        for index, build in enumerate(oracle_builds):
            jar = temporary_path / f"original-{index}.jar"
            jar.write_bytes(build.payload)
            originals.append(
                (build.build_id, output(original_java_command(classes, jar), payload))
            )
    rust = None if java_only else output([str(RUST_BINARY)], payload)
    if self_test:
        if java_only:
            label, values = originals[0]
            values[len(values) // 2] = "__MUTATED__"
            originals[0] = (label, values)
        elif rust:
            rust[len(rust) // 2] = "__MUTATED__"
    lengths = {"canonical Java": len(java)}
    if rust is not None:
        lengths["Rust"] = len(rust)
    lengths.update({f"original {name}": len(values) for name, values in originals})
    if any(length != len(cases) for length in lengths.values()):
        print(
            f"oracle output length mismatch: cases={len(cases)} outputs={lengths}",
            file=sys.stderr,
        )
        return 1
    candidates = [(f"original {name}", values) for name, values in originals]
    if rust is not None:
        candidates.insert(0, ("Rust", rust))
    mismatches = []
    baseline_id = oracle_builds[0].build_id
    for label, candidate in candidates:
        mismatches.extend(
            (index, case, "canonical Java", left, label, right)
            for index, (case, left, right) in enumerate(
                zip(cases, java, candidate, strict=True)
            )
            if left != right
            and not (
                label.startswith("original ")
                and label != f"original {baseline_id}"
                and case.split(" ", 1)[0] in BASELINE_ONLY_COMMANDS
            )
        )
    if self_test:
        if len(mismatches) != 1:
            print(f"SELF-TEST FAILED: expected one injected mismatch, found {len(mismatches)}")
            return 3
        subject = "recovered-JAR" if java_only else "Rust"
        print(f"self-test OK: a one-result {subject} oracle mutation was rejected (R3)")
        return 0
    if mismatches:
        for index, case, left_label, left, right_label, right in mismatches[:12]:
            print(
                f"case {index} {case}: {left_label}={left}, {right_label}={right}",
                file=sys.stderr,
            )
        return 1
    baseline_only_count = sum(
        case.split(" ", 1)[0] in BASELINE_ONLY_COMMANDS for case in cases
    )
    rust_authority = " == Rust" if rust is not None else ""
    print(
        "pure-method oracle OK: recovered baseline == canonical Java"
        f"{rust_authority} for {len(cases)} edge cases; naming-reference JAR "
        f"also agrees on {len(cases) - baseline_only_count} non-variant cases "
        f"({baseline_only_count} reviewed baseline input-policy cases)"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument(
        "--java-only",
        action="store_true",
        help="compare the recovered JARs with canonical Java without building Rust",
    )
    arguments = parser.parse_args()
    return run(self_test=arguments.self_test, java_only=arguments.java_only)


if __name__ == "__main__":
    raise SystemExit(main())
