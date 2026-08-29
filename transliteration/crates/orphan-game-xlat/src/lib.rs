//! Strict transliteration target. This crate uses ordinary Rust and grows only via
//! bytecode + canonical-Java + Rust-AST crosswalked bodies.

extern crate alloc;

use alloc::string::ToString;

mod state;

pub use state::{
    ApplicationState, CheatControllerStatics, GameCanvasState, GameResourceState,
    GameResourceStatics, InkEngineState, InkInterpreterState, InkInterpreterStatics,
    InkScriptState, InkScriptStatics, MenuState, MenuStatics, ResourceRequestState,
    RoomObjectState, RoomObjectStatics, SilentHillGameStatics, GAME_CANVAS_INITIAL_TRANSFORM_TABLE,
};

#[derive(Clone, Copy)]
pub enum JavaObject<'a> {
    Null,
    Integer(i32),
    String(&'a [u16]),
    Other,
}

pub enum JavaOwnedObject {
    Integer(i32),
    String(alloc::vec::Vec<u16>),
    Other,
}

pub enum InkScriptRegistryValue {
    Script(u32),
    Other,
}

pub enum InkScriptExecuteEventError<E> {
    NullPointer,
    ClassCast,
    Execute(E),
}

pub enum InkScriptGetItemNameError<E> {
    ExecuteEvent(InkScriptExecuteEventError<E>),
    ClassCast,
}

pub enum RoomObjectStringEventError<E> {
    ExecuteEvent(InkScriptExecuteEventError<E>),
    ClassCast,
}

pub enum RoomObjectEnterHoverError<E> {
    HasEvent(E),
    ExecuteEvent(InkScriptExecuteEventError<E>),
}

pub enum InkEnginePopupCreateError<E> {
    WrapString(E),
    ArrayAccess(orphan_jvm::ArrayAccessException),
}

pub enum ApplicationClearAllRmsError<E> {
    ResourceClear(E),
    ScriptListNull,
}

pub enum ApplicationSetDisplayError<E> {
    GetDisplay(E),
    DisplayNull,
    SetCurrent(E),
}

#[derive(Debug, Eq, PartialEq)]
pub enum ApplicationRepaintCanvasIfPossibleError<E> {
    CanvasNullBeforeRepaint,
    Repaint(E),
    CanvasNullBeforeServiceRepaints,
    ServiceRepaints(E),
}

#[derive(Debug, Eq, PartialEq)]
pub enum ApplicationRmsDeleteError<E> {
    NotFound,
    RecordStore,
    Uncaught(E),
}

#[derive(Debug, Eq, PartialEq)]
pub enum ApplicationResourceMakeSubChunkError {
    NegativeArraySize(orphan_jvm::NegativeArraySizeException),
    ArrayCopy(orphan_jvm::ArrayCopyException),
}

pub enum GameCanvasNewError<E> {
    SuperConstructor(E),
    SetFullScreen(E),
}

