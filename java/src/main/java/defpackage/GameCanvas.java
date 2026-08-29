package defpackage;

import java.io.DataInputStream;
import java.io.InputStream;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/** Named reconstruction of original class {@code MyCanvas}. */
public class GameCanvas extends javax.microedition.lcdui.game.GameCanvas {
    static final int[] transformTable = {0, 2, 5, 7, 3, 1, 6, 4};
    static Player gPlayer;
    static String soundID;
    static int loopCount;
    static final String SOUND_FORMAT = "mid";
    static final String AUDIO_TYPE = "audio/midi";
    static InputStream shotSound;
    static final int KEY_NOT_SPECIFIED = 0;
    static final int KEY_UP = -1;
    static final int KEY_DOWN = -2;
    static final int KEY_LEFT = -3;
    static final int KEY_RIGHT = -4;
    static final int KEY_MIDDLE_SOFT = -5;
    static final int KEY_LEFT_SOFT = -6;
    static final int KEY_RIGHT_SOFT = -7;
    static final int KEY_ERASE = -8;
    static final int KEY_SEND = -10;
    static final int KEY_RETURN = -11;
    static int keySoftkeyLeft;
    static int keySoftkeyRight;
    static int keySend;
    static int keyReturn;
    static int keySoftkeyCenter;
    static int keyArrowUp;
    static int keyArrowDown;
    static int keyArrowLeft;
    static int keyArrowRight;
    static int keyErase;

    protected GameCanvas() {
        super(false);
        setFullScreenMode(true);
    }

    public void paint(Graphics g) {
        Application.paint(g);
    }

    protected void hideNotify() {
        Application.keyDown = 0;
        Application.keyLastPressed = 0;
        Application.keyNew = false;
        Application.keyPressed = false;
        Application.setHide(true);
        stopSound();
        if (InkEngine.textFadePausedTime == -1) {
            InkEngine.textFadePausedTime = System.currentTimeMillis() - InkEngine.textFadeStartTime;
        }
    }

    protected void showNotify() {
        Application.setHide(false);
        resumeSound();
    }

    protected void keyPressed(int key) {
        if (Application.loadingMode != KEY_UP || Application.loadBarActive || Application.gotoDissolveFXCounter > KEY_LEFT) {
            return;
        }
        Application.setKeyStatus(keyConvertToKeyId(key), true);
    }

    protected void keyReleased(int key) {
        if (Application.loadingMode != KEY_UP || Application.loadBarActive || Application.gotoDissolveFXCounter > KEY_LEFT) {
            return;
        }
        Application.setKeyStatus(keyConvertToKeyId(key), false);
    }

    static void playSound(String id, int loops) {
        InputStream soundStream;
        if (InkEngine.FirstLoad && id.equals("ls")) {
            InkEngine.FirstLoad = !InkEngine.FirstLoad;
            return;
        }
        if (Application.curSoundMode) {
            try {
                soundID = null;
                loopCount = 0;
                stopSound();
                if (id.equals("sh_shot")) {
                    if (shotSound == null) {
                        shotSound = Application.resourceGet(new StringBuffer().append(Application.gameId).append("/sfx/").append(SOUND_FORMAT).append("/").append(id).append(".").append(SOUND_FORMAT).toString());
                    }
                    shotSound.reset();
                    soundStream = new DataInputStream(shotSound);
                } else {
                    soundStream = Application.resourceGet(new StringBuffer().append(Application.gameId).append("/sfx/").append(SOUND_FORMAT).append("/").append(id).append(".").append(SOUND_FORMAT).toString());
                }
                gPlayer = Manager.createPlayer(soundStream, AUDIO_TYPE);
                gPlayer.setLoopCount(loops);
                try {
                    gPlayer.prefetch();
                    gPlayer.realize();
                } catch (Exception e) {
                }
                try {
                    gPlayer.start();
                } catch (Exception e2) {
                }
                if (loops != 1 && loops == KEY_UP) {
                    soundID = id;
                    loopCount = KEY_UP;
                }
                soundStream.close();
            } catch (Exception e3) {
            }
        }
    }

    static void resumeSound() {
        if (Application.curSoundMode && loopCount == KEY_UP) {
            playSound(soundID, loopCount);
        }
    }

    static void stopSound() {
        if (gPlayer != null) {
            try {
                gPlayer.stop();
                gPlayer.close();
            } catch (Exception e) {
            }
            gPlayer = null;
        }
    }

    static void keyInit() {
        keySoftkeyLeft = Application.toInt(InkEngine.settingsHash.get("KEY_SOFTKEY_LEFT"));
        keySoftkeyRight = Application.toInt(InkEngine.settingsHash.get("KEY_SOFTKEY_RIGHT"));
        keySend = Application.toInt(InkEngine.settingsHash.get("KEY_SEND"));
        keyReturn = Application.toInt(InkEngine.settingsHash.get("KEY_END"));
        keySoftkeyCenter = Application.toInt(InkEngine.settingsHash.get("KEY_SOFTKEY_THUMBSTICK"));
        keyArrowUp = Application.toInt(InkEngine.settingsHash.get("KEY_UP_ARROW"));
        keyArrowDown = Application.toInt(InkEngine.settingsHash.get("KEY_DOWN_ARROW"));
        keyArrowLeft = Application.toInt(InkEngine.settingsHash.get("KEY_LEFT_ARROW"));
        keyArrowRight = Application.toInt(InkEngine.settingsHash.get("KEY_RIGHT_ARROW"));
        keyErase = Application.toInt(InkEngine.settingsHash.get("KEY_CLEAR"));
    }

    private static int keyJadEntryAsInt(String jadEntry) {
        try {
            return Integer.parseInt(Application.midlet.getAppProperty(jadEntry));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int keyConvertToKeyId(int keyCode) {
        if (keyCode == 0) {
            return 0;
        }
        if (keyCode == keySoftkeyLeft) {
            return KEY_LEFT_SOFT;
        }
        if (keyCode == keySoftkeyRight) {
            return KEY_RIGHT_SOFT;
        }
        if (keyCode == keySoftkeyCenter || keyCode == 53) {
            return KEY_MIDDLE_SOFT;
        }
        if (keyCode == keyArrowUp || keyCode == 121 || keyCode == 116 || keyCode == 50) {
            return KEY_UP;
        }
        if (keyCode == keyArrowDown || keyCode == 98 || keyCode == 118 || keyCode == 56) {
            return KEY_DOWN;
        }
        if (keyCode == keyArrowLeft || keyCode == 102 || keyCode == 100 || keyCode == 52) {
            return KEY_LEFT;
        }
        if (keyCode == keyArrowRight || keyCode == 106 || keyCode == 107 || keyCode == 54) {
            return KEY_RIGHT;
        }
        if (keyCode == keyReturn || keyCode == keyErase) {
            return 0;
        }
        if (keyCode == keySend) {
            return KEY_SEND;
        }
        if ((keyCode >= 48 && keyCode <= 57) || keyCode == 42 || keyCode == 35) {
            return keyCode;
        }
        return 0;
    }
}
