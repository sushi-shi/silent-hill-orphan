package javax.microedition.midlet;

public abstract class MIDlet {
    public static MIDlet oraclePropertyReceiver;
    public static String oraclePropertyKey;
    public static String oraclePropertyValue;
    public static int oraclePropertyCalls;
    public static boolean oraclePropertyFails;
    public static MIDlet oracleNotifyDestroyedFirstReceiver;
    public static MIDlet oracleNotifyDestroyedSecondReceiver;
    public static int oracleNotifyDestroyedCalls;
    public static int oracleNotifyDestroyedMode;
    public static Runnable oracleNotifyDestroyedFirstHook;
    public static Runnable oracleNotifyDestroyedSecondHook;

    protected MIDlet() {}

    protected abstract void startApp() throws MIDletStateChangeException;

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final String getAppProperty(String key) {
        oraclePropertyCalls++;
        oraclePropertyReceiver = this;
        oraclePropertyKey = key;
        if (oraclePropertyFails) {
            throw new NullPointerException("injected getAppProperty failure");
        }
        return oraclePropertyValue;
    }

    public static void oracleResetProperty(String value, boolean fails) {
        oraclePropertyReceiver = null;
        oraclePropertyKey = null;
        oraclePropertyValue = value;
        oraclePropertyCalls = 0;
        oraclePropertyFails = fails;
    }

    public final void notifyDestroyed() {
        oracleNotifyDestroyedCalls++;
        if (oracleNotifyDestroyedCalls == 1) {
            oracleNotifyDestroyedFirstReceiver = this;
            if (oracleNotifyDestroyedFirstHook != null) {
                oracleNotifyDestroyedFirstHook.run();
            }
        } else {
            oracleNotifyDestroyedSecondReceiver = this;
            if (oracleNotifyDestroyedSecondHook != null) {
                oracleNotifyDestroyedSecondHook.run();
            }
        }
        if (oracleNotifyDestroyedMode == 1) {
            throw new NullPointerException("injected notifyDestroyed exception");
        }
        if (oracleNotifyDestroyedMode == 2 && oracleNotifyDestroyedCalls == 1) {
            throw new NullPointerException("injected first notifyDestroyed exception");
        }
        if (oracleNotifyDestroyedMode == 3 && oracleNotifyDestroyedCalls == 1) {
            throw new AssertionError("injected first notifyDestroyed error");
        }
        if (oracleNotifyDestroyedMode == 4) {
            if (oracleNotifyDestroyedCalls == 1) {
                throw new NullPointerException("injected first notifyDestroyed exception");
            }
            throw new AssertionError("injected second notifyDestroyed error");
        }
    }

    public static void oracleResetNotifyDestroyed(
            int mode, Runnable firstHook, Runnable secondHook) {
        oracleNotifyDestroyedFirstReceiver = null;
        oracleNotifyDestroyedSecondReceiver = null;
        oracleNotifyDestroyedCalls = 0;
        oracleNotifyDestroyedMode = mode;
        oracleNotifyDestroyedFirstHook = firstHook;
        oracleNotifyDestroyedSecondHook = secondHook;
    }

    public final void notifyPaused() {}

    public final void resumeRequest() {}

    public final boolean platformRequest(String url) {
        return false;
    }
}
