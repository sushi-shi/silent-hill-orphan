package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public abstract class GameCanvas extends Canvas {
    public static GameCanvas oracleConstructorReceiver;
    public static int oracleConstructorCalls;
    public static boolean oracleSuppressKeyEvents;
    public static boolean oracleFailConstructor;

    protected GameCanvas(boolean suppressKeyEvents) {
        oracleConstructorCalls++;
        oracleConstructorReceiver = this;
        oracleSuppressKeyEvents = suppressKeyEvents;
        if (oracleFailConstructor) {
            throw new NullPointerException("injected GameCanvas constructor failure");
        }
    }

    public static void oracleResetConstructor(boolean fail) {
        oracleConstructorReceiver = null;
        oracleConstructorCalls = 0;
        oracleSuppressKeyEvents = true;
        oracleFailConstructor = fail;
    }

    public Graphics getGraphics() {
        return null;
    }

    public void flushGraphics() {}

    public void flushGraphics(int x, int y, int width, int height) {}

    public int getKeyStates() {
        return 0;
    }
}
