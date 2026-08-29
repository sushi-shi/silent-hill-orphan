#!/usr/bin/env python3
"""Validate reviewed original-bytecode / javac-AST / syn-AST port claims.

Reviewed method bodies and state declarations use the same exhaustive rule:
every javac and syn node has exactly one semantic owner or a reasoned one-sided
adaptation.  A reverse syn inventory also prevents functions, value
declarations, and owner containers from entering the strict crate unreviewed.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import subprocess
import sys
import tomllib
import zipfile
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "transliteration" / "audits" / "method-audit.toml"
CLASS_MAP = (
    ROOT / "java" / "reconstruction" / "mappings" / "canonical" / "classes.toml"
)
sys.path.insert(0, str(ROOT / "tools" / "corpus"))
sys.path.insert(0, str(ROOT / "tools" / "transliteration"))

import classfile  # noqa: E402
import corpus  # noqa: E402
import validate_ast_authority  # noqa: E402


@dataclass(frozen=True)
class RustEvidence:
    ast: str
    nodes: tuple[str, ...]


RUST_FUNCTION_PREFIXES = ("fn:", "impl:")
RUST_VALUE_PREFIXES = ("field:", "const:", "static:", "variant:")
RUST_CONTAINER_PREFIXES = ("struct:", "enum:")


def digest(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def original_methods() -> dict[tuple[str, str, str], classfile.MethodSymbol]:
    selected = corpus.load_manifest()["baseline"]
    build = next(item for item in corpus.builds() if item.build_id == selected)
    result = {}
    with zipfile.ZipFile(io.BytesIO(build.payload)) as archive:
        for name in sorted(archive.namelist()):
            if not name.endswith(".class") or "/" in name:
                continue
            info = classfile.parse_class(name, archive.read(name))
            for method in info.methods:
                key = (info.internal_name, method.name, method.descriptor)
                if key in result:
                    raise RuntimeError(f"duplicate original method {key}")
                result[key] = method
    return result


def original_fields() -> dict[tuple[str, str, str], classfile.FieldSymbol]:
    selected = corpus.load_manifest()["baseline"]
    build = next(item for item in corpus.builds() if item.build_id == selected)
    result = {}
    with zipfile.ZipFile(io.BytesIO(build.payload)) as archive:
        for name in sorted(archive.namelist()):
            if not name.endswith(".class") or "/" in name:
                continue
            info = classfile.parse_class(name, archive.read(name))
            for field in info.fields:
                key = (info.internal_name, field.name, field.descriptor)
                if key in result:
                    raise RuntimeError(f"duplicate original field {key}")
                result[key] = field
    return result


def canonical_classes() -> dict[str, str]:
    document = tomllib.loads(CLASS_MAP.read_text(encoding="utf-8"))
    return {
        entry["original_name"]: entry["semantic_name"]
        for entry in document.get("classes", [])
    }


def rust_asts(files: set[str]) -> dict[tuple[str, str], RustEvidence]:
    if not files:
        return {}
    paths = [str(ROOT / file) for file in sorted(files)]
    output = subprocess.run(
        [
            "cargo",
            "run",
            "-q",
            "-p",
            "orphan-ast-audit",
            "--",
            "--production-only",
            *paths,
        ],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    ).stdout
    absolute_to_relative = {str(ROOT / file): file for file in files}
    result = {}
    for line in output.splitlines():
        path, item, encoded_ast, encoded_nodes = line.split("\t", 3)
        key = (absolute_to_relative[path], item)
        raw_nodes = bytes.fromhex(encoded_nodes).decode("utf-8")
        result[key] = RustEvidence(
            bytes.fromhex(encoded_ast).decode("utf-8"),
            tuple(raw_nodes.splitlines()) if raw_nodes else (),
        )
    return result


def rust_node_ref(value: str, target_count: int, label: str) -> tuple[int, int]:
    try:
        target, node = (int(part) for part in value.split(":", 1))
    except (AttributeError, TypeError, ValueError) as error:
        raise ValueError(f"{label}: invalid Rust node reference {value!r}") from error
    if target < 0 or target >= target_count or node < 0:
        raise ValueError(f"{label}: invalid Rust node reference {value!r}")
    return target, node


def validate_semantic_crosswalk(
    entry: dict,
    label: str,
    java_evidence: validate_ast_authority.AstEvidence,
    rust_evidence: list[RustEvidence],
) -> list[str]:
    """Require every Java and Rust AST node to have one semantic owner.

    Bidirectional ``operation`` rows compare nodes under one written semantic
    operation. A one-sided node is legal only in an ``adaptation`` row with a
    reason. Duplicate, missing, and out-of-range nodes are all hard failures.
    """

    errors: list[str] = []
    expected_java = set(range(len(java_evidence.nodes)))
    expected_rust = {
        (target, node)
        for target, evidence in enumerate(rust_evidence)
        for node in range(len(evidence.nodes))
    }
    if entry.get("java_node_count") != len(expected_java):
        errors.append(
            f"{label}: Java node ratchet says {entry.get('java_node_count')}, "
            f"AST has {len(expected_java)}"
        )
    actual_rust_counts = [len(evidence.nodes) for evidence in rust_evidence]
    if entry.get("rust_node_counts") != actual_rust_counts:
        errors.append(
            f"{label}: Rust node ratchet says {entry.get('rust_node_counts')}, "
            f"ASTs have {actual_rust_counts}"
        )

    covered_java: set[int] = set()
    covered_rust: set[tuple[int, int]] = set()

    def add_java(node: object, context: str) -> None:
        if not isinstance(node, int) or node not in expected_java:
            errors.append(f"{label}: {context} has invalid Java node {node!r}")
        elif node in covered_java:
            errors.append(f"{label}: Java node {node} is covered more than once")
        else:
            covered_java.add(node)

    def add_rust(value: object, context: str) -> None:
        try:
            node = rust_node_ref(value, len(rust_evidence), label)  # type: ignore[arg-type]
        except ValueError as error:
            errors.append(str(error))
            return
        if node not in expected_rust:
            errors.append(f"{label}: {context} has invalid Rust node {value!r}")
        elif node in covered_rust:
            errors.append(f"{label}: Rust node {value} is covered more than once")
        else:
            covered_rust.add(node)

    def add_java_range(value: object, context: str) -> None:
        if (
            not isinstance(value, list)
            or len(value) != 2
            or not all(isinstance(part, int) for part in value)
            or value[0] > value[1]
        ):
            errors.append(f"{label}: {context} has invalid Java range {value!r}")
            return
        for node in range(value[0], value[1] + 1):
            add_java(node, context)

    def add_rust_range(value: object, context: str) -> None:
        if not isinstance(value, dict):
            errors.append(f"{label}: {context} has invalid Rust range {value!r}")
            return
        target, start, end = value.get("target"), value.get("start"), value.get("end")
        if (
            not isinstance(target, int)
            or not isinstance(start, int)
            or not isinstance(end, int)
            or start > end
        ):
            errors.append(f"{label}: {context} has invalid Rust range {value!r}")
            return
        for node in range(start, end + 1):
            add_rust(f"{target}:{node}", context)

    for index, operation in enumerate(entry.get("operation", [])):
        context = f"operation {index} ({operation.get('semantic', 'unnamed')})"
        java_nodes = operation.get("java_nodes", [])
        java_ranges = operation.get("java_node_ranges", [])
        rust_nodes = operation.get("rust_nodes", [])
        rust_ranges = operation.get("rust_node_ranges", [])
        if not operation.get("semantic"):
            errors.append(f"{label}: {context} needs a semantic label")
        if (not java_nodes and not java_ranges) or (not rust_nodes and not rust_ranges):
            errors.append(f"{label}: {context} must cover both Java and Rust")
        for node in java_nodes:
            add_java(node, context)
        for value in java_ranges:
            add_java_range(value, context)
        for node in rust_nodes:
            add_rust(node, context)
        for value in rust_ranges:
            add_rust_range(value, context)

    for index, adaptation in enumerate(entry.get("adaptation", [])):
        context = f"adaptation {index}"
        side = adaptation.get("side")
        if not adaptation.get("reason"):
            errors.append(f"{label}: {context} needs a reason")
        if side == "java":
            if adaptation.get("rust_nodes") or adaptation.get("rust_node_ranges"):
                errors.append(f"{label}: {context} is Java-only but has Rust nodes")
            for node in adaptation.get("java_nodes", []):
                add_java(node, context)
            for value in adaptation.get("java_node_ranges", []):
                add_java_range(value, context)
        elif side == "rust":
            if adaptation.get("java_nodes") or adaptation.get("java_node_ranges"):
                errors.append(f"{label}: {context} is Rust-only but has Java nodes")
            for node in adaptation.get("rust_nodes", []):
                add_rust(node, context)
            for value in adaptation.get("rust_node_ranges", []):
                add_rust_range(value, context)
        else:
            errors.append(f"{label}: {context} side must be 'java' or 'rust'")

    missing_java = sorted(expected_java - covered_java)
    missing_rust = sorted(expected_rust - covered_rust)
    if missing_java:
        errors.append(f"{label}: uncovered Java nodes {missing_java}")
    if missing_rust:
        errors.append(
            f"{label}: uncovered Rust nodes "
            f"{[f'{target}:{node}' for target, node in missing_rust]}"
        )
    return errors


def java_evidence_facts(entry: dict, methods: dict, java: dict) -> dict:
    method = methods[(entry["java_class"], entry["java_name"], entry["descriptor"])]
    java_evidence = java[(entry["canonical_class"], entry["java_item"])]
    return {
        "code_sha256": method.code_sha256,
        "opcode_sha256": method.opcode_sha256,
        "java_ast_sha256": digest(java_evidence.ast),
        "java_nodes_sha256": digest("\n".join(java_evidence.nodes)),
        "java_node_count": len(java_evidence.nodes),
    }


def java_field_evidence_facts(entry: dict, fields: dict, java: dict) -> dict:
    fields[(entry["java_class"], entry["java_name"], entry["descriptor"])]
    java_evidence = java[(entry["canonical_class"], entry["java_item"])]
    return {
        "java_ast_sha256": digest(java_evidence.ast),
        "java_nodes_sha256": digest("\n".join(java_evidence.nodes)),
        "java_node_count": len(java_evidence.nodes),
    }


def rust_evidence_facts(target: dict, rust: dict) -> dict:
    evidence = rust[(target["file"], target["item"])]
    return {
        "ast_sha256": digest(evidence.ast),
        "nodes_sha256": digest("\n".join(evidence.nodes)),
        "node_count": len(evidence.nodes),
    }


FIELD_REQUESTS = [
    ("Ext", "SilentHillGame", "HUD_ammoNumWidth", "I", "SilentHillGameStatics", "hud_ammo_number_width"),
    ("Ext", "SilentHillGame", "HUD_ammoUpdateNeeded", "Z", "SilentHillGameStatics", "hud_ammo_update_needed"),
    ("Ext", "SilentHillGame", "INK_menu_logo", "Ljavax/microedition/lcdui/Image;", "SilentHillGameStatics", "ink_menu_logo"),
    ("Cheat", "CheatController", "lastKey", "I", "CheatControllerStatics", "last_key"),
    ("M", "Application", "tickBasedTimeValue", "I", "ApplicationState", "tick_based_time_value"),
    ("M", "Application", "canvas", "Ljavax/microedition/lcdui/Canvas;", "ApplicationState", "canvas_instance"),
    ("M", "Application", "canvasWidth", "I", "ApplicationState", "canvas_width"),
    ("M", "Application", "painting", "Z", "ApplicationState", "painting"),
    ("M", "Application", "FADE_FRAMES", "I", "ApplicationState", "fade_frames"),
    ("M", "Application", "DEMO_FRAMES", "I", "ApplicationState", "demo_frames"),
    ("M", "Application", "keyLastPressed", "I", "ApplicationState", "key_last_pressed"),
    ("M", "Application", "keyNew", "Z", "ApplicationState", "key_new"),
    ("M", "Application", "keyPressed", "Z", "ApplicationState", "key_pressed"),
    ("M", "Application", "loadBarActive", "Z", "ApplicationState", "load_bar_active"),
    ("M", "Application", "gotoDissolveFXCounter", "I", "ApplicationState", "goto_dissolve_fx_counter"),
    ("M", "Application", "loadingMode", "I", "ApplicationState", "loading_mode"),
    ("M", "Application", "loadThread", "Ljava/lang/Thread;", "ApplicationState", "load_thread"),
    ("M", "Application", "roomRepaintThread", "Ljava/lang/Thread;", "ApplicationState", "room_repaint_thread"),
    ("M", "Application", "resourceImportants", "Ljava/util/Vector;", "ApplicationState", "resource_importants"),
    ("M", "Application", "resourcesToDownload", "Ljava/util/Vector;", "ApplicationState", "resources_to_download"),
    ("M", "Application", "gameId", "Ljava/lang/String;", "ApplicationState", "game_id"),
    ("M", "Application", "gameTexts", "[Ljava/lang/String;", "ApplicationState", "game_texts"),
    ("M", "Application", "languages", "[Ljava/lang/String;", "ApplicationState", "languages"),
    ("M", "Application", "resourceHeapSourceLRE", "[I", "ApplicationState", "resource_heap_sources"),
    ("M", "Application", "resourceSCData", "[B", "ApplicationState", "resource_sc_data"),
    ("M", "Application", "resourceSCCurrentSize", "I", "ApplicationState", "resource_sc_current_size"),
    ("M", "Application", "randomInstance", "Ljava/util/Random;", "ApplicationState", "random_instance"),
    ("M", "Application", "runtime", "Ljava/lang/Runtime;", "ApplicationState", "runtime_instance"),
    ("M", "Application", "midlet", "LM;", "ApplicationState", "midlet_instance"),
    ("M", "Application", "inkServerVariables", "Ljava/util/Hashtable;", "ApplicationState", "ink_server_variables"),
    ("M", "Application", "inkServerHint", "Ljava/util/Hashtable;", "ApplicationState", "ink_server_hints"),
    ("M", "Application", "gameChangedSinceLastSave", "Z", "ApplicationState", "game_changed_since_last_save"),
    ("M", "Application", "saveIsPossible", "Z", "ApplicationState", "save_is_possible"),
    ("M", "Application", "curSoundMode", "Z", "ApplicationState", "cur_sound_mode"),
    ("LoadRequest", "ResourceRequest", "type", "I", "ResourceRequestState", "resource_type"),
    ("LoadRequest", "ResourceRequest", "integerID", "I", "ResourceRequestState", "integer_id"),
    ("LoadRequest", "ResourceRequest", "stringID", "Ljava/lang/String;", "ResourceRequestState", "string_id"),
    ("LoadRequest", "ResourceRequest", "imageTransform", "I", "ResourceRequestState", "image_transform"),
    ("Resource", "GameResource", "type", "I", "GameResourceState", "resource_type"),
    ("Resource", "GameResource", "ID", "Ljava/lang/Object;", "GameResourceState", "id"),
    ("Resource", "GameResource", "image", "Ljavax/microedition/lcdui/Image;", "GameResourceState", "image"),
    ("Resource", "GameResource", "imageWidth", "I", "GameResourceState", "image_width"),
    ("Resource", "GameResource", "imageHeight", "I", "GameResourceState", "image_height"),
    ("Resource", "GameResource", "imageRegPointX", "I", "GameResourceState", "image_registration_x"),
    ("Resource", "GameResource", "imageRegPointY", "I", "GameResourceState", "image_registration_y"),
    ("Resource", "GameResource", "imageTransform", "I", "GameResourceState", "image_transform"),
    ("Resource", "GameResource", "imagesLRE", "Ljava/util/Vector;", "GameResourceStatics", "cached_images"),
    ("Resource", "GameResource", "imagesImportants", "Ljava/util/Vector;", "GameResourceStatics", "important_images"),
    ("ExtBase", "InkEngine", "menuScrollTickCounter", "B", "InkEngineState", "menu_scroll_tick_counter"),
    ("ExtBase", "InkEngine", "settingsHash", "Ljava/util/Hashtable;", "InkEngineState", "settings_hash"),
    ("ExtBase", "InkEngine", "actionKey_keyCodes", "[I", "InkEngineState", "action_key_key_codes"),
    ("ExtBase", "InkEngine", "actionKey_scriptIds", "[Ljava/lang/String;", "InkEngineState", "action_key_script_ids"),
    ("ExtBase", "InkEngine", "curSplash", "I", "InkEngineState", "current_splash"),
    ("ExtBase", "InkEngine", "numOfSplashes", "I", "InkEngineState", "number_of_splashes"),
    ("ExtBase", "InkEngine", "popupEndTime", "J", "InkEngineState", "popup_end_time"),
    ("ExtBase", "InkEngine", "popupMinTimeEnds", "J", "InkEngineState", "popup_minimum_time_ends"),
    ("ExtBase", "InkEngine", "popupCurrent", "I", "InkEngineState", "popup_current"),
    ("ExtBase", "InkEngine", "popupNumOf", "I", "InkEngineState", "popup_number"),
    ("ExtBase", "InkEngine", "popupActive", "Z", "InkEngineState", "popup_active"),
    ("ExtBase", "InkEngine", "popup_choice", "B", "InkEngineState", "popup_choice"),
    ("ExtBase", "InkEngine", "popupRecoveryCode", "[I", "InkEngineState", "popup_recovery_codes"),
    ("ExtBase", "InkEngine", "popupText", "[[Ljava/lang/String;", "InkEngineState", "popup_texts"),
    ("ExtBase", "InkEngine", "popupMaxTime", "[I", "InkEngineState", "popup_maximum_times"),
    ("MyCanvas", "GameCanvas", "transformTable", "[I", "GameCanvasState", "transform_table"),
    ("MyCanvas", "GameCanvas", "soundID", "Ljava/lang/String;", "GameCanvasState", "sound_id"),
    ("MyCanvas", "GameCanvas", "loopCount", "I", "GameCanvasState", "loop_count"),
    ("MyCanvas", "GameCanvas", "keySoftkeyLeft", "I", "GameCanvasState", "key_softkey_left"),
    ("MyCanvas", "GameCanvas", "keySoftkeyRight", "I", "GameCanvasState", "key_softkey_right"),
    ("MyCanvas", "GameCanvas", "keySend", "I", "GameCanvasState", "key_send"),
    ("MyCanvas", "GameCanvas", "keyReturn", "I", "GameCanvasState", "key_return"),
    ("MyCanvas", "GameCanvas", "keySoftkeyCenter", "I", "GameCanvasState", "key_softkey_center"),
    ("MyCanvas", "GameCanvas", "keyArrowUp", "I", "GameCanvasState", "key_arrow_up"),
    ("MyCanvas", "GameCanvas", "keyArrowDown", "I", "GameCanvasState", "key_arrow_down"),
    ("MyCanvas", "GameCanvas", "keyArrowLeft", "I", "GameCanvasState", "key_arrow_left"),
    ("MyCanvas", "GameCanvas", "keyArrowRight", "I", "GameCanvasState", "key_arrow_right"),
    ("MyCanvas", "GameCanvas", "keyErase", "I", "GameCanvasState", "key_erase"),
    ("Menu", "MenuModel", "isCurrent", "Z", "MenuState", "is_current"),
    ("Menu", "MenuModel", "selectedChoiceNr", "I", "MenuState", "selected_choice_number"),
    ("Menu", "MenuModel", "x", "I", "MenuState", "x"),
    ("Menu", "MenuModel", "y", "I", "MenuState", "y"),
    ("Menu", "MenuModel", "scroll", "I", "MenuState", "scroll"),
    ("Menu", "MenuModel", "textScrolling", "Z", "MenuState", "text_scrolling"),
    ("Menu", "MenuModel", "updateMenu", "Z", "MenuState", "update_menu"),
    ("Menu", "MenuModel", "topText", "Ljava/lang/String;", "MenuState", "top_text"),
    ("Menu", "MenuModel", "updateTopLines", "Z", "MenuState", "update_top_lines"),
    ("Menu", "MenuModel", "engineSoftkeyOptionLeft", "Ljava/lang/String;", "MenuState", "engine_softkey_option_left"),
    ("Menu", "MenuModel", "engineSoftkeyOptionRight", "Ljava/lang/String;", "MenuState", "engine_softkey_option_right"),
    ("Menu", "MenuModel", "choiceIDs", "Ljava/util/Vector;", "MenuState", "choice_ids"),
    ("Menu", "MenuModel", "choiceTexts", "Ljava/util/Vector;", "MenuState", "choice_texts"),
    ("Menu", "MenuModel", "updateBodyLines", "Z", "MenuState", "update_body_lines"),
    ("Menu", "MenuModel", "curInvItemResource", "LResource;", "MenuState", "current_inventory_item_resource"),
    ("Menu", "MenuModel", "stack", "Ljava/util/Vector;", "MenuStatics", "stack"),
    ("ScriptThread", "InkInterpreter", "pausedThread", "LScriptThread;", "InkInterpreterStatics", "paused_thread"),
    ("ScriptThread", "InkInterpreter", "script", "LScript;", "InkInterpreterState", "script"),
    ("ScriptThread", "InkInterpreter", "status", "I", "InkInterpreterState", "status"),
    ("ScriptThread", "InkInterpreter", "offset", "I", "InkInterpreterState", "offset"),
    ("ScriptThread", "InkInterpreter", "roomObject", "LRoomObject;", "InkInterpreterState", "room_object"),
    ("ScriptThread", "InkInterpreter", "languageDebugMode", "Z", "InkInterpreterState", "language_debug_mode"),
    ("Script", "InkScript", "data", "[B", "InkScriptState", "data"),
    ("Script", "InkScript", "eventOffsets", "[I", "InkScriptState", "event_offsets"),
    ("Script", "InkScript", "stringList", "[Ljava/lang/String;", "InkScriptState", "string_list"),
    ("Script", "InkScript", "gfxID", "Ljava/lang/Object;", "InkScriptState", "gfx_id"),
    ("Script", "InkScript", "list", "Ljava/util/Hashtable;", "InkScriptStatics", "scripts"),
    ("Script", "InkScript", "waitStop", "J", "InkScriptStatics", "wait_stop"),
    ("Script", "InkScript", "itemID", "Ljava/lang/String;", "InkScriptStatics", "item_id"),
    ("RoomObject", "RoomObject", "type", "I", "RoomObjectState", "object_type"),
    ("RoomObject", "RoomObject", "x", "I", "RoomObjectState", "x"),
    ("RoomObject", "RoomObject", "y", "I", "RoomObjectState", "y"),
    ("RoomObject", "RoomObject", "width", "I", "RoomObjectState", "width"),
    ("RoomObject", "RoomObject", "height", "I", "RoomObjectState", "height"),
    ("RoomObject", "RoomObject", "regPointX", "I", "RoomObjectState", "registration_x"),
    ("RoomObject", "RoomObject", "regPointY", "I", "RoomObjectState", "registration_y"),
    ("RoomObject", "RoomObject", "transform", "I", "RoomObjectState", "transform"),
    ("RoomObject", "RoomObject", "gfxID", "Ljava/lang/Object;", "RoomObjectState", "gfx_id"),
    ("RoomObject", "RoomObject", "textAlignment", "I", "RoomObjectState", "text_alignment"),
    ("RoomObject", "RoomObject", "animationData", "[[Ljava/lang/Object;", "RoomObjectState", "animation_data"),
    ("RoomObject", "RoomObject", "animationParts", "[I", "RoomObjectState", "animation_parts"),
    ("RoomObject", "RoomObject", "animationDuration", "[I", "RoomObjectState", "animation_duration"),
    ("RoomObject", "RoomObject", "animationImagePoints", "[[I", "RoomObjectState", "animation_image_points"),
    ("RoomObject", "RoomObject", "animationTime", "J", "RoomObjectState", "animation_time"),
    ("RoomObject", "RoomObject", "idleAnimationTime", "J", "RoomObjectState", "idle_animation_time"),
    ("RoomObject", "RoomObject", "runAnimLoops", "I", "RoomObjectState", "run_animation_loops"),
    ("RoomObject", "RoomObject", "battlePanelID", "I", "RoomObjectState", "battle_panel_id"),
    ("RoomObject", "RoomObject", "battlePanel", "[I", "RoomObjectState", "battle_panel"),
    ("RoomObject", "RoomObject", "color", "I", "RoomObjectState", "color"),
    ("RoomObject", "RoomObject", "text", "Ljava/lang/String;", "RoomObjectState", "text"),
    ("RoomObject", "RoomObject", "runAnimPausedTime", "I", "RoomObjectState", "run_animation_paused_time"),
    ("RoomObject", "RoomObject", "paintingAnimationTime", "J", "RoomObjectStatics", "painting_animation_time"),
    ("RoomObject", "RoomObject", "noVibraYet", "Z", "RoomObjectStatics", "no_vibration_yet"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_HERO_HEALTH", "I", "RoomObjectStatics", "battle_panel_hero_health_id"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_ENEMY_HEALTH", "I", "RoomObjectStatics", "battle_panel_enemy_health_id"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_TIMEBAR", "I", "RoomObjectStatics", "battle_panel_time_bar_id"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_HARD_ATTACK", "I", "RoomObjectStatics", "battle_panel_hard_attack_id"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_FAST_ATTACK", "I", "RoomObjectStatics", "battle_panel_fast_attack_id"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_INVENTORY", "I", "RoomObjectStatics", "battle_panel_inventory_id"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_ID_ESCAPE", "I", "RoomObjectStatics", "battle_panel_escape_id"),
    ("RoomObject", "RoomObject", "scriptID", "Ljava/lang/String;", "RoomObjectState", "script_id"),
    ("RoomObject", "RoomObject", "script", "LScript;", "RoomObjectState", "script"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_MAX_HEALTH", "I", "RoomObjectStatics", "battle_panel_max_health"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_HEALTH", "I", "RoomObjectStatics", "battle_panel_health"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_BAR_SIZE", "I", "RoomObjectStatics", "battle_panel_bar_size"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_TIME", "I", "RoomObjectStatics", "battle_panel_time"),
    ("RoomObject", "RoomObject", "BATTLE_PANEL_SIZE", "I", "RoomObjectStatics", "battle_panel_size"),
    ("RoomObject", "RoomObject", "visible", "Z", "RoomObjectState", "visible"),
    ("RoomObject", "RoomObject", "active", "Z", "RoomObjectState", "active"),
    ("RoomObject", "RoomObject", "left", "I", "RoomObjectState", "left"),
    ("RoomObject", "RoomObject", "right", "I", "RoomObjectState", "right"),
    ("RoomObject", "RoomObject", "top", "I", "RoomObjectState", "top"),
    ("RoomObject", "RoomObject", "bottom", "I", "RoomObjectState", "bottom"),
]

CONST_FIELD_REQUESTS = [
    ("MyCanvas", "GameCanvas", "KEY_UP", "KEY_UP", "I", "GAME_CANVAS_KEY_UP"),
    (
        "M",
        "Application",
        "COLLUM_WIDTH",
        "CODED_STRING_COLUMN_WIDTH",
        "I",
        "CODED_STRING_COLUMN_WIDTH",
    ),
    ("Resource", "GameResource", "TYPE_GFX", "TYPE_GFX", "I", "GAME_RESOURCE_TYPE_GRAPHICS"),
    ("Resource", "GameResource", "TYPE_SFX", "TYPE_SFX", "I", "GAME_RESOURCE_TYPE_SOUND"),
    (
        "M",
        "Application",
        "MAX_LINE",
        "MAX_LINE",
        "I",
        "CODED_STRING_MAX_LINES",
    ),
    ("Script", "InkScript", "GFX_TYPE_NONE", "GFX_TYPE_NONE", "I", "INK_SCRIPT_GFX_TYPE_NONE"),
    ("Script", "InkScript", "GFX_TYPE_STRING", "GFX_TYPE_STRING", "I", "INK_SCRIPT_GFX_TYPE_STRING"),
    ("Script", "InkScript", "GFX_TYPE_INTEGER", "GFX_TYPE_INTEGER", "I", "INK_SCRIPT_GFX_TYPE_INTEGER"),
    ("RoomObject", "RoomObject", "TYPE_GFX", "TYPE_GFX", "I", "ROOM_OBJECT_TYPE_GRAPHICS"),
    ("RoomObject", "RoomObject", "TYPE_ZONE", "TYPE_ZONE", "I", "ROOM_OBJECT_TYPE_ZONE"),
    ("RoomObject", "RoomObject", "TYPE_ZONE_BATTLE", "TYPE_ZONE_BATTLE", "I", "ROOM_OBJECT_TYPE_BATTLE_ZONE"),
    ("RoomObject", "RoomObject", "TYPE_ZONE_COLOR", "TYPE_ZONE_COLOR", "I", "ROOM_OBJECT_TYPE_COLOR_ZONE"),
    ("RoomObject", "RoomObject", "TYPE_ZONE_TEXT", "TYPE_ZONE_TEXT", "I", "ROOM_OBJECT_TYPE_TEXT_ZONE"),
    ("RoomObject", "RoomObject", "TYPE_ZONE_TILES", "TYPE_ZONE_TILES", "I", "ROOM_OBJECT_TYPE_TILE_ZONE"),
    ("RoomObject", "RoomObject", "GFX_ID_TYPE_STRING", "GFX_ID_TYPE_STRING", "I", "ROOM_OBJECT_GRAPHICS_ID_TYPE_STRING"),
    ("RoomObject", "RoomObject", "GFX_ID_TYPE_INTEGER", "GFX_ID_TYPE_INTEGER", "I", "ROOM_OBJECT_GRAPHICS_ID_TYPE_INTEGER"),
    ("RoomObject", "RoomObject", "ANIMATION_IDLE", "ANIMATION_IDLE", "I", "ROOM_OBJECT_ANIMATION_IDLE"),
    ("RoomObject", "RoomObject", "ANIMATION_ENEMYATTACK1", "ANIMATION_ENEMYATTACK1", "I", "ROOM_OBJECT_ANIMATION_ENEMY_ATTACK_1"),
    ("RoomObject", "RoomObject", "ANIMATION_ENEMYATTACK2", "ANIMATION_ENEMYATTACK2", "I", "ROOM_OBJECT_ANIMATION_ENEMY_ATTACK_2"),
    ("RoomObject", "RoomObject", "ANIMATION_ENEMYATTACK3", "ANIMATION_ENEMYATTACK3", "I", "ROOM_OBJECT_ANIMATION_ENEMY_ATTACK_3"),
    ("RoomObject", "RoomObject", "ANIMATION_HEROATTACK1", "ANIMATION_HEROATTACK1", "I", "ROOM_OBJECT_ANIMATION_HERO_ATTACK_1"),
    ("RoomObject", "RoomObject", "ANIMATION_HEROATTACK2", "ANIMATION_HEROATTACK2", "I", "ROOM_OBJECT_ANIMATION_HERO_ATTACK_2"),
    ("RoomObject", "RoomObject", "ANIMATION_SINGLE", "ANIMATION_SINGLE", "I", "ROOM_OBJECT_ANIMATION_SINGLE"),
    ("RoomObject", "RoomObject", "ANIMATION_COUNT", "ANIMATION_COUNT", "I", "ROOM_OBJECT_ANIMATION_COUNT"),
    ("RoomObject", "RoomObject", "ANIMATION_VALUES_PER_PART", "ANIMATION_VALUES_PER_PART", "I", "ROOM_OBJECT_ANIMATION_VALUES_PER_PART"),
    ("RoomObject", "RoomObject", "ANIMATION_OFFSET_GFX", "ANIMATION_OFFSET_GFX", "I", "ROOM_OBJECT_ANIMATION_OFFSET_GRAPHICS"),
    ("RoomObject", "RoomObject", "ANIMATION_OFFSET_DURATION", "ANIMATION_OFFSET_DURATION", "I", "ROOM_OBJECT_ANIMATION_OFFSET_DURATION"),
    ("RoomObject", "RoomObject", "ANIMATION_OFFSET_OFFSET_X", "ANIMATION_OFFSET_OFFSET_X", "I", "ROOM_OBJECT_ANIMATION_OFFSET_X"),
    ("RoomObject", "RoomObject", "ANIMATION_OFFSET_OFFSET_Y", "ANIMATION_OFFSET_OFFSET_Y", "I", "ROOM_OBJECT_ANIMATION_OFFSET_Y"),
    ("RoomObject", "RoomObject", "DEFAULT_COLOR", "DEFAULT_COLOR", "I", "ROOM_OBJECT_DEFAULT_COLOR"),
    ("RoomObject", "RoomObject", "ZONE_TEXT_DEFAULT_WIDTH", "ZONE_TEXT_DEFAULT_WIDTH", "I", "ROOM_OBJECT_TEXT_ZONE_DEFAULT_WIDTH"),
    ("s", "InkCodes", "EVENT_GETNAME", "EVENT_GETNAME", "I", "INK_EVENT_GET_NAME"),
    ("s", "InkCodes", "EVENT_GETMOVEDIR", "EVENT_GETMOVEDIR", "I", "INK_EVENT_GET_MOVE_DIRECTION"),
    ("s", "InkCodes", "EVENT_HOOVERIN", "EVENT_HOVER_IN", "I", "INK_EVENT_HOVER_IN"),
]


def print_declaration_entries() -> None:
    rust_file = "transliteration/crates/orphan-game-xlat/src/state.rs"
    fields = original_fields()
    java = validate_ast_authority.java_asts()
    rust = rust_asts({rust_file})
    for java_class, canonical_class, java_name, descriptor, owner, rust_name in FIELD_REQUESTS:
        entry = {
            "java_class": java_class,
            "canonical_class": canonical_class,
            "java_name": java_name,
            "descriptor": descriptor,
            "java_item": f"<field:{java_name}>",
        }
        entry.update(java_field_evidence_facts(entry, fields, java))
        target = {
            "file": rust_file,
            "item": f"field:{owner}::{rust_name}",
        }
        target.update(rust_evidence_facts(target, rust))
        entry["rust"] = [target]
        entry["rust_node_counts"] = [target["node_count"]]
        print(json.dumps(entry, sort_keys=True))
    value_file = "transliteration/crates/orphan-game-xlat/src/lib.rs"
    value_rust = rust_asts({value_file})
    for (
        java_class,
        canonical_class,
        java_name,
        canonical_name,
        descriptor,
        rust_name,
    ) in CONST_FIELD_REQUESTS:
        entry = {
            "java_class": java_class,
            "canonical_class": canonical_class,
            "java_name": java_name,
            "canonical_name": canonical_name,
            "descriptor": descriptor,
            "java_item": f"<field:{canonical_name}>",
        }
        entry.update(java_field_evidence_facts(entry, fields, java))
        target = {"file": value_file, "item": f"const:{rust_name}"}
        target.update(rust_evidence_facts(target, value_rust))
        entry["rust"] = [target]
        entry["rust_node_counts"] = [target["node_count"]]
        print(json.dumps(entry, sort_keys=True))
    for owner in (
        "CheatControllerStatics",
        "SilentHillGameStatics",
        "ApplicationState",
        "ResourceRequestState",
        "GameResourceState",
        "GameResourceStatics",
        "InkEngineState",
        "GameCanvasState",
        "MenuState",
        "MenuStatics",
        "InkInterpreterState",
        "InkInterpreterStatics",
        "InkScriptState",
        "InkScriptStatics",
        "RoomObjectState",
        "RoomObjectStatics",
    ):
        target = {"file": rust_file, "item": f"struct:{owner}"}
        target.update(rust_evidence_facts(target, rust))
        print(json.dumps(target, sort_keys=True))
    for item in (
        "variant:JavaObject::Null",
        "variant:JavaObject::Integer",
        "variant:JavaObject::String",
        "variant:JavaObject::Other",
        "enum:JavaObject",
        "variant:JavaOwnedObject::Integer",
        "variant:JavaOwnedObject::String",
        "variant:JavaOwnedObject::Other",
        "enum:JavaOwnedObject",
        "variant:JavaResourceId::Integer",
        "variant:JavaResourceId::String",
        "variant:JavaResourceId::Opaque",
        "enum:JavaResourceId",
        "variant:InkVariableError::NullPointer",
        "variant:InkVariableError::StringIndexOutOfBounds",
        "variant:InkVariableError::NumberFormat",
        "enum:InkVariableError",
        "variant:InkScriptRegistryValue::Script",
        "variant:InkScriptRegistryValue::Other",
        "enum:InkScriptRegistryValue",
        "variant:InkScriptExecuteEventError::NullPointer",
        "variant:InkScriptExecuteEventError::ClassCast",
        "variant:InkScriptExecuteEventError::Execute",
        "enum:InkScriptExecuteEventError",
        "variant:InkScriptGetItemNameError::ExecuteEvent",
        "variant:InkScriptGetItemNameError::ClassCast",
        "enum:InkScriptGetItemNameError",
        "variant:InkEnginePopupCreateError::WrapString",
        "variant:InkEnginePopupCreateError::ArrayAccess",
        "enum:InkEnginePopupCreateError",
        "variant:ApplicationClearAllRmsError::ResourceClear",
        "variant:ApplicationClearAllRmsError::ScriptListNull",
        "enum:ApplicationClearAllRmsError",
        "variant:ApplicationSetDisplayError::GetDisplay",
        "variant:ApplicationSetDisplayError::DisplayNull",
        "variant:ApplicationSetDisplayError::SetCurrent",
        "enum:ApplicationSetDisplayError",
        "variant:ApplicationRmsDeleteError::NotFound",
        "variant:ApplicationRmsDeleteError::RecordStore",
        "variant:ApplicationRmsDeleteError::Uncaught",
        "enum:ApplicationRmsDeleteError",
        "variant:ApplicationResourceMakeSubChunkError::NegativeArraySize",
        "variant:ApplicationResourceMakeSubChunkError::ArrayCopy",
        "enum:ApplicationResourceMakeSubChunkError",
        "variant:ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeRepaint",
        "variant:ApplicationRepaintCanvasIfPossibleError::Repaint",
        "variant:ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeServiceRepaints",
        "variant:ApplicationRepaintCanvasIfPossibleError::ServiceRepaints",
        "enum:ApplicationRepaintCanvasIfPossibleError",
        "variant:GameCanvasNewError::SuperConstructor",
        "variant:GameCanvasNewError::SetFullScreen",
        "enum:GameCanvasNewError",
        "variant:RoomObjectStringEventError::ExecuteEvent",
        "variant:RoomObjectStringEventError::ClassCast",
        "enum:RoomObjectStringEventError",
        "variant:RoomObjectEnterHoverError::HasEvent",
        "variant:RoomObjectEnterHoverError::ExecuteEvent",
        "enum:RoomObjectEnterHoverError",
    ):
        target = {"file": value_file, "item": item}
        target.update(rust_evidence_facts(target, value_rust))
        print(json.dumps(target, sort_keys=True))


def print_pending() -> None:
    document = tomllib.loads(MANIFEST.read_text(encoding="utf-8"))
    reviewed = {
        (entry["java_class"], entry["java_name"], entry["descriptor"])
        for entry in document.get("body", [])
    }
    class_names = canonical_classes()
    candidates = [
        (method.code_size, java_class, method.name, method.descriptor, method.max_stack, method.max_locals)
        for (java_class, _name, _descriptor), method in original_methods().items()
        if (java_class, method.name, method.descriptor) not in reviewed
    ]
    print("bytes\toriginal_class\tcanonical_class\tmethod\tdescriptor\tstack\tlocals")
    for code_size, java_class, name, descriptor, max_stack, max_locals in sorted(candidates):
        print(
            f"{code_size}\t{java_class}\t{class_names[java_class]}\t{name}\t"
            f"{descriptor}\t{max_stack}\t{max_locals}"
        )


def print_entries() -> None:
    requested = [
        ("M", "Application", "getLeft", "(IIIIII)I", "getLeft(int,int,int,int,int,int)", "fn:get_left"),
        ("M", "Application", "getTop", "(IIIIII)I", "getTop(int,int,int,int,int,int)", "fn:get_top"),
        ("M", "Application", "min", "(II)I", "min(int,int)", "fn:min"),
        ("M", "Application", "max", "(II)I", "max(int,int)", "fn:max"),
        ("M", "Application", "abs", "(I)I", "abs(int)", "fn:abs"),
        ("M", "Application", "dir", "(I)I", "dir(int)", "fn:dir"),
        ("M", "Application", "resourceExit", "()V", "resourceExit()", "fn:resource_exit"),
        ("M", "Application", "destroyApp", "(Z)V", "destroyApp(boolean)", "fn:application_destroy_app"),
        ("M", "Application", "pauseApp", "()V", "pauseApp()", "fn:application_pause_app"),
        ("M", "Application", "appStart", "()V", "appStart()", "fn:application_app_start"),
        ("ExtBase", "InkEngine", "popupCreate", "(Ljava/lang/String;I)V", "popupCreate(String,int)", "fn:ink_engine_popup_create"),
        ("ExtBase", "InkEngine", "popupCreate", "(Ljava/lang/String;II)V", "popupCreate(String,int,int)", "fn:ink_engine_popup_create_with_max_time"),
        ("ExtBase", "InkEngine", "popupSetNext", "()V", "popupSetNext()", "fn:ink_engine_popup_set_next"),
        ("ExtBase", "InkEngine", "inventoryEquipUnequipHandling", "(I)V", "inventoryEquipUnequipHandling(int)", "fn:ink_engine_inventory_equip_unequip_handling"),
        ("Cheat", "CheatController", "<clinit>", "()V", "<clinit>()", "fn:cheat_controller_initialize"),
        ("Cheat", "CheatController", "<init>", "()V", "<init>()", "fn:cheat_controller_new"),
        ("Ext", "SilentHillGame", "<init>", "()V", "<init>()", "fn:silent_hill_game_new"),
        ("Ext", "SilentHillGame", "menuResetIngameValues", "()V", "menuResetIngameValues()", "fn:silent_hill_game_menu_reset_ingame_values"),
        ("Ext", "SilentHillGame", "appInit", "()V", "appInit()", "fn:silent_hill_game_app_init"),
        ("ExtBase", "InkEngine", "<init>", "()V", "<init>()", "fn:ink_engine_new"),
        ("M", "Application", "<init>", "()V", "<init>()", "fn:application_new"),
        ("s", "InkCodes", "<init>", "()V", "<init>()", "fn:ink_codes_new"),
        ("txt_consts", "TextId", "<init>", "()V", "<init>()", "fn:text_id_new"),
        ("M", "Application", "resourceURLEncode", "(Ljava/lang/String;)Ljava/lang/String;", "resourceURLEncode(String)", "fn:resource_url_encode"),
        ("M", "Application", "codedString", "([B)Ljava/lang/String;", "codedString(byte[])", "fn:coded_string"),
        ("M", "Application", "printArray", "([[B)V", "printArray(byte[][])", "fn:application_print_array"),
        ("M", "Application", "roomRepaintRun", "()V", "roomRepaintRun()", "fn:application_room_repaint_run"),
        ("M", "Application", "clearAllRMS", "()V", "clearAllRMS()", "fn:application_clear_all_rms"),
        ("M", "Application", "freeMemory", "()J", "freeMemory()", "fn:application_free_memory"),
        ("M", "Application", "setDisplay", "(Ljavax/microedition/lcdui/Displayable;)V", "setDisplay(Displayable)", "fn:application_set_display"),
        ("M", "Application", "paint", "(Ljavax/microedition/lcdui/Graphics;)V", "paint(Graphics)", "fn:application_paint"),
        ("M", "Application", "rmsDelete", "(Ljava/lang/String;)Z", "rmsDelete(String)", "fn:application_rms_delete"),
        ("M", "Application", "saveChunkINI", "(Ljava/io/DataInputStream;)V", "saveChunkINI(DataInputStream)", "fn:application_save_chunk_ini"),
        ("M", "Application", "resourceMakeSubChunk", "()[B", "resourceMakeSubChunk()", "fn:application_resource_make_subchunk"),
        ("M", "Application", "repaintCanvasIfPossible", "()V", "repaintCanvasIfPossible()", "fn:application_repaint_canvas_if_possible"),
        ("MyCanvas", "GameCanvas", "resumeSound", "()V", "resumeSound()", "fn:game_canvas_resume_sound"),
        ("MyCanvas", "GameCanvas", "<init>", "()V", "<init>()", "fn:game_canvas_new"),
        ("MyCanvas", "GameCanvas", "keyJadEntryAsInt", "(Ljava/lang/String;)I", "keyJadEntryAsInt(String)", "fn:game_canvas_key_jad_entry_as_int"),
        ("M", "Application", "resourceRestartImportants", "()V", "resourceRestartImportants()", "fn:resource_restart_importants"),
        ("M", "Application", "resetLoad", "()V", "resetLoad()", "fn:reset_load"),
        ("M", "Application", "readString", "(Ljava/io/DataInputStream;)Ljava/lang/String;", "readString(DataInputStream)", "fn:read_string"),
        ("M", "Application", "find", "(Ljava/io/DataInputStream;Ljava/lang/String;)I", "find(DataInputStream,String)", "fn:find"),
        ("M", "Application", "writeString", "(Ljava/io/DataOutputStream;Ljava/lang/String;)V", "writeString(DataOutputStream,String)", "fn:write_string"),
        ("M", "Application", "readStringList", "(Ljava/io/DataInputStream;)[Ljava/lang/String;", "readStringList(DataInputStream)", ("fn:read_string_list", "fn:read_modified_utf")),
        ("M", "Application", "loadRequest_getResourcePath", "(ILjava/lang/String;)Ljava/lang/String;", "loadRequest_getResourcePath(int,String)", "fn:load_request_resource_path_for_string"),
        ("M", "Application", "getGameLangPath", "()Ljava/lang/String;", "getGameLangPath()", "fn:game_language_path"),
        ("M", "Application", "getGameText", "(I)Ljava/lang/String;", "getGameText(int)", "fn:get_game_text"),
        ("M", "Application", "getGameText", "(Ljava/lang/String;)Ljava/lang/String;", "getGameText(String)", "fn:get_game_text_from_string"),
        ("M", "Application", "txtStringReplace", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", "txtStringReplace(String,String,String)", "fn:text_replace_first"),
        ("M", "Application", "removeStringPrefix", "([Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;", "removeStringPrefix(String[],String)", "fn:remove_string_prefix"),
        ("M", "Application", "getPosInLanguageSelectionList", "(Ljava/lang/String;)I", "getPosInLanguageSelectionList(String)", "fn:get_language_selection_position"),
        ("M", "Application", "resourceIsOnHeap", "(I)I", "resourceIsOnHeap(int)", "fn:resource_heap_index"),
        ("M", "Application", "random", "(I)I", "random(int)", "fn:random_scaled"),
        ("M", "Application", "inkServerGetVariabel", "(Ljava/lang/String;)Ljava/lang/String;", "inkServerGetVariable(String)", "fn:ink_server_get_variable"),
        ("M", "Application", "inkServerGetHint", "(Ljava/lang/String;)Ljava/lang/String;", "inkServerGetHint(String)", "fn:ink_server_get_hint"),
        ("M", "Application", "inkServerSetVariabel", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "inkServerSetVariable(String,String,String)", "fn:ink_server_set_variable"),
        ("M", "Application", "inkServerUnsetVariabel", "(Ljava/lang/String;)V", "inkServerUnsetVariable(String)", "fn:ink_server_unset_variable"),
        ("M", "Application", "resetVariableSystem", "()V", "resetVariableSystem()", "fn:reset_variable_system"),
        ("M", "Application", "roomGetHistorySize", "()I", "roomGetHistorySize()", "fn:room_history_size"),
        ("Script", "InkScript", "getInventorySize", "(Ljava/lang/String;)I", "getInventorySize(String)", "fn:inventory_size"),
        ("Script", "InkScript", "setInventory", "(Ljava/lang/String;I)V", "setInventory(String,int)", "fn:inventory_set"),
        ("Script", "InkScript", "removeInventory", "(Ljava/lang/String;)V", "removeInventory(String)", "fn:inventory_remove"),
        ("Script", "InkScript", "setVariable", "(Ljava/lang/String;Ljava/lang/Object;)V", "setVariable(String,Object)", "fn:ink_script_set_variable"),
        ("Script", "InkScript", "getVariable", "(Ljava/lang/String;)Ljava/lang/Object;", "getVariable(String)", ("fn:ink_script_get_variable", "fn:parse_java_i32")),
        ("Script", "InkScript", "getVariableAsInteger", "(Ljava/lang/String;)I", "getVariableAsInteger(String)", "fn:ink_script_get_variable_as_integer"),
        ("M", "Application", "roomGetCurrent", "()Ljava/lang/String;", "roomGetCurrent()", "fn:room_current"),
        ("M", "Application", "roomSetCurrent", "(Ljava/lang/String;)V", "roomSetCurrent(String)", "fn:room_set_current"),
        ("M", "Application", "roomGetLastInRoomHistory", "()Ljava/lang/String;", "roomGetLastInRoomHistory()", "fn:room_last_in_history"),
        ("M", "Application", "roomAddToRoomHistory", "(Ljava/lang/String;)V", "roomAddToRoomHistory(String)", "fn:room_add_to_history"),
        ("M", "Application", "roomRemoveLastInRoomHistory", "()V", "roomRemoveLastInRoomHistory()", "fn:room_remove_last_from_history"),
        ("M", "Application", "loadRequest_getResourcePath", "(IILjava/lang/String;I)Ljava/lang/String;", "loadRequest_getResourcePath(int,int,String,int)", "fn:load_request_resource_path"),
        ("M", "Application", "loadRequest_getResourcePath", "(Ljava/lang/Object;I)Ljava/lang/String;", "loadRequest_getResourcePath(Object,int)", "fn:load_request_resource_path_for_object"),
        ("LoadRequest", "ResourceRequest", "<init>", "(ILjava/lang/String;)V", "<init>(int,String)", "fn:resource_request_new_for_string"),
        ("LoadRequest", "ResourceRequest", "<init>", "(Ljava/lang/Object;I)V", "<init>(Object,int)", "fn:resource_request_new_for_object"),
        ("LoadRequest", "ResourceRequest", "createFromInputStream", "(Ljava/io/DataInputStream;)LLoadRequest;", "createFromInputStream(DataInputStream)", "fn:resource_request_create_from_input"),
        ("LoadRequest", "ResourceRequest", "getID", "()Ljava/lang/Object;", "getID()", "fn:resource_request_get_id"),
        ("LoadRequest", "ResourceRequest", "getDescription", "()Ljava/lang/String;", "getDescription()", "fn:resource_request_description"),
        ("LoadRequest", "ResourceRequest", "equals", "(Ljava/lang/Object;)Z", "equals(Object)", "fn:resource_request_equals"),
        ("LoadRequest", "ResourceRequest", "getResourcePath", "()Ljava/lang/String;", "getResourcePath()", "fn:resource_request_resource_path"),
        ("LoadRequest", "ResourceRequest", "toString", "()Ljava/lang/String;", "toString()", "fn:resource_request_to_string"),
        ("M", "Application", "charToString", "(C)Ljava/lang/String;", "charToString(char)", "fn:char_to_string"),
        ("M", "Application", "resourceMergeSortCmp", "([B[B)Z", "resourceMergeSortCmp(byte[],byte[])", "fn:resource_merge_sort_cmp"),
        ("M", "Application", "arrayCopyString", "([Ljava/lang/String;I[Ljava/lang/String;II)V", "arrayCopyString(String[],int,String[],int,int)", "fn:array_copy_string_handles"),
        ("M", "Application", "toInt", "(Ljava/lang/Object;)I", "toInt(Object)", "fn:to_int"),
        ("M", "Application", "toBoolean", "(Ljava/lang/Object;)Z", "toBoolean(Object)", "fn:to_boolean"),
        ("ScriptThread", "InkInterpreter", "integerArgument", "(Ljava/lang/Object;)I", "integerArgument(Object)", "fn:ink_interpreter_integer_argument"),
        ("ExtBase", "InkEngine", "actionKeyIdConvert", "(Ljava/lang/String;)I", "actionKeyIdConvert(String)", "fn:action_key_id_convert"),
        ("M", "Application", "tickBasedTime", "()I", "tickBasedTime()", "fn:tick_based_time"),
        ("M", "Application", "tickBasedTimeUpdate", "()V", "tickBasedTimeUpdate()", "fn:tick_based_time_update"),
        ("M", "Application", "tickBasedTimeReset", "()V", "tickBasedTimeReset()", "fn:tick_based_time_reset"),
        ("M", "Application", "loading", "()Z", "loading()", "fn:loading"),
        ("ExtBase", "InkEngine", "isMenuScrollAllowed", "()Z", "isMenuScrollAllowed()", "fn:is_menu_scroll_allowed"),
        ("ExtBase", "InkEngine", "actionKeyKeycodeToActionkey", "(I)I", "actionKeyKeycodeToActionkey(int)", "fn:action_key_keycode_to_action_key"),
        ("ExtBase", "InkEngine", "actionKeyUnsetAllKeys", "()V", "actionKeyUnsetAllKeys()", "fn:action_key_unset_all_keys"),
        ("ExtBase", "InkEngine", "actionKeyInitSystem", "()V", "actionKeyInitSystem()", "fn:action_key_init_system"),
        ("ExtBase", "InkEngine", "actionKeyGetScriptId", "(I)Ljava/lang/String;", "actionKeyGetScriptId(int)", "fn:action_key_get_script_id"),
        ("ExtBase", "InkEngine", "splashMoreExists", "()Z", "splashMoreExists()", "fn:splash_more_exists"),
        ("ExtBase", "InkEngine", "wrapString", "(Ljava/lang/String;I)[Ljava/lang/String;", "wrapString(String,int)", "fn:ink_engine_wrap_string"),
        ("Resource", "GameResource", "<init>", "(ILjava/lang/Object;I)V", "<init>(int,Object,int)", "fn:game_resource_new"),
        ("Resource", "GameResource", "<clinit>", "()V", "<clinit>()", "fn:game_resource_initialize"),
        ("Resource", "GameResource", "equals", "(Ljava/lang/Object;)Z", "equals(Object)", "fn:game_resource_equals"),
        ("Resource", "GameResource", "paint", "(Ljavax/microedition/lcdui/Graphics;III)V", "paint(Graphics,int,int,int)", "fn:game_resource_paint"),
        ("Resource", "GameResource", "paintSimple", "(Ljavax/microedition/lcdui/Graphics;III)V", "paintSimple(Graphics,int,int,int)", "fn:game_resource_paint_simple"),
        ("MyCanvas", "GameCanvas", "paint", "(Ljavax/microedition/lcdui/Graphics;)V", "paint(Graphics)", "fn:game_canvas_paint"),
        ("MyCanvas", "GameCanvas", "showNotify", "()V", "showNotify()", "fn:game_canvas_show_notify"),
        ("MyCanvas", "GameCanvas", "keyInit", "()V", "keyInit()", "fn:key_init"),
        ("MyCanvas", "GameCanvas", "keyConvertToKeyId", "(I)I", "keyConvertToKeyId(int)", "fn:key_convert_to_key_id"),
        ("M", "Application", "setKeyStatus", "(IZ)V", "setKeyStatus(int,boolean)", "fn:set_key_status"),
        ("MyCanvas", "GameCanvas", "keyPressed", "(I)V", "keyPressed(int)", "fn:game_canvas_key_pressed"),
        ("MyCanvas", "GameCanvas", "keyReleased", "(I)V", "keyReleased(int)", "fn:game_canvas_key_released"),
        ("Menu", "MenuModel", "getChoiceNr", "()I", "getChoiceNr()", "fn:menu_get_choice_number"),
        ("Menu", "MenuModel", "addChoice", "(Ljava/lang/Object;Ljava/lang/String;)V", "addChoice(Object,String)", "fn:menu_add_choice"),
        ("Menu", "MenuModel", "addChoice", "(ILjava/lang/String;)V", "addChoice(int,String)", "fn:menu_add_choice_integer"),
        ("Menu", "MenuModel", "countChoices", "()I", "countChoices()", "fn:menu_count_choices"),
        ("Menu", "MenuModel", "getChoiceID", "()Ljava/lang/Object;", "getChoiceID()", "fn:menu_get_choice_id"),
        ("Menu", "MenuModel", "nextChoice", "()V", "nextChoice()", "fn:menu_next_choice"),
        ("Menu", "MenuModel", "previousChoice", "()V", "previousChoice()", "fn:menu_previous_choice"),
        ("Menu", "MenuModel", "setPosition", "(II)V", "setPosition(int,int)", "fn:menu_set_position"),
        ("Menu", "MenuModel", "setCurrent", "(Z)V", "setCurrent(boolean)", "fn:menu_set_current"),
        ("Menu", "MenuModel", "scrollIncrease", "()V", "scrollIncrease()", "fn:menu_scroll_increase"),
        ("Menu", "MenuModel", "scrollDecrease", "()V", "scrollDecrease()", "fn:menu_scroll_decrease"),
        ("Menu", "MenuModel", "setTop", "(Ljava/lang/String;)V", "setTop(String)", "fn:menu_set_top"),
        ("Menu", "MenuModel", "setSoftkeyOptions", "(Ljava/lang/String;Ljava/lang/String;)V", "setSoftkeyOptions(String,String)", "fn:menu_set_softkey_options"),
        ("Menu", "MenuModel", "setInvItemResource", "(LResource;)V", "setInvItemResource(GameResource)", "fn:menu_set_inventory_item_resource"),
        ("Menu", "MenuModel", "active", "()Z", "active()", "fn:menu_active"),
        ("Menu", "MenuModel", "closeAll", "()V", "closeAll()", "fn:menu_close_all"),
        ("Menu", "MenuModel", "closeCurrent", "()V", "closeCurrent()", "fn:menu_close_current"),
        ("Menu", "MenuModel", "getCurrent", "()LMenu;", "getCurrent()", "fn:menu_get_current"),
        ("Menu", "MenuModel", "<clinit>", "()V", "<clinit>()", "fn:menu_initialize"),
        ("ScriptThread", "InkInterpreter", "<init>", "(LScript;ILRoomObject;)V", "<init>(InkScript,int,RoomObject)", "fn:ink_interpreter_new"),
        ("ScriptThread", "InkInterpreter", "execute", "(Ljava/lang/Object;)Ljava/lang/Object;", "execute(Object)", "fn:ink_interpreter_execute"),
        ("ScriptThread", "InkInterpreter", "resume", "()Ljava/lang/Object;", "resume()", "fn:ink_interpreter_resume"),
        ("ScriptThread", "InkInterpreter", "read", "()I", "read()", "fn:ink_interpreter_read"),
        ("ScriptThread", "InkInterpreter", "read", "(I)I", "read(int)", "fn:ink_interpreter_read_bytes"),
        ("ScriptThread", "InkInterpreter", "readSigned", "(I)I", "readSigned(int)", "fn:ink_interpreter_read_signed"),
        ("ScriptThread", "InkInterpreter", "hasCommand", "(I)Z", "hasCommand(int)", "fn:ink_interpreter_has_command"),
        ("Script", "InkScript", "<init>", "(Ljava/io/DataInputStream;[Ljava/lang/String;)V", "<init>(DataInputStream,String[])", "fn:ink_script_new"),
        ("Script", "InkScript", "getString", "(I)Ljava/lang/String;", "getString(int)", "fn:ink_script_get_string"),
        ("Script", "InkScript", "executeEvent", "(ILjava/lang/Object;LRoomObject;)Ljava/lang/Object;", "executeEvent(int,Object,RoomObject)", "fn:ink_script_execute_event"),
        ("Script", "InkScript", "executeEvent", "(ILjava/lang/Object;LRoomObject;Z)Ljava/lang/Object;", "executeEvent(int,Object,RoomObject,boolean)", "fn:ink_script_execute_event_debug"),
        ("Script", "InkScript", "executeEvent", "(Ljava/lang/String;ILjava/lang/Object;LRoomObject;)Ljava/lang/Object;", "executeEvent(String,int,Object,RoomObject)", "fn:ink_script_execute_event_by_id"),
        ("Script", "InkScript", "getItemName", "(Ljava/lang/String;)Ljava/lang/String;", "getItemName(String)", "fn:ink_script_get_item_name"),
        ("Script", "InkScript", "hasCommand", "(I)Z", "hasCommand(int)", "fn:ink_script_has_command"),
        ("Script", "InkScript", "hasEvent", "(I)Z", "hasEvent(int)", "fn:ink_script_has_event"),
        ("Script", "InkScript", "resume", "()V", "resume()", "fn:ink_script_resume"),
        ("Script", "InkScript", "isWaiting", "()Z", "isWaiting()", "fn:ink_script_is_waiting"),
        ("Script", "InkScript", "stop", "()V", "stop()", "fn:ink_script_stop"),
        ("Script", "InkScript", "<clinit>", "()V", "<clinit>()", "fn:ink_script_initialize"),
        ("RoomObject", "RoomObject", "<init>", "(Ljava/io/DataInputStream;[Ljava/lang/String;)V", "<init>(DataInputStream,String[])", "fn:room_object_new"),
        ("RoomObject", "RoomObject", "<clinit>", "()V", "<clinit>()", "fn:room_object_initialize"),
        ("RoomObject", "RoomObject", "battlePanelNew", "(I)V", "battlePanelNew(int)", "fn:room_object_battle_panel_new"),
        ("RoomObject", "RoomObject", "bpSetMaxHealth", "(I)V", "bpSetMaxHealth(int)", "fn:room_object_bp_set_max_health"),
        ("RoomObject", "RoomObject", "bpSetHealth", "(I)V", "bpSetHealth(int)", "fn:room_object_bp_set_health"),
        ("RoomObject", "RoomObject", "bpSetBarSize", "(I)V", "bpSetBarSize(int)", "fn:room_object_bp_set_bar_size"),
        ("RoomObject", "RoomObject", "bpSetTime", "(I)V", "bpSetTime(int)", "fn:room_object_bp_set_time"),
        ("RoomObject", "RoomObject", "executeEvent", "(ILjava/lang/Object;Z)Ljava/lang/Object;", "executeEvent(int,Object,boolean)", "fn:room_object_execute_event"),
        ("RoomObject", "RoomObject", "getName", "()Ljava/lang/String;", "getName()", "fn:room_object_get_name"),
        ("RoomObject", "RoomObject", "getMoveDir", "()Ljava/lang/String;", "getMoveDir()", "fn:room_object_get_move_direction"),
        ("RoomObject", "RoomObject", "hooverIn", "()LRoomObject;", "enterHover()", "fn:room_object_enter_hover"),
        ("RoomObject", "RoomObject", "isOver", "(II)Z", "isOver(int,int)", "fn:room_object_is_over"),
    ]
    rust_file = "transliteration/crates/orphan-game-xlat/src/lib.rs"
    methods = original_methods()
    java = validate_ast_authority.java_asts()
    rust = rust_asts({rust_file})
    for java_class, canonical_class, java_name, descriptor, java_item, rust_item in requested:
        entry = {
            "java_class": java_class,
            "canonical_class": canonical_class,
            "java_name": java_name,
            "descriptor": descriptor,
            "java_item": java_item,
        }
        entry.update(java_evidence_facts(entry, methods, java))
        rust_items = (rust_item,) if isinstance(rust_item, str) else rust_item
        targets = []
        for item in rust_items:
            target = {"file": rust_file, "item": item}
            target.update(rust_evidence_facts(target, rust))
            targets.append(target)
        entry["rust"] = targets
        entry["rust_node_counts"] = [target["node_count"] for target in targets]
        print(json.dumps(entry, sort_keys=True))


def validate(
    *,
    inject_defect: bool,
    inject_unowned: bool = False,
    inject_crosswalk: bool = False,
    inject_declaration_crosswalk: bool = False,
) -> list[str]:
    data = tomllib.loads(MANIFEST.read_text(encoding="utf-8"))
    entries = data.get("body", [])
    field_entries = data.get("field", [])
    rust_value_entries = data.get("rust_value", [])
    container_entries = data.get("rust_container", [])
    methods = original_methods()
    fields = original_fields()
    class_names = canonical_classes()
    java = validate_ast_authority.java_asts()
    game_source = ROOT / "transliteration" / "crates" / "orphan-game-xlat" / "src"
    production_files = {
        str(path.relative_to(ROOT)) for path in sorted(game_source.rglob("*.rs"))
    }
    rust = rust_asts(production_files)
    if inject_unowned:
        rust[(min(production_files), "const:INJECTED_UNOWNED")] = RustEvidence("", ())
    errors: list[str] = []
    if data.get("total_body_count") != len(methods):
        errors.append(
            f"total_body_count is {data.get('total_body_count')}, baseline has {len(methods)}"
        )
    java_field_keys = {key for key in java if key[1].startswith("<field:")}
    if data.get("total_field_count") != len(java_field_keys):
        errors.append(
            f"total_field_count is {data.get('total_field_count')}, "
            f"canonical javac AST has {len(java_field_keys)}"
        )
    if len(fields) != len(java_field_keys):
        errors.append(
            f"original/canonical field denominators differ: {len(fields)} vs "
            f"{len(java_field_keys)}"
        )
    if data.get("reviewed_body_count") != len(entries):
        errors.append("reviewed_body_count does not equal the number of body rows")
    semantic_entries = [
        entry for entry in entries if entry.get("semantic_status") == "crosswalked"
    ]
    if data.get("semantic_reviewed_body_count") != len(semantic_entries):
        errors.append(
            "semantic_reviewed_body_count does not equal the crosswalked body rows"
        )
    semantic_field_entries = [
        entry
        for entry in field_entries
        if entry.get("semantic_status") == "crosswalked"
    ]
    if data.get("semantic_reviewed_field_count") != len(semantic_field_entries):
        errors.append(
            "semantic_reviewed_field_count does not equal the crosswalked field rows"
        )
    java_claims = [
        (entry.get("java_class"), entry.get("java_name"), entry.get("descriptor"))
        for entry in entries
    ]
    rust_function_claims = [
        (target.get("file"), target.get("item"))
        for entry in entries
        for target in entry.get("rust", [])
    ]
    rust_value_claims = [
        (target.get("file"), target.get("item"))
        for entry in field_entries
        for target in entry.get("rust", [])
    ]
    rust_value_claims.extend(
        (entry.get("file"), entry.get("item")) for entry in rust_value_entries
    )
    rust_container_claims = [
        (entry.get("file"), entry.get("item")) for entry in container_entries
    ]
    if len(set(java_claims)) != len(java_claims):
        errors.append("a Java method is reviewed more than once")
    java_field_claims = [
        (entry.get("java_class"), entry.get("java_name"), entry.get("descriptor"))
        for entry in field_entries
    ]
    canonical_field_claims = [
        (entry.get("canonical_class"), entry.get("java_item"))
        for entry in field_entries
    ]
    if len(set(java_field_claims)) != len(java_field_claims):
        errors.append("an original Java field is reviewed more than once")
    if len(set(canonical_field_claims)) != len(canonical_field_claims):
        errors.append("a canonical Java field is reviewed more than once")
    all_rust_claims = [
        *rust_function_claims,
        *rust_value_claims,
        *rust_container_claims,
    ]
    if len(set(all_rust_claims)) != len(all_rust_claims):
        errors.append("a Rust declaration is reviewed more than once")
    production_functions = {
        key for key in rust if key[1].startswith(RUST_FUNCTION_PREFIXES)
    }
    production_values = {
        key for key in rust if key[1].startswith(RUST_VALUE_PREFIXES)
    }
    production_containers = {
        key for key in rust if key[1].startswith(RUST_CONTAINER_PREFIXES)
    }
    production_items = set(rust)
    categorized_items = production_functions | production_values | production_containers
    claimed_functions = set(rust_function_claims)
    claimed_values = set(rust_value_claims)
    claimed_containers = set(rust_container_claims)
    if data.get("total_rust_declaration_count") != len(production_items):
        errors.append(
            "total_rust_declaration_count does not equal the production syn inventory"
        )
    if data.get("total_rust_function_count") != len(production_functions):
        errors.append(
            "total_rust_function_count does not equal the production syn inventory"
        )
    if data.get("reviewed_rust_function_count") != len(claimed_functions):
        errors.append(
            "reviewed_rust_function_count does not equal the reviewed Rust claims"
        )
    if data.get("total_rust_value_declaration_count") != len(production_values):
        errors.append(
            "total_rust_value_declaration_count does not equal the production syn inventory"
        )
    if data.get("reviewed_rust_value_declaration_count") != len(claimed_values):
        errors.append(
            "reviewed_rust_value_declaration_count does not equal the reviewed Rust claims"
        )
    if data.get("total_rust_container_count") != len(production_containers):
        errors.append(
            "total_rust_container_count does not equal the production syn inventory"
        )
    if data.get("reviewed_rust_container_count") != len(claimed_containers):
        errors.append(
            "reviewed_rust_container_count does not equal the reviewed Rust claims"
        )
    missing_functions = sorted(production_functions - claimed_functions)
    extra_functions = sorted(claimed_functions - production_functions)
    missing_values = sorted(production_values - claimed_values)
    extra_values = sorted(claimed_values - production_values)
    missing_containers = sorted(production_containers - claimed_containers)
    extra_containers = sorted(claimed_containers - production_containers)
    unknown_declarations = sorted(production_items - categorized_items)
    if missing_functions:
        errors.append(f"unreviewed production Rust functions: {missing_functions}")
    if extra_functions:
        errors.append(f"review claims absent Rust functions: {extra_functions}")
    if missing_values:
        errors.append(f"unreviewed production Rust value declarations: {missing_values}")
    if extra_values:
        errors.append(f"review claims absent Rust value declarations: {extra_values}")
    if missing_containers:
        errors.append(f"unreviewed production Rust containers: {missing_containers}")
    if extra_containers:
        errors.append(f"review claims absent Rust containers: {extra_containers}")
    if unknown_declarations:
        errors.append(f"unknown production Rust declaration categories: {unknown_declarations}")
    for index, manifest_entry in enumerate(entries):
        entry = manifest_entry
        if inject_crosswalk and index == 0:
            entry = dict(manifest_entry)
            operations = [dict(operation) for operation in entry["operation"]]
            first_ranges = [list(value) for value in operations[0]["java_node_ranges"]]
            first_ranges[0][0] += 1
            operations[0]["java_node_ranges"] = first_ranges
            entry["operation"] = operations
        label = f"body[{index}] {entry.get('java_class')}.{entry.get('java_name')}"
        expected_canonical_class = class_names.get(entry.get("java_class"))
        if entry.get("canonical_class") != expected_canonical_class:
            errors.append(
                f"{label}: canonical_class must be {expected_canonical_class!r}"
            )
        if entry.get("semantic_status") != "crosswalked":
            errors.append(f"{label}: semantic_status must be 'crosswalked'")
        if not entry.get("semantic_review"):
            errors.append(f"{label}: semantic_review is required")
        if entry.get("oracle") != "pure-method-v1":
            errors.append(f"{label}: must name the executable oracle")
        try:
            actual = java_evidence_facts(entry, methods, java)
        except KeyError as error:
            errors.append(f"{label}: missing evidence {error}")
            continue
        for name, value in actual.items():
            expected = entry.get(name)
            if inject_defect and index == 0 and name == "opcode_sha256":
                expected = "0" * 64
            if expected != value:
                errors.append(f"{label}: {name} changed")
        targets = entry.get("rust", [])
        if not targets:
            errors.append(f"{label}: no Rust AST target")
            continue
        target_evidence: list[RustEvidence] = []
        for target_index, target in enumerate(targets):
            target_label = f"{label} Rust target {target_index}"
            try:
                target_actual = rust_evidence_facts(target, rust)
                target_evidence.append(rust[(target["file"], target["item"])])
            except KeyError as error:
                errors.append(f"{target_label}: missing evidence {error}")
                continue
            for name, value in target_actual.items():
                if target.get(name) != value:
                    errors.append(f"{target_label}: {name} changed")
        java_evidence = java.get((entry["canonical_class"], entry["java_item"]))
        if java_evidence is not None and len(target_evidence) == len(targets):
            errors.extend(
                validate_semantic_crosswalk(
                    entry,
                    label,
                    java_evidence,
                    target_evidence,
                )
            )

    for index, manifest_entry in enumerate(field_entries):
        entry = manifest_entry
        if inject_declaration_crosswalk and index == 0:
            entry = dict(manifest_entry)
            operations = [dict(operation) for operation in entry["operation"]]
            first_ranges = [list(value) for value in operations[0]["java_node_ranges"]]
            first_ranges[0][0] += 1
            operations[0]["java_node_ranges"] = first_ranges
            entry["operation"] = operations
        label = f"field[{index}] {entry.get('java_class')}.{entry.get('java_name')}"
        expected_canonical_class = class_names.get(entry.get("java_class"))
        if entry.get("canonical_class") != expected_canonical_class:
            errors.append(
                f"{label}: canonical_class must be {expected_canonical_class!r}"
            )
        canonical_name = entry.get("canonical_name", entry.get("java_name"))
        if entry.get("java_item") != f"<field:{canonical_name}>":
            errors.append(f"{label}: java_item must identify the named field")
        if entry.get("semantic_status") != "crosswalked":
            errors.append(f"{label}: semantic_status must be 'crosswalked'")
        if not entry.get("semantic_review"):
            errors.append(f"{label}: semantic_review is required")
        try:
            actual = java_field_evidence_facts(entry, fields, java)
        except KeyError as error:
            errors.append(f"{label}: missing evidence {error}")
            continue
        for name, value in actual.items():
            if entry.get(name) != value:
                errors.append(f"{label}: {name} changed")
        targets = entry.get("rust", [])
        if not targets:
            errors.append(f"{label}: no Rust AST target")
            continue
        target_evidence = []
        for target_index, target in enumerate(targets):
            target_label = f"{label} Rust target {target_index}"
            try:
                target_actual = rust_evidence_facts(target, rust)
                target_evidence.append(rust[(target["file"], target["item"])])
            except KeyError as error:
                errors.append(f"{target_label}: missing evidence {error}")
                continue
            for name, value in target_actual.items():
                if target.get(name) != value:
                    errors.append(f"{target_label}: {name} changed")
        java_evidence = java.get((entry["canonical_class"], entry["java_item"]))
        if java_evidence is not None and len(target_evidence) == len(targets):
            errors.extend(
                validate_semantic_crosswalk(
                    entry,
                    label,
                    java_evidence,
                    target_evidence,
                )
            )

    for index, entry in enumerate(container_entries):
        label = f"rust_container[{index}] {entry.get('file')}::{entry.get('item')}"
        if not entry.get("semantic_review"):
            errors.append(f"{label}: semantic_review is required")
        try:
            actual = rust_evidence_facts(entry, rust)
        except KeyError as error:
            errors.append(f"{label}: missing evidence {error}")
            continue
        for name, value in actual.items():
            if entry.get(name) != value:
                errors.append(f"{label}: {name} changed")
        if actual["node_count"] != 0:
            errors.append(
                f"{label}: container emitted semantic nodes; crosswalk them explicitly"
            )
    for index, entry in enumerate(rust_value_entries):
        label = f"rust_value[{index}] {entry.get('file')}::{entry.get('item')}"
        if not entry.get("semantic_review"):
            errors.append(f"{label}: semantic_review is required")
        try:
            actual = rust_evidence_facts(entry, rust)
        except KeyError as error:
            errors.append(f"{label}: missing evidence {error}")
            continue
        for name, value in actual.items():
            if entry.get(name) != value:
                errors.append(f"{label}: {name} changed")
        if actual["node_count"] != 0:
            errors.append(
                f"{label}: Rust-only value emitted semantic nodes; crosswalk them explicitly"
            )
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--print-entries", action="store_true")
    parser.add_argument("--print-declaration-entries", action="store_true")
    parser.add_argument("--list-pending", action="store_true")
    parser.add_argument(
        "--show-java-nodes",
        nargs=2,
        metavar=("OWNER", "ITEM"),
        help="print indexed javac nodes for one canonical item",
    )
    parser.add_argument(
        "--show-rust-nodes",
        nargs=2,
        metavar=("FILE", "ITEM"),
        help="print indexed syn nodes for one production item",
    )
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--self-test-ownership", action="store_true")
    parser.add_argument("--self-test-crosswalk", action="store_true")
    parser.add_argument("--self-test-declaration-crosswalk", action="store_true")
    arguments = parser.parse_args()
    if arguments.print_entries:
        print_entries()
        return 0
    if arguments.print_declaration_entries:
        print_declaration_entries()
        return 0
    if arguments.list_pending:
        print_pending()
        return 0
    if arguments.show_java_nodes:
        key = tuple(arguments.show_java_nodes)
        evidence = validate_ast_authority.java_asts().get(key)
        if evidence is None:
            parser.error(f"unknown Java AST item {key[0]}.{key[1]}")
        for index, node in enumerate(evidence.nodes):
            print(f"{index}\t{node}")
        return 0
    if arguments.show_rust_nodes:
        file, item = arguments.show_rust_nodes
        evidence = rust_asts({file}).get((file, item))
        if evidence is None:
            parser.error(f"unknown Rust AST item {file}::{item}")
        for index, node in enumerate(evidence.nodes):
            print(f"{index}\t{node}")
        return 0
    errors = validate(
        inject_defect=arguments.self_test,
        inject_unowned=arguments.self_test_ownership,
        inject_crosswalk=arguments.self_test_crosswalk,
        inject_declaration_crosswalk=arguments.self_test_declaration_crosswalk,
    )
    if arguments.self_test_ownership:
        expected_fragments = (
            "total_rust_declaration_count",
            "total_rust_value_declaration_count",
            "unreviewed production Rust value declarations",
        )
        if len(errors) != 3 or not all(
            any(fragment in error for error in errors)
            for fragment in expected_fragments
        ):
            print(
                "SELF-TEST FAILED: expected declaration-count and ownership defects, "
                f"found {errors}"
            )
            return 3
        print("self-test OK: an unowned production Rust declaration was rejected (R3)")
        return 0
    if arguments.self_test_declaration_crosswalk:
        if len(errors) != 1 or "uncovered Java nodes [0]" not in errors[0]:
            print(
                "SELF-TEST FAILED: expected one uncovered declaration node, "
                f"found {errors}"
            )
            return 3
        print("self-test OK: an uncovered declaration AST node was rejected (R3)")
        return 0
    if arguments.self_test_crosswalk:
        if len(errors) != 1 or "uncovered Java nodes [0]" not in errors[0]:
            print(f"SELF-TEST FAILED: expected one uncovered node, found {errors}")
            return 3
        print("self-test OK: an uncovered semantic AST node was rejected (R3)")
        return 0
    if arguments.self_test:
        if len(errors) != 1 or "opcode_sha256 changed" not in errors[0]:
            print(f"SELF-TEST FAILED: expected one opcode defect, found {errors}")
            return 3
        print("self-test OK: a one-method opcode-authority mutation was rejected (R3)")
        return 0
    if errors:
        print("method audit validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    document = tomllib.loads(MANIFEST.read_text())
    print(
        f"method audit OK: {len(document['body'])}/350 bodies and "
        f"{len(document.get('field', []))}/1075 fields reviewed"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
