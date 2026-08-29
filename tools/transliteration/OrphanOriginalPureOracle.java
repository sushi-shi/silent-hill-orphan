package defpackage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

/** Calls package-private static methods on the verified original {@code M}. */
public final class OrphanOriginalPureOracle {
    private static final class Handle {
        final int id;

        Handle(int id) {
            this.id = id;
        }
    }

    private static final class RecordingThread extends Thread {
        final boolean fail;
        int attempts;

        RecordingThread(boolean fail) {
            this.fail = fail;
        }

        public synchronized void start() {
            this.attempts++;
            if (this.fail) {
                throw new NullPointerException("injected start failure");
            }
        }
    }

    private static final class FixedRandom extends java.util.Random {
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        public int nextInt() {
            return this.value;
        }
    }

    private static final class RecordingOutputStream extends java.io.OutputStream {
        final Vector attempts = new Vector();
        final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        final int failAt;

        RecordingOutputStream(int failAt) {
            this.failAt = failAt;
        }

        public void write(int written) throws java.io.IOException {
            int attempt = this.attempts.size();
            this.attempts.addElement(new Integer(written));
            if (attempt == this.failAt) {
                throw new java.io.IOException("injected write failure");
            }
            this.bytes.write(written);
        }
    }

    private static final class FailingInputStream extends java.io.InputStream {
        final int mode;

        FailingInputStream(int mode) {
            this.mode = mode;
        }

        public int available() throws java.io.IOException {
            if (this.mode == 3) {
                throw new java.io.IOException("injected available failure");
            }
            return 0;
        }

        public int read() throws java.io.IOException {
            throw new java.io.IOException("injected read failure");
        }
    }

    private static final class RecordingGraphics extends Graphics {
        final Image expectedImage;
        final boolean fail;
        String attemptKind;
        String attempt;

        RecordingGraphics(Image expectedImage, boolean fail) {
            this.expectedImage = expectedImage;
            this.fail = fail;
        }

        public void drawImage(Image image, int x, int y, int anchor) {
            String imageId = image == null ? "n" : image == this.expectedImage ? "7" : "WRONG";
            this.attemptKind = "I";
            this.attempt = "9:" + imageId + ":" + Integer.toString(x) + ":"
                    + Integer.toString(y) + ":" + Integer.toString(anchor);
            if (this.fail) {
                throw new NullPointerException("injected draw failure");
            }
        }

        public void drawRegion(Image image, int sourceX, int sourceY, int width, int height,
                int transform, int destinationX, int destinationY, int anchor) {
            String imageId = image == null ? "n" : image == this.expectedImage ? "7" : "WRONG";
            this.attemptKind = "R";
            this.attempt = "9:" + imageId + ":" + Integer.toString(sourceX) + ":"
                    + Integer.toString(sourceY) + ":" + Integer.toString(width) + ":"
                    + Integer.toString(height) + ":" + Integer.toString(transform) + ":"
                    + Integer.toString(destinationX) + ":" + Integer.toString(destinationY)
                    + ":" + Integer.toString(anchor);
            if (this.fail) {
                throw new NullPointerException("injected draw failure");
            }
        }
    }

    private static String paintSimpleOutput(String status, RecordingGraphics graphics) {
        return status + ":" + (graphics == null || graphics.attempt == null
                ? "null" : graphics.attempt);
    }

    private static String paintOutput(String status, RecordingGraphics graphics) {
        return status + ":" + (graphics == null || graphics.attempt == null
                ? "null" : graphics.attemptKind + ":" + graphics.attempt);
    }

    private static int value(String[] parts, int index) {
        return Integer.parseInt(parts[index]);
    }

