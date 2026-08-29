package defpackage;

import java.io.DataInputStream;
import java.util.Hashtable;

/** Named reconstruction of original class {@code Script}. */
class InkScript {
    public byte[] data;
    public int[] eventOffsets;
    private String[] stringList;
    private Object gfxID;
    public static Hashtable list = new Hashtable();
    public static long waitStart;
    public static long waitStop;
    public static MenuModel choiceMenu;
    public static Object choiceID;
    public static String itemID;
    private static final int GFX_TYPE_NONE = 0;
    private static final int GFX_TYPE_STRING = 1;
    private static final int GFX_TYPE_INTEGER = 2;

    InkScript(DataInputStream input, String[] strings) {
        try {
            this.stringList = strings;
            switch (input.readUnsignedByte()) {
                case 1:
                    this.gfxID = getString(input.readUnsignedByte());
                    break;
                case 2:
                    this.gfxID = new Integer(input.readUnsignedShort());
                    break;
            }
            this.eventOffsets = new int[57];
            for (int i = 0; i < this.eventOffsets.length; i++) {
                this.eventOffsets[i] = -1;
            }
            int eventCount = input.readUnsignedByte();
            for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
                this.eventOffsets[input.readUnsignedByte()] = input.readUnsignedShort();
            }
            this.data = new byte[input.readUnsignedShort()];
            input.readFully(this.data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getString(int stringIndex) {
        return this.stringList[stringIndex - 1];
    }

    boolean hasCommand(int commandCode) {
        boolean commandFound = false;
        for (int eventCode = 1; !commandFound && eventCode <= 56; eventCode++) {
            if (this.eventOffsets[eventCode] != -1) {
                commandFound = new InkInterpreter(this, this.eventOffsets[eventCode], null).hasCommand(commandCode);
            }
        }
        return commandFound;
    }

    boolean hasEvent(int eventCode) {
        return this.eventOffsets[eventCode] != -1;
    }

    Object executeEvent(int eventCode, Object initialValue, RoomObject roomObject) {
        return executeEvent(eventCode, initialValue, roomObject, false);
    }

    Object executeEvent(int eventCode, Object initialValue, RoomObject roomObject, boolean languageDebugMode) {
        if (!hasEvent(eventCode)) {
            return initialValue;
        }
        InkInterpreter interpreter = new InkInterpreter(this, this.eventOffsets[eventCode], roomObject);
        interpreter.languageDebugMode = languageDebugMode;
        return interpreter.execute(initialValue);
    }

    static Object executeEvent(String scriptId, int eventCode, Object initialValue, RoomObject roomObject) {
        InkScript script = (InkScript) list.get(scriptId);
        if (script == null) {
            return null;
        }
        return script.executeEvent(eventCode, initialValue, roomObject, false);
    }

    static void setVariable(String variableId, Object value) {
        if (value != null) {
            if (value instanceof String) {
                Application.inkServerSetVariable(variableId, (String) value, Application.charToString('S'));
            } else if (value instanceof Integer) {
                if (((Integer) value).intValue() == 0) {
                    Application.inkServerUnsetVariable(variableId);
                } else {
                    Application.inkServerSetVariable(variableId, value.toString(), new String(Application.charToString('I')));
                }
            }
        }
    }

    static Object getVariable(String variableId) {
        String value = Application.inkServerGetVariable(variableId);
        if (value == null) {
            return null;
        }
        String typeHint = Application.inkServerGetHint(variableId);
        if (typeHint.charAt(0) == 'S') {
            return value;
        }
        if (typeHint.charAt(0) == 'I') {
            return new Integer(Integer.parseInt(value));
        }
        return null;
    }

    static int getVariableAsInteger(String variableId) {
        Object variable = getVariable(variableId);
        if (variable == null) {
            return 0;
        }
        if (variable instanceof Integer) {
            return ((Integer) variable).intValue();
        }
        if (!(variable instanceof String)) {
            return 1;
        }
        try {
            return Integer.parseInt((String) variable);
        } catch (Exception e) {
            return 1;
        }
    }

    static void setInventory(String itemId, int amount) {
        Application.inkServerSetVariable(new StringBuffer().append("inv-").append(itemId).toString(), Integer.toString(amount), Application.charToString('V'));
    }

    static void removeInventory(String itemId) {
        Application.inkServerUnsetVariable(new StringBuffer().append("inv-").append(itemId).toString());
    }

    static int getInventorySize(String itemId) {
        String amount = Application.inkServerGetVariable(new StringBuffer().append("inv-").append(itemId).toString());
        if (amount != null) {
            return Application.toInt(amount);
        }
        return 0;
    }

    static GameResource getInventoryImage(String itemId) {
        InkScript script = (InkScript) list.get(itemId);
        Object imageId = script.executeEvent(InkCodes.EVENT_GETINVENTORYIMAGE, null, null);
        if (imageId == null) {
            imageId = script.gfxID;
        }
        GameResource image = GameResource.getImage(imageId, 0);
        if (image != null && !GameResource.imagesImportants.contains(image)) {
            GameResource.imagesImportants.addElement(image);
        }
        return image;
    }

    static String getItemName(String itemId) {
        return (String) executeEvent(itemId, InkCodes.EVENT_GETNAME, "?", (RoomObject) null);
    }

    static boolean isWaiting() {
        if (waitStop > 0 && System.currentTimeMillis() < waitStop) {
            return true;
        }
        if (waitStop <= 0) {
            return false;
        }
        waitStop = 0L;
        resume();
        return false;
    }

    static void resume() {
        if (InkInterpreter.pausedThread != null) {
            InkInterpreter.pausedThread.resume();
        }
    }

    static void stop() {
        if (InkInterpreter.pausedThread != null) {
            InkInterpreter.pausedThread = null;
        }
    }
}
