use std::io::{self, BufRead};

use orphan_formats::Reader;
use orphan_game_xlat::{
    abs, action_key_get_script_id, action_key_id_convert, action_key_init_system,
    action_key_keycode_to_action_key, action_key_unset_all_keys, application_app_start,
    application_clear_all_rms, application_destroy_app, application_free_memory, application_new,
    application_paint, application_pause_app, application_print_array,
    application_repaint_canvas_if_possible, application_resource_make_subchunk,
    application_rms_delete, application_rms_get, application_room_repaint_run,
    application_save_chunk_ini, application_set_display, array_copy_string_handles, char_to_string,
    cheat_controller_initialize, cheat_controller_new, coded_string, dir, find,
    game_canvas_key_jad_entry_as_int, game_canvas_key_pressed, game_canvas_key_released,
    game_canvas_new, game_canvas_paint, game_canvas_resume_sound, game_canvas_show_notify,
    game_language_path, game_resource_equals, game_resource_initialize, game_resource_new,
    game_resource_paint, game_resource_paint_simple, get_game_text, get_game_text_from_string,
    get_language_selection_position, get_left, get_top, ink_codes_new,
    ink_engine_inventory_equip_unequip_handling, ink_engine_new, ink_engine_popup_create,
    ink_engine_popup_create_with_max_time, ink_engine_popup_set_next, ink_engine_wrap_string,
    ink_interpreter_execute, ink_interpreter_has_command, ink_interpreter_integer_argument,
    ink_interpreter_new, ink_interpreter_read, ink_interpreter_read_bytes,
    ink_interpreter_read_signed, ink_interpreter_resume, ink_script_execute_event,
    ink_script_execute_event_by_id, ink_script_execute_event_debug, ink_script_get_item_name,
    ink_script_get_string, ink_script_get_variable, ink_script_get_variable_as_integer,
    ink_script_has_command, ink_script_has_event, ink_script_initialize, ink_script_is_waiting,
    ink_script_new, ink_script_resume, ink_script_set_variable, ink_script_stop,
    ink_server_get_hint, ink_server_get_variable, ink_server_set_variable,
    ink_server_unset_variable, inventory_remove, inventory_set, inventory_size,
    is_menu_scroll_allowed, key_convert_to_key_id, key_init, load_request_resource_path,
    load_request_resource_path_for_object, load_request_resource_path_for_string, loading, max,
    menu_active, menu_add_choice, menu_add_choice_integer, menu_close_all, menu_close_current,
    menu_count_choices, menu_get_choice_id, menu_get_choice_number, menu_get_current,
    menu_initialize, menu_next_choice, menu_previous_choice, menu_scroll_decrease,
    menu_scroll_increase, menu_set_current, menu_set_inventory_item_resource, menu_set_position,
    menu_set_softkey_options, menu_set_top, min, random_scaled, read_string, read_string_list,
    remove_string_prefix, reset_load, reset_variable_system, resource_exit, resource_heap_index,
    resource_merge_sort_cmp, resource_request_create_from_input, resource_request_description,
    resource_request_equals, resource_request_get_id, resource_request_new_for_object,
    resource_request_new_for_string, resource_request_resource_path, resource_request_to_string,
    resource_restart_importants, resource_url_encode, room_add_to_history, room_current,
    room_history_size, room_last_in_history, room_object_battle_panel_new,
    room_object_bp_set_bar_size, room_object_bp_set_health, room_object_bp_set_max_health,
    room_object_bp_set_time, room_object_enter_hover, room_object_execute_event,
    room_object_get_move_direction, room_object_get_name, room_object_initialize,
    room_object_is_over, room_object_new, room_remove_last_from_history, room_set_current,
    set_key_status, silent_hill_game_app_init, silent_hill_game_menu_reset_ingame_values,
    silent_hill_game_new, splash_more_exists, text_id_new, text_replace_first, tick_based_time,
    tick_based_time_reset, tick_based_time_update, to_boolean, to_int, write_string,
    ApplicationRepaintCanvasIfPossibleError, ApplicationResourceMakeSubChunkError,
    ApplicationRmsDeleteError, ApplicationRmsGetCallError, ApplicationState,
    CheatControllerStatics, GameCanvasState, GameResourceState, GameResourceStatics,
    InkEnginePopupCreateError, InkEngineState, InkInterpreterState, InkInterpreterStatics,
    InkScriptExecuteEventError, InkScriptGetItemNameError, InkScriptRegistryValue, InkScriptState,
    InkScriptStatics, InkVariableError, JavaObject, JavaOwnedObject, JavaResourceId, MenuState,
    MenuStatics, ResourceRequestState, RoomObjectEnterHoverError, RoomObjectState,
    RoomObjectStatics, RoomObjectStringEventError, SilentHillGameStatics,
    GAME_CANVAS_INITIAL_TRANSFORM_TABLE,
};

fn value(parts: &[&str], index: usize) -> i32 {
    parts[index].parse().expect("oracle input must be i32")
}

fn game_canvas_state(parts: &[&str], start: usize) -> GameCanvasState {
    GameCanvasState {
        transform_table: GAME_CANVAS_INITIAL_TRANSFORM_TABLE,
        sound_id: None,
        loop_count: 0,
        key_softkey_left: value(parts, start),
        key_softkey_right: value(parts, start + 1),
        key_send: value(parts, start + 9),
        key_return: value(parts, start + 7),
        key_softkey_center: value(parts, start + 2),
        key_arrow_up: value(parts, start + 3),
        key_arrow_down: value(parts, start + 4),
        key_arrow_left: value(parts, start + 5),
        key_arrow_right: value(parts, start + 6),
        key_erase: value(parts, start + 8),
    }
}

fn key_state_output(application: &ApplicationState, engine: &InkEngineState) -> String {
    format!(
        "{}:{}:{}:{}",
        i32::from(application.key_new),
        i32::from(application.key_pressed),
        application.key_last_pressed,
        engine.menu_scroll_tick_counter
    )
}

fn key_bindings_output(status: &str, canvas: &GameCanvasState) -> String {
    format!(
        "{status}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}",
        canvas.key_softkey_left,
        canvas.key_softkey_right,
        canvas.key_softkey_center,
        canvas.key_arrow_up,
        canvas.key_arrow_down,
        canvas.key_arrow_left,
        canvas.key_arrow_right,
        canvas.key_return,
        canvas.key_erase,
        canvas.key_send
    )
}

fn bytes(token: &str) -> Option<Vec<u8>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('h') && token.len() % 2 == 1);
    Some(
        token.as_bytes()[1..]
            .chunks_exact(2)
            .map(|pair| {
                u8::from_str_radix(std::str::from_utf8(pair).expect("ASCII hex"), 16)
                    .expect("byte token must be hex")
            })
            .collect(),
    )
}

fn byte_arrays(token: &str) -> Option<Vec<Option<Vec<u8>>>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('a'));
    Some(token[1..].split(',').map(bytes).collect())
}

fn bytes_output(value: &[u8]) -> String {
    if value.is_empty() {
        return "-".to_owned();
    }
    let mut result = String::from("h");
    for byte in value {
        result.push_str(&format!("{byte:02x}"));
    }
    result
}

fn write_output(status: &str, attempts: Option<&[i32]>, bytes: Option<&[u8]>) -> String {
    format!(
        "{status}:{}:{}",
        ints_output(attempts),
        bytes.map_or_else(|| "null".to_owned(), bytes_output)
    )
}

fn utf16(token: &str) -> Option<Vec<u16>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('u') && (token.len() - 1).is_multiple_of(4));
    Some(
        token.as_bytes()[1..]
            .chunks_exact(4)
            .map(|unit| {
                u16::from_str_radix(std::str::from_utf8(unit).expect("ASCII hex"), 16)
                    .expect("UTF-16 token must be hex")
            })
            .collect(),
    )
}

fn utf16_output(value: Option<&[u16]>) -> String {
    match value {
        None => "null".to_owned(),
        Some([]) => "-".to_owned(),
        Some(units) => {
            let mut result = String::from("u");
            for unit in units {
                result.push_str(&format!("{unit:04x}"));
            }
            result
        }
    }
}

fn script_ids(token: &str) -> Option<Vec<Option<Vec<u16>>>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('s'));
    Some(token[1..].split(',').map(utf16).collect())
}

fn script_ids_output(values: Option<&[Option<Vec<u16>>]>) -> String {
    match values {
        None => "null".to_owned(),
        Some([]) => "-".to_owned(),
        Some(values) => {
            let mut result = String::from("s");
            for (index, value) in values.iter().enumerate() {
                if index != 0 {
                    result.push(',');
                }
                result.push_str(&utf16_output(value.as_deref()));
            }
            result
        }
    }
}

fn java_owned_object_output(value: Option<&JavaOwnedObject>) -> String {
    match value {
        None => "N".to_owned(),
        Some(JavaOwnedObject::Integer(value)) => format!("I,{value}"),
        Some(JavaOwnedObject::String(value)) => {
            format!("S,{}", utf16_output(Some(value)))
        }
        Some(JavaOwnedObject::Other) => "O".to_owned(),
    }
}

fn ink_script_output(state: &InkScriptState, remaining: &str) -> String {
    format!(
        "{}:{}:{}:{}:{remaining}",
        java_owned_object_output(state.gfx_id.as_ref()),
        ints_output(state.event_offsets.as_deref()),
        state
            .data
            .as_deref()
            .map_or_else(|| "null".to_owned(), bytes_output),
        script_ids_output(state.string_list.as_deref()),
    )
}

fn menu_handles(token: &str) -> Option<Vec<u32>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('m'));
    Some(
        token[1..]
            .split(',')
            .map(|handle| handle.parse().expect("menu handle must be u32"))
            .collect(),
    )
}

fn menu_handles_output(values: Option<&[u32]>) -> String {
    match values {
        None => "null".to_owned(),
        Some([]) => "-".to_owned(),
        Some(values) => {
            let mut result = String::from("m");
            for (index, value) in values.iter().enumerate() {
                if index != 0 {
                    result.push(',');
                }
                result.push_str(&value.to_string());
            }
            result
        }
    }
}

fn string_table(token: &str) -> Option<Vec<(Vec<u16>, Vec<u16>)>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('m'));
    let mut result: Vec<(Vec<u16>, Vec<u16>)> = Vec::new();
    for entry in token[1..].split(',') {
        let (key, value) = entry
            .split_once('=')
            .expect("map entry must contain equals");
        let key = utf16(key).expect("map key must be nonnull");
        let value = utf16(value).expect("map value must be nonnull");
        if let Some((_, old_value)) = result.iter_mut().find(|(old_key, _)| *old_key == key) {
            *old_value = value;
        } else {
            result.push((key, value));
        }
    }
    Some(result)
}

fn settings_table(token: &str) -> Option<Vec<(Vec<u16>, JavaOwnedObject)>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('q'));
    let mut result: Vec<(Vec<u16>, JavaOwnedObject)> = Vec::new();
    for entry in token[1..].split(',') {
        let (key, encoded_value) = entry
            .split_once('=')
            .expect("settings entry must contain equals");
        let key = utf16(key).expect("settings key must be nonnull");
        let setting = match encoded_value.as_bytes().first().copied() {
            Some(b'i') => JavaOwnedObject::Integer(
                encoded_value[1..]
                    .parse()
                    .expect("integer setting must be i32"),
            ),
            Some(b's') => JavaOwnedObject::String(
                utf16(&encoded_value[1..]).expect("string setting must be nonnull"),
            ),
            Some(b'o') if encoded_value.len() == 1 => JavaOwnedObject::Other,
            _ => panic!("unknown settings value kind"),
        };
        if let Some((_, old_value)) = result.iter_mut().find(|(old_key, _)| *old_key == key) {
            *old_value = setting;
        } else {
            result.push((key, setting));
        }
    }
    Some(result)
}

fn string_table_output(values: Option<&[(Vec<u16>, Vec<u16>)]>) -> String {
    let Some(values) = values else {
        return "null".to_owned();
    };
    if values.is_empty() {
        return "-".to_owned();
    }
    let mut entries: Vec<_> = values.iter().collect();
    entries.sort_by(|left, right| left.0.cmp(&right.0));
    let mut result = String::from("m");
    for (index, (key, value)) in entries.into_iter().enumerate() {
        if index != 0 {
            result.push(',');
        }
        result.push_str(&utf16_output(Some(key)));
        result.push('=');
        result.push_str(&utf16_output(Some(value)));
    }
    result
}

fn server_state(variable_token: &str, hint_token: &str, changed: bool) -> ApplicationState {
    ApplicationState {
        tick_based_time_value: 0,
        canvas_width: 0,
        fade_frames: 0,
        demo_frames: 0,
        painting: false,
        cur_sound_mode: false,
        canvas_instance: None,
        key_last_pressed: 0,
        key_new: false,
        key_pressed: false,
        load_bar_active: false,
        goto_dissolve_fx_counter: -6,
        loading_mode: -1,
        load_thread: None,
        room_repaint_thread: None,
        resource_importants: None,
        resources_to_download: None,
        game_id: None,
        game_texts: None,
        languages: None,
        resource_heap_sources: None,
        resource_sc_data: None,
        resource_sc_current_size: 0,
        random_instance: None,
        runtime_instance: None,
        midlet_instance: None,
        ink_server_variables: string_table(variable_token),
        ink_server_hints: string_table(hint_token),
        game_changed_since_last_save: changed,
        save_is_possible: false,
    }
}

fn mutation_output(
    result: Result<(), orphan_jvm::NullPointerException>,
    state: &ApplicationState,
) -> String {
    format!(
        "{}:{}:{}:{}",
        if result.is_ok() { "OK" } else { "NPE" },
        string_table_output(state.ink_server_variables.as_deref()),
        string_table_output(state.ink_server_hints.as_deref()),
        i32::from(state.game_changed_since_last_save),
    )
}

fn ink_variable_error_output(error: InkVariableError) -> &'static str {
    match error {
        InkVariableError::NullPointer => "NPE",
        InkVariableError::StringIndexOutOfBounds => "SIOOBE",
        InkVariableError::NumberFormat => "NFE",
    }
}

fn ink_variable_output(result: Result<JavaObject<'_>, InkVariableError>) -> String {
    match result {
        Ok(JavaObject::Null) => "N".to_owned(),
        Ok(JavaObject::Integer(value)) => format!("I:{value}"),
        Ok(JavaObject::String(value)) => format!("S:{}", utf16_output(Some(value))),
        Ok(JavaObject::Other) => unreachable!("getVariable never constructs another Object kind"),
        Err(error) => ink_variable_error_output(error).to_owned(),
    }
}

fn execution_value(kind: &str, token: &str) -> Option<JavaOwnedObject> {
    match kind {
        "n" => None,
        "i" => Some(JavaOwnedObject::Integer(
            token.parse().expect("execution integer must be i32"),
        )),
        "s" => Some(JavaOwnedObject::String(
            utf16(token).expect("execution string must be non-null"),
        )),
        _ => panic!("invalid execution value kind: {kind}"),
    }
}

fn execution_value_output(value: Option<&JavaOwnedObject>) -> String {
    match value {
        None => "N".to_owned(),
        Some(JavaOwnedObject::Integer(value)) => format!("I:{value}"),
        Some(JavaOwnedObject::String(value)) => format!("S:{}", utf16_output(Some(value))),
        Some(JavaOwnedObject::Other) => "O".to_owned(),
    }
}

fn paused_thread_output(
    paused_thread: Option<u32>,
    self_handle: u32,
    other_handle: u32,
) -> &'static str {
    match paused_thread {
        None => "N",
        Some(handle) if handle == self_handle => "S",
        Some(handle) if handle == other_handle => "O",
        Some(_) => "WRONG",
    }
}

fn execution_output(
    result: Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
    state: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    self_handle: u32,
    other_handle: u32,
) -> String {
    let (outcome, value) = match result {
        Ok(value) => ("OK".to_owned(), value),
        Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => ("NPE".to_owned(), None),
        Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
            (format!("AIOOBE:{}:{}", error.index, error.length), None)
        }
    };
    format!(
        "{outcome}:{}:{}:{}:{}",
        execution_value_output(value.as_ref()),
        state.status,
        state.offset,
        paused_thread_output(statics.paused_thread, self_handle, other_handle)
    )
}

fn event_execution_output(
    result: Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    room_object: Option<u32>,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let (outcome, value) = match result {
        Ok(value) => ("OK", value),
        Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => ("NPE", None),
        Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_)) => ("AIOOBE", None),
    };
    event_execution_state_output(
        outcome,
        value,
        interpreter,
        statics,
        script_handle,
        room_object,
        interpreter_handle,
        old_paused_handle,
    )
}

