package defpackage;

import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Named reconstruction of original class {@code Ext}. */
public class SilentHillGame extends InkEngine {
    static final int ROOMIMAGE_DEFAULT_BACKGROUND_COLOR = 16777215;
    static final int ATTACK_RANDOM_SPAN = 150;
    static final int ENEMY_MOVE_LENGTH = 25;
    static final int RECOIL_VER_LENGTH = 15;
    static final int RECOIL_HOR_LENGTH = 3;
    static final int AIM_SHAKE_LENGTH = 11;
    static int aimShake_counter;
    static int aimShake_x;
    static int aimShake_y;
    static int recoilVer;
    static int recoilHor;
    static long battleTimer;
    static boolean shot;
    static int attackAnimState;
    static final int ATTACK_ANIM_HERO = 0;
    static final int ATTACK_ANIM_1_ENEMY = 1;
    static final int ATTACK_ANIM_2_ENEMY = 2;
    static final int BATTLE_STATE_ENEMY_ONLY = 0;
    static final int BATTLE_STATE_HERO_ONLY = 1;
    static final int BATTLE_STATE_ENEMY_AND_HERO = 2;
    static boolean enemySoundPlayed;
    static int enemyState;
    static int enemyStateTime;
    static int enemyDestinationX;
    static boolean enemyForceAttack;
    static final int ENEMY_STATE_WAIT = 0;
    static final int ENEMY_STATE_ATTACK = 1;
    static final int ENEMY_STATE_SHOCK = 2;
    static final int ENEMY_STATE_MOVE_AWAY = 3;
    static final int ENEMY_STATE_DIE = 4;
    static final int ENEMY_ATTACK_TIME = 40;
    static final int ENEMY_SHOCK_TIME = 30;
    static final int ENEMY_MOVE_STEP = 3;
    static final int ENGINE_TEXT_COLOR = 0;
    static final int ENGINE_FRAME_COLOR_START = 0;
    static final int ENGINE_FRAME_COLOR_END = 0;
    static final int ENGINE_MENU_BG_COLOR = 15393458;
    static final int ENGINE_MENU_SELECTED_BG_COLOR = 8156258;
    static final int ENGINE_BG_COLOR = 0;
    static final int FS_FRAME_OFFSET = 2;
    static final int ENGINE_BOLD_LIMIT = 1000;
    static final int ENGINE_FRAME_COLOR_HEADER_SHADOW_1 = 0;
    static final int ENGINE_FRAME_COLOR_HEADER_SHADOW_2 = 0;
    static final int ENGINE_HEADER_IMAGE_HEIGHT;
    static final boolean ENGINE_HEADER_FORCE_CONSTANT_HEIGHT = true;
    static final int ENGINE_SOFTKEY_COLOR = 16777215;
    static final boolean ENGINE_SOFTKEY_BOLD = true;
    static final int ENGINE_SCROLL_BAR_COLOR_BG = 8156258;
    static final int ENGINE_SCROLL_BAR_COLOR_MARKER = 0;
    static final int POPUP_ENGINE_BG_COLOR = 15125637;
    static final int POPUP_ENGINE_BORDER_COLOR = 0;
    static final int[] engineChoiceSelectionColors;
    private static final int MENU_OMS = 50;
    private static final int CHOICE_OMS_NONE = -99;
    private static final int CHOICE_OMS_EYE = 0;
    private static final int CHOICE_OMS_HAND = 1;
    private static final int CHOICE_OMS_MOUTH = 2;
    private static final int CHOICE_OMS_INV = 3;
    private static final int OMS_INACTIVE_CHOICE = -999;
    private static int OMS_currentOption;
    private static int[] OMS_options;
    static Image INK_menu_logo;
    private static final int[] OMS_EYE_EVENTS;
    private static final int[] OMS_HAND_EVENTS;
    private static final int[] OMS_MOUTH_EVENTS;
    private static final int OMS_MARGIN = 2;
    private static final int OMS_LINE_SPACING = 4;
    private static final Font OMS_FONT;
    public static final int HUD_MARGIN = 5;
    private static int HUD_ammoNumWidth;
    private static int HUD_ammoNumHeight;
    private static int HUD_ammoNumX;
    private static int HUD_ammoNumY;
    private static int HUD_ammoNumOffset;
    private static boolean HUD_ammoUpdateNeeded;
    public static final int GAME_AMMO_MAX = 10;
    public static int game_ammo;
    public static boolean gun_state;
    public static int ammo_in_gun;
    public static int game_life;
    private static boolean game_reload;
    private static boolean noMoreBullets;
    private static int infoBarShowTime;
    private static int INFO_BAR_SHOW_TIME;
    static final String[] DEFAULT_RESOURCES;
    static final int SETUP_INDEX_HUD_BULLET = 22;
    static final int SETUP_INDEX_HUD_LIFE = 23;
    static final int SETUP_INDEX_HUD_NUMBERS = 24;
    static final int SETUP_INDEX_HUD_NUMBERS_RED = 25;
    static final int SETUP_INDEX_OMS_MENU = 26;
    static final int SETUP_INDEX_OMS_SELECTION = 27;
    static final int SETUP_INDEX_OMS_SYMBOLS = 28;
    static final int SETUP_INDEX_CROSSHAIR = 29;
    static final int SETUP_INDEX_COMPASS = 30;
    static final int SETUP_INDEX_COMPASS_POINTER_RIGHT = 31;
    static final int SETUP_INDEX_COMPASS_POINTER_UP = 32;
    static int tutorialBattleState = -1;
    static boolean cursorHintEnable = true;
    static boolean softkeyPainting = true;
    static Image HUD_NESW = null;

    public static void createEscapeMenu(boolean loadError, boolean setSaveOption) {
        InkEngine.attackAnim = false;
        if (MenuModel.active() && (MenuModel.getCurrent().ID == 17 || MenuModel.getCurrent().ID == 9 || MenuModel.getCurrent().ID == 30 || MenuModel.getCurrent().ID == SETUP_INDEX_OMS_SELECTION)) {
            return;
        }
        MenuModel createdMenu = menuCreate(17, Application.canvasWidth >> 1);
        createdMenu.setPosition(Application.canvasWidth, Application.canvasHeight);
        if (loadError) {
            createdMenu.addChoice(-2, Application.getString(TextId.JAVA_APP_INK_RELOAD));
        } else {
            createdMenu.addChoice(-1, Application.getString(TextId.JAVA_APP_INK_CONTINUE));
        }
        if (Application.saveIsPossible && setSaveOption && !InkEngine.battleMode) {
            createdMenu.addChoice(-3, Application.getString(TextId.JAVA_APP_INK_SAVE));
        }
        createdMenu.addChoice(-4, Application.getString(TextId.JAVA_APP_INK_SETTINGS));
        createdMenu.addChoice(-5, Application.getString(TextId.JAVA_APP_INK_HELP));
        createdMenu.addChoice(-6, Application.getString(TextId.JAVA_APP_INK_INGAME_MAIN_MENU));
    }

    static void battleStartInit() {
        HUD_ammoSet(Application.toInt(Application.inkServerGetVariable("sys_ammo")), false, false);
        boolean enemyPresent = false;
        for (int i = 0; Application.roomObjects != null && i < Application.roomObjects.length; i++) {
            RoomObject roomObject = Application.roomObjects[i];
            if (roomObject.visible && roomObject.script != null && roomObject.script.hasEvent(InkCodes.EVENT_GETHEROATTACKANIM1)) {
                enemyPresent = true;
                break;
            }
        }
        if (enemyPresent) {
            enemyForceAttack = false;
            enemyState = 2;
            enemyStateTime = ENEMY_ATTACK_TIME;
            Application.inkServerSetVariable("moveAnim", "0", Application.charToString('I'));
            Application.inkServerSetVariable("sys_battleState", "2", Application.charToString('I'));
            InkEngine.battleState = 2;
        } else {
            enemyState = 4;
        }
        InkEngine.battleMode = true;
        InkEngine.attackAnim = false;
        Application.roomUpdateNeeded = true;
    }

    public static boolean battleKeyHandling() {
        InkEngine.battleState = Application.toInt(Application.inkServerGetVariable("sys_battleState"));
        if (InkEngine.battleState == 0) {
            InkEngine.regPointSystemActive = false;
            return false;
        }
        if (MenuModel.active() && (MenuModel.getCurrent().ID == 17 || MenuModel.getCurrent().ID == 9 || MenuModel.getCurrent().ID == 30 || MenuModel.getCurrent().ID == SETUP_INDEX_OMS_SELECTION)) {
            InkEngine.ingameMenuKeyHandling();
            return true;
        }
        if (Application.keyNew) {
            switch (Application.keyDown) {
                case -11:
                case -8:
                case -7:
                    if (!MenuModel.active()) {
                        createEscapeMenu(false, false);
                    } else {
                        MenuModel.closeCurrent();
                    }
                    break;
                case -5:
                    if (MenuModel.active()) {
                        MenuModel.closeCurrent();
                    } else if (game_ammo > 0 && !game_reload) {
                        shot = true;
                    }
                    break;
            }
        }
        if (recoilVer != 0) {
            Application.cursorX += recoilHor;
            Application.cursorY -= recoilVer;
            recoilVer >>= 1;
            recoilVer = recoilVer <= 1 ? 0 : recoilVer;
        }
        InkEngine.ingameMoveKeyHandling();
        if (InkEngine.attackAnim || !Application.keyNew || MenuModel.active()) {
            return true;
        }
        InkEngine.actionKeyKeyHandling();
        InkEngine.battleState = Application.toInt(Application.inkServerGetVariable("sys_battleState"));
        return true;
    }

    static void enemyAnimUpdate(String move) {
        Application.inkServerSetVariable("moveAnim", "0", Application.charToString('I'));
        for (int i = 0; Application.roomObjects != null && i < Application.roomObjects.length; i++) {
            RoomObject roomObject = Application.roomObjects[i];
            if (roomObject.visible && roomObject.script != null && roomObject.script.hasEvent(InkCodes.EVENT_GETHEROATTACKANIM1)) {
                roomObject.update();
                roomObject.idleAnimationTime = 0L;
            }
        }
    }

