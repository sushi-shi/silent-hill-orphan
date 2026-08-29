extern crate alloc;

use alloc::vec::Vec;

use crate::{InkScriptRegistryValue, JavaOwnedObject, JavaResourceId};

pub const GAME_CANVAS_INITIAL_TRANSFORM_TABLE: [i32; 8] = [0, 2, 5, 7, 3, 1, 6, 4];

pub struct CheatControllerStatics {
    pub last_key: i32,
}

pub struct SilentHillGameStatics {
    pub hud_ammo_number_width: i32,
    pub hud_ammo_update_needed: bool,
    pub ink_menu_logo: Option<u32>,
}

pub struct ApplicationState {
    pub tick_based_time_value: i32,
    pub canvas_width: i32,
    pub fade_frames: i32,
    pub demo_frames: i32,
    pub key_last_pressed: i32,
    pub key_new: bool,
    pub key_pressed: bool,
    pub load_bar_active: bool,
    pub goto_dissolve_fx_counter: i32,
    pub loading_mode: i32,
    pub load_thread: Option<u32>,
    pub room_repaint_thread: Option<u32>,
    pub resource_importants: Option<Vec<u32>>,
    pub resources_to_download: Option<Vec<Option<i32>>>,
    pub game_id: Option<Vec<u16>>,
    pub game_texts: Option<Vec<Option<Vec<u16>>>>,
    pub save_is_possible: bool,
    pub languages: Option<Vec<Option<Vec<u16>>>>,
    pub resource_heap_sources: Option<Vec<i32>>,
    pub resource_sc_data: Option<Vec<u8>>,
    pub resource_sc_current_size: i32,
    pub random_instance: Option<u32>,
    pub runtime_instance: Option<u32>,
    pub midlet_instance: Option<u32>,
    pub ink_server_variables: Option<Vec<(Vec<u16>, Vec<u16>)>>,
    pub ink_server_hints: Option<Vec<(Vec<u16>, Vec<u16>)>>,
    pub game_changed_since_last_save: bool,
}

pub struct ResourceRequestState {
    pub resource_type: i32,
    pub integer_id: i32,
    pub string_id: Option<Vec<u16>>,
    pub image_transform: i32,
}

pub struct GameResourceState {
    pub resource_type: i32,
    pub id: Option<JavaResourceId>,
    pub image: Option<u32>,
    pub image_width: i32,
    pub image_height: i32,
    pub image_registration_x: i32,
    pub image_registration_y: i32,
    pub image_transform: i32,
}

pub struct GameResourceStatics {
    pub cached_images: Option<Vec<u32>>,
    pub important_images: Option<Vec<u32>>,
}

pub struct InkEngineState {
    pub menu_scroll_tick_counter: i8,
    pub settings_hash: Option<Vec<(Vec<u16>, JavaOwnedObject)>>,
    pub action_key_key_codes: Option<Vec<i32>>,
    pub action_key_script_ids: Option<Vec<Option<Vec<u16>>>>,
    pub current_splash: i32,
    pub number_of_splashes: i32,
    pub popup_end_time: i64,
    pub popup_minimum_time_ends: i64,
    pub popup_current: i32,
    pub popup_number: i32,
    pub popup_active: bool,
    pub popup_choice: i8,
    pub popup_recovery_codes: Option<Vec<i32>>,
    #[allow(clippy::type_complexity)]
    pub popup_texts: Option<Vec<Option<Vec<Option<Vec<u16>>>>>>,
    pub popup_maximum_times: Option<Vec<i32>>,
}

pub struct GameCanvasState {
    pub transform_table: [i32; 8],
    pub key_softkey_left: i32,
    pub key_softkey_right: i32,
    pub key_send: i32,
    pub key_return: i32,
    pub key_softkey_center: i32,
    pub key_arrow_up: i32,
    pub key_arrow_down: i32,
    pub key_arrow_left: i32,
    pub key_arrow_right: i32,
    pub key_erase: i32,
}

pub struct MenuState {
    pub is_current: bool,
    pub selected_choice_number: i32,
    pub x: i32,
    pub y: i32,
    pub scroll: i32,
    pub text_scrolling: bool,
    pub update_menu: bool,
    pub top_text: Option<Vec<u16>>,
    pub update_top_lines: bool,
    pub engine_softkey_option_left: Option<Vec<u16>>,
    pub engine_softkey_option_right: Option<Vec<u16>>,
    pub choice_ids: Option<Vec<Option<i32>>>,
    pub choice_texts: Option<Vec<Option<Vec<u16>>>>,
    pub update_body_lines: bool,
    pub current_inventory_item_resource: Option<i32>,
}

pub struct MenuStatics {
    pub stack: Option<Vec<u32>>,
}

pub struct InkInterpreterStatics {
    pub paused_thread: Option<u32>,
}

pub struct InkInterpreterState {
    pub script: Option<u32>,
    pub status: i32,
    pub offset: i32,
    pub room_object: Option<u32>,
    pub language_debug_mode: bool,
}

pub struct InkScriptStatics {
    pub scripts: Option<Vec<(Vec<u16>, InkScriptRegistryValue)>>,
    pub wait_stop: i64,
    pub item_id: Option<Vec<u16>>,
}

pub struct InkScriptState {
    pub data: Option<Vec<u8>>,
    pub event_offsets: Option<Vec<i32>>,
    pub string_list: Option<Vec<Option<Vec<u16>>>>,
    pub gfx_id: Option<JavaOwnedObject>,
}

pub struct RoomObjectState {
    pub object_type: i32,
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
    pub registration_x: i32,
    pub registration_y: i32,
    pub left: i32,
    pub right: i32,
    pub top: i32,
    pub bottom: i32,
    pub transform: i32,
    pub gfx_id: Option<JavaOwnedObject>,
    pub script_id: Option<Vec<u16>>,
    pub script: Option<u32>,
    pub visible: bool,
    pub active: bool,
    pub text_alignment: i32,
    pub animation_data: Option<Vec<Option<Vec<Option<JavaOwnedObject>>>>>,
    pub animation_parts: Option<Vec<i32>>,
    pub animation_duration: Option<Vec<i32>>,
    pub animation_image_points: Option<Vec<Option<Vec<i32>>>>,
    pub animation_time: i64,
    pub idle_animation_time: i64,
    pub run_animation_loops: i32,
    pub battle_panel_id: i32,
    pub battle_panel: Option<Vec<i32>>,
    pub color: i32,
    pub text: Option<Vec<u16>>,
    pub run_animation_paused_time: i32,
}

pub struct RoomObjectStatics {
    pub painting_animation_time: i64,
    pub no_vibration_yet: bool,
    pub battle_panel_hero_health_id: i32,
    pub battle_panel_enemy_health_id: i32,
    pub battle_panel_time_bar_id: i32,
    pub battle_panel_hard_attack_id: i32,
    pub battle_panel_fast_attack_id: i32,
    pub battle_panel_inventory_id: i32,
    pub battle_panel_escape_id: i32,
    pub battle_panel_max_health: i32,
    pub battle_panel_health: i32,
    pub battle_panel_bar_size: i32,
    pub battle_panel_time: i32,
    pub battle_panel_size: i32,
}