#[allow(clippy::too_many_arguments)]
fn event_execution_state_output(
    outcome: &str,
    value: Option<JavaOwnedObject>,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    room_object: Option<u32>,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let paused = match statics.paused_thread {
        None => "N:N:N:N:N:N".to_owned(),
        Some(handle) if handle == old_paused_handle => "O:0:0:N:N:0".to_owned(),
        Some(handle) if handle == interpreter_handle => format!(
            "I:{}:{}:{}:{}:{}",
            interpreter.status,
            interpreter.offset,
            match interpreter.script {
                None => "N",
                Some(handle) if handle == script_handle => "S",
                Some(_) => "WRONG",
            },
            match interpreter.room_object {
                None => "N",
                Some(handle) if Some(handle) == room_object => "R",
                Some(_) => "WRONG",
            },
            i32::from(interpreter.language_debug_mode)
        ),
        Some(_) => "WRONG:N:N:N:N:N".to_owned(),
    };
    format!(
        "{outcome}:{}:{paused}",
        execution_value_output(value.as_ref())
    )
}

#[allow(clippy::too_many_arguments)]
fn event_execution_by_id_output(
    result: Result<
        Option<JavaOwnedObject>,
        InkScriptExecuteEventError<orphan_jvm::ArrayAccessException>,
    >,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    room_object: Option<u32>,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let (outcome, value) = match result {
        Ok(value) => ("OK", value),
        Err(InkScriptExecuteEventError::NullPointer) => ("NPE", None),
        Err(InkScriptExecuteEventError::ClassCast) => ("CCE", None),
        Err(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::NullPointer(_),
        )) => ("NPE", None),
        Err(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_),
        )) => ("AIOOBE", None),
    };
    event_execution_state_output(
        outcome,
        value,
        interpreter,
        statics,
        script_handle,
        room_object,
        interpreter_handle,
        old_paused_handle,
    )
}

#[allow(clippy::too_many_arguments)]
fn item_name_execution_output(
    result: Result<Option<Vec<u16>>, InkScriptGetItemNameError<orphan_jvm::ArrayAccessException>>,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let (outcome, value) = match result {
        Ok(value) => ("OK", value.map(JavaOwnedObject::String)),
        Err(InkScriptGetItemNameError::ClassCast) => ("CCE", None),
        Err(InkScriptGetItemNameError::ExecuteEvent(InkScriptExecuteEventError::NullPointer)) => {
            ("NPE", None)
        }
        Err(InkScriptGetItemNameError::ExecuteEvent(InkScriptExecuteEventError::ClassCast)) => {
            ("CCE", None)
        }
        Err(InkScriptGetItemNameError::ExecuteEvent(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::NullPointer(_),
        ))) => ("NPE", None),
        Err(InkScriptGetItemNameError::ExecuteEvent(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_),
        ))) => ("AIOOBE", None),
    };
    event_execution_state_output(
        outcome,
        value,
        interpreter,
        statics,
        script_handle,
        None,
        interpreter_handle,
        old_paused_handle,
    )
}

fn room_script_output(state: &RoomObjectState, script_handle: u32) -> &'static str {
    match state.script {
        None => "N",
        Some(handle) if handle == script_handle => "S",
        Some(_) => "WRONG",
    }
}

#[allow(clippy::too_many_arguments)]
fn room_event_execution_output(
    result: Result<
        Option<JavaOwnedObject>,
        InkScriptExecuteEventError<orphan_jvm::ArrayAccessException>,
    >,
    room: &RoomObjectState,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    room_handle: u32,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let (outcome, value) = match result {
        Ok(value) => ("OK", value),
        Err(InkScriptExecuteEventError::NullPointer) => ("NPE", None),
        Err(InkScriptExecuteEventError::ClassCast) => ("CCE", None),
        Err(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::NullPointer(_),
        )) => ("NPE", None),
        Err(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_),
        )) => ("AIOOBE", None),
    };
    let execution = event_execution_state_output(
        outcome,
        value,
        interpreter,
        statics,
        script_handle,
        Some(room_handle),
        interpreter_handle,
        old_paused_handle,
    );
    format!("{execution}:{}", room_script_output(room, script_handle))
}

#[allow(clippy::too_many_arguments)]
fn room_string_event_execution_output(
    result: Result<Option<Vec<u16>>, RoomObjectStringEventError<orphan_jvm::ArrayAccessException>>,
    room: &RoomObjectState,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    room_handle: u32,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let (outcome, value) = match result {
        Ok(value) => ("OK", value.map(JavaOwnedObject::String)),
        Err(RoomObjectStringEventError::ClassCast) => ("CCE", None),
        Err(RoomObjectStringEventError::ExecuteEvent(InkScriptExecuteEventError::NullPointer)) => {
            ("NPE", None)
        }
        Err(RoomObjectStringEventError::ExecuteEvent(InkScriptExecuteEventError::ClassCast)) => {
            ("CCE", None)
        }
        Err(RoomObjectStringEventError::ExecuteEvent(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::NullPointer(_),
        ))) => ("NPE", None),
        Err(RoomObjectStringEventError::ExecuteEvent(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_),
        ))) => ("AIOOBE", None),
    };
    let execution = event_execution_state_output(
        outcome,
        value,
        interpreter,
        statics,
        script_handle,
        Some(room_handle),
        interpreter_handle,
        old_paused_handle,
    );
    format!("{execution}:{}", room_script_output(room, script_handle))
}

#[allow(clippy::too_many_arguments)]
fn room_hover_execution_output(
    result: Result<Option<u32>, RoomObjectEnterHoverError<orphan_jvm::ArrayAccessException>>,
    room: &RoomObjectState,
    interpreter: &InkInterpreterState,
    statics: &InkInterpreterStatics,
    script_handle: u32,
    room_handle: u32,
    interpreter_handle: u32,
    old_paused_handle: u32,
) -> String {
    let (outcome, returned_room) = match result {
        Ok(None) => ("OK", "N"),
        Ok(Some(handle)) if handle == room_handle => ("OK", "R"),
        Ok(Some(_)) => ("OK", "WRONG"),
        Err(RoomObjectEnterHoverError::HasEvent(
            orphan_jvm::ArrayAccessException::NullPointer(_),
        )) => ("NPE", "N"),
        Err(RoomObjectEnterHoverError::HasEvent(
            orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_),
        )) => ("AIOOBE", "N"),
        Err(RoomObjectEnterHoverError::ExecuteEvent(InkScriptExecuteEventError::NullPointer)) => {
            ("NPE", "N")
        }
        Err(RoomObjectEnterHoverError::ExecuteEvent(InkScriptExecuteEventError::ClassCast)) => {
            ("CCE", "N")
        }
        Err(RoomObjectEnterHoverError::ExecuteEvent(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::NullPointer(_),
        ))) => ("NPE", "N"),
        Err(RoomObjectEnterHoverError::ExecuteEvent(InkScriptExecuteEventError::Execute(
            orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_),
        ))) => ("AIOOBE", "N"),
    };
    let execution = event_execution_state_output(
        outcome,
        None,
        interpreter,
        statics,
        script_handle,
        Some(room_handle),
        interpreter_handle,
        old_paused_handle,
    );
    format!(
        "{execution}:{returned_room}:{}",
        room_script_output(room, script_handle)
    )
}

fn ink_script_statics(token: &str) -> InkScriptStatics {
    let scripts = if token == "null" {
        None
    } else if token == "-" {
        Some(Vec::new())
    } else {
        let value = if token.starts_with('s') {
            InkScriptRegistryValue::Script(3)
        } else {
            InkScriptRegistryValue::Other
        };
        Some(vec![(
            utf16(&token[1..]).expect("registry key must be non-null"),
            value,
        )])
    };
    InkScriptStatics {
        scripts,
        wait_stop: 0,
        item_id: None,
    }
}

fn oracle_execute_command(
    state: &mut InkInterpreterState,
    script: &InkScriptState,
) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException> {
    let command_byte = ink_interpreter_read(state, script)?;
    let command = command_byte & 63;
    let mut argument_count = command_byte >> 6;
    if argument_count == 3 {
        argument_count = ink_interpreter_read(state, script)?;
    }
    if command == 5 {
        let byte_count = if argument_count == 0 {
            1
        } else if argument_count == 1 {
            2
        } else {
            4
        };
        return ink_interpreter_read_signed(state, script, byte_count)
            .map(|value| Some(JavaOwnedObject::Integer(value)));
    }
    if command == 6 {
        let string_index = ink_interpreter_read(state, script)?;
        return ink_script_get_string(script, string_index)
            .map(|value| value.map(|value| JavaOwnedObject::String(value.to_vec())));
    }
    if command == 2 {
        state.status = 2;
        return Ok(None);
    }
    let mut arguments = Vec::new();
    let mut argument_index = 0;
    while argument_index < argument_count {
        arguments.push(oracle_execute_command(state, script)?);
        argument_index += 1;
    }
    match command {
        1 => {
            state.status = 3;
            Ok(arguments.remove(0))
        }
        25 => {
            state.status = 4;
            Ok(None)
        }
        29 => {
            state.status = 5;
            Ok(None)
        }
        _ => panic!("unsupported execution-oracle command: {command}"),
    }
}

fn choice_ids(token: &str) -> Option<Vec<Option<i32>>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    assert!(token.starts_with('o'));
    Some(
        token[1..]
            .split(',')
            .map(|value| {
                if value == "n" {
                    None
                } else {
                    Some(value.parse().expect("choice handle must be i32"))
                }
            })
            .collect(),
    )
}

fn handle_array_output(values: Option<&[Option<i32>]>) -> String {
    match values {
        None => "null".to_owned(),
        Some([]) => "-".to_owned(),
        Some(values) => {
            let mut result = String::from("o");
            for (index, value) in values.iter().enumerate() {
                if index != 0 {
                    result.push(',');
                }
                match value {
                    Some(value) => result.push_str(&value.to_string()),
                    None => result.push('n'),
                }
            }
            result
        }
    }
}

fn menu_add_output(
    result: Result<(), orphan_jvm::NullPointerException>,
    state: &MenuState,
) -> String {
    format!(
        "{}:{}:{}:{}:{}",
        if result.is_ok() { "OK" } else { "NPE" },
        handle_array_output(state.choice_ids.as_deref()),
        script_ids_output(state.choice_texts.as_deref()),
        i32::from(state.update_body_lines),
        i32::from(state.update_menu),
    )
}

fn nullable_i32(token: &str) -> Option<i32> {
    if token == "n" {
        None
    } else {
        Some(token.parse().expect("nullable handle must be i32 or n"))
    }
}

fn choice_menu_state(
    ids: Option<Vec<Option<i32>>>,
    selected_choice_number: i32,
    scroll: i32,
    update_menu: bool,
) -> MenuState {
    MenuState {
        is_current: false,
        selected_choice_number,
        x: 0,
        y: 0,
        scroll,
        text_scrolling: false,
        update_menu,
        top_text: None,
        update_top_lines: false,
        engine_softkey_option_left: None,
        engine_softkey_option_right: None,
        choice_ids: ids,
        choice_texts: None,
        update_body_lines: false,
        current_inventory_item_resource: None,
    }
}

fn ints(token: &str) -> Option<Vec<i32>> {
    if token == "null" {
        return None;
    }
    if token == "-" {
        return Some(Vec::new());
    }
    Some(
        token[1..]
            .split(',')
            .map(|value| value.parse().expect("integer-array token must contain i32"))
            .collect(),
    )
}

fn ints_output(values: Option<&[i32]>) -> String {
    match values {
        None => "null".to_owned(),
        Some([]) => "-".to_owned(),
        Some(values) => {
            let mut result = String::from("i");
            for (index, value) in values.iter().enumerate() {
                if index != 0 {
                    result.push(',');
                }
                result.push_str(&value.to_string());
            }
            result
        }
    }
}

#[allow(clippy::type_complexity)]
fn popup_texts_output(values: Option<&[Option<Vec<Option<Vec<u16>>>>]>) -> String {
    match values {
        None => "null".to_owned(),
        Some([]) => "-".to_owned(),
        Some(values) => {
            let mut result = String::from("p");
            for (index, lines) in values.iter().enumerate() {
                if index != 0 {
                    result.push(',');
                }
                match lines {
                    None => result.push('n'),
                    Some(lines) if lines.is_empty() => result.push('e'),
                    Some(lines) => {
                        result.push('a');
                        for (line_index, line) in lines.iter().enumerate() {
                            if line_index != 0 {
                                result.push('+');
                            }
                            result.push_str(&utf16_output(line.as_deref()));
                        }
                    }
                }
            }
            result
        }
    }
}

fn request_output(request: &ResourceRequestState) -> String {
    format!(
        "{}:{}:{}:{}",
        request.resource_type,
        request.integer_id,
        utf16_output(request.string_id.as_deref()),
        request.image_transform
    )
}

fn request_id_output(value: (bool, i32, Option<&[u16]>)) -> String {
    if value.0 {
        format!("I:{}", value.1)
    } else {
        format!("S:{}", utf16_output(value.2))
    }
}

fn game_resource_id(token: &str) -> Option<JavaResourceId> {
    match token.as_bytes().first().copied() {
        Some(b'n') if token == "n" => None,
        Some(b'i') => Some(JavaResourceId::Integer(
            token[1..].parse().expect("resource Integer ID must be i32"),
        )),
        Some(b's') => Some(JavaResourceId::String(
            utf16(&token[1..]).expect("resource String ID must be nonnull"),
        )),
        Some(b'o') => Some(JavaResourceId::Opaque(
            token[1..].parse().expect("opaque resource ID must be u32"),
        )),
        _ => panic!("unknown resource ID token"),
    }
}

fn game_resource_id_output(id: Option<&JavaResourceId>) -> String {
    match id {
        None => "n".to_owned(),
        Some(JavaResourceId::Integer(value)) => format!("i{value}"),
        Some(JavaResourceId::String(value)) => format!("s{}", utf16_output(Some(value))),
        Some(JavaResourceId::Opaque(handle)) => format!("o{handle}"),
    }
}

fn game_resource_output(resource: &GameResourceState) -> String {
    format!(
        "{}:{}:{}:{}:{}:{}:{}:{}",
        resource.resource_type,
        game_resource_id_output(resource.id.as_ref()),
        resource
            .image
            .map_or_else(|| "n".to_owned(), |handle| format!("o{handle}")),
        resource.image_width,
        resource.image_height,
        resource.image_registration_x,
        resource.image_registration_y,
        resource.image_transform
    )
}

fn room_object_statics(index: i32, size: i32) -> RoomObjectStatics {
    RoomObjectStatics {
        painting_animation_time: -1,
        no_vibration_yet: true,
        battle_panel_hero_health_id: 1,
        battle_panel_enemy_health_id: 2,
        battle_panel_time_bar_id: 3,
        battle_panel_hard_attack_id: 4,
        battle_panel_fast_attack_id: 5,
        battle_panel_inventory_id: 6,
        battle_panel_escape_id: 7,
        battle_panel_max_health: index,
        battle_panel_health: index,
        battle_panel_bar_size: index,
        battle_panel_time: index,
        battle_panel_size: size,
    }
}

fn room_object_statics_output(statics: &RoomObjectStatics) -> String {
    format!(
        "{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}",
        statics.painting_animation_time,
        statics.no_vibration_yet,
        statics.battle_panel_hero_health_id,
        statics.battle_panel_enemy_health_id,
        statics.battle_panel_time_bar_id,
        statics.battle_panel_hard_attack_id,
        statics.battle_panel_fast_attack_id,
        statics.battle_panel_inventory_id,
        statics.battle_panel_escape_id,
        statics.battle_panel_max_health,
        statics.battle_panel_health,
        statics.battle_panel_bar_size,
        statics.battle_panel_time,
        statics.battle_panel_size,
    )
}

fn empty_room_object_state() -> RoomObjectState {
    RoomObjectState {
        object_type: 0,
        x: 0,
        y: 0,
        width: 0,
        height: 0,
        registration_x: 0,
        registration_y: 0,
        left: 0,
        right: 0,
        top: 0,
        bottom: 0,
        transform: 0,
        gfx_id: None,
        script_id: None,
        script: None,
        visible: false,
        active: false,
        text_alignment: 0,
        animation_data: None,
        animation_parts: None,
        animation_duration: None,
        animation_image_points: None,
        animation_time: 0,
        idle_animation_time: 0,
        run_animation_loops: 0,
        battle_panel_id: 0,
        battle_panel: None,
        color: 16_777_215,
        text: None,
        run_animation_paused_time: -1,
    }
}

