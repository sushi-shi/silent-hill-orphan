package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public abstract class GameCanvas extends Canvas {
    protected GameCanvas(boolean suppressKeyEvents) {}

    public Graphics getGraphics() {
        return null;
    }

    public void flushGraphics() {}

    public void flushGraphics(int x, int y, int width, int height) {}

    public int getKeyStates() {
        return 0;
    }
}
