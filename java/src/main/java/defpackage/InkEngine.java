package defpackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStoreException;

/** Named reconstruction of original class {@code ExtBase}. */
public abstract class InkEngine {
    public static boolean FirstLoad;
    static final int ROOMIMAGE_DEFAULT_BACKGROUND_COLOR = 0;
    static RoomObject roomObjectTick;
    static int tickTimerUpdateInterval;
    public static boolean cursorAnimActive;
    public static GameResource[] cursorAnim;
    public static int cursorAnimFrameCounter;
    public static int cursorAnimCounter;
    static boolean regPointSystemActive;
    static RoomObject hoveredRoomObject;
    static GameResource specialCursorIdle;
    static GameResource hoverCursor;
    static int battleType;
    static final int BATTLE_TYPE_NO_BATTLE = 0;
    static final int BATTLE_TYPE_1 = 1;
    static final int BATTLE_TYPE_2 = 2;
    static Image healthBarFillImg;
    static Image healthOverlapImg;
    static boolean battleMode;
    static int battleState;
    static boolean attackAnim;
    static int battlePanelMode;
    static final int BATTLE_PANEL_MODE_FIGHT_HARD = 0;
    static final int BATTLE_PANEL_MODE_FIGHT_FAST = 1;
    static final int BATTLE_PANEL_MODE_INVENTORY = 2;
    static final int BATTLE_PANEL_MODE_ESC = 3;
    static int battleFightTypeActive;
    static final int BATTLE_FIGHT_TYPE_NEUTRAL = 0;
    static final int BATTLE_FIGHT_TYPE_HARD = 1;
    static final int BATTLE_FIGHT_TYPE_FAST = 2;
    static final int LOADING_BAR_COLOR_BORDER = 0;
    static final int LOADING_BAR_COLOR_ENGINE_BG = 13487565;
    static final int LOADING_BAR_COLOR_ENGINE_MARKER = 16777215;
    static final int LOADING_BAR_COLOR_INGAME_MARKER = 7500402;
    static final int LOADING_BAR_HEIGHT = 6;
    static final int LOADING_BAR_MARKER_WIDTH = 20;
    static final int POPUP_MAX_POPUPS = 5;
    static final int POPUP_MIN_TIME = 500;
    static long popupMinTimeEnds;
    static long popupEndTime;
    static final int POPUP_ACTION_CODE_DO_NOTHING = 0;
    static final int POPUP_ACTION_CODE_MAIN_MENU = 1;
    static final int POPUP_ACTION_CODE_YES_NO = 2;
    static final int POPUP_EXIT = 3;
    static final int POPUP_ACTION_CODE_LEAVE_GAME = 4;
    static final int POPUP_ACTION_CODE_START_NEW_REMOVE_OLD_GAME = 5;
    static final int POPUP_ACTION_CODE_QUIT_CONFIRMATION = 7;
    static final int POPUP_ACTION_CODE_DEMO_END = 8;
    static final int POPUP_ACTION_CODE_DEMO_END_2 = 9;
    static final int POPUP_ACTION_CODE_DEMO_END_3 = 10;
    static final byte POPUP_NOT_CHOICEN = 0;
    static final byte POPUP_YES = 1;
    static final byte POPUP_NO = 2;
    static final int POPUP_ENGINE_BG_COLOR = 0;
    static final int POPUP_ENGINE_BORDER_COLOR = 0;
    static Hashtable splashHash;
    static final int MENU_CONTEXT = 1;
    static final int MENU_SCRIPT_CHOICE = 2;
    static final int MENU_INVENTORY = 3;
    static final int MENU_USE_ITEM = 5;
    static final int MENU_SAY = 6;
    static final int MENU_INVENTORY_ADD = 7;
    static final int MENU_MAIN_MENU = 8;
    static final int MENU_SETTINGS = 9;
    static final int MENU_PLAY = 10;
    static final int MENU_HELP = 11;
    static final int MENU_ESC = 17;
    static final int MENU_START_APP_LANGUAGE = 20;
    static final int MENU_ENGINE_FULLSCREEN = 21;
    static final int MENU_SAY_FULLSCREEN = 27;
    static final int MENU_TEXT_FADE_CENTER = 28;
    static final int MENU_SOUND_VIBRA_SETTINGS = 29;
    static final int MENU_ESC_HELP = 30;
    static final int MENU_GOTO_CHEAT = 100;
    static final int MENU_NONE = 0;
    static final int MENU_SAY_HEIGHT_LIMIT = 3;
    static int[] textFadeList;
    static int textFadePresentColor;
    static boolean textFadeMenuClose;
    static long textFadeStartTime;
    static final int CHOICE_GET_GAME = 1;
    static final int CHOICE_SAVED = 2;
    static final int CHOICE_PLAY = 3;
    static final int CHOICE_START_TRIAL = 31;
    static final int CHOICE_GET_GAME_DEMO = 32;
    static final int MENU_ENGINE_FULLSCREEN2 = 33;
    static final int CHOICE_SETTINGS = 4;
    static final int CHOICE_HELP = 5;
    static final int CHOICE_QUIT = 6;
    static final int CHOICE_SOUND = 7;
    static final int CHOICE_LANG = 8;
    static final int CHOICE_GAMEPLAY = 9;
    static final int CHOICE_CONTROLS = 10;
    static final int CHOICE_HOW_TO_DL = 11;
    static final int CHOICE_NEW_GAMES = 13;
    static final int CHOICE_VIBRA = 14;
    static final int CHOICE_ABOUT = 30;
    static final int CHOICE_CONTEXT_INVENTORY = -2;
    static final int CHOICE_CONTEXT_EXTRA_OPTION = -4;
    static final int CHOICE_ESC_CONTINUE = -1;
    static final int CHOICE_ESC_RELOAD = -2;
    static final int CHOICE_ESC_SAVE = -3;
    static final int CHOICE_ESC_SETTINGS = -4;
    static final int CHOICE_ESC_HELP = -5;
    static final int CHOICE_ESC_MAIN = -6;
    static final int CHOICE_ESC_GAMEPLAY = -7;
    static final int CHOICE_ESC_CONTROLS = -8;
    static final int CHOICE_INVENTORY_ITEM_COMBINE = -1;
    static final int CHOICE_INVENTORY_ITEM_USE = -2;
    static final int CHOICE_INVENTORY_EXIT = -3;
    static final int CHOICE_NONE = 0;
    static final int ENGINE_CHOICE_SELECTION_ANIM_DELAY = 2;
    static final int ENGINE_TEXT_COLOR = 0;
    static final int ENGINE_SOFTKEY_COLOR = 0;
    static final int ENGINE_FRAME_COLOR_START = 16777215;
    static final int ENGINE_FRAME_COLOR_HEADER_SHADOW_1 = 9934743;
    static final int ENGINE_FRAME_COLOR_HEADER_SHADOW_2 = 13027530;
    static final int ENGINE_FRAME_COLOR_END = 13026500;
    static final int ENGINE_BG_COLOR = 16777215;
    static final int ENGINE_BOLD_LIMIT = 170;
    static final boolean ENGINE_SOFTKEY_BOLD = false;
    static final int ENGINE_SCROLL_BAR_COLOR_BORDER = 0;
    static final int ENGINE_SCROLL_BAR_COLOR_BG = 13487565;
    static final int ENGINE_SCROLL_BAR_COLOR_MARKER = 16777215;
    static final int ENGINE_SCROLL_BAR_OFFSET_X = 1;
    static final int ENGINE_SCROLL_BAR_WIDTH = 4;
    static final int ENGINE_MARGIN_WITH_SCROLLBAR = 7;
    static final int ENGINE_SOFTKEY_MARGIN = 2;
    static final int ENGINE_MARGIN = 4;
    static final int ENGINE_LINE_SPACING = 2;
    static final int ENGINE_DELIMITER_HEIGHT = 1;
    static final int ENGINE_DELIMITER_COLOR = 0;
    static int engineHeaderImageHeight;
    static Image engineHeaderImage;
    static int engineFooterImageHeight;
    static Image engineFooterImage;
    static final byte DEFAULT_CHOICE_ROWS = 4;
    public static final int INGAME_DEFAULT_DELIMITER_HEIGHT = 2;
    public static final int INGAME_DELIMITER_MODE_LEFT_REPEATING = 0;
    public static final int INGAME_DELIMITER_MODE_CENTERED_ONE = 1;
    public static final int INGAME_DELIMITER_MODE_CENTERED_REPEATING = 2;
    static final int ENGINE_HEADER_FOOTER_IMAGE_WIDTH = 15;
    static final int ENGINE_HEADER_IMAGE_HEIGHT = 19;
    static final boolean ENGINE_HEADER_FORCE_CONSTANT_HEIGHT = false;
    static int ingameBorderColor;
    static int ingameHeaderColor;
    static int ingameBackgroundColor;
    static int ingameBackgroundColorSelected;
    static int ingameTextColor;
    static int ingameMargin;
    static int ingameLineSpacing;
    static int ingameBorderSize;
    public static boolean ingameUseDynamicSayChoice;
    static int engineScrollBarY;
    static int engineScrollBarHeight;
    static int engineScrollBarX;
    public static int ingameDelimiterHeight;
    public static int ingameDelimiterMode;
    static boolean ingameShowRowByRow_available;
    public static int invArrowHeight;
    public static final int INV_IMAGE_MARGIN = 3;
    public static final int INV_IMAGE_SIZE_MAX = 32;
    public static final int INV_IMAGE_SECTION_WIDTH_MAX = 64;
    static Object[] curInvIds;
    static String[] curInvNames;
    static int curInvNumOfItems;
    static int curInvCounter;
    public static Font engineFont;
    public static int engineFontHeight;
    public static Font ingameFont;
    public static int ingameFontHeight;
    public static Font currentFont;
    public static int currentFontHeight;
    public static final byte MENU_SCROLL_TICK_INTERVAL = 2;
    static final int SETUP_INDEX_CURSOR_IDLE = 1;
    static final int SETUP_INDEX_CURSOR_POINT = 2;
    static final int SETUP_INDEX_CURSOR_BACK = 3;
    static final int SETUP_INDEX_CURSOR_FWD_LEFT = 4;
    static final int SETUP_INDEX_CURSOR_FWD_RIGHT = 5;
    static final int SETUP_INDEX_CURSOR_FWD = 6;
    static final int SETUP_INDEX_CURSOR_MENU = 7;
    static final int SETUP_INDEX_DISSOLVE_EFFECT_IMG = 8;
    static final int SETUP_INDEX_INGAME_SCROLL_ARROW_UP = 9;
    static final int SETUP_INDEX_INGAME_SCROLL_ARROW_DOWN = 10;
    static final int SETUP_INDEX_INV_ARROW_LEFT = 11;
    static final int SETUP_INDEX_INV_ARROW_RIGHT = 12;
    static final int SETUP_INDEX_INGAME_RES_1_IMG = 13;
    static final int SETUP_INDEX_INGAME_RES_2_IMG = 14;
    static final int SETUP_INDEX_INGAME_RES_3_IMG = 15;
    static final int SETUP_INDEX_INGAME_RES_4_IMG = 16;
    static final int SETUP_INDEX_INGAME_RES_5_IMG = 17;
    static final int SETUP_INDEX_INGAME_RES_6_IMG = 18;
    static final int SETUP_INDEX_INGAME_RES_7_IMG = 19;
    static final int SETUP_INDEX_INGAME_RES_8_IMG = 20;
    static final int SETUP_INDEX_INGAME_RES_9_IMG = 21;
    public static GameResource[] systemResources;
    public static String[] actionKey_scriptIds;
    public static final int ACTION_KEY_ID_STAR = 0;
    public static final int ACTION_KEY_ID_POUND = 1;
    public static final int ACTION_KEY_ID_0 = 2;
    public static final int ACTION_KEY_ID_1 = 3;
    public static final int ACTION_KEY_ID_2 = 4;
    public static final int ACTION_KEY_ID_3 = 5;
    public static final int ACTION_KEY_ID_4 = 6;
    public static final int ACTION_KEY_ID_5 = 7;
    public static final int ACTION_KEY_ID_6 = 8;
    public static final int ACTION_KEY_ID_7 = 9;
    public static final int ACTION_KEY_ID_8 = 10;
    public static final int ACTION_KEY_ID_9 = 11;
    public static final int ACTION_KEY_ID_NUM_OF = 12;
    public static int[] actionKey_keyCodes;
    static Image INK_logo;
    public static Image bigScreenHUD_down;
    public static Image mapMenuCursor;
    public static Image imgKonamiLogo;
    public static Image imgKonamiRights;
    public static int[] rgbKonamiLogo;
    public static int logoWidth;
    public static int logoHeight;
    public static byte logoState;
    public static long logoStateStartTime;
    public static Hashtable settingsHash;
    static final String SAVED_GAME_RMS_PREFIX = "RMS_variables_";
    static int EVENT_TICK_UPDATE_TIME = 10;
    public static int cursorAnimFramesBetweenUpdates = 3;
    static boolean battlePaused = false;
    public static boolean bossDead = false;
    public static boolean superBossDead = false;
    static int popupCurrent = 0;
    static int popupNumOf = 0;
    static boolean popupActive = false;
    static int txtOffsetMenu = 0;
    static int IngameMenuOffset = 0;
    static byte popup_choice = 0;
    static int[] popupRecoveryCode = new int[5];
    static String[][] popupText = new String[5][];
    static int[] popupMaxTime = new int[5];
    public static int EXTRA_FONT_HEIGHT = 2;
    static int numOfSplashes = 0;
    static int curSplash = 0;
    static Image curSplashImage = null;
    static int curSplashBgColor = 0;
    static long curSplashTimeUnlocked = -1;
    static long curSplashTimeEnd = -1;
    static boolean firstSoundPlaying = false;
    static boolean languageChange = true;
    static long textFadePausedTime = -1;
    static final int[] engineChoiceSelectionColors = {15161432, 14231592, 12582912, 10551296, 12582912, 14231592};
    static int engineChoiceSelectionColorCounter = 0;
    static int engineChoiceSelectionAnimTimer = 0;
    static boolean ingameScrollArrows = false;
    static int ingameScrollArrowsWidth = -1;
    public static int ingameBorderSizeTop = 0;
    public static int ingameBorderSizeBottom = 0;
    public static int ingameBorderSizeLeft = 0;
    public static int ingameBorderSizeRight = 0;
    public static int INGAME_MENU_TEXT_WIDTH_MIN = 50;
    public static boolean ingameUseImageBorders = true;
    static int ingameShowRowByRow_stepSize = -1;
    static int INGAME_SHOW_ROW_BY_ROW_SHIFT = 6;
    static int TEXT_COLOR_BEN = 1838704;
    static int TEXT_COLOR_MOON = 10685452;
    static int TEXT_COLOR_KAREN = 10619786;
    public static String invFirstItemId = null;
    public static int engineDefaultMenuWidth = 0;
    private static byte LETTERS_FOR_NEW_ROW = 3;
    static String loginUser = "";
    static String loginPassword = "";
    static String loginPasswordHidden = "";
    public static byte menuScrollTickCounter = 0;
    static final String[] DEFAULT_RESOURCES = {null, "cursorIdle", "cursorPoint", "cursorBack", "cursorForwardLeft", "cursorForwardRight", "cursorForward", "cursorMenu", "dissolveEffectImg", "arrow_up", "arrow_down", "inv_arrowLeft", "inv_arrowRight", "border_top", "border_bottom", "border_left", "border_right", "corner_top_left", "corner_top_right", "corner_bottom_left", "corner_bottom_right", "delimiter"};

    public static boolean battleKeyHandling() {
        if (Application.keyNew && !attackAnim) {
            switch (Application.keyDown) {
                case -11:
                case GameCanvas.KEY_ERASE:
                case GameCanvas.KEY_RIGHT_SOFT:
                    if (MenuModel.active() && MenuModel.getCurrent().ID != 17 && MenuModel.getCurrent().ID != 30 && battlePanelMode == 2) {
                        MenuModel.closeCurrent();
                        InkScript.resume();
                    }
                    break;
                case GameCanvas.KEY_LEFT_SOFT:
                case GameCanvas.KEY_MIDDLE_SOFT:
                    if (!MenuModel.active()) {
                        if (InkInterpreter.pausedThread != null) {
                            InkInterpreter.pausedThread.resume();
                        } else if (battlePanelMode != 0) {
                            if (battlePanelMode != 1) {
                                if (battlePanelMode == 2 && Application.removeStringPrefix(Application.inkServerAllNamesWithHint(Application.charToString('V')), "inv-").length > 0) {
                                    Application.keyNew = false;
                                    if (menuInvSetup()) {
                                        createInventory(curInvCounter, false);
                                    }
                                } else if (battlePanelMode == 3) {
                                    Application.overRoomObject.executeEvent(InkCodes.EVENT_CLICK, null, false);
                                }
                            } else if (battleFightTypeActive == 0) {
                                battleFightTypeActive = 2;
                            }
                        } else if (battleFightTypeActive == 0) {
                            battleFightTypeActive = 1;
                        }
                    } else if (MenuModel.getCurrent().ID != 3 && MenuModel.getCurrent().ID != 17 && MenuModel.getCurrent().ID != 30) {
                        MenuModel.closeCurrent();
                        InkScript.resume();
                        return true;
                    }
                    break;
                case -2:
                    battleFightTypeActive = 0;
                    if (!MenuModel.active()) {
                        if (battlePanelMode == 0) {
                            battlePanelMode = 1;
                        } else if (battlePanelMode == 1) {
                            battlePanelMode = 2;
                        } else if (battlePanelMode == 2) {
                            battlePanelMode = 3;
                        }
                    }
                    break;
                case -1:
                    battleFightTypeActive = 0;
                    if (!MenuModel.active()) {
                        if (battlePanelMode == 1) {
                            battlePanelMode = 0;
                        } else if (battlePanelMode == 2) {
                            battlePanelMode = 1;
                        } else if (battlePanelMode == 3) {
                            battlePanelMode = 2;
                        }
                    }
                    break;
            }
        }
        if (MenuModel.active()) {
            return (MenuModel.getCurrent().ID == 3 || MenuModel.getCurrent().ID == 17 || MenuModel.getCurrent().ID == 30) ? false : true;
        }
        return true;
    }

    static void battleStartInit() {
        Application.cursor_OLD_X = Application.cursorX;
        Application.cursor_OLD_Y = Application.cursorY;
        battleState = 1;
        battlePanelMode = 1;
        battleFightTypeActive = 0;
        battleMode = true;
        attackAnim = false;
        Application.roomUpdateNeeded = true;
    }

    static void battleHandling() {
        switch (battleType) {
            case 1:
                if ((!Application.loading() || Application.loadingMode == 1) && !Application.roomUpdateNeeded && !Application.roomRepaintNeeded && !Application.roomRepainting && !InkScript.isWaiting()) {
                    if (InkInterpreter.pausedThread == null) {
                        SilentHillGame.battleUpdate();
                    }
                    if (Application.dissolveFXTime > 0) {
                        Application.drawDissolve();
                    }
                    drawBigScreenAddOn();
                    if (MenuModel.active()) {
                        menuPaintCurrentIngame();
                    }
                }
                break;
        }
    }