pub enum JavaResourceId {
    Integer(i32),
    String(alloc::vec::Vec<u16>),
    Opaque(u32),
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum InkVariableError {
    NullPointer,
    StringIndexOutOfBounds,
    NumberFormat,
}

pub const CODED_STRING_COLUMN_WIDTH: i32 = 30;
pub const CODED_STRING_MAX_LINES: i32 = 100;
pub const GAME_RESOURCE_TYPE_GRAPHICS: i32 = 1;
pub const GAME_RESOURCE_TYPE_SOUND: i32 = 2;
pub const INK_SCRIPT_GFX_TYPE_NONE: i32 = 0;
pub const INK_SCRIPT_GFX_TYPE_STRING: i32 = 1;
pub const INK_SCRIPT_GFX_TYPE_INTEGER: i32 = 2;
pub const INK_EVENT_GET_NAME: i32 = 1;
pub const INK_EVENT_GET_MOVE_DIRECTION: i32 = 30;
pub const INK_EVENT_HOVER_IN: i32 = 54;
pub const ROOM_OBJECT_TYPE_GRAPHICS: i32 = 1;
pub const ROOM_OBJECT_TYPE_ZONE: i32 = 2;
pub const ROOM_OBJECT_TYPE_BATTLE_ZONE: i32 = 3;
pub const ROOM_OBJECT_TYPE_COLOR_ZONE: i32 = 4;
pub const ROOM_OBJECT_TYPE_TEXT_ZONE: i32 = 5;
pub const ROOM_OBJECT_TYPE_TILE_ZONE: i32 = 6;
pub const ROOM_OBJECT_GRAPHICS_ID_TYPE_STRING: i32 = 1;
pub const ROOM_OBJECT_GRAPHICS_ID_TYPE_INTEGER: i32 = 2;
pub const ROOM_OBJECT_ANIMATION_IDLE: i32 = 0;
pub const ROOM_OBJECT_ANIMATION_ENEMY_ATTACK_1: i32 = 1;
pub const ROOM_OBJECT_ANIMATION_ENEMY_ATTACK_2: i32 = 2;
pub const ROOM_OBJECT_ANIMATION_ENEMY_ATTACK_3: i32 = 3;
pub const ROOM_OBJECT_ANIMATION_HERO_ATTACK_1: i32 = 4;
pub const ROOM_OBJECT_ANIMATION_HERO_ATTACK_2: i32 = 5;
pub const ROOM_OBJECT_ANIMATION_SINGLE: i32 = 6;
pub const ROOM_OBJECT_ANIMATION_COUNT: i32 = 7;
pub const ROOM_OBJECT_ANIMATION_VALUES_PER_PART: i32 = 4;
pub const ROOM_OBJECT_ANIMATION_OFFSET_GRAPHICS: i32 = 0;
pub const ROOM_OBJECT_ANIMATION_OFFSET_DURATION: i32 = 1;
pub const ROOM_OBJECT_ANIMATION_OFFSET_X: i32 = 2;
pub const ROOM_OBJECT_ANIMATION_OFFSET_Y: i32 = 3;
pub const ROOM_OBJECT_DEFAULT_COLOR: i32 = 16_777_215;
pub const ROOM_OBJECT_TEXT_ZONE_DEFAULT_WIDTH: i32 = 30;

pub fn min(left: i32, right: i32) -> i32 {
    if left < right {
        left
    } else {
        right
    }
}

pub fn max(left: i32, right: i32) -> i32 {
    if left > right {
        left
    } else {
        right
    }
}

pub fn abs(value: i32) -> i32 {
    if value >= 0 {
        value
    } else {
        value.wrapping_neg()
    }
}

pub fn dir(value: i32) -> i32 {
    if value == 0 {
        0
    } else if value > 0 {
        1
    } else {
        -1
    }
}

pub fn resource_exit() {}

pub fn application_destroy_app<Exit, E>(_forced: bool, exit: Exit) -> Result<(), E>
where
    Exit: FnOnce() -> Result<(), E>,
{
    exit()
}

pub fn application_pause_app<SetHide, E>(set_hide: SetHide) -> Result<(), E>
where
    SetHide: FnOnce(bool) -> Result<(), E>,
{
    set_hide(true)
}

pub fn application_app_start<Start, E>(start: Start) -> Result<(), E>
where
    Start: FnOnce() -> Result<(), E>,
{
    start()
}

pub fn application_print_array<T, Output>(
    data: Option<&[T]>,
    print_array: impl FnOnce(&[T], i32, i32) -> Output,
) -> Result<Output, orphan_jvm::NullPointerException> {
    let data = data.ok_or(orphan_jvm::NullPointerException)?;
    Ok(print_array(data, 0, data.len() as i32))
}

pub fn application_room_repaint_run<Repaint, E>(
    state: &mut ApplicationState,
    room_repaint: Repaint,
) -> Result<(), E>
where
    Repaint: FnOnce(&mut ApplicationState) -> Result<(), E>,
{
    room_repaint(state)?;
    state.room_repaint_thread = None;
    Ok(())
}

pub fn application_clear_all_rms<ResourceClear, E>(
    script_statics: &mut InkScriptStatics,
    resource_clear: ResourceClear,
) -> Result<(), ApplicationClearAllRmsError<E>>
where
    ResourceClear: FnOnce() -> Result<(), E>,
{
    resource_clear().map_err(ApplicationClearAllRmsError::ResourceClear)?;
    let scripts = script_statics
        .scripts
        .as_mut()
        .ok_or(ApplicationClearAllRmsError::ScriptListNull)?;
    scripts.clear();
    Ok(())
}

pub fn application_free_memory<CollectGarbage, FreeMemory>(
    state: &ApplicationState,
    collect_garbage: CollectGarbage,
    free_memory: FreeMemory,
) -> Result<i64, orphan_jvm::NullPointerException>
where
    CollectGarbage: FnOnce(),
    FreeMemory: FnOnce(u32) -> i64,
{
    collect_garbage();
    let runtime = state
        .runtime_instance
        .ok_or(orphan_jvm::NullPointerException)?;
    Ok(free_memory(runtime))
}

pub fn application_set_display<GetDisplay, SetCurrent, E>(
    state: &ApplicationState,
    displayable: Option<u32>,
    get_display: GetDisplay,
    set_current: SetCurrent,
) -> Result<(), ApplicationSetDisplayError<E>>
where
    GetDisplay: FnOnce(Option<u32>) -> Result<Option<u32>, E>,
    SetCurrent: FnOnce(u32, Option<u32>) -> Result<(), E>,
{
    let display = get_display(state.midlet_instance)
        .map_err(ApplicationSetDisplayError::GetDisplay)?
        .ok_or(ApplicationSetDisplayError::DisplayNull)?;
    set_current(display, displayable).map_err(ApplicationSetDisplayError::SetCurrent)?;
    Ok(())
}

pub fn application_paint<Graphics, Paint, E>(
    state: &ApplicationState,
    graphics: Graphics,
    paint: Paint,
) -> Result<(), E>
where
    Paint: FnOnce(Graphics) -> Result<(), E>,
{
    if state.fade_frames <= state.demo_frames {
        paint(graphics)?;
    }
    Ok(())
}

pub fn application_repaint_canvas_if_possible<Repaint, ServiceRepaints, E>(
    state: &mut ApplicationState,
    repaint: Repaint,
    service_repaints: ServiceRepaints,
) -> Result<(), ApplicationRepaintCanvasIfPossibleError<E>>
where
    Repaint: FnOnce(u32, &mut ApplicationState) -> Result<(), E>,
    ServiceRepaints: FnOnce(u32, &mut ApplicationState) -> Result<(), E>,
{
    if state.painting {
        return Ok(());
    }
    state.painting = true;
    let repaint_canvas = state
        .canvas_instance
        .ok_or(ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeRepaint)?;
    repaint(repaint_canvas, state).map_err(ApplicationRepaintCanvasIfPossibleError::Repaint)?;
    let service_repaints_canvas = state
        .canvas_instance
        .ok_or(ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeServiceRepaints)?;
    service_repaints(service_repaints_canvas, state)
        .map_err(ApplicationRepaintCanvasIfPossibleError::ServiceRepaints)?;
    Ok(())
}

pub fn application_rms_delete<Name, Delete, E>(
    name: Name,
    delete_record_store: Delete,
) -> Result<bool, E>
where
    Delete: FnOnce(Name) -> Result<(), ApplicationRmsDeleteError<E>>,
{
    match delete_record_store(name) {
        Ok(()) | Err(ApplicationRmsDeleteError::NotFound) => Ok(true),
        Err(ApplicationRmsDeleteError::RecordStore) => Ok(false),
        Err(ApplicationRmsDeleteError::Uncaught(error)) => Err(error),
    }
}

pub fn application_save_chunk_ini<Input, Bytes, GetBytes, SetRms, E>(
    input: Input,
    get_bytes: GetBytes,
    set_rms: SetRms,
) where
    GetBytes: FnOnce(Input) -> Result<Bytes, E>,
    SetRms: FnOnce(&[u16], Bytes) -> Result<bool, E>,
{
    let Ok(bytes) = get_bytes(input) else {
        return;
    };
    let _ = set_rms(&[82, 77, 83, 95, 99, 104, 117, 110, 107, 73, 78, 73], bytes);
}

pub fn application_resource_make_subchunk(
    state: &ApplicationState,
) -> Result<alloc::vec::Vec<u8>, ApplicationResourceMakeSubChunkError> {
    let allocation_length = state.resource_sc_current_size;
    if allocation_length < 0 {
        return Err(ApplicationResourceMakeSubChunkError::NegativeArraySize(
            orphan_jvm::NegativeArraySizeException {
                length: allocation_length,
            },
        ));
    }
    let mut subchunk = alloc::vec![0; allocation_length as usize];
    let source = state.resource_sc_data.as_deref();
    let copy_length = state.resource_sc_current_size;
    let source = source.ok_or(ApplicationResourceMakeSubChunkError::ArrayCopy(
        orphan_jvm::ArrayCopyException::NullPointer,
    ))?;
    if copy_length < 0 {
        return Err(ApplicationResourceMakeSubChunkError::ArrayCopy(
            orphan_jvm::ArrayCopyException::IndexOutOfBounds,
        ));
    }
    let copy_length = copy_length as usize;
    if source.len() < copy_length || subchunk.len() < copy_length {
        return Err(ApplicationResourceMakeSubChunkError::ArrayCopy(
            orphan_jvm::ArrayCopyException::IndexOutOfBounds,
        ));
    }
    subchunk[..copy_length].copy_from_slice(&source[..copy_length]);
    Ok(subchunk)
}

pub fn ink_engine_wrap_string<Text, Font, Output>(
    text: Text,
    maximum_length: i32,
    current_font: Font,
    wrap_string: impl FnOnce(Text, i32, Font) -> Output,
) -> Output {
    wrap_string(text, maximum_length, current_font)
}

pub fn ink_engine_popup_create<WrapString, CurrentTimeMillis, E>(
    application: &ApplicationState,
    engine: &mut InkEngineState,
    text: Option<&[u16]>,
    recovery_code: i32,
    wrap_string: WrapString,
    current_time_millis: CurrentTimeMillis,
) -> Result<(), InkEnginePopupCreateError<E>>
where
    WrapString: FnOnce(
        Option<&[u16]>,
        i32,
    ) -> Result<Option<alloc::vec::Vec<Option<alloc::vec::Vec<u16>>>>, E>,
    CurrentTimeMillis: FnOnce() -> i64,
{
    ink_engine_popup_create_with_max_time(
        application,
        engine,
        text,
        recovery_code,
        -1,
        wrap_string,
        current_time_millis,
    )
}

pub fn ink_engine_popup_create_with_max_time<WrapString, CurrentTimeMillis, E>(
    application: &ApplicationState,
    engine: &mut InkEngineState,
    text: Option<&[u16]>,
    recovery_code: i32,
    maximum_time: i32,
    wrap_string: WrapString,
    current_time_millis: CurrentTimeMillis,
) -> Result<(), InkEnginePopupCreateError<E>>
where
    WrapString: FnOnce(
        Option<&[u16]>,
        i32,
    ) -> Result<Option<alloc::vec::Vec<Option<alloc::vec::Vec<u16>>>>, E>,
    CurrentTimeMillis: FnOnce() -> i64,
{
    if engine.popup_number < 4 {
        let popup_number = engine.popup_number;
        let wrapped_text = wrap_string(
            text,
            orphan_jvm::i32_sub(orphan_jvm::i32_sub(application.canvas_width, 8), 8),
        )
        .map_err(InkEnginePopupCreateError::WrapString)?;
        *orphan_jvm::array_mut(engine.popup_texts.as_deref_mut(), popup_number)
            .map_err(InkEnginePopupCreateError::ArrayAccess)? = wrapped_text;

        let popup_number = engine.popup_number;
        *orphan_jvm::array_mut(engine.popup_recovery_codes.as_deref_mut(), popup_number)
            .map_err(InkEnginePopupCreateError::ArrayAccess)? = recovery_code;

        let popup_number = engine.popup_number;
        *orphan_jvm::array_mut(engine.popup_maximum_times.as_deref_mut(), popup_number)
            .map_err(InkEnginePopupCreateError::ArrayAccess)? = maximum_time;

        engine.popup_choice = 0;
        if !engine.popup_active {
            engine.popup_current = engine.popup_number;
            let current_maximum_time =
                *orphan_jvm::array_ref(engine.popup_maximum_times.as_deref(), engine.popup_current)
                    .map_err(InkEnginePopupCreateError::ArrayAccess)?;
            if current_maximum_time == -1 {
                engine.popup_end_time = -1;
            } else {
                engine.popup_end_time =
                    current_time_millis().wrapping_add(i64::from(current_maximum_time));
            }
        }
        engine.popup_active = true;
        engine.popup_number = orphan_jvm::i32_add(engine.popup_number, 1);
    }
    Ok(())
}

pub fn ink_engine_popup_set_next<CurrentTimeMillis>(
    engine: &mut InkEngineState,
    mut current_time_millis: CurrentTimeMillis,
) -> Result<(), orphan_jvm::ArrayAccessException>
where
    CurrentTimeMillis: FnMut() -> i64,
{
    engine.popup_current = orphan_jvm::i32_add(engine.popup_current, 1);
    if engine.popup_current >= engine.popup_number {
        engine.popup_number = 0;
        engine.popup_active = false;
        return Ok(());
    }
    engine.popup_minimum_time_ends = current_time_millis().wrapping_add(500);
    let maximum_time =
        *orphan_jvm::array_ref(engine.popup_maximum_times.as_deref(), engine.popup_current)?;
    if maximum_time == -1 {
        engine.popup_end_time = -1;
    } else {
        engine.popup_end_time = current_time_millis().wrapping_add(i64::from(maximum_time));
    }
    Ok(())
}

pub fn cheat_controller_new<SuperConstructor, E>(
    super_constructor: SuperConstructor,
) -> Result<(), E>
where
    SuperConstructor: FnOnce() -> Result<(), E>,
{
    super_constructor()
}

pub fn cheat_controller_initialize(statics: &mut CheatControllerStatics) {
    statics.last_key = -123;
}

pub fn silent_hill_game_new<SuperConstructor, E>(
    super_constructor: SuperConstructor,
) -> Result<(), E>
where
    SuperConstructor: FnOnce() -> Result<(), E>,
{
    super_constructor()
}

pub fn silent_hill_game_menu_reset_ingame_values<ResetIngameValues, E>(
    statics: &mut SilentHillGameStatics,
    reset_ingame_values: ResetIngameValues,
) -> Result<(), E>
where
    ResetIngameValues: FnOnce() -> Result<(), E>,
{
    reset_ingame_values()?;
    statics.hud_ammo_number_width = -1;
    statics.hud_ammo_update_needed = true;
    Ok(())
}

pub fn silent_hill_game_app_init<LoadImage, EngineAppInit, E>(
    statics: &mut SilentHillGameStatics,
    load_image: LoadImage,
    engine_app_init: EngineAppInit,
) -> Result<(), E>
where
    LoadImage: FnOnce(&[u16]) -> Option<u32>,
    EngineAppInit: FnOnce() -> Result<(), E>,
{
    statics.ink_menu_logo = load_image(&[
        103, 102, 120, 47, 109, 101, 110, 117, 95, 108, 111, 103, 111, 46, 112, 110, 103,
    ]);
    engine_app_init()?;
    Ok(())
}

pub fn ink_engine_new<SuperConstructor, E>(super_constructor: SuperConstructor) -> Result<(), E>
where
    SuperConstructor: FnOnce() -> Result<(), E>,
{
    super_constructor()
}

pub fn application_new<SuperConstructor, E>(super_constructor: SuperConstructor) -> Result<(), E>
where
    SuperConstructor: FnOnce() -> Result<(), E>,
{
    super_constructor()
}

pub fn ink_codes_new<SuperConstructor, E>(super_constructor: SuperConstructor) -> Result<(), E>
where
    SuperConstructor: FnOnce() -> Result<(), E>,
{
    super_constructor()
}

pub fn text_id_new<SuperConstructor, E>(super_constructor: SuperConstructor) -> Result<(), E>
where
    SuperConstructor: FnOnce() -> Result<(), E>,
{
    super_constructor()
}

pub fn game_canvas_new<SuperConstructor, SetFullScreen, E>(
    canvas: u32,
    super_constructor: SuperConstructor,
    set_full_screen: SetFullScreen,
) -> Result<(), GameCanvasNewError<E>>
where
    SuperConstructor: FnOnce(u32, bool) -> Result<(), E>,
    SetFullScreen: FnOnce(u32, bool) -> Result<(), E>,
{
    super_constructor(canvas, false).map_err(GameCanvasNewError::SuperConstructor)?;
    set_full_screen(canvas, true).map_err(GameCanvasNewError::SetFullScreen)?;
    Ok(())
}

pub fn game_canvas_key_jad_entry_as_int<GetAppProperty, E>(
    application: &ApplicationState,
    jad_entry: Option<&[u16]>,
    get_app_property: GetAppProperty,
) -> i32
where
    GetAppProperty: FnOnce(u32, Option<&[u16]>) -> Result<Option<alloc::vec::Vec<u16>>, E>,
{
    let Some(midlet) = application.midlet_instance else {
        return 0;
    };
    let Ok(property) = get_app_property(midlet, jad_entry) else {
        return 0;
    };
    property.as_deref().and_then(parse_java_i32).unwrap_or(0)
}

pub fn resource_url_encode(
    value: Option<&[u16]>,
) -> Result<alloc::vec::Vec<u16>, orphan_jvm::NullPointerException> {
    let value = value.ok_or(orphan_jvm::NullPointerException)?;
    let lower_hex_digit = |digit: u16| {
        if digit < 10 {
            0x0030 + digit
        } else {
            0x0061 + digit - 10
        }
    };
    let mut encoded = alloc::vec::Vec::new();
    for character in value {
        if (*character >= 0x0061 && *character <= 0x007a)
            || (*character >= 0x0041 && *character <= 0x005a)
            || (*character >= 0x0030 && *character <= 0x0039)
        {
            encoded.push(*character);
        } else {
            encoded.push(0x0025);
            if *character < 15 {
                encoded.push(0x0030);
            }
            let mut emitted = false;
            for shift in [12_u32, 8, 4, 0] {
                let digit = (*character >> shift) & 15;
                if digit != 0 || emitted || shift == 0 {
                    encoded.push(lower_hex_digit(digit));
                    emitted = true;
                }
            }
        }
    }
    Ok(encoded)
}

pub fn coded_string(
    data: Option<&[u8]>,
) -> Result<alloc::vec::Vec<u16>, orphan_jvm::NullPointerException> {
    let data = data.ok_or(orphan_jvm::NullPointerException)?;
    let lower_hex_digit = |digit: u16| {
        if digit < 10 {
            0x0030 + digit
        } else {
            0x0061 + digit - 10
        }
    };
    let mut result = alloc::vec::Vec::new();
    let mut column_offset = 0_i32;
    let mut line_count = 0_i32;
    let mut hex_column = alloc::vec::Vec::new();
    let mut ascii_column = alloc::vec::Vec::new();
    for byte in data {
        if line_count >= CODED_STRING_MAX_LINES {
            break;
        }
        let character = u16::from(*byte);
        if character < 0x0020 {
            ascii_column.push(0x002e);
        } else {
            ascii_column.push(character);
        }
        if character <= 15 {
            hex_column.push(0x0030);
        } else {
            hex_column.push(lower_hex_digit(character >> 4));
        }
        hex_column.push(lower_hex_digit(character & 15));
        hex_column.push(0x0020);
        column_offset += 1;
        if column_offset >= CODED_STRING_COLUMN_WIDTH {
            result.extend_from_slice(&hex_column);
            result.extend_from_slice(&[0x0020, 0x007c, 0x0020]);
            result.extend_from_slice(&ascii_column);
            result.push(0x000a);
            hex_column.clear();
            ascii_column.clear();
            column_offset = 0;
            line_count += 1;
        }
    }
    if column_offset > 0 {
        while column_offset < CODED_STRING_COLUMN_WIDTH {
            column_offset += 1;
            hex_column.extend_from_slice(&[0x0020, 0x0020, 0x0020]);
            ascii_column.push(0x0020);
        }
        result.extend_from_slice(&hex_column);
        result.extend_from_slice(&[0x0020, 0x007c, 0x0020]);
        result.extend_from_slice(&ascii_column);
        result.push(0x000a);
    }
    Ok(result)
}

pub fn resource_restart_importants(state: &mut ApplicationState) {
    state.resource_importants = Some(alloc::vec::Vec::new());
}

pub fn reset_load(state: &mut ApplicationState) -> Result<(), orphan_jvm::NullPointerException> {
    state.load_thread = None;
    state.loading_mode = -1;
    state
        .resources_to_download
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?
        .clear();
    Ok(())
}

pub fn load_request_resource_path_for_string(
    state: &ApplicationState,
    resource_type: i32,
    resource_id: Option<&[u16]>,
) -> Option<alloc::vec::Vec<u16>> {
    load_request_resource_path(state, resource_type, 0, resource_id, 0)
}

pub fn game_language_path(state: &ApplicationState) -> alloc::vec::Vec<u16> {
    let mut path = alloc::vec::Vec::new();
    if let Some(game_id) = state.game_id.as_deref() {
        path.extend(game_id.iter().copied());
    } else {
        path.extend(b"null".iter().map(|byte| u16::from(*byte)));
    }
    path.extend(b"/lan/".iter().map(|byte| u16::from(*byte)));
    path
}

pub fn get_game_text(
    state: &ApplicationState,
    index: i32,
) -> Result<Option<alloc::vec::Vec<u16>>, orphan_jvm::NullPointerException> {
    if index < 0 {
        return Ok(Some(alloc::vec![0x003f; 3]));
    }
    let game_texts = state
        .game_texts
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    if index as usize >= game_texts.len() {
        Ok(Some(alloc::vec![0x003f; 3]))
    } else {
        Ok(game_texts[index as usize].clone())
    }
}

pub fn get_game_text_from_string(
    state: &ApplicationState,
    index: Option<&[u16]>,
) -> Option<alloc::vec::Vec<u16>> {
    let unknown_text = || Some(alloc::vec![0x003f; 3]);
    let Some(index) = index else {
        return unknown_text();
    };
    if index.is_empty() {
        return unknown_text();
    }
    let (negative, digits) = match index[0] {
        0x002d => (true, &index[1..]),
        0x002b => (false, &index[1..]),
        _ => (false, index),
    };
    if digits.is_empty() {
        return unknown_text();
    }
    let digit_zeroes = [
        0x0030, 0x0660, 0x06f0, 0x07c0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66,
        0x0ce6, 0x0d66, 0x0de6, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x1090, 0x17e0, 0x1810, 0x1946,
        0x19d0, 0x1a80, 0x1a90, 0x1b50, 0x1bb0, 0x1c40, 0x1c50, 0xa620, 0xa8d0, 0xa900, 0xa9d0,
        0xa9f0, 0xaa50, 0xabf0, 0xff10,
    ];
    let limit = if negative {
        2_147_483_648_u64
    } else {
        2_147_483_647_u64
    };
    let mut magnitude = 0_u64;
    for unit in digits {
        let decimal = digit_zeroes.iter().find_map(|zero| {
            if *unit >= *zero && *unit <= *zero + 9 {
                Some(u64::from(*unit - *zero))
            } else {
                None
            }
        });
        let digit = decimal.or_else(|| match *unit {
            0x0041..=0x005a => Some(u64::from(*unit - 0x0041 + 10)),
            0x0061..=0x007a => Some(u64::from(*unit - 0x0061 + 10)),
            0xff21..=0xff3a => Some(u64::from(*unit - 0xff21 + 10)),
            0xff41..=0xff5a => Some(u64::from(*unit - 0xff41 + 10)),
            _ => None,
        });
        let Some(digit) = digit else {
            return unknown_text();
        };
        if digit >= 35 || magnitude > (limit - digit) / 35 {
            return unknown_text();
        }
        magnitude = magnitude * 35 + digit;
    }
    let parsed_index = if negative {
        (0_i64 - magnitude as i64) as i32
    } else {
        magnitude as i32
    };
    get_game_text(state, parsed_index).unwrap_or_else(|_| unknown_text())
}

pub fn text_replace_first(
    haystack: Option<&[u16]>,
    needle: Option<&[u16]>,
    replacement: Option<&[u16]>,
) -> Result<alloc::vec::Vec<u16>, orphan_jvm::NullPointerException> {
    let haystack = haystack.ok_or(orphan_jvm::NullPointerException)?;
    let needle = needle.ok_or(orphan_jvm::NullPointerException)?;
    let match_index = if needle.is_empty() {
        Some(0)
    } else if needle.len() > haystack.len() {
        None
    } else {
        haystack
            .windows(needle.len())
            .position(|window| window == needle)
    };
    let Some(match_index) = match_index else {
        return Ok(haystack.to_vec());
    };
    let mut result = alloc::vec::Vec::new();
    result.extend_from_slice(&haystack[..match_index]);
    if let Some(replacement) = replacement {
        result.extend_from_slice(replacement);
    } else {
        result.extend(b"null".iter().map(|byte| u16::from(*byte)));
    }
    result.extend_from_slice(&haystack[match_index + needle.len()..]);
    Ok(result)
}

pub fn remove_string_prefix(
    strings: Option<&[Option<alloc::vec::Vec<u16>>]>,
    prefix: Option<&[u16]>,
) -> Result<Option<alloc::vec::Vec<Option<alloc::vec::Vec<u16>>>>, orphan_jvm::NullPointerException>
{
    let Some(strings) = strings else {
        return Ok(None);
    };
    if prefix.is_none() || prefix.is_some_and(<[u16]>::is_empty) {
        return Ok(Some(strings.to_vec()));
    }
    let prefix = prefix.expect("non-null prefix established by preceding branch");
    let mut stripped_strings = alloc::vec![None; strings.len()];
    for (index, string) in strings.iter().enumerate() {
        let string = string.as_deref().ok_or(orphan_jvm::NullPointerException)?;
        if string.starts_with(prefix) {
            stripped_strings[index] = Some(string[prefix.len()..].to_vec());
        }
    }
    Ok(Some(stripped_strings))
}

pub fn get_language_selection_position(
    state: &ApplicationState,
    language: Option<&[u16]>,
) -> Result<i32, orphan_jvm::NullPointerException> {
    let languages = state
        .languages
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    for (index, candidate) in languages.iter().enumerate() {
        let language = language.ok_or(orphan_jvm::NullPointerException)?;
        if candidate.as_deref() == Some(language) {
            return Ok(index as i32);
        }
    }
    Ok(0)
}

pub fn resource_heap_index(
    state: &ApplicationState,
    source: i32,
) -> Result<i32, orphan_jvm::ArrayAccessException> {
    let heap_source = *orphan_jvm::array_ref(state.resource_heap_sources.as_deref(), 0)?;
    if source == heap_source {
        Ok(0)
    } else {
        Ok(-1)
    }
}

pub fn random_scaled(
    state: &ApplicationState,
    next_value: i32,
    scale: i32,
) -> Result<i32, orphan_jvm::NullPointerException> {
    state
        .random_instance
        .ok_or(orphan_jvm::NullPointerException)?;
    let unsigned_value = i64::from(next_value as u32);
    Ok(((unsigned_value * i64::from(scale)) >> 32) as i32)
}

pub fn ink_server_get_variable<'a>(
    state: &'a ApplicationState,
    name: Option<&[u16]>,
) -> Result<Option<&'a [u16]>, orphan_jvm::NullPointerException> {
    let variables = state
        .ink_server_variables
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    let name = name.ok_or(orphan_jvm::NullPointerException)?;
    Ok(variables
        .iter()
        .find(|(candidate, _)| candidate.as_slice() == name)
        .map(|(_, value)| value.as_slice()))
}

pub fn ink_server_get_hint<'a>(
    state: &'a ApplicationState,
    name: Option<&[u16]>,
) -> Result<Option<&'a [u16]>, orphan_jvm::NullPointerException> {
    let hints = state
        .ink_server_hints
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    let name = name.ok_or(orphan_jvm::NullPointerException)?;
    Ok(hints
        .iter()
        .find(|(candidate, _)| candidate.as_slice() == name)
        .map(|(_, value)| value.as_slice()))
}