    private static byte[] bytes(String token) {
        if (token.equals("null")) {
            return null;
        }
        if (token.equals("-")) {
            return new byte[0];
        }
        byte[] result = new byte[(token.length() - 1) / 2];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(token.substring(1 + index * 2, 3 + index * 2), 16);
        }
        return result;
    }

    private static byte[][] byteArrays(String token) {
        if (token.equals("null")) {
            return null;
        }
        if (token.equals("-")) {
            return new byte[0][];
        }
        if (!token.startsWith("a")) {
            throw new IllegalArgumentException("invalid byte-array token: " + token);
        }
        String[] values = token.substring(1).split(",");
        byte[][] result = new byte[values.length][];
        for (int index = 0; index < result.length; index++) {
            result[index] = bytes(values[index]);
        }
        return result;
    }

    private static String utf16(String token) {
        if (token.equals("null")) {
            return null;
        }
        if (token.equals("-")) {
            return "";
        }
        char[] result = new char[(token.length() - 1) / 4];
        for (int index = 0; index < result.length; index++) {
            result[index] = (char) Integer.parseInt(token.substring(1 + index * 4, 5 + index * 4), 16);
        }
        return new String(result);
    }

    private static String utf16Output(String value) {
        if (value == null) {
            return "null";
        }
        if (value.length() == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("u");
        for (int index = 0; index < value.length(); index++) {
            String hex = Integer.toHexString(value.charAt(index));
            for (int padding = hex.length(); padding < 4; padding++) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }

    private static String bytesOutput(byte[] value) {
        if (value.length == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("h");
        for (int index = 0; index < value.length; index++) {
            int unsigned = value[index] & 255;
            if (unsigned < 16) {
                result.append('0');
            }
            result.append(Integer.toHexString(unsigned));
        }
        return result.toString();
    }

    private static String writeOutput(String status, RecordingOutputStream output) {
        if (output == null) {
            return status + ":null:null";
        }
        StringBuffer attempts = new StringBuffer();
        if (output.attempts.size() == 0) {
            attempts.append('-');
        } else {
            attempts.append('i');
            for (int index = 0; index < output.attempts.size(); index++) {
                if (index != 0) {
                    attempts.append(',');
                }
                attempts.append(((Integer) output.attempts.elementAt(index)).intValue());
            }
        }
        return status + ":" + attempts.toString() + ":"
                + bytesOutput(output.bytes.toByteArray());
    }

    private static int[] ints(String token) {
        if (token.equals("null")) {
            return null;
        }
        if (token.equals("-")) {
            return new int[0];
        }
        String[] values = token.substring(1).split(",");
        int[] result = new int[values.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = Integer.parseInt(values[index]);
        }
        return result;
    }

    private static String[] scriptIds(String token) {
        if (token.equals("null")) {
            return null;
        }
        if (token.equals("-")) {
            return new String[0];
        }
        String[] tokens = token.substring(1).split(",");
        String[] result = new String[tokens.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = utf16(tokens[index]);
        }
        return result;
    }

    private static String scriptIdsOutput(String[] values) {
        if (values == null) {
            return "null";
        }
        if (values.length == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("s");
        for (int index = 0; index < values.length; index++) {
            if (index != 0) {
                result.append(',');
            }
            result.append(utf16Output(values[index]));
        }
        return result.toString();
    }

    private static Vector menuStack(String token, Object[] menus) {
        if (token.equals("null")) {
            return null;
        }
        Vector result = new Vector();
        if (token.equals("-")) {
            return result;
        }
        if (!token.startsWith("m")) {
            throw new IllegalArgumentException("invalid menu stack token: " + token);
        }
        String[] handles = token.substring(1).split(",");
        for (int index = 0; index < handles.length; index++) {
            result.addElement(menus[Integer.parseInt(handles[index])]);
        }
        return result;
    }

    private static String menuStackOutput(Vector stack, Object[] menus) {
        if (stack == null) {
            return "null";
        }
        if (stack.size() == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("m");
        for (int stackIndex = 0; stackIndex < stack.size(); stackIndex++) {
            if (stackIndex != 0) {
                result.append(',');
            }
            Object entry = stack.elementAt(stackIndex);
            int handle = -1;
            for (int menuIndex = 0; menuIndex < menus.length; menuIndex++) {
                if (entry == menus[menuIndex]) {
                    handle = menuIndex;
                    break;
                }
            }
            result.append(handle);
        }
        return result.toString();
    }

    private static String menuFlagsOutput(Object[] menus, Field isCurrent) throws Exception {
        StringBuffer result = new StringBuffer("b");
        for (int index = 0; index < menus.length; index++) {
            result.append(isCurrent.getBoolean(menus[index]) ? '1' : '0');
        }
        return result.toString();
    }

    private static Object gameResourceId(String token, Object[] opaqueIds) {
        if (token.equals("n")) {
            return null;
        }
        switch (token.charAt(0)) {
            case 'i':
                return new Integer(Integer.parseInt(token.substring(1)));
            case 's':
                return utf16(token.substring(1));
            case 'o':
                return opaqueIds[Integer.parseInt(token.substring(1))];
            default:
                throw new IllegalArgumentException("invalid game-resource ID: " + token);
        }
    }

    private static String gameResourceIdOutput(Object id, Object[] opaqueIds) {
        if (id == null) {
            return "n";
        }
        if (id instanceof Integer) {
            return "i" + id.toString();
        }
        if (id instanceof String) {
            return "s" + utf16Output((String) id);
        }
        for (int index = 0; index < opaqueIds.length; index++) {
            if (id == opaqueIds[index]) {
                return "o" + Integer.toString(index);
            }
        }
        return "WRONG";
    }

    private static Hashtable stringTable(String token) {
        if (token.equals("null")) {
            return null;
        }
        Hashtable result = new Hashtable();
        if (token.equals("-")) {
            return result;
        }
        String[] entries = token.substring(1).split(",");
        for (int index = 0; index < entries.length; index++) {
            int equals = entries[index].indexOf('=');
            result.put(utf16(entries[index].substring(0, equals)),
                    utf16(entries[index].substring(equals + 1)));
        }
        return result;
    }

    private static Hashtable settingsTable(String token) {
        if (token.equals("null")) {
            return null;
        }
        Hashtable result = new Hashtable();
        if (token.equals("-")) {
            return result;
        }
        String[] entries = token.substring(1).split(",");
        for (int index = 0; index < entries.length; index++) {
            int equals = entries[index].indexOf('=');
            String encoded = entries[index].substring(equals + 1);
            Object setting;
            if (encoded.charAt(0) == 'i') {
                setting = Integer.valueOf(encoded.substring(1));
            } else if (encoded.charAt(0) == 's') {
                setting = utf16(encoded.substring(1));
            } else if (encoded.equals("o")) {
                setting = new Handle(index);
            } else {
                throw new IllegalArgumentException("invalid setting token: " + encoded);
            }
            result.put(utf16(entries[index].substring(0, equals)), setting);
        }
        return result;
    }

    private static String stringTableOutput(Hashtable table) {
        if (table == null) {
            return "null";
        }
        if (table.size() == 0) {
            return "-";
        }
        String[] keys = new String[table.size()];
        int count = 0;
        Enumeration enumeration = table.keys();
        while (enumeration.hasMoreElements()) {
            keys[count++] = (String) enumeration.nextElement();
        }
        for (int outer = 0; outer < keys.length; outer++) {
            for (int inner = outer + 1; inner < keys.length; inner++) {
                if (keys[outer].compareTo(keys[inner]) > 0) {
                    String swap = keys[outer];
                    keys[outer] = keys[inner];
                    keys[inner] = swap;
                }
            }
        }
        StringBuffer result = new StringBuffer("m");
        for (int index = 0; index < keys.length; index++) {
            if (index != 0) {
                result.append(',');
            }
            result.append(utf16Output(keys[index]));
            result.append('=');
            result.append(utf16Output((String) table.get(keys[index])));
        }
        return result.toString();
    }

    private static String mutationOutput(String status, Hashtable variables,
            Hashtable hints, boolean changed) {
        return status + ":" + stringTableOutput(variables) + ":"
                + stringTableOutput(hints) + ":" + (changed ? "1" : "0");
    }

    private static String variableOutput(Object value) {
        if (value == null) {
            return "N";
        }
        if (value instanceof Integer) {
            return "I:" + Integer.toString(((Integer) value).intValue());
        }
        return "S:" + utf16Output((String) value);
    }

    private static Object executionValue(String kind, String token) {
        if (kind.equals("n")) {
            return null;
        }
        if (kind.equals("i")) {
            return new Integer(Integer.parseInt(token));
        }
        if (kind.equals("s")) {
            return utf16(token);
        }
        throw new IllegalArgumentException("invalid execution value kind: " + kind);
    }

    private static String pausedThreadOutput(Object paused, Object self, Object other) {
        if (paused == null) {
            return "N";
        }
        if (paused == self) {
            return "S";
        }
        return paused == other ? "O" : "WRONG";
    }

    private static String executionOutput(String outcome, Object result,
            Object interpreter, Object other, Field interpreterStatus,
            Field interpreterOffset, Field pausedThread) throws Exception {
        return outcome + ":" + variableOutput(result) + ":"
                + Integer.toString(interpreterStatus.getInt(interpreter)) + ":"
                + Integer.toString(interpreterOffset.getInt(interpreter)) + ":"
                + pausedThreadOutput(pausedThread.get(null), interpreter, other);
    }

    private static String eventExecutionOutput(String outcome, Object result,
            Object script, Object roomObject, Object oldPaused, Field pausedThread,
            Field interpreterStatus, Field interpreterOffset, Field interpreterScript,
            Field interpreterRoomObject, Field interpreterLanguageDebugMode) throws Exception {
        Object paused = pausedThread.get(null);
        if (paused == null) {
            return outcome + ":" + variableOutput(result) + ":N:N:N:N:N:N";
        }
        Object pausedScript = interpreterScript.get(paused);
        Object pausedRoomObject = interpreterRoomObject.get(paused);
        return outcome + ":" + variableOutput(result) + ":"
                + (paused == oldPaused ? "O:" : "I:")
                + Integer.toString(interpreterStatus.getInt(paused)) + ":"
                + Integer.toString(interpreterOffset.getInt(paused)) + ":"
                + (pausedScript == null ? "N:" : pausedScript == script ? "S:" : "WRONG:")
                + (pausedRoomObject == null ? "N:"
                        : pausedRoomObject == roomObject ? "R:" : "WRONG:")
                + (interpreterLanguageDebugMode.getBoolean(paused) ? "1" : "0");
    }

    private static Vector choiceIds(String token) {
        if (token.equals("null")) {
            return null;
        }
        Vector result = new Vector();
        if (token.equals("-")) {
            return result;
        }
        String[] values = token.substring(1).split(",");
        for (int index = 0; index < values.length; index++) {
            result.addElement(values[index].equals("n")
                    ? null : new Handle(Integer.parseInt(values[index])));
        }
        return result;
    }

    private static String choiceOutput(Object value) {
        return value == null ? "NULL" : Integer.toString(((Handle) value).id);
    }

    private static String choiceIdsOutput(Vector values) {
        if (values == null) {
            return "null";
        }
        if (values.size() == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("o");
        for (int index = 0; index < values.size(); index++) {
            if (index != 0) {
                result.append(',');
            }
            Object value = values.elementAt(index);
            if (value == null) {
                result.append('n');
            } else if (value instanceof Integer) {
                result.append(((Integer) value).intValue());
            } else {
                result.append(((Handle) value).id);
            }
        }
        return result.toString();
    }

    private static Vector choiceTexts(String token) {
        String[] values = scriptIds(token);
        if (values == null) {
            return null;
        }
        Vector result = new Vector();
        for (int index = 0; index < values.length; index++) {
            result.addElement(values[index]);
        }
        return result;
    }

    private static String choiceTextsOutput(Vector values) {
        if (values == null) {
            return "null";
        }
        if (values.size() == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("s");
        for (int index = 0; index < values.size(); index++) {
            if (index != 0) {
                result.append(',');
            }
            result.append(utf16Output((String) values.elementAt(index)));
        }
        return result.toString();
    }

    private static String menuAddOutput(String status, Object menu,
            Field choiceIds, Field choiceTexts, Field updateBodyLines,
            Field updateMenu) throws Exception {
        return status + ":" + choiceIdsOutput((Vector) choiceIds.get(menu)) + ":"
                + choiceTextsOutput((Vector) choiceTexts.get(menu)) + ":"
                + (updateBodyLines.getBoolean(menu) ? "1:" : "0:")
                + (updateMenu.getBoolean(menu) ? "1" : "0");
    }

    private static String intsOutput(int[] values) {
        if (values == null) {
            return "null";
        }
        if (values.length == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("i");
        for (int index = 0; index < values.length; index++) {
            if (index != 0) {
                result.append(',');
            }
            result.append(values[index]);
        }
        return result.toString();
    }

    private static String popupTextsOutput(String[][] values) {
        if (values == null) {
            return "null";
        }
        if (values.length == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("p");
        for (int index = 0; index < values.length; index++) {
            if (index != 0) {
                result.append(',');
            }
            String[] lines = values[index];
            if (lines == null) {
                result.append('n');
            } else if (lines.length == 0) {
                result.append('e');
            } else {
                result.append('a');
                for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                    if (lineIndex != 0) {
                        result.append('+');
                    }
                    result.append(utf16Output(lines[lineIndex]));
                }
            }
        }
        return result.toString();
    }

    private static String requestOutput(Object request, Field requestType,
            Field requestIntegerId, Field requestStringId, Field requestImageTransform)
            throws Exception {
        return Integer.toString(requestType.getInt(request)) + ":"
                + Integer.toString(requestIntegerId.getInt(request)) + ":"
                + utf16Output((String) requestStringId.get(request)) + ":"
                + Integer.toString(requestImageTransform.getInt(request));
    }

    private static String requestIdOutput(Object value) {
        return value instanceof Integer
                ? "I:" + Integer.toString(((Integer) value).intValue())
                : "S:" + utf16Output((String) value);
    }

    private static String inputRemaining(DataInputStream input) throws Exception {
        return input == null ? "N" : Integer.toString(input.available());
    }

    private static String inkScriptGfxOutput(Object value) {
        if (value == null) {
            return "N";
        }
        if (value instanceof Integer) {
            return "I," + Integer.toString(((Integer) value).intValue());
        }
        if (value instanceof String) {
            return "S," + utf16Output((String) value);
        }
        return "O";
    }

    private static String inkScriptOutput(Object script, Field scriptData,
            Field scriptEventOffsets, Field scriptStringList, Field scriptGfxId,
            DataInputStream input) throws Exception {
        byte[] data = (byte[]) scriptData.get(script);
        return inkScriptGfxOutput(scriptGfxId.get(script)) + ":"
                + intsOutput((int[]) scriptEventOffsets.get(script)) + ":"
                + (data == null ? "null" : bytesOutput(data)) + ":"
                + scriptIdsOutput((String[]) scriptStringList.get(script)) + ":"
                + inputRemaining(input);
    }

    private static Object roomObjectField(Object roomObject, String name) throws Exception {
        return field(roomObject.getClass(), name).get(roomObject);
    }

    private static String roomObjectOutput(Object roomObject, DataInputStream input)
            throws Exception {
        StringBuffer result = new StringBuffer();
        String[] integerFields = {
            "type", "x", "y", "width", "height", "regPointX", "regPointY",
            "left", "right", "top", "bottom", "transform"
        };
        for (int index = 0; index < integerFields.length; index++) {
            result.append(((Integer) roomObjectField(roomObject,
                    integerFields[index])).intValue()).append(':');
        }
        result.append(inkScriptGfxOutput(roomObjectField(roomObject, "gfxID"))).append(':');
        result.append(utf16Output((String) roomObjectField(roomObject, "scriptID"))).append(':');
        result.append(roomObjectField(roomObject, "script") == null ? "n:" : "o:");
        result.append(((Boolean) roomObjectField(roomObject, "visible")).booleanValue()
                ? "1:" : "0:");
        result.append(((Boolean) roomObjectField(roomObject, "active")).booleanValue()
                ? "1:" : "0:");
        result.append(((Integer) roomObjectField(roomObject,
                "textAlignment")).intValue()).append(':');
        result.append(roomObjectField(roomObject, "animationData") == null ? "n:" : "a:");
        result.append(intsOutput((int[]) roomObjectField(roomObject,
                "animationParts"))).append(':');
        result.append(intsOutput((int[]) roomObjectField(roomObject,
                "animationDuration"))).append(':');
        result.append(roomObjectField(roomObject, "animationImagePoints") == null
                ? "n:" : "a:");
        result.append(((Long) roomObjectField(roomObject,
                "animationTime")).longValue()).append(':');
        result.append(((Long) roomObjectField(roomObject,
                "idleAnimationTime")).longValue()).append(':');
        result.append(((Integer) roomObjectField(roomObject,
                "runAnimLoops")).intValue()).append(':');
        result.append(((Integer) roomObjectField(roomObject,
                "battlePanelID")).intValue()).append(':');
        result.append(intsOutput((int[]) roomObjectField(roomObject,
                "battlePanel"))).append(':');
        result.append(((Integer) roomObjectField(roomObject, "color")).intValue()).append(':');
        result.append(utf16Output((String) roomObjectField(roomObject, "text"))).append(':');
        result.append(((Integer) roomObjectField(roomObject,
                "runAnimPausedTime")).intValue()).append(':');
        result.append(inputRemaining(input));
        return result.toString();
    }

    private static String roomObjectStaticsOutput(Class<?> owner) throws Exception {
        String[] names = {
            "paintingAnimationTime", "noVibraYet", "BATTLE_PANEL_ID_HERO_HEALTH",
            "BATTLE_PANEL_ID_ENEMY_HEALTH", "BATTLE_PANEL_ID_TIMEBAR",
            "BATTLE_PANEL_ID_HARD_ATTACK", "BATTLE_PANEL_ID_FAST_ATTACK",
            "BATTLE_PANEL_ID_INVENTORY", "BATTLE_PANEL_ID_ESCAPE",
            "BATTLE_PANEL_MAX_HEALTH", "BATTLE_PANEL_HEALTH", "BATTLE_PANEL_BAR_SIZE",
            "BATTLE_PANEL_TIME", "BATTLE_PANEL_SIZE"
        };
        StringBuffer result = new StringBuffer();
        for (int index = 0; index < names.length; index++) {
            if (index != 0) {
                result.append(':');
            }
            result.append(String.valueOf(field(owner, names[index]).get(null)));
        }
        return result.toString();
    }

    private static String[] stringHandles(String token) {
        if (token.equals("null")) {
            return null;
        }
        if (token.equals("-")) {
            return new String[0];
        }
        String[] tokens = token.substring(1).split(",");
        String[] result = new String[tokens.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = tokens[index].equals("n") ? null : new String(tokens[index]);
        }
        return result;
    }

    private static String stringHandlesOutput(String[] values) {
        if (values == null) {
            return "null";
        }
        if (values.length == 0) {
            return "-";
        }
        StringBuffer result = new StringBuffer("o");
        for (int index = 0; index < values.length; index++) {
            if (index != 0) {
                result.append(',');
            }
            result.append(values[index] == null ? "n" : values[index]);
        }
        return result.toString();
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws Exception {
        Method result = owner.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    private static int invoke(Method method, int... arguments) throws Exception {
        Object[] boxed = new Object[arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            boxed[index] = Integer.valueOf(arguments[index]);
        }
        return ((Integer) method.invoke(null, boxed)).intValue();
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static String keyBindingsOutput(String status, Field[] keyFields)
            throws Exception {
        StringBuffer result = new StringBuffer(status);
        for (int index = 0; index < keyFields.length; index++) {
            result.append(':');
            result.append(keyFields[index].getInt(null));
        }
        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        Class<?> application = Class.forName("M");
        Class<?> inkEngine = Class.forName("ExtBase");
        Class<?> cheatController = Class.forName("Cheat");
        Class<?> silentHillGame = Class.forName("Ext");
        Class<?> inkCodes = Class.forName("s");
        Class<?> textId = Class.forName("txt_consts");
        Class<?> requestClass = Class.forName("LoadRequest");
        Class<?> canvas = Class.forName("MyCanvas");
        Class<?> menuClass = Class.forName("Menu");
        Class<?> roomObjectClass = Class.forName("RoomObject");
        Class<?> resourceClass = Class.forName("Resource");
        Class<?> scriptClass = Class.forName("Script");
        Class<?> interpreterClass = Class.forName("ScriptThread");
        Method min = method(application, "min", Integer.TYPE, Integer.TYPE);
        Method max = method(application, "max", Integer.TYPE, Integer.TYPE);
        Method abs = method(application, "abs", Integer.TYPE);
        Method dir = method(application, "dir", Integer.TYPE);
        Method getLeft = method(application, "getLeft", Integer.TYPE, Integer.TYPE, Integer.TYPE,
                Integer.TYPE, Integer.TYPE, Integer.TYPE);
        Method getTop = method(application, "getTop", Integer.TYPE, Integer.TYPE, Integer.TYPE,
                Integer.TYPE, Integer.TYPE, Integer.TYPE);
        Method resourceExit = method(application, "resourceExit");
        Method destroyApp = method(application, "destroyApp", Boolean.TYPE);
        Method pauseApp = method(application, "pauseApp");
        Method appStart = method(application, "appStart");
        Method popupCreate = method(inkEngine, "popupCreate", String.class, Integer.TYPE);
        Method popupCreateMax = method(
                inkEngine, "popupCreate", String.class, Integer.TYPE, Integer.TYPE);
        Method popupSetNext = method(inkEngine, "popupSetNext");
        Method inventoryEquipUnequipHandling = method(
                inkEngine, "inventoryEquipUnequipHandling", Integer.TYPE);
        Method menuResetIngameValues = method(silentHillGame, "menuResetIngameValues");
        Method silentHillGameAppInit = method(silentHillGame, "appInit");
        Method keyJadEntryAsInt = method(canvas, "keyJadEntryAsInt", String.class);
        Method resourceUrlEncode = method(application, "resourceURLEncode", String.class);
        Method codedString = method(application, "codedString", byte[].class);
        Method printArray = method(application, "printArray", byte[][].class);
        Method resourceRestartImportants = method(application, "resourceRestartImportants");
        Method roomRepaintRun = method(application, "roomRepaintRun");
        Method clearAllRms = method(application, "clearAllRMS");
        Method freeMemory = method(application, "freeMemory");
        Method setDisplay = method(application, "setDisplay", Displayable.class);
        Method rmsDelete = method(application, "rmsDelete", String.class);
        Method saveChunkIni = method(application, "saveChunkINI", DataInputStream.class);
        Method resetLoad = method(application, "resetLoad");
        Method resourcePathForString = method(application, "loadRequest_getResourcePath",
                Integer.TYPE, String.class);
        Method resourcePathForObject = method(application, "loadRequest_getResourcePath",
                Object.class, Integer.TYPE);
        Method resourcePath = method(application, "loadRequest_getResourcePath",
                Integer.TYPE, Integer.TYPE, String.class, Integer.TYPE);
        Method getGameLangPath = method(application, "getGameLangPath");
        Method getGameText = method(application, "getGameText", Integer.TYPE);
        Method getGameTextFromString = method(application, "getGameText", String.class);
        Method textReplace = method(application, "txtStringReplace",
                String.class, String.class, String.class);
        Method removeStringPrefix = method(application, "removeStringPrefix",
                String[].class, String.class);
        Method languagePosition = method(application, "getPosInLanguageSelectionList",
                String.class);
        Method resourceHeapIndex = method(application, "resourceIsOnHeap", Integer.TYPE);
        Method randomScaled = method(application, "random", Integer.TYPE);
        Method inkGetVariable = method(application, "inkServerGetVariabel", String.class);
        Method inkGetHint = method(application, "inkServerGetHint", String.class);
        Method inkSetVariable = method(application, "inkServerSetVariabel",
                String.class, String.class, String.class);
        Method inkUnsetVariable = method(application, "inkServerUnsetVariabel", String.class);
        Method resetVariableSystem = method(application, "resetVariableSystem");
        Method roomHistorySize = method(application, "roomGetHistorySize");
        Method inventorySize = method(scriptClass, "getInventorySize", String.class);
        Method inventorySet = method(scriptClass, "setInventory", String.class, Integer.TYPE);
        Method inventoryRemove = method(scriptClass, "removeInventory", String.class);
        Method scriptSetVariable = method(scriptClass, "setVariable", String.class, Object.class);
        Method scriptGetVariable = method(scriptClass, "getVariable", String.class);
        Method scriptGetVariableAsInteger = method(
                scriptClass, "getVariableAsInteger", String.class);
        Method roomCurrent = method(application, "roomGetCurrent");
        Method roomSet = method(application, "roomSetCurrent", String.class);
        Method roomLast = method(application, "roomGetLastInRoomHistory");
        Method roomAdd = method(application, "roomAddToRoomHistory", String.class);
        Method roomRemove = method(application, "roomRemoveLastInRoomHistory");
        Method charToString = method(application, "charToString", Character.TYPE);
        Method resourceMergeSortCmp = method(application, "resourceMergeSortCmp", byte[].class, byte[].class);
        Method arrayCopyString = method(application, "arrayCopyString", String[].class,
                Integer.TYPE, String[].class, Integer.TYPE, Integer.TYPE);
        Method toInt = method(application, "toInt", Object.class);
        Method toBoolean = method(application, "toBoolean", Object.class);
        Method integerArgument = method(interpreterClass, "integerArgument", Object.class);
        Method actionKeyIdConvert = method(inkEngine, "actionKeyIdConvert", String.class);
        Method getDescription = method(requestClass, "getDescription");
        Method requestGetId = method(requestClass, "getID");
        Method requestEquals = method(requestClass, "equals", Object.class);
        Method requestResourcePath = method(requestClass, "getResourcePath");
        Method requestToString = method(requestClass, "toString");
        Method readString = method(application, "readString", DataInputStream.class);
        Method readStringList = method(
                application, "readStringList", DataInputStream.class);
        Method requestFromInput = method(
                requestClass, "createFromInputStream", DataInputStream.class);
        Method find = method(application, "find", DataInputStream.class, String.class);
        Method writeString = method(
                application, "writeString", java.io.DataOutputStream.class, String.class);
        Method tickBasedTime = method(application, "tickBasedTime");
        Method tickBasedTimeUpdate = method(application, "tickBasedTimeUpdate");
        Method tickBasedTimeReset = method(application, "tickBasedTimeReset");
        Method loading = method(application, "loading");
        Method isMenuScrollAllowed = method(inkEngine, "isMenuScrollAllowed");
        Method actionKeyKeycodeToActionkey = method(inkEngine, "actionKeyKeycodeToActionkey", Integer.TYPE);
        Method actionKeyUnsetAllKeys = method(inkEngine, "actionKeyUnsetAllKeys");
        Method actionKeyInitSystem = method(inkEngine, "actionKeyInitSystem");
        Method actionKeyGetScriptId = method(inkEngine, "actionKeyGetScriptId", Integer.TYPE);
        Method splashMoreExists = method(inkEngine, "splashMoreExists");
        Method wrapDefault = method(
                inkEngine, "wrapString", String.class, Integer.TYPE);
        Method keyInit = method(canvas, "keyInit");
        Method keyConvertToKeyId = method(canvas, "keyConvertToKeyId", Integer.TYPE);
        Method setKeyStatus = method(application, "setKeyStatus", Integer.TYPE, Boolean.TYPE);
        Method canvasPaint = method(canvas, "paint", Graphics.class);
        Method canvasShowNotify = method(canvas, "showNotify");
        Method canvasKeyPressed = method(canvas, "keyPressed", Integer.TYPE);
        Method canvasKeyReleased = method(canvas, "keyReleased", Integer.TYPE);
        Method menuGetChoiceNr = method(menuClass, "getChoiceNr");
        Method menuAddChoiceObject = method(
                menuClass, "addChoice", Object.class, String.class);
        Method menuAddChoiceInt = method(
                menuClass, "addChoice", Integer.TYPE, String.class);
        Method menuCountChoices = method(menuClass, "countChoices");
        Method menuGetChoiceId = method(menuClass, "getChoiceID");
        Method menuNextChoice = method(menuClass, "nextChoice");
        Method menuPreviousChoice = method(menuClass, "previousChoice");
        Method menuSetPosition = method(menuClass, "setPosition", Integer.TYPE, Integer.TYPE);
        Method menuSetCurrent = method(menuClass, "setCurrent", Boolean.TYPE);
        Method menuScrollIncrease = method(menuClass, "scrollIncrease");
        Method menuScrollDecrease = method(menuClass, "scrollDecrease");
        Method menuSetTop = method(menuClass, "setTop", String.class);
        Method menuSetSoftkeyOptions = method(menuClass, "setSoftkeyOptions", String.class, String.class);
        Method menuSetInvItemResource = method(menuClass, "setInvItemResource", resourceClass);
        Method menuActive = method(menuClass, "active");
        Method menuCloseAll = method(menuClass, "closeAll");
        Method menuCloseCurrent = method(menuClass, "closeCurrent");
        Method menuGetCurrent = method(menuClass, "getCurrent");
        Method gameResourceEquals = method(resourceClass, "equals", Object.class);
        Method gameResourcePaint = method(resourceClass, "paint",
                Graphics.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
        Method gameResourcePaintSimple = method(resourceClass, "paintSimple",
                Graphics.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
        Method inkScriptGetString = method(scriptClass, "getString", Integer.TYPE);
        Method inkScriptHasEvent = method(scriptClass, "hasEvent", Integer.TYPE);
        Method inkScriptHasCommand = method(scriptClass, "hasCommand", Integer.TYPE);
        Method inkScriptExecuteEvent = method(scriptClass, "executeEvent",
                Integer.TYPE, Object.class, roomObjectClass);
        Method inkScriptExecuteEventDebug = method(scriptClass, "executeEvent",
                Integer.TYPE, Object.class, roomObjectClass, Boolean.TYPE);
        Method inkScriptExecuteEventById = method(scriptClass, "executeEvent",
                String.class, Integer.TYPE, Object.class, roomObjectClass);
        Method inkScriptGetItemName = method(scriptClass, "getItemName", String.class);
        Method inkScriptResume = method(scriptClass, "resume");
        Method inkScriptIsWaiting = method(scriptClass, "isWaiting");
        Method inkScriptStop = method(scriptClass, "stop");
        Method inkInterpreterRead = method(interpreterClass, "read");
        Method inkInterpreterReadBytes = method(interpreterClass, "read", Integer.TYPE);
        Method inkInterpreterReadSigned = method(interpreterClass, "readSigned", Integer.TYPE);
        Method inkInterpreterHasCommand = method(interpreterClass, "hasCommand", Integer.TYPE);
        Method inkInterpreterExecute = method(interpreterClass, "execute", Object.class);
        Method inkInterpreterResume = method(interpreterClass, "resume");
        Method battlePanelNew = method(roomObjectClass, "battlePanelNew", Integer.TYPE);
        Method bpSetMaxHealth = method(roomObjectClass, "bpSetMaxHealth", Integer.TYPE);
        Method bpSetHealth = method(roomObjectClass, "bpSetHealth", Integer.TYPE);
        Method bpSetBarSize = method(roomObjectClass, "bpSetBarSize", Integer.TYPE);
        Method bpSetTime = method(roomObjectClass, "bpSetTime", Integer.TYPE);
        Method roomIsOver = method(roomObjectClass, "isOver", Integer.TYPE, Integer.TYPE);
        Method roomExecuteEvent = method(
                roomObjectClass, "executeEvent", Integer.TYPE, Object.class, Boolean.TYPE);
        Method roomGetName = method(roomObjectClass, "getName");
        Method roomGetMoveDirection = method(roomObjectClass, "getMoveDir");
        Method roomEnterHover = method(roomObjectClass, "hooverIn");
        Constructor<?> requestConstructor = requestClass.getDeclaredConstructor(Integer.TYPE, String.class);
        requestConstructor.setAccessible(true);
        Constructor<?> applicationConstructor = application.getDeclaredConstructor();
        applicationConstructor.setAccessible(true);
        Constructor<?> cheatControllerConstructor = cheatController.getDeclaredConstructor();
        cheatControllerConstructor.setAccessible(true);
        Constructor<?> silentHillGameConstructor = silentHillGame.getDeclaredConstructor();
        silentHillGameConstructor.setAccessible(true);
        Constructor<?> inkCodesConstructor = inkCodes.getDeclaredConstructor();
        inkCodesConstructor.setAccessible(true);
        Constructor<?> textIdConstructor = textId.getDeclaredConstructor();
        textIdConstructor.setAccessible(true);
        Constructor<?> requestObjectConstructor = requestClass.getDeclaredConstructor(Object.class, Integer.TYPE);
        requestObjectConstructor.setAccessible(true);
        Constructor<?> menuConstructor = menuClass.getDeclaredConstructor();
        menuConstructor.setAccessible(true);
        Constructor<?> roomObjectConstructor = roomObjectClass.getDeclaredConstructor(
                DataInputStream.class, String[].class);
        roomObjectConstructor.setAccessible(true);
        Constructor<?> resourceConstructor = resourceClass.getDeclaredConstructor(
                Integer.TYPE, Object.class, Integer.TYPE);
        resourceConstructor.setAccessible(true);
        Constructor<?> canvasConstructor = canvas.getDeclaredConstructor();
        canvasConstructor.setAccessible(true);
        Constructor<?> scriptConstructor = scriptClass.getDeclaredConstructor(
                DataInputStream.class, String[].class);
        scriptConstructor.setAccessible(true);
        Constructor<?> interpreterConstructor = interpreterClass.getDeclaredConstructor(
                scriptClass, Integer.TYPE, roomObjectClass);
        interpreterConstructor.setAccessible(true);
        Field tickBasedTimeValue = field(application, "tickBasedTimeValue");
        Field applicationRuntime = field(application, "runtime");
        Field applicationInited = field(application, "appInited");
        Field applicationMidlet = field(application, "midlet");
        Field applicationHiddenCanvas = field(application, "hiddenCanvas");
        Field applicationMainMenuActive = field(application, "mainMenuActive");
        Field applicationCurSoundMode = field(application, "curSoundMode");
        Field applicationTickerThread = field(application, "tickerThread");
        Field loadThread = field(application, "loadThread");
        Field resourceImportants = field(application, "resourceImportants");
        Field resourcesToDownload = field(application, "resourcesToDownload");
        Field gameId = field(application, "gameId");
        Field gameTexts = field(application, "gameTexts");
        Field saveIsPossible = field(application, "saveIsPossible");
        Field languages = field(application, "languages");
        Field resourceHeapSources = field(application, "resourceHeapSourceLRE");
        Field resourceSubchunkData = field(application, "resourceSCData");
        Field resourceSubchunkSize = field(application, "resourceSCCurrentSize");
        Field randomInstance = field(application, "randomInstance");
        Field inkServerVariables = field(application, "inkServerVariables");
        Field inkServerHints = field(application, "inkServerHint");
        Field gameChangedSinceLastSave = field(application, "gameChangedSinceLastSave");
        Field cheatLastKey = field(cheatController, "lastKey");
        Field hudAmmoNumberWidth = field(silentHillGame, "HUD_ammoNumWidth");
        Field hudAmmoUpdateNeeded = field(silentHillGame, "HUD_ammoUpdateNeeded");
        Field inkMenuLogo = field(silentHillGame, "INK_menu_logo");
        Field ingameMargin = field(inkEngine, "ingameMargin");
        Field keyNew = field(application, "keyNew");
        Field keyPressed = field(application, "keyPressed");
        Field keyLastPressed = field(application, "keyLastPressed");
        Field loadingMode = field(application, "loadingMode");
        Field loadBarActive = field(application, "loadBarActive");
        Field roomRepaintThread = field(application, "roomRepaintThread");
        Field roomGraphics = field(application, "roomGraphics");
        Field roomImage = field(application, "roomImage");
        Field roomObjects = field(application, "roomObjects");
        Field roomRepainting = field(application, "roomRepainting");
        Field roomRepaintNeeded = field(application, "roomRepaintNeeded");
        Field gotoDissolveFxIsSet = field(application, "gotoDissolveFXIsSet");
        Field applicationGraphics = field(application, "gfx");
        Field fadeFrames = field(application, "FADE_FRAMES");
        Field demoFrames = field(application, "DEMO_FRAMES");
        Field applicationPainting = field(application, "painting");
        Field loadingBarMarkerX = field(application, "loadingBarMarkerX");
        Field gotoDissolveFxCounter = field(application, "gotoDissolveFXCounter");
        Field requestType = field(requestClass, "type");
        Field requestIntegerId = field(requestClass, "integerID");
        Field requestStringId = field(requestClass, "stringID");
        Field requestImageTransform = field(requestClass, "imageTransform");
        Field menuScrollTickCounter = field(inkEngine, "menuScrollTickCounter");
        Field actionKeyKeyCodes = field(inkEngine, "actionKey_keyCodes");
        Field actionKeyScriptIds = field(inkEngine, "actionKey_scriptIds");
        Field currentSplash = field(inkEngine, "curSplash");
        Field numberOfSplashes = field(inkEngine, "numOfSplashes");
        Field popupNumber = field(inkEngine, "popupNumOf");
        Field popupActive = field(inkEngine, "popupActive");
        Field popupCurrent = field(inkEngine, "popupCurrent");
        Field popupChoice = field(inkEngine, "popup_choice");
        Field popupEndTime = field(inkEngine, "popupEndTime");
        Field popupMinimumTimeEnds = field(inkEngine, "popupMinTimeEnds");
        Field popupRecoveryCodes = field(inkEngine, "popupRecoveryCode");
        Field popupMaximumTimes = field(inkEngine, "popupMaxTime");
        Field popupTexts = field(inkEngine, "popupText");
        Field currentFont = field(inkEngine, "currentFont");
        Field canvasWidth = field(application, "canvasWidth");
        Field menuSelectedChoiceNr = field(menuClass, "selectedChoiceNr");
        Field menuChoiceIds = field(menuClass, "choiceIDs");
        Field menuChoiceTexts = field(menuClass, "choiceTexts");
        Field menuUpdateBodyLines = field(menuClass, "updateBodyLines");
        Field menuX = field(menuClass, "x");
        Field menuY = field(menuClass, "y");
        Field menuIsCurrent = field(menuClass, "isCurrent");
        Field menuScroll = field(menuClass, "scroll");
        Field menuTextScrolling = field(menuClass, "textScrolling");
        Field menuUpdateMenu = field(menuClass, "updateMenu");
        Field menuTopText = field(menuClass, "topText");
        Field menuUpdateTopLines = field(menuClass, "updateTopLines");
        Field menuSoftkeyLeft = field(menuClass, "engineSoftkeyOptionLeft");
        Field menuSoftkeyRight = field(menuClass, "engineSoftkeyOptionRight");
        Field menuInvItemResource = field(menuClass, "curInvItemResource");
        Field menuStack = field(menuClass, "stack");
        Field gameResourceType = field(resourceClass, "type");
        Field gameResourceId = field(resourceClass, "ID");
        Field gameResourceImage = field(resourceClass, "image");
        Field gameResourceWidth = field(resourceClass, "imageWidth");
        Field gameResourceHeight = field(resourceClass, "imageHeight");
        Field gameResourceRegistrationX = field(resourceClass, "imageRegPointX");
        Field gameResourceRegistrationY = field(resourceClass, "imageRegPointY");
        Field gameResourceTransform = field(resourceClass, "imageTransform");
        Field gameCanvasTransformTable = field(canvas, "transformTable");
        Field gameCanvasLoopCount = field(canvas, "loopCount");
        Field gameCanvasSoundId = field(canvas, "soundID");
        Field gameCanvasPlayer = field(canvas, "gPlayer");
        Field firstLoad = field(inkEngine, "FirstLoad");
        Field scriptData = field(scriptClass, "data");
        Field scriptEventOffsets = field(scriptClass, "eventOffsets");
        Field scriptStringList = field(scriptClass, "stringList");
        Field scriptGfxId = field(scriptClass, "gfxID");
        Field scriptList = field(scriptClass, "list");
        Field scriptWaitStop = field(scriptClass, "waitStop");
        Field scriptItemId = field(scriptClass, "itemID");
        Field interpreterScript = field(interpreterClass, "script");
        Field interpreterStatus = field(interpreterClass, "status");
        Field interpreterOffset = field(interpreterClass, "offset");
        Field interpreterRoomObject = field(interpreterClass, "roomObject");
        Field interpreterLanguageDebugMode = field(interpreterClass, "languageDebugMode");
        Field pausedThread = field(interpreterClass, "pausedThread");
        Field battlePanelId = field(roomObjectClass, "battlePanelID");
        Field battlePanel = field(roomObjectClass, "battlePanel");
        Field panelMaxHealth = field(roomObjectClass, "BATTLE_PANEL_MAX_HEALTH");
        Field panelHealth = field(roomObjectClass, "BATTLE_PANEL_HEALTH");
        Field panelBarSize = field(roomObjectClass, "BATTLE_PANEL_BAR_SIZE");
        Field panelTime = field(roomObjectClass, "BATTLE_PANEL_TIME");
        Field panelSize = field(roomObjectClass, "BATTLE_PANEL_SIZE");
        Field roomVisible = field(roomObjectClass, "visible");
        Field roomScriptId = field(roomObjectClass, "scriptID");
        Field roomScript = field(roomObjectClass, "script");
        Field roomActive = field(roomObjectClass, "active");
        Field roomLeft = field(roomObjectClass, "left");
        Field roomRight = field(roomObjectClass, "right");
        Field roomTop = field(roomObjectClass, "top");
        Field roomBottom = field(roomObjectClass, "bottom");
        Field settingsHash = field(inkEngine, "settingsHash");
        String[] keyFieldNames = {
            "keySoftkeyLeft", "keySoftkeyRight", "keySoftkeyCenter", "keyArrowUp",
            "keyArrowDown", "keyArrowLeft", "keyArrowRight", "keyReturn", "keyErase", "keySend"
        };
        Field[] keyFields = new Field[keyFieldNames.length];
        for (int index = 0; index < keyFields.length; index++) {
            keyFields[index] = field(canvas, keyFieldNames[index]);
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String line;
        while ((line = input.readLine()) != null) {
            String[] parts = line.split(" ");
            String result;
            if (parts[0].equals("cheat-init") && parts.length == 1) {
                result = Integer.toString(cheatLastKey.getInt(null));
            } else if (parts[0].equals("min") && parts.length == 3) {
                result = Integer.toString(invoke(min, value(parts, 1), value(parts, 2)));
            } else if (parts[0].equals("ink-script-init") && parts.length == 1) {
                Hashtable scripts = (Hashtable) scriptList.get(null);
                result = scripts == null ? "null" : Integer.toString(scripts.size());
            } else if (parts[0].equals("room-object-init") && parts.length == 1) {
                result = roomObjectStaticsOutput(roomObjectClass);
            } else if (parts[0].equals("menu-init") && parts.length == 1) {
                Vector stack = (Vector) menuStack.get(null);
                result = stack == null ? "null" : Integer.toString(stack.size());
            } else if (parts[0].equals("game-resource-init") && parts.length == 1) {
                Vector cachedImages = (Vector) field(resourceClass, "imagesLRE").get(null);
                Vector importantImages = (Vector) field(resourceClass, "imagesImportants").get(null);
                result = (cachedImages == null ? "null" : Integer.toString(cachedImages.size()))
                        + ":" + (importantImages == null ? "null"
                                : Integer.toString(importantImages.size()))
                        + ":" + (cachedImages != importantImages ? "1" : "0");
            } else if (parts[0].equals("ink-script-new") && parts.length == 3) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                Object script = scriptConstructor.newInstance(stream, scriptIds(parts[2]));
                result = inkScriptOutput(script, scriptData, scriptEventOffsets,
                        scriptStringList, scriptGfxId, stream);
            } else if (parts[0].equals("room-object-new") && parts.length == 3) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                Object roomObject = roomObjectConstructor.newInstance(
                        new Object[]{stream, scriptIds(parts[2])});
                result = roomObjectOutput(roomObject, stream);
            } else if (parts[0].equals("max") && parts.length == 3) {
                result = Integer.toString(invoke(max, value(parts, 1), value(parts, 2)));
            } else if (parts[0].equals("abs") && parts.length == 2) {
                result = Integer.toString(invoke(abs, value(parts, 1)));
            } else if (parts[0].equals("dir") && parts.length == 2) {
                result = Integer.toString(invoke(dir, value(parts, 1)));
            } else if (parts[0].equals("left") && parts.length == 7) {
                result = Integer.toString(invoke(getLeft, value(parts, 1), value(parts, 2), value(parts, 3),
                        value(parts, 4), value(parts, 5), value(parts, 6)));
            } else if (parts[0].equals("top") && parts.length == 7) {
                result = Integer.toString(invoke(getTop, value(parts, 1), value(parts, 2), value(parts, 3),
                        value(parts, 4), value(parts, 5), value(parts, 6)));
            } else if (parts[0].equals("resource-exit") && parts.length == 1) {
                resourceExit.invoke(null);
                result = "OK";
            } else if (parts[0].equals("destroy-app") && parts.length == 3) {
                Object midlet = applicationConstructor.newInstance();
                applicationRuntime.set(null, Runtime.getRuntime());
                applicationInited.setBoolean(null, true);
                applicationMidlet.set(null, value(parts, 2) == 0 ? null : midlet);
                String outcome;
                try {
                    destroyApp.invoke(midlet, Boolean.valueOf(value(parts, 1) != 0));
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else {
                        throw exception;
                    }
                }
                result = outcome + ":" + (applicationRuntime.get(null) == null ? "N:" : "R:")
                        + (applicationInited.getBoolean(null) ? "1" : "0");
            } else if (parts[0].equals("pause-app") && parts.length == 2) {
                Object midlet = applicationConstructor.newInstance();
                applicationMainMenuActive.setBoolean(null, true);
                applicationHiddenCanvas.setBoolean(null, value(parts, 1) != 0);
                pauseApp.invoke(midlet);
                result = "OK:" + (applicationHiddenCanvas.getBoolean(null) ? "1" : "0");
            } else if (parts[0].equals("app-start") && parts.length == 2) {
                int mode = value(parts, 1);
                RecordingThread ticker = mode == 0 ? null : new RecordingThread(mode == 2);
                applicationTickerThread.set(null, ticker);
                String outcome;
                try {
                    appStart.invoke(null);
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else {
                        throw exception;
                    }
                }
                result = outcome + ":" + (ticker == null ? "0" : Integer.toString(ticker.attempts));
            } else if (parts[0].equals("popup-create") && parts.length == 4) {
                String text = utf16(parts[1]);
                int recoveryCode = value(parts, 2);
                int requestedPopupNumber = value(parts, 3);
                popupNumber.setInt(null, requestedPopupNumber);
                popupActive.setBoolean(null, false);
                popupCurrent.setInt(null, 2);
                popupChoice.setByte(null, (byte) 9);
                popupEndTime.setLong(null, 77L);
                popupRecoveryCodes.set(null, new int[] {101, 102, 103, 104, 105});
                popupMaximumTimes.set(null, new int[] {201, 202, 203, 204, 205});
                popupTexts.set(null, new String[][] {
                    {"s0"}, {"s1"}, {"s2"}, {"s3"}, {"s4"}
                });
                currentFont.set(null, javax.microedition.lcdui.Font.getDefaultFont());
                canvasWidth.setInt(null, 128);
                String outcome;
                try {
                    popupCreate.invoke(null, text, Integer.valueOf(recoveryCode));
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else {
                        throw exception;
                    }
                }
                int[] recoveryCodes = (int[]) popupRecoveryCodes.get(null);
                int[] maximumTimes = (int[]) popupMaximumTimes.get(null);
                String[][] texts = (String[][]) popupTexts.get(null);
                result = outcome + ":" + popupNumber.getInt(null) + ":"
                        + (popupActive.getBoolean(null) ? "1" : "0") + ":"
                        + popupCurrent.getInt(null) + ":" + popupChoice.getByte(null) + ":"
                        + popupEndTime.getLong(null) + ":"
                        + recoveryCodes[requestedPopupNumber] + ":"
                        + maximumTimes[requestedPopupNumber] + ":"
                        + utf16Output(texts[requestedPopupNumber][0]);
            } else if (parts[0].equals("popup-create-max") && parts.length == 10) {
                String text = utf16(parts[1]);
                int recoveryCode = value(parts, 2);
                int maximumTime = value(parts, 3);
                int initialPopupNumber = value(parts, 4);
                boolean initialPopupActive = value(parts, 5) != 0;
                int textLength = value(parts, 6);
                int recoveryLength = value(parts, 7);
                int maximumLength = value(parts, 8);
                int requestedCanvasWidth = value(parts, 9);
                popupNumber.setInt(null, initialPopupNumber);
                popupActive.setBoolean(null, initialPopupActive);
                popupCurrent.setInt(null, 2);
                popupChoice.setByte(null, (byte) 9);
                popupEndTime.setLong(null, 77L);
                String[][] texts = textLength < 0 ? null : new String[textLength][];
                if (texts != null) {
                    for (int index = 0; index < texts.length; index++) {
                        texts[index] = new String[] {"s" + index};
                    }
                }
                popupTexts.set(null, texts);
                int[] recoveryCodes = recoveryLength < 0 ? null : new int[recoveryLength];
                if (recoveryCodes != null) {
                    for (int index = 0; index < recoveryCodes.length; index++) {
                        recoveryCodes[index] = 101 + index;
                    }
                }
                popupRecoveryCodes.set(null, recoveryCodes);
                int[] maximumTimes = maximumLength < 0 ? null : new int[maximumLength];
                if (maximumTimes != null) {
                    for (int index = 0; index < maximumTimes.length; index++) {
                        maximumTimes[index] = 201 + index;
                    }
                }
                popupMaximumTimes.set(null, maximumTimes);
                currentFont.set(null, javax.microedition.lcdui.Font.getDefaultFont());
                canvasWidth.setInt(null, requestedCanvasWidth);
                String outcome;
                try {
                    popupCreateMax.invoke(null, text, Integer.valueOf(recoveryCode),
                            Integer.valueOf(maximumTime));
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        int failedLength;
                        if (initialPopupNumber < 0 || initialPopupNumber >= textLength) {
                            failedLength = textLength;
                        } else if (initialPopupNumber >= recoveryLength) {
                            failedLength = recoveryLength;
                        } else {
                            failedLength = maximumLength;
                        }
                        outcome = "AIOOBE," + initialPopupNumber + "," + failedLength;
                    } else {
                        throw exception;
                    }
                }
                boolean timed = outcome.equals("OK") && initialPopupNumber < 4
                        && !initialPopupActive && maximumTime != -1;
                String endTime = timed && popupEndTime.getLong(null) != 77L
                        ? "T" : Long.toString(popupEndTime.getLong(null));
                result = outcome + ":" + popupNumber.getInt(null) + ":"
                        + (popupActive.getBoolean(null) ? "1" : "0") + ":"
                        + popupCurrent.getInt(null) + ":" + popupChoice.getByte(null) + ":"
                        + endTime + ":" + popupTextsOutput((String[][]) popupTexts.get(null)) + ":"
                        + intsOutput((int[]) popupRecoveryCodes.get(null)) + ":"
                        + intsOutput((int[]) popupMaximumTimes.get(null)) + ":"
                        + (timed ? "1" : "0");
            } else if (parts[0].equals("popup-set-next") && parts.length == 5) {
                int initialPopupCurrent = value(parts, 1);
                int initialPopupNumber = value(parts, 2);
                popupCurrent.setInt(null, initialPopupCurrent);
                popupNumber.setInt(null, initialPopupNumber);
                popupActive.setBoolean(null, value(parts, 3) != 0);
                popupMinimumTimeEnds.setLong(null, 66L);
                popupEndTime.setLong(null, 77L);
                popupMaximumTimes.set(null, ints(parts[4]));
                String outcome;
                try {
                    popupSetNext.invoke(null);
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        int[] maximumTimes = (int[]) popupMaximumTimes.get(null);
                        outcome = "AIOOBE," + popupCurrent.getInt(null) + ","
                                + maximumTimes.length;
                    } else {
                        throw exception;
                    }
                }
                boolean continued = popupCurrent.getInt(null) < initialPopupNumber;
                String minimumTime = continued && popupMinimumTimeEnds.getLong(null) != 66L
                        ? "T" : Long.toString(popupMinimumTimeEnds.getLong(null));
                int[] maximumTimes = (int[]) popupMaximumTimes.get(null);
                boolean timed = outcome.equals("OK") && continued
                        && maximumTimes[popupCurrent.getInt(null)] != -1;
                String endTime = timed && popupEndTime.getLong(null) != 77L
                        ? "T" : Long.toString(popupEndTime.getLong(null));
                int clockCalls = continued ? (timed ? 2 : 1) : 0;
                result = outcome + ":" + popupCurrent.getInt(null) + ":"
                        + popupNumber.getInt(null) + ":"
                        + (popupActive.getBoolean(null) ? "1" : "0") + ":" + minimumTime
                        + ":" + endTime + ":" + intsOutput(maximumTimes) + ":" + clockCalls;
            } else if (parts[0].equals("default-constructor") && parts.length == 2) {
                if (parts[1].equals("cheat")) {
                    cheatControllerConstructor.newInstance();
                } else if (parts[1].equals("game") || parts[1].equals("engine")) {
                    silentHillGameConstructor.newInstance();
                } else if (parts[1].equals("application")) {
                    applicationConstructor.newInstance();
                } else if (parts[1].equals("codes")) {
                    inkCodesConstructor.newInstance();
                } else if (parts[1].equals("text-id")) {
                    textIdConstructor.newInstance();
                } else {
                    throw new AssertionError("unknown constructor request");
                }
                result = "OK:1";
            } else if (parts[0].equals("game-canvas-new") && parts.length == 3) {
                javax.microedition.lcdui.game.GameCanvas.oracleResetConstructor(
                        value(parts, 1) != 0);
                Canvas.oracleResetFullScreen(value(parts, 2) != 0);
                String status;
                try {
                    canvasConstructor.newInstance();
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                String sameReceiver = Canvas.oracleFullScreenCalls == 0
                        ? "-" : Canvas.oracleFullScreenReceiver
                                == javax.microedition.lcdui.game.GameCanvas.oracleConstructorReceiver
                                        ? "1" : "0";
                result = status + ":"
                        + javax.microedition.lcdui.game.GameCanvas.oracleConstructorCalls + ":"
                        + (javax.microedition.lcdui.game.GameCanvas.oracleSuppressKeyEvents
                                ? "1" : "0") + ":"
                        + Canvas.oracleFullScreenCalls + ":"
                        + (Canvas.oracleFullScreenMode ? "1" : "0") + ":" + sameReceiver;
                javax.microedition.lcdui.game.GameCanvas.oracleResetConstructor(false);
                Canvas.oracleResetFullScreen(false);
            } else if (parts[0].equals("menu-reset-ingame-values")
                    && parts.length == 3) {
                hudAmmoNumberWidth.setInt(null, value(parts, 1));
                hudAmmoUpdateNeeded.setBoolean(null, value(parts, 2) != 0);
                ingameMargin.setInt(null, Integer.MIN_VALUE);
                menuResetIngameValues.invoke(null);
                result = ingameMargin.getInt(null) + ":"
                        + hudAmmoNumberWidth.getInt(null) + ":"
                        + (hudAmmoUpdateNeeded.getBoolean(null) ? "1" : "0");
            } else if (parts[0].equals("app-init") && parts.length == 2) {
                Image oldLogo = value(parts, 1) == 0 ? null : Image.createImage(1, 1);
                inkMenuLogo.set(null, oldLogo);
                applicationMidlet.set(null, null);
                canvasWidth.setInt(null, Integer.MIN_VALUE);
                Display.oracleReset(0, 0);
                Image.oracleResetStringCreate();
                String status;
                PrintStream previous = System.out;
                try {
                    System.setOut(new PrintStream(new java.io.ByteArrayOutputStream()));
                    silentHillGameAppInit.invoke(null);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                } finally {
                    System.setOut(previous);
                }
                String requestedPath = Image.oracleStringCreateName == null
                        ? "-" : Image.oracleStringCreateName.substring(1);
                Object currentLogo = inkMenuLogo.get(null);
                String logo = currentLogo == Image.oracleStringCreatedImage
                        ? "NEW" : currentLogo == oldLogo ? "OLD" : "WRONG";
                String engineStarted = canvasWidth.getInt(null) == Integer.MIN_VALUE ? "0" : "1";
                result = status + ":" + Image.oracleStringCreateCalls + ":"
                        + requestedPath + ":" + logo + ":" + engineStarted;
                Image.oracleResetStringCreate();
            } else if (parts[0].equals("key-jad-entry") && parts.length == 5) {
                Object expectedMidlet = value(parts, 1) == 0
                        ? null : applicationConstructor.newInstance();
                String expectedKey = utf16(parts[2]);
                applicationMidlet.set(null, expectedMidlet);
                MIDlet.oracleResetProperty(utf16(parts[4]), value(parts, 3) != 0);
                int parsed = ((Integer) keyJadEntryAsInt.invoke(null, expectedKey)).intValue();
                String receiver = MIDlet.oraclePropertyCalls == 0
                        ? "-" : MIDlet.oraclePropertyReceiver == expectedMidlet ? "M" : "W";
                String key = MIDlet.oraclePropertyCalls == 0
                        ? "-" : MIDlet.oraclePropertyKey == expectedKey ? "K" : "W";
                result = parsed + ":" + MIDlet.oraclePropertyCalls + ":" + receiver + ":" + key;
                MIDlet.oracleResetProperty(null, false);
            } else if (parts[0].equals("url-encode") && parts.length == 2) {
                try {
                    result = utf16Output((String) resourceUrlEncode.invoke(null,
                            new Object[] {utf16(parts[1])}));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("coded-string") && parts.length == 2) {
                try {
                    result = utf16Output((String) codedString.invoke(null,
                            new Object[] {bytes(parts[1])}));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("print-array") && parts.length == 2) {
                java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
                PrintStream previous = System.out;
                PrintStream recording = new PrintStream(captured, true, "UTF-8");
                String status;
                try {
                    System.setOut(recording);
                    printArray.invoke(null, new Object[] {byteArrays(parts[1])});
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                } finally {
                    recording.flush();
                    System.setOut(previous);
                }
                result = status + ":" + bytesOutput(captured.toByteArray());
            } else if (parts[0].equals("room-repaint-run") && parts.length == 3) {
                Thread oldThread = value(parts, 2) == 0 ? null : new Thread();
                roomRepaintThread.set(null, oldThread);
                roomGraphics.set(null, value(parts, 1) == 0 ? null : new Graphics());
                roomImage.set(null, Image.createImage(1, 1));
                roomObjects.set(null, java.lang.reflect.Array.newInstance(roomObjectClass, 0));
                roomRepainting.setBoolean(null, false);
                roomRepaintNeeded.setBoolean(null, true);
                gotoDissolveFxIsSet.setBoolean(null, false);
                loadBarActive.setBoolean(null, false);
                String status;
                try {
                    roomRepaintRun.invoke(null);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                Object retainedThread = roomRepaintThread.get(null);
                String thread = retainedThread == null
                        ? "NULL" : retainedThread == oldThread ? "OLD" : "WRONG";
                result = status + ":" + thread + ":"
                        + (roomRepainting.getBoolean(null) ? "1:" : "0:")
                        + (roomRepaintNeeded.getBoolean(null) ? "1" : "0");
            } else if (parts[0].equals("clear-all-rms") && parts.length == 3) {
                boolean resourceSucceeds = value(parts, 1) != 0;
                int scriptCount = value(parts, 2);
                resourceHeapSources.set(null, new int[7]);
                resourceSubchunkData.set(null, resourceSucceeds ? new byte[] {73} : null);
                resourceSubchunkSize.setInt(null, 91);
                Vector importants = new Vector();
                importants.addElement("a");
                importants.addElement("b");
                resourceImportants.set(null, importants);
                Vector downloadsBefore = new Vector();
                downloadsBefore.addElement("download");
                resourcesToDownload.set(null, downloadsBefore);
                Hashtable scriptsBefore = scriptCount < 0 ? null : new Hashtable();
                if (scriptsBefore != null) {
                    for (int index = 0; index < scriptCount; index++) {
                        scriptsBefore.put("script" + index, new Handle(index));
                    }
                }
                scriptList.set(null, scriptsBefore);
                String status;
                try {
                    clearAllRms.invoke(null);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                byte[] subchunkData = (byte[]) resourceSubchunkData.get(null);
                String subchunk = subchunkData == null
                        ? "NULL" : Byte.toString(subchunkData[0]);
                Vector downloadsAfter = (Vector) resourcesToDownload.get(null);
                String downloads = downloadsAfter == null
                        ? "NULL" : Integer.toString(downloadsAfter.size());
                Hashtable scriptsAfter = (Hashtable) scriptList.get(null);
                String scripts = scriptsAfter == null
                        ? "NULL" : Integer.toString(scriptsAfter.size());
                result = status + ":"
                        + Integer.toString(((int[]) resourceHeapSources.get(null)).length)
                        + ":" + subchunk + ":" + Integer.toString(resourceSubchunkSize.getInt(null))
                        + ":" + Integer.toString(((Vector) resourceImportants.get(null)).size())
                        + ":" + downloads + ":" + scripts;
            } else if (parts[0].equals("free-memory") && parts.length == 2) {
                applicationRuntime.set(
                        null, value(parts, 1) == 0 ? null : Runtime.getRuntime());
                try {
                    long available = ((Long) freeMemory.invoke(null)).longValue();
                    result = "OK:" + (available >= 0L ? "1" : "0");
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE:-";
                }
            } else if (parts[0].equals("set-display") && parts.length == 5) {
                Object expectedMidlet = value(parts, 1) == 0
                        ? null : applicationConstructor.newInstance();
                Displayable expectedCurrent = value(parts, 2) == 0
                        ? null : new Displayable() {};
                applicationMidlet.set(null, expectedMidlet);
                Display.oracleReset(value(parts, 3), value(parts, 4));
                String status;
                try {
                    setDisplay.invoke(null, expectedCurrent);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                String observedMidlet = Display.oracleMidlet == null
                        ? "N" : Display.oracleMidlet == expectedMidlet ? "M" : "W";
                String receiver = Display.oracleSetCurrentReceiver == null
                        ? "-" : Display.oracleSetCurrentReceiver == Display.oracleIssuedDisplay
                                ? "D" : "W";
                String current = Display.oracleSetCurrentCalls == 0
                        ? "-" : Display.oracleCurrent == null
                                ? "N" : Display.oracleCurrent == expectedCurrent ? "C" : "W";
                result = status + ":" + Display.oracleGetDisplayCalls + ":"
                        + Display.oracleSetCurrentCalls + ":" + observedMidlet + ":"
                        + receiver + ":" + current;
            } else if (parts[0].equals("rms-delete") && parts.length == 3) {
                String expectedName = utf16(parts[1]);
                RecordStore.oracleResetDelete(value(parts, 2));
                String status;
                try {
                    status = ((Boolean) rmsDelete.invoke(null, expectedName)).booleanValue()
                            ? "T" : "F";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                String identity = RecordStore.oracleDeleteCalls == 0
                        ? "-" : RecordStore.oracleDeleteName == expectedName ? "I" : "W";
                result = status + ":" + RecordStore.oracleDeleteCalls + ":" + identity;
                RecordStore.oracleResetDelete(0);
            } else if (parts[0].equals("save-chunk-ini") && parts.length == 3) {
                int streamMode = value(parts, 1);
                DataInputStream chunkInput;
                if (streamMode == 0) {
                    chunkInput = null;
                } else if (streamMode == 1) {
                    chunkInput = new DataInputStream(new ByteArrayInputStream(new byte[0]));
                } else if (streamMode == 2) {
                    chunkInput = new DataInputStream(
                            new ByteArrayInputStream(new byte[] {0, 1, (byte) 255}));
                } else {
                    chunkInput = new DataInputStream(new FailingInputStream(streamMode));
                }
                RecordStore.oracleResetWrite(value(parts, 2));
                saveChunkIni.invoke(null, chunkInput);
                result = RecordStore.oracleOpenCalls + ":"
                        + utf16Output(RecordStore.oracleOpenName) + ":"
                        + (RecordStore.oracleOpenCreate ? "1" : "0") + ":"
                        + RecordStore.oracleSetCalls + ":"
                        + (RecordStore.oracleSetData == null
                                ? "null" : bytesOutput(RecordStore.oracleSetData)) + ":"
                        + RecordStore.oracleSetOffset + ":" + RecordStore.oracleSetLength;
                RecordStore.oracleResetWrite(0);
            } else if (parts[0].equals("resource-restart-importants") && parts.length == 2) {
                int oldLength = value(parts, 1);
                Vector oldValues = oldLength < 0 ? null : new Vector();
                if (oldValues != null) {
                    for (int index = 0; index < oldLength; index++) {
                        oldValues.addElement(Integer.valueOf(index));
                    }
                }
                resourceImportants.set(null, oldValues);
                resourceRestartImportants.invoke(null);
                result = Integer.toString(((Vector) resourceImportants.get(null)).size());
            } else if (parts[0].equals("reset-load") && parts.length == 4) {
                loadThread.set(null, value(parts, 1) == 0 ? null : new Thread());
                loadingMode.setInt(null, value(parts, 2));
                resourcesToDownload.set(null, choiceIds(parts[3]));
                String status;
                try {
                    resetLoad.invoke(null);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = status + ":" + (loadThread.get(null) == null ? "0:" : "1:")
                        + Integer.toString(loadingMode.getInt(null)) + ":"
                        + choiceIdsOutput((Vector) resourcesToDownload.get(null));
            } else if (parts[0].equals("resource-path") && parts.length == 6) {
                gameId.set(null, utf16(parts[5]));
                result = utf16Output((String) resourcePath.invoke(null,
                        Integer.valueOf(value(parts, 1)), Integer.valueOf(value(parts, 2)),
                        utf16(parts[3]), Integer.valueOf(value(parts, 4))));
            } else if (parts[0].equals("resource-path-string") && parts.length == 4) {
                gameId.set(null, utf16(parts[3]));
                result = utf16Output((String) resourcePathForString.invoke(null,
                        Integer.valueOf(value(parts, 1)), utf16(parts[2])));
            } else if (parts[0].equals("resource-path-object") && parts.length == 6) {
                gameId.set(null, utf16(parts[5]));
                Object resourceId;
                if (parts[1].equals("integer")) {
                    resourceId = Integer.valueOf(value(parts, 2));
                } else if (parts[1].equals("string")) {
                    resourceId = utf16(parts[3]);
                } else if (parts[1].equals("null")) {
                    resourceId = null;
                } else {
                    resourceId = new Handle(1);
                }
                try {
                    result = utf16Output((String) resourcePathForObject.invoke(
                            null, resourceId, Integer.valueOf(value(parts, 4))));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof ClassCastException)) {
                        throw exception;
                    }
                    result = "CCE";
                }
            } else if (parts[0].equals("game-language-path") && parts.length == 2) {
                gameId.set(null, utf16(parts[1]));
                result = utf16Output((String) getGameLangPath.invoke(null));
            } else if (parts[0].equals("game-text") && parts.length == 3) {
                gameTexts.set(null, scriptIds(parts[1]));
                try {
                    result = utf16Output((String) getGameText.invoke(
                            null, Integer.valueOf(value(parts, 2))));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("game-text-string") && parts.length == 3) {
                gameTexts.set(null, scriptIds(parts[1]));
                result = utf16Output((String) getGameTextFromString.invoke(
                        null, utf16(parts[2])));
            } else if (parts[0].equals("text-replace") && parts.length == 4) {
                try {
                    result = utf16Output((String) textReplace.invoke(null,
                            utf16(parts[1]), utf16(parts[2]), utf16(parts[3])));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("remove-string-prefix") && parts.length == 3) {
                try {
                    result = scriptIdsOutput((String[]) removeStringPrefix.invoke(
                            null, scriptIds(parts[1]), utf16(parts[2])));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("language-position") && parts.length == 3) {
                languages.set(null, scriptIds(parts[1]));
                try {
                    result = Integer.toString(((Integer) languagePosition.invoke(
                            null, utf16(parts[2]))).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("resource-heap-index") && parts.length == 3) {
                resourceHeapSources.set(null, ints(parts[2]));
                try {
                    result = Integer.toString(((Integer) resourceHeapIndex.invoke(
                            null, Integer.valueOf(value(parts, 1)))).intValue());
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("random-scaled") && parts.length == 4) {
                randomInstance.set(null, parts[1].equals("fixed")
                        ? new FixedRandom(value(parts, 2)) : null);
                try {
                    result = Integer.toString(((Integer) randomScaled.invoke(
                            null, Integer.valueOf(value(parts, 3)))).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("ink-get") && parts.length == 4) {
                Hashtable table = stringTable(parts[2]);
                if (parts[1].equals("variable")) {
                    inkServerVariables.set(null, table);
                } else {
                    inkServerHints.set(null, table);
                }
                try {
                    Method getter = parts[1].equals("variable") ? inkGetVariable : inkGetHint;
                    result = utf16Output((String) getter.invoke(null, utf16(parts[3])));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("ink-set") && parts.length == 7) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    inkSetVariable.invoke(null,
                            utf16(parts[4]), utf16(parts[5]), utf16(parts[6]));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("ink-unset") && parts.length == 5) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    inkUnsetVariable.invoke(null, utf16(parts[4]));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("reset-variables") && parts.length == 4) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    resetVariableSystem.invoke(null);
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("room-set") && parts.length == 5) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    roomSet.invoke(null, utf16(parts[4]));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("room-add") && parts.length == 5) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    roomAdd.invoke(null, utf16(parts[4]));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("room-remove") && parts.length == 4) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    roomRemove.invoke(null);
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("inventory-set") && parts.length == 6) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    inventorySet.invoke(null, utf16(parts[4]), Integer.valueOf(value(parts, 5)));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("inventory-remove") && parts.length == 5) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                String status = "OK";
                try {
                    inventoryRemove.invoke(null, utf16(parts[4]));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if (parts[0].equals("script-set-variable") && parts.length == 7) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                gameChangedSinceLastSave.setBoolean(null, value(parts, 3) != 0);
                Object variableValue;
                if (parts[5].equals("null")) {
                    variableValue = null;
                } else if (parts[5].equals("integer")) {
                    variableValue = Integer.valueOf(value(parts, 6));
                } else if (parts[5].equals("string")) {
                    variableValue = utf16(parts[6]);
                } else {
                    variableValue = new Handle(1);
                }
                String status = "OK";
                try {
                    scriptSetVariable.invoke(null, utf16(parts[4]), variableValue);
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = mutationOutput(status,
                        (Hashtable) inkServerVariables.get(null),
                        (Hashtable) inkServerHints.get(null),
                        gameChangedSinceLastSave.getBoolean(null));
            } else if ((parts[0].equals("script-get-variable")
                    || parts[0].equals("script-get-variable-int")) && parts.length == 4) {
                inkServerVariables.set(null, stringTable(parts[1]));
                inkServerHints.set(null, stringTable(parts[2]));
                try {
                    Object variable = parts[0].equals("script-get-variable")
                            ? scriptGetVariable.invoke(null, utf16(parts[3]))
                            : scriptGetVariableAsInteger.invoke(null, utf16(parts[3]));
                    result = parts[0].equals("script-get-variable")
                            ? variableOutput(variable)
                            : Integer.toString(((Integer) variable).intValue());
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof StringIndexOutOfBoundsException) {
                        result = "SIOOBE";
                    } else if (exception.getCause() instanceof NumberFormatException) {
                        result = "NFE";
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("room-history-size") && parts.length == 2) {
                inkServerVariables.set(null, stringTable(parts[1]));
                try {
                    result = Integer.toString(((Integer) roomHistorySize.invoke(null)).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("inventory-size") && parts.length == 3) {
                inkServerVariables.set(null, stringTable(parts[1]));
                try {
                    result = Integer.toString(((Integer) inventorySize.invoke(
                            null, utf16(parts[2]))).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("room-current") && parts.length == 2) {
                inkServerVariables.set(null, stringTable(parts[1]));
                try {
                    result = utf16Output((String) roomCurrent.invoke(null));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("room-last") && parts.length == 2) {
                inkServerVariables.set(null, stringTable(parts[1]));
                try {
                    result = utf16Output((String) roomLast.invoke(null));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("request-resource-path") && parts.length == 6) {
                gameId.set(null, utf16(parts[5]));
                Object request = requestConstructor.newInstance(
                        Integer.valueOf(value(parts, 1)), utf16(parts[3]));
                requestIntegerId.setInt(request, value(parts, 2));
                requestImageTransform.setInt(request, value(parts, 4));
                result = utf16Output((String) requestResourcePath.invoke(request)) + ":"
                        + utf16Output((String) requestToString.invoke(request));
            } else if (parts[0].equals("request-new-string") && parts.length == 3) {
                Object request = requestConstructor.newInstance(
                        Integer.valueOf(value(parts, 1)), utf16(parts[2]));
                result = requestOutput(request, requestType, requestIntegerId,
                        requestStringId, requestImageTransform);
            } else if (parts[0].equals("request-new-object") && parts.length == 5) {
                Object resourceId;
                if (parts[1].equals("integer")) {
                    resourceId = Integer.valueOf(value(parts, 2));
                } else if (parts[1].equals("string")) {
                    resourceId = utf16(parts[3]);
                } else if (parts[1].equals("null")) {
                    resourceId = null;
                } else {
                    resourceId = new Handle(1);
                }
                try {
                    Object request = requestObjectConstructor.newInstance(
                            resourceId, Integer.valueOf(value(parts, 4)));
                    result = requestOutput(request, requestType, requestIntegerId,
                            requestStringId, requestImageTransform);
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof ClassCastException)) {
                        throw exception;
                    }
                    result = "CCE";
                }
            } else if (parts[0].equals("read-string") && parts.length == 2) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                result = utf16Output((String) readString.invoke(null, stream)) + ":"
                        + inputRemaining(stream);
            } else if (parts[0].equals("read-string-list") && parts.length == 5) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                gameTexts.set(null, scriptIds(parts[2]));
                loadingMode.setInt(null, value(parts, 3));
                saveIsPossible.setBoolean(null, value(parts, 4) != 0);
                String[] strings = (String[]) readStringList.invoke(null, stream);
                result = scriptIdsOutput(strings) + ":"
                        + (saveIsPossible.getBoolean(null) ? "1:" : "0:")
                        + inputRemaining(stream);
            } else if (parts[0].equals("request-from-input") && parts.length == 2) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                Object request = requestFromInput.invoke(null, stream);
                result = (request == null ? "NULL" : requestOutput(
                        request, requestType, requestIntegerId,
                        requestStringId, requestImageTransform)) + ":"
                        + inputRemaining(stream);
            } else if (parts[0].equals("find") && parts.length == 3) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                result = Integer.toString(((Integer) find.invoke(
                        null, stream, utf16(parts[2]))).intValue()) + ":"
                        + inputRemaining(stream);
            } else if (parts[0].equals("write-string") && parts.length == 4) {
                RecordingOutputStream recording = parts[1].equals("null") ? null
                        : new RecordingOutputStream(value(parts, 2));
                java.io.DataOutputStream stream = recording == null ? null
                        : new java.io.DataOutputStream(recording);
                String status;
                try {
                    writeString.invoke(null, stream, utf16(parts[3]));
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = writeOutput(status, recording);
            } else if (parts[0].equals("request-get-id") && parts.length == 5) {
                Object request = requestConstructor.newInstance(
                        Integer.valueOf(value(parts, 1)), utf16(parts[3]));
                requestIntegerId.setInt(request, value(parts, 2));
                requestImageTransform.setInt(request, value(parts, 4));
                result = requestIdOutput(requestGetId.invoke(request));
            } else if (parts[0].equals("request-equals") && parts.length == 10) {
                Object request = requestConstructor.newInstance(
                        Integer.valueOf(value(parts, 1)), utf16(parts[3]));
                requestIntegerId.setInt(request, value(parts, 2));
                requestImageTransform.setInt(request, value(parts, 4));
                Object candidate;
                if (parts[5].equals("same")) {
                    candidate = request;
                } else if (parts[5].equals("null")) {
                    candidate = null;
                } else if (parts[5].equals("other")) {
                    candidate = new Handle(1);
                } else {
                    Object other = requestConstructor.newInstance(
                            Integer.valueOf(value(parts, 6)), utf16(parts[8]));
                    requestIntegerId.setInt(other, value(parts, 7));
                    requestImageTransform.setInt(other, value(parts, 9));
                    candidate = other;
                }
                result = ((Boolean) requestEquals.invoke(request, candidate)).booleanValue()
                        ? "1" : "0";
            } else if (parts[0].equals("description") && parts.length == 1) {
                Object request = requestConstructor.newInstance(Integer.valueOf(0), null);
                result = getDescription.invoke(request) == null ? "NULL" : "NONNULL";
            } else if (parts[0].equals("char") && parts.length == 2) {
                String converted = (String) charToString.invoke(null, Character.valueOf((char) value(parts, 1)));
                result = Integer.toString(converted.charAt(0));
            } else if (parts[0].equals("cmp") && parts.length == 3) {
                try {
                    result = ((Boolean) resourceMergeSortCmp.invoke(null, bytes(parts[1]), bytes(parts[2]))).booleanValue() ? "1" : "0";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("array-copy-string") && parts.length == 7) {
                String[] target = stringHandles(parts[3]);
                String[] source = parts[6].equals("1") ? target : stringHandles(parts[1]);
                String status;
                try {
                    arrayCopyString.invoke(null, source, Integer.valueOf(value(parts, 2)),
                            target, Integer.valueOf(value(parts, 4)), Integer.valueOf(value(parts, 5)));
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        status = "NPE";
                    } else if (exception.getCause() instanceof IndexOutOfBoundsException) {
                        status = "IOOBE";
                    } else {
                        throw exception;
                    }
                }
                result = status + ":" + stringHandlesOutput(target);
            } else if (parts[0].equals("object-convert") && parts.length == 4) {
                Object object;
                if (parts[1].equals("null")) {
                    object = null;
                } else if (parts[1].equals("integer")) {
                    object = Integer.valueOf(value(parts, 2));
                } else if (parts[1].equals("string")) {
                    object = utf16(parts[3]);
                } else {
                    object = new Handle(1);
                }
                result = Integer.toString(((Integer) toInt.invoke(null, object)).intValue()) + ":"
                        + (((Boolean) toBoolean.invoke(null, object)).booleanValue() ? "1" : "0") + ":"
                        + Integer.toString(((Integer) integerArgument.invoke(null, object)).intValue());
            } else if (parts[0].equals("action") && parts.length == 2) {
                try {
                    result = Integer.toString(((Integer) actionKeyIdConvert.invoke(null, utf16(parts[1]))).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("tick-get") && parts.length == 2) {
                tickBasedTimeValue.setInt(null, value(parts, 1));
                result = Integer.toString(((Integer) tickBasedTime.invoke(null)).intValue());
            } else if (parts[0].equals("tick-update") && parts.length == 2) {
                tickBasedTimeValue.setInt(null, value(parts, 1));
                tickBasedTimeUpdate.invoke(null);
                result = Integer.toString(tickBasedTimeValue.getInt(null));
            } else if (parts[0].equals("tick-reset") && parts.length == 2) {
                tickBasedTimeValue.setInt(null, value(parts, 1));
                tickBasedTimeReset.invoke(null);
                result = Integer.toString(tickBasedTimeValue.getInt(null));
            } else if (parts[0].equals("loading") && parts.length == 2) {
                loadThread.set(null, value(parts, 1) == 0 ? null : new Thread());
                result = ((Boolean) loading.invoke(null)).booleanValue() ? "1" : "0";
            } else if (parts[0].equals("scroll") && parts.length == 2) {
                menuScrollTickCounter.setByte(null, (byte) value(parts, 1));
                boolean allowed = ((Boolean) isMenuScrollAllowed.invoke(null)).booleanValue();
                result = (allowed ? "1:" : "0:") + menuScrollTickCounter.getByte(null);
            } else if (parts[0].equals("action-code") && parts.length == 3) {
                actionKeyKeyCodes.set(null, ints(parts[2]));
                try {
                    result = Integer.toString(((Integer) actionKeyKeycodeToActionkey.invoke(null, Integer.valueOf(value(parts, 1)))).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("action-unset") && parts.length == 2) {
                int oldLength = value(parts, 1);
                String[] oldValues = oldLength < 0 ? null : new String[oldLength];
                if (oldValues != null) {
                    for (int index = 0; index < oldValues.length; index++) {
                        oldValues[index] = "old";
                    }
                }
                actionKeyScriptIds.set(null, oldValues);
                actionKeyUnsetAllKeys.invoke(null);
                String[] values = (String[]) actionKeyScriptIds.get(null);
                int nullCount = 0;
                for (int index = 0; index < values.length; index++) {
                    if (values[index] == null) {
                        nullCount++;
                    }
                }
                result = Integer.toString(values.length) + ":" + Integer.toString(nullCount);
            } else if (parts[0].equals("action-init") && parts.length == 3) {
                actionKeyKeyCodes.set(null, ints(parts[1]));
                actionKeyScriptIds.set(null, scriptIds(parts[2]));
                actionKeyInitSystem.invoke(null);
                String[] values = (String[]) actionKeyScriptIds.get(null);
                int nullCount = 0;
                for (int index = 0; index < values.length; index++) {
                    if (values[index] == null) {
                        nullCount++;
                    }
                }
                result = intsOutput((int[]) actionKeyKeyCodes.get(null)) + ":"
                        + Integer.toString(values.length) + ":" + Integer.toString(nullCount);
            } else if (parts[0].equals("action-script") && parts.length == 4) {
                actionKeyKeyCodes.set(null, ints(parts[2]));
                actionKeyScriptIds.set(null, scriptIds(parts[3]));
                try {
                    result = utf16Output((String) actionKeyGetScriptId.invoke(
                            null, Integer.valueOf(value(parts, 1))));
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        int index = ((Integer) actionKeyKeycodeToActionkey.invoke(
                                null, Integer.valueOf(value(parts, 1)))).intValue();
                        result = "AIOOBE:" + Integer.toString(index) + ":"
                                + Integer.toString(((String[]) actionKeyScriptIds.get(null)).length);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("splash-more") && parts.length == 3) {
                currentSplash.setInt(null, value(parts, 1));
                numberOfSplashes.setInt(null, value(parts, 2));
                result = ((Boolean) splashMoreExists.invoke(null)).booleanValue() ? "1" : "0";
            } else if (parts[0].equals("key-init") && parts.length == 12) {
                settingsHash.set(null, settingsTable(parts[1]));
                for (int index = 0; index < keyFields.length; index++) {
                    keyFields[index].setInt(null, value(parts, index + 2));
                }
                String status;
                try {
                    keyInit.invoke(null);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = keyBindingsOutput(status, keyFields);
            } else if (parts[0].equals("key-convert") && parts.length == 12) {
                for (int index = 0; index < keyFields.length; index++) {
                    keyFields[index].setInt(null, value(parts, index + 2));
                }
                result = Integer.toString(((Integer) keyConvertToKeyId.invoke(null, Integer.valueOf(value(parts, 1)))).intValue());
            } else if (parts[0].equals("set-key-status") && parts.length == 7) {
                keyNew.setBoolean(null, value(parts, 1) != 0);
                keyPressed.setBoolean(null, value(parts, 2) != 0);
                keyLastPressed.setInt(null, value(parts, 3));
                menuScrollTickCounter.setByte(null, (byte) value(parts, 4));
                setKeyStatus.invoke(null, Integer.valueOf(value(parts, 5)),
                        Boolean.valueOf(value(parts, 6) != 0));
                result = (keyNew.getBoolean(null) ? "1:" : "0:")
                        + (keyPressed.getBoolean(null) ? "1:" : "0:")
                        + Integer.toString(keyLastPressed.getInt(null)) + ":"
                        + Integer.toString(menuScrollTickCounter.getByte(null));
            } else if ((parts[0].equals("key-pressed")
                    || parts[0].equals("key-released")) && parts.length == 19) {
                loadingMode.setInt(null, value(parts, 1));
                loadBarActive.setBoolean(null, value(parts, 2) != 0);
                gotoDissolveFxCounter.setInt(null, value(parts, 3));
                keyNew.setBoolean(null, value(parts, 4) != 0);
                keyPressed.setBoolean(null, value(parts, 5) != 0);
                keyLastPressed.setInt(null, value(parts, 6));
                menuScrollTickCounter.setByte(null, (byte) value(parts, 7));
                for (int index = 0; index < keyFields.length; index++) {
                    keyFields[index].setInt(null, value(parts, index + 9));
                }
                Object canvasInstance = canvasConstructor.newInstance();
                Method event = parts[0].equals("key-pressed")
                        ? canvasKeyPressed : canvasKeyReleased;
                event.invoke(canvasInstance, Integer.valueOf(value(parts, 8)));
                result = (keyNew.getBoolean(null) ? "1:" : "0:")
                        + (keyPressed.getBoolean(null) ? "1:" : "0:")
                        + Integer.toString(keyLastPressed.getInt(null)) + ":"
                        + Integer.toString(menuScrollTickCounter.getByte(null));
            } else if (parts[0].equals("canvas-paint") && parts.length == 4) {
                Graphics previous = new Graphics();
                Graphics argument = value(parts, 1) == 0 ? null : new Graphics();
                applicationGraphics.set(null, previous);
                fadeFrames.setInt(null, value(parts, 2) == 0 ? 1 : 0);
                demoFrames.setInt(null, 0);
                applicationPainting.setBoolean(null, value(parts, 3) != 0);
                applicationInited.setBoolean(null, false);
                loadingMode.setInt(null, 1);
                loadBarActive.setBoolean(null, false);
                loadingBarMarkerX.setInt(null, 73);
                inkServerVariables.set(null, new Hashtable());
                String status;
                try {
                    canvasPaint.invoke(canvasConstructor.newInstance(), new Object[] {argument});
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                Object capturedGraphics = applicationGraphics.get(null);
                String graphicsState = capturedGraphics == argument
                        ? (argument == null ? "NULL" : "ARG")
                        : capturedGraphics == previous ? "PREVIOUS" : "WRONG";
                result = status + ":" + graphicsState + ":"
                        + (applicationPainting.getBoolean(null) ? "1:" : "0:")
                        + Integer.toString(loadingBarMarkerX.getInt(null));
            } else if (parts[0].equals("canvas-show-notify") && parts.length == 4) {
                applicationMainMenuActive.setBoolean(null, true);
                applicationHiddenCanvas.setBoolean(null, value(parts, 1) != 0);
                applicationCurSoundMode.setBoolean(null, value(parts, 2) != 0);
                gameCanvasLoopCount.setInt(null, value(parts, 3));
                gameCanvasSoundId.set(null, null);
                gameCanvasPlayer.set(null, null);
                firstLoad.setBoolean(null, false);
                String status;
                try {
                    canvasShowNotify.invoke(canvasConstructor.newInstance());
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = status + ":"
                        + (applicationHiddenCanvas.getBoolean(null) ? "1:" : "0:")
                        + Integer.toString(gameCanvasLoopCount.getInt(null));
            } else if (parts[0].equals("wrap-default") && parts.length == 4) {
                currentFont.set(null, value(parts, 3) == 0
                        ? null : Font.getDefaultFont());
                try {
                    result = scriptIdsOutput((String[]) wrapDefault.invoke(
                            null, utf16(parts[1]), Integer.valueOf(value(parts, 2))));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("menu-choice") && parts.length == 2) {
                Object menu = menuConstructor.newInstance();
                menuSelectedChoiceNr.setInt(menu, value(parts, 1));
                result = Integer.toString(((Integer) menuGetChoiceNr.invoke(menu)).intValue());
            } else if ((parts[0].equals("menu-add-object")
                    || parts[0].equals("menu-add-int")) && parts.length == 7) {
                Object menu = menuConstructor.newInstance();
                menuChoiceIds.set(menu, choiceIds(parts[1]));
                menuChoiceTexts.set(menu, choiceTexts(parts[2]));
                menuUpdateBodyLines.setBoolean(menu, value(parts, 3) != 0);
                menuUpdateMenu.setBoolean(menu, value(parts, 4) != 0);
                String status;
                try {
                    if (parts[0].equals("menu-add-object")) {
                        menuAddChoiceObject.invoke(menu, new Object[] {
                            parts[5].equals("n") ? null : new Handle(value(parts, 5)),
                            utf16(parts[6])
                        });
                    } else {
                        menuAddChoiceInt.invoke(menu, Integer.valueOf(value(parts, 5)),
                                utf16(parts[6]));
                    }
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = menuAddOutput(status, menu, menuChoiceIds, menuChoiceTexts,
                        menuUpdateBodyLines, menuUpdateMenu);
            } else if (parts[0].equals("menu-count") && parts.length == 2) {
                Object menu = menuConstructor.newInstance();
                menuChoiceIds.set(menu, choiceIds(parts[1]));
                try {
                    result = Integer.toString(((Integer) menuCountChoices.invoke(menu)).intValue());
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("menu-get-id") && parts.length == 3) {
                Object menu = menuConstructor.newInstance();
                Vector ids = choiceIds(parts[1]);
                menuChoiceIds.set(menu, ids);
                menuSelectedChoiceNr.setInt(menu, value(parts, 2));
                try {
                    result = choiceOutput(menuGetChoiceId.invoke(menu));
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE:" + parts[2] + ":" + Integer.toString(ids.size());
                    } else {
                        throw exception;
                    }
                }
            } else if ((parts[0].equals("menu-next") || parts[0].equals("menu-previous"))
                    && parts.length == 5) {
                Object menu = menuConstructor.newInstance();
                menuChoiceIds.set(menu, choiceIds(parts[1]));
                menuSelectedChoiceNr.setInt(menu, value(parts, 2));
                menuScroll.setInt(menu, value(parts, 3));
                menuUpdateMenu.setBoolean(menu, value(parts, 4) != 0);
                String status = "OK";
                try {
                    if (parts[0].equals("menu-next")) {
                        menuNextChoice.invoke(menu);
                    } else {
                        menuPreviousChoice.invoke(menu);
                    }
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = status + ":" + Integer.toString(menuSelectedChoiceNr.getInt(menu)) + ":"
                        + Integer.toString(menuScroll.getInt(menu)) + ":"
                        + (menuUpdateMenu.getBoolean(menu) ? "1" : "0");
            } else if (parts[0].equals("menu-position") && parts.length == 5) {
                Object menu = menuConstructor.newInstance();
                menuX.setInt(menu, value(parts, 1));
                menuY.setInt(menu, value(parts, 2));
                menuSetPosition.invoke(menu, Integer.valueOf(value(parts, 3)), Integer.valueOf(value(parts, 4)));
                result = Integer.toString(menuX.getInt(menu)) + ":" + Integer.toString(menuY.getInt(menu));
            } else if (parts[0].equals("menu-current") && parts.length == 3) {
                Object menu = menuConstructor.newInstance();
                menuIsCurrent.setBoolean(menu, value(parts, 1) != 0);
                menuSetCurrent.invoke(menu, Boolean.valueOf(value(parts, 2) != 0));
                result = menuIsCurrent.getBoolean(menu) ? "1" : "0";
            } else if (parts[0].equals("menu-scroll-increase") && parts.length == 4) {
                Object menu = menuConstructor.newInstance();
                menuScroll.setInt(menu, value(parts, 1));
                menuTextScrolling.setBoolean(menu, value(parts, 2) != 0);
                menuUpdateMenu.setBoolean(menu, value(parts, 3) != 0);
                menuScrollIncrease.invoke(menu);
                result = Integer.toString(menuScroll.getInt(menu)) + ":"
                        + (menuTextScrolling.getBoolean(menu) ? "1:" : "0:")
                        + (menuUpdateMenu.getBoolean(menu) ? "1" : "0");
            } else if (parts[0].equals("menu-scroll-decrease") && parts.length == 4) {
                Object menu = menuConstructor.newInstance();
                menuScroll.setInt(menu, value(parts, 1));
                menuTextScrolling.setBoolean(menu, value(parts, 2) != 0);
                menuUpdateMenu.setBoolean(menu, value(parts, 3) != 0);
                menuScrollDecrease.invoke(menu);
                result = Integer.toString(menuScroll.getInt(menu)) + ":"
                        + (menuTextScrolling.getBoolean(menu) ? "1:" : "0:")
                        + (menuUpdateMenu.getBoolean(menu) ? "1" : "0");
            } else if (parts[0].equals("menu-top") && parts.length == 5) {
                Object menu = menuConstructor.newInstance();
                menuTopText.set(menu, utf16(parts[1]));
                menuUpdateMenu.setBoolean(menu, value(parts, 2) != 0);
                menuUpdateTopLines.setBoolean(menu, value(parts, 3) != 0);
                menuSetTop.invoke(menu, utf16(parts[4]));
                result = utf16Output((String) menuTopText.get(menu)) + ":"
                        + (menuUpdateTopLines.getBoolean(menu) ? "1:" : "0:")
                        + (menuUpdateMenu.getBoolean(menu) ? "1" : "0");
            } else if (parts[0].equals("menu-softkeys") && parts.length == 5) {
                Object menu = menuConstructor.newInstance();
                menuSoftkeyLeft.set(menu, utf16(parts[1]));
                menuSoftkeyRight.set(menu, utf16(parts[2]));
                menuSetSoftkeyOptions.invoke(menu, utf16(parts[3]), utf16(parts[4]));
                result = utf16Output((String) menuSoftkeyLeft.get(menu)) + ":"
                        + utf16Output((String) menuSoftkeyRight.get(menu));
            } else if (parts[0].equals("menu-resource") && parts.length == 3) {
                Object menu = menuConstructor.newInstance();
                Object oldResource = parts[1].equals("n") ? null
                        : resourceConstructor.newInstance(Integer.valueOf(0), null, Integer.valueOf(0));
                Object newResource = parts[2].equals("n") ? null
                        : resourceConstructor.newInstance(Integer.valueOf(0), null, Integer.valueOf(0));
                menuInvItemResource.set(menu, oldResource);
                menuSetInvItemResource.invoke(menu, newResource);
                Object actual = menuInvItemResource.get(menu);
                result = actual == null ? "n" : actual == newResource ? parts[2] : "WRONG";
            } else if (parts[0].equals("game-resource-new") && parts.length == 4) {
                Object[] opaqueIds = {new Handle(0), new Handle(1), new Handle(2)};
                Object resource = resourceConstructor.newInstance(Integer.valueOf(value(parts, 1)),
                        gameResourceId(parts[2], opaqueIds), Integer.valueOf(value(parts, 3)));
                result = Integer.toString(gameResourceType.getInt(resource)) + ":"
                        + gameResourceIdOutput(gameResourceId.get(resource), opaqueIds) + ":"
                        + (gameResourceImage.get(resource) == null ? "n:" : "NONNULL:")
                        + Integer.toString(gameResourceWidth.getInt(resource)) + ":"
                        + Integer.toString(gameResourceHeight.getInt(resource)) + ":"
                        + Integer.toString(gameResourceRegistrationX.getInt(resource)) + ":"
                        + Integer.toString(gameResourceRegistrationY.getInt(resource)) + ":"
                        + Integer.toString(gameResourceTransform.getInt(resource));
            } else if (parts[0].equals("game-resource-equals") && parts.length == 8) {
                Object[] opaqueIds = {new Handle(0), new Handle(1), new Handle(2)};
                Object resource = resourceConstructor.newInstance(Integer.valueOf(value(parts, 1)),
                        gameResourceId(parts[2], opaqueIds), Integer.valueOf(value(parts, 3)));
                Object candidate;
                if (parts[4].equals("null")) {
                    candidate = null;
                } else if (parts[4].equals("other")) {
                    candidate = new Handle(99);
                } else {
                    candidate = resourceConstructor.newInstance(Integer.valueOf(value(parts, 5)),
                            gameResourceId(parts[6], opaqueIds), Integer.valueOf(value(parts, 7)));
                }
                try {
                    result = ((Boolean) gameResourceEquals.invoke(resource, candidate)).booleanValue()
                            ? "1" : "0";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("game-resource-paint") && parts.length == 12) {
                Object resource = resourceConstructor.newInstance(Integer.valueOf(1), null,
                        Integer.valueOf(0));
                Image image = Image.createImage(1, 1);
                gameResourceImage.set(resource, value(parts, 2) == 0 ? null : image);
                gameResourceWidth.setInt(resource, value(parts, 6));
                gameResourceHeight.setInt(resource, value(parts, 7));
                gameResourceRegistrationX.setInt(resource, value(parts, 8));
                gameResourceRegistrationY.setInt(resource, value(parts, 9));
                int[] transforms = ints(parts[11]);
                int[] originalTransforms = (int[]) gameCanvasTransformTable.get(null);
                if (transforms.length != originalTransforms.length) {
                    throw new IllegalArgumentException("paint transform table must have length 8");
                }
                for (int index = 0; index < transforms.length; index++) {
                    originalTransforms[index] = transforms[index];
                }
                RecordingGraphics graphics = value(parts, 1) == 0 ? null
                        : new RecordingGraphics(image, value(parts, 10) != 0);
                String status;
                try {
                    gameResourcePaint.invoke(resource, graphics,
                            Integer.valueOf(value(parts, 3)), Integer.valueOf(value(parts, 4)),
                            Integer.valueOf(value(parts, 5)));
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        status = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        status = "AIOOBE:" + Integer.toString(value(parts, 5)) + ":"
                                + Integer.toString(originalTransforms.length);
                    } else {
                        throw exception;
                    }
                }
                result = paintOutput(status, graphics);
            } else if (parts[0].equals("game-resource-paint-simple")
                    && parts.length == 7) {
                Object resource = resourceConstructor.newInstance(Integer.valueOf(1), null,
                        Integer.valueOf(0));
                Image image = Image.createImage(1, 1);
                gameResourceImage.set(resource, value(parts, 2) == 0 ? null : image);
                RecordingGraphics graphics = value(parts, 1) == 0 ? null
                        : new RecordingGraphics(image, value(parts, 6) != 0);
                String status;
                try {
                    gameResourcePaintSimple.invoke(resource, graphics,
                            Integer.valueOf(value(parts, 3)), Integer.valueOf(value(parts, 4)),
                            Integer.valueOf(value(parts, 5)));
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = paintSimpleOutput(status, graphics);
            } else if ((parts[0].equals("menu-active") || parts[0].equals("menu-close-all")
                    || parts[0].equals("menu-get-current")) && parts.length == 2) {
                int length = value(parts, 1);
                Vector stack = length < 0 ? null : new Vector();
                if (stack != null) {
                    for (int index = 0; index < length; index++) {
                        stack.addElement(menuConstructor.newInstance());
                    }
                }
                menuStack.set(null, stack);
                try {
                    if (parts[0].equals("menu-active")) {
                        result = ((Boolean) menuActive.invoke(null)).booleanValue() ? "1" : "0";
                    } else if (parts[0].equals("menu-close-all")) {
                        menuCloseAll.invoke(null);
                        result = Integer.toString(((Vector) menuStack.get(null)).size());
                    } else {
                        Object current = menuGetCurrent.invoke(null);
                        result = current == null ? "NULL" : Integer.toString(stack.indexOf(current));
                    }
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    result = "NPE";
                }
            } else if (parts[0].equals("menu-close-current") && parts.length == 3) {
                String flags = parts[2].substring(1);
                Object[] menus = new Object[flags.length()];
                for (int index = 0; index < menus.length; index++) {
                    Object menu = menuConstructor.newInstance();
                    menuIsCurrent.setBoolean(menu, flags.charAt(index) == '1');
                    menus[index] = menu;
                }
                menuStack.set(null, menuStack(parts[1], menus));
                String status;
                try {
                    menuCloseCurrent.invoke(null);
                    status = "OK";
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NullPointerException)) {
                        throw exception;
                    }
                    status = "NPE";
                }
                result = status + ":" + menuStackOutput((Vector) menuStack.get(null), menus) + ":"
                        + menuFlagsOutput(menus, menuIsCurrent);
            } else if (parts[0].equals("ink-new") && parts.length == 4) {
                Object script = value(parts, 1) == 0 ? null : scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), null});
                Object roomObject = value(parts, 3) == 0 ? null
                        : roomObjectConstructor.newInstance(new Object[]{null, null});
                Object interpreter = interpreterConstructor.newInstance(
                        new Object[]{script, Integer.valueOf(value(parts, 2)), roomObject});
                result = (interpreterScript.get(interpreter) == null ? "0:" :
                        interpreterScript.get(interpreter) == script ? "1:" : "WRONG:")
                        + Integer.toString(interpreterStatus.getInt(interpreter)) + ":"
                        + Integer.toString(interpreterOffset.getInt(interpreter)) + ":"
                        + (interpreterRoomObject.get(interpreter) == null ? "0:" :
                                interpreterRoomObject.get(interpreter) == roomObject ? "1:" : "WRONG:")
                        + (interpreterLanguageDebugMode.getBoolean(interpreter) ? "1" : "0");
            } else if (parts[0].equals("ink-execute") && parts.length == 8) {
                byte[] data = bytes(parts[3]);
                Object script = null;
                if (value(parts, 2) != 0) {
                    script = scriptConstructor.newInstance(new Object[]{
                            new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                            new String[]{"ret"}});
                    scriptData.set(script, data);
                }
                Object interpreter = interpreterConstructor.newInstance(
                        new Object[]{script, Integer.valueOf(value(parts, 4)), null});
                Object other = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 5) == 0 ? null
                        : value(parts, 5) == 1 ? interpreter : other);
                Object commandResult = null;
                String outcome;
                try {
                    commandResult = parts[1].equals("execute")
                            ? inkInterpreterExecute.invoke(interpreter,
                                    new Object[]{executionValue(parts[6], parts[7])})
                            : inkInterpreterResume.invoke(interpreter);
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE:"
                                + Integer.toString(interpreterOffset.getInt(interpreter) - 1) + ":"
                                + Integer.toString(data.length);
                    } else {
                        throw exception;
                    }
                }
                result = executionOutput(outcome, commandResult, interpreter, other,
                        interpreterStatus, interpreterOffset, pausedThread);
            } else if (parts[0].equals("ink-script-resume") && parts.length == 5) {
                byte[] data = bytes(parts[3]);
                Object script = null;
                if (value(parts, 2) != 0) {
                    script = scriptConstructor.newInstance(new Object[]{
                            new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                            new String[]{"ret"}});
                    scriptData.set(script, data);
                }
                Object interpreter = interpreterConstructor.newInstance(
                        new Object[]{script, Integer.valueOf(value(parts, 4)), null});
                Object other = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 1) == 0 ? null : interpreter);
                String outcome;
                try {
                    inkScriptResume.invoke(null);
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE:"
                                + Integer.toString(interpreterOffset.getInt(interpreter) - 1) + ":"
                                + Integer.toString(data.length);
                    } else {
                        throw exception;
                    }
                }
                result = executionOutput(outcome, null, interpreter, other,
                        interpreterStatus, interpreterOffset, pausedThread);
            } else if (parts[0].equals("ink-script-wait") && parts.length == 6) {
                byte[] data = bytes(parts[4]);
                Object script = null;
                if (value(parts, 3) != 0) {
                    script = scriptConstructor.newInstance(new Object[]{
                            new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                            new String[]{"ret"}});
                    scriptData.set(script, data);
                }
                Object interpreter = interpreterConstructor.newInstance(
                        new Object[]{script, Integer.valueOf(value(parts, 5)), null});
                Object other = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 2) == 0 ? null : interpreter);
                long initialWaitStop = Long.parseLong(parts[1]);
                scriptWaitStop.setLong(null, initialWaitStop);
                Object waiting = null;
                String outcome;
                try {
                    waiting = new Integer(((Boolean) inkScriptIsWaiting.invoke(null))
                            .booleanValue() ? 1 : 0);
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE:"
                                + Integer.toString(interpreterOffset.getInt(interpreter) - 1)
                                + ":" + Integer.toString(data.length);
                    } else {
                        throw exception;
                    }
                }
                String waitStop = initialWaitStop == 1L && scriptWaitStop.getLong(null) != 0L
                        ? "T" : Long.toString(scriptWaitStop.getLong(null));
                result = executionOutput(outcome, waiting, interpreter, other,
                        interpreterStatus, interpreterOffset, pausedThread)
                        + ":" + waitStop;
            } else if (parts[0].equals("ink-script-execute") && parts.length == 10) {
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"}});
                scriptData.set(script, bytes(parts[2]));
                scriptEventOffsets.set(script, ints(parts[3]));
                Object roomObject = value(parts, 7) == 0 ? null
                        : roomObjectConstructor.newInstance(new Object[]{null, null});
                Object oldPaused = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 9) == 0 ? null : oldPaused);
                Object commandResult = null;
                String outcome;
                try {
                    commandResult = parts[1].equals("default")
                            ? inkScriptExecuteEvent.invoke(script,
                                    Integer.valueOf(value(parts, 4)),
                                    executionValue(parts[5], parts[6]), roomObject)
                            : inkScriptExecuteEventDebug.invoke(script,
                                    Integer.valueOf(value(parts, 4)),
                                    executionValue(parts[5], parts[6]), roomObject,
                                    Boolean.valueOf(value(parts, 8) != 0));
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
                result = eventExecutionOutput(outcome, commandResult, script, roomObject,
                        oldPaused, pausedThread, interpreterStatus, interpreterOffset,
                        interpreterScript, interpreterRoomObject, interpreterLanguageDebugMode);
            } else if (parts[0].equals("ink-script-execute-id") && parts.length == 10) {
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"}});
                scriptData.set(script, bytes(parts[3]));
                scriptEventOffsets.set(script, ints(parts[4]));
                Hashtable scripts = null;
                if (!parts[1].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[1].equals("-")) {
                        Object stored = parts[1].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[1].substring(1)), stored);
                    }
                }
                scriptList.set(null, scripts);
                Object roomObject = value(parts, 8) == 0 ? null
                        : roomObjectConstructor.newInstance(new Object[]{null, null});
                Object oldPaused = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 9) == 0 ? null : oldPaused);
                Object commandResult = null;
                String outcome;
                try {
                    commandResult = inkScriptExecuteEventById.invoke(null,
                            utf16(parts[2]), Integer.valueOf(value(parts, 5)),
                            executionValue(parts[6], parts[7]), roomObject);
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ClassCastException) {
                        outcome = "CCE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
                result = eventExecutionOutput(outcome, commandResult, script, roomObject,
                        oldPaused, pausedThread, interpreterStatus, interpreterOffset,
                        interpreterScript, interpreterRoomObject, interpreterLanguageDebugMode);
            } else if (parts[0].equals("inventory-equip") && parts.length == 8) {
                int stackLength = value(parts, 1);
                Vector stack = stackLength < 0 ? null : new Vector();
                if (stack != null) {
                    for (int index = 0; index < stackLength; index++) {
                        stack.addElement(new Handle(index));
                    }
                }
                menuStack.set(null, stack);
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"}});
                scriptData.set(script, bytes(parts[4]));
                scriptEventOffsets.set(script, ints(parts[5]));
                Hashtable scripts = null;
                if (!parts[3].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[3].equals("-")) {
                        Object stored = parts[3].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[3].substring(1)), stored);
                    }
                }
                scriptList.set(null, scripts);
                scriptItemId.set(null, utf16(parts[2]));
                Object oldPaused = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 7) == 0 ? null : oldPaused);
                String outcome;
                try {
                    inventoryEquipUnequipHandling.invoke(
                            null, Integer.valueOf(value(parts, 6)));
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ClassCastException) {
                        outcome = "CCE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
                Vector finalStack = (Vector) menuStack.get(null);
                String stackOutput = finalStack == null ? "null"
                        : finalStack.size() == 0 ? "-" : Integer.toString(finalStack.size());
                result = eventExecutionOutput(outcome, null, script, null,
                        oldPaused, pausedThread, interpreterStatus, interpreterOffset,
                        interpreterScript, interpreterRoomObject, interpreterLanguageDebugMode)
                        + ":" + stackOutput;
            } else if (parts[0].equals("room-event") && parts.length == 12) {
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"}});
                scriptData.set(script, bytes(parts[5]));
                scriptEventOffsets.set(script, ints(parts[6]));
                Hashtable scripts = null;
                if (!parts[4].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[4].equals("-")) {
                        Object stored = parts[4].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[4].substring(1)), stored);
                    }
                }
                scriptList.set(null, scripts);
                Object roomObject = roomObjectConstructor.newInstance(new Object[]{null, null});
                roomScriptId.set(roomObject, utf16(parts[3]));
                roomScript.set(roomObject, value(parts, 2) == 0 ? null : script);
                Object oldPaused = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 11) == 0 ? null : oldPaused);
                Object commandResult = null;
                String outcome;
                try {
                    if (parts[1].equals("execute")) {
                        commandResult = roomExecuteEvent.invoke(roomObject,
                                Integer.valueOf(value(parts, 7)),
                                executionValue(parts[8], parts[9]),
                                Boolean.valueOf(value(parts, 10) != 0));
                    } else if (parts[1].equals("name")) {
                        commandResult = roomGetName.invoke(roomObject);
                    } else if (parts[1].equals("move")) {
                        commandResult = roomGetMoveDirection.invoke(roomObject);
                    } else {
                        commandResult = roomEnterHover.invoke(roomObject);
                    }
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ClassCastException) {
                        outcome = "CCE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
                Object reportedResult = parts[1].equals("hover") ? null : commandResult;
                result = eventExecutionOutput(outcome, reportedResult, script, roomObject,
                        oldPaused, pausedThread, interpreterStatus, interpreterOffset,
                        interpreterScript, interpreterRoomObject, interpreterLanguageDebugMode)
                        + (parts[1].equals("hover")
                                ? ":" + (commandResult == null ? "N"
                                        : commandResult == roomObject ? "R" : "WRONG")
                                : "")
                        + ":" + (roomScript.get(roomObject) == null ? "N"
                                : roomScript.get(roomObject) == script ? "S" : "WRONG");
            } else if (parts[0].equals("ink-script-item-name") && parts.length == 6) {
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"}});
                scriptData.set(script, bytes(parts[3]));
                scriptEventOffsets.set(script, ints(parts[4]));
                Hashtable scripts = null;
                if (!parts[1].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[1].equals("-")) {
                        Object stored = parts[1].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[1].substring(1)), stored);
                    }
                }
                scriptList.set(null, scripts);
                Object oldPaused = interpreterConstructor.newInstance(
                        new Object[]{null, Integer.valueOf(0), null});
                pausedThread.set(null, value(parts, 5) == 0 ? null : oldPaused);
                Object itemName = null;
                String outcome;
                try {
                    itemName = inkScriptGetItemName.invoke(null, utf16(parts[2]));
                    outcome = "OK";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        outcome = "NPE";
                    } else if (exception.getCause() instanceof ClassCastException) {
                        outcome = "CCE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        outcome = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
                result = eventExecutionOutput(outcome, itemName, script, null,
                        oldPaused, pausedThread, interpreterStatus, interpreterOffset,
                        interpreterScript, interpreterRoomObject, interpreterLanguageDebugMode);
            } else if ((parts[0].equals("ink-read") && parts.length == 4)
                    || ((parts[0].equals("ink-read-n") || parts[0].equals("ink-read-signed"))
                            && parts.length == 5)) {
                Object script = null;
                byte[] data = bytes(parts[2]);
                if (value(parts, 1) != 0) {
                    script = scriptConstructor.newInstance(new Object[]{
                            new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), null});
                    scriptData.set(script, data);
                }
                Object interpreter = interpreterConstructor.newInstance(
                        new Object[]{script, Integer.valueOf(value(parts, 3)), null});
                Method reader = parts[0].equals("ink-read") ? inkInterpreterRead
                        : parts[0].equals("ink-read-n") ? inkInterpreterReadBytes
                        : inkInterpreterReadSigned;
                Object[] readerArguments = parts[0].equals("ink-read")
                        ? new Object[0] : new Object[]{Integer.valueOf(value(parts, 4))};
                try {
                    int readValue = ((Integer) reader.invoke(interpreter, readerArguments)).intValue();
                    result = "OK:" + Integer.toString(readValue) + ":"
                            + Integer.toString(interpreterOffset.getInt(interpreter));
                } catch (InvocationTargetException exception) {
                    int updatedOffset = interpreterOffset.getInt(interpreter);
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE:" + Integer.toString(updatedOffset);
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE:" + Integer.toString(updatedOffset - 1) + ":"
                                + Integer.toString(data.length) + ":" + Integer.toString(updatedOffset);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("ink-has-command") && parts.length == 6) {
                Object script = null;
                byte[] data = bytes(parts[2]);
                if (value(parts, 1) != 0) {
                    script = scriptConstructor.newInstance(new Object[]{
                            new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                            scriptIds(parts[3])});
                    scriptData.set(script, data);
                }
                Object interpreter = interpreterConstructor.newInstance(
                        new Object[]{script, Integer.valueOf(value(parts, 4)), null});
                try {
                    boolean found = ((Boolean) inkInterpreterHasCommand.invoke(
                            interpreter, Integer.valueOf(value(parts, 5)))).booleanValue();
                    result = "OK:" + (found ? "1:" : "0:")
                            + Integer.toString(interpreterOffset.getInt(interpreter));
                } catch (InvocationTargetException exception) {
                    int updatedOffset = interpreterOffset.getInt(interpreter);
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE:" + Integer.toString(updatedOffset);
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE:" + Integer.toString(updatedOffset);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("ink-script-has-command") && parts.length == 5) {
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        scriptIds(parts[2])});
                scriptData.set(script, bytes(parts[1]));
                scriptEventOffsets.set(script, ints(parts[3]));
                try {
                    boolean found = ((Boolean) inkScriptHasCommand.invoke(
                            script, Integer.valueOf(value(parts, 4)))).booleanValue();
                    result = found ? "OK:1" : "OK:0";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE";
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("ink-get-string") && parts.length == 3) {
                String[] strings = scriptIds(parts[1]);
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), strings});
                try {
                    result = utf16Output((String) inkScriptGetString.invoke(
                            script, Integer.valueOf(value(parts, 2))));
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE:" + Integer.toString(value(parts, 2) - 1) + ":"
                                + Integer.toString(strings.length);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("ink-has-event") && parts.length == 3) {
                int[] offsets = ints(parts[1]);
                Object script = scriptConstructor.newInstance(new Object[]{
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), null});
                scriptEventOffsets.set(script, offsets);
                try {
                    result = ((Boolean) inkScriptHasEvent.invoke(
                            script, Integer.valueOf(value(parts, 2)))).booleanValue() ? "1" : "0";
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE:" + parts[2] + ":" + Integer.toString(offsets.length);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("ink-stop") && parts.length == 2) {
                pausedThread.set(null, value(parts, 1) == 0 ? null
                        : interpreterConstructor.newInstance(new Object[]{null, Integer.valueOf(0), null}));
                inkScriptStop.invoke(null);
                result = pausedThread.get(null) == null ? "0" : "1";
            } else if (parts[0].equals("panel-new") && parts.length == 5) {
                Object object = roomObjectConstructor.newInstance(new Object[]{null, null});
                battlePanelId.setInt(object, value(parts, 1));
                battlePanel.set(object, ints(parts[2]));
                panelSize.setInt(null, value(parts, 4));
                try {
                    battlePanelNew.invoke(object, Integer.valueOf(value(parts, 3)));
                    result = "OK:" + battlePanelId.getInt(object) + ":"
                            + intsOutput((int[]) battlePanel.get(object));
                } catch (InvocationTargetException exception) {
                    if (!(exception.getCause() instanceof NegativeArraySizeException)) {
                        throw exception;
                    }
                    result = "NAS:" + value(parts, 4) + ":" + battlePanelId.getInt(object) + ":"
                            + intsOutput((int[]) battlePanel.get(object));
                }
            } else if ((parts[0].equals("panel-max") || parts[0].equals("panel-health")
                    || parts[0].equals("panel-bar") || parts[0].equals("panel-time"))
                    && parts.length == 5) {
                Object object = roomObjectConstructor.newInstance(new Object[]{null, null});
                battlePanelId.setInt(object, value(parts, 1));
                battlePanel.set(object, ints(parts[2]));
                Field indexField;
                Method setter;
                if (parts[0].equals("panel-max")) {
                    indexField = panelMaxHealth;
                    setter = bpSetMaxHealth;
                } else if (parts[0].equals("panel-health")) {
                    indexField = panelHealth;
                    setter = bpSetHealth;
                } else if (parts[0].equals("panel-bar")) {
                    indexField = panelBarSize;
                    setter = bpSetBarSize;
                } else {
                    indexField = panelTime;
                    setter = bpSetTime;
                }
                indexField.setInt(null, value(parts, 3));
                try {
                    setter.invoke(object, Integer.valueOf(value(parts, 4)));
                    result = "OK:" + battlePanelId.getInt(object) + ":"
                            + intsOutput((int[]) battlePanel.get(object));
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE:" + battlePanelId.getInt(object) + ":null";
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        int[] values = (int[]) battlePanel.get(object);
                        result = "AIOOBE:" + value(parts, 3) + ":" + values.length + ":"
                                + battlePanelId.getInt(object) + ":" + intsOutput(values);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("room-is-over") && parts.length == 9) {
                Object object = roomObjectConstructor.newInstance(new Object[]{null, null});
                roomVisible.setBoolean(object, value(parts, 1) != 0);
                roomActive.setBoolean(object, value(parts, 2) != 0);
                roomLeft.setInt(object, value(parts, 3));
                roomRight.setInt(object, value(parts, 4));
                roomTop.setInt(object, value(parts, 5));
                roomBottom.setInt(object, value(parts, 6));
                result = ((Boolean) roomIsOver.invoke(object, Integer.valueOf(value(parts, 7)),
                        Integer.valueOf(value(parts, 8)))).booleanValue() ? "1" : "0";
            } else {
                throw new IllegalArgumentException("invalid oracle request: " + line);
            }
            System.out.println(result);
        }
    }
}