    static void battleUpdate() {
        Object battleStateValue;
        if (Application.roomImage != null) {
            Application.gfx.drawImage(Application.roomImage, -Application.roomScrollOffsetX, -Application.roomScrollOffsetY, 0);
        }
        boolean attackAnimationPainted = false;
        for (int i = 0; i < Application.roomObjects.length; i++) {
            RoomObject roomObject = Application.roomObjects[i];
            if (!battlePaused) {
                int objectBattleState = roomObject.battlePanelID != 3 ? Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETSTATE, new Integer(-1), false)) : 0;
                if (roomObject.type == 3) {
                    int battlePanelId = roomObject.battlePanelID;
                    if (battlePanelId == 0) {
                        int newBattlePanelId = Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETMETERID, new Integer(-1), false));
                        if (newBattlePanelId == RoomObject.BATTLE_PANEL_ID_HERO_HEALTH || newBattlePanelId == RoomObject.BATTLE_PANEL_ID_ENEMY_HEALTH) {
                            roomObject.battlePanelNew(newBattlePanelId);
                            roomObject.bpSetBarSize(Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETBARSIZE, new Integer(0), false)));
                            roomObject.bpSetMaxHealth(Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETMAXHEALTH, new Integer(0), false)));
                            roomObject.bpSetHealth(Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETHEALTH, new Integer(0), false)));
                        } else if (newBattlePanelId == RoomObject.BATTLE_PANEL_ID_TIMEBAR) {
                            roomObject.battlePanelNew(newBattlePanelId);
                            roomObject.bpSetBarSize(Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETBARSIZE, new Integer(0), false)));
                            roomObject.bpSetTime(Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETTIME, new Integer(0), false)));
                        } else {
                            roomObject.battlePanelNew(newBattlePanelId);
                        }
                    } else if (battlePanelId == RoomObject.BATTLE_PANEL_ID_HERO_HEALTH || battlePanelId == RoomObject.BATTLE_PANEL_ID_ENEMY_HEALTH) {
                        roomObject.bpSetHealth(Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETHEALTH, new Integer(0), false)));
                    } else if (battlePanelId == RoomObject.BATTLE_PANEL_ID_TIMEBAR && (battleStateValue = roomObject.executeEvent(InkCodes.EVENT_GETTIME, new Integer(0), false)) != null) {
                        int time = Application.toInt(battleStateValue);
                        roomObject.bpSetTime(time);
                        if (battleState < 2 || (!attackAnim && time == 0)) {
                            Application.toInt(roomObject.executeEvent(InkCodes.EVENT_GETSTATE, new Integer(0), false));
                        }
                    }
                } else if (objectBattleState == 1 || (battleState < 2 && objectBattleState < 2 && roomObject.script != null && (roomObject.script.hasEvent(InkCodes.EVENT_GETHEROATTACKANIM1) || roomObject.script.hasEvent(InkCodes.EVENT_GETHEROATTACKANIM2)))) {
                    if (battleFightTypeActive != 0 || attackAnim) {
                        if (attackAnim) {
                            if (battleState == 1) {
                                if (battleFightTypeActive == 2) {
                                    roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 4);
                                } else if (battleFightTypeActive == 1) {
                                    roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 5);
                                }
                            }
                        } else if (battleFightTypeActive == 2) {
                            if (Application.toBoolean(roomObject.executeEvent(InkCodes.EVENT_ATTACK1, new Integer(0), false))) {
                                roomObject.animationTime = 0L;
                                roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 4);
                            }
                        } else if (battleFightTypeActive == 1 && Application.toBoolean(roomObject.executeEvent(InkCodes.EVENT_ATTACK2, new Integer(0), false))) {
                            roomObject.animationTime = 0L;
                            roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 5);
                        }
                    } else if (roomObject.animationTime > 0) {
                        roomObject.animationTime = 0L;
                    }
                    battleState = objectBattleState > 0 ? objectBattleState : battleState;
                } else if (objectBattleState > 1 || battleState > 1) {
                    if (battleState < 2 && attackAnim) {
                        attackAnim = false;
                    }
                    battleFightTypeActive = 0;
                    if (roomObject.script != null && (attackAnim || Application.toBoolean(roomObject.executeEvent(InkCodes.EVENT_ENEMYATTACK, new Integer(0), false)))) {
                        if ((objectBattleState == 2 || battleState == 2) && roomObject.script.hasEvent(InkCodes.EVENT_GETENEMYATTACKANIM1)) {
                            if (!attackAnim) {
                                roomObject.animationTime = 0L;
                            }
                            roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 1);
                            attackAnimationPainted = true;
                        } else if ((objectBattleState == 3 || battleState == 3) && roomObject.script.hasEvent(InkCodes.EVENT_GETENEMYATTACKANIM2)) {
                            if (!attackAnim) {
                                roomObject.animationTime = 0L;
                            }
                            roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 2);
                            attackAnimationPainted = true;
                        } else if ((objectBattleState == 4 || battleState == 4) && roomObject.script.hasEvent(InkCodes.EVENT_GETENEMYATTACKANIM3)) {
                            if (!attackAnim) {
                                roomObject.animationTime = 0L;
                            }
                            roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 3);
                            attackAnimationPainted = true;
                        }
                    }
                    battleState = objectBattleState > 1 ? objectBattleState : battleState;
                }
            }
            if (!attackAnim) {
                if (roomObject.runAnimLoops > 0) {
                    roomObject.runAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY);
                } else {
                    roomObject.animPaint(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY);
                }
            }
            roomObject.battlePanelUpdate(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY);
        }
        if (!attackAnim && Application.overRoomObject != null) {
            systemResources[7].paint(Application.gfx, Application.overRoomObject.x + Application.overRoomObject.width, Application.overRoomObject.y + (Application.overRoomObject.height >> 1), 0);
        }
        battleState = attackAnimationPainted ? battleState : 1;
    }

    public static void menuPaintLoadingBar() {
        if (Application.loadingMode != 0 && Application.loadingMode != 2 && !Application.loadBarActive) {
            Application.loadingBarMarkerX = -20;
            return;
        }
        int barY = Application.canvasHeight - 6;
        Application.gfx.setClip(0, barY, Application.canvasWidth, 6);
        Application.gfx.setColor(0);
        Application.gfx.fillRect(0, barY, Application.canvasWidth, 6);
        Application.gfx.setColor(Application.loadingBarEngineMode ? 13487565 : ingameBackgroundColor);
        Application.gfx.fillRect(0, barY + 1, Application.canvasWidth, 4);
        Application.gfx.setColor(0);
        if (Application.resourceStreamComplete > -1) {
            int progressWidth = (Application.resourceStreamComplete * Application.canvasWidth) >> 10;
            Application.gfx.fillRect(0, barY, progressWidth, 6);
            Application.gfx.setColor(Application.loadingBarEngineMode ? 16777215 : LOADING_BAR_COLOR_INGAME_MARKER);
            Application.gfx.fillRect(1, barY + 1, progressWidth - 2, 4);
            if (Application.loadingBarMarkerX < Application.canvasWidth) {
                Application.loadingBarMarkerX++;
            } else {
                Application.loadingBarMarkerX = -20;
            }
        } else {
            Application.gfx.fillRect(Application.loadingBarMarkerX, barY, 20, 6);
            Application.gfx.setColor(Application.loadingBarEngineMode ? 16777215 : LOADING_BAR_COLOR_INGAME_MARKER);
            Application.gfx.fillRect(Application.loadingBarMarkerX + 1, barY + 1, SETUP_INDEX_INGAME_RES_6_IMG, 4);
            if (Application.loadingBarMarkerX < Application.canvasWidth) {
                Application.loadingBarMarkerX += 3;
            } else {
                Application.loadingBarMarkerX = -20;
            }
        }
        Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
    }

    static void popupCreate(String text, int recoveryCode) {
        popupCreate(text, recoveryCode, -1);
    }

    static void popupCreate(String text, int recoveryCode, int maxTime) {
        if (popupNumOf < 4) {
            popupText[popupNumOf] = wrapString(text, (Application.canvasWidth - 8) - 8);
            popupRecoveryCode[popupNumOf] = recoveryCode;
            popupMaxTime[popupNumOf] = maxTime;
            popup_choice = (byte) 0;
            if (!popupActive) {
                popupCurrent = popupNumOf;
                if (popupMaxTime[popupCurrent] == -1) {
                    popupEndTime = -1L;
                } else {
                    popupEndTime = System.currentTimeMillis() + ((long) popupMaxTime[popupCurrent]);
                }
            }
            popupActive = true;
            popupNumOf++;
        }
    }

    static void popupPaint(Graphics g) {
        int maxTextWidth = 0;
        boolean showSoftKeys = popupRecoveryCode[popupCurrent] == 2 || popupRecoveryCode[popupCurrent] == 4 || popupRecoveryCode[popupCurrent] == 5;
        boolean useImageBorders = (Application.mainMenuActive || systemResources == null || systemResources[13] == null) ? false : true;
        for (int i = 0; i < popupText[popupCurrent].length; i++) {
            maxTextWidth = Application.max(maxTextWidth, currentFont.stringWidth(popupText[popupCurrent][i]));
        }
        int length = (popupText[popupCurrent].length * (engineFontHeight + 2)) - 2;
        int popupX = (Application.canvasCenterX - (maxTextWidth >> 1)) - 4;
        int popupY = (Application.canvasCenterY - (length >> 1)) - 4;
        int popupWidth = maxTextWidth + 8;
        int popupHeight = length + 8;
        g.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
        if (useImageBorders) {
            g.setColor(ingameBackgroundColor);
            g.fillRect(popupX, popupY, popupWidth, popupHeight);
            g.setColor(ingameTextColor);
            menuPaintIngameImageBorders(-1, popupX, popupY, popupWidth, popupHeight, false, 0, false, 0, 0);
        } else {
            g.setColor(15125637);
            g.fillRect(popupX, popupY, popupWidth, popupHeight);
            g.setColor(0);
            g.drawRect(popupX, popupY, popupWidth, popupHeight);
        }
        int textY = popupY + 4;
        int lineIndex = 0;
        while (lineIndex < popupText[popupCurrent].length) {
            g.drawString(popupText[popupCurrent][lineIndex], popupX + 4, textY, 20);
            lineIndex++;
            textY += engineFontHeight + 2;
        }
        if (showSoftKeys) {
            int softKeyBarHeight = 2 + engineFontHeight + 2;
            if (useImageBorders) {
                g.setColor(ingameBackgroundColor);
                g.fillRect(0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, softKeyBarHeight);
                g.setColor(ingameTextColor);
                menuPaintIngameImageBorders(-1, 0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, Application.canvasHeight, false, 0, false, 0, 0);
            } else {
                g.setColor(15125637);
                g.fillRect(0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, softKeyBarHeight);
                g.setColor(0);
                g.drawLine(0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, Application.canvasHeight - softKeyBarHeight);
            }
            g.drawString(Application.getString(TextId.JAVA_APP_INK_YES), 4, Application.canvasHeight - 2, 36);
            g.drawString(Application.getString(TextId.JAVA_APP_INK_NO), Application.canvasWidth - 4, Application.canvasHeight - 2, 40);
        }
    }

    static void popupKeyHandling() {
        if ((popupEndTime == -1 || System.currentTimeMillis() < popupEndTime) && (!Application.keyNew || Application.keyDown == 0 || System.currentTimeMillis() < popupMinTimeEnds)) {
            return;
        }
        switch (popupRecoveryCode[popupCurrent]) {
            case 0:
                if (Application.keyDown != 0 && Application.keyDown != GameCanvas.KEY_LEFT_SOFT) {
                    return;
                }
                break;
            case 1:
                createMainMenu();
                break;
            case 2:
                if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT) {
                    popup_choice = (byte) 1;
                } else if (Application.keyDown != GameCanvas.KEY_RIGHT_SOFT) {
                    return;
                } else {
                    popup_choice = (byte) 2;
                }
                break;
            case 3:
                Application.midlet.destroyApp(true);
                break;
            case 4:
                if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT) {
                    Application.endGame();
                } else if (Application.keyDown != GameCanvas.KEY_RIGHT_SOFT) {
                    return;
                }
                break;
            case 5:
                if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT) {
                    startNewGame(Application.gameId);
                    Application.inkServerGamesSaved = Application.inkServerGetNumOfGames(2);
                } else if (Application.keyDown != GameCanvas.KEY_RIGHT_SOFT) {
                    return;
                }
                break;
            case 7:
                if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT || Application.keyDown == GameCanvas.KEY_MIDDLE_SOFT || Application.keyDown == 7) {
                    Application.midlet.destroyApp(true);
                } else if (Application.keyDown != GameCanvas.KEY_RIGHT_SOFT) {
                    return;
                } else {
                    createMainMenu();
                }
                break;
            case POPUP_ACTION_CODE_DEMO_END:
                if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT || Application.keyDown == GameCanvas.KEY_MIDDLE_SOFT || Application.keyDown == 7) {
                    popupCreate("Drugi ekran", 9);
                } else if (Application.keyDown != GameCanvas.KEY_RIGHT_SOFT) {
                    return;
                } else {
                    createMainMenu();
                }
                break;
            case POPUP_ACTION_CODE_DEMO_END_2:
                if (Application.keyDown != GameCanvas.KEY_LEFT_SOFT && Application.keyDown != GameCanvas.KEY_MIDDLE_SOFT && Application.keyDown != 7) {
                    return;
                }
                if (Application.demoMode == null || Application.demoUrl == null) {
                    popupCreate("no demo in jad", 10);
                } else if (Integer.parseInt(Application.demoMode) == 2 && Application.demoUrl != null) {
                    popupCreate(Application.getString(TextId.STR_DOWNLOAD_NOW), 10);
                } else if (Integer.parseInt(Application.demoMode) == 1 && Application.demoUrl != null) {
                    Application.enableDemoDissolve = false;
                    Application.exitTrial = true;
                    createMainMenu();
                    createEngineFullscreenEngineMenu(Application.getString(TextId.STR_DEMO_END_TEXT));
                }
                break;
            case 10:
                if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT || Application.keyDown == GameCanvas.KEY_MIDDLE_SOFT || Application.keyDown == 7) {
                    if (Application.demoMode != null && Application.demoUrl != null) {
                        if (Integer.parseInt(Application.demoMode) != 2 || Application.demoUrl == null) {
                            return;
                        }
                        try {
                            Application.midlet.platformRequest(Application.demoUrl);
                            break;
                        } catch (Exception e) {
                        }
                        Application.midlet.destroyApp(true);
                    }
                } else {
                    if (Application.keyDown != GameCanvas.KEY_RIGHT_SOFT) {
                        return;
                    }
                    Application.enableDemoDissolve = false;
                    createMainMenu();
                }
                break;
        }
        popupSetNext();
    }

    static void popupSetNext() {
        popupCurrent++;
        if (popupCurrent >= popupNumOf) {
            popupNumOf = 0;
            popupActive = false;
            return;
        }
        popupMinTimeEnds = System.currentTimeMillis() + 500;
        if (popupMaxTime[popupCurrent] == -1) {
            popupEndTime = -1L;
        } else {
            popupEndTime = System.currentTimeMillis() + ((long) popupMaxTime[popupCurrent]);
        }
    }

    static void logoStart() {
        MenuModel.closeAll();
        if (imgKonamiLogo == null) {
            splashStart();
            return;
        }
        logoWidth = imgKonamiLogo.getWidth();
        logoHeight = imgKonamiLogo.getHeight();
        rgbKonamiLogo = new int[logoWidth * logoHeight];
        imgKonamiLogo.getRGB(rgbKonamiLogo, 0, logoWidth, 0, 0, logoWidth, logoHeight);
        setTransparency(0);
        imgKonamiLogo = null;
        logoState = (byte) 0;
        logoStateStartTime = System.currentTimeMillis();
        logoHandling();
    }

    static void setTransparency(int value) {
        if (rgbKonamiLogo != null) {
            for (int i2 = 0; i2 < rgbKonamiLogo.length; i2++) {
                int[] logoPixels = rgbKonamiLogo;
                int pixelIndex = i2;
                logoPixels[pixelIndex] = logoPixels[pixelIndex] & 16777215;
                int[] transparentLogoPixels = rgbKonamiLogo;
                int transparentPixelIndex = i2;
                transparentLogoPixels[transparentPixelIndex] = transparentLogoPixels[transparentPixelIndex] | (value << 24);
            }
        }
    }

    static void logoHandling() {
        switch (logoState) {
            case 0:
                if (System.currentTimeMillis() - logoStateStartTime >= 167) {
                    setTransparency(63);
                    logoState = (byte) 25;
                    logoStateStartTime = System.currentTimeMillis();
                }
                break;
            case 25:
                if (System.currentTimeMillis() - logoStateStartTime >= 167) {
                    setTransparency(127);
                    logoState = (byte) 50;
                    logoStateStartTime = System.currentTimeMillis();
                }
                break;
            case 50:
                if (System.currentTimeMillis() - logoStateStartTime >= 167) {
                    setTransparency(191);
                    logoState = (byte) 75;
                    logoStateStartTime = System.currentTimeMillis();
                }
                break;
            case 75:
                if (System.currentTimeMillis() - logoStateStartTime >= 167) {
                    setTransparency(255);
                    logoState = (byte) 100;
                    logoStateStartTime = System.currentTimeMillis();
                }
                break;
            case 100: // Fully opaque logo frame.
                if (System.currentTimeMillis() - logoStateStartTime >= 1000) {
                    rgbKonamiLogo = null;
                    if (Application.curLanguageId == null) {
                        MenuModel.closeCurrent();
                        MenuModel createdMenu = SilentHillGame.menuCreate(20, engineDefaultMenuWidth);
                        createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
                        for (int i = 0; i < Application.languages.length; i++) {
                            createdMenu.addChoice(i, Application.getString(Application.language_text_ids[i]));
                        }
                        createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), null);
                    } else {
                        createVibraSoundMenu();
                    }
                }
                break;
        }
    }

    static void logoPaint() {
        Application.gfx.setColor(16777215);
        Application.gfx.fillRect(0, 0, Application.canvasWidth, Application.canvasHeight);
        if (rgbKonamiLogo != null) {
            Application.gfx.drawRGB(rgbKonamiLogo, 0, logoWidth, (Application.canvasCenterX + 5) - (logoWidth >> 1), Application.canvasCenterY - (logoHeight >> 1), logoWidth, logoHeight, true);
        }
    }

    static void splashStart() {
        MenuModel.closeAll();
        try {
            splashHash = Application.readIni(Application.openJar("splash.ini"));
            numOfSplashes = Application.toInt(splashHash.get("splashes"));
            if (Application.setGameSpecificData(1)) {
                firstSoundPlaying = true;
                GameCanvas.playSound("sh_bgmusic", 1);
            }
        } catch (Exception e) {
            splashHash = null;
            splashHandling();
        }
    }

    private static boolean splashSetNext() {
        curSplash++;
        if (curSplash <= numOfSplashes) {
            try {
                curSplashImage = Image.createImage((String) splashHash.get(new StringBuffer().append(curSplash).append(".image").toString()));
                curSplashBgColor = Integer.parseInt((String) splashHash.get(new StringBuffer().append(curSplash).append(".bgColor").toString()), SETUP_INDEX_INGAME_RES_4_IMG);
                curSplashTimeUnlocked = System.currentTimeMillis() + ((long) Application.toInt((String) splashHash.get(new StringBuffer().append(curSplash).append(".timeMin").toString())));
                curSplashTimeEnd = System.currentTimeMillis() + ((long) Application.toInt((String) splashHash.get(new StringBuffer().append(curSplash).append(".timeMax").toString())));
                return true;
            } catch (Exception e) {
            }
        }
        curSplashImage = null;
        curSplashTimeUnlocked = -1L;
        curSplashTimeEnd = -1L;
        return false;
    }

    private static boolean splashMoreExists() {
        return curSplash + 1 <= numOfSplashes;
    }

    static void splashHandling() {
        if ((!Application.keyNew || System.currentTimeMillis() <= curSplashTimeUnlocked) && System.currentTimeMillis() <= curSplashTimeEnd) {
            return;
        }
        if (splashMoreExists()) {
            if (splashSetNext()) {
                return;
            }
            splashHandling();
        } else {
            splashHash = null;
            curSplashImage = null;
            createMainMenu();
        }
    }

    static void splashPaint() {
        Application.gfx.setColor(curSplashBgColor);
        Application.gfx.fillRect(0, 0, Application.canvasWidth, Application.canvasHeight);
        Application.gfx.drawImage(imgKonamiRights, 0, Application.canvasHeight - 30, 0);
        if (SilentHillGame.INK_menu_logo != null) {
            Application.gfx.drawImage(SilentHillGame.INK_menu_logo, Application.canvasCenterX, engineHeaderImageHeight + 2, SETUP_INDEX_INGAME_RES_4_IMG | 1);
        }
        if (curSplashImage != null) {
            Application.gfx.drawImage(curSplashImage, 0, Application.canvasCenterY, 6);
        }
    }

    static void appPaint() {
        if (Application.appInited) {
            if (splashHash != null) {
                splashPaint();
                return;
            }
            if (Application.mainMenuActive) {
                menuPaintCurrentEngine();
                return;
            }
            if (battleMode) {
                battleHandling();
                return;
            }
            if ((Application.loading() && Application.loadingMode != 1) || Application.roomUpdateNeeded || Application.roomRepaintNeeded || Application.roomRepainting) {
                return;
            }
            if (InkScript.isWaiting() && !Application.firstLoopInWait) {
                Application.firstLoopInWait = false;
                return;
            }
            if (InkInterpreter.pausedThread == null && roomObjectTick != null) {
                int previousTickTimer = tickTimerUpdateInterval;
                tickTimerUpdateInterval = previousTickTimer - 1;
                if (previousTickTimer == 0) {
                    tickTimerUpdateInterval = EVENT_TICK_UPDATE_TIME;
                    roomObjectTick.executeEvent(InkCodes.EVENT_TICK, null, false);
                }
            }
            if (Application.gotoDissolveFXCounter < 0) {
                roomPaint();
            }
            if (Application.gotoDissolveFXCounter > -3) {
                if (Application.gotoDissolveFXColor == -1) {
                    Application.gfx.setColor(Application.ingameBgColor);
                } else {
                    Application.gfx.setColor(Application.gotoDissolveFXColor);
                }
                Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
                int newDissolveCounter = Application.gotoDissolveFXCounter - 1;
                Application.gotoDissolveFXCounter = newDissolveCounter;
                if (newDissolveCounter >= 2 || Application.gotoDissolveFXCounter <= -2) {
                    int tileWidth = systemResources[8].imageWidth;
                    int tileHeight = systemResources[8].imageHeight;
                    for (int tileX = 0; tileX < Application.canvasWidth; tileX += tileWidth) {
                        for (int tileY = 0; tileY < Application.canvasHeight; tileY += tileHeight) {
                            systemResources[8].paintSimple(Application.gfx, tileX, tileY, 0);
                        }
                    }
                } else {
                    Application.gfx.fillRect(0, 0, Application.canvasWidth, Application.canvasHeight);
                }
            } else if (Application.dissolveFXTime > 0) {
                Application.drawDissolve();
            }
            paintAnimCursorMask();
            drawBigScreenAddOn();
            if (MenuModel.active()) {
                textFadeCenter();
                menuPaintCurrentIngame();
                return;
            }
            if (Application.hideCursor) {
                return;
            }
            GameResource cursor = null;
            if (Application.overRoomObject == null) {
                cursor = specialCursorIdle != null ? specialCursorIdle : systemResources[1];
            } else if (hoverCursor != null) {
                cursor = hoverCursor;
            } else {
                String moveDir = Application.overRoomObject.getMoveDir();
                if (moveDir == null) {
                    cursor = systemResources[2];
                } else if (moveDir.equals("back")) {
                    cursor = systemResources[3];
                } else if (moveDir.equals("left")) {
                    cursor = systemResources[4];
                } else if (moveDir.equals("right")) {
                    cursor = systemResources[5];
                } else if (moveDir.equals("forward")) {
                    cursor = systemResources[6];
                }
            }
            if (cursor != null) {
                cursor.paint(Application.gfx, Application.cursorX - Application.roomScrollOffsetX, Application.cursorY - Application.roomScrollOffsetY, 0);
            }
        }
    }

    public static void paintAnimCursorMask() {
        if (cursorAnimActive) {
            int cursorX = Application.cursorX - Application.roomScrollOffsetX;
            int cursorY = Application.cursorY - Application.roomScrollOffsetY;
            int maskLeft = cursorX - (cursorAnim[cursorAnimFrameCounter].imageWidth >> 1);
            int maskRight = cursorX + (cursorAnim[cursorAnimFrameCounter].imageWidth >> 1);
            int maskTop = cursorY - (cursorAnim[cursorAnimFrameCounter].imageHeight >> 1);
            int maskBottom = cursorY + (cursorAnim[cursorAnimFrameCounter].imageHeight >> 1);
            Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
            cursorAnim[cursorAnimFrameCounter].paint(Application.gfx, cursorX, cursorY, 0);
            Application.gfx.setColor(Application.ingameBgColor);
            Application.gfx.fillRect(maskRight, 0, Application.canvasWidth, Application.canvasHeight);
            Application.gfx.fillRect(0, 0, maskLeft, Application.canvasHeight);
            Application.gfx.fillRect(0, maskBottom, Application.canvasWidth, Application.canvasHeight);
            Application.gfx.fillRect(0, 0, Application.canvasWidth, maskTop);
            if (cursorAnimCounter != cursorAnimFramesBetweenUpdates) {
                cursorAnimCounter++;
                return;
            }
            cursorAnimCounter = 0;
            if (cursorAnimFrameCounter == cursorAnim.length - 1) {
                cursorAnimFrameCounter = 0;
            } else {
                cursorAnimFrameCounter++;
            }
        }
    }

    public static void drawBigScreenAddOn() {
        if (Application.canvasHeight <= Application.roomHeight || bigScreenHUD_down == null) {
            return;
        }
        Application.gfx.drawImage(bigScreenHUD_down, Application.canvasCenterX, (-Application.roomScrollOffsetY) + Application.roomHeight, 17);
    }

    static Object executeCommand_setSystemVar(InkInterpreter scrThread, Object[] arguments, Object returnValue) {
        int length = arguments == null ? 0 : arguments.length;
        if (((String) arguments[0]).equals("s_noScrollHor")) {
            Application.scrollRoomHor = !Application.toBoolean(arguments[1]);
        } else if (((String) arguments[0]).equals("s_noScrollVer")) {
            Application.scrollRoomVer = !Application.toBoolean(arguments[1]);
        } else if (((String) arguments[0]).equals("s_hideCursor")) {
            Application.hideCursor = Application.toBoolean(arguments[1]);
        } else if (((String) arguments[0]).equals("s_animCursor")) {
            InkScript script = scrThread.script;
            InkScript.setVariable((String) arguments[0], arguments[1]);
            cursorAnimActive = Application.toBoolean(arguments[1]);
            if (cursorAnimActive) {
                cursorAnimFramesBetweenUpdates = Application.toInt(arguments[2]);
                cursorAnim = new GameResource[length - 3];
                for (int i = 0; i < cursorAnim.length; i++) {
                    cursorAnim[i] = GameResource.getImage((String) arguments[i + 3], 0);
                }
            } else {
                cursorAnim = null;
            }
        } else if (((String) arguments[0]).equals("s_0")) {
            ingameBackgroundColor = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_1")) {
            ingameBackgroundColorSelected = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_2")) {
            ingameBorderSize = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_3")) {
            ingameBorderColor = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_4")) {
            ingameHeaderColor = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_5")) {
            ingameLineSpacing = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_6")) {
            ingameMargin = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_7")) {
            ingameTextColor = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_8")) {
            Application.ingameBgColor = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_use")) {
            Application.useItemSayText = arguments[1].toString();
        } else if (((String) arguments[0]).equals("s_useAny")) {
            Application.useAnyItemSayText = arguments[1].toString();
        } else if (((String) arguments[0]).equals("s_dissolveFX")) {
            Application.gotoDissolveFXEnabled = Application.toBoolean(arguments[1]);
            if (length > 2) {
                Application.gotoDissolveFXColor = Application.toInt(arguments[2]);
            }
        } else if (((String) arguments[0]).equals("s_battleType")) {
            battleType = Application.toInt(arguments[1]);
            if (length > 2 && battleType > 0) {
                healthBarFillImg = GameResource.getImage((String) arguments[2], 0).image;
                if (length > 3) {
                    healthOverlapImg = GameResource.getImage((String) arguments[3], 0).image;
                }
            }
        } else if (((String) arguments[0]).equals("s_useImageBorders")) {
            ingameUseImageBorders = Application.toBoolean(arguments[1]);
            if (ingameUseImageBorders) {
                Application.updateSystemResourceValuesAll();
            } else {
                ingameBorderSizeTop = 0;
                ingameBorderSizeBottom = 0;
                ingameBorderSizeLeft = 0;
                ingameBorderSizeRight = 0;
                ingameDelimiterHeight = 0;
            }
        } else if (((String) arguments[0]).equals("s_dynamicSayHeight")) {
            ingameUseDynamicSayChoice = Application.toBoolean(arguments[1]);
        } else if (((String) arguments[0]).equals("s_useScrollArrows")) {
            ingameScrollArrows = Application.toBoolean(arguments[1]);
        } else if (((String) arguments[0]).equals("s_delimiterMode")) {
            ingameDelimiterMode = Application.toInt(arguments[1]);
        } else if (((String) arguments[0]).equals("s_firstInventoryItem")) {
            invFirstItemId = arguments[1].toString();
        } else if (((String) arguments[0]).equals("s_storyScreenRowByRow")) {
            ingameShowRowByRow_available = Application.toBoolean(arguments[1]);
        } else if (((String) arguments[0]).equals("s_cursorHintDisable")) {
            SilentHillGame.cursorHintEnable = !Application.toBoolean(arguments[1]);
            InkScript cursorHintScript = scrThread.script;
            InkScript.setVariable((String) arguments[0], arguments[1]);
        } else if (((String) arguments[0]).equals("s_popupMes")) {
            popupCreate((String) arguments[1], 0);
        } else {
            InkScript fallbackScript = scrThread.script;
            InkScript.setVariable((String) arguments[0], arguments[1]);
            if (!Application.mainMenuActive && (length != 3 || Application.toBoolean(arguments[2]))) {
                MenuModel.closeAll();
            }
        }
        return returnValue;
    }

    static void executeCommand_inventoryAdd(InkInterpreter scrThread, String id, int amount, boolean showRevievedMenu) {
        InkScript.setInventory(id, InkScript.getInventorySize(id) + amount);
        if (!Application.mainMenuActive) {
            MenuModel.closeAll();
        }
        Application.roomUpdateNeeded = true;
        if (showRevievedMenu) {
            inventoryAdd(id, -1);
            scrThread.status = 4;
        }
    }

    static Object executeCommand(InkInterpreter scrThread, int commandID, Object[] arguments) {
        Object choiceIdValue;
        int argumentCount = arguments == null ? 0 : arguments.length;
        Integer commandResult = null;
        switch (commandID) {
            case InkCodes.COMMAND_SETVAR:
                if (((String) arguments[0]).startsWith("s_")) {
                    executeCommand_setSystemVar(scrThread, arguments, null);
                } else {
                    InkScript script = scrThread.script;
                    InkScript.setVariable((String) arguments[0], arguments[1]);
                    if (!Application.mainMenuActive && !battleMode && (argumentCount != 3 || Application.toBoolean(arguments[2]))) {
                        MenuModel.closeAll();
                    }
                }
                Application.roomUpdateNeeded = true;
                break;
            case InkCodes.COMMAND_INVENTORYADD:
                SilentHillGame.executeCommand_inventoryAdd(scrThread, (String) arguments[0], argumentCount > 1 ? InkInterpreter.integerArgument(arguments[1]) : 1, argumentCount > 2 ? Application.toBoolean(arguments[2]) : true);
                break;
            case InkCodes.COMMAND_GOTO:
                Application.roomUpdateNeeded = false;
                Application.roomRepaintNeeded = false;
                SilentHillGame.softkeyPainting = false;
                Application.painting = false;
                Application.repaintCanvasIfPossible();
                String roomId = (String) arguments[0];
                if (roomId.equals("moon_intro") || roomId.equals("karen_intro")) {
                    popupActive = false;
                }
                if (roomId.equals("karen_end")) {
                    battleState = 0;
                }
                if (Application.roomGetHistorySize() == 0 && roomId.equals("back")) {
                    roomId = null;
                } else {
                    Application.cursorX = Application.roomWidth >> 1;
                    Application.cursorY = Application.roomHeight >> 1;
                    Application.roomScrollOffsetX = Application.max(0, (Application.roomWidth - Application.canvasWidth) >> 1);
                    Application.roomScrollOffsetY = Application.max(0, (Application.roomHeight - Application.canvasHeight) >> 1);
                    Application.smoothScrollDisable = true;
                }
                if (Application.gotoDissolveFXEnabled && argumentCount > 1) {
                    Application.gotoDissolveFXIsSet = Application.toBoolean(arguments[1]);
                }
                boolean useDissolve = argumentCount > 2 ? Application.toBoolean(arguments[2]) : false;
                if (!Application.mainMenuActive) {
                    MenuModel.closeAll();
                }
                roomInit(roomId, useDissolve);
                scrThread.status = 2;
                SilentHillGame.softkeyPainting = true;
                break;
            case InkCodes.COMMAND_ADDCHOICE:
                String choiceText = (String) arguments[0];
                String header = null;
                MenuModel menu = InkScript.choiceMenu;
                if (argumentCount < 2) {
                    choiceIdValue = menu != null ? new Integer(menu.countChoices() + 1) : new Integer(1);
                } else {
                    choiceIdValue = arguments[1];
                    if (argumentCount == 3) {
                        header = (String) arguments[2];
                    }
                }
                if (menu == null) {
                    int menuX = ingameUseImageBorders ? ingameBorderSizeLeft : ingameBorderSize;
                    MenuModel createdMenu = SilentHillGame.menuCreate(2, Application.canvasWidth - ((ingameMargin << 1) + (ingameUseImageBorders ? ingameBorderSizeLeft + ingameBorderSizeRight : ingameBorderSize << 1)));
                    InkScript.choiceMenu = createdMenu;
                    menu = createdMenu;
                    menu.setPosition(menuX, Application.canvasHeight - ((((currentFontHeight + ingameLineSpacing) * 4) - ingameLineSpacing) + (ingameMargin << 1)));
                }
                if (header != null) {
                    menu.setTop(header);
                }
                menu.addChoice(choiceIdValue, choiceText);
                break;
            case InkCodes.COMMAND_BATTLESTART:
                SilentHillGame.battleStartInit();
                break;
            case InkCodes.COMMAND_BATTLEMODE:
                commandResult = battleMode ? new Integer(1) : new Integer(0);
                break;
            case InkCodes.COMMAND_BATTLESTOP:
                if (Application.cursor_OLD_X > 0 && Application.cursor_OLD_Y > 0) {
                    Application.cursorX = Application.cursor_OLD_X;
                    Application.cursorY = Application.cursor_OLD_Y;
                }
                attackAnim = false;
                battleMode = false;
                if (MenuModel.active()) {
                    MenuModel.closeAll();
                }
                Application.roomUpdateNeeded = true;
                break;
            case InkCodes.COMMAND_STARTDISSOLVE:
                Application.dissolveFXColor = Application.toInt(arguments[1]);
                Application.dissolveFXImgTimer = Application.toInt(arguments[2]);
                Application.dissolveFXColorTimer = Application.toInt(arguments[3]);
                if (argumentCount > 4) {
                    Application.dissolveFXLoops = Application.toInt(arguments[4]);
                }
                Application.dissolveFXTime = System.currentTimeMillis();
                Application.bloodEffectImg1 = GameResource.getImage("new_monster_hurt1", 0).image;
                Application.bloodEffectImg2 = GameResource.getImage("new_monster_hurt2", 0).image;
                Application.bloodEffectX = new int[3][];
                Application.bloodEffectY = new int[3][];
                Application.bloodEffectX[0] = new int[5];
                Application.bloodEffectY[0] = new int[5];
                for (int i3 = 0; i3 < 5; i3++) {
                    Application.bloodEffectX[0][i3] = (Application.canvasWidth >> 2) + Application.random(Application.canvasWidth >> 1);
                    Application.bloodEffectY[0][i3] = (Application.canvasHeight >> 2) + Application.random(Application.canvasHeight >> 1);
                }
                Application.bloodEffectSprayX1 = (Application.canvasWidth >> 2) + Application.random(Application.canvasWidth >> 2);
                Application.bloodEffectSprayY1 = (Application.canvasWidth >> 1) + Application.random(Application.canvasWidth >> 2);
                Application.bloodEffectSprayX2 = (Application.canvasWidth >> 1) + Application.random(Application.canvasWidth >> 2);
                Application.bloodEffectSprayY2 = (Application.canvasWidth >> 2) + Application.random(Application.canvasWidth >> 2);
                Application.bloodEffectBlur = 1;
                Application.bloodEffectShift = 0;
                Application.bloodEffectPhase = (byte) 0;
                break;
            case InkCodes.COMMAND_RUNANIMATION:
                if (scrThread.roomObject.animationTime <= 0 || !attackAnim) {
                    scrThread.roomObject.runAnimLoops = argumentCount > 0 ? InkInterpreter.integerArgument(arguments[0]) : 1;
                    Application.roomRepaintNeeded = true;
                }
                break;
            case InkCodes.COMMAND_SETCURSOR:
                if (argumentCount > 0) {
                    specialCursorIdle = GameResource.getImage((String) arguments[0], 0);
                    if (argumentCount > 1) {
                        hoverCursor = GameResource.getImage((String) arguments[1], 0);
                    }
                }
                break;
            case InkCodes.COMMAND_RESETCURSOR:
                specialCursorIdle = null;
                hoverCursor = null;
                break;
            case InkCodes.COMMAND_SETACTIONKEY:
                String actionKeyName = (String) arguments[0];
                String scriptId = (String) arguments[1];
                int actionKeyId = actionKeyIdConvert(actionKeyName);
                if (actionKeyId != -1) {
                    actionKey_scriptIds[actionKeyId] = scriptId;
                }
                break;
            case InkCodes.COMMAND_UNSETACTIONKEY:
                int secondaryActionKeyId = actionKeyIdConvert((String) arguments[0]);
                if (secondaryActionKeyId != -1) {
                    actionKey_scriptIds[secondaryActionKeyId] = null;
                }
                break;
            case InkCodes.COMMAND_TEXTFADE:
                textFadeList = new int[argumentCount - 1];
                for (int i4 = 1; i4 < argumentCount; i4++) {
                    textFadeList[i4 - 1] = InkInterpreter.integerArgument(arguments[i4]);
                }
                textFadeCenterStart((String) arguments[0]);
                scrThread.status = 4;
                break;
        }
        return commandResult;
    }

    static MenuModel menuCreate(int menuId, int maxTextWidth) {
        MenuModel menu = new MenuModel();
        menu.ID = menuId;
        menu.maxTextWidth = Application.max(maxTextWidth, INGAME_MENU_TEXT_WIDTH_MIN);
        if (ingameScrollArrows) {
            if (ingameScrollArrowsWidth == -1) {
                ingameScrollArrowsWidth = Application.max(systemResources[9].imageWidth, systemResources[10].imageWidth);
            }
            if (menu.ID == 6 || menu.ID == MENU_SAY_FULLSCREEN || menu.ID == 2) {
                menu.maxTextWidth -= ingameMargin + ingameScrollArrowsWidth;
            }
        } else if (menu.ID == MENU_SAY_FULLSCREEN && ingameShowRowByRow_available) {
            menu.ingameShowRowByRow_use = true;
        }
        if (MenuModel.active()) {
            MenuModel.getCurrent().setCurrent(false);
        }
        menu.setCurrent(true);
        MenuModel.stack.addElement(menu);
        menu.choiceIDs = new Vector();
        menu.choiceTexts = new Vector();
        menu.textScrolling = true;
        menu.selectedChoiceNr = 0;
        return menu;
    }

    public static void menuPaintIngameImageBorders(int ID, int x, int y, int width, int height, boolean delimiter1, int delimiter1_y, boolean delimiter2, int delimiter2_y, int ingameDelimiterNumOf) {
        int leftDelimiterX;
        int rightDelimiterX;
        boolean drawBottomBorderParts = true;
        if (ID == 6 || ID == 2 || ID == MENU_SAY_FULLSCREEN) {
            width = Application.canvasWidth - (ingameBorderSizeLeft + ingameBorderSizeRight);
            height = (Application.canvasHeight - y) - (Font.getFont(0, 1, 8).getHeight() + 8);
        }
        Application.gfx.setClip(x, 0, width, Application.canvasHeight);
        int topBorderX = x;
        while (true) {
            int currentX = topBorderX;
            if (currentX >= x + width) {
                break;
            }
            systemResources[13].paint(Application.gfx, currentX, y, 0);
            topBorderX = currentX + systemResources[13].imageWidth;
        }
        if (drawBottomBorderParts) {
            int bottomBorderX = x;
            while (true) {
                int currentX = bottomBorderX;
                if (currentX >= x + width) {
                    break;
                }
                systemResources[14].paint(Application.gfx, currentX, y + height, 0);
                bottomBorderX = currentX + systemResources[14].imageWidth;
            }
        }
        if (delimiter1 || delimiter2) {
            Application.gfx.setColor(ingameBackgroundColor);
            if (delimiter1) {
                Application.gfx.fillRect(x, y + delimiter1_y, x + width, ingameDelimiterHeight);
            }
            if (delimiter2) {
                Application.gfx.fillRect(x, y + delimiter2_y, x + width, ingameDelimiterHeight);
            }
            switch (ingameDelimiterMode) {
                case 0:
                default:
                    int delimiterX = x;
                    while (delimiterX < x + width) {
                        int currentX = delimiterX;
                        if (delimiter1) {
                            systemResources[21].paint(Application.gfx, currentX, y + delimiter1_y, 0);
                        }
                        if (delimiter2) {
                            systemResources[21].paint(Application.gfx, currentX, y + delimiter2_y, 0);
                        }
                        delimiterX = currentX + systemResources[21].imageWidth;
                    }
                    break;
                case 1:
                    int centeredDelimiterX = (x + (width >> 1)) - systemResources[21].imageRegPointX;
                    int centeredFirstDelimiterY = (y + delimiter1_y) - systemResources[21].imageRegPointY;
                    int centeredSecondDelimiterY = (y + delimiter2_y) - systemResources[21].imageRegPointY;
                    if (delimiter1) {
                        systemResources[21].paintSimple(Application.gfx, centeredDelimiterX, centeredFirstDelimiterY, 17);
                    }
                    if (delimiter2) {
                        systemResources[21].paintSimple(Application.gfx, centeredDelimiterX, centeredSecondDelimiterY, 17);
                    }
                    break;
                case 2:
                    int delimiterCenterX = (x + (width >> 1)) - systemResources[21].imageRegPointX;
                    int firstDelimiterY = (y + delimiter1_y) - systemResources[21].imageRegPointY;
                    int secondDelimiterY = (y + delimiter2_y) - systemResources[21].imageRegPointY;
                    if ((ingameDelimiterNumOf & 1) > 0) {
                        if (delimiter1) {
                            systemResources[21].paintSimple(Application.gfx, delimiterCenterX, firstDelimiterY, 17);
                        }
                        if (delimiter2) {
                            systemResources[21].paintSimple(Application.gfx, delimiterCenterX, secondDelimiterY, 17);
                        }
                        leftDelimiterX = (delimiterCenterX - systemResources[21].imageWidth) - (systemResources[21].imageWidth >> 1);
                        rightDelimiterX = delimiterCenterX + (systemResources[21].imageWidth >> 1);
                    } else {
                        leftDelimiterX = delimiterCenterX - systemResources[21].imageWidth;
                        rightDelimiterX = delimiterCenterX;
                    }
                    while (leftDelimiterX >= x) {
                        int currentRightX = rightDelimiterX;
                        if (delimiter1) {
                            systemResources[21].paintSimple(Application.gfx, leftDelimiterX, firstDelimiterY, 20);
                            systemResources[21].paintSimple(Application.gfx, currentRightX, firstDelimiterY, 20);
                        }
                        if (delimiter2) {
                            systemResources[21].paintSimple(Application.gfx, leftDelimiterX, secondDelimiterY, 20);
                            systemResources[21].paintSimple(Application.gfx, currentRightX, secondDelimiterY, 20);
                        }
                        leftDelimiterX -= systemResources[21].imageWidth;
                        rightDelimiterX = currentRightX + systemResources[21].imageWidth;
                    }
                    break;
            }
        }
        Application.gfx.setClip(0, y, Application.canvasWidth, height);
        int leftBorderY = y;
        while (true) {
            int currentY = leftBorderY;
            if (currentY < y + height) {
                systemResources[15].paint(Application.gfx, x, currentY, 0);
                leftBorderY = currentY + systemResources[15].imageHeight;
            } else {
                int rightBorderY = y;
                while (true) {
                    int currentRightY = rightBorderY;
                    if (currentRightY >= y + height) {
                        Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
                        systemResources[17].paint(Application.gfx, x, y, 0);
                        systemResources[SETUP_INDEX_INGAME_RES_6_IMG].paint(Application.gfx, x + width, y, 0);
                        if (drawBottomBorderParts) {
                            systemResources[19].paint(Application.gfx, x, y + height, 0);
                            systemResources[20].paint(Application.gfx, x + width, y + height, 0);
                            return;
                        }
                        return;
                    }
                    systemResources[SETUP_INDEX_INGAME_RES_4_IMG].paint(Application.gfx, x + width, currentRightY, 0);
                    rightBorderY = currentRightY + systemResources[SETUP_INDEX_INGAME_RES_4_IMG].imageHeight;
                }
            }
        }
    }

    public static void menuSystemInit() {
        SilentHillGame.menuResetIngameValues();
        if (Application.canvasWidth > 1000) {
            engineFont = Font.getFont(0, 1, 8);
        } else {
            engineFont = Font.getFont(0, 0, 8);
        }
        engineFontHeight = engineFont.getHeight() + EXTRA_FONT_HEIGHT;
        engineDefaultMenuWidth = (Application.canvasWidth << 2) / 5;
        ingameFont = Font.getFont(0, 0, 8);
        ingameFontHeight = ingameFont.getHeight();
        if (Application.mainMenuActive) {
            currentFont = engineFont;
        } else {
            currentFont = ingameFont;
        }
        currentFontHeight = currentFont.getHeight() + EXTRA_FONT_HEIGHT;
        menuCreateHeaderFooterImages();
        ingameShowRowByRow_stepSize = (ingameFontHeight << INGAME_SHOW_ROW_BY_ROW_SHIFT) / 10;
    }

    static void menuEngineDrawScrollBar(int markerHeight, int markerOffset) {
        Application.gfx.setColor(0);
        Application.gfx.fillRect(engineScrollBarX, engineScrollBarY, 4, engineScrollBarHeight);
        Application.gfx.setColor(8156258);
        Application.gfx.fillRect(engineScrollBarX + 1, engineScrollBarY + 1, 2, engineScrollBarHeight - 2);
        int markerY = engineScrollBarY + markerOffset;
        Application.gfx.setColor(0);
        Application.gfx.fillRect(engineScrollBarX, markerY, 4, markerHeight);
        Application.gfx.setColor(0);
        Application.gfx.fillRect(engineScrollBarX + 1, markerY + 1, 2, markerHeight - 2);
    }

    static void menuCreateHeaderFooterImages() {
        int height = SilentHillGame.ENGINE_HEADER_IMAGE_HEIGHT;
        if (INK_logo != null) {
            height = 2 + INK_logo.getHeight() + 2;
        }
        int footerHeight = Application.max(height, 3 + engineFontHeight + 3);
        Image headerGradient = Application.createGradientImage(15, height, 0, 0, true);
        engineHeaderImage = Image.createImage(15, height + 3);
        Graphics headerGraphics = engineHeaderImage.getGraphics();
        headerGraphics.drawImage(headerGradient, 0, 0, 20);
        headerGraphics.setColor(0);
        headerGraphics.drawLine(0, height, 15, height);
        headerGraphics.setColor(0);
        headerGraphics.drawLine(0, height + 1, 15, height + 1);
        headerGraphics.setColor(0);
        headerGraphics.drawLine(0, height + 2, 15, height + 2);
        engineHeaderImage = Image.createImage(engineHeaderImage);
        engineHeaderImageHeight = engineHeaderImage.getHeight();
        Image footerGradient = Application.createGradientImage(15, footerHeight, 0, 0, true);
        engineFooterImage = Image.createImage(15, footerHeight + 1);
        Graphics footerGraphics = engineFooterImage.getGraphics();
        footerGraphics.drawImage(footerGradient, 0, 1, 20);
        footerGraphics.setColor(0);
        footerGraphics.drawLine(0, 0, 15, 0);
        engineFooterImage = Image.createImage(engineFooterImage);
        engineFooterImageHeight = engineFooterImage.getHeight();
        engineScrollBarY = engineHeaderImageHeight + 2;
        engineScrollBarHeight = (Application.canvasHeight - (((engineHeaderImageHeight + 2) + 2) + engineFooterImageHeight)) + 2;
        engineScrollBarX = (Application.canvasWidth - 1) - 4;
    }

    public static void menuPaintEngine(MenuModel menu) {
        int screenX;
        int screenY;
        int totalHeight = 0;
        int textY = 4;
        int maxTextWidth = 0;
        int textHeight = 0;
        int choicesHeight = 0;
        if (menu.topText != null) {
            if (menu.updateTopLines) {
                menu.updateTopLines = false;
                menu.topLines = wrapString(menu.topText, menu.maxTextWidth);
            }
            int topLineCount = menu.topLines.length;
            for (int lineIndex = 0; lineIndex < topLineCount; lineIndex++) {
                maxTextWidth = Application.max(maxTextWidth, currentFont.stringWidth(menu.topLines[lineIndex]));
            }
            textHeight = (menu.engineFullScreenScroll && menu.choiceIDs.isEmpty()) ? ((menu.topLines.length + menu.scroll) * (engineFontHeight + 2)) - 2 : (menu.topLines.length * (engineFontHeight + 2)) - 2;
            totalHeight = textHeight + 8;
        }
        Vector choiceLines = null;
        Vector choiceLineColors = null;
        int choicesY = 0 + 4 + textHeight;
        if (!menu.choiceIDs.isEmpty()) {
            int choiceCount = menu.countChoices();
            choiceLines = new Vector(choiceCount);
            choiceLineColors = new Vector(choiceCount);
            int hasChoice = menu.ID == 2 ? 1 : 0;
            if (menu.updateBodyLines) {
                menu.updateBodyLines = false;
                menu.bodyLines = new String[choiceCount][];
            }
            int choiceIndex = (-menu.scroll) * hasChoice;
            while (choiceIndex < choiceCount) {
                menu.choiceIDs.elementAt(choiceIndex);
                String choiceText = (String) menu.choiceTexts.elementAt(choiceIndex);
                Integer lineColor = (choiceIndex != menu.selectedChoiceNr || menu.engineFullScreenScroll) ? new Integer(0) : new Integer(SilentHillGame.engineChoiceSelectionColors[engineChoiceSelectionColorCounter]);
                if (menu.bodyLines[choiceIndex] == null) {
                    menu.bodyLines[choiceIndex] = wrapString(choiceText, menu.maxTextWidth);
                }
                int lineCount = menu.bodyLines[choiceIndex].length;
                for (int lineIndex = menu.engineFullScreenScroll ? -menu.scroll : 0; lineIndex < lineCount; lineIndex++) {
                    choiceLines.addElement(menu.bodyLines[choiceIndex][lineIndex]);
                    choiceLineColors.addElement(lineColor);
                    maxTextWidth = Application.max(maxTextWidth, currentFont.stringWidth(menu.bodyLines[choiceIndex][lineIndex]));
                }
                choiceIndex++;
            }
            choicesHeight = (choiceLines.size() * (engineFontHeight + 2)) - 2;
            totalHeight += choicesHeight + 8;
        }
        int totalWidth = maxTextWidth + 8;
        if (textHeight > 0 && choicesHeight > 0) {
            choicesY += 10;
            totalHeight += 2;
        }
        if (menu.engineFullScreenScroll) {
            screenX = menu.x;
            screenY = menu.y;
        } else {
            int centeredX = menu.x - (totalWidth >> 1);
            int centeredY = menu.y - (totalHeight >> 1);
            int nonNegativeX = Application.max(centeredX, 0);
            int nonNegativeY = Application.max(centeredY, 0);
            screenX = Application.min(nonNegativeX, Application.canvasWidth - totalWidth);
            screenY = Application.min(nonNegativeY, Application.canvasHeight - totalHeight);
        }
        if (menu.topText != null) {
            int topLineCount = menu.topLines.length;
            if (menu.engineFullScreenScroll) {
                int hasChoice = menu.choiceIDs.isEmpty() ? 1 : 0;
                Application.gfx.setColor(0);
                for (int lineIndex = (-menu.scroll) * hasChoice; lineIndex < topLineCount; lineIndex++) {
                    Application.gfx.drawString(menu.topLines[lineIndex], screenX + 4, screenY + textY, 0);
                    textY += engineFontHeight + 2;
                }
                if (menu.engineNumOfLinesShownMax == -1) {
                    menu.engineNumOfLinesShownMax = (((((Application.canvasHeight - screenY) - 4) - 4) - engineFooterImageHeight) + 2) / (engineFontHeight + 2);
                }
                if (menu.choiceIDs.isEmpty() && topLineCount > menu.engineNumOfLinesShownMax) {
                    if (menu.engineScrollBarMarkerHeight == -1) {
                        menu.engineScrollBarMarkerHeight = (engineScrollBarHeight * menu.engineNumOfLinesShownMax) / topLineCount;
                    }
                    menuEngineDrawScrollBar(menu.engineScrollBarMarkerHeight, menu.scroll == 0 ? 0 : ((engineScrollBarHeight - menu.engineScrollBarMarkerHeight) * (-menu.scroll)) / (topLineCount - menu.engineNumOfLinesShownMax));
                }
            } else {
                Application.gfx.setColor(0);
                for (int lineIndex = 0; lineIndex < topLineCount; lineIndex++) {
                    Application.gfx.drawString(menu.topLines[lineIndex], screenX + 4, screenY + textY, 0);
                    textY += engineFontHeight + 2;
                }
            }
        }
        if (!menu.choiceIDs.isEmpty()) {
            int choiceLineCount = choiceLines.size();
            for (int lineIndex = 0; lineIndex < choiceLineCount; lineIndex++) {
                Application.gfx.setColor(Application.toInt(choiceLineColors.elementAt(lineIndex)));
                String line = (String) choiceLines.elementAt(lineIndex);
                if (menu.engineFullScreenScroll) {
                    Application.gfx.drawString(line, screenX + 4, screenY + choicesY, 0);
                } else {
                    Application.gfx.drawString(line, screenX + 4, screenY + choicesY, 0);
                }
                choicesY += engineFontHeight + 2;
            }
        }
        if (menu.engineFullScreenScroll) {
            if (menu.choiceIDs.isEmpty()) {
                if (textHeight < (((Application.canvasHeight - screenY) - 4) - 4) - engineFooterImageHeight) {
                    menu.textScrolling = false;
                }
            } else if (choicesHeight < (((Application.canvasHeight - (screenY + textHeight)) - 4) - 4) - engineFooterImageHeight) {
                menu.textScrolling = false;
            }
        }
    }

    static void menuPaintCurrentEngine() {
        int canvasWidth = Application.canvasWidth;
        int canvasHeight = Application.canvasHeight;
        Application.gfx.setColor(0);
        Application.gfx.fillRect(0, 0, canvasWidth, canvasHeight);
        if (engineChoiceSelectionAnimTimer < 2) {
            engineChoiceSelectionAnimTimer++;
        } else {
            if (engineChoiceSelectionColorCounter < SilentHillGame.engineChoiceSelectionColors.length - 1) {
                engineChoiceSelectionColorCounter++;
            } else {
                engineChoiceSelectionColorCounter = 0;
            }
            engineChoiceSelectionAnimTimer = 0;
        }
        synchronized (MenuModel.stack) {
            if (MenuModel.stack.isEmpty()) {
                Application.gfx.setColor(0);
                Application.gfx.fillRect(0, 0, canvasWidth, canvasHeight);
                return;
            }
            Application.gfx.setClip(0, engineHeaderImageHeight, canvasWidth, canvasHeight - (engineHeaderImageHeight + engineFooterImageHeight));
            SilentHillGame.menuPaintEngine((MenuModel) MenuModel.stack.elementAt(MenuModel.stack.size() - 1));
            Application.gfx.setClip(0, 0, canvasWidth, canvasHeight);
            for (int tileX = 0; tileX < canvasWidth; tileX += 15) {
                Application.gfx.drawImage(engineHeaderImage, tileX, 0, SETUP_INDEX_INGAME_RES_4_IMG | 4);
                Application.gfx.drawImage(engineFooterImage, tileX, canvasHeight, 32 | 4);
            }
            if (INK_logo != null) {
                Application.gfx.drawImage(INK_logo, 2, 2, SETUP_INDEX_INGAME_RES_4_IMG | 4);
            }
            synchronized (MenuModel.stack) {
                if (!MenuModel.stack.isEmpty()) {
                    Application.gfx.setColor(16777215);
                    MenuModel menu = (MenuModel) MenuModel.stack.elementAt(MenuModel.stack.size() - 1);
                    Application.gfx.setFont(Font.getFont(0, 1, 8));
                    if (menu.engineSoftkeyOptionLeft != null) {
                        Application.gfx.drawString(menu.engineSoftkeyOptionLeft, 2, canvasHeight - 2, 32 | 4);
                    }
                    if (menu.engineSoftkeyOptionRight != null) {
                        Application.gfx.drawString(menu.engineSoftkeyOptionRight, canvasWidth - 2, canvasHeight - 2, 32 | 8);
                    }
                }
            }
        }
    }

    public static void inventoryMenuPaint(MenuModel menu) {
        if (menu.updateMenu) {
            menuUpdateIngame(menu);
            menu.updateMenu = false;
        }
        int choiceY = menu.choicesY;
        int textY = menu.textY;
        int visibleChoiceCount = (menu.choicesHeight + ingameLineSpacing) / (ingameFontHeight + ingameLineSpacing);
        int softKeyBarHeight = Font.getFont(0, 1, 8).getHeight() + 8;
        if (menu.screenY + menu.totalHeight > Application.canvasHeight - softKeyBarHeight) {
            visibleChoiceCount = (menu.choicesHeight - ((menu.screenY + menu.totalHeight) - (Application.canvasHeight - softKeyBarHeight))) / (ingameFontHeight + ingameLineSpacing);
            menu.totalHeight -= menu.choicesHeight;
            menu.choicesHeight = (visibleChoiceCount * (ingameFontHeight + ingameLineSpacing)) - ingameLineSpacing;
            menu.totalHeight += menu.choicesHeight;
        }
        int imageSectionX = menu.screenX;
        int imageSectionY = menu.screenY + menu.imageSectionY;
        Application.gfx.setColor(ingameBackgroundColor);
        Application.gfx.fillRect(imageSectionX, imageSectionY, menu.totalWidth, 38);
        int imageCenterX = imageSectionX + (menu.totalWidth >> 1);
        int imageCenterY = imageSectionY + 3 + SETUP_INDEX_INGAME_RES_4_IMG;
        try {
            if (menu.curInvItemResource == null) {
                menu.curInvItemResource = InkScript.getInventoryImage((String) curInvIds[curInvCounter]);
            }
            if (menu.curInvItemResource != null) {
                menu.curInvItemResource.paintSimple(Application.gfx, imageCenterX, imageCenterY, 3);
            }
        } catch (Exception e) {
        }
        if (curInvNumOfItems > 1) {
            int arrowYCorrection = (invArrowHeight & 1) != 0 ? 0 : 1;
            int leftArrowX = menu.screenX + ((menu.totalWidth - 32) >> 1);
            systemResources[11].paintSimple(Application.gfx, leftArrowX - 3, ((imageSectionY + 3) + ((32 - invArrowHeight) >> 1)) - arrowYCorrection, 24);
            systemResources[12].paintSimple(Application.gfx, leftArrowX + 32 + 3, ((imageSectionY + 3) + ((32 - invArrowHeight) >> 1)) - arrowYCorrection, 20);
        }
        Application.gfx.setColor(ingameHeaderColor);
        Application.gfx.fillRect(menu.screenX, menu.screenY, menu.totalWidth, menu.textHeight + (ingameMargin << 1));
        Application.gfx.setColor(ingameTextColor);
        for (int i10 = 0; i10 < menu.topLines.length; i10++) {
            Application.gfx.drawString(menu.topLines[i10], menu.screenX + menu.textX, menu.screenY + textY, 0);
            textY += ingameFontHeight + ingameLineSpacing;
        }
        Application.gfx.setColor(ingameBackgroundColor);
        Application.gfx.fillRect(menu.screenX, (menu.screenY + choiceY) - ingameMargin, menu.totalWidth, menu.choicesHeight + (ingameMargin << 1));
        int size = menu.choiceLines.size();
        if (menu.selectedChoiceNr < menu.lowerPaintChoice) {
            menu.lowerPaintChoice = menu.selectedChoiceNr;
        }
        if (menu.selectedChoiceNr >= menu.lowerPaintChoice + visibleChoiceCount) {
            menu.lowerPaintChoice = (menu.selectedChoiceNr - visibleChoiceCount) + 1;
        }
        int upperPaintChoice = menu.lowerPaintChoice + visibleChoiceCount;
        for (int choiceIndex = menu.lowerPaintChoice; choiceIndex < upperPaintChoice; choiceIndex++) {
            int backgroundColor = Application.toInt(menu.choiceLineBackgroundColors.elementAt(choiceIndex));
            if (backgroundColor != ingameBackgroundColor) {
                Application.gfx.setColor(backgroundColor);
                Application.gfx.fillRect(menu.screenX, menu.screenY + choiceY, menu.totalWidth, ingameFontHeight);
            }
            Application.gfx.setColor(ingameTextColor);
            Application.gfx.drawString((String) menu.choiceLines.elementAt(choiceIndex), menu.screenX + menu.textX, menu.screenY + choiceY + IngameMenuOffset, 0);
            choiceY += ingameFontHeight + ingameLineSpacing;
        }
        if (ingameScrollArrows && Application.tickCounter > 10) {
            if (menu.lowerPaintChoice > 0) {
                systemResources[9].paintSimple(Application.gfx, menu.screenX + (menu.totalWidth >> 1), menu.screenY + menu.choicesY, MENU_ENGINE_FULLSCREEN2);
            }
            if (upperPaintChoice < size) {
                systemResources[10].paintSimple(Application.gfx, menu.screenX + (menu.totalWidth >> 1), menu.screenY + menu.totalHeight, MENU_ENGINE_FULLSCREEN2);
            }
        }
        menuPaintIngameImageBorders(menu.ID, menu.screenX, menu.screenY, menu.totalWidth, menu.totalHeight, menu.drawDelimiter_1, menu.delimiterY_1, menu.drawDelimiter_2, menu.delimiterY_2, menu.ingameDelimiterNumOf);
    }

    public static void menuPaintCurrentIngame() {
        int size = MenuModel.stack.size();
        if (size > 0) {
            SilentHillGame.menuPaintIngame((MenuModel) MenuModel.stack.elementAt(size - 1));
        }
    }

    public static void menuUpdateIngame(MenuModel menu) {
        menu.delimiterHeight = ingameUseImageBorders ? ingameDelimiterHeight : 2;
        menu.drawDelimiter_1 = false;
        menu.delimiterY_1 = 0;
        menu.drawDelimiter_2 = false;
        menu.delimiterY_2 = 0;
        menu.totalWidth = 0;
        menu.totalHeight = 0;
        if (menu.ID == 7) {
            menu.textX = ingameMargin + 32 + ingameMargin;
        } else {
            menu.textX = ingameMargin;
        }
        menu.textY = ingameMargin;
        menu.textWidth = 0;
        menu.textHeight = 0;
        menu.choicesY = 0;
        menu.choicesHeight = 0;
        if (menu.topText != null) {
            if (menu.updateTopLines) {
                menu.updateTopLines = false;
                menu.topLines = wrapString(menu.topText, menu.maxTextWidth);
            }
            int length = menu.topLines.length;
            for (int i = 0; i < length; i++) {
                menu.textWidth = Application.max(menu.textWidth, currentFont.stringWidth(menu.topLines[i]));
            }
            if ((menu.ID == 6 || menu.ID == MENU_SAY_FULLSCREEN) && menu.choiceIDs.isEmpty()) {
                menu.textHeight = ((menu.topLines.length + menu.scroll) * (ingameFontHeight + ingameLineSpacing)) - ingameLineSpacing;
            } else {
                menu.textHeight = (menu.topLines.length * (ingameFontHeight + ingameLineSpacing)) - ingameLineSpacing;
            }
            menu.totalHeight = menu.textHeight + (ingameMargin << 1);
        }
        menu.choiceLines = null;
        menu.choiceLineBackgroundColors = null;
        menu.choicesY += menu.textY + menu.textHeight;
        menu.imageSectionY = 0;
        if (menu.ID == 3 || menu.ID == 5) {
            menu.drawDelimiter_1 = true;
            menu.delimiterY_1 = menu.choicesY + ingameMargin;
            menu.choicesY += 35 + menu.delimiterHeight;
            menu.totalHeight += 35 + menu.delimiterHeight;
            menu.imageSectionY = menu.textY + menu.textHeight + ingameMargin + menu.delimiterHeight;
        }
        if (!menu.choiceIDs.isEmpty()) {
            int choiceCount = menu.countChoices();
            menu.choiceLines = new Vector(choiceCount);
            menu.choiceLineBackgroundColors = new Vector(choiceCount);
            int hasChoice = menu.ID == 2 ? 1 : 0;
            if (menu.updateBodyLines) {
                menu.updateBodyLines = false;
                menu.bodyLines = new String[choiceCount][];
            }
            int choiceIndex = (-menu.scroll) * hasChoice;
            while (choiceIndex < choiceCount) {
                menu.choiceIDs.elementAt(choiceIndex);
                String choiceText = (String) menu.choiceTexts.elementAt(choiceIndex);
                Integer choiceBackground = (choiceIndex != menu.selectedChoiceNr || menu.ID == 6 || menu.ID == MENU_SAY_FULLSCREEN) ? new Integer(ingameBackgroundColor) : new Integer(ingameBackgroundColorSelected);
                if (menu.bodyLines[choiceIndex] == null) {
                    menu.bodyLines[choiceIndex] = wrapString(choiceText, menu.maxTextWidth);
                }
                int bodyLineCount = menu.bodyLines[choiceIndex].length;
                if (menu.ID == 6 || menu.ID == MENU_SAY_FULLSCREEN) {
                    for (int i4 = -menu.scroll; i4 < bodyLineCount; i4++) {
                        menu.choiceLines.addElement(menu.bodyLines[choiceIndex][i4]);
                        menu.choiceLineBackgroundColors.addElement(choiceBackground);
                        menu.textWidth = Application.max(menu.textWidth, currentFont.stringWidth(menu.bodyLines[choiceIndex][i4]));
                    }
                } else {
                    for (int i5 = 0; i5 < bodyLineCount; i5++) {
                        menu.choiceLines.addElement(menu.bodyLines[choiceIndex][i5]);
                        menu.choiceLineBackgroundColors.addElement(choiceBackground);
                        menu.textWidth = Application.max(menu.textWidth, currentFont.stringWidth(menu.bodyLines[choiceIndex][i5]));
                    }
                }
                choiceIndex++;
            }
            menu.choicesHeight = (menu.choiceLines.size() * (ingameFontHeight + ingameLineSpacing)) - ingameLineSpacing;
            menu.totalHeight += menu.choicesHeight + (ingameMargin << 1);
        }
        if (menu.ID == 2 && menu.checkChoiceMenuHeight) {
            int longestChoiceLineCount = 0;
            if (menu.bodyLines != null) {
                for (int i6 = 0; i6 < menu.bodyLines.length; i6++) {
                    if (menu.bodyLines[i6] != null) {
                        longestChoiceLineCount = Application.max(longestChoiceLineCount, menu.bodyLines[i6].length);
                    }
                }
            }
            menu.checkChoiceMenuHeight = false;
            if (longestChoiceLineCount > 4) {
                menu.y -= (longestChoiceLineCount - 4) * (currentFontHeight + ingameLineSpacing);
                menuUpdateIngame(menu);
                return;
            } else {
                int size = menu.choiceLines.size();
                if (size >= 4) {
                    menu.y -= ((size - 4) + 1) * (currentFontHeight + ingameLineSpacing);
                    menuUpdateIngame(menu);
                    return;
                }
            }
        }
        if (menu.textHeight > 0 && menu.choicesHeight > 0) {
            menu.drawDelimiter_2 = true;
            menu.delimiterY_2 = menu.choicesY + ingameMargin;
            menu.choicesY += (ingameMargin << 1) + menu.delimiterHeight;
            menu.totalHeight += menu.delimiterHeight;
            if (menu.ID == 3 || menu.ID == 5) {
                menu.delimiterY_2 += 3;
                menu.choicesY += 3;
                menu.totalHeight += 3;
            }
        }
        if (menu.ID == 3 || menu.ID == 5) {
            menu.textWidth = 64;
            if ((menu.textWidth & 1) != 0) {
                menu.textWidth++;
            }
        } else if (menu.ID == 7) {
            menu.totalHeight = Application.max(menu.totalHeight, 32 + (ingameMargin << 1));
            menu.textWidth += 32 + ingameMargin;
        }
        menu.totalWidth = Application.max(menu.textWidth, INGAME_MENU_TEXT_WIDTH_MIN) + (ingameMargin << 1);
        if (ingameUseImageBorders && ingameDelimiterMode == 2 && menu.ingameDelimiterNumOf == -1) {
            menu.ingameDelimiterNumOf = Application.max(1, menu.totalWidth / systemResources[21].imageWidth);
        }
        if (ingameUseDynamicSayChoice && ((menu.ID == 6 || menu.ID == 2) && menu.totalHeight + (ingameMargin << 1) < Application.canvasHeight - menu.y)) {
            menu.y += (Application.canvasHeight - menu.y) - (menu.totalHeight + (ingameMargin << 1));
        }
        if (menu.ID == 3) {
            menu.totalWidth = Application.canvasWidth >> 1;
        } else {
            menu.totalWidth = Application.max(menu.textWidth, INGAME_MENU_TEXT_WIDTH_MIN) + (ingameMargin << 1);
        }
        menu.screenX = menu.x - (menu.totalWidth >> 1);
        menu.screenY = menu.y - (menu.totalHeight >> 1);
        menu.screenX = ingameUseImageBorders ? Application.max(menu.screenX, ingameBorderSizeLeft) : Application.max(menu.screenX, ingameBorderSize);
        menu.screenY = ingameUseImageBorders ? Application.max(menu.screenY, ingameBorderSizeTop) : Application.max(menu.screenY, ingameBorderSize);
        menu.screenX = ingameUseImageBorders ? Application.min(menu.screenX, (Application.canvasWidth - menu.totalWidth) - ingameBorderSizeRight) : Application.min(menu.screenX, (Application.canvasWidth - menu.totalWidth) - ingameBorderSize);
        menu.screenY = ingameUseImageBorders ? Application.min(menu.screenY, (Application.canvasHeight - menu.totalHeight) - ingameBorderSizeBottom) : Application.min(menu.screenY, (Application.canvasHeight - menu.totalHeight) - ingameBorderSize);
        if (menu.ID == 6 || menu.ID == 2 || menu.ID == MENU_SAY_FULLSCREEN) {
            menu.screenX = menu.x;
            if (menu.ID != MENU_SAY_FULLSCREEN) {
                if (menu.ID == 2) {
                    menu.screenY = (menu.y - Font.getFont(0, 1, 8).getHeight()) + 4;
                } else {
                    menu.screenY = (menu.y - Font.getFont(0, 1, 8).getHeight()) + 8;
                }
                if (menu.topText != null && !menu.choiceIDs.isEmpty()) {
                    menu.screenY -= ingameFontHeight + (ingameLineSpacing << 1);
                }
            } else {
                menu.screenY = menu.y;
            }
        }
        if (menu.ID == 17 || menu.ID == 30) {
            menu.screenY -= Font.getFont(0, 1, 8).getHeight() + 4;
        }
        if (menu.ID == 3) {
            int minimumScreenY = 10 + systemResources[23].imageHeight + ingameBorderSizeTop;
            menu.screenY -= Font.getFont(0, 1, 8).getHeight() + 4;
            if (menu.screenY < minimumScreenY) {
                menu.screenY = minimumScreenY;
            }
        }
        if (menu.choiceIDs.isEmpty()) {
            return;
        }
        int choiceY = menu.choicesY;
        int renderedChoiceCount = menu.choiceLines.size();
        for (int i9 = 0; i9 < renderedChoiceCount; i9++) {
            int backgroundColor = Application.toInt(menu.choiceLineBackgroundColors.elementAt(i9));
            if (backgroundColor != ingameBackgroundColor) {
                Application.gfx.setColor(backgroundColor);
                if (menu.ID == 2 && menu.screenY + choiceY + ingameFontHeight > Application.canvasHeight && (-menu.scroll) < menu.countChoices()) {
                    menu.scroll--;
                    if (menu.choicesY < choiceY) {
                        menuUpdateIngame(menu);
                        return;
                    }
                    return;
                }
            }
            choiceY += ingameFontHeight + ingameLineSpacing;
        }
    }

    public static void menuPaintIngame(MenuModel menu) {
        if (menu.updateMenu) {
            menuUpdateIngame(menu);
            menu.updateMenu = false;
        }
        int choiceY = menu.choicesY;
        int textY = menu.textY;
        int softKeyBarHeight = Font.getFont(0, 1, 8).getHeight() + 8;
        int availableHeight = (Application.canvasHeight - menu.screenY) - softKeyBarHeight;
        if (menu.ID != MENU_TEXT_FADE_CENTER) {
            Application.gfx.setColor(ingameBorderColor);
            if (menu.ID == 6 || menu.ID == 2 || menu.ID == MENU_SAY_FULLSCREEN) {
                Application.gfx.setClip(menu.screenX - (ingameUseImageBorders ? ingameBorderSizeLeft : ingameBorderSize), menu.screenY - (ingameUseImageBorders ? ingameBorderSizeTop : ingameBorderSize), Application.canvasWidth, availableHeight + (ingameUseImageBorders ? ingameBorderSizeTop : ingameBorderSize));
                if (!ingameUseImageBorders) {
                    Application.gfx.fillRect(menu.screenX - ingameBorderSize, menu.screenY - ingameBorderSize, Application.canvasWidth, availableHeight);
                    Application.gfx.setColor(ingameBackgroundColor);
                    if (ingameBorderSize > 3) {
                        Application.gfx.drawRect((menu.screenX - ingameBorderSize) + 2, (menu.screenY - ingameBorderSize) + 2, (Application.canvasWidth - 4) - 1, availableHeight);
                    }
                }
            } else if (!ingameUseImageBorders) {
                Application.gfx.fillRect(menu.screenX - ingameBorderSize, menu.screenY - ingameBorderSize, menu.totalWidth + (ingameBorderSize << 1), menu.totalHeight + (ingameBorderSize << 1));
                Application.gfx.setColor(ingameBackgroundColor);
                if (ingameBorderSize > 3) {
                    Application.gfx.drawRect((menu.screenX - ingameBorderSize) + 2, (menu.screenY - ingameBorderSize) + 2, (menu.totalWidth + ((ingameBorderSize - 2) << 1)) - 1, (menu.totalHeight + ((ingameBorderSize - 2) << 1)) - 1);
                }
            }
        }
        if (menu.ID == 3 || menu.ID == 5) {
            int imageSectionX = menu.screenX;
            int imageSectionY = menu.screenY + menu.imageSectionY;
            Application.gfx.setColor(ingameBackgroundColor);
            Application.gfx.fillRect(imageSectionX, imageSectionY, menu.totalWidth, 38);
            int imageCenterX = menu.screenX + (menu.totalWidth >> 1);
            int imageCenterY = imageSectionY + 3 + SETUP_INDEX_INGAME_RES_4_IMG;
            try {
                if (menu.curInvItemResource == null) {
                    menu.curInvItemResource = InkScript.getInventoryImage((String) curInvIds[curInvCounter]);
                }
                if (menu.curInvItemResource != null) {
                    menu.curInvItemResource.paintSimple(Application.gfx, imageCenterX, imageCenterY, 3);
                }
            } catch (Exception e) {
                Application.gfx.setColor(0);
                Application.gfx.fillRect(imageCenterX - 5, imageCenterY - 5, 10, 10);
                Application.gfx.setColor(16777215);
                Application.gfx.drawLine(imageCenterX - 5, imageCenterY - 5, imageCenterX + 5, imageCenterY + 5);
                Application.gfx.drawLine(imageCenterX - 5, imageCenterY + 5, imageCenterX + 5, imageCenterY - 5);
            }
            if (curInvNumOfItems > 1) {
                int arrowYCorrection = (invArrowHeight & 1) != 0 ? 0 : 1;
                int leftArrowX = menu.screenX + ((menu.totalWidth - 32) >> 1);
                systemResources[11].paintSimple(Application.gfx, leftArrowX - 3, ((imageSectionY + 3) + ((32 - invArrowHeight) >> 1)) - arrowYCorrection, 24);
                systemResources[12].paintSimple(Application.gfx, leftArrowX + 32 + 3, ((imageSectionY + 3) + ((32 - invArrowHeight) >> 1)) - arrowYCorrection, 20);
            }
        }
        boolean moreTextBelow = false;
        if (menu.topText != null) {
            Application.gfx.setColor(menu.choiceIDs.isEmpty() ? ingameBackgroundColor : ingameHeaderColor);
            if (menu.ID == 6 || menu.ID == 2 || menu.ID == MENU_SAY_FULLSCREEN) {
                int noChoices = menu.choiceIDs.isEmpty() ? 1 : 0;
                int textAreaHeight = noChoices > 0 ? availableHeight : menu.textHeight;
                Application.gfx.fillRect(menu.screenX, menu.screenY, ingameUseImageBorders ? Application.canvasWidth - (ingameBorderSizeLeft + ingameBorderSizeRight) : Application.canvasWidth - (ingameBorderSize << 1), ingameUseImageBorders ? textAreaHeight + ingameBorderSizeTop + ingameBorderSizeBottom : textAreaHeight + (ingameMargin << 1));
                if (Application.getString(TextId.JAVA_APP_INK_BEN).equals(menu.topText)) {
                    Application.gfx.setColor(TEXT_COLOR_BEN);
                } else if (Application.getString(TextId.JAVA_APP_INK_MOON).equals(menu.topText)) {
                    Application.gfx.setColor(TEXT_COLOR_MOON);
                } else if (Application.getString(TextId.JAVA_APP_INK_KAREN).equals(menu.topText)) {
                    Application.gfx.setColor(TEXT_COLOR_KAREN);
                } else {
                    Application.gfx.setColor(ingameTextColor);
                }
                if (menu.ingameShowRowByRow_use && menu.choiceIDs.isEmpty()) {
                    if (menu.ingameShowRowByRow_curY == -1) {
                        menu.ingameShowRowByRow_curY = (((menu.screenY + textY) + ingameFontHeight) + ingameLineSpacing) << INGAME_SHOW_ROW_BY_ROW_SHIFT;
                    }
                    Application.gfx.setClip(0, 0, Application.canvasWidth, menu.ingameShowRowByRow_curY >> INGAME_SHOW_ROW_BY_ROW_SHIFT);
                }
                for (int lineIndex = (-menu.scroll) * noChoices; lineIndex < menu.topLines.length; lineIndex++) {
                    Application.gfx.drawString(menu.topLines[lineIndex], menu.screenX + menu.textX, menu.screenY + textY, 0);
                    textY += ingameFontHeight + ingameLineSpacing;
                    if (menu.screenY + textY + ingameFontHeight > (Application.canvasHeight - softKeyBarHeight) - ingameBorderSizeBottom) {
                        if (lineIndex + 1 >= menu.topLines.length) {
                            break;
                        }
                        moreTextBelow = true;
                        break;
                    }
                }
                if (menu.ingameShowRowByRow_use) {
                    menu.ingameShowRowByRow_curY += ingameShowRowByRow_stepSize;
                    if ((menu.ingameShowRowByRow_curY >> INGAME_SHOW_ROW_BY_ROW_SHIFT) >= Application.canvasHeight) {
                        menu.ingameShowRowByRow_use = false;
                    }
                    Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
                }
            } else {
                if (menu.ID != MENU_TEXT_FADE_CENTER) {
                    Application.gfx.fillRect(menu.screenX, menu.screenY, menu.totalWidth, menu.ID == 7 ? menu.totalHeight : menu.textHeight + (ingameMargin << 1));
                    Application.gfx.setColor(ingameTextColor);
                } else {
                    Application.gfx.setColor(textFadePresentColor);
                }
                for (int i13 = 0; i13 < menu.topLines.length; i13++) {
                    Application.gfx.drawString(menu.topLines[i13], menu.screenX + menu.textX, menu.screenY + textY, 0);
                    textY += ingameFontHeight + ingameLineSpacing;
                }
            }
        }
        boolean moreChoicesBelow = false;
        if (!menu.choiceIDs.isEmpty()) {
            Application.gfx.setColor(ingameBackgroundColor);
            if (menu.ID == 6 || menu.ID == 2 || menu.ID == MENU_SAY_FULLSCREEN) {
                Application.gfx.fillRect(menu.screenX, (menu.screenY + choiceY) - ingameMargin, ingameUseImageBorders ? Application.canvasWidth - (ingameBorderSizeLeft + ingameBorderSizeRight) : Application.canvasWidth - (ingameBorderSize << 1), availableHeight + (ingameBorderSize << 1));
            } else {
                Application.gfx.fillRect(menu.screenX, (menu.screenY + choiceY) - ingameMargin, menu.totalWidth, menu.choicesHeight + (ingameMargin << 1));
            }
            if (menu.ingameShowRowByRow_use && menu.choiceIDs.isEmpty()) {
                if (menu.ingameShowRowByRow_curY == -1) {
                    menu.ingameShowRowByRow_curY = (((menu.screenY + choiceY) + ingameFontHeight) + ingameLineSpacing) << INGAME_SHOW_ROW_BY_ROW_SHIFT;
                }
                Application.gfx.setClip(0, 0, Application.canvasWidth, menu.ingameShowRowByRow_curY >> INGAME_SHOW_ROW_BY_ROW_SHIFT);
            }
            int size = menu.choiceLines.size();
            for (int i14 = 0; i14 < size; i14++) {
                int backgroundColor = Application.toInt(menu.choiceLineBackgroundColors.elementAt(i14));
                if (backgroundColor != ingameBackgroundColor) {
                    Application.gfx.setColor(backgroundColor);
                    if (menu.ID == 2) {
                        Application.gfx.fillRect(menu.screenX, (menu.screenY + choiceY) - ingameLineSpacing, ingameUseImageBorders ? Application.canvasWidth - (ingameBorderSizeLeft + ingameBorderSizeRight) : Application.canvasWidth - (ingameBorderSize << 1), ingameFontHeight + (ingameLineSpacing << 1));
                    } else {
                        Application.gfx.fillRect(menu.screenX, (menu.screenY + choiceY) - ingameLineSpacing, menu.totalWidth, ingameFontHeight + (ingameLineSpacing << 1));
                    }
                }
                if (Application.getString(TextId.JAVA_APP_INK_BEN).equals(menu.topText)) {
                    Application.gfx.setColor(TEXT_COLOR_BEN);
                } else if (Application.getString(TextId.JAVA_APP_INK_MOON).equals(menu.topText)) {
                    Application.gfx.setColor(TEXT_COLOR_MOON);
                } else if (Application.getString(TextId.JAVA_APP_INK_KAREN).equals(menu.topText)) {
                    Application.gfx.setColor(TEXT_COLOR_KAREN);
                } else {
                    Application.gfx.setColor(ingameTextColor);
                }
                String choiceLine = (String) menu.choiceLines.elementAt(i14);
                if (menu.ID == 2) {
                    int length = 0 + menu.bodyLines[0].length;
                    int basementChoiceLineIndex = length + menu.bodyLines[length].length;
                    if ((Application.inkServerGetVariable("var_floor").equals("floor2") && i14 == 0) || ((Application.inkServerGetVariable("var_floor").equals("floor1") && i14 == length) || (Application.inkServerGetVariable("var_floor").equals("floor_base") && i14 == basementChoiceLineIndex))) {
                        Application.gfx.drawImage(mapMenuCursor, menu.screenX + menu.textX + Application.gfx.getFont().stringWidth(choiceLine) + 10, menu.screenY + choiceY + (ingameFontHeight >> 1), 6);
                    }
                }
                Application.gfx.drawString(choiceLine, menu.screenX + menu.textX, menu.screenY + choiceY + IngameMenuOffset, 0);
                choiceY += ingameFontHeight + ingameLineSpacing;
                if (menu.screenY + choiceY + ingameFontHeight > (Application.canvasHeight - softKeyBarHeight) - ingameBorderSizeBottom) {
                    if (i14 + 1 >= size) {
                        break;
                    }
                    moreChoicesBelow = true;
                    break;
                }
            }
            if (menu.ingameShowRowByRow_use) {
                menu.ingameShowRowByRow_curY += ingameShowRowByRow_stepSize;
                if ((menu.ingameShowRowByRow_curY >> INGAME_SHOW_ROW_BY_ROW_SHIFT) >= Application.canvasHeight) {
                    menu.ingameShowRowByRow_use = false;
                }
                Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
            }
        }
        if (!moreTextBelow && !moreChoicesBelow) {
            menu.textScrolling = false;
        }
        if (ingameScrollArrows && Application.tickCounter > 10 && (menu.ID == 6 || menu.ID == 2 || menu.ID == MENU_SAY_FULLSCREEN)) {
            int contentWidth = Application.canvasWidth - (ingameBorderSizeLeft + ingameBorderSizeRight);
            int contentHeight = (Application.canvasHeight - menu.screenY) - softKeyBarHeight;
            if (menu.scroll < 0) {
                systemResources[9].paintSimple(Application.gfx, ((menu.screenX + contentWidth) - ingameMargin) - ingameScrollArrowsWidth, (menu.textHeight <= 0 || menu.choicesHeight <= 0) ? menu.screenY + ingameMargin : menu.screenY + ingameMargin + menu.textHeight + ingameMargin + menu.delimiterHeight + ingameMargin, 20);
            }
            if (menu.totalHeight > (Application.canvasHeight - menu.y) - softKeyBarHeight && ((menu.ID == 6 || menu.ID == MENU_SAY_FULLSCREEN) && menu.textScrolling)) {
                systemResources[10].paintSimple(Application.gfx, ((menu.screenX + contentWidth) - ingameMargin) - ingameScrollArrowsWidth, (menu.screenY + contentHeight) - ingameMargin, 36);
            }
        }
        if (ingameUseImageBorders && menu.ID != MENU_TEXT_FADE_CENTER) {
            menuPaintIngameImageBorders(menu.ID, menu.screenX, menu.screenY, menu.totalWidth, menu.totalHeight, menu.drawDelimiter_1, menu.delimiterY_1, menu.drawDelimiter_2, menu.delimiterY_2, menu.ingameDelimiterNumOf);
        }
        if (menu.ID != 7 || menu.curInvItemResource == null) {
            return;
        }
        menu.curInvItemResource.paintSimple(Application.gfx, (menu.screenX + ((menu.textX - ingameMargin) - SETUP_INDEX_INGAME_RES_4_IMG)) - 1, menu.screenY + (menu.totalHeight >> 1), 3);
    }

    public static void menuResetIngameValues() {
        invFirstItemId = null;
        ingameBorderColor = 0;
        ingameHeaderColor = 12632256;
        ingameBackgroundColor = 15393458;
        ingameBackgroundColorSelected = 8156258;
        ingameTextColor = 0;
        ingameMargin = 4;
        ingameLineSpacing = 2;
        ingameBorderSize = 4;
        ingameScrollArrows = false;
        ingameScrollArrowsWidth = -1;
        ingameUseDynamicSayChoice = false;
        ingameShowRowByRow_available = false;
        ingameUseImageBorders = false;
        ingameBorderSizeTop = 0;
        ingameBorderSizeBottom = 0;
        ingameBorderSizeLeft = 0;
        ingameBorderSizeRight = 0;
        ingameDelimiterMode = 0;
        actionKeyUnsetAllKeys();
        specialCursorIdle = null;
        hoverCursor = null;
        cursorAnimActive = false;
    }

    static void roomInit(String roomID, boolean saveCurToHistory) {
        bossDead = false;
        Application.last_room_id = Application.current_room_id;
        Application.current_room_id = roomID;
        Application.scrollRoomHor = true;
        Application.scrollRoomVer = true;
        Application.hideCursor = false;
        Application.selectedRoom = roomID;
        if (roomID == null || roomID.length() <= 0) {
            return;
        }
        Application.mainMenuActive = false;
        Application.gameChangedSinceLastSave = false;
        roomObjectTick = null;
        currentFont = ingameFont;
        currentFontHeight = currentFont.getHeight();
        Application.adjacentRoomIds.removeAllElements();
        Application.resourceRestartImportants();
        loadAndRealizeExtras();
        GameResource.imagesLRE.removeAllElements();
        Enumeration enumerationElements = GameResource.imagesImportants.elements();
        while (enumerationElements.hasMoreElements()) {
            GameResource.imagesLRE.addElement(enumerationElements.nextElement());
        }
        System.gc();
        if (roomID == "setup") {
            if (!Application.setupDone) {
                Application.cursorX = Application.roomWidth >> 1;
                Application.cursorY = Application.roomHeight >> 1;
            }
            Application.gotoDissolveFXEnabled = false;
            Application.gotoDissolveFXColor = -1;
            Application.gameInitiated = false;
            SilentHillGame.menuResetIngameValues();
            Application.realizedExtras.removeAllElements();
            Application.endGame = false;
            if (Application.roomGraphics != null) {
                Application.roomGraphics.setColor(16777215);
                Application.roomGraphics.fillRect(0, 0, Application.roomImage.getWidth(), Application.roomImage.getHeight());
            }
        } else {
            if (roomID.equals("back")) {
                roomID = Application.roomGetLastInRoomHistory();
                Application.roomRemoveLastInRoomHistory();
                Application.selectedRoom = roomID;
            } else if (saveCurToHistory && !roomID.equals(Application.roomGetCurrent())) {
                Application.roomAddToRoomHistory(Application.roomGetCurrent());
            }
            Application.roomSetCurrent(roomID);
        }
        Application.nextRoomInit = true;
        Application.loadStart(0);
    }

    static boolean menuInvSetup() {
        String itemName;
        int itemCount;
        String inventoryHint;
        String[] downloadedItemIds = null;
        int[] downloadedItemAmounts = null;
        String[] hintedVariableNames = Application.inkServerAllNamesWithHint(Application.charToString('V'));
        String[] inventoryIds = Application.removeStringPrefix(hintedVariableNames, "inv-");
        int length = inventoryIds.length;
        curInvIds = new Object[length];
        curInvNames = new String[length];
        curInvNumOfItems = 0;
        curInvCounter = 0;
        boolean firstItemInserted = false;
        if (invFirstItemId != null && (inventoryHint = Application.inkServerGetHint(new StringBuffer().append("inv-").append(invFirstItemId).toString())) != null && inventoryHint.equals(Application.charToString('V'))) {
            if (battleMode && battleType == 1) {
                InkScript script = (InkScript) InkScript.list.get(invFirstItemId);
                if (!script.hasEvent(InkCodes.EVENT_ISEQUIPABLE) && !script.hasCommand(InkCodes.COMMAND_GOTO)) {
                    curInvNumOfItems = 1;
                    firstItemInserted = true;
                }
            } else {
                curInvNumOfItems = 1;
                firstItemInserted = true;
            }
        }
        for (int i2 = 0; i2 < length; i2++) {
            String itemId = inventoryIds[i2];
            if (battleMode && battleType == 1) {
                InkScript itemScript = (InkScript) InkScript.list.get(itemId);
                if (itemScript.hasEvent(InkCodes.EVENT_ISEQUIPABLE) || itemScript.hasCommand(InkCodes.COMMAND_GOTO)) {
                    continue;
                }
            }
            itemName = InkScript.getItemName(itemId);
            if (itemName == null) {
                InputStream resourceStream = Application.resourceGet(Application.loadRequest_getResourcePath(1, itemId));
                Application.loadScript(resourceStream, itemId);
                try {
                    resourceStream.close();
                } catch (Exception e) {
                }
                itemName = InkScript.getItemName(itemId);
            }
            itemCount = Application.toInt(Application.inkServerGetVariable(hintedVariableNames[i2]));
            if (itemCount > 1) {
                itemName = new StringBuffer().append(itemName).append(" (").append(itemCount).append(")").toString();
            }
            if (firstItemInserted && invFirstItemId.equals(itemId)) {
                curInvIds[0] = itemId;
                curInvNames[0] = itemName;
                continue;
            }
            curInvIds[curInvNumOfItems] = itemId;
            String[] inventoryNames = curInvNames;
            int insertionIndex = curInvNumOfItems;
            curInvNumOfItems = insertionIndex + 1;
            inventoryNames[insertionIndex] = itemName;
        }
        if (0 > 0) {
            Application.loadStart(3);
            while (Application.loading()) {
            }
            for (int i6 = 0; i6 < 0; i6++) {
                String downloadedItemName = InkScript.getItemName(downloadedItemIds[i6]);
                int downloadedItemAmount = downloadedItemAmounts[i6];
                if (downloadedItemAmount > 1) {
                    downloadedItemName = new StringBuffer().append(downloadedItemName).append(" (").append(downloadedItemAmount).append(")").toString();
                }
                if (firstItemInserted && invFirstItemId.equals(downloadedItemIds[i6])) {
                    curInvIds[0] = downloadedItemIds[i6];
                    curInvNames[0] = downloadedItemName;
                } else {
                    curInvIds[curInvNumOfItems] = downloadedItemIds[i6];
                    String[] inventoryNames = curInvNames;
                    int insertionIndex = curInvNumOfItems;
                    curInvNumOfItems = insertionIndex + 1;
                    inventoryNames[insertionIndex] = downloadedItemName;
                }
            }
        }
        return curInvNumOfItems > 0;
    }

    public static String[] wrapString(String text, int maxLength) {
        return wrapString(text, maxLength, currentFont);
    }

    public static String[] wrapString(String text, int maxLength, Font font) {
        String previousText;
        int newlineIndex;
        do {
            previousText = new String(text);
            text = Application.txtStringReplace(Application.txtStringReplace(text, "\\n", "\n"), "\\N", "\n");
        } while (previousText.compareTo(text) != 0);
        if (font.stringWidth(text) <= maxLength && text.indexOf(10) == -1) {
            if (text.indexOf(43) != -1) {
                text = text.replace('+', ' ');
            }
            return new String[]{text};
        }
        Vector lines = new Vector(2);
        boolean containsNewline = false;
        if (text.indexOf(10) != -1) {
            containsNewline = true;
        }
        while (text.length() > 0) {
            String wrappedLine = "";
            boolean firstWord = true;
            boolean newlineReached = false;
            while (text.length() > 0) {
                int nextDelimiterIndex = text.indexOf(32);
                newlineReached = false;
                if (containsNewline && (newlineIndex = text.indexOf(10)) != -1 && (newlineIndex <= nextDelimiterIndex || nextDelimiterIndex == -1)) {
                    newlineReached = true;
                    nextDelimiterIndex = newlineIndex;
                }
                String nextWord = nextDelimiterIndex >= 0 ? text.substring(0, nextDelimiterIndex + 1) : text;
                if (nextWord.indexOf(43) != -1) {
                    nextWord = nextWord.replace('+', ' ');
                }
                if (font.stringWidth(new StringBuffer().append(wrappedLine).append(nextWord).toString()) > maxLength) {
                    if (!firstWord) {
                        break;
                    }
                    int length = nextWord.length();
                    while (length > 0 && (nextWord.charAt(length - 1) == '.' || nextWord.charAt(length - 1) == ',' || nextWord.charAt(length - 1) == ':')) {
                        length--;
                    }
                    int splitOffset = 0;
                    while (splitOffset < length - LETTERS_FOR_NEW_ROW && font.stringWidth(nextWord.substring(0, splitOffset + 1)) <= maxLength) {
                        splitOffset++;
                    }
                    if (newlineReached && splitOffset == length - 1) {
                        splitOffset = length;
                    }
                    wrappedLine = nextWord.charAt(splitOffset - 1) == '-' ? new StringBuffer().append(wrappedLine).append(nextWord.substring(0, splitOffset)).toString() : new StringBuffer().append(wrappedLine).append(nextWord.substring(0, splitOffset)).append("-").toString();
                    text = text.substring(splitOffset);
                } else {
                    wrappedLine = new StringBuffer().append(wrappedLine).append(nextWord).toString();
                    text = text.substring(nextWord.length());
                }
                firstWord = false;
                if (newlineReached) {
                    break;
                }
            }
            if (newlineReached && wrappedLine.indexOf(10) != -1) {
                wrappedLine = wrappedLine.replace('\n', ' ');
            }
            lines.addElement(wrappedLine);
        }
        String[] wrappedLines = new String[lines.size()];
        lines.copyInto(wrappedLines);
        return wrappedLines;
    }

    static void say(String text, String header, boolean useFullScreen) {
        int menuY;
        int headerlessMenuY;
        MenuModel menu = SilentHillGame.menuCreate(useFullScreen ? MENU_SAY_FULLSCREEN : 6, Application.canvasWidth - ((ingameMargin << 1) + (ingameUseImageBorders ? ingameBorderSizeLeft + ingameBorderSizeRight : ingameBorderSize << 1)));
        if (header == null) {
            menu.setTop(text);
            int menuX = ingameUseImageBorders ? ingameBorderSizeLeft : ingameBorderSize;
            if (useFullScreen) {
                headerlessMenuY = (ingameUseImageBorders ? ingameBorderSizeTop : ingameBorderSize) + 1;
            } else {
                headerlessMenuY = (Application.canvasHeight - ((ingameFontHeight + ingameLineSpacing) << 2)) - (ingameFontHeight >> 1);
            }
            menu.setPosition(menuX, headerlessMenuY);
            return;
        }
        menu.setTop(header);
        menu.addChoice(1, text);
        int menuX = ingameUseImageBorders ? ingameBorderSizeLeft : ingameBorderSize;
        if (useFullScreen) {
            menuY = (ingameUseImageBorders ? ingameBorderSizeTop : ingameBorderSize) + 1;
        } else {
            menuY = (((Application.canvasHeight - ((ingameFontHeight + ingameLineSpacing) << 2)) - (ingameFontHeight >> 1)) - (ingameUseImageBorders ? ingameDelimiterHeight : 2)) - (ingameMargin << 1);
        }
        menu.setPosition(menuX, menuY);
    }

    static void textFadeCenter() {
        if (textFadeList == null) {
            return;
        }
        textFadePresentColor = textFadeList[0];
        int length = textFadeList.length;
        long currentTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < length; i += 2) {
            currentTimeMillis -= (long) textFadeList[i + 1];
            if (currentTimeMillis < textFadeStartTime) {
                textFadePresentColor = textFadeList[i];
                return;
            }
            if (i == length - 2) {
                textFadeStartTime = 0L;
                textFadeList = null;
                textFadeMenuClose = true;
            }
        }
    }

    static void textFadeCenterStart(String text) {
        MenuModel createdMenu = SilentHillGame.menuCreate(MENU_TEXT_FADE_CENTER, ((Application.canvasWidth < Application.roomWidth ? Application.canvasWidth : Application.roomWidth) * 3) >> 2);
        textFadeStartTime = System.currentTimeMillis();
        createdMenu.setTop(text);
        createdMenu.setPosition(Application.canvasWidth < Application.roomWidth ? Application.canvasCenterX : Application.roomWidth >> 1, Application.canvasHeight < Application.roomHeight ? Application.canvasCenterY : Application.roomHeight >> 1);
    }

    static void inventoryAdd(String itemID, int amount) {
        if (Application.getGameText(163).equals(InkScript.getItemName(itemID)) && Application.demoMode != null && Application.demoUrl != null && ((Integer.parseInt(Application.demoMode) == 1 || Integer.parseInt(Application.demoMode) == 2) && Application.demoUrl != null)) {
            Application.enableDemoDissolve = true;
            Application.gotoDissolveFXCounter = 3;
        }
        MenuModel createdMenu = SilentHillGame.menuCreate(7, Application.canvasCenterX);
        String itemMessage = Application.txtStringReplace(Application.getString(TextId.JAVA_APP_INK_RECEIVED), "<item name>", new StringBuffer().append("\n").append(InkScript.getItemName(itemID)).toString());
        if (amount != -1) {
            createdMenu.setTop(new StringBuffer().append(itemMessage).append(" (").append(amount).append(")").toString());
        } else {
            createdMenu.setTop(itemMessage);
        }
        createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
        createdMenu.setInvItemResource(InkScript.getInventoryImage(itemID));
    }

    static void paint(Graphics graphics) {
        Application.gfx = graphics;
        Application.gfx.setFont(currentFont);
        SilentHillGame.appPaint();
        menuPaintLoadingBar();
        Application.painting = false;
        if (Application.roomGetCurrent().equals("q") || Application.roomGetCurrent().equals("r") || Application.roomGetCurrent().equals("1j") || Application.roomGetCurrent().equals("1l") || Application.roomGetCurrent().equals("1n") || Application.roomGetCurrent().equals("20") || Application.roomGetCurrent().equals("22") || Application.roomGetCurrent().equals("24")) {
            graphics.setColor(0);
            graphics.drawLine(Application.canvasWidth - 1, 0, Application.canvasWidth - 1, Application.canvasHeight - 44);
        }
    }

    static void roomPaint() {
        if (Application.roomImage != null) {
            if (Application.roomWidth < Application.canvasWidth || Application.roomHeight < Application.canvasHeight) {
                Application.gfx.setColor(Application.ingameBgColor);
                Application.gfx.fillRect(0, 0, -Application.roomScrollOffsetX, Application.canvasHeight);
                Application.gfx.fillRect((-Application.roomScrollOffsetX) + Application.roomWidth, 0, ((-Application.roomScrollOffsetX) + Application.canvasWidth) - Application.roomWidth, Application.canvasHeight);
                Application.gfx.fillRect(-Application.roomScrollOffsetX, 0, Application.roomWidth, -Application.roomScrollOffsetY);
                Application.gfx.fillRect(-Application.roomScrollOffsetX, (-Application.roomScrollOffsetY) + Application.roomHeight, Application.roomWidth, ((-Application.roomScrollOffsetY) + Application.canvasHeight) - Application.roomHeight);
            }
            Application.gfx.drawImage(Application.roomImage, -Application.roomScrollOffsetX, -Application.roomScrollOffsetY, 0);
            for (int i = 0; Application.roomObjects != null && i < Application.roomObjects.length; i++) {
                if (Application.roomObjects[i] != null) {
                    if (i == 6 && Application.roomGetCurrent().toLowerCase().equals("0")) {
                        Application.roomObjects[i].runAnimLoops = 1;
                        if (RoomObject.paintingAnimationTime == -1) {
                            RoomObject.paintingAnimationTime = Application.roomObjects[i].animationTime;
                        } else {
                            Application.roomObjects[i].animationTime = RoomObject.paintingAnimationTime;
                        }
                    }
                    if (Application.roomObjects[i].runAnimLoops > 0) {
                        Application.roomObjects[i].runAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY);
                    } else {
                        Application.roomObjects[i].animPaint(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY);
                    }
                    if (i == 6 && Application.roomGetCurrent().toLowerCase().equals("0")) {
                        RoomObject.paintingAnimationTime = Application.roomObjects[i].animationTime;
                    }
                }
            }
            if (Application.loadingBarEngineMode) {
                Application.loadingBarEngineMode = false;
            }
        }
    }

    public static void mainMenuKeyHandling() {
        switch (Application.keyDown) {
            case -11:
            case GameCanvas.KEY_ERASE:
            case GameCanvas.KEY_RIGHT_SOFT:
                Application.midlet.destroyApp(true);
                break;
            case GameCanvas.KEY_LEFT_SOFT:
            case GameCanvas.KEY_MIDDLE_SOFT:
                switch (Application.toInt(MenuModel.getCurrent().getChoiceID())) {
                    case 3:
                        if (Application.inkServerGamesOwned <= 1 && Application.inkServerGamesSaved <= 0) {
                            if (Application.inkServerGamesOwned != 1) {
                                popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                            } else {
                                Application.resetVariableSystem();
                                if (!Application.setGameSpecificData(1)) {
                                    popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                                    popupCreate("TODO - error initiating game!!!", 0);
                                } else {
                                    startNewGame(Application.gameId);
                                }
                            }
                        } else if (Application.inkServerGamesSaved != 0) {
                            MenuModel createdMenu = SilentHillGame.menuCreate(10, engineDefaultMenuWidth);
                            createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
                            if (Application.inkServerGamesSaved == 1) {
                                createdMenu.addChoice(2, Application.getString(TextId.JAVA_APP_INK_SAVED_GAME));
                            } else {
                                createdMenu.addChoice(2, Application.getString(TextId.JAVA_APP_INK_SAVED_GAMES));
                            }
                            if (Application.inkServerGamesOwned == 1) {
                                createdMenu.addChoice(13, Application.getString(TextId.JAVA_APP_INK_NEW_GAME));
                            } else {
                                createdMenu.addChoice(13, Application.getString(TextId.JAVA_APP_INK_NEW_GAMES));
                            }
                            createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), Application.getString(TextId.JAVA_APP_INK_BACK));
                        } else {
                            popupCreate("TODO - not supported!!!", 0);
                        }
                        break;
                    case 4:
                        createSettingsMenu();
                        break;
                    case 5:
                        MenuModel secondaryMenu = SilentHillGame.menuCreate(11, engineDefaultMenuWidth);
                        secondaryMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
                        secondaryMenu.addChoice(9, Application.getString(TextId.JAVA_APP_INK_GAMEPLAY));
                        secondaryMenu.addChoice(10, Application.getString(TextId.JAVA_APP_INK_CONTROLS));
                        secondaryMenu.addChoice(11, Application.getString(TextId.JAVA_APP_INK_HOW_TO));
                        secondaryMenu.addChoice(30, Application.getString(TextId.JAVA_APP_INK_ABOUT));
                        secondaryMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), Application.getString(TextId.JAVA_APP_INK_BACK));
                        break;
                }
                break;
            case -2:
                MenuModel.getCurrent().nextChoice();
                break;
            case -1:
                MenuModel.getCurrent().previousChoice();
                break;
        }
    }

    public static void engineMenuKeyHandling() {
        if (MenuModel.active()) {
            int menuId = MenuModel.getCurrent().ID;
            if (!Application.keyNew) {
                if (isMenuScrollAllowed()) {
                    if (Application.keyDown == -1) {
                        if (menuId == 21 || menuId == MENU_ENGINE_FULLSCREEN2) {
                            MenuModel.getCurrent().scrollIncrease();
                            return;
                        }
                        return;
                    }
                    if (Application.keyDown == -2) {
                        if (menuId == 21 || menuId == MENU_ENGINE_FULLSCREEN2) {
                            MenuModel.getCurrent().scrollDecrease();
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            if (menuId == 8) {
                SilentHillGame.mainMenuKeyHandling();
                return;
            }
            switch (Application.keyDown) {
                case -11:
                case GameCanvas.KEY_ERASE:
                case GameCanvas.KEY_RIGHT_SOFT:
                    switch (menuId) {
                        case 20:
                            break;
                        case MENU_SOUND_VIBRA_SETTINGS:
                            MenuModel.getCurrent();
                            MenuModel.closeCurrent();
                            splashStart();
                            break;
                        case MENU_ENGINE_FULLSCREEN2:
                            Application.midlet.destroyApp(true);
                            break;
                        default:
                            MenuModel.closeCurrent();
                            break;
                    }
                    break;
                case GameCanvas.KEY_LEFT_SOFT:
                case GameCanvas.KEY_MIDDLE_SOFT:
                    switch (menuId) {
                        case MENU_SETTINGS:
                        case MENU_SOUND_VIBRA_SETTINGS:
                            switch (Application.toInt(MenuModel.getCurrent().getChoiceID())) {
                                case 7:
                                    Application.soundToggle();
                                    MenuModel.closeCurrent();
                                    if (menuId != 9) {
                                        createVibraSoundMenu();
                                    } else {
                                        createSettingsMenu();
                                    }
                                    break;
                                case CHOICE_LANG:
                                    int posInLanguageSelectionList = Application.getPosInLanguageSelectionList(Application.curLanguageId);
                                    int nextLanguageIndex = posInLanguageSelectionList < Application.languages.length - 1 ? posInLanguageSelectionList + 1 : 0;
                                    Application.saveLanguage(Application.languages[nextLanguageIndex]);
                                    Application.loadLanguage(Application.languages[nextLanguageIndex]);
                                    MenuModel.closeAll();
                                    languageChange = true;
                                    createMainMenu();
                                    createSettingsMenu().previousChoice();
                                    break;
                                case 14:
                                    MenuModel.closeCurrent();
                                    (menuId == 9 ? createSettingsMenu() : createVibraSoundMenu()).nextChoice();
                                    break;
                            }
                            break;
                        case 10:
                            if (Application.toInt(MenuModel.getCurrent().getChoiceID()) != 2) {
                                if (Application.inkServerGamesOwned > 1) {
                                    popupCreate("TODO - not supported!!!", 0);
                                } else if (Application.inkServerGamesOwned != 1) {
                                    popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                                } else {
                                    Application.resetVariableSystem();
                                    if (!Application.setGameSpecificData(1)) {
                                        popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                                        popupCreate("TODO - error initiating game!!!", 0);
                                    } else if (!savedGameExistsInRMS(Application.gameId)) {
                                        startNewGame(Application.gameId);
                                    } else {
                                        popupCreate(Application.getString(TextId.JAVA_APP_INK_START_NEW_GAME_WITH_SAVED), 5);
                                    }
                                }
                            } else if (Application.inkServerGamesSaved > 1) {
                                popupCreate("TODO - not supported!!!", 0);
                            } else if (Application.inkServerGamesSaved != 1) {
                                popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                            } else {
                                Application.resetVariableSystem();
                                if (Application.setGameSpecificData(1) && loadGameFromRMS(Application.gameId)) {
                                    menuPaintWithoutSoftkeys();
                                    SilentHillGame.cursorHintEnable = !Application.toBoolean(Application.inkServerGetVariable("s_cursorHintDisable"));
                                    Application.startSavedGame(Application.gameNumber);
                                } else {
                                    popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                                    popupCreate("TODO - error initiating game!!!", 0);
                                }
                            }
                            break;
                        case MENU_HELP:
                            switch (Application.toInt(MenuModel.getCurrent().getChoiceID())) {
                                case CHOICE_GAMEPLAY:
                                    createEngineFullscreenEngineMenu(Application.getString(TextId.JAVA_APP_INK_GAMEPLAY_TEXT));
                                    break;
                                case 10:
                                    createEngineFullscreenEngineMenu(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_CONTROLS1)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS2)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS3)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS6)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS4)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS7)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS5)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS8)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS9)).toString(), "<JAVA_APP_INK_UP>", Application.getString(TextId.JAVA_APP_INK_UP)), "<JAVA_APP_INK_DOWN>", Application.getString(TextId.JAVA_APP_INK_DOWN)), "<JAVA_APP_INK_LEFT>", Application.getString(TextId.JAVA_APP_INK_LEFT)), "<JAVA_APP_INK_RIGHT>", Application.getString(TextId.JAVA_APP_INK_RIGHT)), "<JAVA_APP_INK_NUM2>", Application.getString(TextId.JAVA_APP_INK_NUM2)), "<JAVA_APP_INK_NUM8>", Application.getString(TextId.JAVA_APP_INK_NUM8)), "<JAVA_APP_INK_NUM4>", Application.getString(TextId.JAVA_APP_INK_NUM4)), "<JAVA_APP_INK_NUM6>", Application.getString(TextId.JAVA_APP_INK_NUM6)), "<JAVA_APP_INK_FIRE>", Application.getString(TextId.JAVA_APP_INK_FIRE)), "<JAVA_APP_INK_NUM5>", Application.getString(TextId.JAVA_APP_INK_NUM5)), "<JAVA_APP_INK_SOFTKEY_B>", Application.getString(TextId.JAVA_APP_INK_SOFTKEY_B)), "<JAVA_APP_INK_NUM1>", Application.getString(TextId.JAVA_APP_INK_NUM1)), "<JAVA_APP_INK_UP>", Application.getString(TextId.JAVA_APP_INK_UP)), "<JAVA_APP_INK_DOWN>", Application.getString(TextId.JAVA_APP_INK_DOWN)), "<JAVA_APP_INK_NUM2>", Application.getString(TextId.JAVA_APP_INK_NUM2)), "<JAVA_APP_INK_NUM8>", Application.getString(TextId.JAVA_APP_INK_NUM8)), "<JAVA_APP_INK_FIRE>", Application.getString(TextId.JAVA_APP_INK_FIRE)), "<JAVA_APP_INK_NUM5>", Application.getString(TextId.JAVA_APP_INK_NUM5)), "<JAVA_APP_INK_SOFTKEY_B>", Application.getString(TextId.JAVA_APP_INK_SOFTKEY_B)), "<JAVA_APP_INK_SOFTKEY_A>", Application.getString(TextId.JAVA_APP_INK_SOFTKEY_A)));
                                    break;
                                case CHOICE_HOW_TO_DL:
                                    createEngineFullscreenEngineMenu(Application.getString(TextId.JAVA_APP_INK_HOW_TO_TEXT));
                                    break;
                                case 30:
                                    createEngineFullscreenEngineMenu(new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_ABOUT_NAME)).append(Application.appName).append("\n").append(Application.getString(TextId.JAVA_APP_INK_ABOUT_VERSION)).append(Application.appVersion).append("\n").append(Application.getString(TextId.JAVA_APP_INK_ABOUT_VENDOR)).append(Application.appVendor).toString());
                                    break;
                            }
                            break;
                        case 20:
                            int languageIndex = Application.toInt(MenuModel.getCurrent().getChoiceID());
                            Application.saveLanguage(Application.languages[languageIndex]);
                            Application.loadLanguage(Application.languages[languageIndex]);
                            createVibraSoundMenu();
                            break;
                        case MENU_ENGINE_FULLSCREEN2:
                            if (Application.inkServerGamesSaved > 0) {
                                MenuModel createdMenu = SilentHillGame.menuCreate(10, engineDefaultMenuWidth);
                                createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
                                createdMenu.addChoice(2, Application.getString(TextId.JAVA_APP_INK_SAVED_GAME));
                                createdMenu.addChoice(13, Application.getString(TextId.JAVA_APP_INK_NEW_GAME));
                                createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), Application.getString(TextId.JAVA_APP_INK_BACK));
                            } else if (Application.inkServerGamesOwned <= 0) {
                                popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                            } else {
                                Application.resetVariableSystem();
                                if (!Application.setGameSpecificData(1)) {
                                    popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                                    popupCreate("Error initiating game!!!", 0);
                                } else {
                                    startNewGame(Application.gameId);
                                }
                            }
                            break;
                    }
                    break;
                case -4:
                    switch (menuId) {
                        case MENU_SETTINGS:
                        case MENU_SOUND_VIBRA_SETTINGS:
                            int choiceId = Application.toInt(MenuModel.getCurrent().getChoiceID());
                            MenuModel.closeCurrent();
                            switch (choiceId) {
                                case 7:
                                    Application.soundToggle();
                                    if (menuId != 9) {
                                        createVibraSoundMenu();
                                    } else {
                                        createSettingsMenu();
                                    }
                                    break;
                                case CHOICE_LANG:
                                    int currentLanguageIndex = Application.getPosInLanguageSelectionList(Application.curLanguageId);
                                    int nextLanguageIndex = currentLanguageIndex < Application.languages.length - 1 ? currentLanguageIndex + 1 : 0;
                                    Application.saveLanguage(Application.languages[nextLanguageIndex]);
                                    Application.loadLanguage(Application.languages[nextLanguageIndex]);
                                    MenuModel.closeAll();
                                    languageChange = true;
                                    createMainMenu();
                                    createSettingsMenu().previousChoice();
                                    break;
                                case 14:
                                    (menuId == 9 ? createSettingsMenu() : createVibraSoundMenu()).nextChoice();
                                    break;
                            }
                            break;
                    }
                    break;
                case -3:
                    switch (menuId) {
                        case MENU_SETTINGS:
                        case MENU_SOUND_VIBRA_SETTINGS:
                            int choiceId = Application.toInt(MenuModel.getCurrent().getChoiceID());
                            MenuModel.closeCurrent();
                            switch (choiceId) {
                                case 7:
                                    Application.soundToggle();
                                    if (menuId != 9) {
                                        createVibraSoundMenu();
                                    } else {
                                        createSettingsMenu();
                                    }
                                    break;
                                case CHOICE_LANG:
                                    int currentLanguageIndex = Application.getPosInLanguageSelectionList(Application.curLanguageId);
                                    int previousLanguageIndex = currentLanguageIndex <= 0 ? Application.languages.length - 1 : currentLanguageIndex - 1;
                                    Application.saveLanguage(Application.languages[previousLanguageIndex]);
                                    Application.loadLanguage(Application.languages[previousLanguageIndex]);
                                    MenuModel.closeAll();
                                    languageChange = true;
                                    createMainMenu();
                                    createSettingsMenu().previousChoice();
                                    break;
                                case 14:
                                    (menuId == 9 ? createSettingsMenu() : createVibraSoundMenu()).nextChoice();
                                    break;
                            }
                            break;
                    }
                    break;
                case -2:
                    switch (menuId) {
                        case 21:
                            MenuModel.getCurrent().scrollDecrease();
                            break;
                        case MENU_ENGINE_FULLSCREEN2:
                            MenuModel.getCurrent().scrollDecrease();
                            break;
                        default:
                            MenuModel.getCurrent().nextChoice();
                            break;
                    }
                    break;
                case -1:
                    switch (menuId) {
                        case 21:
                            MenuModel.getCurrent().scrollIncrease();
                            break;
                        case MENU_ENGINE_FULLSCREEN2:
                            MenuModel.getCurrent().scrollIncrease();
                            break;
                        default:
                            MenuModel.getCurrent().previousChoice();
                            break;
                    }
                    break;
            }
        }
    }

    public static void ingameMoveKeyHandling() {
        int horizontalAcceleration = -Application.cursorSpeedX;
        int verticalAcceleration = -Application.cursorSpeedY;
        switch (Application.keyDown) {
            case -4:
                horizontalAcceleration = 1;
                break;
            case -3:
                horizontalAcceleration = -1;
                break;
            case -2:
                verticalAcceleration = 1;
                break;
            case -1:
                verticalAcceleration = -1;
                break;
        }
        if (Application.abs(Application.cursorSpeedX + horizontalAcceleration) <= 5) {
            Application.cursorSpeedX += horizontalAcceleration;
        }
        if (Application.abs(Application.cursorSpeedY + verticalAcceleration) <= 5) {
            Application.cursorSpeedY += verticalAcceleration;
        }
        Application.cursorX += Application.cursorSpeedX;
        Application.cursorY += Application.cursorSpeedY;
        if (Application.scrollRoomHor || Application.roomWidth < Application.canvasWidth) {
            if (Application.cursorX < 0) {
                Application.cursorX = 0;
            } else if (Application.cursorX >= Application.roomWidth) {
                Application.cursorX = Application.roomWidth - 1;
            }
        } else if (Application.cursorX < Application.roomScrollOffsetX) {
            Application.cursorX = Application.roomScrollOffsetX;
        } else if (Application.cursorX >= Application.min(Application.roomWidth, Application.roomScrollOffsetX + Application.canvasWidth)) {
            Application.cursorX = (Application.roomScrollOffsetX + Application.canvasWidth) - 1;
        }
        if (Application.scrollRoomVer || Application.roomHeight < Application.canvasHeight) {
            if (Application.cursorY < 0) {
                Application.cursorY = 0;
            } else if (Application.cursorY >= Application.roomHeight) {
                Application.cursorY = Application.roomHeight - 1;
            }
        } else if (Application.cursorY < Application.roomScrollOffsetY) {
            Application.cursorY = Application.roomScrollOffsetY;
        } else if (Application.cursorY >= Application.min(Application.roomHeight, Application.roomScrollOffsetY + Application.canvasHeight)) {
            Application.cursorY = Application.min(Application.roomHeight - 1, (Application.roomScrollOffsetY + Application.canvasHeight) - 1);
        }
        if (!Application.scrollRoomHor) {
            int relativeCursorX = Application.cursorX - Application.roomScrollOffsetX;
            if (relativeCursorX < 0) {
                Application.roomScrollOffsetX = Application.max(0, Application.roomScrollOffsetX - Application.canvasCenterX);
                Application.roomRepaintNeeded = true;
            } else if (relativeCursorX >= Application.canvasWidth) {
                Application.roomScrollOffsetX = Application.min(Application.roomWidth - Application.canvasWidth, Application.roomScrollOffsetX + Application.canvasCenterX);
                Application.roomRepaintNeeded = true;
            }
            if (Application.roomRepaintNeeded) {
                Application.cursorSpeedX = 0;
            }
        } else if (Application.smoothScrollDisable) {
            Application.roomScrollOffsetX = Application.min(Application.roomWidth - Application.canvasWidth, Application.cursorX - Application.canvasCenterX);
        } else {
            int targetScrollX = Application.max(0, Application.min(Application.roomWidth - Application.canvasWidth, Application.cursorX - Application.canvasCenterX));
            if (Application.abs(targetScrollX - Application.roomScrollOffsetX) >= 5) {
                Application.roomScrollOffsetX += (targetScrollX - Application.roomScrollOffsetX) / 5;
            } else if (targetScrollX - Application.roomScrollOffsetX != 0) {
                Application.roomScrollOffsetX += targetScrollX < Application.roomScrollOffsetX ? -1 : 1;
            }
        }
        if (!Application.scrollRoomVer) {
            int relativeCursorY = Application.cursorY - Application.roomScrollOffsetY;
            if (relativeCursorY < 0) {
                Application.roomScrollOffsetY = Application.max(0, Application.roomScrollOffsetY - Application.canvasCenterY);
                Application.roomRepaintNeeded = true;
            } else if (relativeCursorY >= Application.canvasHeight) {
                Application.roomScrollOffsetY = Application.min(Application.roomHeight - Application.canvasHeight, Application.roomScrollOffsetY + Application.canvasCenterY);
                Application.roomRepaintNeeded = true;
            }
            if (Application.roomRepaintNeeded) {
                Application.cursorSpeedY = 0;
            }
        } else if (!Application.smoothScrollDisable) {
            int targetScrollY = Application.max(0, Application.min(Application.roomHeight - Application.canvasHeight, Application.cursorY - Application.canvasCenterY));
            if (Application.abs(targetScrollY - Application.roomScrollOffsetY) >= 5) {
                Application.roomScrollOffsetY += (targetScrollY - Application.roomScrollOffsetY) / 5;
            } else if (targetScrollY - Application.roomScrollOffsetY != 0) {
                Application.roomScrollOffsetY += targetScrollY < Application.roomScrollOffsetY ? -1 : 1;
            }
        }
        Application.smoothScrollDisable = false;
        if (SilentHillGame.enemySoundPlayed) {
            return;
        }
        for (int i5 = 0; Application.roomObjects != null && i5 < Application.roomObjects.length; i5++) {
            RoomObject roomObject = Application.roomObjects[i5];
            if (cursorAnimActive && roomObject.script != null && roomObject.script.hasEvent(InkCodes.EVENT_ENEMYATTACK) && roomObject.visible) {
                int haloHalfWidth = cursorAnim[cursorAnimFrameCounter].imageWidth >> 1;
                int haloHalfHeight = cursorAnim[cursorAnimFrameCounter].imageHeight >> 1;
                if (roomObject.x - 8 > Application.cursorX - haloHalfWidth && roomObject.x + 8 < Application.cursorX + haloHalfWidth && roomObject.y - 8 > Application.cursorY - haloHalfHeight && roomObject.y + 8 < Application.cursorY + haloHalfHeight) {
                    SilentHillGame.enemySoundPlayed = true;
                    GameCanvas.playSound("sh_monster_hurt", 1);
                }
            }
        }
    }

    public static void inventoryUseItemMenuKeyHandling() {
        int menuId = MenuModel.getCurrent().ID;
        switch (Application.keyDown) {
            case -11:
            case GameCanvas.KEY_ERASE:
            case GameCanvas.KEY_RIGHT_SOFT:
                switch (menuId) {
                    case 7:
                        break;
                    default:
                        MenuModel.closeCurrent();
                        break;
                }
                break;
            case GameCanvas.KEY_LEFT_SOFT:
            case GameCanvas.KEY_MIDDLE_SOFT:
                switch (menuId) {
                    case 3:
                    case 5:
                        int choiceId = Application.toInt(MenuModel.getCurrent().getChoiceID());
                        switch (choiceId) {
                            case -3:
                                MenuModel.closeCurrent();
                                break;
                            case -2:
                                Integer defaultEventValue = new Integer(0);
                                if (defaultEventValue.equals(Application.overRoomObject.executeEvent(InkCodes.EVENT_USEITEM, defaultEventValue, false))) {
                                    SilentHillGame.say(Application.useItemSayText, null, false);
                                }
                                break;
                            case -1:
                                break;
                            case 45:
                            case 46:
                                SilentHillGame.inventoryEquipUnequipHandling(choiceId);
                                break;
                            default:
                                InkScript.executeEvent(InkScript.itemID, choiceId, (Object) null, (RoomObject) null);
                                break;
                        }
                        break;
                    case 7:
                        MenuModel.closeCurrent();
                        InkScript.resume();
                        break;
                }
                break;
            case -4:
                switch (menuId) {
                    case 3:
                    case 5:
                        if (curInvNumOfItems > 1) {
                            if (curInvCounter == curInvNumOfItems - 1) {
                                curInvCounter = 0;
                            } else {
                                curInvCounter++;
                            }
                        }
                        MenuModel.closeCurrent();
                        if (menuId != 3) {
                            createInventory(curInvCounter, true);
                        } else {
                            createInventory(curInvCounter, false);
                        }
                        break;
                }
                break;
            case -3:
                switch (menuId) {
                    case 3:
                    case 5:
                        if (curInvNumOfItems > 1) {
                            if (curInvCounter == 0) {
                                curInvCounter = curInvNumOfItems - 1;
                            } else {
                                curInvCounter--;
                            }
                        }
                        MenuModel.closeCurrent();
                        if (menuId != 3) {
                            createInventory(curInvCounter, true);
                        } else {
                            createInventory(curInvCounter, false);
                        }
                        break;
                }
                break;
            case -2:
                MenuModel.getCurrent().nextChoice();
                break;
            case -1:
                MenuModel.getCurrent().previousChoice();
                break;
        }
    }

    public static void inventoryEquipUnequipHandling(int choiceID) {
        MenuModel.closeAll();
        InkScript.executeEvent(InkScript.itemID, choiceID, (Object) null, (RoomObject) null);
    }

    public static void ingameMenuKeyHandling() {
        int menuId = MenuModel.getCurrent().ID;
        if (!Application.keyNew) {
            if (isMenuScrollAllowed()) {
                if (Application.keyDown == -1) {
                    if (menuId == 6 || menuId == MENU_SAY_FULLSCREEN) {
                        MenuModel.getCurrent().scrollIncrease();
                        return;
                    }
                    return;
                }
                if (Application.keyDown == -2) {
                    if (menuId == 6 || menuId == MENU_SAY_FULLSCREEN) {
                        MenuModel.getCurrent().scrollDecrease();
                        return;
                    }
                    return;
                }
                return;
            }
            return;
        }
        if (menuId == 3 || menuId == 5 || menuId == 7) {
            inventoryUseItemMenuKeyHandling();
            return;
        }
        switch (Application.keyDown) {
            case -11:
            case GameCanvas.KEY_ERASE:
            case GameCanvas.KEY_RIGHT_SOFT:
                switch (menuId) {
                    case 2:
                        InkInterpreter.pausedThread = null;
                        MenuModel.closeCurrent();
                        break;
                    case 6:
                        String lowerCase = Application.roomGetCurrent().toLowerCase();
                        if (lowerCase.equals("control") || lowerCase.equals("karen_fall_sleep") || lowerCase.equals("ben_backtolight") || lowerCase.indexOf("intro") != -1 || lowerCase.indexOf("end") != -1) {
                            MenuModel.closeCurrent();
                            while (!Application.endGame && lowerCase.equals(Application.roomGetCurrent().toLowerCase())) {
                                InkScript.resume();
                            }
                        }
                        break;
                    case 17:
                        battlePaused = false;
                        MenuModel.closeCurrent();
                        Application.soundContinue = true;
                        Application.soundContinue2 = true;
                        break;
                    case MENU_SAY_FULLSCREEN:
                        MenuModel.closeCurrent();
                        break;
                    case MENU_TEXT_FADE_CENTER:
                        String currentRoomId = Application.roomGetCurrent().toLowerCase();
                        if (!currentRoomId.equals("game_end")) {
                            if (currentRoomId.equals("control") || currentRoomId.equals("karen_fall_sleep") || currentRoomId.equals("ben_backtolight") || currentRoomId.indexOf("intro") != -1 || currentRoomId.indexOf("end") != -1) {
                                MenuModel.closeCurrent();
                                while (!Application.endGame && currentRoomId.equals(Application.roomGetCurrent().toLowerCase())) {
                                    InkScript.resume();
                                }
                                textFadeMenuClose = false;
                            } else {
                                textFadeMenuClose = true;
                            }
                            textFadeStartTime = 0L;
                            textFadeList = null;
                        }
                        break;
                    default:
                        MenuModel.closeCurrent();
                        break;
                }
                break;
            case GameCanvas.KEY_LEFT_SOFT:
            case GameCanvas.KEY_MIDDLE_SOFT:
                switch (menuId) {
                    case 1:
                        if (MenuModel.getCurrent().countChoices() != 0) {
                            int choiceId = Application.toInt(MenuModel.getCurrent().getChoiceID());
                            switch (choiceId) {
                                case -4:
                                    if (Application.toBoolean(InkScript.executeEvent("setupExtraMenuOption", InkCodes.EVENT_ISACTIVE, new Integer(-1), (RoomObject) null))) {
                                        MenuModel.closeAll();
                                        InkScript.executeEvent("setupExtraMenuOption", InkCodes.EVENT_GOTO, (Object) null, (RoomObject) null);
                                    }
                                    break;
                                case -2:
                                    if (menuInvSetup()) {
                                        createInventory(curInvCounter, false);
                                    }
                                    break;
                                default:
                                    MenuModel.closeCurrent();
                                    Application.overRoomObject.executeEvent(choiceId, null, false);
                                    break;
                            }
                        }
                        break;
                    case 2:
                        InkScript.choiceID = MenuModel.getCurrent().getChoiceID();
                        MenuModel.closeCurrent();
                        InkScript.resume();
                        break;
                    case 6:
                        MenuModel.closeCurrent();
                        InkScript.resume();
                        break;
                    case MENU_SETTINGS:
                        switch (Application.toInt(MenuModel.getCurrent().getChoiceID())) {
                            case 7:
                                Application.soundToggle();
                                MenuModel.closeCurrent();
                                createIngameSettingsMenu();
                                break;
                            case 14:
                                MenuModel.closeCurrent();
                                createIngameSettingsMenu().nextChoice();
                                break;
                        }
                        break;
                    case 17:
                    case 30:
                        switch (Application.toInt(MenuModel.getCurrent().getChoiceID())) {
                            case CHOICE_ESC_CONTROLS:
                                SilentHillGame.say(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(Application.txtStringReplace(new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_CONTROLS1)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS2)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS3)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS6)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS4)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS7)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS5)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS8)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CONTROLS9)).toString(), "<JAVA_APP_INK_UP>", Application.getString(TextId.JAVA_APP_INK_UP)), "<JAVA_APP_INK_DOWN>", Application.getString(TextId.JAVA_APP_INK_DOWN)), "<JAVA_APP_INK_LEFT>", Application.getString(TextId.JAVA_APP_INK_LEFT)), "<JAVA_APP_INK_RIGHT>", Application.getString(TextId.JAVA_APP_INK_RIGHT)), "<JAVA_APP_INK_NUM2>", Application.getString(TextId.JAVA_APP_INK_NUM2)), "<JAVA_APP_INK_NUM8>", Application.getString(TextId.JAVA_APP_INK_NUM8)), "<JAVA_APP_INK_NUM4>", Application.getString(TextId.JAVA_APP_INK_NUM4)), "<JAVA_APP_INK_NUM6>", Application.getString(TextId.JAVA_APP_INK_NUM6)), "<JAVA_APP_INK_FIRE>", Application.getString(TextId.JAVA_APP_INK_FIRE)), "<JAVA_APP_INK_NUM5>", Application.getString(TextId.JAVA_APP_INK_NUM5)), "<JAVA_APP_INK_SOFTKEY_B>", Application.getString(TextId.JAVA_APP_INK_SOFTKEY_B)), "<JAVA_APP_INK_NUM1>", Application.getString(TextId.JAVA_APP_INK_NUM1)), "<JAVA_APP_INK_UP>", Application.getString(TextId.JAVA_APP_INK_UP)), "<JAVA_APP_INK_DOWN>", Application.getString(TextId.JAVA_APP_INK_DOWN)), "<JAVA_APP_INK_NUM2>", Application.getString(TextId.JAVA_APP_INK_NUM2)), "<JAVA_APP_INK_NUM8>", Application.getString(TextId.JAVA_APP_INK_NUM8)), "<JAVA_APP_INK_FIRE>", Application.getString(TextId.JAVA_APP_INK_FIRE)), "<JAVA_APP_INK_NUM5>", Application.getString(TextId.JAVA_APP_INK_NUM5)), "<JAVA_APP_INK_SOFTKEY_B>", Application.getString(TextId.JAVA_APP_INK_SOFTKEY_B)), "<JAVA_APP_INK_SOFTKEY_A>", Application.getString(TextId.JAVA_APP_INK_SOFTKEY_A)), null, true);
                                break;
                            case CHOICE_ESC_GAMEPLAY:
                                SilentHillGame.say(Application.getString(TextId.JAVA_APP_INK_GAMEPLAY_TEXT), null, true);
                                break;
                            case CHOICE_ESC_MAIN:
                                if (Application.demoMode == null || Application.demoUrl == null) {
                                    if (!Application.gameChangedSinceLastSave || Application.roomGetCurrent().toLowerCase().equals("control")) {
                                        Application.endGame();
                                    } else if (InkInterpreter.pausedThread == null) {
                                        popupCreate(Application.getString(TextId.JAVA_APP_INK_REALLY_QUIT_QUESTION), 4);
                                        MenuModel.closeCurrent();
                                    } else {
                                        Application.endGame();
                                    }
                                    break;
                                } else if (Integer.parseInt(Application.demoMode) == 2 && Application.demoUrl != null) {
                                    popupCreate(Application.getString(TextId.STR_DOWNLOAD_NOW), 10);
                                    break;
                                } else if (Integer.parseInt(Application.demoMode) == 1 && Application.demoUrl != null) {
                                    Application.enableDemoDissolve = false;
                                    Application.exitTrial = true;
                                    createMainMenu();
                                    createEngineFullscreenEngineMenu(Application.getString(TextId.STR_DEMO_END_TEXT));
                                    break;
                                }
                                break;
                            case CHOICE_ESC_HELP:
                                MenuModel createdMenu = SilentHillGame.menuCreate(30, Application.canvasWidth >> 1);
                                createdMenu.setPosition(Application.canvasWidth, Application.canvasHeight);
                                createdMenu.addChoice(CHOICE_ESC_GAMEPLAY, Application.getString(TextId.JAVA_APP_INK_GAMEPLAY));
                                createdMenu.addChoice(CHOICE_ESC_CONTROLS, Application.getString(TextId.JAVA_APP_INK_CONTROLS));
                                break;
                            case -4:
                                createIngameSettingsMenu();
                                break;
                            case -3:
                                if (Application.saveIsPossible) {
                                    Application.saveGame(true);
                                }
                                MenuModel.closeCurrent();
                                Application.soundContinue = true;
                                Application.soundContinue2 = true;
                                break;
                            case -2:
                                battlePaused = false;
                                if (Application.roomGetCurrent().length() > 0) {
                                    MenuModel.closeCurrent();
                                    roomInit(Application.roomGetCurrent(), false);
                                }
                                break;
                            case -1:
                                battlePaused = false;
                                MenuModel.closeCurrent();
                                Application.soundContinue = true;
                                Application.soundContinue2 = true;
                                break;
                        }
                        break;
                    case MENU_TEXT_FADE_CENTER:
                        textFadeStartTime = 0L;
                        textFadeList = null;
                        textFadeMenuClose = true;
                        break;
                    case MENU_GOTO_CHEAT:
                        roomInit((String) MenuModel.getCurrent().getChoiceID(), false);
                        MenuModel.closeCurrent();
                        break;
                }
                break;
            case -2:
                switch (menuId) {
                    case 2:
                        MenuModel current = MenuModel.getCurrent();
                        if (current.getChoiceNr() < current.countChoices() - 1) {
                            current.nextChoice();
                        }
                        break;
                    case 6:
                    case MENU_SAY_FULLSCREEN:
                        MenuModel.getCurrent().scrollDecrease();
                        break;
                    default:
                        MenuModel.getCurrent().nextChoice();
                        break;
                }
                break;
            case -1:
                switch (menuId) {
                    case 2:
                        MenuModel currentMenu = MenuModel.getCurrent();
                        if (currentMenu.getChoiceNr() > 0) {
                            currentMenu.previousChoice();
                        }
                        break;
                    case 6:
                    case MENU_SAY_FULLSCREEN:
                        MenuModel.getCurrent().scrollIncrease();
                        break;
                    default:
                        MenuModel.getCurrent().previousChoice();
                        break;
                }
                break;
        }
    }

    public static void appKeyHandling() {
        if (Application.keyDown == GameCanvas.KEY_LEFT_SOFT && Application.DEMO_END) {
            Application.DEMO_END = false;
            Application.FADE_FRAMES = 0;
            Application.keyDown = 0;
            Application.endGame();
            Application.canvas.repaint();
        }
        if (popupActive) {
            popupKeyHandling();
            return;
        }
        if (rgbKonamiLogo != null) {
            logoHandling();
            return;
        }
        Application.lastStopKeyHandling = Application.stopKeyHandling;
        if (Application.stopKeyHandling || Application.waitForKeyHandling) {
            Application.keyDown = 0;
        }
        if (!Application.stopKeyHandling && Application.lastStopKeyHandling) {
            Application.timeStopKeyHandling = System.currentTimeMillis();
        }
        if (!Application.stopKeyHandling && System.currentTimeMillis() - Application.timeStopKeyHandling > 1500) {
            Application.waitForKeyHandling = false;
        }
        if (bossDead && Application.roomGetCurrent().equals("2d")) {
            Application.keyDown = 0;
        }
        if (splashHash != null) {
            splashHandling();
            return;
        }
        if (Application.mainMenuActive) {
            engineMenuKeyHandling();
            return;
        }
        boolean menuActive = MenuModel.active();
        if (battleMode && !menuActive && SilentHillGame.battleKeyHandling()) {
            Application.tickBasedTimeUpdate();
            return;
        }
        if (!menuActive) {
            ingameMoveKeyHandling();
            Application.tickBasedTimeUpdate();
            Application.overRoomObject = null;
            if (Application.roomObjects != null) {
                for (int length = Application.roomObjects.length - 1; Application.roomObjects != null && length > -1; length--) {
                    RoomObject roomObject = Application.roomObjects[length];
                    if (roomObject.isOver(Application.cursorX, Application.cursorY)) {
                        Application.overRoomObject = roomObject;
                        if (hoveredRoomObject != roomObject && !InkScript.isWaiting()) {
                            if (hoveredRoomObject != null) {
                                hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                                hoveredRoomObject = null;
                                break;
                            } else {
                                hoveredRoomObject = roomObject.enterHover();
                                break;
                            }
                        }
                        if (hoveredRoomObject != null) {
                            break;
                        }
                    } else {
                        if (hoveredRoomObject == roomObject) {
                            hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                            hoveredRoomObject = null;
                            break;
                        }
                    }
                }
            }
        }
        if (InkScript.isWaiting()) {
            return;
        }
        if (menuActive) {
            SilentHillGame.ingameMenuKeyHandling();
        } else {
            SilentHillGame.ingameNoMenuKeyHandling();
            actionKeyKeyHandling();
        }
    }

    public static void ingameNoMenuKeyHandling() {
        String extraChoiceText;
        if (Application.keyNew) {
            switch (Application.keyDown) {
                case -11:
                case GameCanvas.KEY_ERASE:
                case GameCanvas.KEY_RIGHT_SOFT:
                    SilentHillGame.createEscapeMenu(false, true);
                    break;
                case GameCanvas.KEY_LEFT_SOFT:
                case GameCanvas.KEY_MIDDLE_SOFT:
                    if (!Application.hideCursor) {
                        if (Application.overRoomObject == null || Application.overRoomObject.executeEvent(InkCodes.EVENT_CLICK, new Integer(-1), false) != null) {
                            MenuModel createdMenu = SilentHillGame.menuCreate(1, Application.canvasCenterX);
                            createdMenu.setPosition(Application.cursorX - Application.roomScrollOffsetX, Application.cursorY - Application.roomScrollOffsetY);
                            if (Application.overRoomObject == null) {
                                createdMenu.setTop(Application.getString(TextId.JAVA_APP_INK_INGAME_MENU));
                            } else {
                                createdMenu.setTop(Application.overRoomObject.getName());
                                SilentHillGame.addChoices(Application.overRoomObject, createdMenu);
                            }
                            if (Application.removeStringPrefix(Application.inkServerAllNamesWithHint(Application.charToString('V')), "inv-").length > 0) {
                                createdMenu.addChoice(-2, Application.getString(TextId.JAVA_APP_INK_INVENTORY));
                            }
                            InkScript.executeEvent("setupExtraMenuOption", InkCodes.EVENT_ISACTIVE, new Integer(-1), (RoomObject) null);
                            if (Application.toBoolean(InkScript.executeEvent("setupExtraMenuOption", InkCodes.EVENT_ISACTIVE, new Integer(-1), (RoomObject) null)) && (extraChoiceText = (String) InkScript.executeEvent("setupExtraMenuOption", InkCodes.EVENT_GETNAME, (Object) null, (RoomObject) null)) != null) {
                                createdMenu.addChoice(-4, extraChoiceText);
                            }
                            if (createdMenu.countChoices() == 0) {
                                MenuModel.closeCurrent();
                            }
                        }
                        break;
                    }
                    break;
            }
        }
    }

    public static void createEngineFullscreenEngineMenu(String text) {
        MenuModel createdMenu = SilentHillGame.menuCreate(21, Application.canvasWidth - 11);
        createdMenu.engineFullScreenScroll = true;
        createdMenu.setPosition(0, engineHeaderImageHeight);
        if (Application.exitTrial) {
            createdMenu.setSoftkeyOptions(null, Application.getString(TextId.JAVA_APP_INK_INGAME_MENU));
            Application.exitTrial = false;
        } else {
            createdMenu.setSoftkeyOptions(null, Application.getString(TextId.JAVA_APP_INK_BACK));
        }
        createdMenu.setTop(text);
    }

    public static void createEngineFullscreenEngineMenu2(String text) {
        MenuModel createdMenu = SilentHillGame.menuCreate(MENU_ENGINE_FULLSCREEN2, Application.canvasWidth - 11);
        createdMenu.engineFullScreenScroll = true;
        createdMenu.setPosition(0, engineHeaderImageHeight);
        createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_CONTINUE), Application.getString(TextId.JAVA_APP_INK_EXIT));
        createdMenu.setTop(text);
    }

    public static void createEscapeMenu(boolean loadError, boolean setSaveOption) {
        if (MenuModel.active() && MenuModel.getCurrent().ID == 17) {
            if (!loadError && (!battleMode || battleType != 1)) {
                return;
            } else {
                MenuModel.closeCurrent();
            }
        }
        MenuModel createdMenu = SilentHillGame.menuCreate(17, Application.canvasCenterX);
        createdMenu.setPosition(Application.canvasWidth, Application.canvasHeight);
        if (loadError) {
            createdMenu.addChoice(-2, Application.getString(TextId.JAVA_APP_INK_RELOAD));
        } else {
            createdMenu.addChoice(-1, Application.getString(TextId.JAVA_APP_INK_CONTINUE));
        }
        if (Application.saveIsPossible && setSaveOption && (!battleMode || battleType != 1)) {
            createdMenu.addChoice(-3, Application.getString(TextId.JAVA_APP_INK_SAVE));
        }
        createdMenu.addChoice(CHOICE_ESC_MAIN, Application.getString(TextId.JAVA_APP_INK_INGAME_MAIN_MENU));
    }

    static MenuModel createInventory(int index, boolean showOnlyUseOption) {
        MenuModel createdMenu = showOnlyUseOption ? SilentHillGame.menuCreate(5, 64) : SilentHillGame.menuCreate(3, (Application.canvasWidth >> 1) - (ingameMargin << 1));
        createdMenu.setPosition(Application.cursorX - Application.roomScrollOffsetX, Application.cursorY - Application.roomScrollOffsetY);
        createdMenu.setTop(curInvNames[curInvCounter]);
        InkScript.itemID = (String) curInvIds[index];
        if (showOnlyUseOption) {
            createdMenu.addChoice(-2, Application.getString(TextId.JAVA_APP_INK_USE));
        } else {
            SilentHillGame.addItemChoices(createdMenu, (String) curInvIds[index]);
        }
        createdMenu.addChoice(-3, Application.getString(TextId.JAVA_APP_INK_BACK));
        return createdMenu;
    }

    static void createMainMenu() {
        if (!Application.mainMenuActive) {
            InkScript.stop();
        }
        if (!firstSoundPlaying) {
            GameCanvas.stopSound();
        } else if (languageChange) {
            languageChange = false;
        } else {
            firstSoundPlaying = false;
        }
        battlePaused = false;
        battleState = 0;
        battleMode = false;
        MenuModel.closeAll();
        Application.mainMenuActive = true;
        Application.loadingBarEngineMode = true;
        currentFont = engineFont;
        currentFontHeight = currentFont.getHeight() + EXTRA_FONT_HEIGHT;
        MenuModel createdMenu = SilentHillGame.menuCreate(8, (Application.canvasWidth * 4) / 5);
        createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
        try {
            if ((Integer.parseInt(Application.demoMode) == 1 || Integer.parseInt(Application.demoMode) == 2) && Application.demoUrl != null) {
                createdMenu.addChoice(32, Application.getString(TextId.STR_GET_THE_GAME));
            }
        } catch (Exception e) {
        }
        try {
            if ((Integer.parseInt(Application.demoMode) == 1 || Integer.parseInt(Application.demoMode) == 2) && Application.demoUrl != null) {
                createdMenu.addChoice(CHOICE_START_TRIAL, Application.getString(TextId.STR_DEMO_START_TRIAL));
            } else {
                createdMenu.addChoice(3, Application.getString(TextId.JAVA_APP_INK_PLAY));
            }
        } catch (Exception e2) {
            createdMenu.addChoice(3, Application.getString(TextId.JAVA_APP_INK_PLAY));
        }
        try {
            if ((Integer.parseInt(Application.upSellMode) == 1 || Integer.parseInt(Application.upSellMode) == 2) && Application.upSellUrl != null) {
                createdMenu.addChoice(1, Application.getString(TextId.STR_UPSELL_TITLE));
            }
        } catch (Exception e3) {
        }
        createdMenu.addChoice(4, Application.getString(TextId.JAVA_APP_INK_SETTINGS));
        createdMenu.addChoice(5, Application.getString(TextId.JAVA_APP_INK_HELP));
        createdMenu.addChoice(30, Application.getString(TextId.JAVA_APP_INK_ABOUT));
        createdMenu.addChoice(6, Application.getString(TextId.JAVA_APP_INK_QUIT));
        createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), null);
    }

    static MenuModel createSettingsMenu() {
        String[] languageNames = new String[Application.languages.length];
        for (int i = 0; i < Application.languages.length; i++) {
            languageNames[i] = Application.getString(Application.language_text_ids[i]);
        }
        MenuModel createdMenu = SilentHillGame.menuCreate(9, engineDefaultMenuWidth);
        createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
        createdMenu.addChoice(7, new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_SOUND)).append(" ").append(Application.curSoundMode ? Application.getString(TextId.JAVA_APP_INK_ON) : Application.getString(TextId.JAVA_APP_INK_OFF)).toString());
        createdMenu.addChoice(8, new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_LANGUAGE)).append(" ").append(languageNames[Application.getPosInLanguageSelectionList(Application.curLanguageId)]).toString());
        createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_CHANGE), Application.getString(TextId.JAVA_APP_INK_BACK));
        return createdMenu;
    }

    static MenuModel createIngameSettingsMenu() {
        MenuModel createdMenu = SilentHillGame.menuCreate(9, engineDefaultMenuWidth);
        createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
        createdMenu.addChoice(7, new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_SOUND)).append(" ").append(Application.curSoundMode ? Application.getString(TextId.JAVA_APP_INK_ON) : Application.getString(TextId.JAVA_APP_INK_OFF)).toString());
        createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_CHANGE), Application.getString(TextId.JAVA_APP_INK_BACK));
        return createdMenu;
    }

    static MenuModel createVibraSoundMenu() {
        MenuModel.getCurrent();
        MenuModel.closeCurrent();
        MenuModel createdMenu = SilentHillGame.menuCreate(MENU_SOUND_VIBRA_SETTINGS, engineDefaultMenuWidth);
        createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
        createdMenu.addChoice(7, new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_SOUND)).append(" ").append(Application.curSoundMode ? Application.getString(TextId.JAVA_APP_INK_ON) : Application.getString(TextId.JAVA_APP_INK_OFF)).toString());
        createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_CHANGE), Application.getString(TextId.JAVA_APP_INK_CONTINUE));
        return createdMenu;
    }

    public static boolean isMenuScrollAllowed() {
        byte b = menuScrollTickCounter;
        menuScrollTickCounter = (byte) (b + 1);
        if (b < 1) {
            return false;
        }
        menuScrollTickCounter = (byte) 0;
        return true;
    }

    public static void updateSystemResourceValues(int id) {
        if (systemResources[id] == null) {
        }
        switch (id) {
            case SETUP_INDEX_INV_ARROW_LEFT:
                invArrowHeight = systemResources[11].image.getHeight();
                break;
            case 13:
                ingameBorderSizeTop = Application.max(ingameBorderSizeTop, systemResources[13].imageRegPointY);
                break;
            case 14:
                ingameBorderSizeBottom = Application.max(ingameBorderSizeBottom, systemResources[14].imageHeight - systemResources[14].imageRegPointY);
                break;
            case 17:
                ingameBorderSizeLeft = systemResources[17].imageRegPointX;
                ingameBorderSizeTop = Application.max(ingameBorderSizeTop, systemResources[17].imageRegPointY);
                break;
            case SETUP_INDEX_INGAME_RES_6_IMG:
                ingameBorderSizeRight = systemResources[SETUP_INDEX_INGAME_RES_6_IMG].imageWidth - systemResources[SETUP_INDEX_INGAME_RES_6_IMG].imageRegPointX;
                ingameBorderSizeTop = Application.max(ingameBorderSizeTop, systemResources[SETUP_INDEX_INGAME_RES_6_IMG].imageRegPointY);
                break;
            case 19:
                ingameBorderSizeBottom = Application.max(ingameBorderSizeBottom, systemResources[19].imageHeight - systemResources[19].imageRegPointY);
                break;
            case 20:
                ingameBorderSizeBottom = Application.max(ingameBorderSizeBottom, systemResources[20].imageHeight - systemResources[20].imageRegPointY);
                break;
            case 21:
                ingameDelimiterHeight = systemResources[21].imageHeight;
                break;
        }
    }

    public static boolean loadAndRealizeExtras() {
        boolean allLoaded = true;
        String extraMenuScriptPath = Application.loadRequest_getResourcePath(1, "setupExtraMenuOption");
        InputStream resourceStream = Application.resourceGet(extraMenuScriptPath);
        if (resourceStream == null || Application.realizedExtras.contains(extraMenuScriptPath) || !Application.assertScript(resourceStream)) {
            allLoaded = false;
        } else {
            try {
                resourceStream.close();
            } catch (Exception e) {
            }
            resourceStream = Application.resourceGet(Application.loadRequest_getResourcePath(1, "setupExtraMenuOption"));
            Application.loadScript(resourceStream, "setupExtraMenuOption");
            Application.realizedExtras.addElement(extraMenuScriptPath);
            try {
                resourceStream.close();
            } catch (Exception e2) {
            }
        }
        for (String itemId : Application.removeStringPrefix(Application.inkServerAllNamesWithHint(Application.charToString('V')), "inv-")) {
            String itemScriptPath = Application.loadRequest_getResourcePath(1, itemId);
            resourceStream = Application.resourceGet(itemScriptPath);
            if (resourceStream != null && !Application.realizedExtras.contains(itemId) && Application.assertScript(resourceStream)) {
                try {
                    resourceStream.close();
                } catch (Exception e3) {
                }
                resourceStream = Application.resourceGet(itemScriptPath);
                Application.loadScript(resourceStream, itemId);
                InkScript.getInventoryImage(itemId);
                Application.realizedExtras.addElement(itemId);
            }
            try {
                resourceStream.close();
            } catch (Exception e4) {
            }
        }
        if (systemResources == null) {
            systemResources = new GameResource[SilentHillGame.DEFAULT_RESOURCES.length];
        }
        for (int i = 0; i < SilentHillGame.DEFAULT_RESOURCES.length; i++) {
            if (systemResources[i] == null && SilentHillGame.DEFAULT_RESOURCES[i] != null && SilentHillGame.DEFAULT_RESOURCES[i].length() > 0) {
                String resourcePath = new StringBuffer().append(Application.gameId).append("/gfx/transform0/").append(SilentHillGame.DEFAULT_RESOURCES[i]).append(".png").toString();
                resourceStream = Application.resourceGet(resourcePath);
                if (resourceStream == null || Application.realizedExtras.contains(resourcePath)) {
                    allLoaded = false;
                } else {
                    systemResources[i] = GameResource.getImageFromSetup(resourcePath, SilentHillGame.DEFAULT_RESOURCES[i]);
                    Application.realizedExtras.addElement(resourcePath);
                }
                try {
                    resourceStream.close();
                } catch (Exception e5) {
                }
            }
        }
        boolean languageLoaded = allLoaded | Application.loadGameLanguage(Application.curLanguageId);
        try {
            resourceStream.close();
        } catch (Exception e6) {
        }
        return languageLoaded;
    }

    public static void addChoices(RoomObject ro, MenuModel menu) {
        String[] eventChoiceTexts = new String[56];
        eventChoiceTexts[9] = Application.getString(TextId.JAVA_APP_INK_LOOK_AT);
        eventChoiceTexts[15] = Application.getString(TextId.JAVA_APP_INK_USE);
        eventChoiceTexts[10] = Application.getString(TextId.JAVA_APP_INK_PICK_UP);
        eventChoiceTexts[13] = Application.getString(TextId.JAVA_APP_INK_PULL);
        eventChoiceTexts[11] = Application.getString(TextId.JAVA_APP_INK_OPEN);
        eventChoiceTexts[12] = Application.getString(TextId.JAVA_APP_INK_CLOSE);
        eventChoiceTexts[17] = Application.getString(TextId.JAVA_APP_INK_TALK_TO);
        eventChoiceTexts[26] = Application.getString(TextId.JAVA_APP_INK_ATTACK);
        eventChoiceTexts[8] = Application.getString(TextId.JAVA_APP_INK_GO_TO);
        for (int i = 0; i < 56; i++) {
            String choiceText = eventChoiceTexts[i];
            if (choiceText != null && ro.script.hasEvent(i)) {
                menu.addChoice(i, choiceText);
            }
        }
    }

    public static void actionKeyInitSystem() {
        actionKeyUnsetAllKeys();
        actionKey_keyCodes = new int[]{42, 35, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57};
    }

    public static void actionKeyUnsetAllKeys() {
        actionKey_scriptIds = new String[12];
    }

    public static int actionKeyIdConvert(String id) {
        if (id.equals("actionkey_star")) {
            return 0;
        }
        if (id.equals("actionkey_pound")) {
            return 1;
        }
        for (int i = 0; i < 10; i++) {
            if (id.equals(new StringBuffer().append("actionkey_num").append(i).toString())) {
                return 2 + i;
            }
        }
        return -1;
    }

    public static String actionKeyGetScriptId(int keycode) {
        int actionKeyIndex = actionKeyKeycodeToActionkey(keycode);
        if (actionKeyIndex != -1) {
            return actionKey_scriptIds[actionKeyIndex];
        }
        return null;
    }

    public static int actionKeyKeycodeToActionkey(int keycode) {
        int actionKeyIndex = -1;
        for (int i3 = 0; i3 < actionKey_keyCodes.length; i3++) {
            if (keycode == actionKey_keyCodes[i3]) {
                actionKeyIndex = i3;
                break;
            }
        }
        return actionKeyIndex;
    }

    public static void actionKeyKeyHandling() {
        String scriptId;
        if (!Application.keyNew || (scriptId = actionKeyGetScriptId(Application.keyDown)) == null) {
            return;
        }
        InkScript.executeEvent(scriptId, InkCodes.EVENT_CLICK, new Integer(-1), (RoomObject) null);
    }

    static void appInit() {
        Application.canvasWidth = 240;
        Application.canvasHeight = 320;
        Application.canvasCenterX = Application.canvasWidth >> 1;
        Application.canvasCenterY = Application.canvasHeight >> 1;
        Application.setDisplay(Application.canvas);
        Application.appName = Application.midlet.getAppProperty("MIDlet-Name");
        Application.appVersion = Application.midlet.getAppProperty("MIDlet-Version");
        Application.appVendor = Application.midlet.getAppProperty("MIDlet-Vendor");
        Application.upSellMode = Application.midlet.getAppProperty("ms-upSell");
        Application.upSellUrl = Application.midlet.getAppProperty("ms-upSellUrl");
        Application.demoMode = Application.midlet.getAppProperty("ms-demoMode");
        Application.demoUrl = Application.midlet.getAppProperty("ms-demoUrl");
        try {
            settingsHash = Application.readIni(Application.openJar("settings.ini"));
        } catch (Exception e) {
        }
        GameCanvas.keyInit();
        bigScreenHUD_down = Application.loadImageFromJAR("gfx/bigScreenHUD_hor.png");
        mapMenuCursor = Application.loadImageFromJAR("gfx/map_cursor.png");
        imgKonamiLogo = Application.loadImageFromJAR("gfx/konami_logo.png");
        imgKonamiRights = Application.loadImageFromJAR("gfx/konami_right.png");
        menuSystemInit();
        actionKeyInitSystem();
        try {
            Application.resourceInit();
        } catch (Exception e2) {
        }
        Application.getIndexIni();
        Application.runtime = Runtime.getRuntime();
        Application.loadSoundMode();
        Application.loadChunkIDFromRMS();
        try {
            Hashtable ini = Application.readIni(new DataInputStream(Application.openJar("client.ini")));
            Application.enableStreaming = ini.containsKey("streaming");
            Application.enablePreloading = ini.containsKey("preloading");
            Application.enableChunking = ini.containsKey("chunking");
        } catch (IOException e3) {
        }
        if (Application.roomImage == null) {
            Application.roomImage = Image.createImage(Application.roomWidth, Application.roomHeight);
            Application.roomGraphics = Application.roomImage.getGraphics();
        }
        Application.tickerThread = new Thread(Application.midlet);
        String remainingLanguageIds = (String) settingsHash.get("INK-Languages");
        String[] configuredLanguages = new String[50];
        int languageCount = 0;
        while (true) {
            int commaIndex = remainingLanguageIds.indexOf(44);
            if (commaIndex == -1) {
                break;
            }
            int languageIndex = languageCount;
            languageCount++;
            configuredLanguages[languageIndex] = remainingLanguageIds.substring(0, commaIndex);
            remainingLanguageIds = remainingLanguageIds.substring(commaIndex + 1);
        }
        int finalLanguageIndex = languageCount;
        int totalLanguageCount = languageCount + 1;
        configuredLanguages[finalLanguageIndex] = remainingLanguageIds;
        Application.languages = new String[totalLanguageCount];
        Application.arrayCopyString(configuredLanguages, 0, Application.languages, 0, totalLanguageCount);
        Application.language_text_ids = new int[totalLanguageCount];
        for (int languageIndex = 0; languageIndex < totalLanguageCount; languageIndex++) {
            String languageId = Application.languages[languageIndex];
            for (int i6 = 0; i6 < TextId.SETTING_TEXTS.length; i6++) {
                if (languageId.substring(0, 2).equals(TextId.SETTING_LOCALES[i6])) {
                    Application.language_text_ids[languageIndex] = TextId.SETTING_TEXTS[i6];
                    break;
                }
            }
        }
        try {
            String language = Application.getLanguage();
            if (language != null) {
                Application.loadLanguage(language);
            }
        } catch (RecordStoreException e4) {
        }
        Application.appInited = true;
        Application.appStart();
        if (MenuModel.active()) {
            return;
        }
        logoStart();
    }

    static boolean saveGameInRMS(String gameId) {
        if (SilentHillGame.gun_state && InkScript.getInventorySize("invGun") <= 0) {
            SilentHillGame.ammo_in_gun = SilentHillGame.game_ammo;
            SilentHillGame.HUD_ammoSet(0, true, true);
            SilentHillGame.gun_state = false;
        }
        if (!SilentHillGame.gun_state && InkScript.getInventorySize("invGun") > 0) {
            SilentHillGame.HUD_ammoSet(SilentHillGame.ammo_in_gun, true, true);
            SilentHillGame.gun_state = true;
        }
        if (!SilentHillGame.gun_state) {
            SilentHillGame.HUD_ammoSet(SilentHillGame.ammo_in_gun, false, true);
        }
        if (savedGameExistsInRMS(gameId)) {
            removeSavedGameFromRMS(gameId);
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(Application.inkServerVariables.size());
            Enumeration enumerationKeys = Application.inkServerVariables.keys();
            while (enumerationKeys.hasMoreElements()) {
                String variableName = (String) enumerationKeys.nextElement();
                String variableValue = Application.inkServerGetVariable(variableName);
                String variableHint = Application.inkServerGetHint(variableName);
                dataOutputStream.writeUTF(variableName);
                dataOutputStream.writeUTF(variableValue);
                dataOutputStream.writeUTF(variableHint);
            }
            for (int i = 0; i < actionKey_scriptIds.length; i++) {
                if (actionKey_scriptIds[i] == null) {
                    dataOutputStream.writeUTF("");
                } else {
                    dataOutputStream.writeUTF(actionKey_scriptIds[i]);
                }
            }
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            dataOutputStream.close();
            if (!SilentHillGame.gun_state) {
                SilentHillGame.HUD_ammoSet(0, false, true);
            }
            return Application.rmsSet(new StringBuffer().append(SAVED_GAME_RMS_PREFIX).append(gameId).toString(), byteArray);
        } catch (Exception e) {
            if (SilentHillGame.gun_state) {
                return false;
            }
            SilentHillGame.HUD_ammoSet(0, false, true);
            return false;
        }
    }

    static boolean loadGameFromRMS(String gameId) {
        try {
            FirstLoad = true;
            byte[] saveData = Application.rmsGet(new StringBuffer().append(SAVED_GAME_RMS_PREFIX).append(gameId).toString());
            if (saveData != null) {
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(saveData));
                int variableCount = input.readInt();
                for (int i2 = 0; i2 < variableCount; i2++) {
                    String variableName = input.readUTF();
                    String variableValue = input.readUTF();
                    String variableHint = input.readUTF();
                    Application.inkServerVariables.put(variableName, variableValue);
                    Application.inkServerHint.put(variableName, variableHint);
                }
                for (int i3 = 0; i3 < actionKey_scriptIds.length; i3++) {
                    String actionScriptId = input.readUTF();
                    if (actionScriptId.equals("")) {
                        actionKey_scriptIds[i3] = null;
                    } else {
                        actionKey_scriptIds[i3] = actionScriptId;
                    }
                }
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
            if (InkScript.getInventorySize("invGun") <= 0) {
                SilentHillGame.ammo_in_gun = Application.toInt(Application.inkServerGetVariable("sys_ammo"));
                SilentHillGame.game_ammo = 1;
                SilentHillGame.HUD_ammoSet(0, true, true);
            }
            SilentHillGame.gun_state = InkScript.getInventorySize("invGun") > 0;
            return true;
        } catch (IOException e2) {
            if (InkScript.getInventorySize("invGun") <= 0) {
                SilentHillGame.ammo_in_gun = Application.toInt(Application.inkServerGetVariable("sys_ammo"));
                SilentHillGame.game_ammo = 1;
                SilentHillGame.HUD_ammoSet(0, true, true);
            }
            SilentHillGame.gun_state = InkScript.getInventorySize("invGun") > 0;
            return false;
        }
    }

    static boolean savedGameExistsInRMS(String gameId) {
        return Application.rmsGet(new StringBuffer().append(SAVED_GAME_RMS_PREFIX).append(gameId).toString()) != null;
    }

    static void removeSavedGameFromRMS(String gameId) {
        Application.rmsDelete(new StringBuffer().append(SAVED_GAME_RMS_PREFIX).append(gameId).toString());
    }

    static void menuPaintWithoutSoftkeys() {
        if (MenuModel.stack.isEmpty()) {
            return;
        }
        MenuModel menu = (MenuModel) MenuModel.stack.elementAt(MenuModel.stack.size() - 1);
        menu.engineSoftkeyOptionLeft = null;
        menu.engineSoftkeyOptionRight = null;
        Application.gfx.setFont(currentFont);
        menuPaintCurrentEngine();
    }

    static void startNewGame(String gameId) {
        superBossDead = false;
        menuPaintWithoutSoftkeys();
        RoomObject.paintingAnimationTime = -1L;
        Application.tickBasedTimeReset();
        RoomObject.noVibraYet = true;
        if (savedGameExistsInRMS(gameId)) {
            removeSavedGameFromRMS(gameId);
        }
        MenuModel.closeAll();
        Application.smoothScrollDisable = true;
        Application.setupDone = false;
        SilentHillGame.cursorHintEnable = true;
        SilentHillGame.tutorialBattleState = -1;
        roomInit("setup", false);
        SilentHillGame.HUD_NESW = Application.loadImageFromJAR(new StringBuffer().append("gfx/HUD_NESW_").append(Application.curLanguageId).append(".png").toString());
    }

    static void addItemChoices(MenuModel menu, String itemID) {
        menu.addChoice(9, Application.getString(TextId.JAVA_APP_INK_LOOK_AT));
        InkScript script = (InkScript) InkScript.list.get(itemID);
        if (script != null && (script.hasEvent(InkCodes.EVENT_EQUIP) || script.hasEvent(InkCodes.EVENT_UNEQUIP))) {
            Object canEquipValue = script.executeEvent(InkCodes.EVENT_ISEQUIPABLE, null, null);
            boolean canEquip = false;
            if (canEquipValue != null) {
                canEquip = Application.toBoolean(canEquipValue);
            }
            Object equippedValue = script.executeEvent(InkCodes.EVENT_ISEQUIPED, null, null);
            boolean equipped = false;
            if (equippedValue != null) {
                equipped = Application.toBoolean(equippedValue);
            }
            if (script.hasEvent(InkCodes.EVENT_EQUIP) && canEquip && !equipped) {
                menu.addChoice(45, Application.getString(TextId.JAVA_APP_INK_EQUIP));
            }
            if (script.hasEvent(InkCodes.EVENT_UNEQUIP) && canEquip && equipped) {
                menu.addChoice(46, Application.getString(TextId.JAVA_APP_INK_UNEQUIP));
            }
        }
        if (Application.overRoomObject != null) {
            menu.addChoice(-2, Application.getString(TextId.JAVA_APP_INK_USE));
        }
    }
}