pub fn ink_server_set_variable(
    state: &mut ApplicationState,
    name: Option<&[u16]>,
    value: Option<&[u16]>,
    hint: Option<&[u16]>,
) -> Result<(), orphan_jvm::NullPointerException> {
    let Some(value) = value else {
        return ink_server_unset_variable(state, name);
    };
    let variables = state
        .ink_server_variables
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?;
    let name = name.ok_or(orphan_jvm::NullPointerException)?;
    if let Some((_, stored_value)) = variables
        .iter_mut()
        .find(|(candidate, _)| candidate.as_slice() == name)
    {
        *stored_value = value.to_vec();
    } else {
        variables.push((name.to_vec(), value.to_vec()));
    }
    let hints = state
        .ink_server_hints
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?;
    let hint = hint.ok_or(orphan_jvm::NullPointerException)?;
    if let Some((_, stored_hint)) = hints
        .iter_mut()
        .find(|(candidate, _)| candidate.as_slice() == name)
    {
        *stored_hint = hint.to_vec();
    } else {
        hints.push((name.to_vec(), hint.to_vec()));
    }
    state.game_changed_since_last_save = true;
    Ok(())
}

pub fn ink_server_unset_variable(
    state: &mut ApplicationState,
    name: Option<&[u16]>,
) -> Result<(), orphan_jvm::NullPointerException> {
    let variables = state
        .ink_server_variables
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?;
    let name = name.ok_or(orphan_jvm::NullPointerException)?;
    if let Some(index) = variables
        .iter()
        .position(|(candidate, _)| candidate.as_slice() == name)
    {
        variables.remove(index);
    }
    let hints = state
        .ink_server_hints
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?;
    if let Some(index) = hints
        .iter()
        .position(|(candidate, _)| candidate.as_slice() == name)
    {
        hints.remove(index);
    }
    state.game_changed_since_last_save = true;
    Ok(())
}

pub fn reset_variable_system(
    state: &mut ApplicationState,
) -> Result<(), orphan_jvm::NullPointerException> {
    state
        .ink_server_variables
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?
        .clear();
    state
        .ink_server_hints
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?
        .clear();
    Ok(())
}

pub fn room_history_size(
    state: &ApplicationState,
) -> Result<i32, orphan_jvm::NullPointerException> {
    let name = [
        u16::from(b'r'),
        u16::from(b'o'),
        u16::from(b'o'),
        u16::from(b'm'),
        u16::from(b'-'),
        u16::from(b's'),
        u16::from(b'i'),
        u16::from(b'z'),
        u16::from(b'e'),
    ];
    let value = ink_server_get_variable(state, Some(&name))?;
    Ok(match value {
        Some(value) => to_int(JavaObject::String(value)),
        None => to_int(JavaObject::Null),
    })
}

pub fn inventory_size(
    state: &ApplicationState,
    item_id: Option<&[u16]>,
) -> Result<i32, orphan_jvm::NullPointerException> {
    let mut name = alloc::vec::Vec::new();
    name.extend(b"inv-".iter().map(|byte| u16::from(*byte)));
    if let Some(item_id) = item_id {
        name.extend_from_slice(item_id);
    } else {
        name.extend(b"null".iter().map(|byte| u16::from(*byte)));
    }
    let amount = ink_server_get_variable(state, Some(&name))?;
    Ok(match amount {
        Some(amount) => to_int(JavaObject::String(amount)),
        None => 0,
    })
}

pub fn room_current(
    state: &ApplicationState,
) -> Result<alloc::vec::Vec<u16>, orphan_jvm::NullPointerException> {
    let name = [
        u16::from(b'c'),
        u16::from(b'u'),
        u16::from(b'r'),
        u16::from(b'-'),
        u16::from(b'c'),
        u16::from(b'u'),
        u16::from(b'r'),
        u16::from(b'R'),
        u16::from(b'o'),
        u16::from(b'o'),
        u16::from(b'm'),
    ];
    Ok(ink_server_get_variable(state, Some(&name))?
        .map(<[u16]>::to_vec)
        .unwrap_or_default())
}

pub fn room_set_current(
    state: &mut ApplicationState,
    room_id: Option<&[u16]>,
) -> Result<(), orphan_jvm::NullPointerException> {
    let name = [
        u16::from(b'c'),
        u16::from(b'u'),
        u16::from(b'r'),
        u16::from(b'-'),
        u16::from(b'c'),
        u16::from(b'u'),
        u16::from(b'r'),
        u16::from(b'R'),
        u16::from(b'o'),
        u16::from(b'o'),
        u16::from(b'm'),
    ];
    let hint = [u16::from(b'C')];
    ink_server_set_variable(state, Some(&name), room_id, Some(&hint))
}

pub fn room_last_in_history(
    state: &ApplicationState,
) -> Result<Option<alloc::vec::Vec<u16>>, orphan_jvm::NullPointerException> {
    let mut name = alloc::vec::Vec::new();
    name.extend(b"room-".iter().map(|byte| u16::from(*byte)));
    name.extend(room_history_size(state)?.to_string().encode_utf16());
    Ok(ink_server_get_variable(state, Some(&name))?.map(<[u16]>::to_vec))
}

pub fn room_add_to_history(
    state: &mut ApplicationState,
    room_id: Option<&[u16]>,
) -> Result<(), orphan_jvm::NullPointerException> {
    let last_room = room_last_in_history(state)?;
    let room_id = room_id.ok_or(orphan_jvm::NullPointerException)?;
    if last_room.as_deref() == Some(room_id) {
        return Ok(());
    }
    let history_size = orphan_jvm::i32_add(room_history_size(state)?, 1);
    let mut room_name = alloc::vec::Vec::new();
    room_name.extend(b"room-".iter().map(|byte| u16::from(*byte)));
    room_name.extend(history_size.to_string().encode_utf16());
    let room_hint = [u16::from(b'R')];
    ink_server_set_variable(state, Some(&room_name), Some(room_id), Some(&room_hint))?;
    let history_size_name = [
        u16::from(b'r'),
        u16::from(b'o'),
        u16::from(b'o'),
        u16::from(b'm'),
        u16::from(b'-'),
        u16::from(b's'),
        u16::from(b'i'),
        u16::from(b'z'),
        u16::from(b'e'),
    ];
    let history_size_text = history_size.to_string();
    ink_server_set_variable(
        state,
        Some(&history_size_name),
        Some(
            &history_size_text
                .encode_utf16()
                .collect::<alloc::vec::Vec<_>>(),
        ),
        Some(&room_hint),
    )
}

pub fn room_remove_last_from_history(
    state: &mut ApplicationState,
) -> Result<(), orphan_jvm::NullPointerException> {
    let history_size = room_history_size(state)?;
    if history_size > 0 {
        let mut room_name = alloc::vec::Vec::new();
        room_name.extend(b"room-".iter().map(|byte| u16::from(*byte)));
        room_name.extend(room_history_size(state)?.to_string().encode_utf16());
        ink_server_unset_variable(state, Some(&room_name))?;
        let history_size_name = [
            u16::from(b'r'),
            u16::from(b'o'),
            u16::from(b'o'),
            u16::from(b'm'),
            u16::from(b'-'),
            u16::from(b's'),
            u16::from(b'i'),
            u16::from(b'z'),
            u16::from(b'e'),
        ];
        let history_size_text = orphan_jvm::i32_sub(history_size, 1).to_string();
        let history_size_units = history_size_text
            .encode_utf16()
            .collect::<alloc::vec::Vec<_>>();
        let room_hint = [u16::from(b'R')];
        ink_server_set_variable(
            state,
            Some(&history_size_name),
            Some(&history_size_units),
            Some(&room_hint),
        )?;
    }
    Ok(())
}

pub fn inventory_set(
    state: &mut ApplicationState,
    item_id: Option<&[u16]>,
    amount: i32,
) -> Result<(), orphan_jvm::NullPointerException> {
    let mut name = alloc::vec::Vec::new();
    name.extend(b"inv-".iter().map(|byte| u16::from(*byte)));
    if let Some(item_id) = item_id {
        name.extend_from_slice(item_id);
    } else {
        name.extend(b"null".iter().map(|byte| u16::from(*byte)));
    }
    let amount_text = amount.to_string();
    let amount_units = amount_text.encode_utf16().collect::<alloc::vec::Vec<_>>();
    let hint = [u16::from(b'V')];
    ink_server_set_variable(state, Some(&name), Some(&amount_units), Some(&hint))
}

pub fn inventory_remove(
    state: &mut ApplicationState,
    item_id: Option<&[u16]>,
) -> Result<(), orphan_jvm::NullPointerException> {
    let mut name = alloc::vec::Vec::new();
    name.extend(b"inv-".iter().map(|byte| u16::from(*byte)));
    if let Some(item_id) = item_id {
        name.extend_from_slice(item_id);
    } else {
        name.extend(b"null".iter().map(|byte| u16::from(*byte)));
    }
    ink_server_unset_variable(state, Some(&name))
}

pub fn ink_script_set_variable(
    state: &mut ApplicationState,
    variable_id: Option<&[u16]>,
    value: JavaObject<'_>,
) -> Result<(), orphan_jvm::NullPointerException> {
    match value {
        JavaObject::Null | JavaObject::Other => Ok(()),
        JavaObject::String(text) => {
            let hint = [u16::from(b'S')];
            ink_server_set_variable(state, variable_id, Some(text), Some(&hint))
        }
        JavaObject::Integer(0) => ink_server_unset_variable(state, variable_id),
        JavaObject::Integer(integer) => {
            let text = integer.to_string();
            let text = text.encode_utf16().collect::<alloc::vec::Vec<_>>();
            let hint = [u16::from(b'I')];
            ink_server_set_variable(state, variable_id, Some(&text), Some(&hint))
        }
    }
}

pub fn ink_script_get_variable<'a>(
    state: &'a ApplicationState,
    variable_id: Option<&[u16]>,
) -> Result<JavaObject<'a>, InkVariableError> {
    let value =
        ink_server_get_variable(state, variable_id).map_err(|_| InkVariableError::NullPointer)?;
    let Some(value) = value else {
        return Ok(JavaObject::Null);
    };
    let type_hint = ink_server_get_hint(state, variable_id)
        .map_err(|_| InkVariableError::NullPointer)?
        .ok_or(InkVariableError::NullPointer)?;
    let first_hint = *type_hint
        .first()
        .ok_or(InkVariableError::StringIndexOutOfBounds)?;
    if first_hint == u16::from(b'S') {
        return Ok(JavaObject::String(value));
    }
    let second_hint = *type_hint
        .first()
        .ok_or(InkVariableError::StringIndexOutOfBounds)?;
    if second_hint == u16::from(b'I') {
        let integer = parse_java_i32(value).ok_or(InkVariableError::NumberFormat)?;
        return Ok(JavaObject::Integer(integer));
    }
    Ok(JavaObject::Null)
}

pub fn ink_script_get_variable_as_integer(
    state: &ApplicationState,
    variable_id: Option<&[u16]>,
) -> Result<i32, InkVariableError> {
    match ink_script_get_variable(state, variable_id)? {
        JavaObject::Null => Ok(0),
        JavaObject::Integer(integer) => Ok(integer),
        JavaObject::String(text) => Ok(parse_java_i32(text).unwrap_or(1)),
        JavaObject::Other => Ok(1),
    }
}

pub fn load_request_resource_path_for_object(
    state: &ApplicationState,
    integer_id: Option<i32>,
    string_id: Option<&[u16]>,
    string_cast_succeeds: bool,
    _image_transform: i32,
) -> Result<Option<alloc::vec::Vec<u16>>, orphan_jvm::ClassCastException> {
    if let Some(integer_id) = integer_id {
        Ok(load_request_resource_path(state, 3, integer_id, None, 0))
    } else if string_cast_succeeds {
        Ok(load_request_resource_path(state, 2, -1, string_id, 0))
    } else {
        Err(orphan_jvm::ClassCastException)
    }
}

pub fn load_request_resource_path(
    state: &ApplicationState,
    resource_type: i32,
    integer_id: i32,
    string_id: Option<&[u16]>,
    image_transform: i32,
) -> Option<alloc::vec::Vec<u16>> {
    let append_ascii = |path: &mut alloc::vec::Vec<u16>, text: &[u8]| {
        path.extend(text.iter().map(|byte| u16::from(*byte)));
    };
    let append_nullable = |path: &mut alloc::vec::Vec<u16>, text: Option<&[u16]>| {
        if let Some(text) = text {
            path.extend(text.iter().copied());
        } else {
            append_ascii(path, b"null");
        }
    };
    let append_integer = |path: &mut alloc::vec::Vec<u16>, value: i32| {
        let decimal = value.to_string();
        path.extend(decimal.encode_utf16());
    };

    match resource_type {
        1 => {
            let mut path = alloc::vec::Vec::new();
            append_nullable(&mut path, state.game_id.as_deref());
            append_ascii(&mut path, b"/scr/");
            append_nullable(&mut path, string_id);
            append_ascii(&mut path, b".bin");
            Some(path)
        }
        2 => {
            let mut path = alloc::vec::Vec::new();
            append_nullable(&mut path, state.game_id.as_deref());
            append_ascii(&mut path, b"/gfx/transform");
            append_integer(&mut path, image_transform);
            append_ascii(&mut path, b"/");
            append_nullable(&mut path, string_id);
            append_ascii(&mut path, b".png");
            Some(path)
        }
        3 => {
            let mut path = alloc::vec::Vec::new();
            append_nullable(&mut path, state.game_id.as_deref());
            append_ascii(&mut path, b"/gfx/transform");
            append_integer(&mut path, image_transform);
            append_ascii(&mut path, b"/");
            append_integer(&mut path, integer_id);
            append_ascii(&mut path, b".png");
            Some(path)
        }
        4 => {
            let mut path = alloc::vec::Vec::new();
            append_nullable(&mut path, state.game_id.as_deref());
            append_ascii(&mut path, b"/sfx/mid/");
            append_nullable(&mut path, string_id);
            append_ascii(&mut path, b".mid");
            Some(path)
        }
        5 => {
            let mut path = alloc::vec::Vec::new();
            append_nullable(&mut path, state.game_id.as_deref());
            append_ascii(&mut path, b"/rom/");
            append_nullable(&mut path, string_id);
            append_ascii(&mut path, b".bin");
            Some(path)
        }
        _ => None,
    }
}

pub fn resource_request_description() -> Option<&'static [u16]> {
    None
}

pub fn resource_request_new_for_string(
    resource_type: i32,
    resource_id: Option<&[u16]>,
) -> ResourceRequestState {
    ResourceRequestState {
        resource_type,
        integer_id: 0,
        string_id: resource_id.map(<[u16]>::to_vec),
        image_transform: 0,
    }
}

pub fn resource_request_new_for_object(
    integer_id: Option<i32>,
    string_id: Option<&[u16]>,
    string_cast_succeeds: bool,
    _image_transform: i32,
) -> Result<ResourceRequestState, orphan_jvm::ClassCastException> {
    let mut request = ResourceRequestState {
        resource_type: 0,
        integer_id: 0,
        string_id: None,
        image_transform: 0,
    };
    if let Some(integer_id) = integer_id {
        request.integer_id = integer_id;
        request.resource_type = 3;
    } else if string_cast_succeeds {
        request.string_id = string_id.map(<[u16]>::to_vec);
        request.resource_type = 2;
    } else {
        return Err(orphan_jvm::ClassCastException);
    }
    Ok(request)
}

pub fn game_resource_new(
    resource_type: i32,
    id: Option<JavaResourceId>,
    image_transform: i32,
) -> GameResourceState {
    GameResourceState {
        resource_type,
        id,
        image: None,
        image_width: 0,
        image_height: 0,
        image_registration_x: 0,
        image_registration_y: 0,
        image_transform,
    }
}

pub fn game_resource_initialize(statics: &mut GameResourceStatics) {
    statics.cached_images = Some(alloc::vec::Vec::new());
    statics.important_images = Some(alloc::vec::Vec::new());
}