fn room_object_output(state: &RoomObjectState, remaining: &str) -> String {
    format!(
        "{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{}:{remaining}",
        state.object_type,
        state.x,
        state.y,
        state.width,
        state.height,
        state.registration_x,
        state.registration_y,
        state.left,
        state.right,
        state.top,
        state.bottom,
        state.transform,
        java_owned_object_output(state.gfx_id.as_ref()),
        utf16_output(state.script_id.as_deref()),
        state.script.map_or_else(|| "n".to_owned(), |handle| handle.to_string()),
        i32::from(state.visible),
        i32::from(state.active),
        state.text_alignment,
        if state.animation_data.is_none() { "n" } else { "a" },
        ints_output(state.animation_parts.as_deref()),
        ints_output(state.animation_duration.as_deref()),
        if state.animation_image_points.is_none() { "n" } else { "a" },
        state.animation_time,
        state.idle_animation_time,
        state.run_animation_loops,
        state.battle_panel_id,
        ints_output(state.battle_panel.as_deref()),
        state.color,
        utf16_output(state.text.as_deref()),
        state.run_animation_paused_time,
    )
}

fn panel_set_output(
    result: Result<(), orphan_jvm::ArrayAccessException>,
    state: &RoomObjectState,
) -> String {
    let suffix = format!(
        "{}:{}",
        state.battle_panel_id,
        ints_output(state.battle_panel.as_deref())
    );
    match result {
        Ok(()) => format!("OK:{suffix}"),
        Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => format!("NPE:{suffix}"),
        Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
            format!("AIOOBE:{}:{}:{suffix}", error.index, error.length)
        }
    }
}

