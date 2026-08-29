package javax.microedition.midlet;

public abstract class MIDlet {
    public static MIDlet oraclePropertyReceiver;
    public static String oraclePropertyKey;
    public static String oraclePropertyValue;
    public static int oraclePropertyCalls;
    public static boolean oraclePropertyFails;

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

    public final void notifyDestroyed() {}

    public final void notifyPaused() {}

    public final void resumeRequest() {}

    public final boolean platformRequest(String url) {
        return false;
    }
}