pub fn game_resource_equals(
    state: &GameResourceState,
    candidate: Option<&GameResourceState>,
) -> Result<bool, orphan_jvm::NullPointerException> {
    let Some(candidate) = candidate else {
        return Ok(false);
    };
    if candidate.resource_type != state.resource_type {
        return Ok(false);
    }
    let candidate_id = candidate
        .id
        .as_ref()
        .ok_or(orphan_jvm::NullPointerException)?;
    let ids_equal = match (candidate_id, state.id.as_ref()) {
        (JavaResourceId::Integer(left), Some(JavaResourceId::Integer(right))) => left == right,
        (JavaResourceId::String(left), Some(JavaResourceId::String(right))) => left == right,
        (JavaResourceId::Opaque(left), Some(JavaResourceId::Opaque(right))) => left == right,
        _ => false,
    };
    if !ids_equal {
        return Ok(false);
    }
    Ok(candidate.image_transform == state.image_transform)
}

#[allow(clippy::too_many_arguments)]
pub fn game_resource_paint<DrawImage, DrawRegion>(
    state: &GameResourceState,
    canvas: &GameCanvasState,
    graphics: Option<u32>,
    x: i32,
    y: i32,
    transform: i32,
    mut draw_image: DrawImage,
    mut draw_region: DrawRegion,
) -> Result<(), orphan_jvm::ArrayAccessException>
where
    DrawImage: FnMut(u32, u32, i32, i32, i32) -> Result<(), orphan_jvm::ArrayAccessException>,
    DrawRegion: FnMut(
        u32,
        u32,
        i32,
        i32,
        i32,
        i32,
        i32,
        i32,
        i32,
        i32,
    ) -> Result<(), orphan_jvm::ArrayAccessException>,
{
    let Some(image) = state.image else {
        return Ok(());
    };
    let left = get_left(
        x,
        state.image_width,
        state.image_height,
        state.image_registration_x,
        state.image_registration_y,
        transform,
    );
    let top = get_top(
        y,
        state.image_width,
        state.image_height,
        state.image_registration_x,
        state.image_registration_y,
        transform,
    );
    if transform == 0 {
        let graphics = graphics.ok_or(orphan_jvm::ArrayAccessException::NullPointer(
            orphan_jvm::NullPointerException,
        ))?;
        draw_image(graphics, image, left, top, 20)
    } else {
        let image_width = state.image_width;
        let image_height = state.image_height;
        let manipulation = *orphan_jvm::array_ref(Some(&canvas.transform_table), transform)?;
        let graphics = graphics.ok_or(orphan_jvm::ArrayAccessException::NullPointer(
            orphan_jvm::NullPointerException,
        ))?;
        draw_region(
            graphics,
            image,
            0,
            0,
            image_width,
            image_height,
            manipulation,
            left,
            top,
            20,
        )
    }
}

pub fn game_resource_paint_simple<F>(
    state: &GameResourceState,
    graphics: Option<u32>,
    x: i32,
    y: i32,
    anchor: i32,
    mut draw_image: F,
) -> Result<(), orphan_jvm::NullPointerException>
where
    F: FnMut(u32, Option<u32>, i32, i32, i32) -> Result<(), orphan_jvm::NullPointerException>,
{
    let image = state.image;
    let graphics = graphics.ok_or(orphan_jvm::NullPointerException)?;
    draw_image(graphics, image, x, y, anchor)
}

pub fn read_string(input: Option<&mut orphan_formats::Reader<'_>>) -> alloc::vec::Vec<u16> {
    let mut result = alloc::vec::Vec::new();
    let Some(input) = input else {
        loop {
            core::hint::spin_loop();
        }
    };
    loop {
        let character = match input.u8() {
            Ok(character) => (character as i8 as i16) as u16,
            Err(_) => {
                core::hint::spin_loop();
                continue;
            }
        };
        if character == 0 {
            return result;
        }
        result.push(character);
    }
}

pub fn find(mut input: Option<&mut orphan_formats::Reader<'_>>, target: Option<&[u16]>) -> i32 {
    let mut window = alloc::vec::Vec::new();
    let Some(target) = target else {
        return 0;
    };
    window.resize(target.len(), 0x0020);
    while window.as_slice() != target {
        let Some(input) = input.as_deref_mut() else {
            return 0;
        };
        let byte = match input.u8() {
            Ok(byte) => byte,
            Err(_) => return 0,
        };
        window.push(u16::from(byte));
        window.remove(0);
    }
    0
}

pub fn write_string<F>(mut write: F, value: Option<&[u16]>)
where
    F: FnMut(i32) -> bool,
{
    let Some(value) = value else {
        return;
    };
    for character in value {
        let signed_byte = i32::from((*character as u8) as i8);
        if !write(signed_byte) {
            return;
        }
    }
    let _ = write(0);
}

fn read_modified_utf(input: &mut orphan_formats::Reader<'_>) -> Option<alloc::vec::Vec<u16>> {
    let byte_count = (usize::from(input.u8().ok()?) << 8) | usize::from(input.u8().ok()?);
    let mut encoded = alloc::vec::Vec::with_capacity(byte_count);
    for _ in 0..byte_count {
        encoded.push(input.u8().ok()?);
    }

    let mut decoded = alloc::vec::Vec::with_capacity(byte_count);
    let mut offset = 0_usize;
    while offset < encoded.len() {
        let first = encoded[offset];
        match first >> 4 {
            0..=7 => {
                offset += 1;
                decoded.push(u16::from(first));
            }
            12 | 13 => {
                if offset + 1 >= encoded.len() {
                    return None;
                }
                let second = encoded[offset + 1];
                if second & 0xc0 != 0x80 {
                    return None;
                }
                offset += 2;
                decoded.push((u16::from(first & 0x1f) << 6) | u16::from(second & 0x3f));
            }
            14 => {
                if offset + 2 >= encoded.len() {
                    return None;
                }
                let second = encoded[offset + 1];
                let third = encoded[offset + 2];
                if second & 0xc0 != 0x80 || third & 0xc0 != 0x80 {
                    return None;
                }
                offset += 3;
                decoded.push(
                    (u16::from(first & 0x0f) << 12)
                        | (u16::from(second & 0x3f) << 6)
                        | u16::from(third & 0x3f),
                );
            }
            _ => return None,
        }
    }
    Some(decoded)
}

pub fn read_string_list(
    state: &mut ApplicationState,
    input: Option<&mut orphan_formats::Reader<'_>>,
) -> Option<alloc::vec::Vec<Option<alloc::vec::Vec<u16>>>> {
    let input = input?;
    let count = usize::from(input.u8().ok()?);
    let mut strings = alloc::vec![None; count];
    for index in 0..count {
        let mut value = match read_modified_utf(input) {
            Some(value) => Some(value),
            None => return Some(strings),
        };
        if value
            .as_deref()
            .is_some_and(|value| value.first() == Some(&0x0024))
        {
            value = get_game_text_from_string(state, value.as_deref().map(|value| &value[1..]));
        }
        strings[index] = value;
        let Some(value) = strings[index].as_deref() else {
            return Some(strings);
        };
        if value
            == [
                0x0073, 0x0061, 0x0076, 0x0065, 0x0050, 0x006f, 0x0069, 0x006e, 0x0074,
            ]
            && state.loading_mode == 0
        {
            state.save_is_possible = true;
        }
    }
    Some(strings)
}

pub fn resource_request_create_from_input(
    input: Option<&mut orphan_formats::Reader<'_>>,
) -> Option<ResourceRequestState> {
    let input = input?;
    let request_type = i32::from(input.u8().ok()?);
    match request_type {
        2 => {
            let resource_id = read_string(Some(input));
            let image_transform = i32::from(input.u8().ok()?);
            resource_request_new_for_object(
                None,
                Some(resource_id.as_slice()),
                true,
                image_transform,
            )
            .ok()
        }
        3 => {
            let resource_id = i32::from(input.u8().ok()?);
            let image_transform = i32::from(input.u8().ok()?);
            resource_request_new_for_object(Some(resource_id), None, false, image_transform).ok()
        }
        _ => {
            let resource_id = read_string(Some(input));
            Some(resource_request_new_for_string(
                request_type,
                Some(resource_id.as_slice()),
            ))
        }
    }
}

pub fn resource_request_get_id(request: &ResourceRequestState) -> (bool, i32, Option<&[u16]>) {
    if request.resource_type == 3 {
        (true, request.integer_id, None)
    } else {
        (false, 0, request.string_id.as_deref())
    }
}

pub fn resource_request_equals(
    request: &ResourceRequestState,
    candidate: Option<&ResourceRequestState>,
) -> bool {
    let Some(candidate) = candidate else {
        return false;
    };
    if request.resource_type != candidate.resource_type {
        return false;
    }
    request.string_id.as_deref() == candidate.string_id.as_deref()
        && request.integer_id == candidate.integer_id
        && request.image_transform == candidate.image_transform
}

pub fn resource_request_resource_path(
    state: &ApplicationState,
    request: &ResourceRequestState,
) -> Option<alloc::vec::Vec<u16>> {
    load_request_resource_path(
        state,
        request.resource_type,
        request.integer_id,
        request.string_id.as_deref(),
        request.image_transform,
    )
}

pub fn resource_request_to_string(
    state: &ApplicationState,
    request: &ResourceRequestState,
) -> Option<alloc::vec::Vec<u16>> {
    resource_request_resource_path(state, request)
}

pub fn char_to_string(value: u16) -> [u16; 1] {
    [value]
}

pub fn resource_merge_sort_cmp(
    left: Option<&[u8]>,
    right: Option<&[u8]>,
) -> Result<bool, orphan_jvm::NullPointerException> {
    let left = left.ok_or(orphan_jvm::NullPointerException)?;
    let right = right.ok_or(orphan_jvm::NullPointerException)?;
    for index in 0..core::cmp::min(left.len(), right.len()) {
        let left_byte = left[index];
        let right_byte = right[index];
        if left_byte < right_byte {
            return Ok(true);
        }
        if left_byte > right_byte {
            return Ok(false);
        }
    }
    Ok(left.len() < right.len())
}

pub fn array_copy_string_handles(
    separate_source: Option<&[Option<i32>]>,
    source_start: i32,
    target: Option<&mut [Option<i32>]>,
    target_start: i32,
    size: i32,
    source_is_target: bool,
) -> Result<(), orphan_jvm::ArrayCopyException> {
    let target = target.ok_or(orphan_jvm::ArrayCopyException::NullPointer)?;
    let source_length = if source_is_target {
        target.len()
    } else {
        separate_source
            .ok_or(orphan_jvm::ArrayCopyException::NullPointer)?
            .len()
    };
    let source_end = i64::from(source_start) + i64::from(size);
    let target_end = i64::from(target_start) + i64::from(size);
    if source_start < 0
        || target_start < 0
        || size < 0
        || source_end > source_length as i64
        || target_end > target.len() as i64
    {
        return Err(orphan_jvm::ArrayCopyException::IndexOutOfBounds);
    }
    let source_start = source_start as usize;
    let source_end = source_end as usize;
    let target_start = target_start as usize;
    let target_end = target_end as usize;
    let copied = if source_is_target {
        target[source_start..source_end].to_vec()
    } else {
        let source = separate_source.ok_or(orphan_jvm::ArrayCopyException::NullPointer)?;
        source[source_start..source_end].to_vec()
    };
    target[target_start..target_end].clone_from_slice(&copied);
    Ok(())
}

fn parse_java_i32(text: &[u16]) -> Option<i32> {
    let digit_zeroes = [
        0x0030, 0x0660, 0x06f0, 0x07c0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66,
        0x0ce6, 0x0d66, 0x0de6, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x1090, 0x17e0, 0x1810, 0x1946,
        0x19d0, 0x1a80, 0x1a90, 0x1b50, 0x1bb0, 0x1c40, 0x1c50, 0xa620, 0xa8d0, 0xa900, 0xa9d0,
        0xa9f0, 0xaa50, 0xabf0, 0xff10,
    ];
    if text.is_empty() {
        return None;
    }
    let (negative, digits) = match text[0] {
        0x002d => (true, &text[1..]),
        0x002b => (false, &text[1..]),
        _ => (false, text),
    };
    if digits.is_empty() {
        return None;
    }
    let limit = if negative {
        2_147_483_648_u64
    } else {
        2_147_483_647_u64
    };
    let mut magnitude = 0_u64;
    for unit in digits {
        let digit = digit_zeroes.iter().find_map(|zero| {
            if *unit >= *zero && *unit <= *zero + 9 {
                Some(u64::from(*unit - *zero))
            } else {
                None
            }
        })?;
        if magnitude > (limit - digit) / 10 {
            return None;
        }
        magnitude = magnitude * 10 + digit;
    }
    if negative {
        Some((0_i64 - magnitude as i64) as i32)
    } else {
        Some(magnitude as i32)
    }
}

pub fn to_int(value: JavaObject<'_>) -> i32 {
    let parse_integer = |text: &[u16]| {
        let digit_zeroes = [
            0x0030, 0x0660, 0x06f0, 0x07c0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66,
            0x0ce6, 0x0d66, 0x0de6, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x1090, 0x17e0, 0x1810, 0x1946,
            0x19d0, 0x1a80, 0x1a90, 0x1b50, 0x1bb0, 0x1c40, 0x1c50, 0xa620, 0xa8d0, 0xa900, 0xa9d0,
            0xa9f0, 0xaa50, 0xabf0, 0xff10,
        ];
        if text.is_empty() {
            return None;
        }
        let (negative, digits) = match text[0] {
            0x002d => (true, &text[1..]),
            0x002b => (false, &text[1..]),
            _ => (false, text),
        };
        if digits.is_empty() {
            return None;
        }
        let limit = if negative {
            2_147_483_648_u64
        } else {
            2_147_483_647_u64
        };
        let mut magnitude = 0_u64;
        for unit in digits {
            let digit = digit_zeroes.iter().find_map(|zero| {
                if *unit >= *zero && *unit <= *zero + 9 {
                    Some(u64::from(*unit - *zero))
                } else {
                    None
                }
            })?;
            if magnitude > (limit - digit) / 10 {
                return None;
            }
            magnitude = magnitude * 10 + digit;
        }
        if negative {
            Some((0_i64 - magnitude as i64) as i32)
        } else {
            Some(magnitude as i32)
        }
    };

    match value {
        JavaObject::Null => 0,
        JavaObject::Integer(value) => value,
        JavaObject::String(value) => parse_integer(value).unwrap_or(1),
        JavaObject::Other => 1,
    }
}

pub fn to_boolean(value: JavaObject<'_>) -> bool {
    to_int(value) >= 1
}

pub fn ink_interpreter_integer_argument(value: JavaObject<'_>) -> i32 {
    to_int(value)
}

pub fn action_key_id_convert(id: Option<&[u16]>) -> Result<i32, orphan_jvm::NullPointerException> {
    let id = id.ok_or(orphan_jvm::NullPointerException)?;
    let equals_ascii = |ascii: &[u8]| {
        id.len() == ascii.len()
            && id
                .iter()
                .zip(ascii)
                .all(|(unit, byte)| *unit == u16::from(*byte))
    };
    if equals_ascii(b"actionkey_star") {
        return Ok(0);
    }
    if equals_ascii(b"actionkey_pound") {
        return Ok(1);
    }
    let number_prefix = b"actionkey_num";
    for index in 0..10 {
        if id.len() == number_prefix.len() + 1
            && id[..number_prefix.len()]
                .iter()
                .zip(number_prefix)
                .all(|(unit, byte)| *unit == u16::from(*byte))
            && id[number_prefix.len()] == u16::from(b'0' + index as u8)
        {
            return Ok(2 + index);
        }
    }
    Ok(-1)
}

pub fn tick_based_time(state: &ApplicationState) -> i32 {
    state.tick_based_time_value
}

pub fn tick_based_time_update(state: &mut ApplicationState) {
    state.tick_based_time_value = orphan_jvm::i32_add(state.tick_based_time_value, 60);
}

pub fn tick_based_time_reset(state: &mut ApplicationState) {
    state.tick_based_time_value = 0;
}

pub fn loading(state: &ApplicationState) -> bool {
    state.load_thread.is_some()
}

