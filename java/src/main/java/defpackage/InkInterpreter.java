package defpackage;

/** Named reconstruction of original class {@code ScriptThread}. */
class InkInterpreter {
    static final int STATUS_CREATED = 0;
    static final int STATUS_RUNNING = 1;
    static final int STATUS_STOPPED = 2;
    static final int STATUS_RETURNED = 3;
    static final int STATUS_PAUSED = 4;
    static final int STATUS_WAITING = 5;
    public InkScript script;
    public int status = 0;
    public int offset;
    public RoomObject roomObject;
    public static InkInterpreter pausedThread;
    public boolean languageDebugMode;

    InkInterpreter(InkScript script, int eventOffset, RoomObject roomObject) {
        this.script = script;
        this.offset = eventOffset;
        this.roomObject = roomObject;
    }

    public Object execute(Object initialValue) {
        Object result;
        this.status = 1;
        Object commandResult = initialValue;
        while (true) {
            result = commandResult;
            if (this.status != 1) {
                break;
            }
            commandResult = executeCommand();
        }
        if (this.status == 4 || this.status == 5) {
            pausedThread = this;
        }
        return result;
    }

    public Object resume() {
        pausedThread = null;
        return execute(null);
    }

    private int readSigned(int byteCount) {
        int value = read(byteCount);
        int signBit = 1 << ((byteCount << 3) - 1);
        if ((value & signBit) != 0) {
            value -= signBit + signBit;
        }
        return value;
    }

    private int read(int byteCount) {
        int value = 0;
        for (int byteIndex = 0; byteIndex < byteCount; byteIndex++) {
            value = (value << 8) + read();
        }
        return value;
    }

    private int read() {
        byte[] scriptData = this.script.data;
        int currentOffset = this.offset;
        this.offset = currentOffset + 1;
        return scriptData[currentOffset] & 255;
    }

