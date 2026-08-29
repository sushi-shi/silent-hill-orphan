package defpackage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

/** Calls the actual canonical Java methods for differential testing. */
public final class OrphanJavaPureOracle {
    private static final class OracleCanvas extends Canvas {
        protected void paint(Graphics graphics) {}
    }

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

    private static String repaintCanvasIdentity(Canvas canvas) {
        if (canvas == null) {
            return "null";
        }
        if (canvas == Canvas.oracleRepaintCanvas1) {
            return "1";
        }
        if (canvas == Canvas.oracleRepaintCanvas2) {
            return "2";
        }
        if (canvas == Canvas.oracleRepaintCanvas3) {
            return "3";
        }
        return "WRONG";
    }

    private static String repaintCanvasIfPossibleOutput(
            boolean initialPainting,
            boolean canvasPresent,
            int repaintMode,
            int serviceRepaintsMode) {
        final Canvas canvas1 = new OracleCanvas();
        final Canvas canvas2 = new OracleCanvas();
        final Canvas canvas3 = new OracleCanvas();
        Canvas.oracleResetRepaint(
                canvas1,
                canvas2,
                canvas3,
                repaintMode,
                serviceRepaintsMode,
                new Canvas.OracleRepaintApplication() {
                    public void setCanvas(Canvas canvas) {
                        Application.canvas = canvas;
                    }

                    public void setPainting(boolean painting) {
                        Application.painting = painting;
                    }

                    public void repaintCanvasIfPossible() {
                        Application.repaintCanvasIfPossible();
                    }
                });
        Application.painting = initialPainting;
        Application.canvas = canvasPresent ? canvas1 : null;
        String status;
        try {
            Application.repaintCanvasIfPossible();
            status = "OK";
        } catch (Canvas.OracleRepaintFailure exception) {
            status = "REPAINT";
        } catch (Canvas.OracleServiceRepaintsFailure exception) {
            status = "SERVICE";
        } catch (NullPointerException exception) {
            status = Canvas.oracleRepaintTrace.length() == 0 ? "NPE-R" : "NPE-S";
        }
        String trace = Canvas.oracleRepaintTrace.length() == 0
                ? "-" : Canvas.oracleRepaintTrace.toString();
        String result = status + ":" + trace + ":"
                + (Application.painting ? "1" : "0") + ":"
                + repaintCanvasIdentity(Application.canvas);
        Canvas.oracleResetRepaint(null, null, null, 0, 0, null);
        return result;
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
        if (!token.startsWith("h") || (token.length() & 1) == 0) {
            throw new IllegalArgumentException("invalid byte token: " + token);
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
        if (!token.startsWith("u") || (token.length() - 1) % 4 != 0) {
            throw new IllegalArgumentException("invalid UTF-16 token: " + token);
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

    private static String gameResourceOutput(GameResource resource, Object[] opaqueIds) {
        return Integer.toString(resource.type) + ":"
                + gameResourceIdOutput(resource.ID, opaqueIds) + ":"
                + (resource.image == null ? "n:" : "NONNULL:")
                + Integer.toString(resource.imageWidth) + ":"
                + Integer.toString(resource.imageHeight) + ":"
                + Integer.toString(resource.imageRegPointX) + ":"
                + Integer.toString(resource.imageRegPointY) + ":"
                + Integer.toString(resource.imageTransform);
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

    private static String pausedThreadOutput(InkInterpreter paused,
            InkInterpreter self, InkInterpreter other) {
        if (paused == null) {
            return "N";
        }
        if (paused == self) {
            return "S";
        }
        return paused == other ? "O" : "WRONG";
    }

    private static String executionOutput(String outcome, Object result,
            InkInterpreter interpreter, InkInterpreter other) {
        return outcome + ":" + variableOutput(result) + ":"
                + Integer.toString(interpreter.status) + ":"
                + Integer.toString(interpreter.offset) + ":"
                + pausedThreadOutput(InkInterpreter.pausedThread, interpreter, other);
    }

    private static String eventExecutionOutput(String outcome, Object result,
            InkScript script, RoomObject roomObject, InkInterpreter oldPaused) {
        InkInterpreter paused = InkInterpreter.pausedThread;
        if (paused == null) {
            return outcome + ":" + variableOutput(result) + ":N:N:N:N:N:N";
        }
        return outcome + ":" + variableOutput(result) + ":"
                + (paused == oldPaused ? "O:" : "I:")
                + Integer.toString(paused.status) + ":"
                + Integer.toString(paused.offset) + ":"
                + (paused.script == null ? "N:" : paused.script == script ? "S:" : "WRONG:")
                + (paused.roomObject == null ? "N:"
                        : paused.roomObject == roomObject ? "R:" : "WRONG:")
                + (paused.languageDebugMode ? "1" : "0");
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

    private static String menuAddOutput(String status, MenuModel menu) {
        return status + ":" + choiceIdsOutput(menu.choiceIDs) + ":"
                + choiceTextsOutput(menu.choiceTexts) + ":"
                + (menu.updateBodyLines ? "1:" : "0:")
                + (menu.updateMenu ? "1" : "0");
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

    private static String requestOutput(ResourceRequest request) {
        return Integer.toString(request.type) + ":" + Integer.toString(request.integerID)
                + ":" + utf16Output(request.stringID) + ":"
                + Integer.toString(request.imageTransform);
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

    private static String roomObjectStaticsOutput(Class owner) throws Exception {
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

    private static String requestIdOutput(Object value) {
        return value instanceof Integer
                ? "I:" + Integer.toString(((Integer) value).intValue())
                : "S:" + utf16Output((String) value);
    }

    private static String keyStateOutput() {
        return (Application.keyNew ? "1:" : "0:")
                + (Application.keyPressed ? "1:" : "0:")
                + Integer.toString(Application.keyLastPressed) + ":"
                + Integer.toString(InkEngine.menuScrollTickCounter);
    }

    private static void setKeyConfiguration(String[] parts, int start) {
        GameCanvas.keySoftkeyLeft = value(parts, start);
        GameCanvas.keySoftkeyRight = value(parts, start + 1);
        GameCanvas.keySoftkeyCenter = value(parts, start + 2);
        GameCanvas.keyArrowUp = value(parts, start + 3);
        GameCanvas.keyArrowDown = value(parts, start + 4);
        GameCanvas.keyArrowLeft = value(parts, start + 5);
        GameCanvas.keyArrowRight = value(parts, start + 6);
        GameCanvas.keyReturn = value(parts, start + 7);
        GameCanvas.keyErase = value(parts, start + 8);
        GameCanvas.keySend = value(parts, start + 9);
    }

    private static String keyBindingsOutput(String status) {
        return status + ":" + Integer.toString(GameCanvas.keySoftkeyLeft)
                + ":" + Integer.toString(GameCanvas.keySoftkeyRight)
                + ":" + Integer.toString(GameCanvas.keySoftkeyCenter)
                + ":" + Integer.toString(GameCanvas.keyArrowUp)
                + ":" + Integer.toString(GameCanvas.keyArrowDown)
                + ":" + Integer.toString(GameCanvas.keyArrowLeft)
                + ":" + Integer.toString(GameCanvas.keyArrowRight)
                + ":" + Integer.toString(GameCanvas.keyReturn)
                + ":" + Integer.toString(GameCanvas.keyErase)
                + ":" + Integer.toString(GameCanvas.keySend);
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

    private static Field field(Class owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static Method method(Class owner, String name, Class... parameters) throws Exception {
        Method result = owner.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    public static void main(String[] args) throws Exception {
        Field cheatLastKey = field(CheatController.class, "lastKey");
        Field roomScriptId = field(RoomObject.class, "scriptID");
        Field menuIsCurrent = field(MenuModel.class, "isCurrent");
        Field panelMaxHealth = field(RoomObject.class, "BATTLE_PANEL_MAX_HEALTH");
        Field panelHealth = field(RoomObject.class, "BATTLE_PANEL_HEALTH");
        Field panelBarSize = field(RoomObject.class, "BATTLE_PANEL_BAR_SIZE");
        Field panelTime = field(RoomObject.class, "BATTLE_PANEL_TIME");
        Field panelSize = field(RoomObject.class, "BATTLE_PANEL_SIZE");
        Field scriptData = field(InkScript.class, "data");
        Field scriptEventOffsets = field(InkScript.class, "eventOffsets");
        Field scriptStringList = field(InkScript.class, "stringList");
        Field scriptGfxId = field(InkScript.class, "gfxID");
        Field hudAmmoNumberWidth = field(SilentHillGame.class, "HUD_ammoNumWidth");
        Field hudAmmoUpdateNeeded = field(SilentHillGame.class, "HUD_ammoUpdateNeeded");
        Field ingameMargin = field(InkEngine.class, "ingameMargin");
        Method splashMoreExists = method(InkEngine.class, "splashMoreExists");
        Method getGameLangPath = method(Application.class, "getGameLangPath");
        Method inkInterpreterRead = method(InkInterpreter.class, "read");
        Method inkInterpreterReadBytes = method(InkInterpreter.class, "read", Integer.TYPE);
        Method inkInterpreterReadSigned = method(InkInterpreter.class, "readSigned", Integer.TYPE);
        Method keyJadEntryAsInt = method(GameCanvas.class, "keyJadEntryAsInt", String.class);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String line;
        while ((line = input.readLine()) != null) {
            String[] parts = line.split(" ");
            String result;
            if (parts[0].equals("cheat-init") && parts.length == 1) {
                result = Integer.toString(cheatLastKey.getInt(null));
            } else if (parts[0].equals("min") && parts.length == 3) {
                result = Integer.toString(Application.min(value(parts, 1), value(parts, 2)));
            } else if (parts[0].equals("ink-script-init") && parts.length == 1) {
                result = InkScript.list == null ? "null" : Integer.toString(InkScript.list.size());
            } else if (parts[0].equals("room-object-init") && parts.length == 1) {
                result = roomObjectStaticsOutput(RoomObject.class);
            } else if (parts[0].equals("menu-init") && parts.length == 1) {
                result = MenuModel.stack == null ? "null" : Integer.toString(MenuModel.stack.size());
            } else if (parts[0].equals("game-resource-init") && parts.length == 1) {
                result = (GameResource.imagesLRE == null ? "null"
                        : Integer.toString(GameResource.imagesLRE.size()))
                        + ":" + (GameResource.imagesImportants == null ? "null"
                                : Integer.toString(GameResource.imagesImportants.size()))
                        + ":" + (GameResource.imagesLRE != GameResource.imagesImportants ? "1" : "0");
            } else if (parts[0].equals("ink-script-new") && parts.length == 3) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                InkScript script = new InkScript(stream, scriptIds(parts[2]));
                result = inkScriptOutput(script, scriptData, scriptEventOffsets,
                        scriptStringList, scriptGfxId, stream);
            } else if (parts[0].equals("room-object-new") && parts.length == 3) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                RoomObject roomObject = new RoomObject(stream, scriptIds(parts[2]));
                result = roomObjectOutput(roomObject, stream);
            } else if (parts[0].equals("max") && parts.length == 3) {
                result = Integer.toString(Application.max(value(parts, 1), value(parts, 2)));
            } else if (parts[0].equals("abs") && parts.length == 2) {
                result = Integer.toString(Application.abs(value(parts, 1)));
            } else if (parts[0].equals("dir") && parts.length == 2) {
                result = Integer.toString(Application.dir(value(parts, 1)));
            } else if (parts[0].equals("left") && parts.length == 7) {
                result = Integer.toString(Application.getLeft(value(parts, 1), value(parts, 2), value(parts, 3),
                        value(parts, 4), value(parts, 5), value(parts, 6)));
            } else if (parts[0].equals("top") && parts.length == 7) {
                result = Integer.toString(Application.getTop(value(parts, 1), value(parts, 2), value(parts, 3),
                        value(parts, 4), value(parts, 5), value(parts, 6)));
            } else if (parts[0].equals("resource-exit") && parts.length == 1) {
                Application.resourceExit();
                result = "OK";
            } else if (parts[0].equals("destroy-app") && parts.length == 3) {
                Application midlet = new Application();
                Application.runtime = Runtime.getRuntime();
                Application.appInited = true;
                Application.midlet = value(parts, 2) == 0 ? null : midlet;
                String outcome;
                try {
                    midlet.destroyApp(value(parts, 1) != 0);
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                }
                result = outcome + ":" + (Application.runtime == null ? "N:" : "R:")
                        + (Application.appInited ? "1" : "0");
            } else if (parts[0].equals("pause-app") && parts.length == 2) {
                Application midlet = new Application();
                Application.mainMenuActive = true;
                Application.hiddenCanvas = value(parts, 1) != 0;
                midlet.pauseApp();
                result = "OK:" + (Application.hiddenCanvas ? "1" : "0");
            } else if (parts[0].equals("app-start") && parts.length == 2) {
                int mode = value(parts, 1);
                RecordingThread ticker = mode == 0 ? null : new RecordingThread(mode == 2);
                Application.tickerThread = ticker;
                String outcome;
                try {
                    Application.appStart();
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                }
                result = outcome + ":" + (ticker == null ? "0" : Integer.toString(ticker.attempts));
            } else if (parts[0].equals("repaint-canvas-if-possible")
                    && parts.length == 5) {
                result = repaintCanvasIfPossibleOutput(
                        value(parts, 1) != 0,
                        parts[2].equals("1"),
                        value(parts, 3),
                        value(parts, 4));
            } else if (parts[0].equals("popup-create") && parts.length == 4) {
                String text = utf16(parts[1]);
                int recoveryCode = value(parts, 2);
                int popupNumber = value(parts, 3);
                InkEngine.popupNumOf = popupNumber;
                InkEngine.popupActive = false;
                InkEngine.popupCurrent = 2;
                InkEngine.popup_choice = (byte) 9;
                InkEngine.popupEndTime = 77L;
                InkEngine.popupRecoveryCode = new int[] {101, 102, 103, 104, 105};
                InkEngine.popupMaxTime = new int[] {201, 202, 203, 204, 205};
                InkEngine.popupText = new String[][] {
                    {"s0"}, {"s1"}, {"s2"}, {"s3"}, {"s4"}
                };
                InkEngine.currentFont = javax.microedition.lcdui.Font.getDefaultFont();
                Application.canvasWidth = 128;
                String outcome;
                try {
                    InkEngine.popupCreate(text, recoveryCode);
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                }
                result = outcome + ":" + InkEngine.popupNumOf + ":"
                        + (InkEngine.popupActive ? "1" : "0") + ":" + InkEngine.popupCurrent
                        + ":" + InkEngine.popup_choice + ":" + InkEngine.popupEndTime + ":"
                        + InkEngine.popupRecoveryCode[popupNumber] + ":"
                        + InkEngine.popupMaxTime[popupNumber] + ":"
                        + utf16Output(InkEngine.popupText[popupNumber][0]);
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
                InkEngine.popupNumOf = initialPopupNumber;
                InkEngine.popupActive = initialPopupActive;
                InkEngine.popupCurrent = 2;
                InkEngine.popup_choice = (byte) 9;
                InkEngine.popupEndTime = 77L;
                InkEngine.popupText = textLength < 0 ? null : new String[textLength][];
                if (InkEngine.popupText != null) {
                    for (int index = 0; index < InkEngine.popupText.length; index++) {
                        InkEngine.popupText[index] = new String[] {"s" + index};
                    }
                }
                InkEngine.popupRecoveryCode = recoveryLength < 0 ? null : new int[recoveryLength];
                if (InkEngine.popupRecoveryCode != null) {
                    for (int index = 0; index < InkEngine.popupRecoveryCode.length; index++) {
                        InkEngine.popupRecoveryCode[index] = 101 + index;
                    }
                }
                InkEngine.popupMaxTime = maximumLength < 0 ? null : new int[maximumLength];
                if (InkEngine.popupMaxTime != null) {
                    for (int index = 0; index < InkEngine.popupMaxTime.length; index++) {
                        InkEngine.popupMaxTime[index] = 201 + index;
                    }
                }
                InkEngine.currentFont = javax.microedition.lcdui.Font.getDefaultFont();
                Application.canvasWidth = requestedCanvasWidth;
                String outcome;
                try {
                    InkEngine.popupCreate(text, recoveryCode, maximumTime);
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    int failedLength;
                    if (initialPopupNumber < 0 || initialPopupNumber >= textLength) {
                        failedLength = textLength;
                    } else if (initialPopupNumber >= recoveryLength) {
                        failedLength = recoveryLength;
                    } else {
                        failedLength = maximumLength;
                    }
                    outcome = "AIOOBE," + initialPopupNumber + "," + failedLength;
                }
                boolean timed = outcome.equals("OK") && initialPopupNumber < 4
                        && !initialPopupActive && maximumTime != -1;
                String endTime = timed && InkEngine.popupEndTime != 77L
                        ? "T" : Long.toString(InkEngine.popupEndTime);
                result = outcome + ":" + InkEngine.popupNumOf + ":"
                        + (InkEngine.popupActive ? "1" : "0") + ":" + InkEngine.popupCurrent
                        + ":" + InkEngine.popup_choice + ":" + endTime + ":"
                        + popupTextsOutput(InkEngine.popupText) + ":"
                        + intsOutput(InkEngine.popupRecoveryCode) + ":"
                        + intsOutput(InkEngine.popupMaxTime) + ":" + (timed ? "1" : "0");
            } else if (parts[0].equals("popup-set-next") && parts.length == 5) {
                int initialPopupCurrent = value(parts, 1);
                int initialPopupNumber = value(parts, 2);
                InkEngine.popupCurrent = initialPopupCurrent;
                InkEngine.popupNumOf = initialPopupNumber;
                InkEngine.popupActive = value(parts, 3) != 0;
                InkEngine.popupMinTimeEnds = 66L;
                InkEngine.popupEndTime = 77L;
                InkEngine.popupMaxTime = ints(parts[4]);
                String outcome;
                try {
                    InkEngine.popupSetNext();
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    int failedLength = InkEngine.popupMaxTime.length;
                    outcome = "AIOOBE," + InkEngine.popupCurrent + "," + failedLength;
                }
                boolean continued = InkEngine.popupCurrent < initialPopupNumber;
                String minimumTime = continued && InkEngine.popupMinTimeEnds != 66L
                        ? "T" : Long.toString(InkEngine.popupMinTimeEnds);
                boolean timed = outcome.equals("OK") && continued
                        && InkEngine.popupMaxTime[InkEngine.popupCurrent] != -1;
                String endTime = timed && InkEngine.popupEndTime != 77L
                        ? "T" : Long.toString(InkEngine.popupEndTime);
                int clockCalls = continued ? (timed ? 2 : 1) : 0;
                result = outcome + ":" + InkEngine.popupCurrent + ":" + InkEngine.popupNumOf
                        + ":" + (InkEngine.popupActive ? "1" : "0") + ":" + minimumTime
                        + ":" + endTime + ":" + intsOutput(InkEngine.popupMaxTime)
                        + ":" + clockCalls;
            } else if (parts[0].equals("default-constructor") && parts.length == 2) {
                if (parts[1].equals("cheat")) {
                    new CheatController();
                } else if (parts[1].equals("game") || parts[1].equals("engine")) {
                    new SilentHillGame();
                } else if (parts[1].equals("application")) {
                    new Application();
                } else if (parts[1].equals("codes")) {
                    new InkCodes();
                } else if (parts[1].equals("text-id")) {
                    new TextId();
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
                    new GameCanvas();
                    status = "OK";
                } catch (NullPointerException exception) {
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
                SilentHillGame.menuResetIngameValues();
                result = ingameMargin.getInt(null) + ":"
                        + hudAmmoNumberWidth.getInt(null) + ":"
                        + (hudAmmoUpdateNeeded.getBoolean(null) ? "1" : "0");
            } else if (parts[0].equals("app-init") && parts.length == 2) {
                Image oldLogo = value(parts, 1) == 0 ? null : Image.createImage(1, 1);
                SilentHillGame.INK_menu_logo = oldLogo;
                Application.midlet = null;
                Application.canvasWidth = Integer.MIN_VALUE;
                Display.oracleReset(0, 0);
                Image.oracleResetStringCreate();
                String status;
                PrintStream previous = System.out;
                try {
                    System.setOut(new PrintStream(new java.io.ByteArrayOutputStream()));
                    SilentHillGame.appInit();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                } finally {
                    System.setOut(previous);
                }
                String requestedPath = Image.oracleStringCreateName == null
                        ? "-" : Image.oracleStringCreateName.substring(1);
                String logo = SilentHillGame.INK_menu_logo == Image.oracleStringCreatedImage
                        ? "NEW" : SilentHillGame.INK_menu_logo == oldLogo ? "OLD" : "WRONG";
                String engineStarted = Application.canvasWidth == Integer.MIN_VALUE ? "0" : "1";
                result = status + ":" + Image.oracleStringCreateCalls + ":"
                        + requestedPath + ":" + logo + ":" + engineStarted;
                Image.oracleResetStringCreate();
            } else if (parts[0].equals("key-jad-entry") && parts.length == 5) {
                Application expectedMidlet = value(parts, 1) == 0 ? null : new Application();
                String expectedKey = utf16(parts[2]);
                Application.midlet = expectedMidlet;
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
                    result = utf16Output(Application.resourceURLEncode(utf16(parts[1])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("coded-string") && parts.length == 2) {
                try {
                    result = utf16Output(Application.codedString(bytes(parts[1])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("print-array") && parts.length == 2) {
                java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
                PrintStream previous = System.out;
                PrintStream recording = new PrintStream(captured, true, "UTF-8");
                String status;
                try {
                    System.setOut(recording);
                    Application.printArray(byteArrays(parts[1]));
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                } finally {
                    recording.flush();
                    System.setOut(previous);
                }
                result = status + ":" + bytesOutput(captured.toByteArray());
            } else if (parts[0].equals("room-repaint-run") && parts.length == 3) {
                Thread oldThread = value(parts, 2) == 0 ? null : new Thread();
                Application.roomRepaintThread = oldThread;
                Application.roomGraphics = value(parts, 1) == 0 ? null : new Graphics();
                Application.roomImage = Image.createImage(1, 1);
                Application.roomObjects = new RoomObject[0];
                Application.roomRepainting = false;
                Application.roomRepaintNeeded = true;
                Application.gotoDissolveFXIsSet = false;
                Application.loadBarActive = false;
                String status;
                try {
                    Application.roomRepaintRun();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                String thread = Application.roomRepaintThread == null
                        ? "NULL" : Application.roomRepaintThread == oldThread ? "OLD" : "WRONG";
                result = status + ":" + thread + ":"
                        + (Application.roomRepainting ? "1:" : "0:")
                        + (Application.roomRepaintNeeded ? "1" : "0");
            } else if (parts[0].equals("clear-all-rms") && parts.length == 3) {
                boolean resourceSucceeds = value(parts, 1) != 0;
                int scriptCount = value(parts, 2);
                Application.resourceHeapSourceLRE = new int[7];
                Application.resourceSCData = resourceSucceeds ? new byte[] {73} : null;
                Application.resourceSCCurrentSize = 91;
                Application.resourceImportants = new Vector();
                Application.resourceImportants.addElement("a");
                Application.resourceImportants.addElement("b");
                Application.resourcesToDownload = new Vector();
                Application.resourcesToDownload.addElement("download");
                InkScript.list = scriptCount < 0 ? null : new Hashtable();
                if (InkScript.list != null) {
                    for (int index = 0; index < scriptCount; index++) {
                        InkScript.list.put("script" + index, new Handle(index));
                    }
                }
                String status;
                try {
                    Application.clearAllRMS();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                String subchunk = Application.resourceSCData == null
                        ? "NULL" : Byte.toString(Application.resourceSCData[0]);
                String downloads = Application.resourcesToDownload == null
                        ? "NULL" : Integer.toString(Application.resourcesToDownload.size());
                String scripts = InkScript.list == null
                        ? "NULL" : Integer.toString(InkScript.list.size());
                result = status + ":" + Integer.toString(Application.resourceHeapSourceLRE.length)
                        + ":" + subchunk + ":"
                        + Integer.toString(Application.resourceSCCurrentSize) + ":"
                        + Integer.toString(Application.resourceImportants.size()) + ":"
                        + downloads + ":" + scripts;
            } else if (parts[0].equals("free-memory") && parts.length == 2) {
                Application.runtime = value(parts, 1) == 0 ? null : Runtime.getRuntime();
                try {
                    long available = Application.freeMemory();
                    result = "OK:" + (available >= 0L ? "1" : "0");
                } catch (NullPointerException exception) {
                    result = "NPE:-";
                }
            } else if (parts[0].equals("set-display") && parts.length == 5) {
                Application expectedMidlet = value(parts, 1) == 0 ? null : new Application();
                Displayable expectedCurrent = value(parts, 2) == 0
                        ? null : new Displayable() {};
                Application.midlet = expectedMidlet;
                Display.oracleReset(value(parts, 3), value(parts, 4));
                String status;
                try {
                    Application.setDisplay(expectedCurrent);
                    status = "OK";
                } catch (NullPointerException exception) {
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
                    status = Application.rmsDelete(expectedName) ? "T" : "F";
                } catch (NullPointerException exception) {
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
                Application.saveChunkINI(chunkInput);
                result = RecordStore.oracleOpenCalls + ":"
                        + utf16Output(RecordStore.oracleOpenName) + ":"
                        + (RecordStore.oracleOpenCreate ? "1" : "0") + ":"
                        + RecordStore.oracleSetCalls + ":"
                        + (RecordStore.oracleSetData == null
                                ? "null" : bytesOutput(RecordStore.oracleSetData)) + ":"
                        + RecordStore.oracleSetOffset + ":" + RecordStore.oracleSetLength;
                RecordStore.oracleResetWrite(0);
            } else if (parts[0].equals("resource-make-subchunk") && parts.length == 3) {
                byte[] source = bytes(parts[1]);
                Application.resourceSCData = source;
                Application.resourceSCCurrentSize = value(parts, 2);
                String status;
                String returned = "null";
                String identity = "-";
                try {
                    byte[] subchunk = Application.resourceMakeSubChunk();
                    status = "OK";
                    returned = bytesOutput(subchunk);
                    identity = subchunk == source ? "S" : "D";
                } catch (NegativeArraySizeException exception) {
                    status = "NASE";
                } catch (NullPointerException exception) {
                    status = "NPE";
                } catch (IndexOutOfBoundsException exception) {
                    status = "AIOOBE";
                }
                result = status + ":" + returned + ":" + identity + ":"
                        + (Application.resourceSCData == null
                                ? "null" : bytesOutput(Application.resourceSCData))
                        + ":" + Application.resourceSCCurrentSize;
            } else if (parts[0].equals("resource-restart-importants") && parts.length == 2) {
                int oldLength = value(parts, 1);
                Application.resourceImportants = oldLength < 0 ? null : new Vector();
                if (Application.resourceImportants != null) {
                    for (int index = 0; index < oldLength; index++) {
                        Application.resourceImportants.addElement(Integer.valueOf(index));
                    }
                }
                Application.resourceRestartImportants();
                result = Integer.toString(Application.resourceImportants.size());
            } else if (parts[0].equals("reset-load") && parts.length == 4) {
                Application.loadThread = value(parts, 1) == 0 ? null : new Thread();
                Application.loadingMode = value(parts, 2);
                Application.resourcesToDownload = choiceIds(parts[3]);
                String status;
                try {
                    Application.resetLoad();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = status + ":" + (Application.loadThread == null ? "0:" : "1:")
                        + Integer.toString(Application.loadingMode) + ":"
                        + choiceIdsOutput(Application.resourcesToDownload);
            } else if (parts[0].equals("resource-path") && parts.length == 6) {
                Application.gameId = utf16(parts[5]);
                result = utf16Output(Application.loadRequest_getResourcePath(
                        value(parts, 1), value(parts, 2), utf16(parts[3]), value(parts, 4)));
            } else if (parts[0].equals("resource-path-string") && parts.length == 4) {
                Application.gameId = utf16(parts[3]);
                result = utf16Output(Application.loadRequest_getResourcePath(
                        value(parts, 1), utf16(parts[2])));
            } else if (parts[0].equals("resource-path-object") && parts.length == 6) {
                Application.gameId = utf16(parts[5]);
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
                    result = utf16Output(Application.loadRequest_getResourcePath(
                            resourceId, value(parts, 4)));
                } catch (ClassCastException exception) {
                    result = "CCE";
                }
            } else if (parts[0].equals("game-language-path") && parts.length == 2) {
                Application.gameId = utf16(parts[1]);
                result = utf16Output((String) getGameLangPath.invoke(null));
            } else if (parts[0].equals("game-text") && parts.length == 3) {
                Application.gameTexts = scriptIds(parts[1]);
                try {
                    result = utf16Output(Application.getGameText(value(parts, 2)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("game-text-string") && parts.length == 3) {
                Application.gameTexts = scriptIds(parts[1]);
                result = utf16Output(Application.getGameText(utf16(parts[2])));
            } else if (parts[0].equals("text-replace") && parts.length == 4) {
                try {
                    result = utf16Output(Application.txtStringReplace(
                            utf16(parts[1]), utf16(parts[2]), utf16(parts[3])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("remove-string-prefix") && parts.length == 3) {
                try {
                    result = scriptIdsOutput(Application.removeStringPrefix(
                            scriptIds(parts[1]), utf16(parts[2])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("language-position") && parts.length == 3) {
                Application.languages = scriptIds(parts[1]);
                try {
                    result = Integer.toString(Application.getPosInLanguageSelectionList(
                            utf16(parts[2])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("resource-heap-index") && parts.length == 3) {
                Application.resourceHeapSourceLRE = ints(parts[2]);
                try {
                    result = Integer.toString(Application.resourceIsOnHeap(value(parts, 1)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE";
                }
            } else if (parts[0].equals("random-scaled") && parts.length == 4) {
                Application.randomInstance = parts[1].equals("fixed")
                        ? new FixedRandom(value(parts, 2)) : null;
                try {
                    result = Integer.toString(Application.random(value(parts, 3)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("ink-get") && parts.length == 4) {
                Hashtable table = stringTable(parts[2]);
                if (parts[1].equals("variable")) {
                    Application.inkServerVariables = table;
                } else {
                    Application.inkServerHint = table;
                }
                try {
                    String value = parts[1].equals("variable")
                            ? Application.inkServerGetVariable(utf16(parts[3]))
                            : Application.inkServerGetHint(utf16(parts[3]));
                    result = utf16Output(value);
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("ink-set") && parts.length == 7) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    Application.inkServerSetVariable(
                            utf16(parts[4]), utf16(parts[5]), utf16(parts[6]));
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("ink-unset") && parts.length == 5) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    Application.inkServerUnsetVariable(utf16(parts[4]));
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("reset-variables") && parts.length == 4) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    Application.resetVariableSystem();
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("room-set") && parts.length == 5) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    Application.roomSetCurrent(utf16(parts[4]));
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("room-add") && parts.length == 5) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    Application.roomAddToRoomHistory(utf16(parts[4]));
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("room-remove") && parts.length == 4) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    Application.roomRemoveLastInRoomHistory();
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("inventory-set") && parts.length == 6) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    InkScript.setInventory(utf16(parts[4]), value(parts, 5));
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("inventory-remove") && parts.length == 5) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
                String status = "OK";
                try {
                    InkScript.removeInventory(utf16(parts[4]));
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if (parts[0].equals("script-set-variable") && parts.length == 7) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                Application.gameChangedSinceLastSave = value(parts, 3) != 0;
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
                    InkScript.setVariable(utf16(parts[4]), variableValue);
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = mutationOutput(status, Application.inkServerVariables,
                        Application.inkServerHint, Application.gameChangedSinceLastSave);
            } else if ((parts[0].equals("script-get-variable")
                    || parts[0].equals("script-get-variable-int")) && parts.length == 4) {
                Application.inkServerVariables = stringTable(parts[1]);
                Application.inkServerHint = stringTable(parts[2]);
                try {
                    result = parts[0].equals("script-get-variable")
                            ? variableOutput(InkScript.getVariable(utf16(parts[3])))
                            : Integer.toString(InkScript.getVariableAsInteger(utf16(parts[3])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (StringIndexOutOfBoundsException exception) {
                    result = "SIOOBE";
                } catch (NumberFormatException exception) {
                    result = "NFE";
                }
            } else if (parts[0].equals("room-history-size") && parts.length == 2) {
                Application.inkServerVariables = stringTable(parts[1]);
                try {
                    result = Integer.toString(Application.roomGetHistorySize());
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("inventory-size") && parts.length == 3) {
                Application.inkServerVariables = stringTable(parts[1]);
                try {
                    result = Integer.toString(InkScript.getInventorySize(utf16(parts[2])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("room-current") && parts.length == 2) {
                Application.inkServerVariables = stringTable(parts[1]);
                try {
                    result = utf16Output(Application.roomGetCurrent());
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("room-last") && parts.length == 2) {
                Application.inkServerVariables = stringTable(parts[1]);
                try {
                    result = utf16Output(Application.roomGetLastInRoomHistory());
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("request-resource-path") && parts.length == 6) {
                Application.gameId = utf16(parts[5]);
                ResourceRequest request = new ResourceRequest(value(parts, 1), utf16(parts[3]));
                request.integerID = value(parts, 2);
                request.imageTransform = value(parts, 4);
                result = utf16Output(request.getResourcePath()) + ":" + utf16Output(request.toString());
            } else if (parts[0].equals("request-new-string") && parts.length == 3) {
                result = requestOutput(new ResourceRequest(value(parts, 1), utf16(parts[2])));
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
                    result = requestOutput(new ResourceRequest(resourceId, value(parts, 4)));
                } catch (ClassCastException exception) {
                    result = "CCE";
                }
            } else if (parts[0].equals("read-string") && parts.length == 2) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                result = utf16Output(Application.readString(stream)) + ":"
                        + inputRemaining(stream);
            } else if (parts[0].equals("read-string-list") && parts.length == 5) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                Application.gameTexts = scriptIds(parts[2]);
                Application.loadingMode = value(parts, 3);
                Application.saveIsPossible = value(parts, 4) != 0;
                String[] strings = Application.readStringList(stream);
                result = scriptIdsOutput(strings) + ":"
                        + (Application.saveIsPossible ? "1:" : "0:")
                        + inputRemaining(stream);
            } else if (parts[0].equals("request-from-input") && parts.length == 2) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                ResourceRequest request = ResourceRequest.createFromInputStream(stream);
                result = (request == null ? "NULL" : requestOutput(request)) + ":"
                        + inputRemaining(stream);
            } else if (parts[0].equals("find") && parts.length == 3) {
                byte[] data = bytes(parts[1]);
                DataInputStream stream = data == null ? null
                        : new DataInputStream(new ByteArrayInputStream(data));
                result = Integer.toString(Application.find(stream, utf16(parts[2]))) + ":"
                        + inputRemaining(stream);
            } else if (parts[0].equals("write-string") && parts.length == 4) {
                RecordingOutputStream recording = parts[1].equals("null") ? null
                        : new RecordingOutputStream(value(parts, 2));
                java.io.DataOutputStream stream = recording == null ? null
                        : new java.io.DataOutputStream(recording);
                String status;
                try {
                    Application.writeString(stream, utf16(parts[3]));
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = writeOutput(status, recording);
            } else if (parts[0].equals("request-get-id") && parts.length == 5) {
                ResourceRequest request = new ResourceRequest(value(parts, 1), utf16(parts[3]));
                request.integerID = value(parts, 2);
                request.imageTransform = value(parts, 4);
                result = requestIdOutput(request.getID());
            } else if (parts[0].equals("request-equals") && parts.length == 10) {
                ResourceRequest request = new ResourceRequest(value(parts, 1), utf16(parts[3]));
                request.integerID = value(parts, 2);
                request.imageTransform = value(parts, 4);
                Object candidate;
                if (parts[5].equals("same")) {
                    candidate = request;
                } else if (parts[5].equals("null")) {
                    candidate = null;
                } else if (parts[5].equals("other")) {
                    candidate = new Handle(1);
                } else {
                    ResourceRequest other = new ResourceRequest(value(parts, 6), utf16(parts[8]));
                    other.integerID = value(parts, 7);
                    other.imageTransform = value(parts, 9);
                    candidate = other;
                }
                result = request.equals(candidate) ? "1" : "0";
            } else if (parts[0].equals("description") && parts.length == 1) {
                String description = new ResourceRequest(0, null).getDescription();
                result = description == null ? "NULL" : description;
            } else if (parts[0].equals("char") && parts.length == 2) {
                String converted = Application.charToString((char) value(parts, 1));
                result = Integer.toString(converted.charAt(0));
            } else if (parts[0].equals("cmp") && parts.length == 3) {
                try {
                    result = Application.resourceMergeSortCmp(bytes(parts[1]), bytes(parts[2])) ? "1" : "0";
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("array-copy-string") && parts.length == 7) {
                String[] target = stringHandles(parts[3]);
                String[] source = parts[6].equals("1") ? target : stringHandles(parts[1]);
                String status;
                try {
                    Application.arrayCopyString(source, value(parts, 2), target,
                            value(parts, 4), value(parts, 5));
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                } catch (IndexOutOfBoundsException exception) {
                    status = "IOOBE";
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
                result = Integer.toString(Application.toInt(object)) + ":"
                        + (Application.toBoolean(object) ? "1" : "0") + ":"
                        + Integer.toString(InkInterpreter.integerArgument(object));
            } else if (parts[0].equals("action") && parts.length == 2) {
                try {
                    result = Integer.toString(InkEngine.actionKeyIdConvert(utf16(parts[1])));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("tick-get") && parts.length == 2) {
                Application.tickBasedTimeValue = value(parts, 1);
                result = Integer.toString(Application.tickBasedTime());
            } else if (parts[0].equals("tick-update") && parts.length == 2) {
                Application.tickBasedTimeValue = value(parts, 1);
                Application.tickBasedTimeUpdate();
                result = Integer.toString(Application.tickBasedTimeValue);
            } else if (parts[0].equals("tick-reset") && parts.length == 2) {
                Application.tickBasedTimeValue = value(parts, 1);
                Application.tickBasedTimeReset();
                result = Integer.toString(Application.tickBasedTimeValue);
            } else if (parts[0].equals("loading") && parts.length == 2) {
                Application.loadThread = value(parts, 1) == 0 ? null : new Thread();
                result = Application.loading() ? "1" : "0";
            } else if (parts[0].equals("scroll") && parts.length == 2) {
                InkEngine.menuScrollTickCounter = (byte) value(parts, 1);
                boolean allowed = InkEngine.isMenuScrollAllowed();
                result = (allowed ? "1:" : "0:") + InkEngine.menuScrollTickCounter;
            } else if (parts[0].equals("action-code") && parts.length == 3) {
                InkEngine.actionKey_keyCodes = ints(parts[2]);
                try {
                    result = Integer.toString(InkEngine.actionKeyKeycodeToActionkey(value(parts, 1)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("action-unset") && parts.length == 2) {
                int oldLength = value(parts, 1);
                InkEngine.actionKey_scriptIds = oldLength < 0 ? null : new String[oldLength];
                if (InkEngine.actionKey_scriptIds != null) {
                    for (int index = 0; index < InkEngine.actionKey_scriptIds.length; index++) {
                        InkEngine.actionKey_scriptIds[index] = "old";
                    }
                }
                InkEngine.actionKeyUnsetAllKeys();
                int nullCount = 0;
                for (int index = 0; index < InkEngine.actionKey_scriptIds.length; index++) {
                    if (InkEngine.actionKey_scriptIds[index] == null) {
                        nullCount++;
                    }
                }
                result = Integer.toString(InkEngine.actionKey_scriptIds.length) + ":"
                        + Integer.toString(nullCount);
            } else if (parts[0].equals("action-init") && parts.length == 3) {
                InkEngine.actionKey_keyCodes = ints(parts[1]);
                InkEngine.actionKey_scriptIds = scriptIds(parts[2]);
                InkEngine.actionKeyInitSystem();
                int nullCount = 0;
                for (int index = 0; index < InkEngine.actionKey_scriptIds.length; index++) {
                    if (InkEngine.actionKey_scriptIds[index] == null) {
                        nullCount++;
                    }
                }
                result = intsOutput(InkEngine.actionKey_keyCodes) + ":"
                        + Integer.toString(InkEngine.actionKey_scriptIds.length) + ":"
                        + Integer.toString(nullCount);
            } else if (parts[0].equals("action-script") && parts.length == 4) {
                InkEngine.actionKey_keyCodes = ints(parts[2]);
                InkEngine.actionKey_scriptIds = scriptIds(parts[3]);
                try {
                    result = utf16Output(InkEngine.actionKeyGetScriptId(value(parts, 1)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    int index = InkEngine.actionKeyKeycodeToActionkey(value(parts, 1));
                    result = "AIOOBE:" + Integer.toString(index) + ":"
                            + Integer.toString(InkEngine.actionKey_scriptIds.length);
                }
            } else if (parts[0].equals("splash-more") && parts.length == 3) {
                InkEngine.curSplash = value(parts, 1);
                InkEngine.numOfSplashes = value(parts, 2);
                result = ((Boolean) splashMoreExists.invoke(null)).booleanValue() ? "1" : "0";
            } else if (parts[0].equals("key-init") && parts.length == 12) {
                InkEngine.settingsHash = settingsTable(parts[1]);
                setKeyConfiguration(parts, 2);
                String status;
                try {
                    GameCanvas.keyInit();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = keyBindingsOutput(status);
            } else if (parts[0].equals("key-convert") && parts.length == 12) {
                setKeyConfiguration(parts, 2);
                result = Integer.toString(GameCanvas.keyConvertToKeyId(value(parts, 1)));
            } else if (parts[0].equals("set-key-status") && parts.length == 7) {
                Application.keyNew = value(parts, 1) != 0;
                Application.keyPressed = value(parts, 2) != 0;
                Application.keyLastPressed = value(parts, 3);
                InkEngine.menuScrollTickCounter = (byte) value(parts, 4);
                Application.setKeyStatus(value(parts, 5), value(parts, 6) != 0);
                result = keyStateOutput();
            } else if ((parts[0].equals("key-pressed")
                    || parts[0].equals("key-released")) && parts.length == 19) {
                Application.loadingMode = value(parts, 1);
                Application.loadBarActive = value(parts, 2) != 0;
                Application.gotoDissolveFXCounter = value(parts, 3);
                Application.keyNew = value(parts, 4) != 0;
                Application.keyPressed = value(parts, 5) != 0;
                Application.keyLastPressed = value(parts, 6);
                InkEngine.menuScrollTickCounter = (byte) value(parts, 7);
                setKeyConfiguration(parts, 9);
                GameCanvas canvas = new GameCanvas();
                if (parts[0].equals("key-pressed")) {
                    canvas.keyPressed(value(parts, 8));
                } else {
                    canvas.keyReleased(value(parts, 8));
                }
                result = keyStateOutput();
            } else if (parts[0].equals("canvas-paint") && parts.length == 4) {
                Graphics previous = new Graphics();
                Graphics argument = value(parts, 1) == 0 ? null : new Graphics();
                Application.gfx = previous;
                Application.FADE_FRAMES = value(parts, 2) == 0 ? 1 : 0;
                Application.DEMO_FRAMES = 0;
                Application.painting = value(parts, 3) != 0;
                Application.appInited = false;
                Application.loadingMode = 1;
                Application.loadBarActive = false;
                Application.loadingBarMarkerX = 73;
                Application.inkServerVariables = new Hashtable();
                String status;
                try {
                    new GameCanvas().paint(argument);
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                String graphicsState = Application.gfx == argument
                        ? (argument == null ? "NULL" : "ARG")
                        : Application.gfx == previous ? "PREVIOUS" : "WRONG";
                result = status + ":" + graphicsState + ":"
                        + (Application.painting ? "1:" : "0:")
                        + Integer.toString(Application.loadingBarMarkerX);
            } else if (parts[0].equals("canvas-show-notify") && parts.length == 4) {
                Application.mainMenuActive = true;
                Application.hiddenCanvas = value(parts, 1) != 0;
                Application.curSoundMode = value(parts, 2) != 0;
                GameCanvas.loopCount = value(parts, 3);
                GameCanvas.soundID = null;
                GameCanvas.gPlayer = null;
                InkEngine.FirstLoad = false;
                String status;
                try {
                    new GameCanvas().showNotify();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = status + ":" + (Application.hiddenCanvas ? "1:" : "0:")
                        + Integer.toString(GameCanvas.loopCount);
            } else if (parts[0].equals("canvas-resume-sound") && parts.length == 5) {
                Application.curSoundMode = value(parts, 1) != 0;
                GameCanvas.loopCount = value(parts, 2);
                InkEngine.FirstLoad = value(parts, 3) != 0;
                GameCanvas.soundID = utf16(parts[4]);
                GameCanvas.gPlayer = null;
                String status;
                try {
                    GameCanvas.resumeSound();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = status + ":" + (Application.curSoundMode ? "1:" : "0:")
                        + Integer.toString(GameCanvas.loopCount) + ":"
                        + utf16Output(GameCanvas.soundID) + ":"
                        + (InkEngine.FirstLoad ? "1" : "0");
            } else if (parts[0].equals("wrap-default") && parts.length == 4) {
                InkEngine.currentFont = value(parts, 3) == 0
                        ? null : Font.getDefaultFont();
                try {
                    result = scriptIdsOutput(InkEngine.wrapString(
                            utf16(parts[1]), value(parts, 2)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("menu-choice") && parts.length == 2) {
                MenuModel menu = new MenuModel();
                menu.selectedChoiceNr = value(parts, 1);
                result = Integer.toString(menu.getChoiceNr());
            } else if ((parts[0].equals("menu-add-object")
                    || parts[0].equals("menu-add-int")) && parts.length == 7) {
                MenuModel menu = new MenuModel();
                menu.choiceIDs = choiceIds(parts[1]);
                menu.choiceTexts = choiceTexts(parts[2]);
                menu.updateBodyLines = value(parts, 3) != 0;
                menu.updateMenu = value(parts, 4) != 0;
                String status;
                try {
                    if (parts[0].equals("menu-add-object")) {
                        menu.addChoice(parts[5].equals("n")
                                ? null : new Handle(value(parts, 5)), utf16(parts[6]));
                    } else {
                        menu.addChoice(value(parts, 5), utf16(parts[6]));
                    }
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = menuAddOutput(status, menu);
            } else if (parts[0].equals("menu-count") && parts.length == 2) {
                MenuModel menu = new MenuModel();
                menu.choiceIDs = choiceIds(parts[1]);
                try {
                    result = Integer.toString(menu.countChoices());
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("menu-get-id") && parts.length == 3) {
                MenuModel menu = new MenuModel();
                Vector ids = choiceIds(parts[1]);
                menu.choiceIDs = ids;
                menu.selectedChoiceNr = value(parts, 2);
                try {
                    result = choiceOutput(menu.getChoiceID());
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE:" + parts[2] + ":" + Integer.toString(ids.size());
                }
            } else if ((parts[0].equals("menu-next") || parts[0].equals("menu-previous"))
                    && parts.length == 5) {
                MenuModel menu = new MenuModel();
                menu.choiceIDs = choiceIds(parts[1]);
                menu.selectedChoiceNr = value(parts, 2);
                menu.scroll = value(parts, 3);
                menu.updateMenu = value(parts, 4) != 0;
                String status = "OK";
                try {
                    if (parts[0].equals("menu-next")) {
                        menu.nextChoice();
                    } else {
                        menu.previousChoice();
                    }
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = status + ":" + Integer.toString(menu.selectedChoiceNr) + ":"
                        + Integer.toString(menu.scroll) + ":" + (menu.updateMenu ? "1" : "0");
            } else if (parts[0].equals("menu-position") && parts.length == 5) {
                MenuModel menu = new MenuModel();
                menu.x = value(parts, 1);
                menu.y = value(parts, 2);
                menu.setPosition(value(parts, 3), value(parts, 4));
                result = Integer.toString(menu.x) + ":" + Integer.toString(menu.y);
            } else if (parts[0].equals("menu-current") && parts.length == 3) {
                MenuModel menu = new MenuModel();
                menuIsCurrent.setBoolean(menu, value(parts, 1) != 0);
                menu.setCurrent(value(parts, 2) != 0);
                result = menuIsCurrent.getBoolean(menu) ? "1" : "0";
            } else if (parts[0].equals("menu-scroll-increase") && parts.length == 4) {
                MenuModel menu = new MenuModel();
                menu.scroll = value(parts, 1);
                menu.textScrolling = value(parts, 2) != 0;
                menu.updateMenu = value(parts, 3) != 0;
                menu.scrollIncrease();
                result = Integer.toString(menu.scroll) + ":" + (menu.textScrolling ? "1:" : "0:")
                        + (menu.updateMenu ? "1" : "0");
            } else if (parts[0].equals("menu-scroll-decrease") && parts.length == 4) {
                MenuModel menu = new MenuModel();
                menu.scroll = value(parts, 1);
                menu.textScrolling = value(parts, 2) != 0;
                menu.updateMenu = value(parts, 3) != 0;
                menu.scrollDecrease();
                result = Integer.toString(menu.scroll) + ":" + (menu.textScrolling ? "1:" : "0:")
                        + (menu.updateMenu ? "1" : "0");
            } else if (parts[0].equals("menu-top") && parts.length == 5) {
                MenuModel menu = new MenuModel();
                menu.topText = utf16(parts[1]);
                menu.updateMenu = value(parts, 2) != 0;
                menu.updateTopLines = value(parts, 3) != 0;
                menu.setTop(utf16(parts[4]));
                result = utf16Output(menu.topText) + ":" + (menu.updateTopLines ? "1:" : "0:")
                        + (menu.updateMenu ? "1" : "0");
            } else if (parts[0].equals("menu-softkeys") && parts.length == 5) {
                MenuModel menu = new MenuModel();
                menu.engineSoftkeyOptionLeft = utf16(parts[1]);
                menu.engineSoftkeyOptionRight = utf16(parts[2]);
                menu.setSoftkeyOptions(utf16(parts[3]), utf16(parts[4]));
                result = utf16Output(menu.engineSoftkeyOptionLeft) + ":"
                        + utf16Output(menu.engineSoftkeyOptionRight);
            } else if (parts[0].equals("menu-resource") && parts.length == 3) {
                MenuModel menu = new MenuModel();
                GameResource oldResource = parts[1].equals("n") ? null
                        : new GameResource(0, null, 0);
                GameResource newResource = parts[2].equals("n") ? null
                        : new GameResource(0, null, 0);
                menu.curInvItemResource = oldResource;
                menu.setInvItemResource(newResource);
                result = menu.curInvItemResource == null ? "n"
                        : menu.curInvItemResource == newResource ? parts[2] : "WRONG";
            } else if (parts[0].equals("game-resource-new") && parts.length == 4) {
                Object[] opaqueIds = {new Handle(0), new Handle(1), new Handle(2)};
                GameResource resource = new GameResource(
                        value(parts, 1), gameResourceId(parts[2], opaqueIds), value(parts, 3));
                result = gameResourceOutput(resource, opaqueIds);
            } else if (parts[0].equals("game-resource-equals") && parts.length == 8) {
                Object[] opaqueIds = {new Handle(0), new Handle(1), new Handle(2)};
                GameResource resource = new GameResource(
                        value(parts, 1), gameResourceId(parts[2], opaqueIds), value(parts, 3));
                Object candidate;
                if (parts[4].equals("null")) {
                    candidate = null;
                } else if (parts[4].equals("other")) {
                    candidate = new Handle(99);
                } else {
                    candidate = new GameResource(value(parts, 5),
                            gameResourceId(parts[6], opaqueIds), value(parts, 7));
                }
                try {
                    result = resource.equals(candidate) ? "1" : "0";
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("game-resource-paint") && parts.length == 12) {
                GameResource resource = new GameResource(1, null, 0);
                Image image = Image.createImage(1, 1);
                resource.image = value(parts, 2) == 0 ? null : image;
                resource.imageWidth = value(parts, 6);
                resource.imageHeight = value(parts, 7);
                resource.imageRegPointX = value(parts, 8);
                resource.imageRegPointY = value(parts, 9);
                int[] transforms = ints(parts[11]);
                if (transforms.length != GameCanvas.transformTable.length) {
                    throw new IllegalArgumentException("paint transform table must have length 8");
                }
                for (int index = 0; index < transforms.length; index++) {
                    GameCanvas.transformTable[index] = transforms[index];
                }
                RecordingGraphics graphics = value(parts, 1) == 0 ? null
                        : new RecordingGraphics(image, value(parts, 10) != 0);
                String status;
                try {
                    resource.paint(graphics, value(parts, 3), value(parts, 4),
                            value(parts, 5));
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    status = "AIOOBE:" + Integer.toString(value(parts, 5)) + ":"
                            + Integer.toString(GameCanvas.transformTable.length);
                }
                result = paintOutput(status, graphics);
            } else if (parts[0].equals("game-resource-paint-simple")
                    && parts.length == 7) {
                GameResource resource = new GameResource(1, null, 0);
                Image image = Image.createImage(1, 1);
                resource.image = value(parts, 2) == 0 ? null : image;
                RecordingGraphics graphics = value(parts, 1) == 0 ? null
                        : new RecordingGraphics(image, value(parts, 6) != 0);
                String status;
                try {
                    resource.paintSimple(graphics, value(parts, 3), value(parts, 4),
                            value(parts, 5));
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = paintSimpleOutput(status, graphics);
            } else if ((parts[0].equals("menu-active") || parts[0].equals("menu-close-all")
                    || parts[0].equals("menu-get-current")) && parts.length == 2) {
                int length = value(parts, 1);
                Vector stack = length < 0 ? null : new Vector();
                if (stack != null) {
                    for (int index = 0; index < length; index++) {
                        stack.addElement(new MenuModel());
                    }
                }
                MenuModel.stack = stack;
                try {
                    if (parts[0].equals("menu-active")) {
                        result = MenuModel.active() ? "1" : "0";
                    } else if (parts[0].equals("menu-close-all")) {
                        MenuModel.closeAll();
                        result = Integer.toString(MenuModel.stack.size());
                    } else {
                        MenuModel current = MenuModel.getCurrent();
                        result = current == null ? "NULL" : Integer.toString(stack.indexOf(current));
                    }
                } catch (NullPointerException exception) {
                    result = "NPE";
                }
            } else if (parts[0].equals("menu-close-current") && parts.length == 3) {
                String flags = parts[2].substring(1);
                Object[] menus = new Object[flags.length()];
                for (int index = 0; index < menus.length; index++) {
                    MenuModel menu = new MenuModel();
                    menuIsCurrent.setBoolean(menu, flags.charAt(index) == '1');
                    menus[index] = menu;
                }
                MenuModel.stack = menuStack(parts[1], menus);
                String status;
                try {
                    MenuModel.closeCurrent();
                    status = "OK";
                } catch (NullPointerException exception) {
                    status = "NPE";
                }
                result = status + ":" + menuStackOutput(MenuModel.stack, menus) + ":"
                        + menuFlagsOutput(menus, menuIsCurrent);
            } else if (parts[0].equals("ink-new") && parts.length == 4) {
                InkScript script = value(parts, 1) == 0 ? null : new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), null);
                RoomObject roomObject = value(parts, 3) == 0 ? null : new RoomObject(null, null);
                InkInterpreter interpreter = new InkInterpreter(script, value(parts, 2), roomObject);
                result = (interpreter.script == null ? "0:"
                        : interpreter.script == script ? "1:" : "WRONG:")
                        + Integer.toString(interpreter.status) + ":"
                        + Integer.toString(interpreter.offset) + ":"
                        + (interpreter.roomObject == null ? "0:"
                                : interpreter.roomObject == roomObject ? "1:" : "WRONG:")
                        + (interpreter.languageDebugMode ? "1" : "0");
            } else if (parts[0].equals("ink-execute") && parts.length == 8) {
                byte[] data = bytes(parts[3]);
                InkScript script = value(parts, 2) == 0 ? null : new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                if (script != null) {
                    script.data = data;
                }
                InkInterpreter interpreter = new InkInterpreter(script, value(parts, 4), null);
                InkInterpreter other = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 5) == 0 ? null
                        : value(parts, 5) == 1 ? interpreter : other;
                Object commandResult = null;
                String outcome;
                try {
                    commandResult = parts[1].equals("execute")
                            ? interpreter.execute(executionValue(parts[6], parts[7]))
                            : interpreter.resume();
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE:" + Integer.toString(interpreter.offset - 1) + ":"
                            + Integer.toString(data.length);
                }
                result = executionOutput(outcome, commandResult, interpreter, other);
            } else if (parts[0].equals("ink-script-resume") && parts.length == 5) {
                byte[] data = bytes(parts[3]);
                InkScript script = value(parts, 2) == 0 ? null : new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                if (script != null) {
                    script.data = data;
                }
                InkInterpreter interpreter = new InkInterpreter(script, value(parts, 4), null);
                InkInterpreter other = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 1) == 0 ? null : interpreter;
                String outcome;
                try {
                    InkScript.resume();
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE:" + Integer.toString(interpreter.offset - 1) + ":"
                            + Integer.toString(data.length);
                }
                result = executionOutput(outcome, null, interpreter, other);
            } else if (parts[0].equals("ink-script-wait") && parts.length == 6) {
                byte[] data = bytes(parts[4]);
                InkScript script = value(parts, 3) == 0 ? null : new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                if (script != null) {
                    script.data = data;
                }
                InkInterpreter interpreter = new InkInterpreter(
                        script, value(parts, 5), null);
                InkInterpreter other = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 2) == 0 ? null : interpreter;
                long initialWaitStop = Long.parseLong(parts[1]);
                InkScript.waitStop = initialWaitStop;
                Object waiting = null;
                String outcome;
                try {
                    waiting = new Integer(InkScript.isWaiting() ? 1 : 0);
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE:" + Integer.toString(interpreter.offset - 1) + ":"
                            + Integer.toString(data.length);
                }
                String waitStop = initialWaitStop == 1L && InkScript.waitStop != 0L
                        ? "T" : Long.toString(InkScript.waitStop);
                result = executionOutput(outcome, waiting, interpreter, other)
                        + ":" + waitStop;
            } else if (parts[0].equals("ink-script-execute") && parts.length == 10) {
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                script.data = bytes(parts[2]);
                script.eventOffsets = ints(parts[3]);
                RoomObject roomObject = value(parts, 7) == 0 ? null : new RoomObject(null, null);
                InkInterpreter oldPaused = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 9) == 0 ? null : oldPaused;
                Object commandResult = null;
                String outcome;
                try {
                    commandResult = parts[1].equals("default")
                            ? script.executeEvent(value(parts, 4),
                                    executionValue(parts[5], parts[6]), roomObject)
                            : script.executeEvent(value(parts, 4),
                                    executionValue(parts[5], parts[6]), roomObject,
                                    value(parts, 8) != 0);
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE";
                }
                result = eventExecutionOutput(outcome, commandResult, script, roomObject, oldPaused);
            } else if (parts[0].equals("ink-script-execute-id") && parts.length == 10) {
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                script.data = bytes(parts[3]);
                script.eventOffsets = ints(parts[4]);
                Hashtable scripts = null;
                if (!parts[1].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[1].equals("-")) {
                        Object stored = parts[1].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[1].substring(1)), stored);
                    }
                }
                InkScript.list = scripts;
                RoomObject roomObject = value(parts, 8) == 0 ? null : new RoomObject(null, null);
                InkInterpreter oldPaused = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 9) == 0 ? null : oldPaused;
                Object commandResult = null;
                String outcome;
                try {
                    commandResult = InkScript.executeEvent(
                            utf16(parts[2]), value(parts, 5),
                            executionValue(parts[6], parts[7]), roomObject);
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ClassCastException exception) {
                    outcome = "CCE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE";
                }
                result = eventExecutionOutput(outcome, commandResult, script, roomObject, oldPaused);
            } else if (parts[0].equals("inventory-equip") && parts.length == 8) {
                int stackLength = value(parts, 1);
                Vector stack = stackLength < 0 ? null : new Vector();
                if (stack != null) {
                    for (int index = 0; index < stackLength; index++) {
                        stack.addElement(new Handle(index));
                    }
                }
                MenuModel.stack = stack;
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                script.data = bytes(parts[4]);
                script.eventOffsets = ints(parts[5]);
                Hashtable scripts = null;
                if (!parts[3].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[3].equals("-")) {
                        Object stored = parts[3].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[3].substring(1)), stored);
                    }
                }
                InkScript.list = scripts;
                InkScript.itemID = utf16(parts[2]);
                InkInterpreter oldPaused = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 7) == 0 ? null : oldPaused;
                String outcome;
                try {
                    InkEngine.inventoryEquipUnequipHandling(value(parts, 6));
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ClassCastException exception) {
                    outcome = "CCE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE";
                }
                String stackOutput = MenuModel.stack == null ? "null"
                        : MenuModel.stack.size() == 0 ? "-"
                        : Integer.toString(MenuModel.stack.size());
                result = eventExecutionOutput(outcome, null, script, null, oldPaused)
                        + ":" + stackOutput;
            } else if (parts[0].equals("room-event") && parts.length == 12) {
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                script.data = bytes(parts[5]);
                script.eventOffsets = ints(parts[6]);
                Hashtable scripts = null;
                if (!parts[4].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[4].equals("-")) {
                        Object stored = parts[4].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[4].substring(1)), stored);
                    }
                }
                InkScript.list = scripts;
                RoomObject roomObject = new RoomObject(null, null);
                roomScriptId.set(roomObject, utf16(parts[3]));
                roomObject.script = value(parts, 2) == 0 ? null : script;
                InkInterpreter oldPaused = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 11) == 0 ? null : oldPaused;
                Object commandResult = null;
                String outcome;
                try {
                    if (parts[1].equals("execute")) {
                        commandResult = roomObject.executeEvent(
                                value(parts, 7), executionValue(parts[8], parts[9]),
                                value(parts, 10) != 0);
                    } else if (parts[1].equals("name")) {
                        commandResult = roomObject.getName();
                    } else if (parts[1].equals("move")) {
                        commandResult = roomObject.getMoveDir();
                    } else {
                        commandResult = roomObject.enterHover();
                    }
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ClassCastException exception) {
                    outcome = "CCE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE";
                }
                Object reportedResult = parts[1].equals("hover") ? null : commandResult;
                result = eventExecutionOutput(outcome, reportedResult, script, roomObject, oldPaused)
                        + (parts[1].equals("hover")
                                ? ":" + (commandResult == null ? "N"
                                        : commandResult == roomObject ? "R" : "WRONG")
                                : "")
                        + ":" + (roomObject.script == null ? "N"
                                : roomObject.script == script ? "S" : "WRONG");
            } else if (parts[0].equals("ink-script-item-name") && parts.length == 6) {
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        new String[]{"ret"});
                script.data = bytes(parts[3]);
                script.eventOffsets = ints(parts[4]);
                Hashtable scripts = null;
                if (!parts[1].equals("null")) {
                    scripts = new Hashtable();
                    if (!parts[1].equals("-")) {
                        Object stored = parts[1].charAt(0) == 's' ? script : new Handle(31);
                        scripts.put(utf16(parts[1].substring(1)), stored);
                    }
                }
                InkScript.list = scripts;
                InkInterpreter oldPaused = new InkInterpreter(null, 0, null);
                InkInterpreter.pausedThread = value(parts, 5) == 0 ? null : oldPaused;
                Object itemName = null;
                String outcome;
                try {
                    itemName = InkScript.getItemName(utf16(parts[2]));
                    outcome = "OK";
                } catch (NullPointerException exception) {
                    outcome = "NPE";
                } catch (ClassCastException exception) {
                    outcome = "CCE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    outcome = "AIOOBE";
                }
                result = eventExecutionOutput(outcome, itemName, script, null, oldPaused);
            } else if ((parts[0].equals("ink-read") && parts.length == 4)
                    || ((parts[0].equals("ink-read-n") || parts[0].equals("ink-read-signed"))
                            && parts.length == 5)) {
                byte[] data = bytes(parts[2]);
                InkScript script = value(parts, 1) == 0 ? null : new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), null);
                if (script != null) {
                    script.data = data;
                }
                InkInterpreter interpreter = new InkInterpreter(script, value(parts, 3), null);
                Method reader = parts[0].equals("ink-read") ? inkInterpreterRead
                        : parts[0].equals("ink-read-n") ? inkInterpreterReadBytes
                        : inkInterpreterReadSigned;
                Object[] readerArguments = parts[0].equals("ink-read")
                        ? new Object[0] : new Object[]{Integer.valueOf(value(parts, 4))};
                try {
                    int readValue = ((Integer) reader.invoke(interpreter, readerArguments)).intValue();
                    result = "OK:" + Integer.toString(readValue) + ":"
                            + Integer.toString(interpreter.offset);
                } catch (InvocationTargetException exception) {
                    if (exception.getCause() instanceof NullPointerException) {
                        result = "NPE:" + Integer.toString(interpreter.offset);
                    } else if (exception.getCause() instanceof ArrayIndexOutOfBoundsException) {
                        result = "AIOOBE:" + Integer.toString(interpreter.offset - 1) + ":"
                                + Integer.toString(data.length) + ":" + Integer.toString(interpreter.offset);
                    } else {
                        throw exception;
                    }
                }
            } else if (parts[0].equals("ink-has-command") && parts.length == 6) {
                byte[] data = bytes(parts[2]);
                InkScript script = value(parts, 1) == 0 ? null : new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        scriptIds(parts[3]));
                if (script != null) {
                    script.data = data;
                }
                InkInterpreter interpreter = new InkInterpreter(script, value(parts, 4), null);
                try {
                    result = "OK:" + (interpreter.hasCommand(value(parts, 5)) ? "1:" : "0:")
                            + Integer.toString(interpreter.offset);
                } catch (NullPointerException exception) {
                    result = "NPE:" + Integer.toString(interpreter.offset);
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE:" + Integer.toString(interpreter.offset);
                }
            } else if (parts[0].equals("ink-script-has-command") && parts.length == 5) {
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})),
                        scriptIds(parts[2]));
                script.data = bytes(parts[1]);
                script.eventOffsets = ints(parts[3]);
                try {
                    result = script.hasCommand(value(parts, 4)) ? "OK:1" : "OK:0";
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE";
                }
            } else if (parts[0].equals("ink-get-string") && parts.length == 3) {
                String[] strings = scriptIds(parts[1]);
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), strings);
                try {
                    result = utf16Output(script.getString(value(parts, 2)));
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE:" + Integer.toString(value(parts, 2) - 1) + ":"
                            + Integer.toString(strings.length);
                }
            } else if (parts[0].equals("ink-has-event") && parts.length == 3) {
                int[] offsets = ints(parts[1]);
                InkScript script = new InkScript(
                        new DataInputStream(new ByteArrayInputStream(new byte[]{0, 0, 0, 0})), null);
                script.eventOffsets = offsets;
                try {
                    result = script.hasEvent(value(parts, 2)) ? "1" : "0";
                } catch (NullPointerException exception) {
                    result = "NPE";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE:" + parts[2] + ":" + Integer.toString(offsets.length);
                }
            } else if (parts[0].equals("ink-stop") && parts.length == 2) {
                InkInterpreter.pausedThread = value(parts, 1) == 0 ? null
                        : new InkInterpreter(null, 0, null);
                InkScript.stop();
                result = InkInterpreter.pausedThread == null ? "0" : "1";
            } else if (parts[0].equals("panel-new") && parts.length == 5) {
                RoomObject object = new RoomObject(null, null);
                object.battlePanelID = value(parts, 1);
                object.battlePanel = ints(parts[2]);
                panelSize.setInt(null, value(parts, 4));
                try {
                    object.battlePanelNew(value(parts, 3));
                    result = "OK:" + object.battlePanelID + ":" + intsOutput(object.battlePanel);
                } catch (NegativeArraySizeException exception) {
                    result = "NAS:" + value(parts, 4) + ":" + object.battlePanelID + ":"
                            + intsOutput(object.battlePanel);
                }
            } else if ((parts[0].equals("panel-max") || parts[0].equals("panel-health")
                    || parts[0].equals("panel-bar") || parts[0].equals("panel-time"))
                    && parts.length == 5) {
                RoomObject object = new RoomObject(null, null);
                object.battlePanelID = value(parts, 1);
                object.battlePanel = ints(parts[2]);
                Field indexField;
                if (parts[0].equals("panel-max")) {
                    indexField = panelMaxHealth;
                } else if (parts[0].equals("panel-health")) {
                    indexField = panelHealth;
                } else if (parts[0].equals("panel-bar")) {
                    indexField = panelBarSize;
                } else {
                    indexField = panelTime;
                }
                indexField.setInt(null, value(parts, 3));
                try {
                    if (parts[0].equals("panel-max")) {
                        object.bpSetMaxHealth(value(parts, 4));
                    } else if (parts[0].equals("panel-health")) {
                        object.bpSetHealth(value(parts, 4));
                    } else if (parts[0].equals("panel-bar")) {
                        object.bpSetBarSize(value(parts, 4));
                    } else {
                        object.bpSetTime(value(parts, 4));
                    }
                    result = "OK:" + object.battlePanelID + ":" + intsOutput(object.battlePanel);
                } catch (NullPointerException exception) {
                    result = "NPE:" + object.battlePanelID + ":null";
                } catch (ArrayIndexOutOfBoundsException exception) {
                    result = "AIOOBE:" + value(parts, 3) + ":" + object.battlePanel.length + ":"
                            + object.battlePanelID + ":" + intsOutput(object.battlePanel);
                }
            } else if (parts[0].equals("room-is-over") && parts.length == 9) {
                RoomObject object = new RoomObject(null, null);
                object.visible = value(parts, 1) != 0;
                object.active = value(parts, 2) != 0;
                object.left = value(parts, 3);
                object.right = value(parts, 4);
                object.top = value(parts, 5);
                object.bottom = value(parts, 6);
                result = object.isOver(value(parts, 7), value(parts, 8)) ? "1" : "0";
            } else {
                throw new IllegalArgumentException("invalid oracle request: " + line);
            }
            System.out.println(result);
        }
    }
}