pub fn is_menu_scroll_allowed(state: &mut InkEngineState) -> bool {
    let previous = state.menu_scroll_tick_counter;
    state.menu_scroll_tick_counter = previous.wrapping_add(1);
    if previous < 1 {
        false
    } else {
        state.menu_scroll_tick_counter = 0;
        true
    }
}

#[allow(clippy::needless_range_loop)]
pub fn action_key_keycode_to_action_key(
    state: &InkEngineState,
    keycode: i32,
) -> Result<i32, orphan_jvm::NullPointerException> {
    let key_codes = state
        .action_key_key_codes
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    let mut action_key_index = -1;
    for index in 0..key_codes.len() {
        if keycode == key_codes[index] {
            action_key_index = index as i32;
            break;
        }
    }
    Ok(action_key_index)
}

pub fn action_key_unset_all_keys(state: &mut InkEngineState) {
    state.action_key_script_ids = Some(alloc::vec![None; 12]);
}

pub fn action_key_init_system(state: &mut InkEngineState) {
    action_key_unset_all_keys(state);
    state.action_key_key_codes = Some(alloc::vec![42, 35, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57]);
}

pub fn action_key_get_script_id(
    state: &InkEngineState,
    keycode: i32,
) -> Result<Option<&[u16]>, orphan_jvm::ArrayAccessException> {
    let action_key_index = action_key_keycode_to_action_key(state, keycode)
        .map_err(orphan_jvm::ArrayAccessException::NullPointer)?;
    if action_key_index != -1 {
        let script_id =
            orphan_jvm::array_ref(state.action_key_script_ids.as_deref(), action_key_index)?;
        return Ok(script_id.as_deref());
    }
    Ok(None)
}

pub fn splash_more_exists(state: &InkEngineState) -> bool {
    orphan_jvm::i32_add(state.current_splash, 1) <= state.number_of_splashes
}

pub fn key_init(
    engine: &InkEngineState,
    canvas: &mut GameCanvasState,
) -> Result<(), orphan_jvm::NullPointerException> {
    let setting = |name: &[u8]| -> Result<JavaObject<'_>, orphan_jvm::NullPointerException> {
        let settings = engine
            .settings_hash
            .as_deref()
            .ok_or(orphan_jvm::NullPointerException)?;
        let value = settings
            .iter()
            .find(|(candidate, _)| {
                candidate.len() == name.len()
                    && candidate
                        .iter()
                        .zip(name)
                        .all(|(unit, byte)| *unit == u16::from(*byte))
            })
            .map(|(_, value)| value);
        Ok(match value {
            None => JavaObject::Null,
            Some(JavaOwnedObject::Integer(value)) => JavaObject::Integer(*value),
            Some(JavaOwnedObject::String(value)) => JavaObject::String(value),
            Some(JavaOwnedObject::Other) => JavaObject::Other,
        })
    };
    canvas.key_softkey_left = to_int(setting(b"KEY_SOFTKEY_LEFT")?);
    canvas.key_softkey_right = to_int(setting(b"KEY_SOFTKEY_RIGHT")?);
    canvas.key_send = to_int(setting(b"KEY_SEND")?);
    canvas.key_return = to_int(setting(b"KEY_END")?);
    canvas.key_softkey_center = to_int(setting(b"KEY_SOFTKEY_THUMBSTICK")?);
    canvas.key_arrow_up = to_int(setting(b"KEY_UP_ARROW")?);
    canvas.key_arrow_down = to_int(setting(b"KEY_DOWN_ARROW")?);
    canvas.key_arrow_left = to_int(setting(b"KEY_LEFT_ARROW")?);
    canvas.key_arrow_right = to_int(setting(b"KEY_RIGHT_ARROW")?);
    canvas.key_erase = to_int(setting(b"KEY_CLEAR")?);
    Ok(())
}

pub fn key_convert_to_key_id(state: &GameCanvasState, key_code: i32) -> i32 {
    if key_code == 0 {
        return 0;
    }
    if key_code == state.key_softkey_left {
        return -6;
    }
    if key_code == state.key_softkey_right {
        return -7;
    }
    if key_code == state.key_softkey_center || key_code == 53 {
        return -5;
    }
    if key_code == state.key_arrow_up || key_code == 121 || key_code == 116 || key_code == 50 {
        return -1;
    }
    if key_code == state.key_arrow_down || key_code == 98 || key_code == 118 || key_code == 56 {
        return -2;
    }
    if key_code == state.key_arrow_left || key_code == 102 || key_code == 100 || key_code == 52 {
        return -3;
    }
    if key_code == state.key_arrow_right || key_code == 106 || key_code == 107 || key_code == 54 {
        return -4;
    }
    if key_code == state.key_return || key_code == state.key_erase {
        return 0;
    }
    if key_code == state.key_send {
        return -10;
    }
    if (48..=57).contains(&key_code) || key_code == 42 || key_code == 35 {
        return key_code;
    }
    0
}

pub fn set_key_status(
    application: &mut ApplicationState,
    engine: &mut InkEngineState,
    key: i32,
    status: bool,
) {
    application.key_new |= status;
    application.key_pressed = status;
    if status {
        application.key_last_pressed = key;
        engine.menu_scroll_tick_counter = 0;
    }
}

pub fn game_canvas_paint<G, R>(graphics: G, application_paint: impl FnOnce(G) -> R) -> R {
    application_paint(graphics)
}

pub fn game_canvas_show_notify<SetHide, ResumeSound, E>(
    set_hide: SetHide,
    resume_sound: ResumeSound,
) -> Result<(), E>
where
    SetHide: FnOnce(bool) -> Result<(), E>,
    ResumeSound: FnOnce() -> Result<(), E>,
{
    set_hide(false)?;
    resume_sound()
}

pub fn game_canvas_key_pressed(
    application: &mut ApplicationState,
    engine: &mut InkEngineState,
    canvas: &GameCanvasState,
    key: i32,
) {
    if application.loading_mode != -1
        || application.load_bar_active
        || application.goto_dissolve_fx_counter > -3
    {
        return;
    }
    set_key_status(
        application,
        engine,
        key_convert_to_key_id(canvas, key),
        true,
    );
}

pub fn game_canvas_key_released(
    application: &mut ApplicationState,
    engine: &mut InkEngineState,
    canvas: &GameCanvasState,
    key: i32,
) {
    if application.loading_mode != -1
        || application.load_bar_active
        || application.goto_dissolve_fx_counter > -3
    {
        return;
    }
    set_key_status(
        application,
        engine,
        key_convert_to_key_id(canvas, key),
        false,
    );
}

pub fn menu_get_choice_number(state: &MenuState) -> i32 {
    state.selected_choice_number
}

pub fn menu_add_choice(
    state: &mut MenuState,
    choice_id: Option<i32>,
    choice_text: Option<alloc::vec::Vec<u16>>,
) -> Result<(), orphan_jvm::NullPointerException> {
    state
        .choice_ids
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?
        .push(choice_id);
    state
        .choice_texts
        .as_mut()
        .ok_or(orphan_jvm::NullPointerException)?
        .push(choice_text);
    state.update_body_lines = true;
    state.update_menu = true;
    Ok(())
}

pub fn menu_add_choice_integer(
    state: &mut MenuState,
    choice_id: i32,
    choice_text: Option<alloc::vec::Vec<u16>>,
) -> Result<(), orphan_jvm::NullPointerException> {
    menu_add_choice(state, Some(choice_id), choice_text)
}

pub fn menu_count_choices(state: &MenuState) -> Result<i32, orphan_jvm::NullPointerException> {
    let choice_ids = state
        .choice_ids
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    Ok(choice_ids.len() as i32)
}

pub fn menu_get_choice_id(
    state: &MenuState,
) -> Result<Option<i32>, orphan_jvm::ArrayAccessException> {
    let choice_id =
        orphan_jvm::array_ref(state.choice_ids.as_deref(), state.selected_choice_number)?;
    Ok(*choice_id)
}

pub fn menu_next_choice(state: &mut MenuState) -> Result<(), orphan_jvm::NullPointerException> {
    let next_choice_index = orphan_jvm::i32_add(state.selected_choice_number, 1);
    state.selected_choice_number = next_choice_index;
    if next_choice_index >= menu_count_choices(state)? {
        state.selected_choice_number = 0;
        state.scroll = if state.scroll < 0 { 0 } else { state.scroll };
    }
    state.update_menu = true;
    Ok(())
}

pub fn menu_previous_choice(state: &mut MenuState) -> Result<(), orphan_jvm::NullPointerException> {
    let previous_choice_index = orphan_jvm::i32_sub(state.selected_choice_number, 1);
    state.selected_choice_number = previous_choice_index;
    if previous_choice_index < 0 {
        state.selected_choice_number = orphan_jvm::i32_sub(menu_count_choices(state)?, 1);
    }
    let scroll_increment = if state.scroll < 0 { 1 } else { 0 };
    state.scroll = orphan_jvm::i32_add(state.scroll, scroll_increment);
    state.update_menu = true;
    Ok(())
}

pub fn menu_set_position(state: &mut MenuState, x: i32, y: i32) {
    state.x = x;
    state.y = y;
}

pub fn menu_set_current(state: &mut MenuState, current: bool) {
    state.is_current = current;
}

pub fn menu_scroll_increase(state: &mut MenuState) {
    let increment = if state.scroll < 0 { 1 } else { 0 };
    state.scroll = orphan_jvm::i32_add(state.scroll, increment);
    state.text_scrolling = true;
    state.update_menu = true;
}

pub fn menu_scroll_decrease(state: &mut MenuState) {
    let decrement = if state.text_scrolling { 1 } else { 0 };
    state.scroll = orphan_jvm::i32_sub(state.scroll, decrement);
    state.update_menu = true;
}

pub fn menu_set_top(state: &mut MenuState, text: Option<alloc::vec::Vec<u16>>) {
    state.top_text = text;
    state.update_top_lines = true;
    state.update_menu = true;
}

pub fn menu_set_softkey_options(
    state: &mut MenuState,
    left_option: Option<alloc::vec::Vec<u16>>,
    right_option: Option<alloc::vec::Vec<u16>>,
) {
    state.engine_softkey_option_left = left_option;
    state.engine_softkey_option_right = right_option;
}

pub fn menu_set_inventory_item_resource(state: &mut MenuState, resource: Option<i32>) {
    state.current_inventory_item_resource = resource;
}

pub fn menu_active(statics: &MenuStatics) -> Result<bool, orphan_jvm::NullPointerException> {
    let stack = statics
        .stack
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    Ok(!stack.is_empty())
}

pub fn menu_initialize(statics: &mut MenuStatics) {
    statics.stack = Some(alloc::vec::Vec::new());
}

pub fn menu_close_all(statics: &mut MenuStatics) {
    statics.stack = Some(alloc::vec::Vec::new());
}

pub fn menu_close_current<F>(
    statics: &mut MenuStatics,
    mut set_current: F,
) -> Result<(), orphan_jvm::NullPointerException>
where
    F: FnMut(u32, bool) -> Result<(), orphan_jvm::NullPointerException>,
{
    if menu_active(statics)? {
        let current = menu_get_current(statics)?.ok_or(orphan_jvm::NullPointerException)?;
        let stack = statics
            .stack
            .as_mut()
            .ok_or(orphan_jvm::NullPointerException)?;
        if let Some(index) = stack.iter().position(|candidate| *candidate == current) {
            stack.remove(index);
        }
        if menu_active(statics)? {
            let current = menu_get_current(statics)?.ok_or(orphan_jvm::NullPointerException)?;
            set_current(current, true)?;
        }
    }
    Ok(())
}

pub fn menu_get_current(
    statics: &MenuStatics,
) -> Result<Option<u32>, orphan_jvm::NullPointerException> {
    let stack = statics
        .stack
        .as_deref()
        .ok_or(orphan_jvm::NullPointerException)?;
    if stack.is_empty() {
        return Ok(None);
    }
    Ok(stack.last().copied())
}

pub fn ink_script_stop(statics: &mut InkInterpreterStatics) {
    if statics.paused_thread.is_some() {
        statics.paused_thread = None;
    }
}

pub fn ink_script_initialize(statics: &mut InkScriptStatics) {
    statics.scripts = Some(alloc::vec::Vec::new());
}

pub fn ink_script_resume<Resume>(
    statics: &mut InkInterpreterStatics,
    mut resume: Resume,
) -> Result<(), orphan_jvm::ArrayAccessException>
where
    Resume: FnMut(
        &mut InkInterpreterStatics,
        u32,
    ) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
{
    if let Some(paused_thread) = statics.paused_thread {
        resume(statics, paused_thread)?;
    }
    Ok(())
}

pub fn ink_script_is_waiting<CurrentTimeMillis, Resume, E>(
    script_statics: &mut InkScriptStatics,
    interpreter_statics: &mut InkInterpreterStatics,
    current_time_millis: CurrentTimeMillis,
    resume: Resume,
) -> Result<bool, E>
where
    CurrentTimeMillis: FnOnce() -> i64,
    Resume: FnOnce(&mut InkScriptStatics, &mut InkInterpreterStatics) -> Result<(), E>,
{
    if script_statics.wait_stop > 0 && current_time_millis() < script_statics.wait_stop {
        return Ok(true);
    }
    if script_statics.wait_stop <= 0 {
        return Ok(false);
    }
    script_statics.wait_stop = 0;
    resume(script_statics, interpreter_statics)?;
    Ok(false)
}

pub fn ink_script_new(
    input: Option<&mut orphan_formats::Reader<'_>>,
    strings: Option<&[Option<alloc::vec::Vec<u16>>]>,
) -> InkScriptState {
    let mut state = InkScriptState {
        data: None,
        event_offsets: None,
        string_list: None,
        gfx_id: None,
    };
    let _ = (|| -> Result<(), ()> {
        state.string_list = strings.map(<[Option<alloc::vec::Vec<u16>>]>::to_vec);
        let input = input.ok_or(())?;
        match i32::from(input.u8().map_err(|_| ())?) {
            INK_SCRIPT_GFX_TYPE_STRING => {
                let string_index = i32::from(input.u8().map_err(|_| ())?);
                let zero_based_index = orphan_jvm::i32_sub(string_index, 1);
                state.gfx_id =
                    orphan_jvm::array_ref(state.string_list.as_deref(), zero_based_index)
                        .map_err(|_| ())?
                        .as_ref()
                        .map(|value| JavaOwnedObject::String(value.clone()));
            }
            INK_SCRIPT_GFX_TYPE_INTEGER => {
                let high = u16::from(input.u8().map_err(|_| ())?);
                let low = u16::from(input.u8().map_err(|_| ())?);
                state.gfx_id = Some(JavaOwnedObject::Integer(i32::from((high << 8) | low)));
            }
            _ => {}
        }
        state.event_offsets = Some(alloc::vec![-1; 57]);
        let event_count = input.u8().map_err(|_| ())?;
        for _ in 0..event_count {
            let event_code = i32::from(input.u8().map_err(|_| ())?);
            let high = u16::from(input.u8().map_err(|_| ())?);
            let low = u16::from(input.u8().map_err(|_| ())?);
            let event_offset = i32::from((high << 8) | low);
            *orphan_jvm::array_mut(state.event_offsets.as_deref_mut(), event_code)
                .map_err(|_| ())? = event_offset;
        }
        let high = u16::from(input.u8().map_err(|_| ())?);
        let low = u16::from(input.u8().map_err(|_| ())?);
        let data_length = usize::from((high << 8) | low);
        state.data = Some(alloc::vec![0; data_length]);
        for index in 0..data_length {
            let byte = input.u8().map_err(|_| ())?;
            state
                .data
                .as_mut()
                .expect("data was allocated before readFully")[index] = byte;
        }
        Ok(())
    })();
    state
}

pub fn ink_script_get_string(
    state: &InkScriptState,
    string_index: i32,
) -> Result<Option<&[u16]>, orphan_jvm::ArrayAccessException> {
    let zero_based_index = orphan_jvm::i32_sub(string_index, 1);
    let string = orphan_jvm::array_ref(state.string_list.as_deref(), zero_based_index)?;
    Ok(string.as_deref())
}

