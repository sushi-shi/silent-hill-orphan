package defpackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

/** Named reconstruction of original class {@code M}. */
public class Application extends MIDlet implements Runnable {
    static long time_start2;
    static Application midlet;
    static Canvas canvas;
    static int canvasWidth;
    static int canvasHeight;
    static int canvasCenterX;
    static int canvasCenterY;
    static boolean hiddenCanvas;
    static Thread tickerThread;
    static boolean tickerGo;
    static byte tickCounter;
    static final int MILLIS_PER_TICK = 50;
    static final int SMOOTH_SCROLL_DIVIDER = 5;
    static final int SHIFT_ARGUMENTS = 6;
    static final int MASK_COMMAND = 63;
    static final int TRANSFORM_NONE = 0;
    static final int TRANSFORM_FLIPH = 1;
    static final int TRANSFORM_ROTATE90 = 2;
    static final int TRANSFORM_ROTATE180 = 4;
    static final int TRANSFORM_ROTATE270 = 6;
    static final int TRANSFORM_FLIPH_ROTATE90 = 3;
    static final int TRANSFORM_FLIPH_ROTATE180 = 5;
    static final int TRANSFORM_FLIPH_ROTATE270 = 7;
    static final int CURSOR_ACCELLERATION_MAX = 5;
    static final int TOPLEFT = 20;
    static final int TRUE = 1;
    static final int FALSE = 0;
    public static int ingameBgColor;
    public static String[] languages;
    static Graphics gfx;
    static int keyLastPressed;
    static boolean keyNew;
    static boolean keyPressed;
    static boolean appInited;
    static String appName;
    static String appVersion;
    static String appVendor;
    static String upSellMode;
    static String upSellUrl;
    static String demoMode;
    static String demoUrl;
    static boolean roomUpdateNeeded;
    static boolean roomRepaintNeeded;
    static boolean setupDone;
    static boolean saveIsPossible;
    static boolean nextRoomInit;
    static RoomObject[] roomObjects;
    static Image roomImage;
    static Graphics roomGraphics;
    static boolean smoothScrollDisable;
    static boolean dissolveFXEnabled;
    static Image dissolveFXImg;
    static long dissolveFXTime;
    static int dissolveFXImgTimer;
    static int dissolveFXColorTimer;
    static int dissolveFXLoops;
    static int[][] bloodEffectX;
    static int[][] bloodEffectY;
    static byte bloodEffectPhase;
    static final byte BLOOD_COLLECTION_AMOUNT = 3;
    static final byte BLOOD_IMAGES_AMOUNT = 5;
    static Image bloodEffectImg1;
    static Image bloodEffectImg2;
    static int bloodEffectSprayX1;
    static int bloodEffectSprayY1;
    static int bloodEffectSprayX2;
    static int bloodEffectSprayY2;
    static int bloodEffectBlur;
    static int bloodEffectShift;
    static boolean gotoDissolveFXEnabled;
    static boolean gotoDissolveFXIsSet;
    static final int GOTO_DISSOLVE_FX_FRAMES = 6;
    static int roomScrollOffsetX;
    static int roomScrollOffsetY;
    static int cursor_OLD_X;
    static int cursor_OLD_Y;
    static int cursorSpeedX;
    static int cursorSpeedY;
    static RoomObject overRoomObject;
    static boolean roomEntered;
    static boolean gameChangedSinceLastSave;
    static boolean firstLoopInWait;
    static int previousTextInputMenuChoice;
    public static boolean endGame;
    public static int reloadCounter;
    public static final int MAX_RELOADS = 3;
    static Thread roomRepaintThread;
    static boolean roomRepainting;
    static Thread loadThread;
    public static Vector loadedChunksID;
    static boolean loadingChunk;
    static String chunkID;
    static String selectedRoom;
    static final int LOADING_MODE_ROOM = 0;
    static final int LOADING_MODE_PRELOADING = 1;
    static final int LOADING_MODE_CHUNK = 2;
    static final int LOADING_MODE_INV = 3;
    static boolean loadBarActive;
    static final char INK_SERVER_HINT_STRING = 'S';
    static final char INK_SERVER_HINT_INTEGER = 'I';
    static final char INK_SERVER_HINT_INVENTORY = 'V';
    static final char INK_SERVER_HINT_CUR_ROOM = 'C';
    static final char INK_SERVER_HINT_ROOM_HISTORY = 'R';
    static final String INK_SERVER_VARIABLE_PREFIX_INVENTORY = "inv-";
    static final String INK_SERVER_VARIABLE_PREFIX_ROOM = "room-";
    static final String INK_SERVER_VARIABLE_NAME_CUR_ROOM = "cur-curRoom";
    static final String INK_SERVER_VARIABLE_NAME_ROOM_HISTORY_SIZE = "room-size";
    public static final int INK_SERVER_GAME_TYPE_NEW = 1;
    public static final int INK_SERVER_GAME_TYPE_SAVED = 2;
    static final int RESOURCE_HEAP_COUNT = 1;
    static final String RESOURCE_INFO_NAME = "ink.resourceinfo";
    static final String RESOURCE_CURRENTSUBCHUNK = "ink.currentsubchunk";
    static final String resourceChunkInRMSBase = "ink.";
    static int[] resourceRMSLRE;
    static int[] resourceJARLRE;
    static Vector resourcesToDownload;
    static final byte RESOURCE_SOURCE_JAR = 1;
    static final byte RESOURCE_SOURCE_RMS = 2;
    static final byte RESOURCE_SOURCE_CURRENTSUBCHUNK = 3;
    static final int RESOURCE_SC_MAX_SIZE = 20480;
    static byte[] resourceSCData;
    static int resourceSCCurrentSize;
    static byte[] resourceFileIndex;
    static Hashtable resourceFileHashtable;
    static final int CODED_STRING_COLUMN_WIDTH = 30;
    static final int MAX_LINE = 100;
    static final byte RESOURCE_ROLLBACK_SET = 0;
    static final byte RESOURCE_ROLLBACK_REMOVE = 1;
    static final byte RESOURCE_ROLLBACK_DO = 2;
    static final String RESOURCE_ROLLBACK_ID = "ink.rollback";
    static String[] gameTexts;
    private static final char TEXT_IDENTIFICATOR = '$';
    static Runtime runtime;
    public static String gameIdPrefix;
    public static String gameId;
    public static int gameNumber;
    static Hashtable indexIniHash;
    public static String[] textLabels;
    public static int[] language_text_ids;
    public static String curLanguageId;
    public static int prevGameId;
    public static int prevGameSessionId;
    public static int minFreeMemory;
    public static final int LOAD_REQUEST_SCRIPT = 1;
    public static final int LOAD_REQUEST_GFX_STRING = 2;
    public static final int LOAD_REQUEST_GFX_INTEGER = 3;
    public static final int LOAD_REQUEST_SFX = 4;
    public static final int LOAD_REQUEST_ROOM = 5;
    public static boolean soundContinue = false;
    public static boolean soundContinue2 = false;
    public static boolean stopKeyHandling = true;
    public static boolean lastStopKeyHandling = true;
    public static boolean waitForKeyHandling = true;
    public static long timeStopKeyHandling = System.currentTimeMillis();
    static String current_room_id = "00";
    static String last_room_id = "000";
    public static boolean DEMO_END = false;
    public static boolean enableDemoDissolve = false;
    public static boolean exitTrial = false;
    public static int FADE_FRAMES = 0;
    public static int DEMO_FRAMES = 2;
    static boolean painting = false;
    public static int loadingBarMarkerX = 0;
    public static boolean loadingBarEngineMode = true;
    public static boolean forceLoadingBar = false;
    public static int resNum = 1;
    static int keyDown = 0;
    static String useItemSayText = "";
    static String useAnyItemSayText = "";
    static int dissolveFXColor = -1;
    static int gotoDissolveFXColor = -1;
    static int gotoDissolveFXCounter = -6;
    static int roomWidth = 256;
    static int roomHeight = 256;
    static int cursorX = roomWidth >> 1;
    static int cursorY = roomHeight >> 1;
    static boolean mainMenuActive = true;
    public static boolean enableStreaming = false;
    public static boolean enablePreloading = false;
    public static boolean enableChunking = false;
    public static boolean scrollRoomHor = true;
    public static boolean scrollRoomVer = true;
    public static boolean hideCursor = false;
    static String sysUser = "";
    static String sysPassword = "";
    static String sysPasswordHidden = "";
    static boolean firstStreamAttempt = true;
    static boolean collectAdjacentRoomIds = false;
    static Vector adjacentRoomIds = new Vector();
    static int loadingMode = -1;
    static Vector realizedExtras = new Vector();
    static int currentServer = 0;
    public static boolean gameInitiated = false;
    static Hashtable inkServerVariables = new Hashtable();
    static Hashtable inkServerHint = new Hashtable();
    static int inkServerGamesOwned = 0;
    static int inkServerGamesSaved = 0;
    static Random randomInstance = new Random();
    static int tickBasedTimeValue = 0;
    static String resourceChunkInJarBase = "chunks/";
    static int resourceRMSCount = 0;
    static int resourceJARCount = 0;
    static byte[][] resourceHeapDataLRE = new byte[1][];
    static int[] resourceHeapSourceLRE = new int[1];
    static Vector resourceImportants = new Vector();
    public static int resourceStreamComplete = -1;
    public static String prevLanguageId = "en";
    public static boolean curSoundMode = true;
    public static boolean curVibraMode = false;

    protected void startApp() {
        if (midlet != null) {
            setHide(false);
            return;
        }
        midlet = this;
        canvas = new GameCanvas();
        SilentHillGame.appInit();
    }

    protected void pauseApp() {
        setHide(true);
    }

    protected void destroyApp(boolean forced) {
        exit();
    }

    static DataInputStream openJar(String name) {
        InputStream resourceAsStream = name.getClass().getResourceAsStream(new StringBuffer().append("/").append(name).toString());
        if (resourceAsStream == null) {
            return null;
        }
        return new DataInputStream(resourceAsStream);
    }

