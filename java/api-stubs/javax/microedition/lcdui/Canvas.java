package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public interface OracleRepaintApplication {
        void setCanvas(Canvas canvas);

        void setPainting(boolean painting);

        void repaintCanvasIfPossible();
    }

    public static final class OracleRepaintFailure extends RuntimeException {
        public OracleRepaintFailure() {
            super("injected repaint failure");
        }
    }

    public static final class OracleServiceRepaintsFailure extends RuntimeException {
        public OracleServiceRepaintsFailure() {
            super("injected serviceRepaints failure");
        }
    }

    public static Canvas oracleFullScreenReceiver;
    public static int oracleFullScreenCalls;
    public static boolean oracleFullScreenMode;
    public static boolean oracleFailFullScreen;
    public static Canvas oracleRepaintCanvas1;
    public static Canvas oracleRepaintCanvas2;
    public static Canvas oracleRepaintCanvas3;
    public static int oracleRepaintMode;
    public static int oracleServiceRepaintsMode;
    public static StringBuffer oracleRepaintTrace = new StringBuffer();
    public static OracleRepaintApplication oracleRepaintApplication;

    public static final int UP = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int DOWN = 6;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;
    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;

    protected Canvas() {}

    protected abstract void paint(Graphics graphics);

    protected void keyPressed(int keyCode) {}

    protected void keyReleased(int keyCode) {}

    protected void keyRepeated(int keyCode) {}

    protected void hideNotify() {}

    protected void showNotify() {}

    public int getGameAction(int keyCode) {
        return 0;
    }

    public int getWidth() {
        return 128;
    }

    public int getHeight() {
        return 128;
    }

    private static void oracleRecordRepaintCall(String operation, Canvas receiver) {
        if (oracleRepaintTrace.length() != 0) {
            oracleRepaintTrace.append(',');
        }
        int id = receiver == oracleRepaintCanvas1 ? 1
                : receiver == oracleRepaintCanvas2 ? 2
                : receiver == oracleRepaintCanvas3 ? 3 : -1;
        oracleRepaintTrace.append(operation).append(id);
    }

    public final void repaint() {
        oracleRecordRepaintCall("R", this);
        switch (oracleRepaintMode) {
            case 1:
                throw new OracleRepaintFailure();
            case 2:
                oracleRepaintApplication.setCanvas(null);
                break;
            case 3:
                oracleRepaintApplication.setCanvas(oracleRepaintCanvas2);
                break;
            case 4:
                oracleRepaintApplication.setPainting(false);
                break;
            case 5:
                oracleRepaintApplication.setCanvas(oracleRepaintCanvas2);
                oracleRepaintApplication.setPainting(false);
                break;
            case 6:
                oracleRepaintApplication.repaintCanvasIfPossible();
                break;
            default:
                break;
        }
    }

    public final void serviceRepaints() {
        oracleRecordRepaintCall("S", this);
        switch (oracleServiceRepaintsMode) {
            case 1:
                throw new OracleServiceRepaintsFailure();
            case 2:
                oracleRepaintApplication.setCanvas(null);
                break;
            case 3:
                oracleRepaintApplication.setCanvas(oracleRepaintCanvas3);
                break;
            case 4:
                oracleRepaintApplication.setPainting(false);
                break;
            default:
                break;
        }
    }

    public static void oracleResetRepaint(
            Canvas canvas1,
            Canvas canvas2,
            Canvas canvas3,
            int repaintMode,
            int serviceRepaintsMode,
            OracleRepaintApplication application) {
        oracleRepaintCanvas1 = canvas1;
        oracleRepaintCanvas2 = canvas2;
        oracleRepaintCanvas3 = canvas3;
        oracleRepaintMode = repaintMode;
        oracleServiceRepaintsMode = serviceRepaintsMode;
        oracleRepaintTrace = new StringBuffer();
        oracleRepaintApplication = application;
    }

    public void setFullScreenMode(boolean mode) {
        oracleFullScreenCalls++;
        oracleFullScreenReceiver = this;
        oracleFullScreenMode = mode;
        if (oracleFailFullScreen) {
            throw new NullPointerException("injected setFullScreenMode failure");
        }
    }

    public static void oracleResetFullScreen(boolean fail) {
        oracleFullScreenReceiver = null;
        oracleFullScreenCalls = 0;
        oracleFullScreenMode = false;
        oracleFailFullScreen = fail;
    }
}
