package defpackage;

import java.io.DataInputStream;
import javax.microedition.lcdui.Graphics;

/** Named reconstruction of original class {@code RoomObject}. */
class RoomObject {
    public static final int TYPE_GFX = 1;
    public static final int TYPE_ZONE = 2;
    public static final int TYPE_ZONE_BATTLE = 3;
    public static final int TYPE_ZONE_COLOR = 4;
    public static final int TYPE_ZONE_TEXT = 5;
    public static final int TYPE_ZONE_TILES = 6;
    public static final int GFX_ID_TYPE_STRING = 1;
    public static final int GFX_ID_TYPE_INTEGER = 2;
    public static final int ANIMATION_IDLE = 0;
    public static final int ANIMATION_ENEMYATTACK1 = 1;
    public static final int ANIMATION_ENEMYATTACK2 = 2;
    public static final int ANIMATION_ENEMYATTACK3 = 3;
    public static final int ANIMATION_HEROATTACK1 = 4;
    public static final int ANIMATION_HEROATTACK2 = 5;
    public static final int ANIMATION_SINGLE = 6;
    public static final int ANIMATION_COUNT = 7;
    public static final int ANIMATION_VALUES_PER_PART = 4;
    public static final int ANIMATION_OFFSET_GFX = 0;
    public static final int ANIMATION_OFFSET_DURATION = 1;
    public static final int ANIMATION_OFFSET_OFFSET_X = 2;
    public static final int ANIMATION_OFFSET_OFFSET_Y = 3;
    public int type;
    public int x;
    public int y;
    public int width;
    public int height;
    public int regPointX;
    public int regPointY;
    public int left;
    public int right;
    public int top;
    public int bottom;
    private int transform;
    private Object gfxID;
    private String scriptID;
    public InkScript script;
    public boolean visible;
    public boolean active;
    private static final int DEFAULT_COLOR = 16777215;
    private int textAlignment;
    private Object[][] animationData;
    private int[] animationParts;
    private int[] animationDuration;
    private int[][] animationImagePoints;
    public long animationTime;
    public long idleAnimationTime;
    public int runAnimLoops;
    public int battlePanelID;
    public int[] battlePanel;
    private static final int ZONE_TEXT_DEFAULT_WIDTH = 30;
    public static long paintingAnimationTime = -1;
    public static boolean noVibraYet = true;
    public static int BATTLE_PANEL_ID_HERO_HEALTH = 1;
    public static int BATTLE_PANEL_ID_ENEMY_HEALTH = 2;
    public static int BATTLE_PANEL_ID_TIMEBAR = 3;
    public static int BATTLE_PANEL_ID_HARD_ATTACK = 4;
    public static int BATTLE_PANEL_ID_FAST_ATTACK = 5;
    public static int BATTLE_PANEL_ID_INVENTORY = 6;
    public static int BATTLE_PANEL_ID_ESCAPE = 7;
    private static int BATTLE_PANEL_MAX_HEALTH = 0;
    private static int BATTLE_PANEL_HEALTH = 1;
    private static int BATTLE_PANEL_BAR_SIZE = 2;
    private static int BATTLE_PANEL_TIME = 3;
    private static int BATTLE_PANEL_SIZE = 4;
    private int color = DEFAULT_COLOR;
    private String text = null;
    private int runAnimPausedTime = -1;

    RoomObject(DataInputStream input, String[] strings) {
        try {
            this.idleAnimationTime = 0L;
            this.visible = true;
            this.active = false;
            this.type = input.readUnsignedByte();
            switch (this.type) {
                case 1:
                    this.x = input.readShort();
                    this.y = input.readShort();
                    this.transform = input.readUnsignedByte();
                    switch (input.readUnsignedByte()) {
                        case 1:
                            this.gfxID = strings[input.readUnsignedByte() - 1];
                            break;
                        case 2:
                            this.gfxID = new Integer(input.readUnsignedShort());
                            break;
                    }
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    this.x = input.readShort();
                    this.y = input.readShort();
                    this.width = input.readUnsignedByte();
                    this.height = input.readUnsignedByte();
                    break;
            }
            int scriptStringIndex = input.readUnsignedByte();
            if (scriptStringIndex != 0) {
                this.scriptID = strings[scriptStringIndex - 1];
            }
        } catch (Exception e) {
        }
    }