pub fn ink_script_has_event(
    state: &InkScriptState,
    event_code: i32,
) -> Result<bool, orphan_jvm::ArrayAccessException> {
    let event_offset = orphan_jvm::array_ref(state.event_offsets.as_deref(), event_code)?;
    Ok(*event_offset != -1)
}

#[allow(clippy::too_many_arguments)]
pub fn ink_script_execute_event<ExecuteCommand>(
    statics: &mut InkInterpreterStatics,
    script: &InkScriptState,
    script_handle: u32,
    interpreter: &mut InkInterpreterState,
    interpreter_handle: u32,
    event_code: i32,
    initial_value: Option<JavaOwnedObject>,
    room_object: Option<u32>,
    execute_command: ExecuteCommand,
) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>
where
    ExecuteCommand: FnMut(
        &mut InkInterpreterState,
    ) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
{
    ink_script_execute_event_debug(
        statics,
        script,
        script_handle,
        interpreter,
        interpreter_handle,
        event_code,
        initial_value,
        room_object,
        false,
        execute_command,
    )
}

pub fn ink_script_execute_event_by_id<ExecuteEvent, E>(
    statics: &InkScriptStatics,
    script_id: Option<&[u16]>,
    event_code: i32,
    initial_value: Option<JavaOwnedObject>,
    room_object: Option<u32>,
    execute_event: ExecuteEvent,
) -> Result<Option<JavaOwnedObject>, InkScriptExecuteEventError<E>>
where
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        Option<u32>,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    let scripts = statics
        .scripts
        .as_deref()
        .ok_or(InkScriptExecuteEventError::NullPointer)?;
    let script_id = script_id.ok_or(InkScriptExecuteEventError::NullPointer)?;
    let script = scripts
        .iter()
        .find(|(candidate, _)| candidate.as_slice() == script_id)
        .map(|(_, script)| script);
    let Some(script) = script else {
        return Ok(None);
    };
    let script_handle = match script {
        InkScriptRegistryValue::Script(script_handle) => *script_handle,
        InkScriptRegistryValue::Other => return Err(InkScriptExecuteEventError::ClassCast),
    };
    execute_event(script_handle, event_code, initial_value, room_object, false)
        .map_err(InkScriptExecuteEventError::Execute)
}

pub fn ink_script_get_item_name<ExecuteEvent, E>(
    statics: &InkScriptStatics,
    item_id: Option<&[u16]>,
    execute_event: ExecuteEvent,
) -> Result<Option<alloc::vec::Vec<u16>>, InkScriptGetItemNameError<E>>
where
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        Option<u32>,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    let item_name = ink_script_execute_event_by_id(
        statics,
        item_id,
        INK_EVENT_GET_NAME,
        Some(JavaOwnedObject::String(alloc::vec![u16::from(b'?')])),
        None,
        execute_event,
    )
    .map_err(InkScriptGetItemNameError::ExecuteEvent)?;
    match item_name {
        None => Ok(None),
        Some(JavaOwnedObject::String(item_name)) => Ok(Some(item_name)),
        Some(JavaOwnedObject::Integer(_) | JavaOwnedObject::Other) => {
            Err(InkScriptGetItemNameError::ClassCast)
        }
    }
}

#[allow(clippy::too_many_arguments)]
pub fn ink_script_execute_event_debug<ExecuteCommand>(
    statics: &mut InkInterpreterStatics,
    script: &InkScriptState,
    script_handle: u32,
    interpreter: &mut InkInterpreterState,
    interpreter_handle: u32,
    event_code: i32,
    initial_value: Option<JavaOwnedObject>,
    room_object: Option<u32>,
    language_debug_mode: bool,
    execute_command: ExecuteCommand,
) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>
where
    ExecuteCommand: FnMut(
        &mut InkInterpreterState,
    ) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
{
    if !ink_script_has_event(script, event_code)? {
        return Ok(initial_value);
    }
    let event_offset = *orphan_jvm::array_ref(script.event_offsets.as_deref(), event_code)?;
    *interpreter = ink_interpreter_new(Some(script_handle), event_offset, room_object);
    interpreter.language_debug_mode = language_debug_mode;
    ink_interpreter_execute(
        statics,
        interpreter,
        interpreter_handle,
        initial_value,
        execute_command,
    )
}

pub fn ink_interpreter_read(
    state: &mut InkInterpreterState,
    script: &InkScriptState,
) -> Result<i32, orphan_jvm::ArrayAccessException> {
    let _script_handle = state
        .script
        .ok_or(orphan_jvm::ArrayAccessException::NullPointer(
            orphan_jvm::NullPointerException,
        ))?;
    let script_data = script.data.as_deref();
    let current_offset = state.offset;
    state.offset = orphan_jvm::i32_add(current_offset, 1);
    let value = orphan_jvm::array_ref(script_data, current_offset)?;
    Ok(i32::from(*value))
}

pub fn ink_interpreter_new(
    script: Option<u32>,
    event_offset: i32,
    room_object: Option<u32>,
) -> InkInterpreterState {
    InkInterpreterState {
        script,
        status: 0,
        offset: event_offset,
        room_object,
        language_debug_mode: false,
    }
}

pub fn ink_interpreter_execute<ExecuteCommand>(
    statics: &mut InkInterpreterStatics,
    state: &mut InkInterpreterState,
    self_handle: u32,
    initial_value: Option<JavaOwnedObject>,
    mut execute_command: ExecuteCommand,
) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>
where
    ExecuteCommand: FnMut(
        &mut InkInterpreterState,
    ) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
{
    state.status = 1;
    let mut command_result = initial_value;
    let result = loop {
        let result = command_result;
        if state.status != 1 {
            break result;
        }
        command_result = execute_command(state)?;
    };
    if state.status == 4 || state.status == 5 {
        statics.paused_thread = Some(self_handle);
    }
    Ok(result)
}

pub fn ink_interpreter_resume<ExecuteCommand>(
    statics: &mut InkInterpreterStatics,
    state: &mut InkInterpreterState,
    self_handle: u32,
    execute_command: ExecuteCommand,
) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>
where
    ExecuteCommand: FnMut(
        &mut InkInterpreterState,
    ) -> Result<Option<JavaOwnedObject>, orphan_jvm::ArrayAccessException>,
{
    statics.paused_thread = None;
    ink_interpreter_execute(statics, state, self_handle, None, execute_command)
}

pub fn ink_interpreter_read_bytes(
    state: &mut InkInterpreterState,
    script: &InkScriptState,
    byte_count: i32,
) -> Result<i32, orphan_jvm::ArrayAccessException> {
    let mut value = 0;
    let mut byte_index = 0;
    while byte_index < byte_count {
        value = orphan_jvm::i32_add(
            orphan_jvm::i32_shl(value, 8),
            ink_interpreter_read(state, script)?,
        );
        byte_index = orphan_jvm::i32_add(byte_index, 1);
    }
    Ok(value)
}

pub fn ink_interpreter_read_signed(
    state: &mut InkInterpreterState,
    script: &InkScriptState,
    byte_count: i32,
) -> Result<i32, orphan_jvm::ArrayAccessException> {
    let mut value = ink_interpreter_read_bytes(state, script, byte_count)?;
    let sign_bit = orphan_jvm::i32_shl(
        1,
        orphan_jvm::i32_sub(orphan_jvm::i32_shl(byte_count, 3), 1),
    );
    if value & sign_bit != 0 {
        value = orphan_jvm::i32_sub(value, orphan_jvm::i32_add(sign_bit, sign_bit));
    }
    Ok(value)
}

pub fn ink_interpreter_has_command(
    state: &mut InkInterpreterState,
    script: &InkScriptState,
    target_command: i32,
) -> Result<bool, orphan_jvm::ArrayAccessException> {
    let command_integer = 5;
    let command_string = 6;
    let command_end = 2;
    let command_byte = ink_interpreter_read(state, script)?;
    let command = command_byte & 63;
    if command == target_command {
        return Ok(true);
    }
    let mut argument_count = command_byte >> 6;
    if argument_count == 3 {
        argument_count = ink_interpreter_read(state, script)?;
    }
    let mut command_found = false;
    if command == command_integer {
        if argument_count == 0 {
            let _value = ink_interpreter_read_signed(state, script, 1)?;
            return Ok(false);
        }
        if argument_count == 1 {
            let _value = ink_interpreter_read_signed(state, script, 2)?;
            return Ok(false);
        }
        let _value = ink_interpreter_read_signed(state, script, 4)?;
        return Ok(false);
    }
    if command == command_string {
        let string_index = ink_interpreter_read(state, script)?;
        let _string = ink_script_get_string(script, string_index)?;
        return Ok(false);
    }
    if command == command_end {
        return Ok(false);
    }
    let mut argument_index = 0;
    while argument_index < argument_count {
        command_found = ink_interpreter_has_command(state, script, target_command)?;
        if command_found {
            return Ok(true);
        }
        argument_index = orphan_jvm::i32_add(argument_index, 1);
    }
    Ok(command_found)
}

pub fn ink_engine_inventory_equip_unequip_handling<ExecuteEvent, E>(
    menus: &mut MenuStatics,
    scripts: &InkScriptStatics,
    choice_id: i32,
    execute_event: ExecuteEvent,
) -> Result<(), InkScriptExecuteEventError<E>>
where
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        Option<u32>,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    menu_close_all(menus);
    ink_script_execute_event_by_id(
        scripts,
        scripts.item_id.as_deref(),
        choice_id,
        None,
        None,
        execute_event,
    )?;
    Ok(())
}

pub fn ink_script_has_command(
    script: &InkScriptState,
    target_command: i32,
) -> Result<bool, orphan_jvm::ArrayAccessException> {
    let mut command_found = false;
    let mut event_code = 1;
    while !command_found && event_code <= 56 {
        let event_offset = *orphan_jvm::array_ref(script.event_offsets.as_deref(), event_code)?;
        if event_offset != -1 {
            let mut interpreter = ink_interpreter_new(Some(0), event_offset, None);
            command_found = ink_interpreter_has_command(&mut interpreter, script, target_command)?;
        }
        event_code = orphan_jvm::i32_add(event_code, 1);
    }
    Ok(command_found)
}

pub fn room_object_new(
    input: Option<&mut orphan_formats::Reader<'_>>,
    strings: Option<&[Option<alloc::vec::Vec<u16>>]>,
) -> RoomObjectState {
    let mut state = RoomObjectState {
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
        color: ROOM_OBJECT_DEFAULT_COLOR,
        text: None,
        run_animation_paused_time: -1,
    };
    let _ = (|| -> Result<(), ()> {
        state.idle_animation_time = 0;
        state.visible = true;
        state.active = false;
        let input = input.ok_or(())?;
        state.object_type = i32::from(input.u8().map_err(|_| ())?);
        match state.object_type {
            ROOM_OBJECT_TYPE_GRAPHICS => {
                let x_high = input.u8().map_err(|_| ())?;
                let x_low = input.u8().map_err(|_| ())?;
                state.x = i32::from(i16::from_be_bytes([x_high, x_low]));
                let y_high = input.u8().map_err(|_| ())?;
                let y_low = input.u8().map_err(|_| ())?;
                state.y = i32::from(i16::from_be_bytes([y_high, y_low]));
                state.transform = i32::from(input.u8().map_err(|_| ())?);
                match i32::from(input.u8().map_err(|_| ())?) {
                    ROOM_OBJECT_GRAPHICS_ID_TYPE_STRING => {
                        let string_index = i32::from(input.u8().map_err(|_| ())?);
                        let zero_based_index = orphan_jvm::i32_sub(string_index, 1);
                        state.gfx_id = orphan_jvm::array_ref(strings, zero_based_index)
                            .map_err(|_| ())?
                            .as_ref()
                            .map(|value| JavaOwnedObject::String(value.clone()));
                    }
                    ROOM_OBJECT_GRAPHICS_ID_TYPE_INTEGER => {
                        let high = u16::from(input.u8().map_err(|_| ())?);
                        let low = u16::from(input.u8().map_err(|_| ())?);
                        state.gfx_id = Some(JavaOwnedObject::Integer(i32::from((high << 8) | low)));
                    }
                    _ => {}
                }
            }
            ROOM_OBJECT_TYPE_ZONE
            | ROOM_OBJECT_TYPE_BATTLE_ZONE
            | ROOM_OBJECT_TYPE_COLOR_ZONE
            | ROOM_OBJECT_TYPE_TEXT_ZONE
            | ROOM_OBJECT_TYPE_TILE_ZONE => {
                let x_high = input.u8().map_err(|_| ())?;
                let x_low = input.u8().map_err(|_| ())?;
                state.x = i32::from(i16::from_be_bytes([x_high, x_low]));
                let y_high = input.u8().map_err(|_| ())?;
                let y_low = input.u8().map_err(|_| ())?;
                state.y = i32::from(i16::from_be_bytes([y_high, y_low]));
                state.width = i32::from(input.u8().map_err(|_| ())?);
                state.height = i32::from(input.u8().map_err(|_| ())?);
            }
            _ => {}
        }
        let script_string_index = i32::from(input.u8().map_err(|_| ())?);
        if script_string_index != 0 {
            let zero_based_index = orphan_jvm::i32_sub(script_string_index, 1);
            state.script_id = orphan_jvm::array_ref(strings, zero_based_index)
                .map_err(|_| ())?
                .clone();
        }
        Ok(())
    })();
    state
}

pub fn room_object_initialize(statics: &mut RoomObjectStatics) {
    statics.painting_animation_time = -1;
    statics.no_vibration_yet = true;
    statics.battle_panel_hero_health_id = 1;
    statics.battle_panel_enemy_health_id = 2;
    statics.battle_panel_time_bar_id = 3;
    statics.battle_panel_hard_attack_id = 4;
    statics.battle_panel_fast_attack_id = 5;
    statics.battle_panel_inventory_id = 6;
    statics.battle_panel_escape_id = 7;
    statics.battle_panel_max_health = 0;
    statics.battle_panel_health = 1;
    statics.battle_panel_bar_size = 2;
    statics.battle_panel_time = 3;
    statics.battle_panel_size = 4;
}

pub fn room_object_battle_panel_new(
    state: &mut RoomObjectState,
    statics: &RoomObjectStatics,
    panel_id: i32,
) -> Result<(), orphan_jvm::NegativeArraySizeException> {
    state.battle_panel_id = panel_id;
    let panel = orphan_jvm::new_i32_array(statics.battle_panel_size)?;
    state.battle_panel = Some(panel);
    Ok(())
}

pub fn room_object_bp_set_max_health(
    state: &mut RoomObjectState,
    statics: &RoomObjectStatics,
    max_health: i32,
) -> Result<(), orphan_jvm::ArrayAccessException> {
    let slot = orphan_jvm::array_mut(
        state.battle_panel.as_deref_mut(),
        statics.battle_panel_max_health,
    )?;
    *slot = max_health;
    Ok(())
}

pub fn room_object_bp_set_health(
    state: &mut RoomObjectState,
    statics: &RoomObjectStatics,
    health: i32,
) -> Result<(), orphan_jvm::ArrayAccessException> {
    let slot = orphan_jvm::array_mut(
        state.battle_panel.as_deref_mut(),
        statics.battle_panel_health,
    )?;
    *slot = health;
    Ok(())
}

pub fn room_object_bp_set_bar_size(
    state: &mut RoomObjectState,
    statics: &RoomObjectStatics,
    bar_size: i32,
) -> Result<(), orphan_jvm::ArrayAccessException> {
    let slot = orphan_jvm::array_mut(
        state.battle_panel.as_deref_mut(),
        statics.battle_panel_bar_size,
    )?;
    *slot = bar_size;
    Ok(())
}

pub fn room_object_bp_set_time(
    state: &mut RoomObjectState,
    statics: &RoomObjectStatics,
    time: i32,
) -> Result<(), orphan_jvm::ArrayAccessException> {
    let slot = orphan_jvm::array_mut(state.battle_panel.as_deref_mut(), statics.battle_panel_time)?;
    *slot = time;
    Ok(())
}