    public static int integerArgument(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }
        if (!(value instanceof String)) {
            return 1;
        }
        try {
            return Integer.parseInt((String) value);
        } catch (Exception e) {
            return 1;
        }
    }

    boolean hasCommand(int targetCommand) {
        int commandByte = read();
        int command = 63 & commandByte;
        if (command == targetCommand) {
            return true;
        }
        int argumentCount = commandByte >> 6;
        if (argumentCount == 3) {
            argumentCount = read();
        }
        boolean commandFound = false;
        if (command == InkCodes.COMMAND_INTEGER) {
            if (argumentCount == 0) {
                readSigned(1);
                return false;
            }
            if (argumentCount == 1) {
                readSigned(2);
                return false;
            }
            readSigned(4);
            return false;
        }
        if (command == InkCodes.COMMAND_STRING) {
            this.script.getString(read());
            return false;
        }
        if (command == InkCodes.COMMAND_END) {
            return false;
        }
        for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
            commandFound = hasCommand(targetCommand);
            if (commandFound) {
                return true;
            }
        }
        return commandFound;
    }

    Object executeCommand() {
        int updatedVariableValue;
        int commandByte = read();
        int command = 63 & commandByte;
        int argumentCount = commandByte >> 6;
        if (argumentCount == 3) {
            argumentCount = read();
        }
        Object commandResult = null;
        if (command == InkCodes.COMMAND_INTEGER) {
            return new Integer(argumentCount == 0 ? readSigned(1) : argumentCount == 1 ? readSigned(2) : readSigned(4));
        }
        if (command == InkCodes.COMMAND_STRING) {
            return this.script.getString(read());
        }
        if (command != InkCodes.COMMAND_END) {
            Object[] arguments = new Object[argumentCount];
            for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
                arguments[argumentIndex] = executeCommand();
            }
            commandResult = null;
            switch (command) {
                case InkCodes.COMMAND_RETURN:
                    this.status = 3;
                    commandResult = arguments[0];
                    break;
                case InkCodes.COMMAND_END:
                case InkCodes.COMMAND_INTEGER:
                case InkCodes.COMMAND_STRING:
                case InkCodes.COMMAND_SETVAR:
                case InkCodes.COMMAND_INVENTORYADD:
                case InkCodes.COMMAND_GOTO:
                case InkCodes.COMMAND_ADDCHOICE:
                case InkCodes.COMMAND_BATTLESTART:
                case InkCodes.COMMAND_BATTLEMODE:
                case InkCodes.COMMAND_GETMOVES:
                case InkCodes.COMMAND_SETMOVES:
                case InkCodes.COMMAND_HASMOVES:
                case InkCodes.COMMAND_USEMOVES:
                case InkCodes.COMMAND_DAMAGE:
                case InkCodes.COMMAND_BATTLESTOP:
                case InkCodes.COMMAND_STARTDISSOLVE:
                case InkCodes.COMMAND_TEXTINPUT:
                case InkCodes.COMMAND_LOADCHUNK:
                default:
                    commandResult = InkEngine.executeCommand(this, command, arguments);
                    break;
                case InkCodes.COMMAND_JUMP:
                    this.offset += integerArgument(arguments[0]);
                    break;
                case InkCodes.COMMAND_JUMPIFFALSE:
                    if (integerArgument(arguments[0]) == 0) {
                        this.offset += integerArgument(arguments[1]);
                    }
                    break;
                case InkCodes.COMMAND_LIST:
                    commandResult = arguments;
                    break;
                case InkCodes.COMMAND_NOT:
                    commandResult = new Integer(integerArgument(arguments[0]) == 0 ? 1 : 0);
                    break;
                case InkCodes.COMMAND_GETVAR:
                    commandResult = Application.inkServerGetVariable((String) arguments[0]);
                    if (commandResult == null) {
                        commandResult = "0";
                    }
                    break;
                case InkCodes.COMMAND_INCREASE:
                case InkCodes.COMMAND_DECREASE:
                    int variableAsInteger = InkScript.getVariableAsInteger((String) arguments[0]);
                    int delta = argumentCount < 2 ? 1 : integerArgument(arguments[1]);
                    if (command == InkCodes.COMMAND_INCREASE) {
                        updatedVariableValue = variableAsInteger + delta;
                        if (argumentCount == 3) {
                            updatedVariableValue = Application.min(updatedVariableValue, integerArgument(arguments[2]));
                        }
                    } else {
                        updatedVariableValue = variableAsInteger - delta;
                        if (argumentCount == 3) {
                            updatedVariableValue = Application.max(updatedVariableValue, integerArgument(arguments[2]));
                        }
                    }
                    InkScript.setVariable((String) arguments[0], new Integer(updatedVariableValue));
                    if (!InkEngine.battleMode) {
                        Application.roomUpdateNeeded = true;
                    }
                    break;
                case InkCodes.COMMAND_SAY:
                case InkCodes.COMMAND_STORYSCREEN:
                    SilentHillGame.say((String) arguments[0], argumentCount == 2 ? (String) arguments[1] : null, false);
                    if (pausedThread == null || pausedThread.script == this.script) {
                        this.status = 4;
                    }
                    break;
                case InkCodes.COMMAND_ADD:
                    int sum = 0;
                    for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
                        sum += integerArgument(arguments[argumentIndex]);
                    }
                    commandResult = new Integer(sum);
                    break;
                case InkCodes.COMMAND_EQUALS:
                    int allEqual = 1;
                    if (arguments[0] instanceof String) {
                        for (int argumentIndex = 1; argumentIndex < argumentCount && allEqual != 0; argumentIndex++) {
                            if (arguments[argumentIndex] instanceof Integer) {
                                if (!arguments[0].equals(((Integer) arguments[argumentIndex]).toString())) {
                                    allEqual = 0;
                                }
                            } else if (!arguments[0].equals(arguments[argumentIndex])) {
                                allEqual = 0;
                            }
                        }
                    } else {
                        for (int argumentIndex = 1; argumentIndex < argumentCount && allEqual != 0; argumentIndex++) {
                            if (arguments[argumentIndex] instanceof Integer) {
                                if (((Integer) arguments[0]).intValue() != ((Integer) arguments[argumentIndex]).intValue()) {
                                    allEqual = 0;
                                }
                            } else if (!((Integer) arguments[0]).toString().equals(arguments[argumentIndex])) {
                                allEqual = 0;
                            }
                        }
                    }
                    commandResult = new Integer(allEqual);
                    break;
                case InkCodes.COMMAND_BOTH:
                    int allTrue = 1;
                    for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
                        if (integerArgument(arguments[argumentIndex]) == 0) {
                            allTrue = 0;
                        }
                    }
                    commandResult = new Integer(allTrue);
                    break;
                case InkCodes.COMMAND_EITHER:
                    int anyTrue = 0;
                    for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
                        if (integerArgument(arguments[argumentIndex]) != 0) {
                            anyTrue = 1;
                        }
                    }
                    commandResult = new Integer(anyTrue);
                    break;
                case InkCodes.COMMAND_USEDITEM:
                    commandResult = InkScript.itemID;
                    break;
                case InkCodes.COMMAND_INVENTORYHAS:
                    commandResult = new Integer(InkScript.getInventorySize((String) arguments[0]) == 0 ? 0 : 1);
                    break;
                case InkCodes.COMMAND_INVENTORYHASNOT:
                    commandResult = new Integer(InkScript.getInventorySize((String) arguments[0]) == 0 ? 1 : 0);
                    break;
                case InkCodes.COMMAND_PLAYSOUND:
                    if (Application.curSoundMode) {
                        int repeatCount = argumentCount > 1 ? integerArgument(arguments[1]) : 1;
                        GameCanvas.stopSound();
                        GameCanvas.playSound((String) arguments[0], repeatCount);
                    }
                    break;
                case InkCodes.COMMAND_CHOOSE:
                    InkScript.choiceMenu = null;
                    this.status = 4;
                    break;
                case InkCodes.COMMAND_CHOICE:
                    commandResult = InkScript.choiceID;
                    break;
                case InkCodes.COMMAND_CHOICEIS:
                    commandResult = new Integer(InkScript.choiceID.equals(arguments[0]) ? 1 : 0);
                    break;
                case InkCodes.COMMAND_RANDOM:
                    commandResult = new Integer(Application.random(argumentCount == 0 ? 2 : integerArgument(arguments[0])));
                    break;
                case InkCodes.COMMAND_WAIT:
                    Application.firstLoopInWait = true;
                    InkScript.waitStart = System.currentTimeMillis();
                    InkScript.waitStop = InkScript.waitStart + ((long) integerArgument(arguments[0]));
                    this.status = 5;
                    break;
                case InkCodes.COMMAND_ENDGAME:
                    if (MenuModel.active()) {
                        MenuModel.closeAll();
                    }
                    Application.endGame = true;
                    break;
                case InkCodes.COMMAND_GREATERTHAN:
                    commandResult = new Integer(integerArgument(arguments[0]) > integerArgument(arguments[1]) ? 1 : 0);
                    break;
                case InkCodes.COMMAND_SMALLERTHAN:
                    commandResult = new Integer(integerArgument(arguments[0]) < integerArgument(arguments[1]) ? 1 : 0);
                    break;
                case InkCodes.COMMAND_INVENTORYREMOVE:
                    int inventorySize = InkScript.getInventorySize((String) arguments[0]);
                    int remainingAmount = argumentCount > 1 ? inventorySize - integerArgument(arguments[1]) : inventorySize - 1;
                    if (remainingAmount <= 0) {
                        InkScript.removeInventory((String) arguments[0]);
                    } else {
                        InkScript.setInventory((String) arguments[0], remainingAmount);
                    }
                    break;
                case InkCodes.COMMAND_INVENTORYITEMAMOUNT:
                    commandResult = new Integer(InkScript.getInventorySize((String) arguments[0]));
                    break;
                case InkCodes.COMMAND_SUBTRACT:
                    int difference = integerArgument(arguments[0]);
                    for (int argumentIndex = 1; argumentIndex < argumentCount; argumentIndex++) {
                        difference -= integerArgument(arguments[argumentIndex]);
                    }
                    commandResult = new Integer(difference);
                    break;
                case InkCodes.COMMAND_SAVEGAME:
                    Application.saveGame(argumentCount == 1 ? Application.toBoolean(arguments[0]) : true);
                    break;
                case InkCodes.COMMAND_STOPSOUND:
                    if (Application.curSoundMode) {
                        GameCanvas.stopSound();
                    }
                    break;
            }
        } else {
            this.status = 2;
        }
        return commandResult;
    }
}