fn main() {
    for line in io::stdin().lock().lines() {
        let line = line.expect("oracle input must be readable");
        let parts: Vec<_> = line.split_ascii_whitespace().collect();
        let result = match parts.first().copied() {
            Some("cheat-init") if parts.len() == 1 => {
                let mut statics = CheatControllerStatics { last_key: 77 };
                cheat_controller_initialize(&mut statics);
                statics.last_key.to_string()
            }
            Some("min") if parts.len() == 3 => min(value(&parts, 1), value(&parts, 2)).to_string(),
            Some("ink-script-init") if parts.len() == 1 => {
                let mut statics = InkScriptStatics {
                    scripts: None,
                    wait_stop: 0,
                    item_id: None,
                };
                ink_script_initialize(&mut statics);
                statics
                    .scripts
                    .as_deref()
                    .map_or_else(|| "null".to_owned(), |scripts| scripts.len().to_string())
            }
            Some("room-object-init") if parts.len() == 1 => {
                let mut statics = RoomObjectStatics {
                    painting_animation_time: i64::MAX,
                    no_vibration_yet: false,
                    battle_panel_hero_health_id: i32::MIN,
                    battle_panel_enemy_health_id: i32::MIN,
                    battle_panel_time_bar_id: i32::MIN,
                    battle_panel_hard_attack_id: i32::MIN,
                    battle_panel_fast_attack_id: i32::MIN,
                    battle_panel_inventory_id: i32::MIN,
                    battle_panel_escape_id: i32::MIN,
                    battle_panel_max_health: i32::MIN,
                    battle_panel_health: i32::MIN,
                    battle_panel_bar_size: i32::MIN,
                    battle_panel_time: i32::MIN,
                    battle_panel_size: i32::MIN,
                };
                room_object_initialize(&mut statics);
                room_object_statics_output(&statics)
            }
            Some("menu-init") if parts.len() == 1 => {
                let mut statics = MenuStatics { stack: None };
                menu_initialize(&mut statics);
                statics
                    .stack
                    .as_deref()
                    .map_or_else(|| "null".to_owned(), |stack| stack.len().to_string())
            }
            Some("game-resource-init") if parts.len() == 1 => {
                let mut statics = GameResourceStatics {
                    cached_images: None,
                    important_images: None,
                };
                game_resource_initialize(&mut statics);
                let distinct = match (&statics.cached_images, &statics.important_images) {
                    (Some(cached), Some(important)) => !core::ptr::eq(cached, important),
                    _ => false,
                };
                format!(
                    "{}:{}:{}",
                    statics
                        .cached_images
                        .as_deref()
                        .map_or_else(|| "null".to_owned(), |images| images.len().to_string()),
                    statics
                        .important_images
                        .as_deref()
                        .map_or_else(|| "null".to_owned(), |images| images.len().to_string()),
                    i32::from(distinct),
                )
            }
            Some("ink-script-new") if parts.len() == 3 => {
                let input_bytes = bytes(parts[1]);
                let mut input = input_bytes.as_deref().map(Reader::new);
                let strings = script_ids(parts[2]);
                let state = ink_script_new(input.as_mut(), strings.as_deref());
                let remaining = input
                    .as_ref()
                    .map_or_else(|| "N".to_owned(), |input| input.remaining().to_string());
                ink_script_output(&state, &remaining)
            }
            Some("room-object-new") if parts.len() == 3 => {
                let input_bytes = bytes(parts[1]);
                let mut input = input_bytes.as_deref().map(Reader::new);
                let strings = script_ids(parts[2]);
                let state = room_object_new(input.as_mut(), strings.as_deref());
                let remaining = input
                    .as_ref()
                    .map_or_else(|| "N".to_owned(), |input| input.remaining().to_string());
                room_object_output(&state, &remaining)
            }
            Some("max") if parts.len() == 3 => max(value(&parts, 1), value(&parts, 2)).to_string(),
            Some("abs") if parts.len() == 2 => abs(value(&parts, 1)).to_string(),
            Some("dir") if parts.len() == 2 => dir(value(&parts, 1)).to_string(),
            Some("left") if parts.len() == 7 => get_left(
                value(&parts, 1),
                value(&parts, 2),
                value(&parts, 3),
                value(&parts, 4),
                value(&parts, 5),
                value(&parts, 6),
            )
            .to_string(),
            Some("top") if parts.len() == 7 => get_top(
                value(&parts, 1),
                value(&parts, 2),
                value(&parts, 3),
                value(&parts, 4),
                value(&parts, 5),
                value(&parts, 6),
            )
            .to_string(),
            Some("resource-exit") if parts.len() == 1 => {
                resource_exit();
                "OK".to_owned()
            }
            Some("destroy-app") if parts.len() == 3 => {
                let mut runtime_present = true;
                let mut app_inited = true;
                let result: Result<(), orphan_jvm::NullPointerException> =
                    application_destroy_app(value(&parts, 1) != 0, || {
                        runtime_present = false;
                        app_inited = false;
                        if value(&parts, 2) == 0 {
                            Err(orphan_jvm::NullPointerException)
                        } else {
                            Ok(())
                        }
                    });
                format!(
                    "{}:{}:{}",
                    if result.is_ok() { "OK" } else { "NPE" },
                    if runtime_present { "R" } else { "N" },
                    i32::from(app_inited)
                )
            }
            Some("pause-app") if parts.len() == 2 => {
                let mut hidden = value(&parts, 1) != 0;
                let result: Result<(), orphan_jvm::NullPointerException> =
                    application_pause_app(|hide| {
                        hidden = hide;
                        Ok(())
                    });
                format!(
                    "{}:{}",
                    if result.is_ok() { "OK" } else { "NPE" },
                    i32::from(hidden)
                )
            }
            Some("app-start") if parts.len() == 2 => {
                let mode = value(&parts, 1);
                let mut attempts = 0;
                let result: Result<(), orphan_jvm::NullPointerException> =
                    application_app_start(|| {
                        if mode == 0 {
                            return Err(orphan_jvm::NullPointerException);
                        }
                        attempts += 1;
                        if mode == 2 {
                            Err(orphan_jvm::NullPointerException)
                        } else {
                            Ok(())
                        }
                    });
                format!("{}:{attempts}", if result.is_ok() { "OK" } else { "NPE" })
            }
            Some("popup-create") if parts.len() == 4 => {
                let text = utf16(parts[1]);
                let recovery_code = value(&parts, 2);
                let popup_number = value(&parts, 3);
                let target = popup_number as usize;
                let mut application = server_state("-", "-", false);
                application.canvas_width = 128;
                let mut engine = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 77,
                    popup_minimum_time_ends: 66,
                    popup_current: 2,
                    popup_number,
                    popup_active: false,
                    popup_choice: 9,
                    popup_recovery_codes: Some(vec![101, 102, 103, 104, 105]),
                    popup_texts: Some(vec![
                        Some(vec![Some(vec![0x0073, 0x0030])]),
                        Some(vec![Some(vec![0x0073, 0x0031])]),
                        Some(vec![Some(vec![0x0073, 0x0032])]),
                        Some(vec![Some(vec![0x0073, 0x0033])]),
                        Some(vec![Some(vec![0x0073, 0x0034])]),
                    ]),
                    popup_maximum_times: Some(vec![201, 202, 203, 204, 205]),
                };
                let result = ink_engine_popup_create(
                    &application,
                    &mut engine,
                    text.as_deref(),
                    recovery_code,
                    |forwarded_text, width| {
                        if width != 112 {
                            return Err(orphan_jvm::NullPointerException);
                        }
                        let forwarded_text =
                            forwarded_text.ok_or(orphan_jvm::NullPointerException)?;
                        Ok(Some(vec![Some(forwarded_text.to_vec())]))
                    },
                    || panic!("minus-one popup timeout must not read the clock"),
                );
                let recovery_codes = engine
                    .popup_recovery_codes
                    .as_deref()
                    .expect("seeded recovery array remains present");
                let maximum_times = engine
                    .popup_maximum_times
                    .as_deref()
                    .expect("seeded maximum-time array remains present");
                let popup_texts = engine
                    .popup_texts
                    .as_deref()
                    .expect("seeded text array remains present");
                let first_text = popup_texts[target]
                    .as_deref()
                    .and_then(|lines| lines.first())
                    .and_then(Option::as_deref);
                format!(
                    "{}:{}:{}:{}:{}:{}:{}:{}:{}",
                    if result.is_ok() { "OK" } else { "NPE" },
                    engine.popup_number,
                    i32::from(engine.popup_active),
                    engine.popup_current,
                    engine.popup_choice,
                    engine.popup_end_time,
                    recovery_codes[target],
                    maximum_times[target],
                    utf16_output(first_text)
                )
            }
            Some("popup-create-max") if parts.len() == 10 => {
                let text = utf16(parts[1]);
                let recovery_code = value(&parts, 2);
                let maximum_time = value(&parts, 3);
                let initial_popup_number = value(&parts, 4);
                let initial_popup_active = value(&parts, 5) != 0;
                let text_length = value(&parts, 6);
                let recovery_length = value(&parts, 7);
                let maximum_length = value(&parts, 8);
                let canvas_width = value(&parts, 9);
                let mut application = server_state("-", "-", false);
                application.canvas_width = canvas_width;
                let popup_texts = (text_length >= 0).then(|| {
                    (0..text_length)
                        .map(|index| Some(vec![Some(vec![0x0073, 0x0030 + index as u16])]))
                        .collect()
                });
                let popup_recovery_codes = (recovery_length >= 0)
                    .then(|| (0..recovery_length).map(|index| 101 + index).collect());
                let popup_maximum_times = (maximum_length >= 0)
                    .then(|| (0..maximum_length).map(|index| 201 + index).collect());
                let mut engine = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 77,
                    popup_minimum_time_ends: 66,
                    popup_current: 2,
                    popup_number: initial_popup_number,
                    popup_active: initial_popup_active,
                    popup_choice: 9,
                    popup_recovery_codes,
                    popup_texts,
                    popup_maximum_times,
                };
                let expected_width = orphan_jvm::i32_sub(orphan_jvm::i32_sub(canvas_width, 8), 8);
                let mut clock_calls = 0;
                let result = ink_engine_popup_create_with_max_time(
                    &application,
                    &mut engine,
                    text.as_deref(),
                    recovery_code,
                    maximum_time,
                    |forwarded_text, width| {
                        if width != expected_width {
                            return Err(orphan_jvm::NullPointerException);
                        }
                        let forwarded_text =
                            forwarded_text.ok_or(orphan_jvm::NullPointerException)?;
                        Ok(Some(vec![Some(forwarded_text.to_vec())]))
                    },
                    || {
                        clock_calls += 1;
                        1_000_000
                    },
                );
                let status = match &result {
                    Ok(()) => "OK".to_owned(),
                    Err(InkEnginePopupCreateError::WrapString(_)) => "NPE".to_owned(),
                    Err(InkEnginePopupCreateError::ArrayAccess(
                        orphan_jvm::ArrayAccessException::NullPointer(_),
                    )) => "NPE".to_owned(),
                    Err(InkEnginePopupCreateError::ArrayAccess(
                        orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error),
                    )) => format!("AIOOBE,{},{}", error.index, error.length),
                };
                let timed = result.is_ok()
                    && initial_popup_number < 4
                    && !initial_popup_active
                    && maximum_time != -1;
                let end_time = if timed {
                    "T".to_owned()
                } else {
                    engine.popup_end_time.to_string()
                };
                format!(
                    "{status}:{}:{}:{}:{}:{end_time}:{}:{}:{}:{clock_calls}",
                    engine.popup_number,
                    i32::from(engine.popup_active),
                    engine.popup_current,
                    engine.popup_choice,
                    popup_texts_output(engine.popup_texts.as_deref()),
                    ints_output(engine.popup_recovery_codes.as_deref()),
                    ints_output(engine.popup_maximum_times.as_deref())
                )
            }
            Some("popup-set-next") if parts.len() == 5 => {
                let initial_popup_current = value(&parts, 1);
                let initial_popup_number = value(&parts, 2);
                let initial_popup_active = value(&parts, 3) != 0;
                let mut engine = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 77,
                    popup_minimum_time_ends: 66,
                    popup_current: initial_popup_current,
                    popup_number: initial_popup_number,
                    popup_active: initial_popup_active,
                    popup_choice: 9,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: ints(parts[4]),
                };
                let mut clock_calls = 0;
                let result = ink_engine_popup_set_next(&mut engine, || {
                    clock_calls += 1;
                    1_000_000 + i64::from(clock_calls)
                });
                let status = match &result {
                    Ok(()) => "OK".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!("AIOOBE,{},{}", error.index, error.length)
                    }
                };
                let continued = engine.popup_current < initial_popup_number;
                let minimum_time = if continued && engine.popup_minimum_time_ends != 66 {
                    "T".to_owned()
                } else {
                    engine.popup_minimum_time_ends.to_string()
                };
                let timed = result.is_ok()
                    && continued
                    && engine
                        .popup_maximum_times
                        .as_deref()
                        .and_then(|times| {
                            usize::try_from(engine.popup_current)
                                .ok()
                                .and_then(|index| times.get(index))
                        })
                        .is_some_and(|maximum_time| *maximum_time != -1);
                let end_time = if timed && engine.popup_end_time != 77 {
                    "T".to_owned()
                } else {
                    engine.popup_end_time.to_string()
                };
                format!(
                    "{status}:{}:{}:{}:{minimum_time}:{end_time}:{}:{clock_calls}",
                    engine.popup_current,
                    engine.popup_number,
                    i32::from(engine.popup_active),
                    ints_output(engine.popup_maximum_times.as_deref())
                )
            }
            Some("default-constructor") if parts.len() == 2 => {
                let mut attempts = 0;
                let result: Result<(), orphan_jvm::NullPointerException> = match parts[1] {
                    "cheat" => cheat_controller_new(|| {
                        attempts += 1;
                        Ok(())
                    }),
                    "game" => silent_hill_game_new(|| {
                        attempts += 1;
                        Ok(())
                    }),
                    "engine" => ink_engine_new(|| {
                        attempts += 1;
                        Ok(())
                    }),
                    "application" => application_new(|| {
                        attempts += 1;
                        Ok(())
                    }),
                    "codes" => ink_codes_new(|| {
                        attempts += 1;
                        Ok(())
                    }),
                    "text-id" => text_id_new(|| {
                        attempts += 1;
                        Ok(())
                    }),
                    _ => unreachable!("request generator emits reviewed constructor names"),
                };
                format!("{}:{attempts}", if result.is_ok() { "OK" } else { "NPE" })
            }
            Some("game-canvas-new") if parts.len() == 3 => {
                let super_fails = value(&parts, 1) != 0;
                let full_screen_fails = value(&parts, 2) != 0;
                let super_calls = core::cell::Cell::new(0);
                let full_screen_calls = core::cell::Cell::new(0);
                let suppress_keys = core::cell::Cell::new(true);
                let full_screen_mode = core::cell::Cell::new(false);
                let constructor_receiver = core::cell::Cell::new(None);
                let full_screen_receiver = core::cell::Cell::new(None);
                let status = if game_canvas_new(
                    41,
                    |canvas, suppress| {
                        super_calls.set(super_calls.get() + 1);
                        constructor_receiver.set(Some(canvas));
                        suppress_keys.set(suppress);
                        if super_fails {
                            Err("super")
                        } else {
                            Ok(())
                        }
                    },
                    |canvas, full_screen| {
                        full_screen_calls.set(full_screen_calls.get() + 1);
                        full_screen_receiver.set(Some(canvas));
                        full_screen_mode.set(full_screen);
                        if full_screen_fails {
                            Err("full-screen")
                        } else {
                            Ok(())
                        }
                    },
                )
                .is_ok()
                {
                    "OK"
                } else {
                    "NPE"
                };
                let same_receiver = if full_screen_calls.get() == 0 {
                    "-"
                } else if full_screen_receiver.get() == constructor_receiver.get() {
                    "1"
                } else {
                    "0"
                };
                format!(
                    "{status}:{}:{}:{}:{}:{same_receiver}",
                    super_calls.get(),
                    i32::from(suppress_keys.get()),
                    full_screen_calls.get(),
                    i32::from(full_screen_mode.get())
                )
            }
            Some("menu-reset-ingame-values") if parts.len() == 3 => {
                let mut statics = SilentHillGameStatics {
                    hud_ammo_number_width: value(&parts, 1),
                    hud_ammo_update_needed: value(&parts, 2) != 0,
                    ink_menu_logo: None,
                };
                let mut ingame_margin = i32::MIN;
                silent_hill_game_menu_reset_ingame_values(&mut statics, || {
                    ingame_margin = 4;
                    Ok::<(), orphan_jvm::NullPointerException>(())
                })
                .expect("reviewed InkEngine reset callback succeeds");
                format!(
                    "{ingame_margin}:{}:{}",
                    statics.hud_ammo_number_width,
                    i32::from(statics.hud_ammo_update_needed)
                )
            }
            Some("app-init") if parts.len() == 2 => {
                let old_logo = (value(&parts, 1) != 0).then_some(7);
                let mut statics = SilentHillGameStatics {
                    hud_ammo_number_width: 0,
                    hud_ammo_update_needed: false,
                    ink_menu_logo: old_logo,
                };
                let mut load_calls = 0;
                let mut requested_path = String::from("-");
                let mut canvas_width = i32::MIN;
                let result = silent_hill_game_app_init(
                    &mut statics,
                    |path| {
                        load_calls += 1;
                        requested_path = String::from_utf16(path)
                            .expect("reviewed menu-logo path is valid UTF-16");
                        Some(41)
                    },
                    || {
                        canvas_width = 240;
                        Err(orphan_jvm::NullPointerException)
                    },
                );
                let logo = if statics.ink_menu_logo == Some(41) {
                    "NEW"
                } else if statics.ink_menu_logo == old_logo {
                    "OLD"
                } else {
                    "WRONG"
                };
                format!(
                    "{}:{load_calls}:{requested_path}:{logo}:{}",
                    if result.is_ok() { "OK" } else { "NPE" },
                    i32::from(canvas_width != i32::MIN)
                )
            }
            Some("key-jad-entry") if parts.len() == 5 => {
                let mut application = server_state("-", "-", false);
                application.midlet_instance = (value(&parts, 1) != 0).then_some(19);
                let key = utf16(parts[2]);
                let lookup_fails = value(&parts, 3) != 0;
                let property = utf16(parts[4]);
                let mut calls = 0;
                let mut receiver = "-";
                let mut key_identity = "-";
                let parsed = game_canvas_key_jad_entry_as_int(
                    &application,
                    key.as_deref(),
                    |midlet, observed_key| {
                        calls += 1;
                        receiver = if midlet == 19 { "M" } else { "W" };
                        key_identity = if observed_key == key.as_deref() {
                            "K"
                        } else {
                            "W"
                        };
                        if lookup_fails {
                            Err(orphan_jvm::NullPointerException)
                        } else {
                            Ok(property.clone())
                        }
                    },
                );
                format!("{parsed}:{calls}:{receiver}:{key_identity}")
            }
            Some("url-encode") if parts.len() == 2 => {
                let input = utf16(parts[1]);
                match resource_url_encode(input.as_deref()) {
                    Ok(encoded) => utf16_output(Some(&encoded)),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("coded-string") if parts.len() == 2 => {
                let input = bytes(parts[1]);
                match coded_string(input.as_deref()) {
                    Ok(encoded) => utf16_output(Some(&encoded)),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("print-array") if parts.len() == 2 => {
                let data = byte_arrays(parts[1]);
                let mut captured = Vec::new();
                let outcome = application_print_array(data.as_deref(), |data, from, to| {
                    assert_eq!((from, to), (0, data.len() as i32));
                    for (index, value) in data.iter().enumerate() {
                        let encoded = coded_string(value.as_deref())?;
                        let rendered = String::from_utf16(&encoded)
                            .expect("codedString emits valid Unicode for byte input");
                        captured.extend_from_slice(index.to_string().as_bytes());
                        captured.extend_from_slice(b": ");
                        captured.extend_from_slice(rendered.as_bytes());
                    }
                    captured.push(b'\n');
                    Ok::<(), orphan_jvm::NullPointerException>(())
                });
                let status = match outcome {
                    Ok(Ok(())) => "OK",
                    Ok(Err(_)) | Err(_) => "NPE",
                };
                format!("{status}:{}", bytes_output(&captured))
            }
            Some("room-repaint-run") if parts.len() == 3 => {
                let delegate_succeeds = value(&parts, 1) != 0;
                let expected_thread = (value(&parts, 2) != 0).then_some(71);
                let mut state = server_state("-", "-", false);
                state.room_repaint_thread = expected_thread;
                let mut room_repainting = false;
                let mut room_repaint_needed = true;
                let mut calls = 0;
                let status = if application_room_repaint_run(&mut state, |state| {
                    calls += 1;
                    assert_eq!(state.room_repaint_thread, expected_thread);
                    room_repainting = true;
                    if !delegate_succeeds {
                        return Err(());
                    }
                    room_repaint_needed = false;
                    room_repainting = false;
                    Ok(())
                })
                .is_ok()
                {
                    "OK"
                } else {
                    "NPE"
                };
                assert_eq!(calls, 1);
                let thread = match state.room_repaint_thread {
                    None => "NULL",
                    Some(71) => "OLD",
                    Some(_) => "WRONG",
                };
                format!(
                    "{status}:{thread}:{}:{}",
                    i32::from(room_repainting),
                    i32::from(room_repaint_needed)
                )
            }
            Some("clear-all-rms") if parts.len() == 3 => {
                let resource_succeeds = value(&parts, 1) != 0;
                let script_count = value(&parts, 2);
                let mut script_statics = InkScriptStatics {
                    scripts: (script_count >= 0).then(|| {
                        (0..script_count)
                            .map(|index| {
                                (
                                    vec![index as u16],
                                    InkScriptRegistryValue::Script(index as u32),
                                )
                            })
                            .collect()
                    }),
                    wait_stop: 0,
                    item_id: None,
                };
                let mut heap_source_length = 7;
                let mut subchunk_byte = Some(73);
                let mut subchunk_size = 91;
                let mut important_count = 2;
                let mut downloads_count = Some(1);
                let status = if application_clear_all_rms(&mut script_statics, || {
                    heap_source_length = 1;
                    if !resource_succeeds {
                        subchunk_byte = None;
                        return Err(());
                    }
                    subchunk_byte = Some(0);
                    subchunk_size = 1;
                    important_count = 0;
                    downloads_count = None;
                    Ok(())
                })
                .is_ok()
                {
                    "OK"
                } else {
                    "NPE"
                };
                let subchunk =
                    subchunk_byte.map_or_else(|| "NULL".to_owned(), |value| value.to_string());
                let downloads =
                    downloads_count.map_or_else(|| "NULL".to_owned(), |count| count.to_string());
                let scripts = script_statics
                    .scripts
                    .as_ref()
                    .map_or_else(|| "NULL".to_owned(), |values| values.len().to_string());
                format!(
                    "{status}:{heap_source_length}:{subchunk}:{subchunk_size}:\
                     {important_count}:{downloads}:{scripts}"
                )
            }
            Some("free-memory") if parts.len() == 2 => {
                let runtime_present = value(&parts, 1) != 0;
                let mut state = server_state("-", "-", false);
                state.runtime_instance = runtime_present.then_some(37);
                let collect_calls = core::cell::Cell::new(0);
                let sample_calls = core::cell::Cell::new(0);
                match application_free_memory(
                    &state,
                    || collect_calls.set(collect_calls.get() + 1),
                    |runtime| {
                        assert_eq!(runtime, 37);
                        sample_calls.set(sample_calls.get() + 1);
                        4096
                    },
                ) {
                    Ok(available) => {
                        assert_eq!(collect_calls.get(), 1);
                        assert_eq!(sample_calls.get(), 1);
                        format!("OK:{}", i32::from(available >= 0))
                    }
                    Err(_) => {
                        assert_eq!(collect_calls.get(), 1);
                        assert_eq!(sample_calls.get(), 0);
                        "NPE:-".to_owned()
                    }
                }
            }
            Some("set-display") if parts.len() == 5 => {
                let expected_midlet = (value(&parts, 1) != 0).then_some(11);
                let expected_current = (value(&parts, 2) != 0).then_some(29);
                let get_mode = value(&parts, 3);
                let set_mode = value(&parts, 4);
                let mut state = server_state("-", "-", false);
                state.midlet_instance = expected_midlet;
                let get_calls = core::cell::Cell::new(0);
                let set_calls = core::cell::Cell::new(0);
                let observed_midlet = core::cell::Cell::new(None);
                let observed_receiver = core::cell::Cell::new(None);
                let observed_current = core::cell::Cell::new(None);
                let status = if application_set_display(
                    &state,
                    expected_current,
                    |midlet| {
                        get_calls.set(get_calls.get() + 1);
                        observed_midlet.set(Some(midlet));
                        match get_mode {
                            0 => Ok(Some(17)),
                            1 => Ok(None),
                            2 => Err("get"),
                            _ => unreachable!(),
                        }
                    },
                    |display, current| {
                        set_calls.set(set_calls.get() + 1);
                        observed_receiver.set(Some(display));
                        observed_current.set(Some(current));
                        if set_mode == 0 {
                            Ok(())
                        } else {
                            Err("set")
                        }
                    },
                )
                .is_ok()
                {
                    "OK"
                } else {
                    "NPE"
                };
                let midlet = match observed_midlet.get() {
                    Some(None) => "N",
                    Some(value) if value == expected_midlet => "M",
                    _ => "W",
                };
                let receiver = match observed_receiver.get() {
                    None => "-",
                    Some(17) => "D",
                    Some(_) => "W",
                };
                let current = match observed_current.get() {
                    None => "-",
                    Some(None) => "N",
                    Some(value) if value == expected_current => "C",
                    _ => "W",
                };
                format!(
                    "{status}:{}:{}:{midlet}:{receiver}:{current}",
                    get_calls.get(),
                    set_calls.get()
                )
            }
            Some("rms-delete") if parts.len() == 3 => {
                let name = utf16(parts[1]);
                let mode = value(&parts, 2);
                let calls = core::cell::Cell::new(0);
                let identity = core::cell::Cell::new("-");
                let result = application_rms_delete(name.as_deref(), |observed| {
                    calls.set(calls.get() + 1);
                    identity.set(match (observed, name.as_deref()) {
                        (None, None) => "I",
                        (Some(left), Some(right))
                            if left.as_ptr() == right.as_ptr() && left.len() == right.len() =>
                        {
                            "I"
                        }
                        _ => "W",
                    });
                    match mode {
                        0 => Ok(()),
                        1 => Err(ApplicationRmsDeleteError::NotFound),
                        2 => Err(ApplicationRmsDeleteError::RecordStore),
                        3 => Err(ApplicationRmsDeleteError::Uncaught(
                            orphan_jvm::NullPointerException,
                        )),
                        _ => unreachable!(),
                    }
                });
                let status = match result {
                    Ok(true) => "T",
                    Ok(false) => "F",
                    Err(_) => "NPE",
                };
                format!("{status}:{}:{}", calls.get(), identity.get())
            }
            Some("rms-get") if parts.len() == 6 => {
                let name = utf16(parts[1]);
                let open_mode = value(&parts, 2);
                let get_mode = value(&parts, 3);
                let close_mode = value(&parts, 4);
                let data = bytes(parts[5]);
                let expected_data_pointer = data.as_deref().map(<[u8]>::as_ptr);
                let expected_data_length = data.as_deref().map(<[u8]>::len);
                let open_calls = core::cell::Cell::new(0);
                let name_identity = core::cell::Cell::new("-");
                let open_create = core::cell::Cell::new(false);
                let get_calls = core::cell::Cell::new(0);
                let get_id = core::cell::Cell::new(None);
                let close_calls = core::cell::Cell::new(0);
                let result = application_rms_get(
                    name.as_deref(),
                    |observed_name, create| {
                        open_calls.set(open_calls.get() + 1);
                        name_identity.set(match (observed_name, name.as_deref()) {
                            (None, None) => "I",
                            (Some(left), Some(right))
                                if left.as_ptr() == right.as_ptr() && left.len() == right.len() =>
                            {
                                "I"
                            }
                            _ => "W",
                        });
                        open_create.set(create);
                        match open_mode {
                            0 => Ok(17_u32),
                            1 => Err(ApplicationRmsGetCallError::RecordStoreNotFound),
                            2 => Err(ApplicationRmsGetCallError::RecordStore),
                            3 => Err(ApplicationRmsGetCallError::Uncaught(())),
                            4 => Err(ApplicationRmsGetCallError::OtherException),
                            _ => unreachable!(),
                        }
                    },
                    |store, record_id| {
                        get_calls.set(get_calls.get() + 1);
                        get_id.set(Some(record_id));
                        assert_eq!(*store, 17);
                        match get_mode {
                            0 => Ok(data),
                            1 => Err(ApplicationRmsGetCallError::RecordStoreNotFound),
                            2 => Err(ApplicationRmsGetCallError::RecordStore),
                            3 => Err(ApplicationRmsGetCallError::Uncaught(())),
                            4 => Err(ApplicationRmsGetCallError::OtherException),
                            _ => unreachable!(),
                        }
                    },
                    |store| {
                        close_calls.set(close_calls.get() + 1);
                        assert_eq!(store, 17);
                        match close_mode {
                            0 => Ok(()),
                            1 => Err(ApplicationRmsGetCallError::RecordStoreNotFound),
                            2 => Err(ApplicationRmsGetCallError::RecordStore),
                            3 => Err(ApplicationRmsGetCallError::Uncaught(())),
                            4 => Err(ApplicationRmsGetCallError::OtherException),
                            _ => unreachable!(),
                        }
                    },
                );
                let (status, returned, data_identity) = match result {
                    Err(()) => ("ERR", "null".to_owned(), "-"),
                    Ok(returned) => {
                        let identity = if get_calls.get() == 1 && get_mode == 0 {
                            match (
                                returned.as_deref(),
                                expected_data_pointer,
                                expected_data_length,
                            ) {
                                (None, None, None) => "I",
                                (Some(value), Some(pointer), Some(length))
                                    if value.as_ptr() == pointer && value.len() == length =>
                                {
                                    "I"
                                }
                                _ => "W",
                            }
                        } else {
                            "-"
                        };
                        let output = returned
                            .as_deref()
                            .map_or_else(|| "null".to_owned(), bytes_output);
                        ("OK", output, identity)
                    }
                };
                let get_id = get_id
                    .get()
                    .map_or_else(|| "-".to_owned(), |record_id| record_id.to_string());
                format!(
                    "{status}:{returned}:{data_identity}:{}:{}:{}:{}:{get_id}:{}",
                    open_calls.get(),
                    name_identity.get(),
                    i32::from(open_create.get()),
                    get_calls.get(),
                    close_calls.get()
                )
            }
            Some("save-chunk-ini") if parts.len() == 3 => {
                let stream_mode = value(&parts, 1);
                let open_mode = value(&parts, 2);
                let input = (stream_mode != 0).then_some(41_u32);
                let mut open_calls = 0;
                let mut open_name = String::from("null");
                let mut open_create = false;
                let mut set_calls = 0;
                let mut set_data: Option<Vec<u8>> = None;
                let mut set_offset = 0;
                let mut set_length = 0;
                application_save_chunk_ini(
                    input,
                    |observed| {
                        assert_eq!(observed, input);
                        match stream_mode {
                            0 => Ok(None),
                            1 => Ok(Some(Vec::new())),
                            2 => Ok(Some(vec![0, 1, 255])),
                            3 | 4 => Err("read"),
                            _ => unreachable!(),
                        }
                    },
                    |name, data| {
                        let Some(data) = data else {
                            return Ok(false);
                        };
                        open_calls += 1;
                        open_name = utf16_output(Some(name));
                        open_create = true;
                        if open_mode != 0 {
                            return Ok(false);
                        }
                        set_calls += 1;
                        set_offset = 0;
                        set_length = data.len();
                        set_data = Some(data);
                        Ok::<_, &'static str>(true)
                    },
                );
                let data = set_data
                    .as_deref()
                    .map_or_else(|| "null".to_owned(), bytes_output);
                format!(
                    "{open_calls}:{open_name}:{}:{set_calls}:{data}:{set_offset}:{set_length}",
                    i32::from(open_create)
                )
            }
            Some("resource-make-subchunk") if parts.len() == 3 => {
                let source = bytes(parts[1]);
                let size = value(&parts, 2);
                let mut state = server_state("-", "-", false);
                state.resource_sc_data = source;
                state.resource_sc_current_size = size;
                let (status, returned, identity) = match application_resource_make_subchunk(&state)
                {
                    Ok(subchunk) => ("OK", bytes_output(&subchunk), "D"),
                    Err(ApplicationResourceMakeSubChunkError::NegativeArraySize(_)) => {
                        ("NASE", "null".to_owned(), "-")
                    }
                    Err(ApplicationResourceMakeSubChunkError::ArrayCopy(
                        orphan_jvm::ArrayCopyException::NullPointer,
                    )) => ("NPE", "null".to_owned(), "-"),
                    Err(ApplicationResourceMakeSubChunkError::ArrayCopy(
                        orphan_jvm::ArrayCopyException::IndexOutOfBounds,
                    )) => ("AIOOBE", "null".to_owned(), "-"),
                };
                let source_after = state
                    .resource_sc_data
                    .as_deref()
                    .map_or_else(|| "null".to_owned(), bytes_output);
                format!(
                    "{status}:{returned}:{identity}:{source_after}:{}",
                    state.resource_sc_current_size
                )
            }
            Some("resource-restart-importants") if parts.len() == 2 => {
                let old_length = value(&parts, 1);
                let mut state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: if old_length < 0 {
                        None
                    } else {
                        Some((0..old_length as u32).collect())
                    },
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                resource_restart_importants(&mut state);
                state
                    .resource_importants
                    .as_deref()
                    .expect("new vector")
                    .len()
                    .to_string()
            }
            Some("reset-load") if parts.len() == 4 => {
                let mut state = server_state("-", "-", false);
                state.load_thread = if value(&parts, 1) == 0 { None } else { Some(1) };
                state.loading_mode = value(&parts, 2);
                state.resources_to_download = choice_ids(parts[3]);
                let result = reset_load(&mut state);
                format!(
                    "{}:{}:{}:{}",
                    if result.is_ok() { "OK" } else { "NPE" },
                    i32::from(state.load_thread.is_some()),
                    state.loading_mode,
                    handle_array_output(state.resources_to_download.as_deref())
                )
            }
            Some("resource-path") if parts.len() == 6 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: utf16(parts[5]),
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let string_id = utf16(parts[3]);
                utf16_output(
                    load_request_resource_path(
                        &state,
                        value(&parts, 1),
                        value(&parts, 2),
                        string_id.as_deref(),
                        value(&parts, 4),
                    )
                    .as_deref(),
                )
            }
            Some("game-language-path") if parts.len() == 2 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: utf16(parts[1]),
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                utf16_output(Some(&game_language_path(&state)))
            }
            Some("game-text") if parts.len() == 3 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: script_ids(parts[1]),
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                match get_game_text(&state, value(&parts, 2)) {
                    Ok(text) => utf16_output(text.as_deref()),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("game-text-string") if parts.len() == 3 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: script_ids(parts[1]),
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let index = utf16(parts[2]);
                utf16_output(get_game_text_from_string(&state, index.as_deref()).as_deref())
            }
            Some("text-replace") if parts.len() == 4 => {
                let haystack = utf16(parts[1]);
                let needle = utf16(parts[2]);
                let replacement = utf16(parts[3]);
                match text_replace_first(
                    haystack.as_deref(),
                    needle.as_deref(),
                    replacement.as_deref(),
                ) {
                    Ok(text) => utf16_output(Some(&text)),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("remove-string-prefix") if parts.len() == 3 => {
                let strings = script_ids(parts[1]);
                let prefix = utf16(parts[2]);
                match remove_string_prefix(strings.as_deref(), prefix.as_deref()) {
                    Ok(strings) => script_ids_output(strings.as_deref()),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("language-position") if parts.len() == 3 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: script_ids(parts[1]),
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let language = utf16(parts[2]);
                match get_language_selection_position(&state, language.as_deref()) {
                    Ok(index) => index.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("resource-heap-index") if parts.len() == 3 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: ints(parts[2]),
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                match resource_heap_index(&state, value(&parts, 1)) {
                    Ok(index) => index.to_string(),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_)) => {
                        "AIOOBE".to_owned()
                    }
                }
            }
            Some("random-scaled") if parts.len() == 4 => {
                let has_random = parts[1] == "fixed";
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: has_random.then_some(1),
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                match random_scaled(&state, value(&parts, 2), value(&parts, 3)) {
                    Ok(value) => value.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("ink-get") if parts.len() == 4 => {
                let table = string_table(parts[2]);
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: if parts[1] == "variable" {
                        table.clone()
                    } else {
                        None
                    },
                    ink_server_hints: if parts[1] == "hint" { table } else { None },
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let name = utf16(parts[3]);
                let value = if parts[1] == "variable" {
                    ink_server_get_variable(&state, name.as_deref())
                } else {
                    ink_server_get_hint(&state, name.as_deref())
                };
                match value {
                    Ok(value) => utf16_output(value),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("ink-set") if parts.len() == 7 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let name = utf16(parts[4]);
                let new_value = utf16(parts[5]);
                let hint = utf16(parts[6]);
                let result = ink_server_set_variable(
                    &mut state,
                    name.as_deref(),
                    new_value.as_deref(),
                    hint.as_deref(),
                );
                mutation_output(result, &state)
            }
            Some("ink-unset") if parts.len() == 5 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let name = utf16(parts[4]);
                let result = ink_server_unset_variable(&mut state, name.as_deref());
                mutation_output(result, &state)
            }
            Some("reset-variables") if parts.len() == 4 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let result = reset_variable_system(&mut state);
                mutation_output(result, &state)
            }
            Some("room-set") if parts.len() == 5 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let room_id = utf16(parts[4]);
                let result = room_set_current(&mut state, room_id.as_deref());
                mutation_output(result, &state)
            }
            Some("room-add") if parts.len() == 5 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let room_id = utf16(parts[4]);
                let result = room_add_to_history(&mut state, room_id.as_deref());
                mutation_output(result, &state)
            }
            Some("room-remove") if parts.len() == 4 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let result = room_remove_last_from_history(&mut state);
                mutation_output(result, &state)
            }
            Some("inventory-set") if parts.len() == 6 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let item_id = utf16(parts[4]);
                let result = inventory_set(&mut state, item_id.as_deref(), value(&parts, 5));
                mutation_output(result, &state)
            }
            Some("inventory-remove") if parts.len() == 5 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let item_id = utf16(parts[4]);
                let result = inventory_remove(&mut state, item_id.as_deref());
                mutation_output(result, &state)
            }
            Some("script-set-variable") if parts.len() == 7 => {
                let mut state = server_state(parts[1], parts[2], value(&parts, 3) != 0);
                let variable_id = utf16(parts[4]);
                let string_value = if parts[5] == "string" {
                    utf16(parts[6])
                } else {
                    None
                };
                let variable_value = match parts[5] {
                    "null" => JavaObject::Null,
                    "integer" => JavaObject::Integer(value(&parts, 6)),
                    "string" => JavaObject::String(
                        string_value
                            .as_deref()
                            .expect("string Object payload must be nonnull"),
                    ),
                    "other" => JavaObject::Other,
                    _ => panic!("unknown script variable Object kind"),
                };
                let result =
                    ink_script_set_variable(&mut state, variable_id.as_deref(), variable_value);
                mutation_output(result, &state)
            }
            Some(command @ ("script-get-variable" | "script-get-variable-int"))
                if parts.len() == 4 =>
            {
                let state = server_state(parts[1], parts[2], false);
                let variable_id = utf16(parts[3]);
                if command == "script-get-variable" {
                    ink_variable_output(ink_script_get_variable(&state, variable_id.as_deref()))
                } else {
                    match ink_script_get_variable_as_integer(&state, variable_id.as_deref()) {
                        Ok(value) => value.to_string(),
                        Err(error) => ink_variable_error_output(error).to_owned(),
                    }
                }
            }
            Some("room-history-size") if parts.len() == 2 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: string_table(parts[1]),
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                match room_history_size(&state) {
                    Ok(size) => size.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("inventory-size") if parts.len() == 3 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: string_table(parts[1]),
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let item_id = utf16(parts[2]);
                match inventory_size(&state, item_id.as_deref()) {
                    Ok(size) => size.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("room-current") if parts.len() == 2 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: string_table(parts[1]),
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                match room_current(&state) {
                    Ok(room) => utf16_output(Some(&room)),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("room-last") if parts.len() == 2 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: string_table(parts[1]),
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                match room_last_in_history(&state) {
                    Ok(room) => utf16_output(room.as_deref()),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("resource-path-string") if parts.len() == 4 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: utf16(parts[3]),
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let resource_id = utf16(parts[2]);
                utf16_output(
                    load_request_resource_path_for_string(
                        &state,
                        value(&parts, 1),
                        resource_id.as_deref(),
                    )
                    .as_deref(),
                )
            }
            Some("resource-path-object") if parts.len() == 6 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: utf16(parts[5]),
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let string_id = utf16(parts[3]);
                let (integer_id, string_cast_succeeds) = match parts[1] {
                    "integer" => (Some(value(&parts, 2)), false),
                    "string" | "null" => (None, true),
                    "other" => (None, false),
                    _ => panic!("unknown resource object kind"),
                };
                match load_request_resource_path_for_object(
                    &state,
                    integer_id,
                    string_id.as_deref(),
                    string_cast_succeeds,
                    value(&parts, 4),
                ) {
                    Ok(path) => utf16_output(path.as_deref()),
                    Err(orphan_jvm::ClassCastException) => "CCE".to_owned(),
                }
            }
            Some("request-resource-path") if parts.len() == 6 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: utf16(parts[5]),
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                let request = ResourceRequestState {
                    resource_type: value(&parts, 1),
                    integer_id: value(&parts, 2),
                    string_id: utf16(parts[3]),
                    image_transform: value(&parts, 4),
                };
                let path = resource_request_resource_path(&state, &request);
                let rendered = resource_request_to_string(&state, &request);
                format!(
                    "{}:{}",
                    utf16_output(path.as_deref()),
                    utf16_output(rendered.as_deref())
                )
            }
            Some("request-new-string") if parts.len() == 3 => {
                let string_id = utf16(parts[2]);
                request_output(&resource_request_new_for_string(
                    value(&parts, 1),
                    string_id.as_deref(),
                ))
            }
            Some("request-new-object") if parts.len() == 5 => {
                let string_id = utf16(parts[3]);
                let (integer_id, string_cast_succeeds) = match parts[1] {
                    "integer" => (Some(value(&parts, 2)), false),
                    "string" | "null" => (None, true),
                    "other" => (None, false),
                    _ => panic!("unknown request object kind"),
                };
                match resource_request_new_for_object(
                    integer_id,
                    string_id.as_deref(),
                    string_cast_succeeds,
                    value(&parts, 4),
                ) {
                    Ok(request) => request_output(&request),
                    Err(orphan_jvm::ClassCastException) => "CCE".to_owned(),
                }
            }
            Some("game-resource-new") if parts.len() == 4 => {
                game_resource_output(&game_resource_new(
                    value(&parts, 1),
                    game_resource_id(parts[2]),
                    value(&parts, 3),
                ))
            }
            Some("game-resource-equals") if parts.len() == 8 => {
                let resource = game_resource_new(
                    value(&parts, 1),
                    game_resource_id(parts[2]),
                    value(&parts, 3),
                );
                let candidate_state = (parts[4] == "resource").then(|| {
                    game_resource_new(
                        value(&parts, 5),
                        game_resource_id(parts[6]),
                        value(&parts, 7),
                    )
                });
                match game_resource_equals(&resource, candidate_state.as_ref()) {
                    Ok(equal) => i32::from(equal).to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("game-resource-paint") if parts.len() == 12 => {
                let mut resource = game_resource_new(1, None, 0);
                resource.image = (value(&parts, 2) != 0).then_some(7);
                resource.image_width = value(&parts, 6);
                resource.image_height = value(&parts, 7);
                resource.image_registration_x = value(&parts, 8);
                resource.image_registration_y = value(&parts, 9);
                let transform_table: [i32; 8] = ints(parts[11])
                    .expect("paint transform table must be nonnull")
                    .try_into()
                    .expect("paint transform table must contain eight values");
                let canvas = GameCanvasState {
                    transform_table,
                    sound_id: None,
                    loop_count: 0,
                    key_softkey_left: 0,
                    key_softkey_right: 0,
                    key_send: 0,
                    key_return: 0,
                    key_softkey_center: 0,
                    key_arrow_up: 0,
                    key_arrow_down: 0,
                    key_arrow_left: 0,
                    key_arrow_right: 0,
                    key_erase: 0,
                };
                let graphics = (value(&parts, 1) != 0).then_some(9);
                let fail = value(&parts, 10) != 0;
                let attempt = std::cell::RefCell::new(None);
                let call = game_resource_paint(
                    &resource,
                    &canvas,
                    graphics,
                    value(&parts, 3),
                    value(&parts, 4),
                    value(&parts, 5),
                    |actual_graphics, image, left, top, anchor| {
                        attempt.replace(Some(format!(
                            "I:{actual_graphics}:{image}:{left}:{top}:{anchor}"
                        )));
                        if fail {
                            Err(orphan_jvm::ArrayAccessException::NullPointer(
                                orphan_jvm::NullPointerException,
                            ))
                        } else {
                            Ok(())
                        }
                    },
                    |actual_graphics,
                     image,
                     source_x,
                     source_y,
                     width,
                     height,
                     manipulation,
                     left,
                     top,
                     anchor| {
                        attempt.replace(Some(format!(
                            "R:{actual_graphics}:{image}:{source_x}:{source_y}:{width}:\
                             {height}:{manipulation}:{left}:{top}:{anchor}"
                        )));
                        if fail {
                            Err(orphan_jvm::ArrayAccessException::NullPointer(
                                orphan_jvm::NullPointerException,
                            ))
                        } else {
                            Ok(())
                        }
                    },
                );
                let status = match call {
                    Ok(()) => "OK".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!("AIOOBE:{}:{}", error.index, error.length)
                    }
                };
                format!("{status}:{}", attempt.borrow().as_deref().unwrap_or("null"))
            }
            Some("game-resource-paint-simple") if parts.len() == 7 => {
                let mut resource = game_resource_new(1, None, 0);
                resource.image = (value(&parts, 2) != 0).then_some(7);
                let graphics = (value(&parts, 1) != 0).then_some(9);
                let fail = value(&parts, 6) != 0;
                let mut attempt = None;
                let status = match game_resource_paint_simple(
                    &resource,
                    graphics,
                    value(&parts, 3),
                    value(&parts, 4),
                    value(&parts, 5),
                    |actual_graphics, image, x, y, anchor| {
                        attempt = Some(format!(
                            "{actual_graphics}:{}:{x}:{y}:{anchor}",
                            image.map_or_else(|| "n".to_owned(), |handle| handle.to_string())
                        ));
                        if fail {
                            Err(orphan_jvm::NullPointerException)
                        } else {
                            Ok(())
                        }
                    },
                ) {
                    Ok(()) => "OK",
                    Err(orphan_jvm::NullPointerException) => "NPE",
                };
                format!("{status}:{}", attempt.as_deref().unwrap_or("null"))
            }
            Some("read-string") if parts.len() == 2 => match bytes(parts[1]) {
                None => format!("{}:N", utf16_output(Some(&read_string(None)))),
                Some(data) => {
                    let mut input = Reader::new(&data);
                    let result = read_string(Some(&mut input));
                    format!("{}:{}", utf16_output(Some(&result)), input.remaining())
                }
            },
            Some("read-string-list") if parts.len() == 5 => {
                let mut state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: value(&parts, 3),
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: script_ids(parts[2]),
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: value(&parts, 4) != 0,
                };
                match bytes(parts[1]) {
                    None => {
                        let strings = read_string_list(&mut state, None);
                        format!(
                            "{}:{}:N",
                            script_ids_output(strings.as_deref()),
                            i32::from(state.save_is_possible)
                        )
                    }
                    Some(data) => {
                        let mut input = Reader::new(&data);
                        let strings = read_string_list(&mut state, Some(&mut input));
                        format!(
                            "{}:{}:{}",
                            script_ids_output(strings.as_deref()),
                            i32::from(state.save_is_possible),
                            input.remaining()
                        )
                    }
                }
            }
            Some("request-from-input") if parts.len() == 2 => match bytes(parts[1]) {
                None => {
                    let result = resource_request_create_from_input(None);
                    format!(
                        "{}:N",
                        result.as_ref().map_or("NULL".to_owned(), request_output)
                    )
                }
                Some(data) => {
                    let mut input = Reader::new(&data);
                    let result = resource_request_create_from_input(Some(&mut input));
                    format!(
                        "{}:{}",
                        result.as_ref().map_or("NULL".to_owned(), request_output),
                        input.remaining()
                    )
                }
            },
            Some("find") if parts.len() == 3 => {
                let target = utf16(parts[2]);
                match bytes(parts[1]) {
                    None => format!("{}:N", find(None, target.as_deref())),
                    Some(data) => {
                        let mut input = Reader::new(&data);
                        let result = find(Some(&mut input), target.as_deref());
                        format!("{result}:{}", input.remaining())
                    }
                }
            }
            Some("write-string") if parts.len() == 4 => {
                let string_value = utf16(parts[3]);
                if parts[1] == "null" {
                    write_string(|_| false, string_value.as_deref());
                    write_output("OK", None, None)
                } else {
                    let fail_at = value(&parts, 2);
                    let mut attempts = Vec::new();
                    let mut written = Vec::new();
                    write_string(
                        |byte| {
                            let attempt = attempts.len() as i32;
                            attempts.push(byte);
                            if attempt == fail_at {
                                false
                            } else {
                                written.push(byte as u8);
                                true
                            }
                        },
                        string_value.as_deref(),
                    );
                    write_output("OK", Some(&attempts), Some(&written))
                }
            }
            Some("request-get-id") if parts.len() == 5 => {
                let request = ResourceRequestState {
                    resource_type: value(&parts, 1),
                    integer_id: value(&parts, 2),
                    string_id: utf16(parts[3]),
                    image_transform: value(&parts, 4),
                };
                request_id_output(resource_request_get_id(&request))
            }
            Some("request-equals") if parts.len() == 10 => {
                let request = ResourceRequestState {
                    resource_type: value(&parts, 1),
                    integer_id: value(&parts, 2),
                    string_id: utf16(parts[3]),
                    image_transform: value(&parts, 4),
                };
                let candidate_state = (parts[5] == "request").then(|| ResourceRequestState {
                    resource_type: value(&parts, 6),
                    integer_id: value(&parts, 7),
                    string_id: utf16(parts[8]),
                    image_transform: value(&parts, 9),
                });
                let candidate = if parts[5] == "same" {
                    Some(&request)
                } else {
                    candidate_state.as_ref()
                };
                if resource_request_equals(&request, candidate) {
                    "1".to_owned()
                } else {
                    "0".to_owned()
                }
            }
            Some("description") if parts.len() == 1 => {
                if resource_request_description().is_none() {
                    "NULL".to_owned()
                } else {
                    "NONNULL".to_owned()
                }
            }
            Some("char") if parts.len() == 2 => {
                char_to_string(value(&parts, 1) as u16)[0].to_string()
            }
            Some("cmp") if parts.len() == 3 => {
                let left = bytes(parts[1]);
                let right = bytes(parts[2]);
                match resource_merge_sort_cmp(left.as_deref(), right.as_deref()) {
                    Ok(result) => if result { "1" } else { "0" }.to_owned(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("array-copy-string") if parts.len() == 7 => {
                let source = choice_ids(parts[1]);
                let mut target = choice_ids(parts[3]);
                let status = match array_copy_string_handles(
                    source.as_deref(),
                    value(&parts, 2),
                    target.as_deref_mut(),
                    value(&parts, 4),
                    value(&parts, 5),
                    parts[6] == "1",
                ) {
                    Ok(()) => "OK",
                    Err(orphan_jvm::ArrayCopyException::NullPointer) => "NPE",
                    Err(orphan_jvm::ArrayCopyException::IndexOutOfBounds) => "IOOBE",
                };
                format!("{}:{}", status, handle_array_output(target.as_deref()))
            }
            Some("object-convert") if parts.len() == 4 => {
                let string = utf16(parts[3]);
                let object = match parts[1] {
                    "null" => JavaObject::Null,
                    "integer" => JavaObject::Integer(value(&parts, 2)),
                    "string" => {
                        JavaObject::String(string.as_deref().expect("string token must be nonnull"))
                    }
                    "other" => JavaObject::Other,
                    _ => panic!("unknown Java object kind"),
                };
                format!(
                    "{}:{}:{}",
                    to_int(object),
                    if to_boolean(object) { 1 } else { 0 },
                    ink_interpreter_integer_argument(object)
                )
            }
            Some("action") if parts.len() == 2 => {
                let id = utf16(parts[1]);
                match action_key_id_convert(id.as_deref()) {
                    Ok(result) => result.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("tick-get") if parts.len() == 2 => {
                let state = ApplicationState {
                    tick_based_time_value: value(&parts, 1),
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                tick_based_time(&state).to_string()
            }
            Some("tick-update") if parts.len() == 2 => {
                let mut state = ApplicationState {
                    tick_based_time_value: value(&parts, 1),
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                tick_based_time_update(&mut state);
                state.tick_based_time_value.to_string()
            }
            Some("tick-reset") if parts.len() == 2 => {
                let mut state = ApplicationState {
                    tick_based_time_value: value(&parts, 1),
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: None,
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                tick_based_time_reset(&mut state);
                state.tick_based_time_value.to_string()
            }
            Some("loading") if parts.len() == 2 => {
                let state = ApplicationState {
                    tick_based_time_value: 0,
                    canvas_width: 0,
                    fade_frames: 0,
                    demo_frames: 0,
                    painting: false,
                    cur_sound_mode: false,
                    canvas_instance: None,
                    key_last_pressed: 0,
                    key_new: false,
                    key_pressed: false,
                    load_bar_active: false,
                    goto_dissolve_fx_counter: -6,
                    loading_mode: -1,
                    load_thread: if value(&parts, 1) == 0 { None } else { Some(1) },
                    room_repaint_thread: None,
                    resource_importants: None,
                    resources_to_download: None,
                    game_id: None,
                    game_texts: None,
                    languages: None,
                    resource_heap_sources: None,
                    resource_sc_data: None,
                    resource_sc_current_size: 0,
                    random_instance: None,
                    runtime_instance: None,
                    midlet_instance: None,
                    ink_server_variables: None,
                    ink_server_hints: None,
                    game_changed_since_last_save: false,
                    save_is_possible: false,
                };
                i32::from(loading(&state)).to_string()
            }
            Some("scroll") if parts.len() == 2 => {
                let mut state = InkEngineState {
                    menu_scroll_tick_counter: value(&parts, 1) as i8,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                let allowed = is_menu_scroll_allowed(&mut state);
                format!("{}:{}", i32::from(allowed), state.menu_scroll_tick_counter)
            }
            Some("action-code") if parts.len() == 3 => {
                let state = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: ints(parts[2]),
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                match action_key_keycode_to_action_key(&state, value(&parts, 1)) {
                    Ok(result) => result.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("action-unset") if parts.len() == 2 => {
                let old_length = value(&parts, 1);
                let mut state = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: if old_length < 0 {
                        None
                    } else {
                        Some(vec![Some(vec![0x6f, 0x6c, 0x64]); old_length as usize])
                    },
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                action_key_unset_all_keys(&mut state);
                let values = state.action_key_script_ids.as_deref().expect("new array");
                format!(
                    "{}:{}",
                    values.len(),
                    values.iter().filter(|value| value.is_none()).count()
                )
            }
            Some("action-init") if parts.len() == 3 => {
                let mut state = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: ints(parts[1]),
                    action_key_script_ids: script_ids(parts[2]),
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                action_key_init_system(&mut state);
                let script_ids = state.action_key_script_ids.as_deref().expect("new array");
                format!(
                    "{}:{}:{}",
                    ints_output(state.action_key_key_codes.as_deref()),
                    script_ids.len(),
                    script_ids.iter().filter(|value| value.is_none()).count()
                )
            }
            Some("action-script") if parts.len() == 4 => {
                let state = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: ints(parts[2]),
                    action_key_script_ids: script_ids(parts[3]),
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                match action_key_get_script_id(&state, value(&parts, 1)) {
                    Ok(result) => utf16_output(result),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!("AIOOBE:{}:{}", error.index, error.length)
                    }
                }
            }
            Some("splash-more") if parts.len() == 3 => {
                let state = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: value(&parts, 1),
                    number_of_splashes: value(&parts, 2),
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                i32::from(splash_more_exists(&state)).to_string()
            }
            Some("key-init") if parts.len() == 12 => {
                let engine = InkEngineState {
                    menu_scroll_tick_counter: 0,
                    settings_hash: settings_table(parts[1]),
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                let mut canvas = game_canvas_state(&parts, 2);
                let status = if key_init(&engine, &mut canvas).is_ok() {
                    "OK"
                } else {
                    "NPE"
                };
                key_bindings_output(status, &canvas)
            }
            Some("key-convert") if parts.len() == 12 => {
                let state = game_canvas_state(&parts, 2);
                key_convert_to_key_id(&state, value(&parts, 1)).to_string()
            }
            Some("set-key-status") if parts.len() == 7 => {
                let mut application = server_state("-", "-", false);
                application.key_new = value(&parts, 1) != 0;
                application.key_pressed = value(&parts, 2) != 0;
                application.key_last_pressed = value(&parts, 3);
                let mut engine = InkEngineState {
                    menu_scroll_tick_counter: value(&parts, 4) as i8,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                set_key_status(
                    &mut application,
                    &mut engine,
                    value(&parts, 5),
                    value(&parts, 6) != 0,
                );
                key_state_output(&application, &engine)
            }
            Some(command @ ("key-pressed" | "key-released")) if parts.len() == 19 => {
                let mut application = server_state("-", "-", false);
                application.loading_mode = value(&parts, 1);
                application.load_bar_active = value(&parts, 2) != 0;
                application.goto_dissolve_fx_counter = value(&parts, 3);
                application.key_new = value(&parts, 4) != 0;
                application.key_pressed = value(&parts, 5) != 0;
                application.key_last_pressed = value(&parts, 6);
                let mut engine = InkEngineState {
                    menu_scroll_tick_counter: value(&parts, 7) as i8,
                    settings_hash: None,
                    action_key_key_codes: None,
                    action_key_script_ids: None,
                    current_splash: 0,
                    number_of_splashes: 0,
                    popup_end_time: 0,
                    popup_minimum_time_ends: 0,
                    popup_current: 0,
                    popup_number: 0,
                    popup_active: false,
                    popup_choice: 0,
                    popup_recovery_codes: None,
                    popup_texts: None,
                    popup_maximum_times: None,
                };
                let canvas = game_canvas_state(&parts, 9);
                if command == "key-pressed" {
                    game_canvas_key_pressed(
                        &mut application,
                        &mut engine,
                        &canvas,
                        value(&parts, 8),
                    );
                } else {
                    game_canvas_key_released(
                        &mut application,
                        &mut engine,
                        &canvas,
                        value(&parts, 8),
                    );
                }
                key_state_output(&application, &engine)
            }
            Some("repaint-canvas-if-possible") if parts.len() == 5 => {
                let repaint_mode = value(&parts, 3);
                let service_mode = value(&parts, 4);
                let mut application = server_state("-", "-", false);
                application.painting = value(&parts, 1) != 0;
                application.canvas_instance = match parts[2] {
                    "null" => None,
                    token => Some(token.parse().expect("canvas token must be u32")),
                };
                let calls = std::cell::RefCell::new(Vec::new());
                let result = application_repaint_canvas_if_possible(
                    &mut application,
                    |canvas, state| {
                        calls.borrow_mut().push(format!("R{canvas}"));
                        match repaint_mode {
                            0 => Ok(()),
                            1 => Err("repaint"),
                            2 => {
                                state.canvas_instance = None;
                                Ok(())
                            }
                            3 => {
                                state.canvas_instance = Some(2);
                                Ok(())
                            }
                            4 => {
                                state.painting = false;
                                Ok(())
                            }
                            5 => {
                                state.canvas_instance = Some(2);
                                state.painting = false;
                                Ok(())
                            }
                            6 => {
                                let nested = application_repaint_canvas_if_possible(
                                    state,
                                    |nested_canvas, _| {
                                        calls.borrow_mut().push(format!("NR{nested_canvas}"));
                                        Err::<(), _>("nested-repaint")
                                    },
                                    |nested_canvas, _| {
                                        calls.borrow_mut().push(format!("NS{nested_canvas}"));
                                        Err::<(), _>("nested-service")
                                    },
                                );
                                if nested.is_ok() {
                                    Ok(())
                                } else {
                                    Err("recursive")
                                }
                            }
                            _ => unreachable!(),
                        }
                    },
                    |canvas, state| {
                        calls.borrow_mut().push(format!("S{canvas}"));
                        match service_mode {
                            0 => Ok(()),
                            1 => Err("service"),
                            2 => {
                                state.canvas_instance = None;
                                Ok(())
                            }
                            3 => {
                                state.canvas_instance = Some(3);
                                Ok(())
                            }
                            4 => {
                                state.painting = false;
                                Ok(())
                            }
                            _ => unreachable!(),
                        }
                    },
                );
                let status = match result {
                    Ok(()) => "OK",
                    Err(ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeRepaint) => {
                        "NPE-R"
                    }
                    Err(ApplicationRepaintCanvasIfPossibleError::Repaint(_)) => "REPAINT",
                    Err(
                        ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeServiceRepaints,
                    ) => "NPE-S",
                    Err(ApplicationRepaintCanvasIfPossibleError::ServiceRepaints(_)) => "SERVICE",
                };
                let calls = calls.borrow();
                let trace = if calls.is_empty() {
                    "-".to_owned()
                } else {
                    calls.join(",")
                };
                let canvas = application
                    .canvas_instance
                    .map_or_else(|| "null".to_owned(), |canvas| canvas.to_string());
                format!(
                    "{status}:{trace}:{}:{canvas}",
                    i32::from(application.painting)
                )
            }
            Some("canvas-paint") if parts.len() == 4 => {
                let argument = (value(&parts, 1) != 0).then_some(73_u32);
                let delegate_enabled = value(&parts, 2) != 0;
                let mut application = server_state("-", "-", false);
                application.fade_frames = if delegate_enabled { 0 } else { 1 };
                application.demo_frames = 0;
                let mut captured_graphics = Some(41_u32);
                let mut painting = value(&parts, 3) != 0;
                let mut loading_bar_marker_x = 73;
                let mut application_calls = 0;
                let mut engine_calls = 0;
                let result = game_canvas_paint(argument, |graphics| {
                    application_calls += 1;
                    application_paint(&application, graphics, |graphics| {
                        engine_calls += 1;
                        captured_graphics = graphics;
                        if graphics.is_none() {
                            return Err(orphan_jvm::NullPointerException);
                        }
                        loading_bar_marker_x = -20;
                        painting = false;
                        Ok(())
                    })
                });
                assert_eq!(application_calls, 1);
                assert_eq!(engine_calls, usize::from(delegate_enabled));
                let status = if result.is_ok() { "OK" } else { "NPE" };
                let graphics_state = if captured_graphics == argument {
                    if argument.is_none() {
                        "NULL"
                    } else {
                        "ARG"
                    }
                } else if captured_graphics == Some(41) {
                    "PREVIOUS"
                } else {
                    "WRONG"
                };
                format!(
                    "{status}:{graphics_state}:{}:{loading_bar_marker_x}",
                    i32::from(painting)
                )
            }
            Some("canvas-show-notify") if parts.len() == 4 => {
                let hidden = std::cell::Cell::new(value(&parts, 1) != 0);
                let sound_enabled = value(&parts, 2) != 0;
                let loop_count = std::cell::Cell::new(value(&parts, 3));
                let phase = std::cell::Cell::new(0);
                let status = if game_canvas_show_notify(
                    |hide| {
                        assert_eq!(phase.get(), 0);
                        assert!(!hide);
                        hidden.set(hide);
                        phase.set(1);
                        Ok::<(), ()>(())
                    },
                    || {
                        assert_eq!(phase.get(), 1);
                        phase.set(2);
                        if sound_enabled && loop_count.get() == -1 {
                            loop_count.set(0);
                        }
                        Ok::<(), ()>(())
                    },
                )
                .is_ok()
                {
                    "OK"
                } else {
                    "NPE"
                };
                assert_eq!(phase.get(), 2);
                format!("{status}:{}:{}", i32::from(hidden.get()), loop_count.get())
            }
            Some("canvas-resume-sound") if parts.len() == 5 => {
                let mut application = server_state("-", "-", false);
                application.cur_sound_mode = value(&parts, 1) != 0;
                let initial_sound_id = utf16(parts[4]);
                let canvas = GameCanvasState {
                    transform_table: GAME_CANVAS_INITIAL_TRANSFORM_TABLE,
                    sound_id: initial_sound_id.clone(),
                    loop_count: value(&parts, 2),
                    key_softkey_left: 0,
                    key_softkey_right: 0,
                    key_send: 0,
                    key_return: 0,
                    key_softkey_center: 0,
                    key_arrow_up: 0,
                    key_arrow_down: 0,
                    key_arrow_left: 0,
                    key_arrow_right: 0,
                    key_erase: 0,
                };
                let final_sound_id = std::cell::RefCell::new(initial_sound_id);
                let final_loop_count = std::cell::Cell::new(canvas.loop_count);
                let first_load = std::cell::Cell::new(value(&parts, 3) != 0);

                // resumeSound lends the current ID to playSound. The callee can
                // nevertheless replace the Java static that held that ID, so
                // its final static observations live separately from the
                // wrapper's immutable input borrow in this oracle adapter.
                let result: Result<(), orphan_jvm::NullPointerException> =
                    game_canvas_resume_sound(&application, &canvas, |sound_id, loop_count| {
                        assert_eq!(loop_count, canvas.loop_count);
                        if first_load.get() {
                            let sound_id = sound_id.ok_or(orphan_jvm::NullPointerException)?;
                            if sound_id == [u16::from(b'l'), u16::from(b's')] {
                                first_load.set(false);
                                return Ok(());
                            }
                        }

                        // playSound publishes these two stores before any of
                        // its internally caught ID/resource/player failures.
                        final_sound_id.replace(None);
                        final_loop_count.set(0);
                        Ok(())
                    });
                let status = if result.is_ok() { "OK" } else { "NPE" };
                let sound_id = utf16_output(final_sound_id.borrow().as_deref());
                format!(
                    "{status}:{}:{}:{sound_id}:{}",
                    i32::from(application.cur_sound_mode),
                    final_loop_count.get(),
                    i32::from(first_load.get())
                )
            }
            Some("wrap-default") if parts.len() == 4 => {
                let text = utf16(parts[1]);
                let maximum_length = value(&parts, 2);
                let current_font = (value(&parts, 3) != 0).then_some(29_u32);
                let expected_text = text.clone();
                let wrapped = ink_engine_wrap_string(
                    text,
                    maximum_length,
                    current_font,
                    |text, forwarded_maximum_length, font| {
                        assert_eq!(text, expected_text);
                        assert_eq!(forwarded_maximum_length, maximum_length);
                        assert_eq!(font, current_font);
                        let Some(mut text) = text else {
                            return Err(());
                        };
                        if font.is_none() {
                            return Err(());
                        }
                        for unit in &mut text {
                            if *unit == u16::from(b'+') {
                                *unit = u16::from(b' ');
                            }
                        }
                        Ok(vec![Some(text)])
                    },
                );
                match wrapped {
                    Ok(lines) => script_ids_output(Some(&lines)),
                    Err(()) => "NPE".to_owned(),
                }
            }
            Some("menu-choice") if parts.len() == 2 => {
                let state = MenuState {
                    is_current: false,
                    selected_choice_number: value(&parts, 1),
                    x: 0,
                    y: 0,
                    scroll: 0,
                    text_scrolling: false,
                    update_menu: false,
                    top_text: None,
                    update_top_lines: false,
                    engine_softkey_option_left: None,
                    engine_softkey_option_right: None,
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_get_choice_number(&state).to_string()
            }
            Some(command @ ("menu-add-object" | "menu-add-int")) if parts.len() == 7 => {
                let mut state =
                    choice_menu_state(choice_ids(parts[1]), 0, 0, value(&parts, 4) != 0);
                state.choice_texts = script_ids(parts[2]);
                state.update_body_lines = value(&parts, 3) != 0;
                let choice_text = utf16(parts[6]);
                let result = if command == "menu-add-object" {
                    menu_add_choice(&mut state, nullable_i32(parts[5]), choice_text)
                } else {
                    menu_add_choice_integer(&mut state, value(&parts, 5), choice_text)
                };
                menu_add_output(result, &state)
            }
            Some("menu-count") if parts.len() == 2 => {
                let state = choice_menu_state(choice_ids(parts[1]), 0, 0, false);
                match menu_count_choices(&state) {
                    Ok(count) => count.to_string(),
                    Err(_) => "NPE".to_owned(),
                }
            }
            Some("menu-get-id") if parts.len() == 3 => {
                let state = choice_menu_state(choice_ids(parts[1]), value(&parts, 2), 0, false);
                match menu_get_choice_id(&state) {
                    Ok(Some(handle)) => handle.to_string(),
                    Ok(None) => "NULL".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!("AIOOBE:{}:{}", error.index, error.length)
                    }
                }
            }
            Some(command @ ("menu-next" | "menu-previous")) if parts.len() == 5 => {
                let mut state = choice_menu_state(
                    choice_ids(parts[1]),
                    value(&parts, 2),
                    value(&parts, 3),
                    value(&parts, 4) != 0,
                );
                let result = if command == "menu-next" {
                    menu_next_choice(&mut state)
                } else {
                    menu_previous_choice(&mut state)
                };
                format!(
                    "{}:{}:{}:{}",
                    if result.is_ok() { "OK" } else { "NPE" },
                    state.selected_choice_number,
                    state.scroll,
                    i32::from(state.update_menu)
                )
            }
            Some("menu-position") if parts.len() == 5 => {
                let mut state = MenuState {
                    is_current: false,
                    selected_choice_number: 0,
                    x: value(&parts, 1),
                    y: value(&parts, 2),
                    scroll: 0,
                    text_scrolling: false,
                    update_menu: false,
                    top_text: None,
                    update_top_lines: false,
                    engine_softkey_option_left: None,
                    engine_softkey_option_right: None,
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_set_position(&mut state, value(&parts, 3), value(&parts, 4));
                format!("{}:{}", state.x, state.y)
            }
            Some("menu-current") if parts.len() == 3 => {
                let mut state = MenuState {
                    is_current: value(&parts, 1) != 0,
                    selected_choice_number: 0,
                    x: 0,
                    y: 0,
                    scroll: 0,
                    text_scrolling: false,
                    update_menu: false,
                    top_text: None,
                    update_top_lines: false,
                    engine_softkey_option_left: None,
                    engine_softkey_option_right: None,
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_set_current(&mut state, value(&parts, 2) != 0);
                if state.is_current { "1" } else { "0" }.to_owned()
            }
            Some("menu-scroll-increase") if parts.len() == 4 => {
                let mut state = MenuState {
                    is_current: false,
                    selected_choice_number: 0,
                    x: 0,
                    y: 0,
                    scroll: value(&parts, 1),
                    text_scrolling: value(&parts, 2) != 0,
                    update_menu: value(&parts, 3) != 0,
                    top_text: None,
                    update_top_lines: false,
                    engine_softkey_option_left: None,
                    engine_softkey_option_right: None,
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_scroll_increase(&mut state);
                format!(
                    "{}:{}:{}",
                    state.scroll,
                    i32::from(state.text_scrolling),
                    i32::from(state.update_menu)
                )
            }
            Some("menu-scroll-decrease") if parts.len() == 4 => {
                let mut state = MenuState {
                    is_current: false,
                    selected_choice_number: 0,
                    x: 0,
                    y: 0,
                    scroll: value(&parts, 1),
                    text_scrolling: value(&parts, 2) != 0,
                    update_menu: value(&parts, 3) != 0,
                    top_text: None,
                    update_top_lines: false,
                    engine_softkey_option_left: None,
                    engine_softkey_option_right: None,
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_scroll_decrease(&mut state);
                format!(
                    "{}:{}:{}",
                    state.scroll,
                    i32::from(state.text_scrolling),
                    i32::from(state.update_menu)
                )
            }
            Some("menu-top") if parts.len() == 5 => {
                let mut state = MenuState {
                    is_current: false,
                    selected_choice_number: 0,
                    x: 0,
                    y: 0,
                    scroll: 0,
                    text_scrolling: false,
                    update_menu: value(&parts, 2) != 0,
                    top_text: utf16(parts[1]),
                    update_top_lines: value(&parts, 3) != 0,
                    engine_softkey_option_left: None,
                    engine_softkey_option_right: None,
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_set_top(&mut state, utf16(parts[4]));
                format!(
                    "{}:{}:{}",
                    utf16_output(state.top_text.as_deref()),
                    i32::from(state.update_top_lines),
                    i32::from(state.update_menu)
                )
            }
            Some("menu-softkeys") if parts.len() == 5 => {
                let mut state = MenuState {
                    is_current: false,
                    selected_choice_number: 0,
                    x: 0,
                    y: 0,
                    scroll: 0,
                    text_scrolling: false,
                    update_menu: false,
                    top_text: None,
                    update_top_lines: false,
                    engine_softkey_option_left: utf16(parts[1]),
                    engine_softkey_option_right: utf16(parts[2]),
                    choice_ids: None,
                    choice_texts: None,
                    update_body_lines: false,
                    current_inventory_item_resource: None,
                };
                menu_set_softkey_options(&mut state, utf16(parts[3]), utf16(parts[4]));
                format!(
                    "{}:{}",
                    utf16_output(state.engine_softkey_option_left.as_deref()),
                    utf16_output(state.engine_softkey_option_right.as_deref())
                )
            }
            Some("menu-resource") if parts.len() == 3 => {
                let mut state = choice_menu_state(None, 0, 0, false);
                state.current_inventory_item_resource = nullable_i32(parts[1]);
                menu_set_inventory_item_resource(&mut state, nullable_i32(parts[2]));
                state
                    .current_inventory_item_resource
                    .map_or_else(|| "n".to_owned(), |handle| handle.to_string())
            }
            Some(command @ ("menu-active" | "menu-close-all" | "menu-get-current"))
                if parts.len() == 2 =>
            {
                let length = value(&parts, 1);
                let mut statics = MenuStatics {
                    stack: if length < 0 {
                        None
                    } else {
                        Some((0..length as u32).collect())
                    },
                };
                match command {
                    "menu-active" => match menu_active(&statics) {
                        Ok(active) => i32::from(active).to_string(),
                        Err(_) => "NPE".to_owned(),
                    },
                    "menu-close-all" => {
                        menu_close_all(&mut statics);
                        statics
                            .stack
                            .as_deref()
                            .expect("new stack")
                            .len()
                            .to_string()
                    }
                    "menu-get-current" => match menu_get_current(&statics) {
                        Ok(Some(index)) => index.to_string(),
                        Ok(None) => "NULL".to_owned(),
                        Err(_) => "NPE".to_owned(),
                    },
                    _ => unreachable!(),
                }
            }
            Some("menu-close-current") if parts.len() == 3 => {
                let mut statics = MenuStatics {
                    stack: menu_handles(parts[1]),
                };
                let mut menus: Vec<MenuState> = parts[2]
                    .strip_prefix('b')
                    .expect("menu flags need b prefix")
                    .bytes()
                    .map(|flag| {
                        let mut menu = choice_menu_state(None, 0, 0, false);
                        menu.is_current = match flag {
                            b'0' => false,
                            b'1' => true,
                            _ => panic!("menu flag must be boolean"),
                        };
                        menu
                    })
                    .collect();
                let status = match menu_close_current(&mut statics, |handle, current| {
                    let menu = menus
                        .get_mut(handle as usize)
                        .ok_or(orphan_jvm::NullPointerException)?;
                    menu_set_current(menu, current);
                    Ok(())
                }) {
                    Ok(()) => "OK",
                    Err(_) => "NPE",
                };
                let flags: String = menus
                    .iter()
                    .map(|menu| if menu.is_current { '1' } else { '0' })
                    .collect();
                format!(
                    "{status}:{}:b{flags}",
                    menu_handles_output(statics.stack.as_deref())
                )
            }
            Some(command @ ("ink-read" | "ink-read-n" | "ink-read-signed"))
                if parts.len() == if command == "ink-read" { 4 } else { 5 } =>
            {
                let script = InkScriptState {
                    data: bytes(parts[2]),
                    event_offsets: None,
                    string_list: None,
                    gfx_id: None,
                };
                let mut interpreter = ink_interpreter_new(
                    if value(&parts, 1) == 0 { None } else { Some(1) },
                    value(&parts, 3),
                    None,
                );
                let read_result = match command {
                    "ink-read" => ink_interpreter_read(&mut interpreter, &script),
                    "ink-read-n" => {
                        ink_interpreter_read_bytes(&mut interpreter, &script, value(&parts, 4))
                    }
                    "ink-read-signed" => {
                        ink_interpreter_read_signed(&mut interpreter, &script, value(&parts, 4))
                    }
                    _ => unreachable!(),
                };
                match read_result {
                    Ok(read_value) => format!("OK:{read_value}:{}", interpreter.offset),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => {
                        format!("NPE:{}", interpreter.offset)
                    }
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!(
                            "AIOOBE:{}:{}:{}",
                            error.index, error.length, interpreter.offset
                        )
                    }
                }
            }
            Some("ink-has-command") if parts.len() == 6 => {
                let script = InkScriptState {
                    data: bytes(parts[2]),
                    event_offsets: None,
                    string_list: script_ids(parts[3]),
                    gfx_id: None,
                };
                let mut interpreter = ink_interpreter_new(
                    if value(&parts, 1) == 0 { None } else { Some(1) },
                    value(&parts, 4),
                    None,
                );
                match ink_interpreter_has_command(&mut interpreter, &script, value(&parts, 5)) {
                    Ok(found) => format!("OK:{}:{}", i32::from(found), interpreter.offset),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => {
                        format!("NPE:{}", interpreter.offset)
                    }
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_)) => {
                        format!("AIOOBE:{}", interpreter.offset)
                    }
                }
            }
            Some("ink-script-has-command") if parts.len() == 5 => {
                let script = InkScriptState {
                    data: bytes(parts[1]),
                    event_offsets: ints(parts[3]),
                    string_list: script_ids(parts[2]),
                    gfx_id: None,
                };
                match ink_script_has_command(&script, value(&parts, 4)) {
                    Ok(found) => format!("OK:{}", i32::from(found)),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(_)) => {
                        "AIOOBE".to_owned()
                    }
                }
            }
            Some("ink-new") if parts.len() == 4 => {
                let interpreter = ink_interpreter_new(
                    if value(&parts, 1) == 0 {
                        None
                    } else {
                        Some(11)
                    },
                    value(&parts, 2),
                    if value(&parts, 3) == 0 {
                        None
                    } else {
                        Some(22)
                    },
                );
                format!(
                    "{}:{}:{}:{}:{}",
                    i32::from(interpreter.script.is_some()),
                    interpreter.status,
                    interpreter.offset,
                    i32::from(interpreter.room_object.is_some()),
                    i32::from(interpreter.language_debug_mode)
                )
            }
            Some("ink-execute") if parts.len() == 8 => {
                const SELF_HANDLE: u32 = 7;
                const OTHER_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[3]),
                    event_offsets: None,
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let mut state = ink_interpreter_new(
                    if value(&parts, 2) == 0 { None } else { Some(1) },
                    value(&parts, 4),
                    None,
                );
                let mut statics = InkInterpreterStatics {
                    paused_thread: match value(&parts, 5) {
                        0 => None,
                        1 => Some(SELF_HANDLE),
                        _ => Some(OTHER_HANDLE),
                    },
                };
                let result = if parts[1] == "execute" {
                    ink_interpreter_execute(
                        &mut statics,
                        &mut state,
                        SELF_HANDLE,
                        execution_value(parts[6], parts[7]),
                        |state| oracle_execute_command(state, &script),
                    )
                } else {
                    ink_interpreter_resume(&mut statics, &mut state, SELF_HANDLE, |state| {
                        oracle_execute_command(state, &script)
                    })
                };
                execution_output(result, &state, &statics, SELF_HANDLE, OTHER_HANDLE)
            }
            Some("ink-script-resume") if parts.len() == 5 => {
                const SELF_HANDLE: u32 = 7;
                const OTHER_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[3]),
                    event_offsets: None,
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let mut state = ink_interpreter_new(
                    if value(&parts, 2) == 0 { None } else { Some(1) },
                    value(&parts, 4),
                    None,
                );
                let mut statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 1) == 0 {
                        None
                    } else {
                        Some(SELF_HANDLE)
                    },
                };
                let result = ink_script_resume(&mut statics, |statics, paused_thread| {
                    ink_interpreter_resume(statics, &mut state, paused_thread, |state| {
                        oracle_execute_command(state, &script)
                    })
                })
                .map(|()| None);
                execution_output(result, &state, &statics, SELF_HANDLE, OTHER_HANDLE)
            }
            Some("ink-script-wait") if parts.len() == 6 => {
                const SELF_HANDLE: u32 = 7;
                const OTHER_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[4]),
                    event_offsets: None,
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let mut interpreter = ink_interpreter_new(
                    if value(&parts, 3) == 0 { None } else { Some(1) },
                    value(&parts, 5),
                    None,
                );
                let mut interpreter_statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 2) == 0 {
                        None
                    } else {
                        Some(SELF_HANDLE)
                    },
                };
                let mut script_statics = InkScriptStatics {
                    scripts: None,
                    wait_stop: parts[1].parse().expect("wait deadline must be i64"),
                    item_id: None,
                };
                let initial_wait_stop = script_statics.wait_stop;
                let result = ink_script_is_waiting(
                    &mut script_statics,
                    &mut interpreter_statics,
                    || 1_000,
                    |script_statics, interpreter_statics| {
                        let result =
                            ink_script_resume(interpreter_statics, |statics, paused_thread| {
                                ink_interpreter_resume(
                                    statics,
                                    &mut interpreter,
                                    paused_thread,
                                    |state| oracle_execute_command(state, &script),
                                )
                            });
                        if interpreter.status == 5 {
                            script_statics.wait_stop = 1_000;
                        }
                        result
                    },
                )
                .map(|waiting| Some(JavaOwnedObject::Integer(i32::from(waiting))));
                let wait_stop = if initial_wait_stop == 1 && script_statics.wait_stop != 0 {
                    "T".to_owned()
                } else {
                    script_statics.wait_stop.to_string()
                };
                format!(
                    "{}:{}",
                    execution_output(
                        result,
                        &interpreter,
                        &interpreter_statics,
                        SELF_HANDLE,
                        OTHER_HANDLE,
                    ),
                    wait_stop
                )
            }
            Some("ink-script-execute") if parts.len() == 10 => {
                const SCRIPT_HANDLE: u32 = 3;
                const ROOM_HANDLE: u32 = 4;
                const INTERPRETER_HANDLE: u32 = 7;
                const OLD_PAUSED_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[2]),
                    event_offsets: ints(parts[3]),
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let room_object = if value(&parts, 7) == 0 {
                    None
                } else {
                    Some(ROOM_HANDLE)
                };
                let mut interpreter = ink_interpreter_new(None, 123, Some(99));
                let mut statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 9) == 0 {
                        None
                    } else {
                        Some(OLD_PAUSED_HANDLE)
                    },
                };
                let initial_value = execution_value(parts[5], parts[6]);
                let result = if parts[1] == "default" {
                    ink_script_execute_event(
                        &mut statics,
                        &script,
                        SCRIPT_HANDLE,
                        &mut interpreter,
                        INTERPRETER_HANDLE,
                        value(&parts, 4),
                        initial_value,
                        room_object,
                        |state| oracle_execute_command(state, &script),
                    )
                } else {
                    ink_script_execute_event_debug(
                        &mut statics,
                        &script,
                        SCRIPT_HANDLE,
                        &mut interpreter,
                        INTERPRETER_HANDLE,
                        value(&parts, 4),
                        initial_value,
                        room_object,
                        value(&parts, 8) != 0,
                        |state| oracle_execute_command(state, &script),
                    )
                };
                event_execution_output(
                    result,
                    &interpreter,
                    &statics,
                    SCRIPT_HANDLE,
                    room_object,
                    INTERPRETER_HANDLE,
                    OLD_PAUSED_HANDLE,
                )
            }
            Some("ink-script-execute-id") if parts.len() == 10 => {
                const SCRIPT_HANDLE: u32 = 3;
                const ROOM_HANDLE: u32 = 4;
                const INTERPRETER_HANDLE: u32 = 7;
                const OLD_PAUSED_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[3]),
                    event_offsets: ints(parts[4]),
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let script_statics = ink_script_statics(parts[1]);
                let room_object = if value(&parts, 8) == 0 {
                    None
                } else {
                    Some(ROOM_HANDLE)
                };
                let mut interpreter = ink_interpreter_new(None, 123, Some(99));
                let mut interpreter_statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 9) == 0 {
                        None
                    } else {
                        Some(OLD_PAUSED_HANDLE)
                    },
                };
                let initial_value = execution_value(parts[6], parts[7]);
                let script_id = utf16(parts[2]);
                let result = ink_script_execute_event_by_id(
                    &script_statics,
                    script_id.as_deref(),
                    value(&parts, 5),
                    initial_value,
                    room_object,
                    |script_handle, event_code, initial_value, room_object, language_debug_mode| {
                        assert_eq!(script_handle, SCRIPT_HANDLE);
                        ink_script_execute_event_debug(
                            &mut interpreter_statics,
                            &script,
                            script_handle,
                            &mut interpreter,
                            INTERPRETER_HANDLE,
                            event_code,
                            initial_value,
                            room_object,
                            language_debug_mode,
                            |state| oracle_execute_command(state, &script),
                        )
                    },
                );
                event_execution_by_id_output(
                    result,
                    &interpreter,
                    &interpreter_statics,
                    SCRIPT_HANDLE,
                    room_object,
                    INTERPRETER_HANDLE,
                    OLD_PAUSED_HANDLE,
                )
            }
            Some("inventory-equip") if parts.len() == 8 => {
                const SCRIPT_HANDLE: u32 = 3;
                const INTERPRETER_HANDLE: u32 = 7;
                const OLD_PAUSED_HANDLE: u32 = 8;
                let stack_length = value(&parts, 1);
                let mut menus = MenuStatics {
                    stack: if stack_length < 0 {
                        None
                    } else {
                        Some((0..stack_length as u32).collect())
                    },
                };
                let script = InkScriptState {
                    data: bytes(parts[4]),
                    event_offsets: ints(parts[5]),
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let mut script_statics = ink_script_statics(parts[3]);
                script_statics.item_id = utf16(parts[2]);
                let mut interpreter = ink_interpreter_new(None, 123, Some(99));
                let mut interpreter_statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 7) == 0 {
                        None
                    } else {
                        Some(OLD_PAUSED_HANDLE)
                    },
                };
                let result = ink_engine_inventory_equip_unequip_handling(
                    &mut menus,
                    &script_statics,
                    value(&parts, 6),
                    |script_handle, event_code, initial_value, room_object, language_debug_mode| {
                        assert_eq!(script_handle, SCRIPT_HANDLE);
                        ink_script_execute_event_debug(
                            &mut interpreter_statics,
                            &script,
                            script_handle,
                            &mut interpreter,
                            INTERPRETER_HANDLE,
                            event_code,
                            initial_value,
                            room_object,
                            language_debug_mode,
                            |state| oracle_execute_command(state, &script),
                        )
                    },
                )
                .map(|()| None);
                format!(
                    "{}:{}",
                    event_execution_by_id_output(
                        result,
                        &interpreter,
                        &interpreter_statics,
                        SCRIPT_HANDLE,
                        None,
                        INTERPRETER_HANDLE,
                        OLD_PAUSED_HANDLE,
                    ),
                    menu_handles_output(menus.stack.as_deref())
                )
            }
            Some("room-event") if parts.len() == 12 => {
                const SCRIPT_HANDLE: u32 = 3;
                const ROOM_HANDLE: u32 = 4;
                const INTERPRETER_HANDLE: u32 = 7;
                const OLD_PAUSED_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[5]),
                    event_offsets: ints(parts[6]),
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let script_statics = ink_script_statics(parts[4]);
                let mut room = RoomObjectState {
                    script_id: utf16(parts[3]),
                    script: (value(&parts, 2) != 0).then_some(SCRIPT_HANDLE),
                    visible: true,
                    ..empty_room_object_state()
                };
                let mut interpreter = ink_interpreter_new(None, 123, Some(99));
                let mut interpreter_statics = InkInterpreterStatics {
                    paused_thread: (value(&parts, 11) != 0).then_some(OLD_PAUSED_HANDLE),
                };
                match parts[1] {
                    "execute" => {
                        let result = room_object_execute_event(
                            &script_statics,
                            &mut room,
                            value(&parts, 7),
                            execution_value(parts[8], parts[9]),
                            value(&parts, 10) != 0,
                            |script_handle,
                             event_code,
                             initial_value,
                             _room,
                             language_debug_mode| {
                                assert_eq!(script_handle, SCRIPT_HANDLE);
                                ink_script_execute_event_debug(
                                    &mut interpreter_statics,
                                    &script,
                                    script_handle,
                                    &mut interpreter,
                                    INTERPRETER_HANDLE,
                                    event_code,
                                    initial_value,
                                    Some(ROOM_HANDLE),
                                    language_debug_mode,
                                    |state| oracle_execute_command(state, &script),
                                )
                            },
                        );
                        room_event_execution_output(
                            result,
                            &room,
                            &interpreter,
                            &interpreter_statics,
                            SCRIPT_HANDLE,
                            ROOM_HANDLE,
                            INTERPRETER_HANDLE,
                            OLD_PAUSED_HANDLE,
                        )
                    }
                    "name" => {
                        let result = room_object_get_name(
                            &script_statics,
                            &mut room,
                            |script_handle,
                             event_code,
                             initial_value,
                             _room,
                             language_debug_mode| {
                                assert_eq!(script_handle, SCRIPT_HANDLE);
                                ink_script_execute_event_debug(
                                    &mut interpreter_statics,
                                    &script,
                                    script_handle,
                                    &mut interpreter,
                                    INTERPRETER_HANDLE,
                                    event_code,
                                    initial_value,
                                    Some(ROOM_HANDLE),
                                    language_debug_mode,
                                    |state| oracle_execute_command(state, &script),
                                )
                            },
                        );
                        room_string_event_execution_output(
                            result,
                            &room,
                            &interpreter,
                            &interpreter_statics,
                            SCRIPT_HANDLE,
                            ROOM_HANDLE,
                            INTERPRETER_HANDLE,
                            OLD_PAUSED_HANDLE,
                        )
                    }
                    "move" => {
                        let result = room_object_get_move_direction(
                            &script_statics,
                            &mut room,
                            |script_handle,
                             event_code,
                             initial_value,
                             _room,
                             language_debug_mode| {
                                assert_eq!(script_handle, SCRIPT_HANDLE);
                                ink_script_execute_event_debug(
                                    &mut interpreter_statics,
                                    &script,
                                    script_handle,
                                    &mut interpreter,
                                    INTERPRETER_HANDLE,
                                    event_code,
                                    initial_value,
                                    Some(ROOM_HANDLE),
                                    language_debug_mode,
                                    |state| oracle_execute_command(state, &script),
                                )
                            },
                        );
                        room_string_event_execution_output(
                            result,
                            &room,
                            &interpreter,
                            &interpreter_statics,
                            SCRIPT_HANDLE,
                            ROOM_HANDLE,
                            INTERPRETER_HANDLE,
                            OLD_PAUSED_HANDLE,
                        )
                    }
                    "hover" => {
                        let result = room_object_enter_hover(
                            &script_statics,
                            &mut room,
                            ROOM_HANDLE,
                            |script_handle, event_code| {
                                assert_eq!(script_handle, SCRIPT_HANDLE);
                                ink_script_has_event(&script, event_code)
                            },
                            |script_handle,
                             event_code,
                             initial_value,
                             _room,
                             language_debug_mode| {
                                assert_eq!(script_handle, SCRIPT_HANDLE);
                                ink_script_execute_event_debug(
                                    &mut interpreter_statics,
                                    &script,
                                    script_handle,
                                    &mut interpreter,
                                    INTERPRETER_HANDLE,
                                    event_code,
                                    initial_value,
                                    Some(ROOM_HANDLE),
                                    language_debug_mode,
                                    |state| oracle_execute_command(state, &script),
                                )
                            },
                        );
                        room_hover_execution_output(
                            result,
                            &room,
                            &interpreter,
                            &interpreter_statics,
                            SCRIPT_HANDLE,
                            ROOM_HANDLE,
                            INTERPRETER_HANDLE,
                            OLD_PAUSED_HANDLE,
                        )
                    }
                    _ => unreachable!(),
                }
            }
            Some("ink-script-item-name") if parts.len() == 6 => {
                const SCRIPT_HANDLE: u32 = 3;
                const INTERPRETER_HANDLE: u32 = 7;
                const OLD_PAUSED_HANDLE: u32 = 8;
                let script = InkScriptState {
                    data: bytes(parts[3]),
                    event_offsets: ints(parts[4]),
                    string_list: Some(vec![Some("ret".encode_utf16().collect())]),
                    gfx_id: None,
                };
                let script_statics = ink_script_statics(parts[1]);
                let mut interpreter = ink_interpreter_new(None, 123, Some(99));
                let mut interpreter_statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 5) == 0 {
                        None
                    } else {
                        Some(OLD_PAUSED_HANDLE)
                    },
                };
                let item_id = utf16(parts[2]);
                let result = ink_script_get_item_name(
                    &script_statics,
                    item_id.as_deref(),
                    |script_handle, event_code, initial_value, room_object, language_debug_mode| {
                        assert_eq!(script_handle, SCRIPT_HANDLE);
                        ink_script_execute_event_debug(
                            &mut interpreter_statics,
                            &script,
                            script_handle,
                            &mut interpreter,
                            INTERPRETER_HANDLE,
                            event_code,
                            initial_value,
                            room_object,
                            language_debug_mode,
                            |state| oracle_execute_command(state, &script),
                        )
                    },
                );
                item_name_execution_output(
                    result,
                    &interpreter,
                    &interpreter_statics,
                    SCRIPT_HANDLE,
                    INTERPRETER_HANDLE,
                    OLD_PAUSED_HANDLE,
                )
            }
            Some("ink-get-string") if parts.len() == 3 => {
                let state = InkScriptState {
                    data: None,
                    event_offsets: None,
                    string_list: script_ids(parts[1]),
                    gfx_id: None,
                };
                match ink_script_get_string(&state, value(&parts, 2)) {
                    Ok(string) => utf16_output(string),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!("AIOOBE:{}:{}", error.index, error.length)
                    }
                }
            }
            Some("ink-has-event") if parts.len() == 3 => {
                let state = InkScriptState {
                    data: None,
                    event_offsets: ints(parts[1]),
                    string_list: None,
                    gfx_id: None,
                };
                match ink_script_has_event(&state, value(&parts, 2)) {
                    Ok(has_event) => i32::from(has_event).to_string(),
                    Err(orphan_jvm::ArrayAccessException::NullPointer(_)) => "NPE".to_owned(),
                    Err(orphan_jvm::ArrayAccessException::ArrayIndexOutOfBounds(error)) => {
                        format!("AIOOBE:{}:{}", error.index, error.length)
                    }
                }
            }
            Some("ink-stop") if parts.len() == 2 => {
                let mut statics = InkInterpreterStatics {
                    paused_thread: if value(&parts, 1) == 0 { None } else { Some(1) },
                };
                ink_script_stop(&mut statics);
                i32::from(statics.paused_thread.is_some()).to_string()
            }
            Some("panel-new") if parts.len() == 5 => {
                let mut state = RoomObjectState {
                    battle_panel_id: value(&parts, 1),
                    battle_panel: ints(parts[2]),
                    ..empty_room_object_state()
                };
                let statics = room_object_statics(0, value(&parts, 4));
                match room_object_battle_panel_new(&mut state, &statics, value(&parts, 3)) {
                    Ok(()) => format!(
                        "OK:{}:{}",
                        state.battle_panel_id,
                        ints_output(state.battle_panel.as_deref())
                    ),
                    Err(error) => format!(
                        "NAS:{}:{}:{}",
                        error.length,
                        state.battle_panel_id,
                        ints_output(state.battle_panel.as_deref())
                    ),
                }
            }
            Some(command @ ("panel-max" | "panel-health" | "panel-bar" | "panel-time"))
                if parts.len() == 5 =>
            {
                let mut state = RoomObjectState {
                    battle_panel_id: value(&parts, 1),
                    battle_panel: ints(parts[2]),
                    ..empty_room_object_state()
                };
                let statics = room_object_statics(value(&parts, 3), 0);
                let new_value = value(&parts, 4);
                let result = match command {
                    "panel-max" => room_object_bp_set_max_health(&mut state, &statics, new_value),
                    "panel-health" => room_object_bp_set_health(&mut state, &statics, new_value),
                    "panel-bar" => room_object_bp_set_bar_size(&mut state, &statics, new_value),
                    "panel-time" => room_object_bp_set_time(&mut state, &statics, new_value),
                    _ => unreachable!(),
                };
                panel_set_output(result, &state)
            }
            Some("room-is-over") if parts.len() == 9 => {
                let state = RoomObjectState {
                    visible: value(&parts, 1) != 0,
                    active: value(&parts, 2) != 0,
                    left: value(&parts, 3),
                    right: value(&parts, 4),
                    top: value(&parts, 5),
                    bottom: value(&parts, 6),
                    ..empty_room_object_state()
                };
                i32::from(room_object_is_over(
                    &state,
                    value(&parts, 7),
                    value(&parts, 8),
                ))
                .to_string()
            }
            _ => panic!("invalid oracle request: {line}"),
        };
        println!("{result}");
    }
}