    public void battlePanelNew(int panelId) {
        this.battlePanelID = panelId;
        this.battlePanel = new int[BATTLE_PANEL_SIZE];
    }

    public void bpSetMaxHealth(int maxHealth) {
        this.battlePanel[BATTLE_PANEL_MAX_HEALTH] = maxHealth;
    }

    public void bpSetHealth(int health) {
        this.battlePanel[BATTLE_PANEL_HEALTH] = health;
    }

    public void bpSetBarSize(int barSize) {
        this.battlePanel[BATTLE_PANEL_BAR_SIZE] = barSize;
    }

    public void bpSetTime(int time) {
        this.battlePanel[BATTLE_PANEL_TIME] = time;
    }

    public Object executeEvent(int eventCode, Object initialValue, boolean languageDebugMode) {
        if (this.script == null) {
            if (this.scriptID == null) {
                return initialValue;
            }
            this.script = (InkScript) InkScript.list.get(this.scriptID);
            if (this.script == null) {
            }
        }
        return this.script.executeEvent(eventCode, initialValue, this, languageDebugMode);
    }

    public boolean update() {
        String newText;
        if (this.scriptID == null) {
            getBounds();
            return false;
        }
        Application.resourceGet(Application.loadRequest_getResourcePath(1, this.scriptID));
        this.active = Application.toBoolean(executeEvent(InkCodes.EVENT_ISACTIVE, new Integer(1), false));
        boolean newVisibility = Application.toBoolean(executeEvent(InkCodes.EVENT_ISVISIBLE, new Integer(1), false));
        boolean repaintNeeded = (this.type == 1 || this.type == 4 || this.type == 5 || this.type == 6) && this.visible != newVisibility;
        if (this.type == 3) {
            if (InkEngine.battleMode && this.gfxID == null) {
                int panelId = Application.toInt(executeEvent(InkCodes.EVENT_GETMETERID, new Integer(-1), false));
                if (panelId >= BATTLE_PANEL_ID_HARD_ATTACK && panelId <= BATTLE_PANEL_ID_ESCAPE) {
                    this.x = 0;
                    if (Application.canvasHeight > 160) {
                        this.y = Application.canvasCenterY + (this.y - (Application.roomHeight >> 1));
                    } else {
                        this.y = Application.canvasCenterY + (((this.y - this.height) - this.height) - (Application.roomHeight >> 1));
                    }
                }
                Object newGfxId = executeEvent(InkCodes.EVENT_GETIMAGE, null, false);
                if (newGfxId != null) {
                    if (this.gfxID == null) {
                        this.gfxID = newGfxId;
                    } else if (!this.gfxID.equals(newGfxId)) {
                        this.right = 0;
                        getBounds();
                    }
                }
            } else if (!InkEngine.battleMode) {
                if (this.gfxID != null) {
                    repaintNeeded = true;
                }
            }
            newVisibility = false;
        }
        this.visible = newVisibility;
        if (this.type == 1) {
            Object newGfxId = executeEvent(InkCodes.EVENT_GETIMAGE, null, false);
            if (newGfxId != null) {
                if (this.gfxID == null) {
                    this.gfxID = newGfxId;
                    repaintNeeded = true;
                } else if (!this.gfxID.equals(newGfxId)) {
                    this.gfxID = newGfxId;
                    repaintNeeded = true;
                    this.right = 0;
                    getBounds();
                }
            }
            if (!Application.roomRepaintNeeded && !repaintNeeded && Application.toBoolean(executeEvent(InkCodes.EVENT_ISMETER, new Integer(0), false))) {
                repaintNeeded = true;
            }
        } else if (this.type == 4 || this.type == 5) {
            int newColor = DEFAULT_COLOR;
            Object colorValue = executeEvent(InkCodes.EVENT_GETCOLOR, null, false);
            if (colorValue != null) {
                newColor = Application.toInt(colorValue);
            }
            if (this.color != newColor) {
                this.color = newColor;
                repaintNeeded = true;
            }
            if (this.type == 5) {
                Object textValue = executeEvent(InkCodes.EVENT_GETTEXT, null, false);
                if (textValue == null) {
                    newText = null;
                } else if (textValue instanceof String) {
                    newText = (String) textValue;
                } else {
                    newText = textValue instanceof Integer ? ((Integer) textValue).toString() : null;
                }
                boolean textChanged = false;
                if (this.text == null) {
                    if (newText != null) {
                        textChanged = true;
                    }
                } else if (!this.text.equals(newText)) {
                    textChanged = true;
                }
                if (textChanged) {
                    this.text = newText;
                    repaintNeeded = true;
                }
                Object alignmentValue = executeEvent(InkCodes.EVENT_GETTEXTALIGNMENT, null, false);
                int newTextAlignment = alignmentValue != null ? Application.toInt(alignmentValue) : 0;
                if (this.textAlignment != newTextAlignment) {
                    this.textAlignment = newTextAlignment;
                    repaintNeeded = true;
                }
            }
        }
        Object[][] oldAnimationData = this.animationData;
        int[][] oldAnimationImagePoints = this.animationImagePoints;
        this.animationData = new Object[7][];
        this.animationParts = new int[7];
        this.animationDuration = new int[7];
        this.animationImagePoints = new int[7][];
        int[] animationEventCodes = {
            InkCodes.EVENT_GETIDLEANIMATION,
            InkCodes.EVENT_GETENEMYATTACKANIM1,
            InkCodes.EVENT_GETENEMYATTACKANIM2,
            InkCodes.EVENT_GETENEMYATTACKANIM3,
            InkCodes.EVENT_GETHEROATTACKANIM1,
            InkCodes.EVENT_GETHEROATTACKANIM2,
            InkCodes.EVENT_GETRUNANIMATION
        };
        for (int animationIndex = 0; animationIndex < 7; animationIndex++) {
            Object[] oldAnimation = oldAnimationData == null ? null : oldAnimationData[animationIndex];
            Object animationValue = executeEvent(animationEventCodes[animationIndex], null, false);
            if (animationValue != null) {
                Object[] newAnimation = (Object[]) animationValue;
                this.animationData[animationIndex] = newAnimation;
                int frameCount = newAnimation.length / 4;
                this.animationParts[animationIndex] = frameCount;
                int totalDuration = 0;
                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    totalDuration += Math.abs(Application.toInt(newAnimation[(frameIndex * 4) + 1]));
                }
                this.animationDuration[animationIndex] = totalDuration;
                if (InkEngine.regPointSystemActive) {
                    this.animationImagePoints[animationIndex] = new int[frameCount << 1];
                } else {
                    this.animationImagePoints = (int[][]) null;
                    oldAnimationImagePoints = (int[][]) null;
                }
                boolean animationChanged = false;
                if (oldAnimation != null && oldAnimation.length == newAnimation.length) {
                    for (int valueIndex = 0; valueIndex < newAnimation.length && !animationChanged; valueIndex++) {
                        animationChanged = !newAnimation[valueIndex].equals(oldAnimation[valueIndex]);
                    }
                    repaintNeeded = repaintNeeded || animationChanged;
                } else {
                    repaintNeeded = true;
                }
                if (this.animationImagePoints != null) {
                    if (!animationChanged || oldAnimationImagePoints == null) {
                        int imagePointOffset = 0;
                        int dataOffset = 0;
                        while (dataOffset < newAnimation.length) {
                            if (!newAnimation[dataOffset].equals("") && ((dataOffset > 0 && !newAnimation[dataOffset].equals(newAnimation[dataOffset - 4])) || dataOffset == 0)) {
                                GameResource frameImage = GameResource.getImage(newAnimation[dataOffset], this.transform);
                                this.animationImagePoints[animationIndex][imagePointOffset] = (this.x + this.regPointX) - frameImage.imageRegPointX;
                                this.animationImagePoints[animationIndex][imagePointOffset + 1] = (this.y - this.regPointY) + frameImage.imageRegPointY;
                            } else if (newAnimation[dataOffset].equals("")) {
                                this.animationImagePoints[animationIndex][imagePointOffset] = 0;
                                this.animationImagePoints[animationIndex][imagePointOffset + 1] = 0;
                            } else {
                                this.animationImagePoints[animationIndex][imagePointOffset] = this.animationImagePoints[animationIndex][imagePointOffset - 2];
                                this.animationImagePoints[animationIndex][imagePointOffset + 1] = this.animationImagePoints[animationIndex][imagePointOffset - 1];
                            }
                            dataOffset += 4;
                            imagePointOffset += 2;
                        }
                    } else {
                        this.animationImagePoints[animationIndex] = oldAnimationImagePoints[animationIndex];
                    }
                }
            } else {
                repaintNeeded = repaintNeeded || oldAnimation != null;
            }
        }
        if (this.visible) {
            getBounds();
        }
        if (!Application.roomEntered) {
            executeEvent(InkCodes.EVENT_ENTERROOM, null, false);
            repaintNeeded = true;
            SilentHillGame.enemySoundPlayed = false;
        }
        if (InkEngine.roomObjectTick == null && this.script.hasEvent(InkCodes.EVENT_TICK)) {
            InkEngine.roomObjectTick = this;
            InkEngine.tickTimerUpdateInterval = InkEngine.EVENT_TICK_UPDATE_TIME;
        }
        if (Application.setupDone) {
            if (this.scriptID.equals("setupScript")) {
                if (Application.roomGetCurrent().length() > 0) {
                    InkEngine.roomInit(Application.roomGetCurrent(), false);
                }
                Application.gameChangedSinceLastSave = false;
            }
        } else if (this.scriptID.equals("setupScript")) {
            Application.gameChangedSinceLastSave = false;
            InkEngine.roomPaint();
            InkEngine.drawBigScreenAddOn();
        } else {
            Application.setupDone = true;
        }
        return repaintNeeded;
    }

    public void getBounds() {
        if (this.right > 0) {
            return;
        }
        if (this.type == 1) {
            GameResource.getImageInfo(this, this.gfxID, this.transform);
            this.left = Application.getLeft(this.x, this.width, this.height, this.regPointX, this.regPointY, this.transform);
            this.top = Application.getTop(this.y, this.width, this.height, this.regPointX, this.regPointY, this.transform);
            if ((this.transform & 2) == 0) {
                this.right = this.left + this.width;
                this.bottom = this.top + this.height;
                return;
            } else {
                this.right = this.left + this.height;
                this.bottom = this.top + this.width;
                return;
            }
        }
        if (this.type == 6) {
            this.left = 0;
            this.top = 0;
            this.right = 255;
            this.bottom = 255;
            return;
        }
        this.left = this.x;
        this.top = this.y;
        this.right = this.x + this.width;
        this.bottom = this.y + this.height;
    }

    public void paint(Graphics graphics, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        int textAnchor;
        if (this.type != 2 && this.visible) {
            if (this.scriptID != null) {
                for (int i6 = 0; i6 < 7; i6++) {
                    if (this.animationData[i6] != null && (this.runAnimLoops > 0 || i6 == 0)) {
                        return;
                    }
                }
            }
            if (this.right >= viewportX && this.bottom >= viewportY && this.left <= viewportX + viewportWidth && this.top <= viewportY + viewportHeight) {
                if (this.type == 4) {
                    graphics.setColor(this.color);
                    graphics.fillRect(this.x - viewportX, this.y - viewportY, this.width, this.height);
                    return;
                }
                if (this.type == 6) {
                    Object imageIdsValue = executeEvent(InkCodes.EVENT_GETTILEDATAGFXID, null, false);
                    Object transformsValue = executeEvent(InkCodes.EVENT_GETTILEDATAGFXTRANSFORMATION, null, false);
                    Object visibilitiesValue = executeEvent(InkCodes.EVENT_GETTILEDATABOOLEAN, null, false);
                    Object xCoordinatesValue = executeEvent(InkCodes.EVENT_GETTILEDATAPOSX, null, false);
                    Object yCoordinatesValue = executeEvent(InkCodes.EVENT_GETTILEDATAPOSY, null, false);
                    if (imageIdsValue == null || transformsValue == null || visibilitiesValue == null || xCoordinatesValue == null || yCoordinatesValue == null) {
                        return;
                    }
                    Object[] imageIds = (Object[]) imageIdsValue;
                    Object[] transforms = (Object[]) transformsValue;
                    Object[] visibilities = (Object[]) visibilitiesValue;
                    Object[] xCoordinates = (Object[]) xCoordinatesValue;
                    Object[] yCoordinates = (Object[]) yCoordinatesValue;
                    int tileCount = Application.min(Application.min(Application.min(Application.min(imageIds.length, transforms.length), visibilities.length), xCoordinates.length), yCoordinates.length);
                    for (int tileIndex = 0; tileIndex < tileCount; tileIndex++) {
                        if (Application.toBoolean(visibilities[tileIndex])) {
                            GameResource.getImage(imageIds[tileIndex], Application.toInt(transforms[tileIndex])).paint(graphics, Application.toInt(xCoordinates[tileIndex]) - viewportX, Application.toInt(yCoordinates[tileIndex]) - viewportY, Application.toInt(transforms[tileIndex]));
                        }
                    }
                    return;
                }
                if (this.type == 5) {
                    if (this.text != null) {
                        int textX = this.x - viewportX;
                        if (this.textAlignment == 2) {
                            textAnchor = 24;
                            textX += ZONE_TEXT_DEFAULT_WIDTH;
                        } else if (this.textAlignment == 1) {
                            textAnchor = 17;
                            textX += 15;
                        } else {
                            textAnchor = 20;
                        }
                        graphics.setColor(this.color);
                        graphics.drawString(this.text, textX, (this.y - viewportY) + ((this.height - InkEngine.ingameFontHeight) >> 1), textAnchor);
                        return;
                    }
                    return;
                }
                GameResource.getImage(this.gfxID, this.transform).paint(graphics, this.x - viewportX, this.y - viewportY, this.transform);
                Object borderVisibleValue = executeEvent(InkCodes.EVENT_ISMETER, null, false);
                if (borderVisibleValue == null || !Application.toBoolean(borderVisibleValue)) {
                    return;
                }
                Object horizontalValue = executeEvent(InkCodes.EVENT_ISMETERHORIZONTAL, null, false);
                boolean horizontal = horizontalValue != null ? Application.toBoolean(horizontalValue) : true;
                Object leadingBorderValue = executeEvent(InkCodes.EVENT_GETMETERFILLDATA, null, false);
                if (leadingBorderValue != null) {
                    Object[] border = (Object[]) leadingBorderValue;
                    String imageId = (String) border[0];
                    int startSegment = Application.toInt(border[1]);
                    int segmentCount = Application.toInt(border[2]);
                    GameResource borderImage = GameResource.getImage(imageId, 0);
                    if (borderImage != null && startSegment > 0 && segmentCount >= 0) {
                        int segmentWidth = borderImage.imageWidth;
                        int segmentHeight = borderImage.imageHeight;
                        int segmentX = this.x - viewportX;
                        int segmentY = this.y - viewportY;
                        if (horizontal) {
                            segmentX += (startSegment - 1) * segmentWidth;
                        } else {
                            segmentY += (startSegment - 1) * segmentHeight;
                        }
                        for (int remainingSegments = segmentCount; remainingSegments > 0; remainingSegments--) {
                            graphics.drawImage(borderImage.image, segmentX, segmentY, 20);
                            if (horizontal) {
                                segmentX += segmentWidth;
                            } else {
                                segmentY += segmentHeight;
                            }
                        }
                    }
                }
                Object trailingBorderValue = executeEvent(InkCodes.EVENT_GETMETERFILLDATA2, null, false);
                if (trailingBorderValue != null) {
                    Object[] border = (Object[]) trailingBorderValue;
                    String imageId = (String) border[0];
                    int startSegment = Application.toInt(border[1]);
                    int segmentCount = Application.toInt(border[2]);
                    GameResource borderImage = GameResource.getImage(imageId, 0);
                    if (borderImage == null || startSegment <= 0 || segmentCount < 0) {
                        return;
                    }
                    int segmentWidth = borderImage.imageWidth;
                    int segmentHeight = borderImage.imageHeight;
                    int segmentX = this.x - viewportX;
                    int segmentY = this.y - viewportY;
                    if (horizontal) {
                        segmentX += (startSegment - 1) * segmentWidth;
                    } else {
                        segmentY += (startSegment - 1) * segmentHeight;
                    }
                    for (int remainingSegments = segmentCount; remainingSegments > 0; remainingSegments--) {
                        graphics.drawImage(borderImage.image, segmentX, segmentY, 20);
                        if (horizontal) {
                            segmentX += segmentWidth;
                        } else {
                            segmentY += segmentHeight;
                        }
                    }
                }
            }
        }
    }

    public void battlePanelUpdate(Graphics graphics, int viewportX, int viewportY) {
        if (this.battlePanelID == 0) {
            bpPaint(graphics, this.x - viewportX, this.y - viewportY);
            return;
        }
        if (this.battlePanelID == BATTLE_PANEL_ID_HERO_HEALTH) {
            int panelY = Application.min(Application.canvasHeight, Application.roomHeight) - (this.height >> 1);
            int panelX = Application.canvasCenterX - (this.width >> 1);
            if (this.gfxID == null) {
                int healthUnitWidth = (this.width << 7) / this.battlePanel[BATTLE_PANEL_BAR_SIZE];
                graphics.setColor(0);
                graphics.fillRect(panelX - 1, panelY - 1, this.width + 2, this.height + 2);
                graphics.setColor(16711680);
                graphics.fillRect(panelX, panelY, (healthUnitWidth * this.battlePanel[BATTLE_PANEL_HEALTH]) >> 7, this.height);
                graphics.setColor(15658734);
                graphics.fillRect(panelX + ((healthUnitWidth * this.battlePanel[BATTLE_PANEL_MAX_HEALTH]) >> 7), panelY, (healthUnitWidth * (this.battlePanel[BATTLE_PANEL_BAR_SIZE] - this.battlePanel[BATTLE_PANEL_MAX_HEALTH])) >> 7, this.height);
                return;
            }
            bpPaint(graphics, panelX, panelY);
            int healthSegmentWidth = InkEngine.healthBarFillImg.getWidth();
            for (int healthSegment = this.battlePanel[BATTLE_PANEL_HEALTH]; healthSegment < this.battlePanel[BATTLE_PANEL_MAX_HEALTH]; healthSegment++) {
                graphics.drawImage(InkEngine.healthBarFillImg, panelX + (healthSegment * healthSegmentWidth), panelY, 16 | 4);
            }
            for (int healthSegment = this.battlePanel[BATTLE_PANEL_MAX_HEALTH]; healthSegment < this.battlePanel[BATTLE_PANEL_BAR_SIZE]; healthSegment++) {
                graphics.drawImage(InkEngine.healthOverlapImg, panelX + (healthSegment * healthSegmentWidth), panelY, 16 | 4);
            }
            return;
        }
        if (this.battlePanelID == BATTLE_PANEL_ID_ENEMY_HEALTH) {
            int panelX = Application.canvasCenterX - (this.width >> 1);
            int panelY = -(this.height >> 1);
            if (this.gfxID == null) {
                int healthUnitWidth = (this.width << 7) / this.battlePanel[BATTLE_PANEL_BAR_SIZE];
                graphics.setColor(0);
                graphics.fillRect(panelX - 1, panelY - 1, this.width + 2, this.height + 2);
                graphics.setColor(16711680);
                graphics.fillRect(panelX, panelY, (healthUnitWidth * this.battlePanel[BATTLE_PANEL_HEALTH]) >> 7, this.height);
                graphics.setColor(15658734);
                graphics.fillRect(panelX + ((healthUnitWidth * this.battlePanel[BATTLE_PANEL_MAX_HEALTH]) >> 7), panelY, (healthUnitWidth * (this.battlePanel[BATTLE_PANEL_BAR_SIZE] - this.battlePanel[BATTLE_PANEL_MAX_HEALTH])) >> 7, this.height);
                return;
            }
            bpPaint(graphics, panelX, panelY);
            int healthSegmentWidth = InkEngine.healthBarFillImg.getWidth();
            for (int healthSegment = this.battlePanel[BATTLE_PANEL_HEALTH]; healthSegment < this.battlePanel[BATTLE_PANEL_MAX_HEALTH]; healthSegment++) {
                graphics.drawImage(InkEngine.healthBarFillImg, panelX + (healthSegment * healthSegmentWidth), panelY, 16 | 4);
            }
            for (int healthSegment = this.battlePanel[BATTLE_PANEL_MAX_HEALTH]; healthSegment < this.battlePanel[BATTLE_PANEL_BAR_SIZE]; healthSegment++) {
                graphics.drawImage(InkEngine.healthOverlapImg, panelX + (healthSegment * healthSegmentWidth), panelY, 16 | 4);
            }
            return;
        }
        if (this.battlePanelID == BATTLE_PANEL_ID_TIMEBAR) {
            int panelY = Application.canvasHeight > 160 ? Application.canvasCenterY : Application.canvasCenterY - (this.height >> 1);
            if (this.gfxID != null) {
                bpPaint(graphics, Application.min(Application.canvasWidth, Application.roomWidth) - this.width, panelY);
            } else {
                graphics.setColor(255);
                graphics.fillRect((Application.min(Application.canvasWidth, Application.roomWidth) - this.width) - 1, panelY - 1, this.width + 2, this.height + 2);
            }
            graphics.setColor(0);
            graphics.fillRect(Application.min(Application.canvasWidth, Application.roomWidth) - this.width, panelY, this.width, this.height - ((((this.height << 7) / this.battlePanel[BATTLE_PANEL_BAR_SIZE]) * this.battlePanel[BATTLE_PANEL_TIME]) >> 7));
            return;
        }
        if (this.battlePanelID == BATTLE_PANEL_ID_HARD_ATTACK) {
            if (this.gfxID != null) {
                bpPaint(graphics, this.x, this.y);
            } else {
                graphics.setColor(15658734);
                graphics.fillRect(this.x, this.y, this.width, this.height);
                graphics.setColor(0);
                graphics.drawRect(this.x, this.y, this.width, this.height);
            }
            if (InkEngine.battlePanelMode == 0) {
                Application.overRoomObject = this;
                return;
            }
            return;
        }
        if (this.battlePanelID == BATTLE_PANEL_ID_FAST_ATTACK) {
            if (this.gfxID != null) {
                bpPaint(graphics, this.x, this.y);
            } else {
                graphics.setColor(15658734);
                graphics.fillRect(this.x, this.y, this.width, this.height);
                graphics.setColor(0);
                graphics.drawRect(this.x, this.y, this.width, this.height);
            }
            if (InkEngine.battlePanelMode == 1) {
                Application.overRoomObject = this;
                return;
            }
            return;
        }
        if (this.battlePanelID == BATTLE_PANEL_ID_INVENTORY) {
            if (this.gfxID != null) {
                bpPaint(graphics, this.x, this.y);
            } else {
                graphics.setColor(15658734);
                graphics.fillRect(this.x, this.y, this.width, this.height);
                graphics.setColor(0);
                graphics.drawRect(this.x, this.y, this.width, this.height);
            }
            if (InkEngine.battlePanelMode == 2) {
                Application.overRoomObject = this;
                return;
            }
            return;
        }
        if (this.battlePanelID != BATTLE_PANEL_ID_ESCAPE) {
            bpPaint(graphics, this.x, this.y);
            return;
        }
        if (this.gfxID != null) {
            bpPaint(graphics, this.x, this.y);
        } else {
            graphics.setColor(15658734);
            graphics.fillRect(this.x, this.y, this.width, this.height);
            graphics.setColor(0);
            graphics.drawRect(this.x, this.y, this.width, this.height);
        }
        if (InkEngine.battlePanelMode == 3) {
            Application.overRoomObject = this;
        }
    }

    public void bpPaint(Graphics graphics, int x, int y) {
        if (this.scriptID != null && this.type == 3) {
            GameResource.getImage(this.gfxID, this.transform).paint(graphics, x, y, this.transform);
        }
    }

    public void animPaint(Graphics graphics, int viewportX, int viewportY) {
        if (this.scriptID == null || !this.visible || this.animationData == null || this.animationData[0] == null) {
            return;
        }
        int elapsedTicks = 0;
        if (this.idleAnimationTime == 0) {
            this.idleAnimationTime = Application.tickBasedTime();
        } else {
            elapsedTicks = (int) (((long) Application.tickBasedTime()) - this.idleAnimationTime);
            if (elapsedTicks >= this.animationDuration[0]) {
                this.idleAnimationTime = Application.tickBasedTime();
                elapsedTicks = 0;
            }
        }
        animRoutine(graphics, elapsedTicks, viewportX, viewportY, 0);
    }

    public void fightAnimation(Graphics graphics, int viewportX, int viewportY, int animationIndex) {
        if (this.scriptID == null || this.animationData == null || this.animationData[animationIndex] == null) {
            return;
        }
        int elapsedMillis = 0;
        if (this.animationTime == 0) {
            InkEngine.attackAnim = true;
            this.animationTime = System.currentTimeMillis();
        } else {
            elapsedMillis = (int) (System.currentTimeMillis() - this.animationTime);
            if (elapsedMillis >= this.animationDuration[animationIndex]) {
                InkEngine.attackAnim = false;
                InkEngine.battleFightTypeActive = 0;
                this.animationTime = 0L;
                return;
            }
        }
        animRoutine(graphics, elapsedMillis, viewportX, viewportY, animationIndex);
    }

    public void runAnimation(Graphics graphics, int viewportX, int viewportY) {
        if (this.scriptID == null || this.animationData == null || this.animationData[6] == null) {
            return;
        }
        int elapsedTicks = 0;
        if (this.animationTime == 0) {
            this.animationTime = Application.tickBasedTime();
        } else {
            elapsedTicks = (int) (((long) Application.tickBasedTime()) - this.animationTime);
            if (elapsedTicks >= this.animationDuration[6]) {
                int remainingLoops = this.runAnimLoops - 1;
                this.runAnimLoops = remainingLoops;
                if (remainingLoops <= 0) {
                    this.animationTime = 0L;
                    if (this.visible) {
                        GameResource.getImage(this.gfxID, this.transform).paint(graphics, this.x - viewportX, this.y - viewportY, this.transform);
                    }
                    Application.roomRepaintNeeded = true;
                    return;
                }
                this.animationTime = Application.tickBasedTime();
            }
        }
        animRoutine(graphics, elapsedTicks, viewportX, viewportY, 6);
    }

    public void animRoutine(Graphics graphics, int elapsedTime, int viewportX, int viewportY, int animationIndex) {
        Object[] animation = this.animationData[animationIndex];
        int frameEndTime = 0;
        int frameCount = this.animationParts[animationIndex];
        Object imageId = this.gfxID;
        int offsetX = 0;
        int offsetY = 0;
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            int frameOffset = frameIndex * 4;
            int frameDuration = Application.toInt(animation[frameOffset + 1]);
            frameEndTime += Math.abs(frameDuration);
            if (elapsedTime < frameEndTime) {
                if (noVibraYet && frameDuration < 0) {
                    if (frameEndTime + frameDuration < elapsedTime + 400) {
                    }
                    noVibraYet = false;
                }
                Object frameImageId = animation[frameOffset + 0];
                if (!frameImageId.equals("")) {
                    if (!frameImageId.equals(imageId)) {
                        imageId = frameImageId;
                        if (this.animationImagePoints != null && this.animationImagePoints[animationIndex] != null) {
                            this.x = this.animationImagePoints[animationIndex][frameIndex << 1];
                            this.y = this.animationImagePoints[animationIndex][(frameIndex << 1) + 1];
                        }
                    }
                    offsetX = Application.toInt(animation[frameOffset + 2]);
                    offsetY = Application.toInt(animation[frameOffset + 3]);
                    break;
                }
                return;
            }
        }
        GameResource.getImage(imageId, this.transform).paint(graphics, (this.x - viewportX) + offsetX, (this.y - viewportY) + offsetY, this.transform);
    }

    public boolean isOver(int x, int y) {
        return this.visible && this.active && x >= this.left && x <= this.right && y >= this.top && y <= this.bottom;
    }

    public String getName() {
        return (String) executeEvent(InkCodes.EVENT_GETNAME, "?", false);
    }

    public String getMoveDir() {
        return (String) executeEvent(InkCodes.EVENT_GETMOVEDIR, null, false);
    }

    public RoomObject enterHover() {
        if (this.script == null || !this.script.hasEvent(InkCodes.EVENT_HOVER_IN)) {
            return null;
        }
        executeEvent(InkCodes.EVENT_HOVER_IN, null, false);
        return this;
    }
}
