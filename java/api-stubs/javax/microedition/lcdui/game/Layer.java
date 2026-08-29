package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;

/** Declaration-compatible JSR-118 layer stub used only by Java authorities. */
public abstract class Layer {
    protected Layer(int width, int height) {
    }

    public void setPosition(int x, int y) {
    }

    public abstract void paint(Graphics graphics);
}