    @Override // java.lang.Runnable
    public void run() {
        Thread currentThread = Thread.currentThread();
        try {
            if (currentThread == tickerThread) {
                tickerGo = true;
                tickCounter = (byte) 0;
                while (tickerGo) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (soundContinue2) {
                        time_start2 = System.currentTimeMillis();
                        soundContinue2 = false;
                    }
                    if (soundContinue && currentTimeMillis - time_start2 > 3000) {
                        GameCanvas.resumeSound();
                        soundContinue = false;
                    }
                    tick();
                    repaintCanvasIfPossible();
                    long elapsedTickMillis = System.currentTimeMillis() - currentTimeMillis;
                    if (elapsedTickMillis < 50) {
                        synchronized (this) {
                            Thread thread = tickerThread;
                            Thread.sleep(50 - elapsedTickMillis);
                        }
                    } else {
                        Thread.yield();
                    }
                }
            } else if (currentThread == loadThread) {
                while (loading()) {
                    loadRun2();
                }
            } else if (currentThread == roomRepaintThread) {
                roomRepaintRun();
            }
        } catch (InterruptedException e) {
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public static void repaintCanvasIfPossible() {
        if (painting) {
            return;
        }
        painting = true;
        canvas.repaint();
        canvas.serviceRepaints();
    }

    static void tick() {
        if (keyNew || keyPressed) {
            keyDown = keyLastPressed;
        } else {
            keyDown = 0;
        }
        appTick();
        keyNew = false;
    }

    static void paint(Graphics graphics) {
        if (FADE_FRAMES <= DEMO_FRAMES) {
            InkEngine.paint(graphics);
        }
    }

    static void setHide(boolean hide) {
        if (!mainMenuActive && hiddenCanvas && !hide) {
            if (InkEngine.battleMode) {
                InkEngine.battlePaused = true;
            }
            SilentHillGame.createEscapeMenu(false, InkInterpreter.pausedThread == null);
        }
        hiddenCanvas = hide;
    }

    static void setDisplay(Displayable d) {
        Display.getDisplay(midlet).setCurrent(d);
    }

    static void setKeyStatus(int key, boolean status) {
        keyNew |= status;
        keyPressed = status;
        if (status) {
            keyLastPressed = key;
            InkEngine.menuScrollTickCounter = (byte) 0;
        }
    }

    static void appStart() {
        tickerThread.start();
    }

    static void endGame() {
        GameResource.imagesImportants.removeAllElements();
        GameResource.imagesLRE.removeAllElements();
        resetLoad();
        resetVariableSystem();
        GameCanvas.stopSound();
        soundContinue = false;
        InkEngine.createMainMenu();
    }

    static void startSavedGame(int sessionId) {
        InkEngine.superBossDead = false;
        RoomObject.noVibraYet = false;
        tickBasedTimeValue = 1000000;
        RoomObject.paintingAnimationTime = 1L;
        MenuModel.closeAll();
        cursorX = roomWidth >> 1;
        cursorY = roomHeight >> 1;
        roomScrollOffsetX = max(0, (roomWidth - canvasWidth) >> 1);
        roomScrollOffsetY = max(0, (roomHeight - canvasHeight) >> 1);
        setupDone = true;
        InkEngine.roomInit("setup", false);
        SilentHillGame.HUD_NESW = loadImageFromJAR(new StringBuffer().append("gfx/HUD_NESW_").append(curLanguageId).append(".png").toString());
    }

    static void appTick() {
        if (((loadingMode == 0 || loadingMode == 2) && !mainMenuActive) || roomRepainting) {
            return;
        }
        if (loadingChunk) {
            loadingChunk = false;
            InkScript.resume();
        }
        if (InkEngine.textFadeMenuClose) {
            MenuModel.closeCurrent();
            InkScript.resume();
            InkEngine.textFadeMenuClose = false;
        }
        byte b = (byte) (tickCounter + 1);
        tickCounter = b;
        if (b > TOPLEFT) {
            tickCounter = (byte) 0;
        }
        InkEngine.appKeyHandling();
        if (loadingMode == 1 || loadingMode == -1) {
            roomCheckUpdate();
            roomCheckRepaint();
        }
        if (endGame) {
            endGame();
            endGame = false;
        }
    }

    public static void soundToggle() {
        curSoundMode = !curSoundMode;
        if (curSoundMode) {
            if (gameId == null) {
                setGameSpecificData(1);
            }
            GameCanvas.playSound("sh_shot", 1);
        } else {
            GameCanvas.stopSound();
        }
        saveSoundMode();
    }

    public static int getPosInLanguageSelectionList(String language) {
        for (int i = 0; i < languages.length; i++) {
            if (language.equals(languages[i])) {
                return i;
            }
        }
        return 0;
    }

    static void drawDissolve() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis < dissolveFXTime + ((long) dissolveFXImgTimer)) {
            for (int i = 0; i < bloodEffectBlur; i++) {
                for (int i2 = 0; i2 < bloodEffectPhase; i2++) {
                    for (int i3 = 0; i3 < 5; i3++) {
                        gfx.drawImage(bloodEffectImg1, bloodEffectX[i2][i3], bloodEffectY[i2][i3] + (i << 1) + bloodEffectShift, 0);
                    }
                }
            }
            if (bloodEffectPhase > 0) {
                gfx.drawImage(bloodEffectImg2, bloodEffectSprayX1, bloodEffectSprayY1, 0);
                gfx.drawImage(bloodEffectImg2, bloodEffectSprayX2, bloodEffectSprayY2, 0);
            }
            if (bloodEffectPhase == 2) {
                if (random(5) == 0) {
                    bloodEffectBlur++;
                    if (bloodEffectBlur > 4) {
                        bloodEffectBlur = 4;
                    }
                }
                if (bloodEffectBlur == 4) {
                    bloodEffectShift += random(7);
                }
            }
            if (bloodEffectPhase + 1 < 3) {
                bloodEffectPhase = (byte) (bloodEffectPhase + 1);
                bloodEffectX[bloodEffectPhase] = new int[5];
                bloodEffectY[bloodEffectPhase] = new int[5];
                for (int i4 = 0; i4 < 5; i4++) {
                    bloodEffectX[bloodEffectPhase][i4] = (canvasWidth >> 2) + random(canvasWidth >> 1);
                    bloodEffectY[bloodEffectPhase][i4] = (canvasHeight >> 2) + random(canvasHeight >> 1);
                }
                return;
            }
            return;
        }
        if (currentTimeMillis < dissolveFXTime + ((long) dissolveFXImgTimer) + ((long) dissolveFXColorTimer)) {
            gfx.setColor(dissolveFXColor);
            gfx.setClip(0, 0, canvasWidth, canvasHeight);
            gfx.fillRect(0, 0, canvasWidth, canvasHeight);
            return;
        }
        if (currentTimeMillis > dissolveFXTime + ((long) dissolveFXImgTimer) + ((long) dissolveFXImgTimer) + ((long) dissolveFXColorTimer)) {
            if (dissolveFXLoops != 0) {
                dissolveFXLoops--;
                dissolveFXTime = currentTimeMillis;
                return;
            }
            dissolveFXTime = 0L;
            dissolveFXImg = null;
            bloodEffectImg1 = null;
            bloodEffectImg2 = null;
            bloodEffectX = (int[][]) null;
            bloodEffectY = (int[][]) null;
            return;
        }
        for (int i5 = 0; i5 < bloodEffectBlur; i5++) {
            for (int i6 = 0; i6 < bloodEffectPhase; i6++) {
                for (int i7 = 0; i7 < 5; i7++) {
                    gfx.drawImage(bloodEffectImg1, bloodEffectX[i6][i7], bloodEffectY[i6][i7] + (i5 << 1) + bloodEffectShift, 0);
                }
            }
        }
        if (bloodEffectPhase > 0) {
            gfx.drawImage(bloodEffectImg2, bloodEffectSprayX1, bloodEffectSprayY1, 0);
            gfx.drawImage(bloodEffectImg2, bloodEffectSprayX2, bloodEffectSprayY2, 0);
        }
        if (bloodEffectPhase == 2) {
            if (random(5) == 0) {
                bloodEffectBlur++;
                if (bloodEffectBlur > 4) {
                    bloodEffectBlur = 4;
                }
            }
            if (bloodEffectBlur == 4) {
                bloodEffectShift += random(7);
            }
        }
        if (bloodEffectPhase + 1 < 3) {
            bloodEffectPhase = (byte) (bloodEffectPhase + 1);
            bloodEffectX[bloodEffectPhase] = new int[5];
            bloodEffectY[bloodEffectPhase] = new int[5];
            for (int i8 = 0; i8 < 5; i8++) {
                bloodEffectX[bloodEffectPhase][i8] = (canvasWidth >> 2) + random(canvasWidth >> 1);
                bloodEffectY[bloodEffectPhase][i8] = (canvasHeight >> 2) + random(canvasHeight >> 1);
            }
        }
    }

    static String roomGetCurrent() {
        String currentRoomId = inkServerGetVariable(INK_SERVER_VARIABLE_NAME_CUR_ROOM);
        return currentRoomId == null ? "" : currentRoomId;
    }

    static void roomSetCurrent(String id) {
        inkServerSetVariable(INK_SERVER_VARIABLE_NAME_CUR_ROOM, id, charToString('C'));
    }

    static void roomCheckUpdate() {
        if (!roomUpdateNeeded || mainMenuActive || roomRepainting) {
            return;
        }
        try {
            roomUpdateNeeded = false;
            if (roomObjects == null) {
                return;
            }
            nextRoomInit = false;
            for (int i = 0; roomObjects != null && i < roomObjects.length; i++) {
                if (loadBarActive) {
                    repaintCanvasIfPossible();
                }
                RoomObject roomObject = roomObjects[i];
                if (roomObject != null) {
                    boolean objectUpdated = roomObject.update();
                    if (nextRoomInit) {
                        return;
                    } else {
                        roomRepaintNeeded = roomRepaintNeeded || objectUpdated;
                    }
                }
            }
            if (!roomEntered) {
                roomEntered = true;
            }
        } catch (Exception e) {
            roomUpdateNeeded = false;
            roomRepaintNeeded = false;
            SilentHillGame.createEscapeMenu(true, true);
        }
    }

    static void roomCheckRepaint() {
        if (!roomRepaintNeeded || roomRepainting) {
            return;
        }
        if (!loading() || loadingMode == 1) {
            roomRepainting = true;
            roomRepaintThread = new Thread(midlet);
            roomRepaintThread.start();
        }
    }

    static void roomRepaintRun() {
        roomRepaint();
        roomRepaintThread = null;
    }

    static void roomRepaint() {
        roomRepainting = true;
        if (gotoDissolveFXIsSet && gotoDissolveFXCounter <= -3) {
            gotoDissolveFXCounter = 3;
            gotoDissolveFXIsSet = false;
        }
        roomGraphics.setColor(ingameBgColor);
        roomGraphics.fillRect(0, 0, roomImage.getWidth(), roomImage.getHeight());
        for (int i = 0; i < roomObjects.length && roomRepainting; i++) {
            try {
                roomObjects[i].paint(roomGraphics, 0, 0, roomImage.getWidth(), roomImage.getHeight());
            } catch (Exception e) {
            }
        }
        if (loadBarActive) {
            setKeyStatus(0, false);
            loadBarActive = false;
        }
        roomRepaintNeeded = false;
        roomRepainting = false;
    }

    static int getLeft(int x, int width, int height, int regPointX, int regPointY, int transform) {
        switch (transform) {
            case 0:
            default:
                return x - regPointX;
            case 1:
                return x - (width - regPointX);
            case 2:
                return x - (height - regPointY);
            case 3:
                return x - (height - regPointY);
            case 4:
                return x - (width - regPointX);
            case 5:
                return x - regPointX;
            case 6:
                return x - regPointY;
            case 7:
                return x - regPointY;
        }
    }

    static int getTop(int y, int width, int height, int regPointX, int regPointY, int transform) {
        switch (transform) {
            case 0:
            default:
                return y - regPointY;
            case 1:
                return y - regPointY;
            case 2:
                return y - regPointX;
            case 3:
                return y - (width - regPointX);
            case 4:
                return y - (height - regPointY);
            case 5:
                return y - (height - regPointY);
            case 6:
                return y - (width - regPointX);
            case 7:
                return y - regPointX;
        }
    }

    static boolean loading() {
        return loadThread != null;
    }

    static void loadStart(int loadingMode) {
        Application.loadingMode = loadingMode;
        roomRepaintThread = null;
        roomRepainting = false;
        if (loading()) {
            return;
        }
        GameCanvas.stopSound();
        Object[] retainedScripts = new Object[InkEngine.actionKey_scriptIds.length];
        for (int scriptIndex = 0; scriptIndex < InkEngine.actionKey_scriptIds.length; scriptIndex++) {
            if (InkEngine.actionKey_scriptIds[scriptIndex] != null) {
                retainedScripts[scriptIndex] = InkScript.list.get(InkEngine.actionKey_scriptIds[scriptIndex]);
            }
        }
        InkScript.list.clear();
        for (int scriptIndex = 0; scriptIndex < InkEngine.actionKey_scriptIds.length; scriptIndex++) {
            if (InkEngine.actionKey_scriptIds[scriptIndex] != null) {
                InkScript.list.put(InkEngine.actionKey_scriptIds[scriptIndex], retainedScripts[scriptIndex]);
            }
        }
        roomObjects = null;
        if (GameCanvas.shotSound == null) {
            GameCanvas.shotSound = resourceGet(new StringBuffer().append(gameId).append("/sfx/").append("mid").append("/sh_shot.").append("mid").toString());
        }
        loadThread = new Thread(midlet);
        loadThread.start();
    }

    static void loadChunk() {
        if (!loadINIFromServer()) {
            getIndexIni();
        }
        boolean chunkLoaded = false;
        if (indexIniHash != null) {
            int serverCount = toInt(indexIniHash.get(new StringBuffer().append(gameIdPrefix).append("servers").toString()));
            for (int attempt = 0; attempt < serverCount && !chunkLoaded; attempt++) {
                try {
                    String chunkUrl = new StringBuffer().append((String) indexIniHash.get(new StringBuffer().append(gameIdPrefix).append("server.").append(currentServer + 1).toString())).append(gameId).append("/").append((String) indexIniHash.get(new StringBuffer().append(gameIdPrefix).append(chunkID).toString())).toString();
                    if (resourceGetChunkFromServer(chunkUrl)) {
                        loadedChunksID.addElement(new StringBuffer().append(gameId).append("_").append(chunkID).toString());
                        saveChunkIDInRMS();
                        chunkLoaded = true;
                    } else {
                        System.err.println(new StringBuffer().append("Failed to load chunk from ").append(chunkUrl).toString());
                        currentServer = (currentServer + 1) % serverCount;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (chunkLoaded || indexIniHash == null) {
            loadingMode = -1;
        } else {
            InkScript.stop();
            InkEngine.popupCreate(getString(128), 1);
        }
    }

    static void loadStream() {
        if (firstStreamAttempt) {
            if (!loadINIFromServer()) {
                getIndexIni();
            }
            firstStreamAttempt = false;
        }
        boolean streamLoaded = false;
        if (indexIniHash != null) {
            int serverCount = toInt(indexIniHash.get(new StringBuffer().append(gameIdPrefix).append("servers").toString()));
            for (int attempt = 0; attempt < serverCount && !streamLoaded; attempt++) {
                try {
                    String streamUrl = new StringBuffer().append((String) indexIniHash.get(new StringBuffer().append(gameIdPrefix).append("server.").append(currentServer + 1).toString())).append((String) indexIniHash.get(new StringBuffer().append(gameIdPrefix).append("streaming").toString())).toString();
                    if (resourceGetStreamFromServer(streamUrl)) {
                        streamLoaded = true;
                    } else {
                        System.err.println(new StringBuffer().append("Failed to load stream from ").append(streamUrl).toString());
                        currentServer = (currentServer + 1) % serverCount;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (streamLoaded || indexIniHash == null) {
            return;
        }
        InkEngine.popupCreate(getString(128), 1);
    }

    static Hashtable readIni(DataInputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        Hashtable hashtable = new Hashtable();
        StringBuffer keyBuffer = new StringBuffer();
        StringBuffer valueBuffer = new StringBuffer();
        boolean comment = false;
        boolean readingKey = true;
        while (true) {
            int character = in.read();
            if (character < 0) {
                return hashtable;
            }
            if (character == 59) {
                comment = true;
            } else if (character == 10 || character == 13) {
                comment = false;
                readingKey = true;
                if (keyBuffer.length() > 0 && valueBuffer.length() > 0) {
                    hashtable.put(keyBuffer.toString().trim(), valueBuffer.toString().trim());
                }
                keyBuffer = new StringBuffer();
                valueBuffer = new StringBuffer();
            } else if (character == 61) {
                readingKey = false;
            }
            if (!comment) {
                if (readingKey) {
                    keyBuffer.append((char) character);
                } else {
                    valueBuffer.append((char) character);
                }
            }
        }
    }

    static Hashtable httpIniReader(String url) {
        Hashtable ini = null;
        int attemptsRemaining = 5;
        while (attemptsRemaining > 0) {
            attemptsRemaining--;
            HttpConnection httpConnection = null;
            try {
                httpConnection = (HttpConnection) Connector.open(url);
                switch (httpConnection.getResponseCode()) {
                    case 200:
                        saveChunkINI(httpConnection.openDataInputStream());
                        ini = readIni(loadChunkINI());
                        break;
                    case 301:
                    case 302:
                    case 307:
                        url = httpConnection.getHeaderField("Location");
                        continue;
                }
                attemptsRemaining = 0;
            } catch (Exception e) {
            } finally {
                if (httpConnection != null) {
                    try {
                        httpConnection.close();
                    } catch (IOException e2) {
                    }
                }
            }
        }
        return ini;
    }

    static void loadRun2() {
        switch (loadingMode) {
            case 0:
                collectAdjacentRoomIds = enablePreloading;
                if (InkEngine.loadAndRealizeExtras() && assertLoaded(selectedRoom)) {
                    realize(selectedRoom);
                    loadingMode = 1;
                    loadBarActive = true;
                }
                break;
            case 1:
                collectAdjacentRoomIds = false;
                if (!adjacentRoomIds.isEmpty()) {
                    int roomIndex = 0;
                    while (roomIndex < adjacentRoomIds.size()) {
                        if (assertLoaded((String) adjacentRoomIds.elementAt(roomIndex))) {
                            adjacentRoomIds.removeElementAt(roomIndex);
                            roomIndex--;
                        }
                        roomIndex++;
                    }
                } else {
                    loadingMode = -1;
                }
                break;
            case 2:
                if (!enableChunking) {
                    System.out.println("Warning! Loadchunk even though chunking was disabled");
                }
                loadChunk();
                loadingMode = -1;
                break;
            case 3:
                if (resourcesToDownload.isEmpty()) {
                    loadingMode = -1;
                }
                break;
            default:
                loadingMode = -1;
                loadThread = null;
                break;
        }
        if (resourcesToDownload.isEmpty()) {
            return;
        }
        if (enableStreaming) {
            loadStream();
        } else {
            System.out.println("Warning! Stream even though streaming was disabled");
        }
    }

    static boolean assertLoaded(String room) {
        InputStream resourceStream = resourceGet(loadRequest_getResourcePath(5, room));
        if (resourceStream != null) {
            assertRoom(resourceStream);
            try {
                resourceStream.close();
            } catch (Exception e) {
            }
        }
        return resourcesToDownload.isEmpty();
    }

    static void assertRoom(InputStream is) {
        DataInputStream input = new DataInputStream(is);
        try {
            String[] stringList = readStringList(input);
            readReferences2(input, stringList, 1, false);
            readReferences2(input, stringList, 2, true);
            readReferences2(input, stringList, 3, true);
        } catch (Exception e) {
        }
    }

    static boolean assertScript(InputStream is) {
        DataInputStream input = new DataInputStream(is);
        int size = resourcesToDownload.size();
        try {
            String[] stringList = readStringList(input);
            readReferences2(input, stringList, 5, false);
            readReferences2(input, stringList, 1, false);
            readReferences2(input, stringList, 2, false);
            readReferences2(input, stringList, 3, false);
            readReferences2(input, stringList, 4, false);
        } catch (Exception e) {
        }
        return resourcesToDownload.size() == size;
    }

    static void readReferences2(DataInputStream is, String[] stringList, int requestType, boolean readImageTransforms) {
        String resourcePath;
        try {
            int unsignedByte = is.readUnsignedByte();
            String referencedId = null;
            for (int i2 = 0; i2 < unsignedByte; i2++) {
                switch (requestType) {
                    case 2:
                        referencedId = stringList[is.readUnsignedByte() - 1];
                        resourcePath = loadRequest_getResourcePath(referencedId, readImageTransforms ? is.readUnsignedByte() : 0);
                        break;
                    case 3:
                        int unsignedShort = is.readUnsignedShort();
                        resourcePath = loadRequest_getResourcePath(new Integer(unsignedShort), readImageTransforms ? is.readUnsignedByte() : 0);
                        break;
                    case 4:
                        referencedId = stringList[is.readUnsignedByte() - 1];
                        resourcePath = loadRequest_getResourcePath(requestType, referencedId);
                        break;
                    default:
                        referencedId = stringList[is.readUnsignedByte() - 1];
                        resourcePath = loadRequest_getResourcePath(requestType, referencedId);
                        break;
                }
                if (requestType != 5) {
                    InputStream resourceStream = resourceGet(resourcePath);
                    if (resourceStream != null && requestType == 1) {
                        assertScript(resourceStream);
                    }
                    try {
                        resourceStream.close();
                    } catch (Exception e) {
                    }
                } else if (collectAdjacentRoomIds) {
                    adjacentRoomIds.addElement(referencedId);
                }
            }
        } catch (Exception e2) {
        }
    }

    static void realize(String room) {
        InputStream resourceStream = resourceGet(loadRequest_getResourcePath(5, room));
        loadRoom(resourceStream);
        try {
            resourceStream.close();
        } catch (Exception e) {
        }
        roomEntered = false;
        roomUpdateNeeded = true;
        roomRepaintNeeded = true;
    }

    public static void resetLoad() {
        loadThread = null;
        loadingMode = -1;
        resourcesToDownload.removeAllElements();
    }

    static String[] readReferences(DataInputStream is, String[] stringList, int requestType, boolean readImageTransforms) {
        try {
            int unsignedByte = is.readUnsignedByte();
            String[] referenceIds = new String[unsignedByte];
            for (int i2 = 0; i2 < unsignedByte; i2++) {
                switch (requestType) {
                    case 2:
                    case 4:
                        is.skip(1L);
                        if (readImageTransforms) {
                            is.skip(1L);
                        }
                        break;
                    case 3:
                        is.skip(2L);
                        if (readImageTransforms) {
                            is.skip(1L);
                        }
                        break;
                    default:
                        referenceIds[i2] = stringList[is.readUnsignedByte() - 1];
                        break;
                }
                if (requestType == 1) {
                    InputStream resourceStream = resourceGet(loadRequest_getResourcePath(requestType, referenceIds[i2]));
                    loadScript(resourceStream, referenceIds[i2]);
                    try {
                        resourceStream.close();
                    } catch (Exception e) {
                    }
                }
            }
            return referenceIds;
        } catch (Exception e2) {
            return null;
        }
    }

    static boolean loadRoom(InputStream in) {
        try {
            DataInputStream input = new DataInputStream(in);
            saveIsPossible = false;
            String[] stringList = readStringList(input);
            readReferences(input, stringList, 1, false);
            readReferences(input, stringList, 2, true);
            readReferences(input, stringList, 3, true);
            int unsignedByte = input.readUnsignedByte();
            roomObjects = new RoomObject[unsignedByte];
            for (int i = 0; i < unsignedByte; i++) {
                roomObjects[i] = new RoomObject(input, stringList);
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    static boolean loadScript(InputStream in, String id) {
        if (InkScript.list.contains(id)) {
            return true;
        }
        try {
            DataInputStream input = new DataInputStream(in);
            if (input == null) {
                return false;
            }
            String[] stringList = readStringList(input);
            readReferences(input, stringList, 5, false);
            readReferences(input, stringList, 1, false);
            readReferences(input, stringList, 2, false);
            readReferences(input, stringList, 3, false);
            readReferences(input, stringList, 4, false);
            InkScript.list.put(id, new InkScript(input, stringList));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    static int find(DataInputStream is, String target) {
        try {
            StringBuffer stringBuffer = new StringBuffer();
            for (int length = target.length(); length > 0; length--) {
                stringBuffer.append(' ');
            }
            while (!stringBuffer.toString().equals(target)) {
                stringBuffer.append((char) is.readUnsignedByte());
                stringBuffer.deleteCharAt(0);
            }
        } catch (Exception e) {
        }
        return 0;
    }

    static String[] readStringList(DataInputStream is) {
        String[] strings = null;
        try {
            int unsignedByte = is.readUnsignedByte();
            strings = new String[unsignedByte];
            for (int i = 0; i < unsignedByte; i++) {
                String value = is.readUTF();
                if (value.length() > 0 && value.charAt(0) == TEXT_IDENTIFICATOR) {
                    value = getGameText(value.substring(1));
                }
                strings[i] = value;
                if (strings[i].equals("savePoint") && loadingMode == 0) {
                    saveIsPossible = true;
                }
            }
        } catch (Exception e) {
        }
        return strings;
    }

    static String readString(DataInputStream is) {
        StringBuffer result = new StringBuffer();
        while (true) {
            try {
                char c = (char) is.readByte();
                if (c == 0) {
                    break;
                }
                result.append(c);
            } catch (Exception e) {
            }
        }
        return result.toString();
    }

    static void writeString(DataOutputStream os, String value) {
        try {
            for (int i = 0; i < value.length(); i++) {
                os.write((byte) value.charAt(i));
            }
            os.write(0);
        } catch (Exception e) {
        }
    }

    public static int inkServerGetNumOfGames(int browseMode) {
        int gameCount = 0;
        if (indexIniHash == null) {
            return 0;
        }
        int configuredGameCount = toInt(indexIniHash.get("games"));
        switch (browseMode) {
            case 1:
                gameCount = configuredGameCount;
                break;
            case 2:
                int savedGameCount = 0;
                for (int gameIndex = 1; gameIndex <= configuredGameCount; gameIndex++) {
                    if (InkEngine.savedGameExistsInRMS((String) indexIniHash.get(new StringBuffer().append(gameIndex).append(".datadir").toString()))) {
                        savedGameCount++;
                    }
                }
                gameCount = savedGameCount;
                break;
        }
        return gameCount;
    }

    static void inkServerSetVariable(String name, String value, String hint) {
        if (value == null) {
            inkServerUnsetVariable(name);
        } else {
            inkServerVariables.put(name, value);
            inkServerHint.put(name, hint);
        }
        gameChangedSinceLastSave = true;
    }

    static void inkServerUnsetVariable(String name) {
        inkServerVariables.remove(name);
        inkServerHint.remove(name);
        gameChangedSinceLastSave = true;
    }

    static String inkServerGetVariable(String name) {
        return (String) inkServerVariables.get(name);
    }

    static String inkServerGetHint(String name) {
        return (String) inkServerHint.get(name);
    }

    static String[] inkServerAllNamesWithHint(String hint) {
        String[] matchingNames = new String[inkServerHint.size()];
        int matchCount = 0;
        Enumeration enumerationKeys = inkServerHint.keys();
        while (enumerationKeys.hasMoreElements()) {
            String name = (String) enumerationKeys.nextElement();
            if (inkServerGetHint(name).equals(hint)) {
                int matchIndex = matchCount;
                matchCount++;
                matchingNames[matchIndex] = name;
            }
        }
        String[] result = new String[matchCount];
        if (matchCount > 0) {
            arrayCopyString(matchingNames, 0, result, 0, matchCount);
        }
        return result;
    }

    static byte[] inkServerGetBytes(InputStream in) throws Exception {
        if (in == null) {
            return null;
        }
        int bufferSize = 512;
        if (in.available() > 0) {
            bufferSize = in.available();
        }
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataInputStream input = new DataInputStream(in);
        while (true) {
            int bytesRead = input.read(buffer, 0, buffer.length);
            if (bytesRead == -1) {
                break;
            }
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        try {
            byteArrayOutputStream.close();
            input.close();
        } catch (IOException e) {
        }
        return byteArray;
    }

    static int toInt(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (!(o instanceof String)) {
            return 1;
        }
        try {
            return Integer.parseInt((String) o);
        } catch (Exception e) {
            return 1;
        }
    }

    static boolean toBoolean(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue() >= 1;
        }
        if (!(o instanceof String)) {
            return true;
        }
        try {
            return Integer.parseInt((String) o) >= 1;
        } catch (Exception e) {
            return true;
        }
    }

    static int min(int a, int b) {
        return a < b ? a : b;
    }

    static int max(int a, int b) {
        return a > b ? a : b;
    }

    static int abs(int a) {
        return a >= 0 ? a : -a;
    }

    static int dir(int a) {
        if (a == 0) {
            return 0;
        }
        return a > 0 ? 1 : -1;
    }

    static int random(int scale) {
        return (int) (((((long) randomInstance.nextInt()) & 4294967295L) * ((long) scale)) >> 32);
    }

    static int tickBasedTime() {
        return tickBasedTimeValue;
    }

    static void tickBasedTimeUpdate() {
        tickBasedTimeValue += 60;
    }

    static void tickBasedTimeReset() {
        tickBasedTimeValue = 0;
    }

    static String charToString(char c) {
        return new String(new char[]{c});
    }

    static String[] removeStringPrefix(String[] strings, String prefix) {
        if (strings == null) {
            return null;
        }
        if (prefix == null || prefix.length() == 0) {
            int length = strings.length;
            String[] copy = new String[length];
            arrayCopyString(strings, 0, copy, 0, length);
            return copy;
        }
        int stringCount = strings.length;
        String[] strippedStrings = new String[stringCount];
        int prefixLength = prefix.length();
        for (int i = 0; i < stringCount; i++) {
            if (strings[i].startsWith(prefix)) {
                strippedStrings[i] = strings[i].substring(prefixLength);
            }
        }
        return strippedStrings;
    }

    static void arrayCopyString(String[] source, int sourceStart, String[] target, int targetStart, int size) {
        System.arraycopy(source, sourceStart, target, targetStart, size);
    }

    static void roomAddToRoomHistory(String id) {
        if (id.equals(roomGetLastInRoomHistory())) {
            return;
        }
        int historySize = roomGetHistorySize() + 1;
        inkServerSetVariable(new StringBuffer().append(INK_SERVER_VARIABLE_PREFIX_ROOM).append(historySize).toString(), id, charToString('R'));
        inkServerSetVariable(INK_SERVER_VARIABLE_NAME_ROOM_HISTORY_SIZE, Integer.toString(historySize), charToString('R'));
    }

    static String roomGetLastInRoomHistory() {
        return inkServerGetVariable(new StringBuffer().append(INK_SERVER_VARIABLE_PREFIX_ROOM).append(roomGetHistorySize()).toString());
    }

    static void roomRemoveLastInRoomHistory() {
        int historySize = roomGetHistorySize();
        if (historySize > 0) {
            inkServerUnsetVariable(new StringBuffer().append(INK_SERVER_VARIABLE_PREFIX_ROOM).append(roomGetHistorySize()).toString());
            inkServerSetVariable(INK_SERVER_VARIABLE_NAME_ROOM_HISTORY_SIZE, Integer.toString(historySize - 1), charToString('R'));
        }
    }

    static int roomGetHistorySize() {
        return toInt(inkServerGetVariable(INK_SERVER_VARIABLE_NAME_ROOM_HISTORY_SIZE));
    }

    public static void saveGame(boolean showPopup) {
        resetLoad();
        if (InkEngine.saveGameInRMS(gameId)) {
            gameChangedSinceLastSave = false;
            if (showPopup) {
                InkEngine.popupCreate(getString(80), 0);
            }
        } else if (showPopup) {
            InkEngine.popupCreate(getString(118), 0);
        }
        inkServerGamesSaved = inkServerGetNumOfGames(2);
    }

    public static void updateSystemResourceValuesAll() {
        if (InkEngine.systemResources == null) {
            return;
        }
        for (int i = 0; i < InkEngine.systemResources.length; i++) {
            InkEngine.updateSystemResourceValues(i);
        }
    }

    public static boolean setGameSpecificData(int gameNumber) {
        try {
            gameIdPrefix = new StringBuffer().append(gameNumber).append(".").toString();
            gameId = (String) indexIniHash.get(new StringBuffer().append(gameIdPrefix).append("datadir").toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void resetVariableSystem() {
        inkServerVariables.clear();
        inkServerHint.clear();
    }

    static void resourceInit() {
        int jarChunkCount = 0;
        while (openJar(new StringBuffer().append(resourceChunkInJarBase).append(Integer.toString(jarChunkCount, TEXT_IDENTIFICATOR)).append(".bin").toString()) != null) {
            jarChunkCount++;
        }
        resourceJARCount = jarChunkCount;
        resourceJARLRE = new int[jarChunkCount];
        for (int chunkIndex = 0; chunkIndex < jarChunkCount; chunkIndex++) {
            resourceJARLRE[chunkIndex] = chunkIndex;
        }
        resourceRollback((byte) 2);
        int rmsChunkCount = 0;
        while (rmsGet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(rmsChunkCount, TEXT_IDENTIFICATOR)).toString()) != null) {
            rmsChunkCount++;
        }
        resourceRMSCount = rmsChunkCount;
        resourceRMSLRE = new int[rmsChunkCount];
        for (int i4 = 0; i4 < resourceRMSLRE.length; i4++) {
            resourceRMSLRE[i4] = i4;
        }
        resourceSCData = rmsGet(RESOURCE_CURRENTSUBCHUNK);
        if (resourceSCData == null) {
            resourceSCData = new byte[RESOURCE_SC_MAX_SIZE];
            resourceSCCurrentSize = 1;
        }
        int entryCount = 255 & resourceSCData[0];
        int currentOffset = 1;
        for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
            int dataLengthOffset = currentOffset + 1 + (255 & resourceSCData[currentOffset]);
            int dataLengthLowOffset = dataLengthOffset + 1;
            currentOffset = dataLengthLowOffset + 1 + (((255 & resourceSCData[dataLengthOffset]) << 8) | (255 & resourceSCData[dataLengthLowOffset]));
        }
        resourceSCCurrentSize = currentOffset;
        resourcesToDownload = new Vector();
        resourceUpdateFilelists();
    }

    static void resourceExit() throws IOException {
    }

    static void resourceUpdateFilelists() {
        try {
            Vector entries = new Vector(300, MAX_LINE);
            for (int i = 0; i < resourceJARCount; i++) {
                resourceGetNamesAndData(inkServerGetBytes(openJar(new StringBuffer().append(resourceChunkInJarBase).append(Integer.toString(i, TEXT_IDENTIFICATOR)).append(".bin").toString())), entries, (byte) 1, (byte) i);
            }
            for (int i2 = 0; i2 < resourceRMSCount; i2++) {
                resourceGetNamesAndData(rmsGet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(i2, TEXT_IDENTIFICATOR)).toString()), entries, (byte) 2, (byte) i2);
            }
            resourceGetNamesAndData(resourceSCData, entries, (byte) 3, (byte) 0);
            byte[][] sortedEntries = new byte[entries.size()][];
            Enumeration enumerationElements = entries.elements();
            int entryIndex = 0;
            while (enumerationElements.hasMoreElements()) {
                sortedEntries[entryIndex] = (byte[]) enumerationElements.nextElement();
                entryIndex++;
            }
            System.gc();
            resourceMergeSort(sortedEntries, 0, sortedEntries.length);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] previousName = new byte[256];
            int previousNameLength = 0;
            for (int sortedIndex = 0; sortedIndex < sortedEntries.length; sortedIndex++) {
                byte[] entry = sortedEntries[sortedIndex];
                int nameLength = entry.length - 4;
                int commonPrefixLength = 0;
                while (commonPrefixLength < nameLength && commonPrefixLength < previousNameLength && entry[commonPrefixLength] == previousName[commonPrefixLength]) {
                    commonPrefixLength++;
                }
                byteArrayOutputStream.write(commonPrefixLength);
                byteArrayOutputStream.write(nameLength - commonPrefixLength);
                byteArrayOutputStream.write(entry, commonPrefixLength, (nameLength - commonPrefixLength) + 4);
                System.arraycopy(entry, 0, previousName, 0, nameLength);
                previousNameLength = nameLength;
                sortedEntries[sortedIndex] = null;
            }
            resourceFileIndex = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            resourceFileIndex = null;
        }
    }

    static void printArray(byte[][] data) {
        printArray(data, 0, data.length);
    }

    static void printArray(byte[][] data, int from, int to) {
        for (int i3 = from; i3 < to; i3++) {
            System.out.print(new StringBuffer().append(i3).append(": ").append(codedString(data[i3])).toString());
        }
        System.out.println();
    }

    static void resourceGetNamesAndData(byte[] chunk, Vector v, byte source, byte id) {
        int entryCount = 255 & chunk[0];
        int entryOffset = 1;
        for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
            int nameLengthOffset = entryOffset;
            int nameOffset = entryOffset + 1;
            int nameLength = 255 & chunk[nameLengthOffset];
            byte[] indexedName = new byte[nameLength + 4];
            System.arraycopy(chunk, nameOffset, indexedName, 0, nameLength);
            int dataLengthOffset = nameOffset + nameLength;
            int dataLength = ((255 & chunk[dataLengthOffset]) << 8) | (255 & chunk[dataLengthOffset + 1]);
            indexedName[nameLength] = source;
            indexedName[nameLength + 1] = id;
            indexedName[nameLength + 2] = (byte) (255 & (dataLengthOffset >> 8));
            indexedName[nameLength + 3] = (byte) (255 & dataLengthOffset);
            entryOffset = dataLengthOffset + dataLength + 2;
            v.addElement(indexedName);
        }
    }

    static void resourceMergeSort(byte[][] v, int from, int to) {
        if (to - from <= 1) {
            return;
        }
        int middle = (from + to) / 2;
        if (middle != from) {
            resourceMergeSort(v, from, middle);
            resourceMergeSort(v, middle, to);
        }
        byte[][] merged = new byte[to - from][];
        int leftIndex = from;
        int rightIndex = middle;
        for (int mergedIndex = 0; mergedIndex < merged.length; mergedIndex++) {
            if (resourceMergeSortCmp(v[leftIndex], v[rightIndex])) {
                int selectedIndex = leftIndex;
                leftIndex++;
                merged[mergedIndex] = v[selectedIndex];
            } else {
                int selectedIndex = rightIndex;
                rightIndex++;
                merged[mergedIndex] = v[selectedIndex];
            }
            if (leftIndex == middle) {
                System.arraycopy(v, rightIndex, merged, mergedIndex + 1, to - rightIndex);
                break;
            } else {
                if (rightIndex == to) {
                    System.arraycopy(v, leftIndex, merged, mergedIndex + 1, middle - leftIndex);
                    break;
                }
            }
        }
        System.arraycopy(merged, 0, v, from, to - from);
    }

    static boolean resourceMergeSortCmp(byte[] a, byte[] b) {
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int leftByte = 255 & a[i];
            int rightByte = 255 & b[i];
            if (leftByte < rightByte) {
                return true;
            }
            if (leftByte > rightByte) {
                return false;
            }
        }
        return a.length < b.length;
    }

    static void resourceClear() {
        resourceHeapDataLRE = new byte[1][];
        resourceHeapSourceLRE = new int[1];
        resourceSCData[0] = 0;
        resourceSCCurrentSize = 1;
        resourceRMSLRE = new int[0];
        rmsDelete(RESOURCE_INFO_NAME);
        for (int i = 0; rmsGet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(i, TEXT_IDENTIFICATOR)).toString()) != null; i++) {
            rmsDelete(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(i, TEXT_IDENTIFICATOR)).toString());
        }
        rmsDelete(RESOURCE_CURRENTSUBCHUNK);
        resourceImportants = new Vector();
        resourcesToDownload = null;
        System.gc();
    }

    static InputStream resourceGet(String name) {
        System.currentTimeMillis();
        if (!resourceImportants.contains(name)) {
            resourceImportants.addElement(name);
        }
        if (resourcesToDownload.contains(name)) {
            return null;
        }
        InputStream resourceStream = null;
        if (resourceStream == null) {
            resourceStream = openJar(name);
        }
        if (resourceStream == null) {
            int resourceIndex = resourceLookup(name);
            if (resourceIndex != -1) {
                resourceStream = resourceGetDirectAccess(resourceIndex, name);
            } else {
                System.out.println(new StringBuffer().append("Failed to get ").append(name).append(" from subchunks direct access").toString());
            }
        }
        if (resourceStream == null) {
            resourceStream = resourceGetFromJar(name);
        }
        if (resourceStream == null) {
            resourceStream = resourceGetFromRMS(name);
        }
        if (resourceStream == null) {
            resourceGetFromServer(name);
        }
        return resourceStream;
    }

    static InputStream resourceGetDirectAccess(int lookupResult, String name) {
        if (resourceFileIndex == null) {
            return null;
        }
        try {
            int sourceType = (lookupResult >> 24) & 255;
            int sourceId = (lookupResult >> 16) & 255;
            int dataLengthOffset = lookupResult & 65535;
            byte[] storedBytes = null;
            int sourceKey = (sourceType << 16) | sourceId;
            int heapIndex = resourceIsOnHeap(sourceKey);
            if (heapIndex == -1) {
                switch (sourceType) {
                    case 1:
                        storedBytes = inkServerGetBytes(openJar(new StringBuffer().append(resourceChunkInJarBase).append(Integer.toString(sourceId, TEXT_IDENTIFICATOR)).append(".bin").toString()));
                        break;
                    case 2:
                        storedBytes = rmsGet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(sourceId, TEXT_IDENTIFICATOR)).toString());
                        break;
                    case 3:
                        storedBytes = resourceSCData;
                        break;
                }
            } else {
                storedBytes = resourceHeapDataLRE[heapIndex];
            }
            if (storedBytes == null) {
                return null;
            }
            int dataLength = ((255 & storedBytes[dataLengthOffset]) << 8) | (storedBytes[dataLengthOffset + 1] & 255);
            resourceAddToHeap(storedBytes, sourceKey);
            if (new String(storedBytes, dataLengthOffset - name.length(), name.length()).equals(name)) {
                return new ByteArrayInputStream(storedBytes, dataLengthOffset + 2, dataLength);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static int resourceLookup(String name) {
        byte[] bytes = name.getBytes();
        int indexOffset = 0;
        int matchedLength = 0;
        int result = -1;
        int entryNumber = 0;
        while (indexOffset < resourceFileIndex.length) {
            int commonPrefixOffset = indexOffset;
            int uncommonLengthOffset = indexOffset + 1;
            int commonPrefixLength = 255 & resourceFileIndex[commonPrefixOffset];
            indexOffset = uncommonLengthOffset + 1;
            int uncommonLength = 255 & resourceFileIndex[uncommonLengthOffset];
            int fullNameLength = commonPrefixLength + uncommonLength;
            if (commonPrefixLength < matchedLength) {
                break;
            }
            if (commonPrefixLength > matchedLength) {
                indexOffset += uncommonLength + 4;
                entryNumber++;
            } else {
                int newlyMatched = 0;
                while (uncommonLength > 0 && matchedLength < bytes.length) {
                    if (resourceFileIndex[indexOffset] == bytes[matchedLength]) {
                        indexOffset++;
                        matchedLength++;
                        uncommonLength--;
                        newlyMatched++;
                    } else {
                        indexOffset += uncommonLength + 4;
                        uncommonLength = 0;
                    }
                }
                if (matchedLength == bytes.length) {
                    if (matchedLength != fullNameLength) {
                        break;
                    }
                    int sourceTypeOffset = indexOffset;
                    int sourceIdOffset = indexOffset + 1;
                    int dataLengthHighOffset = sourceIdOffset + 1;
                    int sourceKey = ((255 & resourceFileIndex[sourceTypeOffset]) << 24) | ((255 & resourceFileIndex[sourceIdOffset]) << 16);
                    int dataLengthLowOffset = dataLengthHighOffset + 1;
                    int sourceAndHighOffset = sourceKey | ((255 & resourceFileIndex[dataLengthHighOffset]) << 8);
                    int nextEntryOffset = dataLengthLowOffset + 1;
                    result = sourceAndHighOffset | (255 & resourceFileIndex[dataLengthLowOffset]);
                    break;
                }
                entryNumber++;
            }
        }
        return result;
    }

    static InputStream resourceGetFromJar(String name) {
        InputStream chunkStream = null;
        int recencyIndex = 0;
        while (recencyIndex < resourceJARLRE.length && chunkStream == null) {
            try {
                int chunkId = resourceJARLRE[recencyIndex];
                if (resourceIsOnHeap(65536 | chunkId) == -1) {
                    chunkStream = openJar(new StringBuffer().append(resourceChunkInJarBase).append(Integer.toString(chunkId, TEXT_IDENTIFICATOR)).append(".bin").toString());
                    if (chunkStream == null) {
                        break;
                    }
                    chunkStream = resourceGetFromBytes(name, inkServerGetBytes(chunkStream), 65536 | chunkId);
                }
                recencyIndex++;
            } catch (Exception e) {
            }
        }
        if (chunkStream != null) {
            int recentlyUsedChunk = resourceJARLRE[recencyIndex - 1];
            for (int moveIndex = 0; moveIndex < recencyIndex; moveIndex++) {
                int displacedChunk = resourceJARLRE[moveIndex];
                resourceJARLRE[moveIndex] = recentlyUsedChunk;
                recentlyUsedChunk = displacedChunk;
            }
        }
        return chunkStream;
    }

    static InputStream resourceGetFromRMS(String name) {
        InputStream resourceStream = null;
        int recencyIndex = 0;
        while (recencyIndex < resourceRMSLRE.length && resourceStream == null) {
            try {
                int chunkId = resourceRMSLRE[recencyIndex];
                if (resourceIsOnHeap(131072 | chunkId) == -1) {
                    byte[] storedBytes = rmsGet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(chunkId, TEXT_IDENTIFICATOR)).toString());
                    if (storedBytes == null) {
                        break;
                    }
                    resourceStream = resourceGetFromBytes(name, storedBytes, 131072 | chunkId);
                }
                recencyIndex++;
            } catch (Exception e) {
            }
        }
        if (resourceStream != null) {
            int recentlyUsedChunk = resourceRMSLRE[recencyIndex - 1];
            for (int moveIndex = 0; moveIndex < recencyIndex; moveIndex++) {
                int displacedChunk = resourceRMSLRE[moveIndex];
                resourceRMSLRE[moveIndex] = recentlyUsedChunk;
                recentlyUsedChunk = displacedChunk;
            }
        }
        return resourceStream;
    }

    static void resourceAddToHeap(byte[] data, int source) {
        byte[] displacedData = null;
        byte[] dataToInsert = data;
        int sourceToInsert = source;
        for (int cacheIndex = 0; cacheIndex < 1 && displacedData != data && dataToInsert != null; cacheIndex++) {
            displacedData = resourceHeapDataLRE[cacheIndex];
            int displacedSource = resourceHeapSourceLRE[cacheIndex];
            resourceHeapDataLRE[cacheIndex] = dataToInsert;
            resourceHeapSourceLRE[cacheIndex] = sourceToInsert;
            dataToInsert = displacedData;
            sourceToInsert = displacedSource;
        }
    }

    static int resourceIsOnHeap(int source) {
        for (int i2 = 0; i2 < 1; i2++) {
            if (source == resourceHeapSourceLRE[i2]) {
                return i2;
            }
        }
        return -1;
    }

    static void resourceRestartImportants() {
        resourceImportants = new Vector();
    }

    static void resourceGetFromServer(String name) {
        if (!enableStreaming) {
            System.out.println(new StringBuffer().append("missing:").append(name).toString());
            loadingMode = -1;
            InkEngine.popupCreate(getString(39), 1);
        } else {
            System.out.println(new StringBuffer().append("adding to stream:").append(name).toString());
            if (resourcesToDownload.contains(name)) {
                return;
            }
            resourcesToDownload.addElement(name);
        }
    }

    static boolean resourceGetChunkFromServer(String url) {
        boolean loaded = false;
        int attemptsRemaining = 5;
        while (attemptsRemaining > 0) {
            attemptsRemaining--;
            HttpConnection connection = null;
            InputStream responseStream = null;
            try {
                connection = (HttpConnection) Connector.open(url);
                switch (connection.getResponseCode()) {
                    case 200:
                        responseStream = connection.openInputStream();
                        resourceLoadStream(responseStream, new StringBuffer().append(gameId).append("/").toString());
                        loaded = true;
                        break;
                    case 301:
                    case 302:
                    case 307:
                        url = connection.getHeaderField("Location");
                        System.out.println(new StringBuffer().append("Redirect:").append(url).toString());
                        continue;
                }
                attemptsRemaining = 0;
            } catch (Exception e5) {
                e5.printStackTrace();
                loaded = false;
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (IOException e3) {
                    }
                }
                if (responseStream != null) {
                    try {
                        responseStream.close();
                    } catch (IOException e4) {
                    }
                }
            }
        }
        return loaded;
    }

    static String resourceURLEncode(String value) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if ((character < 'a' || character > 'z') && ((character < 'A' || character > 'Z') && (character < '0' || character > '9'))) {
                stringBuffer.append(new StringBuffer().append("%").append(character < 15 ? "0" : "").append(Integer.toHexString(character)).toString());
            } else {
                stringBuffer.append(character);
            }
        }
        return stringBuffer.toString();
    }

    static boolean resourceGetStreamFromServer(String url) {
        boolean loaded = false;
        String[] requestedResources = new String[resourcesToDownload.size()];
        for (int resourceIndex = 0; resourceIndex < requestedResources.length; resourceIndex++) {
            requestedResources[resourceIndex] = (String) resourcesToDownload.elementAt(resourceIndex);
        }
        String postBody = new StringBuffer().append("").append("COUNT=").append(requestedResources.length).toString();
        for (int resourceIndex = 0; resourceIndex < requestedResources.length; resourceIndex++) {
            postBody = new StringBuffer().append(postBody).append("&FILE_").append(resourceIndex).append("=").append(resourceURLEncode(requestedResources[resourceIndex])).toString();
        }
        System.out.println(postBody);
        int attemptsRemaining = 5;
        while (attemptsRemaining > 0) {
            attemptsRemaining--;
            HttpConnection connection = null;
            OutputStream requestStream = null;
            InputStream responseStream = null;
            try {
                connection = (HttpConnection) Connector.open(url);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Length", new StringBuffer().append("").append(postBody.length()).toString());
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                requestStream = connection.openOutputStream();
                requestStream.write(postBody.getBytes());
                switch (connection.getResponseCode()) {
                    case 200:
                        responseStream = connection.openInputStream();
                        resourceLoadStream(responseStream, null);
                        for (String requestedResource : requestedResources) {
                            resourcesToDownload.removeElement(requestedResource);
                        }
                        loaded = true;
                        break;
                    case 301:
                    case 302:
                    case 307:
                        url = connection.getHeaderField("Location");
                        continue;
                }
                attemptsRemaining = 0;
            } catch (Exception e4) {
                e4.printStackTrace();
            } finally {
                try {
                    if (connection != null) connection.close();
                } catch (IOException e5) {
                }
                try {
                    if (requestStream != null) requestStream.close();
                } catch (IOException e6) {
                }
                try {
                    if (responseStream != null) responseStream.close();
                } catch (IOException e7) {
                }
            }
        }
        return loaded;
    }

    static String codedString(byte[] data) {
        StringBuffer result = new StringBuffer();
        int columnOffset = 0;
        int lineCount = 0;
        StringBuffer hexColumn = new StringBuffer();
        StringBuffer asciiColumn = new StringBuffer();
        for (int dataIndex = 0; dataIndex < data.length && lineCount < MAX_LINE; dataIndex++) {
            char c = (char) (data[dataIndex] & 255);
            if (c < ' ') {
                asciiColumn.append(".");
            } else {
                asciiColumn.append(c);
            }
            hexColumn.append(new StringBuffer().append(c <= 15 ? "0" : "").append(Integer.toHexString(c)).append(" ").toString());
            columnOffset++;
            if (columnOffset >= CODED_STRING_COLUMN_WIDTH) {
                result.append(new StringBuffer().append((Object) hexColumn).append(" | ").append((Object) asciiColumn).append("\n").toString());
                hexColumn.setLength(0);
                asciiColumn.setLength(0);
                columnOffset = 0;
                lineCount++;
            }
        }
        if (columnOffset > 0) {
            while (columnOffset < CODED_STRING_COLUMN_WIDTH) {
                columnOffset++;
                hexColumn.append("   ");
                asciiColumn.append(" ");
            }
            result.append(new StringBuffer().append((Object) hexColumn).append(" | ").append((Object) asciiColumn).append("\n").toString());
        }
        return result.toString();
    }

    public static InputStream peekStream(InputStream in) {
        try {
            byte[] bytes = inkServerGetBytes(in);
            System.out.println(codedString(bytes));
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void resourceLoadStream(InputStream in, String addGamePrefix) throws IOException {
        resourceRollback((byte) 0);
        try {
            DataInputStream input = new DataInputStream(in);
            int resourceCount = input.readUnsignedShort();
            resourceStreamComplete = 0;
            for (int resourceIndex = 0; resourceIndex < resourceCount; resourceIndex++) {
                resourceStreamComplete = (resourceIndex * 1024) / resourceCount;
                String resourceName = input.readUTF();
                byte[] resourceData = new byte[input.readUnsignedShort()];
                input.readFully(resourceData);
                if (resourceName.startsWith(resourceChunkInJarBase)) {
                    resourceCommitSCToRMS(resourceData);
                } else {
                    if (addGamePrefix != null) {
                        resourceName = new StringBuffer().append(addGamePrefix).append(resourceName).toString();
                    }
                    resourceAddToRms(resourceName, resourceData);
                }
            }
            rmsSet(RESOURCE_CURRENTSUBCHUNK, resourceSCData);
            resourceRollback((byte) 1);
            resourceUpdateFilelists();
            resourceStreamComplete = -1;
        } catch (IOException e) {
            resourceRollback((byte) 2);
            throw e;
        }
    }

    static void resourceRollback(byte mode) {
        switch (mode) {
            case 0:
                rmsSet(RESOURCE_ROLLBACK_ID, new byte[]{(byte) (255 & (resourceRMSLRE.length >> 8)), (byte) (255 & resourceRMSLRE.length)});
                System.out.println(new StringBuffer().append("Rollback set to: ").append(resourceRMSLRE.length).toString());
                break;
            case 1:
                rmsDelete(RESOURCE_ROLLBACK_ID);
                break;
            case 2:
                System.out.println("Rolling back...");
                byte[] storedBytes = rmsGet(RESOURCE_ROLLBACK_ID);
                if (storedBytes != null) {
                    int rollbackChunkId = ((255 & storedBytes[0]) << 8) | (255 & storedBytes[1]);
                    System.out.println(new StringBuffer().append("Rolling from: ").append(rollbackChunkId).toString());
                    while (true) {
                        String chunkName = new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(rollbackChunkId, TEXT_IDENTIFICATOR)).toString();
                        if (rmsGet(chunkName) == null) {
                            rmsDelete(RESOURCE_ROLLBACK_ID);
                            resourceInit();
                            break;
                        } else {
                            System.out.println(new StringBuffer().append("Killing: ").append(rollbackChunkId).toString());
                            rmsDelete(chunkName);
                            rollbackChunkId++;
                        }
                    }
                }
                break;
        }
    }

    static void resourceAddToRms(String name, byte[] data) {
        int length = data.length + name.length() + 3;
        if (length + resourceSCCurrentSize >= RESOURCE_SC_MAX_SIZE && resourceSCCurrentSize != 1) {
            byte[] completedSubchunk = resourceMakeSubChunk();
            resourceSCCurrentSize = 1;
            resourceSCData[0] = 0;
            resourceCommitSCToRMS(completedSubchunk);
            resourceAddToRms(name, data);
            return;
        }
        int entryOffset = resourceSCCurrentSize;
        byte[] subchunkData = resourceSCData;
        subchunkData[0] = (byte) (subchunkData[0] + 1);
        byte[] bytes = name.getBytes();
        int nameOffset = entryOffset + 1;
        resourceSCData[entryOffset] = (byte) bytes.length;
        System.arraycopy(bytes, 0, resourceSCData, nameOffset, bytes.length);
        int dataLengthOffset = nameOffset + bytes.length;
        int dataLengthLowOffset = dataLengthOffset + 1;
        resourceSCData[dataLengthOffset] = (byte) (255 & (data.length >> 8));
        int dataOffset = dataLengthLowOffset + 1;
        resourceSCData[dataLengthLowOffset] = (byte) (255 & data.length);
        System.arraycopy(data, 0, resourceSCData, dataOffset, data.length);
        int endOffset = dataOffset + data.length;
        resourceSCCurrentSize += length;
        if (endOffset != resourceSCCurrentSize) {
            System.err.println("Warning: inconsistency in making sub-chunk");
        }
    }

    private static void resourceCommitSCToRMS(byte[] subchunk) {
        int length = resourceRMSLRE.length;
        if (rmsSet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(length, TEXT_IDENTIFICATOR)).toString(), subchunk)) {
            int[] previousRecency = resourceRMSLRE;
            resourceRMSLRE = new int[previousRecency.length + 1];
            System.arraycopy(previousRecency, 0, resourceRMSLRE, 1, previousRecency.length);
            resourceRMSLRE[0] = length;
            resourceRMSCount++;
            return;
        }
        int evictedChunkId = resourceRMSLRE[resourceRMSLRE.length - 1];
        byte[] evictedChunk = rmsGet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(evictedChunkId, TEXT_IDENTIFICATOR)).toString());
        for (int i2 = 0; i2 < 0; i2++) {
            String importantResource = (String) resourceImportants.elementAt(i2);
            InputStream resourceStream = resourceGetFromBytes(importantResource, evictedChunk, 0);
            if (resourceStream != null) {
                try {
                    resourceAddToRms(importantResource, inkServerGetBytes(resourceStream));
                    resourceStream.close();
                } catch (Exception e) {
                }
            }
        }
        rmsSet(new StringBuffer().append(resourceChunkInRMSBase).append(Integer.toString(evictedChunkId, TEXT_IDENTIFICATOR)).toString(), subchunk);
        int displacedChunk = -1;
        int chunkToInsert = evictedChunkId;
        for (int recencyIndex = 0; recencyIndex < resourceRMSLRE.length && displacedChunk != evictedChunkId; recencyIndex++) {
            displacedChunk = resourceRMSLRE[recencyIndex];
            resourceRMSLRE[recencyIndex] = chunkToInsert;
            chunkToInsert = displacedChunk;
        }
    }

    static byte[] resourceMakeSubChunk() {
        byte[] subchunk = new byte[resourceSCCurrentSize];
        System.arraycopy(resourceSCData, 0, subchunk, 0, resourceSCCurrentSize);
        return subchunk;
    }

    static InputStream resourceGetFromBytes(String name, byte[] chunk, int source) {
        if (chunk == null || name == null) {
            return null;
        }
        int entryOffset = 1;
        for (int entriesRemaining = 255 & chunk[0]; entriesRemaining > 0; entriesRemaining--) {
            int nameLengthOffset = entryOffset;
            int nameOffset = entryOffset + 1;
            int nameLength = 255 & chunk[nameLengthOffset];
            String entryName = new String(chunk, nameOffset, nameLength);
            int dataLengthOffset = nameOffset + nameLength;
            int dataLengthLowOffset = dataLengthOffset + 1;
            int dataOffset = dataLengthLowOffset + 1;
            int dataLength = ((255 & chunk[dataLengthOffset]) << 8) | (255 & chunk[dataLengthLowOffset]);
            if (entryName.equals(name)) {
                if (source != 0) {
                    resourceAddToHeap(chunk, source);
                }
                return new ByteArrayInputStream(chunk, dataOffset, dataLength);
            }
            entryOffset = dataOffset + dataLength;
        }
        return null;
    }

    static byte[] rmsGet(String name) {
        byte[] record = null;
        try {
            RecordStore recordStore = RecordStore.openRecordStore(name, false);
            record = recordStore.getRecord(1);
            recordStore.closeRecordStore();
        } catch (Exception e) {
        }
        return record;
    }

    static boolean rmsSet(String name, byte[] data) {
        if (data == null) {
            return false;
        }
        int length = data.length;
        try {
            RecordStore recordStore = RecordStore.openRecordStore(name, true);
            try {
                recordStore.setRecord(1, data, 0, length);
                recordStore.closeRecordStore();
                return true;
            } catch (Exception e) {
                try {
                    recordStore.addRecord(data, 0, length);
                    recordStore.closeRecordStore();
                    return true;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e3) {
            return false;
        }
    }

    static boolean rmsDelete(String name) {
        try {
            RecordStore.deleteRecordStore(name);
            return true;
        } catch (RecordStoreNotFoundException e2) {
            return true;
        } catch (RecordStoreException e) {
            return false;
        }
    }

    public static void loadSoundMode() {
        try {
            byte[] storedBytes = rmsGet("soundRecordStore");
            if (storedBytes != null) {
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(storedBytes));
                curSoundMode = input.readBoolean();
                input.close();
            }
        } catch (IOException e) {
        }
    }

    public static void saveSoundMode() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeBoolean(curSoundMode);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            dataOutputStream.close();
            rmsSet("soundRecordStore", byteArray);
        } catch (Exception e) {
        }
    }

    public static String getLanguage() throws RecordStoreException {
        String languageId = null;
        try {
            byte[] storedBytes = rmsGet("languageRecordStore");
            if (storedBytes != null) {
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(storedBytes));
                languageId = input.readUTF();
                input.close();
            }
        } catch (IOException e) {
        }
        return languageId;
    }

    public static void saveLanguage(String language_id) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF(language_id);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            dataOutputStream.close();
            rmsSet("languageRecordStore", byteArray);
        } catch (Exception e) {
        }
        curLanguageId = language_id;
    }

    public static boolean loadGameLanguage(String lang) {
        if (gameTexts != null) {
            return true;
        }
        String languagePath = new StringBuffer().append(getGameLangPath()).append(lang).append(".lan").toString();
        DataInputStream input = new DataInputStream(resourceGet(languagePath));
        if (input == null) {
            return false;
        }
        if (realizedExtras.contains(languagePath)) {
            return true;
        }
        gameTexts = null;
        try {
            try {
                int unsignedShort = input.readUnsignedShort();
                gameTexts = new String[unsignedShort];
                for (int i = 0; i < unsignedShort; i++) {
                    gameTexts[i] = input.readUTF();
                }
                try {
                    input.close();
                } catch (Exception e) {
                }
                realizedExtras.addElement(languagePath);
                return true;
            } catch (Exception e2) {
                gameTexts = null;
                try {
                    input.close();
                } catch (Exception e3) {
                }
                return false;
            }
        } catch (Throwable th) {
            try {
                input.close();
            } catch (Exception e4) {
            }
            throw th;
        }
    }

    private static String getGameLangPath() {
        return new StringBuffer().append(gameId).append("/lan/").toString();
    }

    public static String getGameText(String index) {
        try {
            return getGameText(Integer.parseInt(index, TEXT_IDENTIFICATOR));
        } catch (Exception e) {
            return "???";
        }
    }

    public static String getGameText(int index) {
        return (index < 0 || index >= gameTexts.length) ? "???" : gameTexts[index];
    }

    public static Image loadImageFromJAR(String fileName) {
        if (fileName == null) {
            return null;
        }
        System.out.println(new StringBuffer().append("").append(fileName).toString());
        try {
            return Image.createImage(new StringBuffer().append("/").append(fileName).toString());
        } catch (IOException e) {
            return null;
        } catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static String txtStringReplace(String haystack, String needle, String replacement) {
        int matchIndex = haystack.indexOf(needle);
        if (matchIndex > -1) {
            haystack = new StringBuffer().append(haystack.substring(0, matchIndex)).append(replacement).append(haystack.substring(matchIndex + needle.length())).toString();
        }
        return haystack;
    }

    public static String getString(int string_id) {
        if (textLabels == null) {
            if (languages == null || languages.length <= 0) {
                loadLanguage("en");
            } else {
                loadLanguage(languages[0]);
            }
        } else if (textLabels[0] == null) {
            if (languages == null || languages.length <= 0) {
                loadLanguage("en");
            } else {
                loadLanguage(languages[0]);
            }
        }
        return textLabels[string_id];
    }

    public static void loadLanguage(String language) {
        textLabels = new String[185];
        InputStreamReader inputStreamReader = null;
        StringBuffer propertiesBuffer = new StringBuffer(1024);
        try {
            inputStreamReader = new InputStreamReader(propertiesBuffer.getClass().getResourceAsStream(new StringBuffer().append("/localization/").append(language).append(".properties").toString()), "UTF-8");
            char[] characterBuffer = new char[1024];
            while (true) {
                int charactersRead = inputStreamReader.read(characterBuffer, 0, characterBuffer.length);
                if (charactersRead == -1) {
                    break;
                } else {
                    propertiesBuffer.append(characterBuffer, 0, charactersRead);
                }
            }
            inputStreamReader.close();
            String properties = new String(propertiesBuffer);
            StringBuffer labelBuffer = new StringBuffer();
            int labelIndex = 0;
            for (int characterIndex = 0; characterIndex < properties.length(); characterIndex++) {
                char character = properties.charAt(characterIndex);
                if (character != '\r' && character != '\n') {
                    labelBuffer.append(properties.charAt(characterIndex));
                } else if (labelBuffer.length() != 0) {
                    textLabels[labelIndex] = new String(labelBuffer);
                    labelIndex++;
                    labelBuffer = new StringBuffer();
                }
            }
            curLanguageId = language;
            try {
                inputStreamReader.close();
            } catch (Exception e) {
            }
        } catch (Exception e2) {
            try {
                inputStreamReader.close();
            } catch (Exception e3) {
            }
        } catch (Throwable th) {
            try {
                inputStreamReader.close();
            } catch (Exception e4) {
            }
            throw th;
        }
        gameTexts = null;
    }

    static void exit() {
        try {
            runtime = null;
            appInited = false;
            resourceExit();
            midlet.notifyDestroyed();
        } catch (Exception e) {
            midlet.notifyDestroyed();
        }
    }

    static long freeMemory() {
        System.gc();
        return runtime.freeMemory();
    }

    public static void clearAllRMS() {
        resourceClear();
        InkScript.list.clear();
    }

    static void getIndexIni() {
        if (indexIniHash == null) {
            try {
                indexIniHash = readIni(loadChunkINI());
            } catch (IOException e) {
            }
            if (indexIniHash == null) {
                try {
                    indexIniHash = readIni(openJar("index.ini"));
                } catch (IOException e2) {
                }
                if (indexIniHash == null && !loadINIFromServer()) {
                    InkEngine.popupCreate(getString(39), 0);
                    return;
                }
            }
        }
        inkServerGamesOwned = inkServerGetNumOfGames(1);
        inkServerGamesSaved = inkServerGetNumOfGames(2);
    }

    static boolean loadINIFromServer() {
        indexIniHash = httpIniReader((String) InkEngine.settingsHash.get("INK-URL"));
        return indexIniHash != null;
    }

    static DataInputStream loadChunkINI() {
        byte[] storedBytes = rmsGet("RMS_chunkINI");
        DataInputStream input = null;
        if (storedBytes != null) {
            input = new DataInputStream(new ByteArrayInputStream(storedBytes));
        }
        return input;
    }

    static void saveChunkINI(DataInputStream in) {
        try {
            rmsSet("RMS_chunkINI", inkServerGetBytes(in));
        } catch (Exception e) {
        }
    }

    static void loadChunkIDFromRMS() {
        loadedChunksID = new Vector();
        try {
            byte[] storedBytes = rmsGet("RMS_loadChunkID");
            if (storedBytes != null) {
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(storedBytes));
                int chunkCount = input.readInt();
                for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                    loadedChunksID.addElement(input.readUTF());
                }
                input.close();
            }
        } catch (IOException e) {
        }
    }

    static void saveChunkIDInRMS() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(loadedChunksID.size());
            for (int i = 0; i < loadedChunksID.size(); i++) {
                dataOutputStream.writeUTF((String) loadedChunksID.elementAt(i));
            }
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            dataOutputStream.close();
            rmsSet("RMS_loadChunkID", byteArray);
        } catch (Exception e) {
        }
    }

    public static Image createGradientImage(int width, int height, int startColor, int endColor, boolean vertical) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        int startRed = (startColor >> 16) & 255;
        int startGreen = (startColor >> 8) & 255;
        int startBlue = startColor & 255;
        int redDelta = (((endColor >> 16) & 255) - startRed) << 7;
        int greenDelta = (((endColor >> 8) & 255) - startGreen) << 7;
        int blueDelta = ((endColor & 255) - startBlue) << 7;
        Image gradientImage = Image.createImage(width, height);
        Graphics imageGraphics = gradientImage.getGraphics();
        if (vertical) {
            int previousY = 0;
            for (int step = 1; step <= 32; step++) {
                imageGraphics.setColor(((startRed + (((redDelta * step) >> 5) >> 7)) << 16) + ((startGreen + (((greenDelta * step) >> 5) >> 7)) << 8) + startBlue + (((blueDelta * step) >> 5) >> 7));
                int nextY = (((height << 7) * step) >> 5) >> 7;
                imageGraphics.fillRect(0, previousY, width, nextY - previousY);
                previousY = nextY;
            }
        } else {
            int previousX = 0;
            for (int step = 1; step <= 32; step++) {
                imageGraphics.setColor(((startRed + (((redDelta * step) >> 5) >> 7)) << 16) + ((startGreen + (((greenDelta * step) >> 5) >> 7)) << 8) + startBlue + (((blueDelta * step) >> 5) >> 7));
                int nextX = (((width << 7) * step) >> 5) >> 7;
                imageGraphics.fillRect(previousX, 0, nextX - previousX, height);
                previousX = nextX;
            }
        }
        return Image.createImage(gradientImage);
    }

    public static String loadRequest_getResourcePath(int type, String id) {
        return loadRequest_getResourcePath(type, 0, id, 0);
    }

    public static String loadRequest_getResourcePath(Object id, int imageTransform) {
        try {
            return loadRequest_getResourcePath(3, ((Integer) id).intValue(), null, 0);
        } catch (Exception e) {
            return loadRequest_getResourcePath(2, -1, (String) id, 0);
        }
    }

    public static String loadRequest_getResourcePath(int type, int idInt, String idString, int imageTrans) {
        switch (type) {
            case 1:
                return new StringBuffer().append(gameId).append("/scr/").append(idString).append(".bin").toString();
            case 2:
                return new StringBuffer().append(gameId).append("/gfx/transform").append(imageTrans).append("/").append(idString).append(".png").toString();
            case 3:
                return new StringBuffer().append(gameId).append("/gfx/transform").append(imageTrans).append("/").append(idInt).append(".png").toString();
            case 4:
                return new StringBuffer().append(gameId).append("/sfx/").append("mid").append("/").append(idString).append(".").append("mid").toString();
            case 5:
                return new StringBuffer().append(gameId).append("/rom/").append(idString).append(".bin").toString();
            default:
                return null;
        }
    }
}
