package defpackage;

/** Named reconstruction of original class {@code Cheat}. */
public class CheatController {
    public static final int CHOICE_BEN = 0;
    public static final int CHOICE_MOON = 1;
    public static final int CHOICE_KAREN = 2;
    public static final int INIT_VALUE = -123;
    private static int lastKey = INIT_VALUE;

    public static void cheatNoMenuKeyHandling() {
        if (Application.keyNew) {
            switch (Application.keyDown) {
                case 48:
                    if (lastKey == 57) {
                        MenuModel cheatMenu = SilentHillGame.menuCreate(100, Application.canvasCenterX);
                        cheatMenu.setPosition(Application.canvasWidth, Application.canvasHeight);
                        cheatMenu.addChoice(0, Application.getString(TextId.JAVA_APP_INK_BEN));
                        cheatMenu.addChoice(1, Application.getString(TextId.JAVA_APP_INK_MOON));
                        cheatMenu.addChoice(2, Application.getString(TextId.JAVA_APP_INK_KAREN));
                    }
                    lastKey = INIT_VALUE;
                    break;
                case 55:
                    if (lastKey != -123) {
                        lastKey = INIT_VALUE;
                    } else {
                        lastKey = Application.keyDown;
                    }
                    break;
                case 57:
                    if (lastKey != 55) {
                        lastKey = INIT_VALUE;
                    } else {
                        lastKey = Application.keyDown;
                    }
                    break;
                default:
                    lastKey = INIT_VALUE;
                    break;
            }
        }
    }

    public static void cheatMenuKeyHandling() {
        if (MenuModel.getCurrent().ID == 100 && Application.keyNew) {
            switch (Application.keyDown) {
                case -11:
                case -8:
                case -7:
                    MenuModel.closeCurrent();
                    break;
                case -6:
                case -5:
                    int characterChoice = Application.toInt(MenuModel.getCurrent().getChoiceID());
                    Application.roomUpdateNeeded = false;
                    Application.roomRepaintNeeded = false;
                    SilentHillGame.softkeyPainting = false;
                    Application.painting = false;
                    Application.repaintCanvasIfPossible();
                    switch (characterChoice) {
                        case 0:
                            Application.resetVariableSystem();
                            InkEngine.roomInit("intro", false);
                            MenuModel.closeCurrent();
                            break;
                        case 1:
                            InkEngine.roomInit("moon_intro", false);
                            MenuModel.closeCurrent();
                            break;
                        case 2:
                            InkEngine.roomInit("karen_intro", false);
                            MenuModel.closeCurrent();
                            break;
                    }
                    SilentHillGame.softkeyPainting = true;
                    break;
                case -2:
                    MenuModel.getCurrent().nextChoice();
                    break;
                case -1:
                    MenuModel.getCurrent().previousChoice();
                    break;
            }
        }
    }
}
