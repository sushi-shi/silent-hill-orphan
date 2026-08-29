package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;

public class Display {
    public static MIDlet oracleMidlet;
    public static Displayable oracleCurrent;
    public static Display oracleIssuedDisplay;
    public static Display oracleSetCurrentReceiver;
    public static int oracleGetDisplayCalls;
    public static int oracleSetCurrentCalls;
    public static int oracleGetDisplayMode;
    public static int oracleSetCurrentMode;

    private Display() {}

    public static Display getDisplay(MIDlet midlet) {
        oracleGetDisplayCalls++;
        oracleMidlet = midlet;
        if (oracleGetDisplayMode == 2) {
            throw new NullPointerException("injected getDisplay failure");
        }
        if (oracleGetDisplayMode == 1) {
            return null;
        }
        oracleIssuedDisplay = new Display();
        return oracleIssuedDisplay;
    }

    public void setCurrent(Displayable displayable) {
        oracleSetCurrentCalls++;
        oracleSetCurrentReceiver = this;
        oracleCurrent = displayable;
        if (oracleSetCurrentMode != 0) {
            throw new NullPointerException("injected setCurrent failure");
        }
    }

    public static void oracleReset(int getDisplayMode, int setCurrentMode) {
        oracleMidlet = null;
        oracleCurrent = null;
        oracleIssuedDisplay = null;
        oracleSetCurrentReceiver = null;
        oracleGetDisplayCalls = 0;
        oracleSetCurrentCalls = 0;
        oracleGetDisplayMode = getDisplayMode;
        oracleSetCurrentMode = setCurrentMode;
    }

    public Displayable getCurrent() {
        return null;
    }
}
