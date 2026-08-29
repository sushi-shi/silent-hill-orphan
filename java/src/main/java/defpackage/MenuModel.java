package defpackage;

import java.util.Vector;

/** Named reconstruction of original class {@code Menu}. */
class MenuModel {
    public int ID;
    private boolean isCurrent;
    int maxTextWidth;
    String topText;
    Vector choiceIDs;
    Vector choiceTexts;
    int selectedChoiceNr;
    int x;
    int y;
    public static Vector stack = new Vector();
    GameResource curInvItemResource;
    int delimiterHeight;
    boolean drawDelimiter_1;
    int delimiterY_1;
    boolean drawDelimiter_2;
    int delimiterY_2;
    int totalWidth;
    int totalHeight;
    int screenX;
    int screenY;
    int textX;
    int textY;
    int textWidth;
    int textHeight;
    int choicesY;
    int choicesHeight;
    Vector choiceLines;
    Vector choiceLineBackgroundColors;
    int imageSectionY;
    int scroll;
    boolean textScrolling;
    boolean checkChoiceMenuHeight = true;
    String[] topLines = null;
    boolean updateTopLines = true;
    String[][] bodyLines = (String[][]) null;
    boolean updateBodyLines = true;
    int lowerPaintChoice = 0;
    int ingameShowRowByRow_curY = -1;
    boolean ingameShowRowByRow_use = false;
    String engineSoftkeyOptionLeft = null;
    String engineSoftkeyOptionRight = null;
    public int ingameDelimiterNumOf = -1;
    boolean updateMenu = true;
    int engineNumOfLinesShownMax = -1;
    int engineScrollBarMarkerHeight = -1;
    boolean engineFullScreenScroll = false;
    boolean textInputMenu = false;

    MenuModel() {
    }

    public void addChoice(Object choiceId, String choiceText) {
        this.choiceIDs.addElement(choiceId);
        this.choiceTexts.addElement(choiceText);
        this.updateBodyLines = true;
        this.updateMenu = true;
    }

    public void addChoice(int choiceId, String choiceText) {
        addChoice(new Integer(choiceId), choiceText);
    }

    public int countChoices() {
        return this.choiceIDs.size();
    }

    public void nextChoice() {
        int nextChoiceIndex = this.selectedChoiceNr + 1;
        this.selectedChoiceNr = nextChoiceIndex;
        if (nextChoiceIndex >= countChoices()) {
            this.selectedChoiceNr = 0;
            this.scroll = this.scroll < 0 ? 0 : this.scroll;
        }
        this.updateMenu = true;
    }

    public void previousChoice() {
        int previousChoiceIndex = this.selectedChoiceNr - 1;
        this.selectedChoiceNr = previousChoiceIndex;
        if (previousChoiceIndex < 0) {
            this.selectedChoiceNr = countChoices() - 1;
        }
        this.scroll += this.scroll < 0 ? 1 : 0;
        this.updateMenu = true;
    }

    public void scrollIncrease() {
        this.scroll += this.scroll < 0 ? 1 : 0;
        this.textScrolling = true;
        this.updateMenu = true;
    }

    public void scrollDecrease() {
        this.scroll -= this.textScrolling ? 1 : 0;
        this.updateMenu = true;
    }

    public void setInvItemResource(GameResource resource) {
        this.curInvItemResource = resource;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setTop(String text) {
        this.topText = text;
        this.updateTopLines = true;
        this.updateMenu = true;
    }

    public int getChoiceNr() {
        return this.selectedChoiceNr;
    }

    public Object getChoiceID() {
        return this.choiceIDs.elementAt(this.selectedChoiceNr);
    }

    public void setCurrent(boolean current) {
        this.isCurrent = current;
    }

    static void closeCurrent() {
        if (active()) {
            stack.removeElement(getCurrent());
            if (active()) {
                getCurrent().setCurrent(true);
            }
        }
    }

    static void closeAll() {
        stack = new Vector();
    }

    static MenuModel getCurrent() {
        if (stack.size() == 0) {
            return null;
        }
        return (MenuModel) stack.lastElement();
    }

    static boolean active() {
        return stack.size() > 0;
    }

    public void setSoftkeyOptions(String leftOption, String rightOption) {
        this.engineSoftkeyOptionLeft = leftOption;
        this.engineSoftkeyOptionRight = rightOption;
    }
}