pub fn room_object_execute_event<ExecuteEvent, E>(
    statics: &InkScriptStatics,
    state: &mut RoomObjectState,
    event_code: i32,
    initial_value: Option<JavaOwnedObject>,
    language_debug_mode: bool,
    execute_event: ExecuteEvent,
) -> Result<Option<JavaOwnedObject>, InkScriptExecuteEventError<E>>
where
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        &mut RoomObjectState,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    if state.script.is_none() {
        let Some(script_id) = state.script_id.as_deref() else {
            return Ok(initial_value);
        };
        let scripts = statics
            .scripts
            .as_deref()
            .ok_or(InkScriptExecuteEventError::NullPointer)?;
        let script = scripts
            .iter()
            .find(|(candidate, _)| candidate.as_slice() == script_id)
            .map(|(_, script)| script);
        if let Some(script) = script {
            state.script = Some(match script {
                InkScriptRegistryValue::Script(script_handle) => *script_handle,
                InkScriptRegistryValue::Other => {
                    return Err(InkScriptExecuteEventError::ClassCast);
                }
            });
        }
    }
    let script_handle = state
        .script
        .ok_or(InkScriptExecuteEventError::NullPointer)?;
    execute_event(
        script_handle,
        event_code,
        initial_value,
        state,
        language_debug_mode,
    )
    .map_err(InkScriptExecuteEventError::Execute)
}

pub fn room_object_get_name<ExecuteEvent, E>(
    statics: &InkScriptStatics,
    state: &mut RoomObjectState,
    execute_event: ExecuteEvent,
) -> Result<Option<alloc::vec::Vec<u16>>, RoomObjectStringEventError<E>>
where
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        &mut RoomObjectState,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    let name = room_object_execute_event(
        statics,
        state,
        INK_EVENT_GET_NAME,
        Some(JavaOwnedObject::String(alloc::vec![u16::from(b'?')])),
        false,
        execute_event,
    )
    .map_err(RoomObjectStringEventError::ExecuteEvent)?;
    match name {
        None => Ok(None),
        Some(JavaOwnedObject::String(name)) => Ok(Some(name)),
        Some(JavaOwnedObject::Integer(_) | JavaOwnedObject::Other) => {
            Err(RoomObjectStringEventError::ClassCast)
        }
    }
}

pub fn room_object_get_move_direction<ExecuteEvent, E>(
    statics: &InkScriptStatics,
    state: &mut RoomObjectState,
    execute_event: ExecuteEvent,
) -> Result<Option<alloc::vec::Vec<u16>>, RoomObjectStringEventError<E>>
where
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        &mut RoomObjectState,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    let direction = room_object_execute_event(
        statics,
        state,
        INK_EVENT_GET_MOVE_DIRECTION,
        None,
        false,
        execute_event,
    )
    .map_err(RoomObjectStringEventError::ExecuteEvent)?;
    match direction {
        None => Ok(None),
        Some(JavaOwnedObject::String(direction)) => Ok(Some(direction)),
        Some(JavaOwnedObject::Integer(_) | JavaOwnedObject::Other) => {
            Err(RoomObjectStringEventError::ClassCast)
        }
    }
}

pub fn room_object_enter_hover<HasEvent, ExecuteEvent, E>(
    statics: &InkScriptStatics,
    state: &mut RoomObjectState,
    room_handle: u32,
    has_event: HasEvent,
    execute_event: ExecuteEvent,
) -> Result<Option<u32>, RoomObjectEnterHoverError<E>>
where
    HasEvent: FnOnce(u32, i32) -> Result<bool, E>,
    ExecuteEvent: FnOnce(
        u32,
        i32,
        Option<JavaOwnedObject>,
        &mut RoomObjectState,
        bool,
    ) -> Result<Option<JavaOwnedObject>, E>,
{
    let Some(script_handle) = state.script else {
        return Ok(None);
    };
    if !has_event(script_handle, INK_EVENT_HOVER_IN).map_err(RoomObjectEnterHoverError::HasEvent)? {
        return Ok(None);
    }
    room_object_execute_event(
        statics,
        state,
        INK_EVENT_HOVER_IN,
        None,
        false,
        execute_event,
    )
    .map_err(RoomObjectEnterHoverError::ExecuteEvent)?;
    Ok(Some(room_handle))
}

pub fn room_object_is_over(state: &RoomObjectState, x: i32, y: i32) -> bool {
    state.visible
        && state.active
        && x >= state.left
        && x <= state.right
        && y >= state.top
        && y <= state.bottom
}

pub fn get_left(
    position: i32,
    width: i32,
    height: i32,
    anchor_x: i32,
    anchor_y: i32,
    transform: i32,
) -> i32 {
    use orphan_jvm::i32_sub;
    match transform {
        1 | 4 => i32_sub(position, i32_sub(width, anchor_x)),
        2 | 3 => i32_sub(position, i32_sub(height, anchor_y)),
        6 | 7 => i32_sub(position, anchor_y),
        _ => i32_sub(position, anchor_x),
    }
}