    static void battleUpdate() {
        if (InkEngine.attackAnim && Application.roomImage != null) {
            Application.gfx.drawImage(Application.roomImage, -Application.roomScrollOffsetX, -Application.roomScrollOffsetY, 0);
        }
        if (InkEngine.battleState == 2 && !MenuModel.active() && InkScript.getInventorySize("invGun") > 0) {
            InkEngine.specialCursorIdle = GameResource.getImage("haircross", 0);
            InkEngine.hoverCursor = GameResource.getImage("jb", 0);
        }
        if (enemyState == 1 || enemyState == 2) {
            enemyStateTime--;
            if (enemyStateTime < 0) {
                if (!InkEngine.cursorAnimActive && enemyState == 2) {
                    enemyForceAttack = true;
                    enemyState = 0;
                    if (InkEngine.battleState < 2) {
                        Application.inkServerSetVariable("sys_battleState", "2", Application.charToString('I'));
                        InkEngine.battleState = 2;
                    }
                } else if (enemyState == 1) {
                    enemyDestinationX = (Application.roomWidth >> 4) + Application.random(Application.roomWidth - (Application.roomWidth >> 1));
                    enemyState = 3;
                    enemyAnimUpdate("1");
                } else if (InkEngine.battleState < 2) {
                    Application.inkServerSetVariable("sys_battleState", "2", Application.charToString('I'));
                    InkEngine.battleState = 2;
                    enemyForceAttack = true;
                    enemyState = 0;
                } else {
                    if (Application.cursorX > (Application.roomWidth >> 1)) {
                        enemyDestinationX = Application.roomWidth >> 4;
                    } else {
                        enemyDestinationX = Application.roomWidth - (Application.roomWidth >> 4);
                    }
                    enemyState = 3;
                    enemyAnimUpdate("1");
                }
            }
        }
        Application.overRoomObject = null;
        boolean skipHoverUpdate = false;
        boolean stopMoving = false;
        for (int i = 0; Application.roomObjects != null && i < Application.roomObjects.length; i++) {
            RoomObject roomObject = Application.roomObjects[i];
            if (!InkEngine.attackAnim) {
                attackAnimState = 0;
                roomObject.animationTime = 0L;
            } else if (attackAnimState == 0) {
                roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 4);
            } else if (attackAnimState == 1) {
                roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 1);
            } else {
                roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 2);
            }
            if (InkEngine.cursorAnimActive && roomObject.script != null && roomObject.script.hasEvent(InkCodes.EVENT_GETHEROATTACKANIM1) && InkEngine.battleState > 0 && enemyState == 3) {
                stopMoving = roomObject.x > enemyDestinationX ? true : true;
            }
            if (roomObject.visible && roomObject.script != null) {
                if (InkEngine.battleState != 1 && !InkEngine.attackAnim && roomObject.script.hasEvent(InkCodes.EVENT_ENEMYATTACK) && ((Application.random(ATTACK_RANDOM_SPAN) == 0 || enemyForceAttack) && ((enemyState == 1 && enemyStateTime > 0) || (enemyState == 0 && !InkEngine.cursorAnimActive)))) {
                    enemyForceAttack = false;
                    attackAnimState = 1;
                    roomObject.animationTime = 0L;
                    roomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 1);
                    roomObject.executeEvent(InkCodes.EVENT_ENEMYATTACK, null, false);
                }
                if (!skipHoverUpdate) {
                    if (InkEngine.battleState > 0) {
                        int enemyX = roomObject.x;
                        int enemyY = roomObject.y;
                        if (InkEngine.cursorAnimActive && roomObject.script.hasEvent(InkCodes.EVENT_ENEMYATTACK)) {
                            int haloHalfWidth = InkEngine.cursorAnim[InkEngine.cursorAnimFrameCounter].imageWidth >> 1;
                            int haloHalfHeight = InkEngine.cursorAnim[InkEngine.cursorAnimFrameCounter].imageHeight >> 1;
                            if (enemyX - 8 <= Application.cursorX - haloHalfWidth || enemyX + 8 >= Application.cursorX + haloHalfWidth || enemyY - 8 <= Application.cursorY - haloHalfHeight || enemyY + 8 >= Application.cursorY + haloHalfHeight) {
                                if ((enemyState == 1 || enemyState == 2) && InkEngine.battleState == 2) {
                                    enemyState = 0;
                                }
                            } else if (enemyState == 0) {
                                enemyState = 1;
                                enemyStateTime = ENEMY_ATTACK_TIME;
                            }
                        }
                        if (roomObject.script.hasEvent(InkCodes.EVENT_ATTACK1)) {
                            if (attackAnimState != 2 && enemyX - 8 < Application.cursorX && enemyX + 8 > Application.cursorX && enemyY - 8 < Application.cursorY && enemyY + 8 > Application.cursorY) {
                                Application.overRoomObject = roomObject;
                                if (InkEngine.hoveredRoomObject != roomObject && !InkScript.isWaiting()) {
                                    if (InkEngine.hoveredRoomObject != null) {
                                        InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                                        InkEngine.hoveredRoomObject = null;
                                        battleTimer = 0L;
                                    } else {
                                        InkEngine.hoveredRoomObject = roomObject.enterHover();
                                    }
                                    skipHoverUpdate = true;
                                } else if (InkEngine.hoveredRoomObject != null) {
                                    skipHoverUpdate = true;
                                }
                            } else if (InkEngine.hoveredRoomObject == roomObject) {
                                InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                                InkEngine.hoveredRoomObject = null;
                                battleTimer = 0L;
                                skipHoverUpdate = true;
                            }
                        }
                    } else if (roomObject.isOver(Application.cursorX, Application.cursorY)) {
                        Application.overRoomObject = roomObject;
                        if (InkEngine.hoveredRoomObject != roomObject && !InkScript.isWaiting()) {
                            if (InkEngine.hoveredRoomObject != null) {
                                InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                                InkEngine.hoveredRoomObject = null;
                            } else {
                                InkEngine.hoveredRoomObject = roomObject.enterHover();
                            }
                            skipHoverUpdate = true;
                        } else if (InkEngine.hoveredRoomObject != null) {
                            skipHoverUpdate = true;
                        }
                    } else if (InkEngine.hoveredRoomObject == roomObject) {
                        InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                        InkEngine.hoveredRoomObject = null;
                        skipHoverUpdate = true;
                    }
                }
                if (shot && roomObject.script.hasEvent(InkCodes.EVENT_ATTACK1) && InkEngine.hoveredRoomObject != null) {
                    InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_ATTACK1, new Integer(0), false);
                    if (Application.toBoolean(Application.inkServerGetVariable("deathAnim"))) {
                        enemyState = 4;
                        enemyAnimUpdate("0");
                        InkEngine.bossDead = true;
                        if (Application.roomGetCurrent().equals("2d")) {
                            InkEngine.superBossDead = true;
                        }
                    } else {
                        attackAnimState = 0;
                        InkEngine.hoveredRoomObject.animationTime = 0L;
                        InkEngine.hoveredRoomObject.fightAnimation(Application.gfx, Application.roomScrollOffsetX, Application.roomScrollOffsetY, 4);
                        enemyState = 2;
                        enemyStateTime = 30;
                        boolean enemyDied = Application.toBoolean(Application.inkServerGetVariable(Application.inkServerGetVariable("enemy_dead")));
                        if (InkEngine.battleState < 2 && !enemyDied) {
                            Application.inkServerSetVariable("sys_battleState", "2", Application.charToString('I'));
                        }
                    }
                }
            } else if (InkEngine.hoveredRoomObject == roomObject) {
                InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_HOVER_OUT, null, false);
                InkEngine.hoveredRoomObject = null;
            }
        }
        if (stopMoving) {
            enemyState = 0;
            enemyAnimUpdate("0");
        }
        if (shot) {
            shot = false;
            GameCanvas.playSound(Application.inkServerGetVariable("sys_gunShotSound"), 1);
            HUD_ammoSet(game_ammo - 1, true, true);
            recoilVer += RECOIL_VER_LENGTH;
            recoilHor = Application.random(2) == 0 ? -3 : 3;
        }
        InkEngine.battleState = Application.toInt(Application.inkServerGetVariable("sys_battleState"));
        if (InkEngine.battleState <= 1 || InkEngine.hoveredRoomObject == null) {
            return;
        }
        if (battleTimer > 0) {
            if (((int) (battleTimer - System.currentTimeMillis())) <= 0) {
                battleTimer = 0L;
                if (!InkEngine.attackAnim) {
                    attackAnimState = 2;
                }
            }
        } else if (battleTimer == 0) {
            battleTimer = ((long) Application.toInt(InkEngine.hoveredRoomObject.executeEvent(InkCodes.EVENT_GETTIME, new Integer(0), false))) + System.currentTimeMillis();
        }
        InkEngine.battleState = Application.toInt(Application.inkServerGetVariable("sys_battleState"));
    }

    static void appPaint() {
        int compassDirection;
        int compassX;
        int compassY;
        if (Application.appInited) {
            if (InkEngine.rgbKonamiLogo != null) {
                InkEngine.logoPaint();
                return;
            }
            if (InkEngine.splashHash != null) {
                InkEngine.splashPaint();
                return;
            }
            if (Application.mainMenuActive) {
                InkEngine.menuPaintCurrentEngine();
                if (InkEngine.popupActive) {
                    popupPaint(Application.gfx);
                    return;
                }
                return;
            }
            if ((Application.loading() && Application.loadingMode != 1) || Application.roomUpdateNeeded || Application.roomRepaintNeeded || Application.roomRepainting) {
                return;
            }
            boolean escapeMenuActive = MenuModel.active() && (MenuModel.getCurrent().ID == 17 || MenuModel.getCurrent().ID == 9 || MenuModel.getCurrent().ID == 30 || MenuModel.getCurrent().ID == SETUP_INDEX_OMS_SELECTION);
            if (!escapeMenuActive) {
                if (InkInterpreter.pausedThread == null && InkEngine.roomObjectTick != null) {
                    int ticksUntilRoomObjectUpdate = InkEngine.tickTimerUpdateInterval;
                    InkEngine.tickTimerUpdateInterval = ticksUntilRoomObjectUpdate - 1;
                    if (ticksUntilRoomObjectUpdate == 0) {
                        InkEngine.tickTimerUpdateInterval = InkEngine.EVENT_TICK_UPDATE_TIME;
                        InkEngine.roomObjectTick.executeEvent(InkCodes.EVENT_TICK, null, false);
                        Application.roomUpdateNeeded = false;
                        for (int roomObjectIndex = 0; Application.roomObjects != null && roomObjectIndex < Application.roomObjects.length; roomObjectIndex++) {
                            RoomObject roomObject = Application.roomObjects[roomObjectIndex];
                            if (roomObject.script != null && roomObject.script.hasEvent(InkCodes.EVENT_TICK)) {
                                roomObject.paint(Application.roomGraphics, 0, 0, Application.roomImage.getWidth(), Application.roomImage.getHeight());
                            }
                        }
                    }
                }
                if (!InkEngine.battleMode) {
                    InkEngine.regPointSystemActive = false;
                } else if (!(InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 2 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 4 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 5 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 7 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 9 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 10)) {
                    battleUpdate();
                }
            } else if (MenuModel.active() && MenuModel.getCurrent().ID == 17) {
                GameCanvas.stopSound();
            }
            game_life = Application.toInt(Application.inkServerGetVariable("sys_health"));
            if (InkScript.isWaiting() && !Application.firstLoopInWait) {
                Application.firstLoopInWait = false;
                return;
            }
            if (!InkEngine.attackAnim && Application.gotoDissolveFXCounter < 0) {
                InkEngine.roomPaint();
            }
            if (gun_state && InkScript.getInventorySize("invGun") <= 0) {
                ammo_in_gun = game_ammo;
                HUD_ammoSet(0, true, true);
                gun_state = false;
            }
            if (!gun_state && InkScript.getInventorySize("invGun") > 0) {
                HUD_ammoSet(ammo_in_gun, true, true);
                gun_state = true;
            }
            if (HUD_ammoUpdateNeeded && !escapeMenuActive) {
                HUD_update();
            }
            InkEngine.paintAnimCursorMask();
            if (tutorialBattleState == -1) {
                InkEngine.battleState = Application.toInt(Application.inkServerGetVariable("sys_battleState"));
                if (InkEngine.battleState == -1) {
                    tutorialBattleState = 0;
                }
            } else if (InkEngine.battleState == -1) {
                HUD_paint();
                InkEngine.battleState = Application.toInt(Application.inkServerGetVariable("sys_battleState"));
            }
            if (infoBarShowTime > 0) {
                HUD_paint();
                infoBarShowTime--;
            }
            if (InkEngine.battleState > 0) {
                HUD_paint();
                aimShake_x = Application.random(2) > 0 ? -1 : 1;
                aimShake_y = Application.random(2) > 0 ? -1 : 1;
            } else {
                aimShake_x = 0;
                aimShake_y = 0;
            }
            if (Application.enableDemoDissolve) {
                if (Application.gotoDissolveFXCounter > 0) {
                    Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
                    int tileWidth = InkEngine.systemResources[8].imageWidth;
                    int tileHeight = InkEngine.systemResources[8].imageHeight;
                    for (int tileX = 0; tileX < Application.canvasWidth; tileX += tileWidth) {
                        for (int tileY = 0; tileY < Application.canvasHeight; tileY += tileHeight) {
                            InkEngine.systemResources[8].paintSimple(Application.gfx, tileX, tileY, 0);
                        }
                    }
                } else {
                    Application.gfx.setColor(0);
                    Application.gfx.fillRect(0, 0, Application.canvasWidth, Application.canvasHeight);
                    if (Application.gotoDissolveFXCounter == -3) {
                        InkEngine.popupCreate(Application.getString(TextId.STR_DEMO_END_LEVEL), 9);
                    }
                }
                Application.repaintCanvasIfPossible();
                Application.gotoDissolveFXCounter--;
            } else if (Application.gotoDissolveFXCounter > -3) {
                if (Application.gotoDissolveFXColor == -1) {
                    Application.gfx.setColor(Application.ingameBgColor);
                } else {
                    Application.gfx.setColor(Application.gotoDissolveFXColor);
                }
                Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
                int nextDissolveFrame = Application.gotoDissolveFXCounter - 1;
                Application.gotoDissolveFXCounter = nextDissolveFrame;
                if (nextDissolveFrame >= 2 || Application.gotoDissolveFXCounter <= -2) {
                    if (Application.DEMO_END) {
                        Application.FADE_FRAMES++;
                    }
                    if (Application.FADE_FRAMES <= Application.DEMO_FRAMES) {
                        int tileWidth = InkEngine.systemResources[8].imageWidth;
                        int tileHeight = InkEngine.systemResources[8].imageHeight;
                        for (int tileX = 0; tileX < Application.canvasWidth; tileX += tileWidth) {
                            for (int tileY = 0; tileY < Application.canvasHeight; tileY += tileHeight) {
                                InkEngine.systemResources[8].paintSimple(Application.gfx, tileX, tileY, 0);
                            }
                        }
                    }
                } else {
                    if (Application.DEMO_END) {
                        Application.FADE_FRAMES++;
                    }
                    if (Application.FADE_FRAMES <= Application.DEMO_FRAMES) {
                        Application.gfx.fillRect(0, 0, Application.canvasWidth, Application.canvasHeight);
                    }
                }
            } else if (Application.dissolveFXTime > 0) {
                if (escapeMenuActive) {
                    Application.dissolveFXTime = 0L;
                    Application.dissolveFXImg = null;
                } else {
                    Application.drawDissolve();
                }
            }
            InkEngine.drawBigScreenAddOn();
            if (!InkEngine.battleMode && infoBarShowTime == 0 && ((!MenuModel.active() || MenuModel.getCurrent().ID != 3) && (compassDirection = Application.toInt(Application.inkServerGetVariable("s_compassDirection"))) != 0)) {
                if (Application.roomWidth >= Application.canvasWidth || Application.roomHeight >= Application.canvasHeight) {
                    compassX = Application.canvasWidth - 35;
                    compassY = 39;
                } else {
                    compassX = ((-Application.roomScrollOffsetX) + Application.roomWidth) - 35;
                    compassY = (-Application.roomScrollOffsetY) + 39;
                }
                InkEngine.systemResources[30].paint(Application.gfx, compassX, compassY, 0);
                switch (compassDirection) {
                    case 1:
                        InkEngine.systemResources[32].paint(Application.gfx, compassX, compassY, 0);
                        break;
                    case 2:
                        InkEngine.systemResources[SETUP_INDEX_COMPASS_POINTER_RIGHT].paint(Application.gfx, compassX, compassY, 0);
                        break;
                    case 3:
                        InkEngine.systemResources[32].paint(Application.gfx, compassX, compassY, 5);
                        break;
                    case 4:
                        InkEngine.systemResources[SETUP_INDEX_COMPASS_POINTER_RIGHT].paint(Application.gfx, compassX, compassY, 1);
                        break;
                }
                Application.gfx.drawImage(HUD_NESW, compassX, compassY, 3);
            }
            String currentRoom = Application.roomGetCurrent().toLowerCase();
            if (!MenuModel.active() || InkEngine.popupActive) {
                Application.gfx.setFont(Font.getFont(0, 1, 8));
                Application.gfx.setColor(16777215);
                if (softkeyPainting && currentRoom.indexOf("intro") == -1) {
                    if (InkEngine.popupActive) {
                        int softkeyY = Application.canvasHeight - 2;
                        Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_OK), 2 + 2, softkeyY, 32 | 4);
                    } else if (currentRoom.indexOf("floor") != -1 || currentRoom.equals("children_drawing")) {
                        String rightSoftkeyText = Application.getString(TextId.JAVA_APP_INK_BACK);
                        int rightSoftkeyX = Application.canvasWidth - 2;
                        int softkeyY = Application.canvasHeight - 2;
                        Application.gfx.drawString(rightSoftkeyText, rightSoftkeyX, softkeyY, 32 | 8);
                    } else {
                        if (currentRoom.indexOf("puzzle") != -1) {
                            if (!currentRoom.equals("safepuzzle") || !Application.toBoolean(Application.inkServerGetVariable("var_safePuzzleSolved"))) {
                                int softkeyY = Application.canvasHeight - 2;
                                Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_BACK), 2 + 2, softkeyY, 32 | 4);
                            }
                        } else if (!InkEngine.battleMode && InkScript.getInventorySize("invMap") != 0 && !InkEngine.superBossDead && (!Application.current_room_id.equals("5") || !Application.last_room_id.equals("clockPuzzle"))) {
                            int softkeyY = Application.canvasHeight - 2;
                            Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_MAP), 2 + 2, softkeyY, 32 | 4);
                        }
                        if (!Application.roomGetCurrent().equals("karen_end") && !Application.roomGetCurrent().equals("game_end") && !Application.roomGetCurrent().equals("2e") && ((!Application.current_room_id.equals("5") || !Application.last_room_id.equals("clockPuzzle")) && !InkEngine.superBossDead)) {
                            String rightSoftkeyText = Application.getString(TextId.JAVA_APP_INK_PAUSE);
                            int rightSoftkeyX = Application.canvasWidth - 2;
                            int softkeyY = Application.canvasHeight - 2;
                            Application.gfx.drawString(rightSoftkeyText, rightSoftkeyX, softkeyY, 32 | 8);
                        }
                    }
                }
                if (!Application.hideCursor && !InkEngine.popupActive) {
                    GameResource cursor = null;
                    String[] cursorHint = null;
                    int cursorDx = 0;
                    int cursorDy = 0;
                    int cursorAnchor = 0;
                    int currentFontHeight = Application.gfx.getFont().getHeight();
                    if (Application.overRoomObject == null) {
                        cursor = InkEngine.specialCursorIdle != null ? InkEngine.specialCursorIdle : InkEngine.systemResources[1];
                    } else if (InkEngine.hoverCursor != null) {
                        cursor = InkEngine.hoverCursor;
                    } else {
                        String dir = Application.overRoomObject.getMoveDir();
                        if (dir == null) {
                            cursor = InkEngine.systemResources[2];
                        } else if (dir.equals("back")) {
                            cursor = InkEngine.systemResources[3];
                            cursorHint = new String[]{Application.getString(TextId.JAVA_APP_INK_STEP_BACKWARD)};
                            cursorDy = -cursor.imageHeight;
                            cursorAnchor = 1 | 32;
                        } else if (dir.equals("left")) {
                            cursor = InkEngine.systemResources[4];
                            cursorHint = InkEngine.wrapString(Application.getString(TextId.JAVA_APP_INK_TURN_LEFT), Application.canvasWidth > Application.roomWidth ? (Application.roomWidth - Application.cursorX) - cursor.imageWidth : Application.canvasWidth - ((Application.cursorX - Application.roomScrollOffsetX) + cursor.imageWidth), Application.gfx.getFont());
                            cursorDx = cursor.imageWidth;
                            cursorDy = currentFontHeight >> 1;
                            cursorAnchor = 4 | 32;
                        } else if (dir.equals("right")) {
                            cursor = InkEngine.systemResources[5];
                            cursorHint = InkEngine.wrapString(Application.getString(TextId.JAVA_APP_INK_TURN_RIGHT), Application.canvasWidth > Application.roomWidth ? Application.cursorX - cursor.imageWidth : (Application.cursorX - Application.roomScrollOffsetX) - cursor.imageWidth, Application.gfx.getFont());
                            cursorDx = -cursor.imageWidth;
                            cursorDy = currentFontHeight >> 1;
                            cursorAnchor = 8 | 32;
                        } else if (dir.equals("forward")) {
                            cursor = InkEngine.systemResources[6];
                            cursorHint = new String[]{Application.getString(TextId.JAVA_APP_INK_MOVE_FORWARD)};
                            cursorDy = cursor.imageHeight;
                            cursorAnchor = 1 | 16;
                        }
                    }
                    if (cursor != null) {
                        cursor.paint(Application.gfx, (Application.cursorX + aimShake_x) - Application.roomScrollOffsetX, (Application.cursorY + aimShake_y) - Application.roomScrollOffsetY, 0);
                        if (cursorHint != null && cursorHintEnable) {
                            for (int hintLine = 0; hintLine < cursorHint.length; hintLine++) {
                                if ((((Application.cursorX + aimShake_x) - Application.roomScrollOffsetX) + cursorDx) - ((Application.gfx.getFont().stringWidth(cursorHint[hintLine]) / 2) - 1) < 0) {
                                    int hintY = ((Application.cursorY + aimShake_y) - Application.roomScrollOffsetY) + cursorDy + (hintLine * currentFontHeight);
                                    Application.gfx.drawString(cursorHint[hintLine], 0, hintY, 4 | 32);
                                } else if (((Application.cursorX + aimShake_x) - Application.roomScrollOffsetX) + cursorDx + (Application.gfx.getFont().stringWidth(cursorHint[hintLine]) / 2) + 1 > Application.canvasWidth) {
                                    int hintY = ((Application.cursorY + aimShake_y) - Application.roomScrollOffsetY) + cursorDy + (hintLine * currentFontHeight);
                                    Application.gfx.drawString(cursorHint[hintLine], Application.canvasWidth, hintY, 8 | 32);
                                } else {
                                    Application.gfx.drawString(cursorHint[hintLine], ((Application.cursorX + aimShake_x) - Application.roomScrollOffsetX) + cursorDx, ((Application.cursorY + aimShake_y) - Application.roomScrollOffsetY) + cursorDy + (hintLine * currentFontHeight), cursorAnchor);
                                }
                            }
                        }
                    }
                }
                if (InkEngine.popupActive) {
                    popupPaint(Application.gfx);
                }
            } else {
                if (!escapeMenuActive) {
                    InkEngine.textFadePausedTime = -1L;
                    InkEngine.textFadeCenter();
                } else if (InkEngine.textFadePausedTime != -1) {
                    for (int menuIndex = MenuModel.stack.size() - 1; menuIndex >= 0; menuIndex--) {
                        MenuModel menu = (MenuModel) MenuModel.stack.elementAt(menuIndex);
                        if (menu.ID == SETUP_INDEX_OMS_SYMBOLS) {
                            menuPaintIngame(menu);
                            break;
                        }
                    }
                    InkEngine.textFadeStartTime = System.currentTimeMillis() - InkEngine.textFadePausedTime;
                }
                InkEngine.menuPaintCurrentIngame();
                if (softkeyPainting) {
                    MenuModel current = MenuModel.getCurrent();
                    if ((current.ID == SETUP_INDEX_OMS_SYMBOLS || current.ID == 6) && (currentRoom.equals("control") || currentRoom.equals("karen_fall_sleep") || currentRoom.equals("ben_backtolight") || currentRoom.indexOf("intro") != -1 || currentRoom.indexOf("end") != -1)) {
                        Application.gfx.setFont(Font.getFont(0, 1, 8));
                        if (currentRoom.equals("control") || currentRoom.equals("game_end") || Application.canvasHeight > Application.roomHeight) {
                            Application.gfx.setColor(16777215);
                        } else {
                            Application.gfx.setColor(InkEngine.ingameTextColor);
                        }
                        Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
                        if (currentRoom.equals("game_end")) {
                            int softkeyY = Application.canvasHeight - 2;
                            Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_OK), 2 + 2, softkeyY, 32 | 4);
                        } else {
                            int softkeyY = Application.canvasHeight - 2;
                            Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_NEXT), 2 + 2, softkeyY, 32 | 4);
                            String rightSoftkeyText = Application.getString(TextId.JAVA_APP_INK_SKIP);
                            int rightSoftkeyX = Application.canvasWidth - 2;
                            Application.gfx.drawString(rightSoftkeyText, rightSoftkeyX, softkeyY, 32 | 8);
                        }
                    } else {
                        Application.gfx.setFont(Font.getFont(0, 1, 8));
                        Application.gfx.setColor(16777215);
                        if (current.ID == 6 || current.ID == 7) {
                            int softkeyY = Application.canvasHeight - 2;
                            Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_OK), 2 + 2, softkeyY, 32 | 4);
                        } else {
                            if (current.ID == 9) {
                                int softkeyY = Application.canvasHeight - 2;
                                Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_CHANGE), 2 + 2, softkeyY, 32 | 4);
                            } else if (current.ID != SETUP_INDEX_OMS_SELECTION) {
                                int softkeyY = Application.canvasHeight - 2;
                                Application.gfx.drawString(Application.getString(TextId.JAVA_APP_INK_SELECT), 2 + 2, softkeyY, 32 | 4);
                            }
                            String rightSoftkeyText = Application.getString(TextId.JAVA_APP_INK_BACK);
                            int rightSoftkeyX = Application.canvasWidth - 4;
                            int softkeyY = Application.canvasHeight - 2;
                            Application.gfx.drawString(rightSoftkeyText, rightSoftkeyX, softkeyY, 32 | 8);
                        }
                        if (current.ID == 3) {
                            HUD_paint();
                        }
                    }
                }
            }
            Application.gfx.setFont(InkEngine.currentFont);
        }
    }

    public static void mainMenuKeyHandling() {
        switch (Application.keyDown) {
            case -6:
            case -5:
                switch (Application.toInt(MenuModel.getCurrent().getChoiceID())) {
                    case 1:
                        if (Integer.parseInt(Application.upSellMode) == 1) {
                            InkEngine.createEngineFullscreenEngineMenu(Application.getString(TextId.STR_UPSELL_TEXT_NO_PUSH));
                        } else if (Integer.parseInt(Application.upSellMode) == 2) {
                            try {
                                Application.midlet.platformRequest(Application.upSellUrl);
                                break;
                            } catch (Exception e) {
                            }
                            Application.midlet.destroyApp(true);
                        }
                        break;
                    case 3:
                        if (Application.inkServerGamesSaved > 0) {
                            MenuModel createdMenu = menuCreate(10, InkEngine.engineDefaultMenuWidth);
                            createdMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
                            createdMenu.addChoice(2, Application.getString(TextId.JAVA_APP_INK_SAVED_GAME));
                            createdMenu.addChoice(13, Application.getString(TextId.JAVA_APP_INK_NEW_GAME));
                            createdMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), Application.getString(TextId.JAVA_APP_INK_BACK));
                        } else if (Application.inkServerGamesOwned <= 0) {
                            InkEngine.popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                        } else {
                            Application.resetVariableSystem();
                            if (!Application.setGameSpecificData(1)) {
                                InkEngine.popupCreate(Application.getString(TextId.JAVA_APP_INK_NO_GAMES_AVAILABLE), 0);
                                InkEngine.popupCreate("Error initiating game!!!", 0);
                            } else {
                                InkEngine.startNewGame(Application.gameId);
                            }
                        }
                        break;
                    case 4:
                        InkEngine.createSettingsMenu();
                        break;
                    case 5:
                        MenuModel secondaryMenu = menuCreate(11, InkEngine.engineDefaultMenuWidth);
                        secondaryMenu.setPosition(Application.canvasCenterX, Application.canvasCenterY);
                        secondaryMenu.addChoice(9, Application.getString(TextId.JAVA_APP_INK_GAMEPLAY));
                        secondaryMenu.addChoice(10, Application.getString(TextId.JAVA_APP_INK_CONTROLS));
                        secondaryMenu.setSoftkeyOptions(Application.getString(TextId.JAVA_APP_INK_SELECT), Application.getString(TextId.JAVA_APP_INK_BACK));
                        break;
                    case 6:
                        MenuModel.closeAll();
                        InkEngine.popupCreate(Application.getString(TextId.JAVA_APP_INK_QUIT_CONFIRMATION), 7);
                        break;
                    case 30:
                        InkEngine.createEngineFullscreenEngineMenu(new StringBuffer().append(Application.getString(TextId.JAVA_APP_INK_CREDITS1)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_ABOUT_NAME)).append(Application.appName).append("\n").append(Application.getString(TextId.JAVA_APP_INK_ABOUT_VERSION)).append(Application.appVersion).append("\n").append(Application.getString(TextId.JAVA_APP_INK_ABOUT_VENDOR)).append(Application.appVendor).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS2)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS3)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS4)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS5)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS6)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS7)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS8)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS9)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS10)).append("\n\n").append(Application.getString(TextId.JAVA_APP_INK_CREDITS11)).toString());
                        break;
                    case InkEngine.CHOICE_START_TRIAL:
                        InkEngine.createEngineFullscreenEngineMenu2(new StringBuffer().append(Application.getString(TextId.STR_DEMO_START_TRIAL)).append("\n\n").append(Application.getString(TextId.STR_DEMO_START_TRIAL_INFO)).toString());
                        break;
                    case 32:
                        if (Integer.parseInt(Application.demoMode) == 1) {
                            Application.exitTrial = true;
                            InkEngine.createEngineFullscreenEngineMenu(Application.getString(TextId.STR_DEMO_END_TEXT));
                        } else if (Integer.parseInt(Application.demoMode) == 2) {
                            try {
                                Application.midlet.platformRequest(Application.demoUrl);
                                break;
                            } catch (Exception e2) {
                            }
                            Application.midlet.destroyApp(true);
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

    public static void showMap() {
        if (InkEngine.menuInvSetup()) {
            for (int i = 0; i < InkEngine.curInvNames.length; i++) {
                if (InkEngine.curInvNames[i].equals(Application.getString(TextId.JAVA_APP_INK_MAP))) {
                    InkScript.itemID = (String) InkEngine.curInvIds[i];
                    InkScript.executeEvent(InkScript.itemID, InkCodes.EVENT_LOOKAT, (Object) null, (RoomObject) null);
                    if (Application.inkServerGetVariable("var_floor").equals("floor1")) {
                        MenuModel.getCurrent().nextChoice();
                        return;
                    } else {
                        if (Application.inkServerGetVariable("var_floor").equals("floor_base")) {
                            MenuModel.getCurrent().nextChoice();
                            MenuModel.getCurrent().nextChoice();
                            return;
                        }
                        return;
                    }
                }
            }
        }
    }

    public static void scrGoBack() {
        for (int i = 0; i < Application.roomObjects.length; i++) {
            String moveDir = Application.roomObjects[i].getMoveDir();
            if (Application.roomObjects[i].active && "back".equals(moveDir) && Application.roomObjects[i].executeEvent(InkCodes.EVENT_CLICK, new Integer(-1), false) == null) {
                return;
            }
        }
    }

    public static void ingameNoMenuKeyHandling() {
        if (Application.keyNew) {
            switch (Application.keyDown) {
                case -11:
                case -8:
                case -7:
                    String lowerCase = Application.roomGetCurrent().toLowerCase();
                    if (lowerCase.indexOf("floor") == -1 && !lowerCase.equals("children_drawing")) {
                        createEscapeMenu(false, true);
                    } else {
                        scrGoBack();
                    }
                    break;
                case -6:
                    String currentRoomId = Application.roomGetCurrent().toLowerCase();
                    if (currentRoomId.indexOf("floor") == -1 && !currentRoomId.equals("children_drawing")) {
                        if (currentRoomId.indexOf("puzzle") == -1) {
                            showMap();
                        } else {
                            scrGoBack();
                        }
                        break;
                    }
                    break;
                case -5:
                    if (Application.overRoomObject != null && !Application.hideCursor && Application.overRoomObject.executeEvent(InkCodes.EVENT_CLICK, new Integer(-1), false) != null) {
                        MenuModel createdMenu = menuCreate(MENU_OMS, Application.canvasWidth >> 1);
                        createdMenu.setPosition(Application.cursorX - Application.roomScrollOffsetX, Application.cursorY - Application.roomScrollOffsetY);
                        createdMenu.setTop(Application.overRoomObject.getName());
                        addChoices(Application.overRoomObject, createdMenu);
                        OMS_options[3] = OMS_INACTIVE_CHOICE;
                        if (Application.removeStringPrefix(Application.inkServerAllNamesWithHint(Application.charToString('V')), "inv-").length > 0) {
                            OMS_options[3] = -2;
                        }
                    }
                    break;
                case 35:
                case 42:
                    if (InkEngine.menuInvSetup()) {
                        InkEngine.createInventory(InkEngine.curInvCounter, false);
                        MenuModel.getCurrent().updateMenu = true;
                    }
                    break;
            }
        }
    }

    public static void ingameMenuKeyHandling() {
        if (MenuModel.getCurrent().ID != MENU_OMS) {
            InkEngine.ingameMenuKeyHandling();
        }
        if (Application.keyNew) {
            switch (Application.keyDown) {
                case -11:
                case -8:
                case -7:
                    MenuModel.closeCurrent();
                    break;
                case -6:
                case -5:
                    if (OMS_currentOption != CHOICE_OMS_NONE) {
                        if (OMS_currentOption != 3) {
                            if (OMS_options[OMS_currentOption] != OMS_INACTIVE_CHOICE) {
                                MenuModel.closeCurrent();
                                Application.overRoomObject.executeEvent(OMS_options[OMS_currentOption], null, false);
                            } else {
                                say(Application.useItemSayText, null, false);
                            }
                        } else if (OMS_options[OMS_currentOption] != OMS_INACTIVE_CHOICE && InkEngine.menuInvSetup()) {
                            InkEngine.createInventory(InkEngine.curInvCounter, false);
                            MenuModel.getCurrent().updateMenu = true;
                        } else {
                            say(Application.getString(TextId.JAVA_APP_INK_SH_SPECIFIC_1), null, false);
                        }
                        break;
                    }
                    break;
                case -4:
                    OMS_currentOption = 1;
                    break;
                case -3:
                    OMS_currentOption = 3;
                    break;
                case -2:
                    OMS_currentOption = 2;
                    break;
                case -1:
                    OMS_currentOption = 0;
                    break;
            }
        }
    }

    public static void addChoices(RoomObject ro, MenuModel menu) {
        OMS_options[0] = OMS_INACTIVE_CHOICE;
        for (int i = 0; i < OMS_EYE_EVENTS.length; i++) {
            if (ro.script.hasEvent(OMS_EYE_EVENTS[i])) {
                if (OMS_options[0] != OMS_INACTIVE_CHOICE) {
                    System.out.println("WARNING!!! More then one 'eye' option in OMS");
                }
                OMS_options[0] = OMS_EYE_EVENTS[i];
            }
        }
        OMS_options[1] = OMS_INACTIVE_CHOICE;
        for (int i2 = 0; i2 < OMS_HAND_EVENTS.length; i2++) {
            if (ro.script.hasEvent(OMS_HAND_EVENTS[i2])) {
                if (OMS_options[1] != OMS_INACTIVE_CHOICE) {
                    System.out.println("WARNING!!! More then one 'hand' option in OMS");
                }
                OMS_options[1] = OMS_HAND_EVENTS[i2];
            }
        }
        OMS_options[2] = OMS_INACTIVE_CHOICE;
        for (int i3 = 0; i3 < OMS_MOUTH_EVENTS.length; i3++) {
            if (ro.script.hasEvent(OMS_MOUTH_EVENTS[i3])) {
                if (OMS_options[2] != OMS_INACTIVE_CHOICE) {
                    System.out.println("WARNING!!! More then one 'mouth' option in OMS");
                }
                OMS_options[2] = OMS_MOUTH_EVENTS[i3];
            }
        }
    }

    public static void menuPaintIngame(MenuModel menu) {
        if (menu.ID == MENU_OMS) {
            menuPaintOMS(menu);
        } else if (menu.ID == 3) {
            InkEngine.inventoryMenuPaint(menu);
        } else {
            InkEngine.menuPaintIngame(menu);
        }
    }

    static void popupPaint(Graphics g) {
        int maxTextWidth = 0;
        boolean showSoftKeys = InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 2 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 4 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 5 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 7 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 9 || InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 10;
        boolean useImageBorders = (InkEngine.systemResources == null || InkEngine.systemResources[13] == null) ? false : true;
        for (int i = 0; i < InkEngine.popupText[InkEngine.popupCurrent].length; i++) {
            maxTextWidth = Application.max(maxTextWidth, InkEngine.currentFont.stringWidth(InkEngine.popupText[InkEngine.popupCurrent][i]));
        }
        int length = (InkEngine.popupText[InkEngine.popupCurrent].length * (InkEngine.engineFontHeight + 2)) - 2;
        int popupX = (Application.canvasCenterX - (maxTextWidth >> 1)) - 4;
        int popupY = (Application.canvasCenterY - (length >> 1)) - 4;
        int popupWidth = maxTextWidth + 8;
        int popupHeight = length + 8;
        g.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
        if (useImageBorders) {
            g.setColor(InkEngine.ingameBackgroundColor);
            g.fillRect(popupX, popupY, popupWidth, popupHeight);
            g.setColor(InkEngine.ingameTextColor);
            InkEngine.menuPaintIngameImageBorders(-1, popupX, popupY, popupWidth, popupHeight, false, 0, false, 0, 0);
        } else {
            g.setColor(POPUP_ENGINE_BG_COLOR);
            g.fillRect(popupX, popupY, popupWidth, popupHeight);
            g.setColor(0);
            g.drawRect(popupX, popupY, popupWidth, popupHeight);
        }
        g.setFont(InkEngine.currentFont);
        int textY = popupY + 4;
        int lineIndex = 0;
        while (lineIndex < InkEngine.popupText[InkEngine.popupCurrent].length) {
            g.drawString(InkEngine.popupText[InkEngine.popupCurrent][lineIndex], popupX + 4, textY, 20);
            lineIndex++;
            textY += InkEngine.engineFontHeight + 2;
        }
        if (showSoftKeys) {
            int softKeyBarHeight = 2 + InkEngine.engineFontHeight + 2;
            if (useImageBorders) {
                g.setColor(InkEngine.ingameBackgroundColor);
                g.fillRect(0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, softKeyBarHeight);
                g.setColor(InkEngine.ingameTextColor);
                InkEngine.menuPaintIngameImageBorders(-1, 0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, Application.canvasHeight, false, 0, false, 0, 0);
            } else {
                g.setColor(POPUP_ENGINE_BG_COLOR);
                g.fillRect(0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, softKeyBarHeight);
                g.setColor(0);
                g.drawLine(0, Application.canvasHeight - softKeyBarHeight, Application.canvasWidth, Application.canvasHeight - softKeyBarHeight);
            }
            if (InkEngine.popupRecoveryCode[InkEngine.popupCurrent] == 9) {
                g.drawString(Application.getString(TextId.JAVA_APP_INK_NEXT), 4, Application.canvasHeight - 2, 36);
                return;
            }
            if (InkEngine.popupRecoveryCode[InkEngine.popupCurrent] != 10) {
                g.drawString(Application.getString(TextId.JAVA_APP_INK_YES), 4, Application.canvasHeight - 2, 36);
                g.drawString(Application.getString(TextId.JAVA_APP_INK_NO), Application.canvasWidth - 4, Application.canvasHeight - 2, ENEMY_ATTACK_TIME);
                return;
            }
            if (Application.demoMode != null && Application.demoUrl != null && Integer.parseInt(Application.demoMode) == 2 && Application.demoUrl != null) {
                g.drawString(Application.getString(TextId.STR_DEMO_GET), 4, Application.canvasHeight - 2, 36);
            }
            g.drawString(Application.getString(TextId.JAVA_APP_INK_INGAME_MENU), Application.canvasWidth - 4, Application.canvasHeight - 2, ENEMY_ATTACK_TIME);
        }
    }

    public static void menuPaintEngine(MenuModel menu) {
        int screenX;
        int screenY;
        if (InkEngine.systemResources == null) {
            InkEngine.systemResources = new GameResource[DEFAULT_RESOURCES.length];
        }
        for (int resourceIndex = 0; resourceIndex < DEFAULT_RESOURCES.length; resourceIndex++) {
            if (InkEngine.systemResources[resourceIndex] == null && DEFAULT_RESOURCES[resourceIndex] != null && DEFAULT_RESOURCES[resourceIndex].length() > 0) {
                String resourcePath = new StringBuffer().append((String) Application.indexIniHash.get("1.datadir")).append("/gfx/transform0/").append(DEFAULT_RESOURCES[resourceIndex]).append(".png").toString();
                if (Application.resourceGet(resourcePath) != null && !Application.realizedExtras.contains(resourcePath)) {
                    InkEngine.systemResources[resourceIndex] = GameResource.getImageFromSetup(resourcePath, DEFAULT_RESOURCES[resourceIndex]);
                    Application.realizedExtras.addElement(resourcePath);
                }
                InkEngine.updateSystemResourceValues(resourceIndex);
            }
        }
        int totalHeight = 0;
        int textY = 4;
        int maxTextWidth = 0;
        int textHeight = 0;
        int choicesHeight = 0;
        int softKeyBarHeight = Font.getFont(0, 1, 8).getHeight() + 8;
        if (menu.topText != null) {
            if (menu.updateTopLines) {
                menu.updateTopLines = false;
                menu.topLines = InkEngine.wrapString(menu.topText, menu.maxTextWidth - InkEngine.ingameBorderSizeRight);
            }
            int numberOfTopLines = menu.topLines.length;
            for (int lineIndex = 0; lineIndex < numberOfTopLines; lineIndex++) {
                maxTextWidth = Application.max(maxTextWidth, InkEngine.currentFont.stringWidth(menu.topLines[lineIndex]));
            }
            textHeight = (menu.engineFullScreenScroll && menu.choiceIDs.isEmpty()) ? ((menu.topLines.length + menu.scroll) * (InkEngine.engineFontHeight + 2)) - 2 : (menu.topLines.length * (InkEngine.engineFontHeight + 2)) - 2;
            totalHeight = textHeight + 8;
        }
        Vector choiceLines = null;
        Vector choiceLineBackgroundColors = null;
        int choicesY = 0 + 4 + textHeight;
        if (!menu.choiceIDs.isEmpty()) {
            int choiceCount = menu.countChoices();
            choiceLines = new Vector(choiceCount);
            choiceLineBackgroundColors = new Vector(choiceCount);
            int hasChoice = menu.ID == 2 ? 1 : 0;
            if (menu.updateBodyLines) {
                menu.updateBodyLines = false;
                menu.bodyLines = new String[choiceCount][];
            }
            int choiceIndex = (-menu.scroll) * hasChoice;
            while (choiceIndex < choiceCount) {
                menu.choiceIDs.elementAt(choiceIndex);
                String choiceText = (String) menu.choiceTexts.elementAt(choiceIndex);
                Integer backgroundColor = (choiceIndex != menu.selectedChoiceNr || menu.engineFullScreenScroll) ? new Integer(ENGINE_MENU_BG_COLOR) : new Integer(8156258);
                if (menu.bodyLines[choiceIndex] == null) {
                    menu.bodyLines[choiceIndex] = InkEngine.wrapString(choiceText, menu.maxTextWidth);
                }
                int choiceLineCount = menu.bodyLines[choiceIndex].length;
                for (int lineIndex = menu.engineFullScreenScroll ? -menu.scroll : 0; lineIndex < choiceLineCount; lineIndex++) {
                    choiceLines.addElement(menu.bodyLines[choiceIndex][lineIndex]);
                    choiceLineBackgroundColors.addElement(backgroundColor);
                    maxTextWidth = Application.max(maxTextWidth, InkEngine.currentFont.stringWidth(menu.bodyLines[choiceIndex][lineIndex]));
                }
                choiceIndex++;
            }
            choicesHeight = (choiceLines.size() * (InkEngine.engineFontHeight + 2)) - 2;
            totalHeight += choicesHeight + 8;
        }
        int totalWidth = maxTextWidth + 8;
        if (textHeight > 0 && choicesHeight > 0) {
            choicesY += 10;
            totalHeight += 2;
        }
        if (menu.engineFullScreenScroll) {
            screenX = menu.x + InkEngine.ingameBorderSizeRight;
            textY = 9;
            screenY = menu.y;
        } else {
            int centeredX = menu.x - (totalWidth >> 1);
            int centeredY = menu.y - (totalHeight >> 1);
            int nonNegativeX = Application.max(centeredX, 0);
            int nonNegativeY = Application.max(centeredY, 0);
            screenX = Application.min(nonNegativeX, Application.canvasWidth - totalWidth);
            screenY = Application.min(nonNegativeY, Application.canvasHeight - totalHeight);
        }
        Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
        Application.gfx.drawImage(INK_menu_logo, Application.canvasCenterX, InkEngine.engineHeaderImageHeight + 2, 16 | 1);
        Application.gfx.setColor(ENGINE_MENU_BG_COLOR);
        if (menu.engineFullScreenScroll) {
            Application.gfx.fillRect(screenX, screenY + InkEngine.ingameBorderSizeTop, (Application.canvasWidth - InkEngine.ingameBorderSizeRight) - InkEngine.ingameBorderSizeLeft, ((((Application.canvasHeight - InkEngine.ingameBorderSizeTop) - InkEngine.ingameBorderSizeBottom) - InkEngine.engineHeaderImageHeight) - InkEngine.engineFooterImageHeight) + 2);
            InkEngine.menuPaintIngameImageBorders(-1, screenX, screenY + InkEngine.ingameBorderSizeTop, (Application.canvasWidth - InkEngine.ingameBorderSizeRight) - InkEngine.ingameBorderSizeLeft, ((((Application.canvasHeight - InkEngine.ingameBorderSizeTop) - InkEngine.ingameBorderSizeBottom) - InkEngine.engineHeaderImageHeight) - InkEngine.engineFooterImageHeight) + 2, false, 0, false, 0, 0);
        } else {
            Application.gfx.fillRect(screenX, screenY, totalWidth, totalHeight);
            InkEngine.menuPaintIngameImageBorders(-1, screenX, screenY, totalWidth, totalHeight, false, 0, false, 0, 0);
        }
        if (menu.topText != null) {
            int numberOfTopLines = menu.topLines.length;
            if (menu.engineFullScreenScroll) {
                int hasChoice = menu.choiceIDs.isEmpty() ? 1 : 0;
                Application.gfx.setColor(0);
                for (int lineIndex = (-menu.scroll) * hasChoice; lineIndex < numberOfTopLines; lineIndex++) {
                    Application.gfx.drawString(menu.topLines[lineIndex], screenX + 4, screenY + textY, 0);
                    textY += InkEngine.engineFontHeight + 2;
                    if (((screenY + textY) - 2) + InkEngine.engineFontHeight > (Application.canvasHeight - softKeyBarHeight) - InkEngine.ingameBorderSizeBottom) {
                        break;
                    }
                }
                if (menu.engineNumOfLinesShownMax == -1) {
                    menu.engineNumOfLinesShownMax = (((((Application.canvasHeight - screenY) - 4) - 4) - InkEngine.engineFooterImageHeight) + 2) / (InkEngine.engineFontHeight + 2);
                }
                if (menu.choiceIDs.isEmpty() && numberOfTopLines > menu.engineNumOfLinesShownMax) {
                    if (menu.engineScrollBarMarkerHeight == -1) {
                        menu.engineScrollBarMarkerHeight = ((InkEngine.engineScrollBarHeight * menu.engineNumOfLinesShownMax) / numberOfTopLines) + 2;
                    }
                    InkEngine.menuEngineDrawScrollBar(menu.engineScrollBarMarkerHeight, menu.scroll == 0 ? 0 : ((InkEngine.engineScrollBarHeight - menu.engineScrollBarMarkerHeight) * (-menu.scroll)) / (numberOfTopLines - menu.engineNumOfLinesShownMax));
                }
            } else {
                Application.gfx.setColor(0);
                for (int lineIndex = 0; lineIndex < numberOfTopLines; lineIndex++) {
                    Application.gfx.drawString(menu.topLines[lineIndex], screenX + 4, screenY + textY, 0);
                    textY += InkEngine.engineFontHeight + 2;
                    if (((screenY + textY) - 2) + InkEngine.engineFontHeight > (Application.canvasHeight - softKeyBarHeight) - InkEngine.ingameBorderSizeBottom) {
                        break;
                    }
                }
            }
        }
        if (!menu.choiceIDs.isEmpty()) {
            int numberOfChoiceLines = choiceLines.size();
            for (int lineIndex = 0; lineIndex < numberOfChoiceLines; lineIndex++) {
                Application.gfx.setColor(Application.toInt(choiceLineBackgroundColors.elementAt(lineIndex)));
                Application.gfx.fillRect(screenX, screenY + choicesY, totalWidth, InkEngine.engineFontHeight);
                Application.gfx.setColor(0);
                String line = (String) choiceLines.elementAt(lineIndex);
                if (menu.engineFullScreenScroll) {
                    Application.gfx.drawString(line, screenX + 4, screenY + choicesY, 0);
                } else {
                    Application.gfx.drawString(line, screenX + 4, screenY + choicesY + InkEngine.txtOffsetMenu, 0);
                }
                choicesY += InkEngine.engineFontHeight + 2;
            }
        }
        if (menu.engineFullScreenScroll) {
            if (menu.choiceIDs.isEmpty()) {
                if (textHeight < (((Application.canvasHeight - screenY) - 4) - 4) - InkEngine.engineFooterImageHeight) {
                    menu.textScrolling = false;
                }
            } else if (choicesHeight < (((Application.canvasHeight - (screenY + textHeight)) - 4) - 4) - InkEngine.engineFooterImageHeight) {
                menu.textScrolling = false;
            }
        }
        if (!menu.engineFullScreenScroll || Application.tickCounter <= 10) {
            return;
        }
        int scrollArrowX = (Application.canvasWidth - InkEngine.ingameBorderSizeLeft) - InkEngine.systemResources[9].imageWidth;
        int cornerHeight = InkEngine.systemResources[18].imageHeight + InkEngine.ingameMargin;
        int availableHeight = (Application.canvasHeight - menu.screenY) - softKeyBarHeight;
        if (menu.scroll < 0) {
            InkEngine.systemResources[9].paintSimple(Application.gfx, screenX + scrollArrowX, screenY + cornerHeight, 20);
        }
        if (totalHeight > (Application.canvasHeight - menu.y) - softKeyBarHeight) {
            InkEngine.systemResources[10].paintSimple(Application.gfx, screenX + scrollArrowX, ((screenY + availableHeight) - InkEngine.ingameMargin) - cornerHeight, 36);
        }
    }

    static MenuModel menuCreate(int menuId, int maxTextWidth) {
        Application.last_room_id = "0000";
        if (menuId == 20 || menuId == SETUP_INDEX_CROSSHAIR) {
            Application.stopKeyHandling = false;
        }
        Application.timeStopKeyHandling = System.currentTimeMillis();
        if (menuId == MENU_OMS) {
            OMS_currentOption = 0;
        }
        return InkEngine.menuCreate(menuId, maxTextWidth);
    }

    private static void menuUpdateOMS(MenuModel menu) {
        menu.totalWidth = 0;
        menu.totalHeight = 2;
        menu.textX = 2;
        menu.textY = 2;
        menu.textWidth = 0;
        menu.textHeight = 0;
        if (menu.topText != null) {
            if (menu.updateTopLines) {
                menu.updateTopLines = false;
                menu.topLines = InkEngine.wrapString(menu.topText, Application.canvasWidth, OMS_FONT);
            }
            int length = menu.topLines.length;
            for (int i = 0; i < length; i++) {
                menu.textWidth = Application.max(menu.textWidth, OMS_FONT.stringWidth(menu.topLines[i]));
            }
            menu.textHeight = (menu.topLines.length * (InkEngine.ingameFontHeight + 4)) - 4;
            menu.totalHeight += menu.textHeight + 4;
        }
        menu.totalHeight += InkEngine.systemResources[SETUP_INDEX_OMS_MENU].imageHeight + 2;
        menu.totalWidth = Application.max(menu.textWidth, InkEngine.systemResources[SETUP_INDEX_OMS_MENU].imageWidth) + 4;
        menu.screenX = menu.x - (menu.totalWidth >> 1);
        menu.screenY = menu.y - (menu.totalHeight >> 1);
        menu.screenX = Application.max(menu.screenX, 0);
        menu.screenY = Application.max(menu.screenY, 2);
        int height = Font.getFont(0, 1, 8).getHeight() + 8;
        menu.screenX = Application.min(menu.screenX, Application.canvasWidth - menu.totalWidth);
        menu.screenY = Application.min(menu.screenY, (Application.canvasHeight - menu.totalHeight) - height);
    }

    private static void menuPaintOMS(MenuModel menu) {
        if (menu.updateMenu) {
            menuUpdateOMS(menu);
            menu.updateMenu = false;
        }
        int menuCenterX = menu.screenX + (menu.totalWidth >> 1);
        Application.gfx.setFont(OMS_FONT);
        int textY = menu.screenY + 2;
        for (int lineIndex = 0; lineIndex < menu.topLines.length; lineIndex++) {
            Application.gfx.setColor(0);
            Application.gfx.drawString(menu.topLines[lineIndex], menuCenterX + 1, textY, 17);
            Application.gfx.drawString(menu.topLines[lineIndex], menuCenterX - 1, textY, 17);
            Application.gfx.drawString(menu.topLines[lineIndex], menuCenterX, textY + 1, 17);
            Application.gfx.drawString(menu.topLines[lineIndex], menuCenterX, textY - 1, 17);
            Application.gfx.setColor(16777215);
            Application.gfx.drawString(menu.topLines[lineIndex], menuCenterX, textY, 17);
            textY += InkEngine.ingameFontHeight + InkEngine.ingameLineSpacing;
        }
        Application.gfx.setFont(InkEngine.currentFont);
        int graphicsCenterY = menu.screenY + 2 + menu.textHeight + 4 + (InkEngine.systemResources[SETUP_INDEX_OMS_MENU].imageHeight >> 1);
        InkEngine.systemResources[SETUP_INDEX_OMS_MENU].paintSimple(Application.gfx, menuCenterX, graphicsCenterY, 3);
        if (OMS_currentOption != CHOICE_OMS_NONE) {
            int selectionX = menuCenterX;
            int selectionY = graphicsCenterY;
            switch (OMS_currentOption) {
                case 0:
                    selectionX++;
                    selectionY -= 20;
                    break;
                case 1:
                    selectionX += 21;
                    selectionY--;
                    break;
                case 2:
                    selectionY += 18;
                    break;
                case 3:
                    selectionX -= 20;
                    selectionY--;
                    break;
            }
            InkEngine.systemResources[SETUP_INDEX_OMS_SELECTION].paintSimple(Application.gfx, selectionX, selectionY, 3);
        }
        InkEngine.systemResources[SETUP_INDEX_OMS_SYMBOLS].paintSimple(Application.gfx, menuCenterX + 1, graphicsCenterY, 3);
    }

    static void say(String text, String header, boolean useFullScreen) {
        int menuY;
        int headerlessMenuY;
        int totalBorderWidth = InkEngine.ingameUseImageBorders ? InkEngine.ingameBorderSizeLeft + InkEngine.ingameBorderSizeRight : InkEngine.ingameBorderSize << 1;
        InkEngine.wrapString(text, (Application.max(Application.canvasWidth - ((InkEngine.ingameMargin << 1) + totalBorderWidth), InkEngine.INGAME_MENU_TEXT_WIDTH_MIN) - InkEngine.ingameMargin) + InkEngine.ingameScrollArrowsWidth);
        MenuModel menu = menuCreate(useFullScreen ? SETUP_INDEX_OMS_SELECTION : 6, Application.canvasWidth - ((InkEngine.ingameMargin << 1) + totalBorderWidth));
        if (header == null) {
            menu.setTop(text);
            int menuX = InkEngine.ingameUseImageBorders ? InkEngine.ingameBorderSizeLeft : InkEngine.ingameBorderSize;
            if (useFullScreen) {
                headerlessMenuY = (InkEngine.ingameUseImageBorders ? InkEngine.ingameBorderSizeTop : InkEngine.ingameBorderSize) + 1;
            } else {
                headerlessMenuY = (Application.canvasHeight - ((InkEngine.ingameFontHeight + InkEngine.ingameLineSpacing) << 2)) - (InkEngine.ingameFontHeight >> 1);
            }
            menu.setPosition(menuX, headerlessMenuY);
            return;
        }
        menu.setTop(header);
        menu.addChoice(1, text);
        int menuX = InkEngine.ingameUseImageBorders ? InkEngine.ingameBorderSizeLeft : InkEngine.ingameBorderSize;
        if (useFullScreen) {
            menuY = (InkEngine.ingameUseImageBorders ? InkEngine.ingameBorderSizeTop : InkEngine.ingameBorderSize) + 1;
        } else {
            menuY = (((Application.canvasHeight - ((InkEngine.ingameFontHeight + InkEngine.ingameLineSpacing) << 2)) - (InkEngine.ingameFontHeight >> 1)) - (InkEngine.ingameUseImageBorders ? InkEngine.ingameDelimiterHeight : 2)) - (InkEngine.ingameMargin << 1);
        }
        menu.setPosition(menuX, menuY);
    }

    private static void HUD_paint() {
        if (Application.roomWidth >= Application.canvasWidth || Application.roomHeight >= Application.canvasHeight) {
            InkEngine.systemResources[SETUP_INDEX_HUD_BULLET].paintSimple(Application.gfx, 5, 9, 20);
        } else {
            InkEngine.systemResources[SETUP_INDEX_HUD_BULLET].paintSimple(Application.gfx, (-Application.roomScrollOffsetX) + 5, (-Application.roomScrollOffsetY) + 9, 20);
        }
        int ammoNumbersY = HUD_ammoNumY + HUD_ammoNumHeight + HUD_ammoNumOffset;
        if (Application.roomWidth >= Application.canvasWidth || Application.roomHeight >= Application.canvasHeight) {
            Application.gfx.setClip(HUD_ammoNumX, HUD_ammoNumY, HUD_ammoNumWidth, HUD_ammoNumHeight);
            if (game_reload) {
                InkEngine.systemResources[25].paintSimple(Application.gfx, HUD_ammoNumX, ammoNumbersY, 36);
            } else {
                InkEngine.systemResources[SETUP_INDEX_HUD_NUMBERS].paintSimple(Application.gfx, HUD_ammoNumX, ammoNumbersY, 36);
            }
        } else {
            Application.gfx.setClip((-Application.roomScrollOffsetX) + HUD_ammoNumX, (-Application.roomScrollOffsetY) + HUD_ammoNumY, HUD_ammoNumWidth, HUD_ammoNumHeight);
            if (game_reload) {
                InkEngine.systemResources[25].paintSimple(Application.gfx, (-Application.roomScrollOffsetX) + HUD_ammoNumX, (-Application.roomScrollOffsetY) + ammoNumbersY, 36);
            } else {
                InkEngine.systemResources[SETUP_INDEX_HUD_NUMBERS].paintSimple(Application.gfx, (-Application.roomScrollOffsetX) + HUD_ammoNumX, (-Application.roomScrollOffsetY) + ammoNumbersY, 36);
            }
        }
        int lifeWidth = game_life * 6;
        if (Application.roomWidth >= Application.canvasWidth || Application.roomHeight >= Application.canvasHeight) {
            Application.gfx.setClip((Application.canvasWidth - 5) - lifeWidth, 5, lifeWidth, InkEngine.systemResources[SETUP_INDEX_HUD_LIFE].imageHeight);
            InkEngine.systemResources[SETUP_INDEX_HUD_LIFE].paintSimple(Application.gfx, Application.canvasWidth - 5, 5, SETUP_INDEX_HUD_NUMBERS);
        } else {
            Application.gfx.setClip((((-Application.roomScrollOffsetX) + Application.roomWidth) - 5) - lifeWidth, (-Application.roomScrollOffsetY) + 5, lifeWidth, InkEngine.systemResources[SETUP_INDEX_HUD_LIFE].imageHeight);
            InkEngine.systemResources[SETUP_INDEX_HUD_LIFE].paintSimple(Application.gfx, ((-Application.roomScrollOffsetX) + Application.roomWidth) - 5, (-Application.roomScrollOffsetY) + 5, SETUP_INDEX_HUD_NUMBERS);
        }
        Application.gfx.setClip(0, 0, Application.canvasWidth, Application.canvasHeight);
    }

    private static void HUD_update() {
        if (HUD_ammoNumWidth == -1 && InkEngine.systemResources[SETUP_INDEX_HUD_NUMBERS] != null) {
            HUD_ammoNumHeight = InkEngine.systemResources[SETUP_INDEX_HUD_NUMBERS].imageHeight / 11;
            int heightDifference = InkEngine.systemResources[SETUP_INDEX_HUD_BULLET].imageHeight - HUD_ammoNumHeight;
            HUD_ammoNumY = 9 + (heightDifference == 0 ? 0 : heightDifference >> 1);
            HUD_ammoNumX = 5 + InkEngine.systemResources[SETUP_INDEX_HUD_BULLET].imageWidth + 3;
            HUD_ammoNumWidth = InkEngine.systemResources[SETUP_INDEX_HUD_NUMBERS].imageWidth;
            HUD_ammoSet(Application.toInt(Application.inkServerGetVariable("sys_ammo")), false, false);
        }
        int targetAmmoOffset = game_ammo * HUD_ammoNumHeight;
        if (HUD_ammoNumOffset != targetAmmoOffset) {
            if (HUD_ammoNumOffset < targetAmmoOffset) {
                HUD_ammoNumOffset++;
            } else {
                HUD_ammoNumOffset--;
            }
            HUD_ammoUpdateNeeded = true;
            return;
        }
        if (gun_state && InkScript.getInventorySize("invGun") <= 0) {
            ammo_in_gun = game_ammo;
            HUD_ammoSet(0, true, true);
            gun_state = false;
        }
        if (!gun_state && InkScript.getInventorySize("invGun") > 0) {
            HUD_ammoSet(ammo_in_gun, true, true);
            gun_state = true;
        }
        if (game_ammo != 0 || InkScript.getInventorySize("invGun") <= 0) {
            HUD_ammoUpdateNeeded = false;
            game_reload = false;
            return;
        }
        int inventorySize = InkScript.getInventorySize("invBulletClips");
        if (inventorySize > 0) {
            int ammoLoaded = Application.min(inventorySize, 10);
            HUD_ammoSet(ammoLoaded, true, true);
            int remainingClips = inventorySize - ammoLoaded;
            if (remainingClips <= 0) {
                InkScript.removeInventory("invBulletClips");
            } else {
                InkScript.setInventory("invBulletClips", remainingClips);
            }
            game_reload = true;
            return;
        }
        if (noMoreBullets) {
            return;
        }
        noMoreBullets = true;
        if (InkEngine.bossDead && Application.roomGetCurrent().equals("2d")) {
            return;
        }
        say(Application.getString(TextId.JAVA_APP_INK_NO_MORE_BULLETS), null, false);
    }

    public static void HUD_ammoSet(int newAmmo, boolean useScroll, boolean setAmmoVar) {
        if (newAmmo < 0) {
            newAmmo = 0;
        }
        if (game_ammo != newAmmo) {
            game_ammo = newAmmo;
            if (!useScroll) {
                HUD_ammoNumOffset = game_ammo * HUD_ammoNumHeight;
            }
            if (setAmmoVar) {
                Application.inkServerSetVariable("sys_ammo", new StringBuffer().append("").append(game_ammo).toString(), Application.charToString('I'));
            }
            HUD_ammoUpdateNeeded = true;
        }
    }

    public static void menuResetIngameValues() {
        InkEngine.menuResetIngameValues();
        HUD_ammoNumWidth = -1;
        HUD_ammoUpdateNeeded = true;
    }

    static void addItemChoices(MenuModel menu, String itemID) {
        String moveDir;
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
                if (itemID.equals("invBulletClips")) {
                    menu.addChoice(45, Application.getString(TextId.JAVA_APP_INK_SH_SPECIFIC_2));
                } else if (itemID.equals("invGun")) {
                    menu.addChoice(45, Application.getString(TextId.JAVA_APP_INK_EQUIP));
                } else {
                    menu.addChoice(45, Application.getString(TextId.JAVA_APP_INK_SH_SPECIFIC_3));
                }
            }
            if (script.hasEvent(InkCodes.EVENT_UNEQUIP) && canEquip && equipped) {
                menu.addChoice(46, Application.getString(TextId.JAVA_APP_INK_UNEQUIP));
            }
        }
        if (Application.overRoomObject != null && ((moveDir = Application.overRoomObject.getMoveDir()) == null || (!moveDir.equals("back") && !moveDir.equals("right") && !moveDir.equals("left") && !moveDir.equals("forward")))) {
            menu.addChoice(-2, Application.getString(TextId.JAVA_APP_INK_USE));
        }
        menu.addChoice(9, Application.getString(TextId.JAVA_APP_INK_LOOK_AT));
    }

    public static void inventoryEquipUnequipHandling(int choiceID) {
        MenuModel.closeAll();
        int previousAmmo = game_ammo;
        InkScript.executeEvent(InkScript.itemID, choiceID, (Object) null, (RoomObject) null);
        infoBarShowTime = INFO_BAR_SHOW_TIME / MENU_OMS;
        int newAmmo = Application.toInt(Application.inkServerGetVariable("sys_ammo"));
        if (newAmmo != previousAmmo) {
            HUD_ammoSet(newAmmo, false, false);
        }
    }

    static void executeCommand_inventoryAdd(InkInterpreter scrThread, String id, int amount, boolean showRevievedMenu) {
        if (id.equals("invBulletClips")) {
            if (gun_state && InkScript.getInventorySize("invGun") <= 0) {
                ammo_in_gun = game_ammo;
                HUD_ammoSet(0, true, true);
                gun_state = false;
            }
            if (!gun_state && InkScript.getInventorySize("invGun") > 0) {
                HUD_ammoSet(ammo_in_gun, true, true);
                gun_state = true;
            }
            if (game_ammo == 0 && InkScript.getInventorySize(id) == 0 && InkScript.getInventorySize("invGun") > 0) {
                int ammoLoaded = Application.min(amount, 10);
                HUD_ammoSet(ammoLoaded, true, true);
                game_reload = true;
                amount -= ammoLoaded;
            }
            noMoreBullets = false;
        }
        int inventorySize = InkScript.getInventorySize(id) + amount;
        if (inventorySize > 0) {
            InkScript.setInventory(id, inventorySize);
        }
        if (!Application.mainMenuActive) {
            MenuModel.closeAll();
        }
        Application.roomUpdateNeeded = true;
        if (showRevievedMenu) {
            if (amount > 1) {
                InkEngine.inventoryAdd(id, amount);
            } else {
                InkEngine.inventoryAdd(id, -1);
            }
            scrThread.status = 4;
        }
    }

    static void appInit() {
        INK_menu_logo = Application.loadImageFromJAR("gfx/menu_logo.png");
        InkEngine.appInit();
    }

    static {
        ENGINE_HEADER_IMAGE_HEIGHT = Application.canvasHeight < 200 ? 5 : 19;
        engineChoiceSelectionColors = new int[]{80657596, 12105655, 10197915, 0, 10197915, 12105655};
        OMS_options = new int[]{OMS_INACTIVE_CHOICE, OMS_INACTIVE_CHOICE, OMS_INACTIVE_CHOICE, OMS_INACTIVE_CHOICE};
        OMS_EYE_EVENTS = new int[]{InkCodes.EVENT_LOOKAT};
        OMS_HAND_EVENTS = new int[]{InkCodes.EVENT_USE, InkCodes.EVENT_PICKUP, InkCodes.EVENT_PULL, InkCodes.EVENT_OPEN, InkCodes.EVENT_CLOSE};
        OMS_MOUTH_EVENTS = new int[]{InkCodes.EVENT_TALKTO};
        OMS_FONT = Font.getFont(0, 1, 8);
        HUD_ammoUpdateNeeded = true;
        gun_state = false;
        game_reload = true;
        noMoreBullets = false;
        infoBarShowTime = 0;
        INFO_BAR_SHOW_TIME = 2000;
        DEFAULT_RESOURCES = new String[]{null, "cursorIdle", "cursorPoint", "cursorBack", "cursorForwardLeft", "cursorForwardRight", "cursorForward", "cursorMenu", "dissolveEffectImg", "arrow_up", "arrow_down", "inv_arrowLeft", "inv_arrowRight", "border_top", "border_bottom", "border_left", "border_right", "corner_top_left", "corner_top_right", "corner_bottom_left", "corner_bottom_right", "delimiter", "HUD_bullet", "HUD_life", "HUD_numbers", "HUD_numbersRed", "OMS_menu", "OMS_selected", "OMS_symbols", "haircross", "HUD_compass", "HUD_pointerRight", "HUD_pointerUp"};
    }
}