pub fn get_top(
    position: i32,
    width: i32,
    height: i32,
    anchor_x: i32,
    anchor_y: i32,
    transform: i32,
) -> i32 {
    use orphan_jvm::i32_sub;
    match transform {
        2 | 7 => i32_sub(position, anchor_x),
        3 | 6 => i32_sub(position, i32_sub(width, anchor_x)),
        4 | 5 => i32_sub(position, i32_sub(height, anchor_y)),
        _ => i32_sub(position, anchor_y),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pure_helpers_preserve_java_overflow() {
        assert_eq!(abs(i32::MIN), i32::MIN);
        assert_eq!(get_left(i32::MIN, i32::MAX, 0, -1, 0, 1), 0);
        assert_eq!(get_top(10, 20, 30, 2, 3, 4), -17);
    }

    #[test]
    fn game_canvas_paint_forwards_the_graphics_argument_once() {
        let mut calls = 0;
        let output = game_canvas_paint(Some(37_u32), |graphics| {
            calls += 1;
            assert_eq!(graphics, Some(37));
            "painted"
        });
        assert_eq!(calls, 1);
        assert_eq!(output, "painted");
    }

    #[test]
    fn game_canvas_show_notify_preserves_call_order_and_failure_boundaries() {
        let calls = core::cell::RefCell::new(Vec::new());
        let result = game_canvas_show_notify(
            |hidden| {
                calls.borrow_mut().push(("set-hide", hidden));
                Ok::<(), &'static str>(())
            },
            || {
                calls.borrow_mut().push(("resume-sound", false));
                Ok::<(), &'static str>(())
            },
        );
        assert_eq!(result, Ok(()));
        assert_eq!(
            calls.into_inner(),
            [("set-hide", false), ("resume-sound", false)]
        );

        let resume_calls = core::cell::Cell::new(0);
        let set_hide_failure = game_canvas_show_notify(
            |_| Err("set-hide"),
            || {
                resume_calls.set(resume_calls.get() + 1);
                Ok(())
            },
        );
        assert_eq!(set_hide_failure, Err("set-hide"));
        assert_eq!(resume_calls.get(), 0);

        let resume_failure = game_canvas_show_notify(|_| Ok(()), || Err::<(), _>("resume-sound"));
        assert_eq!(resume_failure, Err("resume-sound"));
    }

    #[test]
    fn ink_engine_wrap_string_forwards_all_three_arguments_once() {
        let mut calls = 0;
        let output = ink_engine_wrap_string(
            Some([65_u16, 66]),
            -7,
            Some(19_u32),
            |text, maximum_length, font| {
                calls += 1;
                assert_eq!(text, Some([65, 66]));
                assert_eq!(maximum_length, -7);
                assert_eq!(font, Some(19));
                "wrapped"
            },
        );
        assert_eq!(calls, 1);
        assert_eq!(output, "wrapped");
    }

    #[test]
    fn application_print_array_checks_length_before_forwarding_the_full_range() {
        let data = [Some(vec![1_u8]), None, Some(vec![2, 3])];
        let mut calls = 0;
        let output = application_print_array(Some(&data), |forwarded, from, to| {
            calls += 1;
            assert!(core::ptr::eq(forwarded, &data));
            assert_eq!((from, to), (0, 3));
            "printed"
        });
        assert_eq!(output, Ok("printed"));
        assert_eq!(calls, 1);

        let null_output = application_print_array::<Option<Vec<u8>>, _>(None, |_, _, _| {
            calls += 1;
        });
        assert_eq!(null_output, Err(orphan_jvm::NullPointerException));
        assert_eq!(calls, 1);
    }

    #[test]
    fn clear_all_rms_clears_scripts_only_after_resource_clear_succeeds() {
        let mut statics = InkScriptStatics {
            scripts: Some(vec![(vec![65_u16], InkScriptRegistryValue::Script(7))]),
            wait_stop: 0,
            item_id: None,
        };
        let mut resource_calls = 0;
        let success = application_clear_all_rms(&mut statics, || {
            resource_calls += 1;
            Ok::<(), &'static str>(())
        });
        assert!(success.is_ok());
        assert_eq!(resource_calls, 1);
        assert!(statics.scripts.as_ref().unwrap().is_empty());

        statics.scripts = Some(vec![(vec![66_u16], InkScriptRegistryValue::Script(11))]);
        let failure = application_clear_all_rms(&mut statics, || Err("resource-clear"));
        assert!(matches!(
            failure,
            Err(ApplicationClearAllRmsError::ResourceClear("resource-clear"))
        ));
        assert_eq!(statics.scripts.as_ref().unwrap().len(), 1);

        statics.scripts = None;
        let null_list = application_clear_all_rms(&mut statics, || Ok::<(), ()>(()));
        assert!(matches!(
            null_list,
            Err(ApplicationClearAllRmsError::ScriptListNull)
        ));
    }

    #[test]
    fn free_memory_collects_before_resolving_and_sampling_the_runtime() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: false,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: 0,
            loading_mode: 0,
            load_thread: None,
            room_repaint_thread: None,
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
            languages: None,
            resource_heap_sources: None,
            resource_sc_data: None,
            resource_sc_current_size: 0,
            random_instance: None,
            runtime_instance: Some(37),
            midlet_instance: None,
            ink_server_variables: None,
            ink_server_hints: None,
            game_changed_since_last_save: false,
        };
        let calls = core::cell::RefCell::new(alloc::vec::Vec::new());
        let result = application_free_memory(
            &application,
            || calls.borrow_mut().push("gc"),
            |runtime| {
                calls.borrow_mut().push("free");
                assert_eq!(runtime, 37);
                i64::MIN
            },
        );
        assert_eq!(result, Ok(i64::MIN));
        assert_eq!(&*calls.borrow(), &["gc", "free"]);

        application.runtime_instance = None;
        calls.borrow_mut().clear();
        let result = application_free_memory(
            &application,
            || calls.borrow_mut().push("gc"),
            |_| {
                calls.borrow_mut().push("free");
                0
            },
        );
        assert_eq!(result, Err(orphan_jvm::NullPointerException));
        assert_eq!(&*calls.borrow(), &["gc"]);
    }

    #[test]
    fn set_display_forwards_midlet_display_and_displayable_in_order() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: false,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: 0,
            loading_mode: 0,
            load_thread: None,
            room_repaint_thread: None,
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
            languages: None,
            resource_heap_sources: None,
            resource_sc_data: None,
            resource_sc_current_size: 0,
            random_instance: None,
            runtime_instance: None,
            midlet_instance: Some(11),
            ink_server_variables: None,
            ink_server_hints: None,
            game_changed_since_last_save: false,
        };
        let calls = core::cell::RefCell::new(alloc::vec::Vec::new());
        let success = application_set_display(
            &application,
            Some(29),
            |midlet| {
                calls.borrow_mut().push("get");
                assert_eq!(midlet, Some(11));
                Ok::<_, &'static str>(Some(17))
            },
            |display, displayable| {
                calls.borrow_mut().push("set");
                assert_eq!(display, 17);
                assert_eq!(displayable, Some(29));
                Ok(())
            },
        );
        assert!(success.is_ok());
        assert_eq!(&*calls.borrow(), &["get", "set"]);

        application.midlet_instance = None;
        calls.borrow_mut().clear();
        let null_display = application_set_display(
            &application,
            None,
            |midlet| {
                calls.borrow_mut().push("get");
                assert_eq!(midlet, None);
                Ok::<_, &'static str>(None)
            },
            |_, _| {
                calls.borrow_mut().push("set");
                Ok(())
            },
        );
        assert!(matches!(
            null_display,
            Err(ApplicationSetDisplayError::DisplayNull)
        ));
        assert_eq!(&*calls.borrow(), &["get"]);

        let get_failure = application_set_display(
            &application,
            None,
            |_| Err::<Option<u32>, _>("get"),
            |_, _| Ok(()),
        );
        assert!(matches!(
            get_failure,
            Err(ApplicationSetDisplayError::GetDisplay("get"))
        ));

        let set_failure =
            application_set_display(&application, None, |_| Ok(Some(17)), |_, _| Err("set"));
        assert!(matches!(
            set_failure,
            Err(ApplicationSetDisplayError::SetCurrent("set"))
        ));
    }

    #[test]
    fn application_paint_delegates_only_at_or_below_the_demo_frame() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 4,
            demo_frames: 4,
            painting: false,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: 0,
            loading_mode: 0,
            load_thread: None,
            room_repaint_thread: None,
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
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
        };
        let mut calls = 0;
        let failed = application_paint(&application, Some(31_u32), |graphics| {
            calls += 1;
            assert_eq!(graphics, Some(31));
            Err("paint")
        });
        assert_eq!(failed, Err("paint"));
        assert_eq!(calls, 1);

        application.fade_frames = 5;
        let skipped = application_paint(&application, None::<u32>, |_| {
            calls += 1;
            Err::<(), _>("unreachable")
        });
        assert_eq!(skipped, Ok(()));
        assert_eq!(calls, 1);
    }

    #[test]
    fn repaint_canvas_if_possible_preserves_guard_rereads_and_failure_boundaries() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: true,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: 0,
            loading_mode: 0,
            load_thread: None,
            room_repaint_thread: None,
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
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
        };
        let calls = core::cell::RefCell::new(alloc::vec::Vec::new());

        let skipped = application_repaint_canvas_if_possible(
            &mut application,
            |_, _| {
                calls.borrow_mut().push(("repaint", 0));
                Err::<(), _>("unreachable")
            },
            |_, _| {
                calls.borrow_mut().push(("service", 0));
                Err::<(), _>("unreachable")
            },
        );
        assert_eq!(skipped, Ok(()));
        assert!(calls.borrow().is_empty());

        application.painting = false;
        let null_repaint = application_repaint_canvas_if_possible(
            &mut application,
            |_, _| Ok::<(), &'static str>(()),
            |_, _| Ok(()),
        );
        assert_eq!(
            null_repaint,
            Err(ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeRepaint)
        );
        assert!(application.painting);

        application.painting = false;
        application.canvas_instance = Some(1);
        let repaint_failure = application_repaint_canvas_if_possible(
            &mut application,
            |canvas, _| {
                calls.borrow_mut().push(("repaint", canvas));
                Err("repaint")
            },
            |canvas, _| {
                calls.borrow_mut().push(("service", canvas));
                Ok(())
            },
        );
        assert_eq!(
            repaint_failure,
            Err(ApplicationRepaintCanvasIfPossibleError::Repaint("repaint"))
        );
        assert_eq!(&*calls.borrow(), &[("repaint", 1)]);
        assert!(application.painting);

        calls.borrow_mut().clear();
        application.painting = false;
        application.canvas_instance = Some(1);
        let cleared = application_repaint_canvas_if_possible(
            &mut application,
            |canvas, state| {
                calls.borrow_mut().push(("repaint", canvas));
                state.canvas_instance = None;
                Ok::<(), &'static str>(())
            },
            |canvas, _| {
                calls.borrow_mut().push(("service", canvas));
                Ok(())
            },
        );
        assert_eq!(
            cleared,
            Err(ApplicationRepaintCanvasIfPossibleError::CanvasNullBeforeServiceRepaints)
        );
        assert_eq!(&*calls.borrow(), &[("repaint", 1)]);
        assert!(application.painting);

        calls.borrow_mut().clear();
        application.painting = false;
        application.canvas_instance = Some(1);
        let replaced = application_repaint_canvas_if_possible(
            &mut application,
            |canvas, state| {
                calls.borrow_mut().push(("repaint", canvas));
                state.canvas_instance = Some(2);
                state.painting = false;
                Ok::<(), &'static str>(())
            },
            |canvas, state| {
                calls.borrow_mut().push(("service", canvas));
                assert!(!state.painting);
                Ok(())
            },
        );
        assert_eq!(replaced, Ok(()));
        assert_eq!(&*calls.borrow(), &[("repaint", 1), ("service", 2)]);
        assert!(!application.painting);
        assert_eq!(application.canvas_instance, Some(2));

        calls.borrow_mut().clear();
        application.painting = false;
        application.canvas_instance = Some(1);
        let service_failure = application_repaint_canvas_if_possible(
            &mut application,
            |canvas, _| {
                calls.borrow_mut().push(("repaint", canvas));
                Ok::<(), &'static str>(())
            },
            |canvas, _| {
                calls.borrow_mut().push(("service", canvas));
                Err("service")
            },
        );
        assert_eq!(
            service_failure,
            Err(ApplicationRepaintCanvasIfPossibleError::ServiceRepaints(
                "service"
            ))
        );
        assert_eq!(&*calls.borrow(), &[("repaint", 1), ("service", 1)]);
        assert!(application.painting);

        calls.borrow_mut().clear();
        application.painting = false;
        let recursive = application_repaint_canvas_if_possible(
            &mut application,
            |canvas, state| {
                calls.borrow_mut().push(("repaint", canvas));
                let nested = application_repaint_canvas_if_possible(
                    state,
                    |_, _| Err::<(), _>("nested-repaint"),
                    |_, _| Err::<(), _>("nested-service"),
                );
                assert_eq!(nested, Ok(()));
                Ok::<(), &'static str>(())
            },
            |canvas, _| {
                calls.borrow_mut().push(("service", canvas));
                Ok(())
            },
        );
        assert_eq!(recursive, Ok(()));
        assert_eq!(&*calls.borrow(), &[("repaint", 1), ("service", 1)]);
    }

    #[test]
    fn rms_delete_forwards_once_and_distinguishes_the_two_caught_failures() {
        let name = [0_u16, 0xd800, 65];
        let calls = core::cell::Cell::new(0);
        let observe = |observed: Option<&[u16]>| {
            calls.set(calls.get() + 1);
            let observed = observed.expect("the hostile name remains nonnull");
            assert_eq!(observed.as_ptr(), name.as_ptr());
            assert_eq!(observed.len(), name.len());
        };

        let success = application_rms_delete(Some(&name[..]), |observed| {
            observe(observed);
            Ok::<(), ApplicationRmsDeleteError<&'static str>>(())
        });
        assert_eq!(success, Ok(true));

        let absent = application_rms_delete(Some(&name[..]), |observed| {
            observe(observed);
            Err(ApplicationRmsDeleteError::<&'static str>::NotFound)
        });
        assert_eq!(absent, Ok(true));

        let failed = application_rms_delete(Some(&name[..]), |observed| {
            observe(observed);
            Err(ApplicationRmsDeleteError::<&'static str>::RecordStore)
        });
        assert_eq!(failed, Ok(false));

        let uncaught = application_rms_delete(Some(&name[..]), |observed| {
            observe(observed);
            Err(ApplicationRmsDeleteError::Uncaught("npe"))
        });
        assert_eq!(uncaught, Err("npe"));
        assert_eq!(calls.get(), 4);
    }

    #[test]
    fn save_chunk_ini_reads_before_setting_the_fixed_record_name_and_swallows_failures() {
        let input = [1_u8, 0, 255];
        let bytes = [7_u8, 8];
        let calls = core::cell::RefCell::new(Vec::new());
        application_save_chunk_ini(
            Some(&input[..]),
            |observed| {
                calls.borrow_mut().push("get");
                assert_eq!(observed.unwrap().as_ptr(), input.as_ptr());
                Ok::<_, &'static str>(&bytes[..])
            },
            |name, observed| {
                calls.borrow_mut().push("set");
                assert_eq!(name, &[82, 77, 83, 95, 99, 104, 117, 110, 107, 73, 78, 73]);
                assert_eq!(observed.as_ptr(), bytes.as_ptr());
                Ok(false)
            },
        );
        assert_eq!(&*calls.borrow(), &["get", "set"]);

        calls.borrow_mut().clear();
        application_save_chunk_ini(
            None::<u32>,
            |_| Ok::<_, &'static str>(None::<Vec<u8>>),
            |name, observed| {
                calls.borrow_mut().push("set-null");
                assert_eq!(name, &[82, 77, 83, 95, 99, 104, 117, 110, 107, 73, 78, 73]);
                assert!(observed.is_none());
                Ok(false)
            },
        );
        assert_eq!(&*calls.borrow(), &["set-null"]);

        calls.borrow_mut().clear();
        application_save_chunk_ini(
            Some(&input[..]),
            |_| Err::<&[u8], _>("get"),
            |_, _| {
                calls.borrow_mut().push("set");
                Ok(false)
            },
        );
        assert!(calls.borrow().is_empty());

        application_save_chunk_ini(
            Some(&input[..]),
            |_| Ok::<_, &'static str>(&bytes[..]),
            |_, _| Err("set"),
        );
    }

    #[test]
    fn resource_make_subchunk_allocates_then_copies_the_current_prefix() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: false,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: 0,
            loading_mode: 0,
            load_thread: None,
            room_repaint_thread: None,
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
            languages: None,
            resource_heap_sources: None,
            resource_sc_data: Some(vec![1, 2, 3, 4]),
            resource_sc_current_size: 3,
            random_instance: None,
            runtime_instance: None,
            midlet_instance: None,
            ink_server_variables: None,
            ink_server_hints: None,
            game_changed_since_last_save: false,
        };

        let mut copy = application_resource_make_subchunk(&application).unwrap();
        assert_eq!(copy, [1, 2, 3]);
        copy[0] = 9;
        assert_eq!(
            application.resource_sc_data.as_deref(),
            Some(&[1, 2, 3, 4][..])
        );

        application.resource_sc_current_size = -1;
        application.resource_sc_data = None;
        assert_eq!(
            application_resource_make_subchunk(&application),
            Err(ApplicationResourceMakeSubChunkError::NegativeArraySize(
                orphan_jvm::NegativeArraySizeException { length: -1 }
            ))
        );

        application.resource_sc_current_size = 0;
        assert_eq!(
            application_resource_make_subchunk(&application),
            Err(ApplicationResourceMakeSubChunkError::ArrayCopy(
                orphan_jvm::ArrayCopyException::NullPointer
            ))
        );

        application.resource_sc_current_size = 5;
        application.resource_sc_data = Some(vec![1, 2, 3, 4]);
        assert_eq!(
            application_resource_make_subchunk(&application),
            Err(ApplicationResourceMakeSubChunkError::ArrayCopy(
                orphan_jvm::ArrayCopyException::IndexOutOfBounds
            ))
        );
    }

    #[test]
    fn game_canvas_constructor_calls_super_false_then_full_screen_true() {
        let calls = core::cell::RefCell::new(alloc::vec::Vec::new());
        let success = game_canvas_new(
            41,
            |canvas, suppress_keys| {
                assert_eq!(canvas, 41);
                calls.borrow_mut().push(("super", suppress_keys));
                Ok::<(), &'static str>(())
            },
            |canvas, full_screen| {
                assert_eq!(canvas, 41);
                calls.borrow_mut().push(("full", full_screen));
                Ok(())
            },
        );
        assert!(success.is_ok());
        assert_eq!(&*calls.borrow(), &[("super", false), ("full", true)]);

        calls.borrow_mut().clear();
        let super_failure = game_canvas_new(
            41,
            |canvas, suppress_keys| {
                assert_eq!(canvas, 41);
                calls.borrow_mut().push(("super", suppress_keys));
                Err("super")
            },
            |canvas, full_screen| {
                assert_eq!(canvas, 41);
                calls.borrow_mut().push(("full", full_screen));
                Ok(())
            },
        );
        assert!(matches!(
            super_failure,
            Err(GameCanvasNewError::SuperConstructor("super"))
        ));
        assert_eq!(&*calls.borrow(), &[("super", false)]);

        calls.borrow_mut().clear();
        let full_screen_failure = game_canvas_new(
            41,
            |canvas, suppress_keys| {
                assert_eq!(canvas, 41);
                calls.borrow_mut().push(("super", suppress_keys));
                Ok(())
            },
            |canvas, full_screen| {
                assert_eq!(canvas, 41);
                calls.borrow_mut().push(("full", full_screen));
                Err("full")
            },
        );
        assert!(matches!(
            full_screen_failure,
            Err(GameCanvasNewError::SetFullScreen("full"))
        ));
        assert_eq!(&*calls.borrow(), &[("super", false), ("full", true)]);
    }

    #[test]
    fn key_jad_entry_catches_lookup_and_decimal_parse_failures() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: false,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: 0,
            loading_mode: 0,
            load_thread: None,
            room_repaint_thread: None,
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
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
        };
        let mut calls = 0;
        assert_eq!(
            game_canvas_key_jad_entry_as_int(&application, None, |_, _| {
                calls += 1;
                Ok::<_, &'static str>(Some(alloc::vec![49]))
            }),
            0
        );
        assert_eq!(calls, 0);

        application.midlet_instance = Some(19);
        let key = [0xd800_u16];
        assert_eq!(
            game_canvas_key_jad_entry_as_int(&application, Some(&key), |midlet, observed| {
                calls += 1;
                assert_eq!(midlet, 19);
                assert_eq!(observed, Some(key.as_slice()));
                Ok::<_, &'static str>(Some("-2147483648".encode_utf16().collect()))
            }),
            i32::MIN
        );
        assert_eq!(calls, 1);
        assert_eq!(
            game_canvas_key_jad_entry_as_int(&application, None, |_, _| {
                Err::<Option<alloc::vec::Vec<u16>>, _>("lookup")
            }),
            0
        );
        assert_eq!(
            game_canvas_key_jad_entry_as_int(&application, None, |_, _| {
                Ok::<_, &'static str>(Some("2147483648".encode_utf16().collect()))
            }),
            0
        );
    }

    #[test]
    fn menu_reset_ingame_values_delegates_before_invalidating_ammo_layout() {
        let mut statics = SilentHillGameStatics {
            hud_ammo_number_width: i32::MIN,
            hud_ammo_update_needed: false,
            ink_menu_logo: None,
        };
        let mut calls = 0;
        let succeeded = silent_hill_game_menu_reset_ingame_values(&mut statics, || {
            calls += 1;
            Ok::<(), &'static str>(())
        });
        assert_eq!(succeeded, Ok(()));
        assert_eq!(calls, 1);
        assert_eq!(statics.hud_ammo_number_width, -1);
        assert!(statics.hud_ammo_update_needed);

        statics.hud_ammo_number_width = i32::MAX;
        statics.hud_ammo_update_needed = false;
        let failed = silent_hill_game_menu_reset_ingame_values(&mut statics, || Err("reset"));
        assert_eq!(failed, Err("reset"));
        assert_eq!(statics.hud_ammo_number_width, i32::MAX);
        assert!(!statics.hud_ammo_update_needed);
    }

    #[test]
    fn app_init_publishes_the_exact_menu_logo_before_engine_initialization() {
        let mut statics = SilentHillGameStatics {
            hud_ammo_number_width: 0,
            hud_ammo_update_needed: false,
            ink_menu_logo: Some(7),
        };
        let calls = core::cell::RefCell::new(alloc::vec::Vec::new());
        let succeeded = silent_hill_game_app_init(
            &mut statics,
            |path| {
                calls.borrow_mut().push("load");
                assert_eq!(
                    path,
                    "gfx/menu_logo.png"
                        .encode_utf16()
                        .collect::<alloc::vec::Vec<_>>()
                );
                Some(41)
            },
            || {
                calls.borrow_mut().push("engine");
                Ok::<(), &'static str>(())
            },
        );
        assert_eq!(succeeded, Ok(()));
        assert_eq!(statics.ink_menu_logo, Some(41));
        assert_eq!(&*calls.borrow(), &["load", "engine"]);

        let failed = silent_hill_game_app_init(&mut statics, |_| None, || Err("engine"));
        assert_eq!(failed, Err("engine"));
        assert_eq!(statics.ink_menu_logo, None);
    }

    #[test]
    fn room_repaint_run_clears_the_thread_only_after_success() {
        let mut application = ApplicationState {
            tick_based_time_value: 0,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: false,
            canvas_instance: None,
            key_last_pressed: 0,
            key_new: false,
            key_pressed: false,
            load_bar_active: false,
            goto_dissolve_fx_counter: -6,
            loading_mode: -1,
            load_thread: None,
            room_repaint_thread: Some(91),
            resource_importants: None,
            resources_to_download: None,
            game_id: None,
            game_texts: None,
            save_is_possible: false,
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
        };
        let failed = application_room_repaint_run(&mut application, |state| {
            assert_eq!(state.room_repaint_thread, Some(91));
            Err("paint failed")
        });
        assert_eq!(failed, Err("paint failed"));
        assert_eq!(application.room_repaint_thread, Some(91));

        let succeeded = application_room_repaint_run(&mut application, |state| {
            assert_eq!(state.room_repaint_thread, Some(91));
            Ok::<(), &str>(())
        });
        assert_eq!(succeeded, Ok(()));
        assert_eq!(application.room_repaint_thread, None);
    }

    #[test]
    fn leaf_helpers_preserve_java_domains() {
        assert_eq!(char_to_string(0xd800), [0xd800]);
        assert_eq!(resource_request_description(), None);
        assert_eq!(
            resource_merge_sort_cmp(Some(&[0xff]), Some(&[0])),
            Ok(false)
        );
        assert_eq!(
            resource_merge_sort_cmp(None, Some(&[])),
            Err(orphan_jvm::NullPointerException)
        );
        let number_nine: [u16; 14] = [
            97, 99, 116, 105, 111, 110, 107, 101, 121, 95, 110, 117, 109, 57,
        ];
        assert_eq!(action_key_id_convert(Some(&number_nine)), Ok(11));
        assert_eq!(
            action_key_id_convert(None),
            Err(orphan_jvm::NullPointerException)
        );
        let mut application = ApplicationState {
            tick_based_time_value: i32::MAX,
            canvas_width: 0,
            fade_frames: 0,
            demo_frames: 0,
            painting: false,
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
        tick_based_time_update(&mut application);
        assert_eq!(application.tick_based_time_value, i32::MIN + 59);
        let mut engine = InkEngineState {
            menu_scroll_tick_counter: i8::MAX,
            settings_hash: None,
            action_key_key_codes: Some([1, 7, 7].into()),
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
        assert!(is_menu_scroll_allowed(&mut engine));
        assert_eq!(engine.menu_scroll_tick_counter, 0);
        assert_eq!(action_key_keycode_to_action_key(&engine, 7), Ok(1));
        let canvas = GameCanvasState {
            transform_table: GAME_CANVAS_INITIAL_TRANSFORM_TABLE,
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
        assert_eq!(key_convert_to_key_id(&canvas, 53), -5);
        let mut menu = MenuState {
            is_current: false,
            selected_choice_number: i32::MIN,
            x: 11,
            y: 12,
            scroll: i32::MIN,
            text_scrolling: true,
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
        assert_eq!(menu_get_choice_number(&menu), i32::MIN);
        menu_set_position(&mut menu, i32::MAX, i32::MIN);
        assert_eq!((menu.x, menu.y), (i32::MAX, i32::MIN));
        menu_set_current(&mut menu, true);
        assert!(menu.is_current);
        menu_scroll_decrease(&mut menu);
        assert_eq!(menu.scroll, i32::MAX);
        assert!(menu.update_menu);
        menu.scroll = -2;
        menu.text_scrolling = false;
        menu.update_menu = false;
        menu_scroll_increase(&mut menu);
        assert_eq!(menu.scroll, -1);
        assert!(menu.text_scrolling);
        assert!(menu.update_menu);
    }
}
