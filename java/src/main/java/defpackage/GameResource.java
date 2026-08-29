package defpackage;

import java.io.DataInputStream;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Named reconstruction of original class {@code Resource}. */
class GameResource {
    static final int TYPE_GFX = 1;
    static final int TYPE_SFX = 2;
    public int type;
    public Object ID;
    public Image image;
    public int imageWidth;
    public int imageHeight;
    public int imageRegPointX;
    public int imageRegPointY;
    public int imageTransform;
    static Vector imagesLRE = new Vector();
    public static Vector imagesImportants = new Vector();

    public GameResource(int resourceType, Object resourceId, int transform) {
        this.type = resourceType;
        this.ID = resourceId;
        this.imageTransform = transform;
    }

    public boolean equals(Object candidate) {
        if (candidate == null || !(candidate instanceof GameResource)) {
            return false;
        }
        GameResource resource = (GameResource) candidate;
        return resource.type == this.type && resource.ID.equals(this.ID) && resource.imageTransform == this.imageTransform;
    }

    public void paint(Graphics graphics, int x, int y, int transform) {
        if (this.image == null) {
            return;
        }
        int left = Application.getLeft(x, this.imageWidth, this.imageHeight, this.imageRegPointX, this.imageRegPointY, transform);
        int top = Application.getTop(y, this.imageWidth, this.imageHeight, this.imageRegPointX, this.imageRegPointY, transform);
        if (transform == 0) {
            graphics.drawImage(this.image, left, top, 20);
        } else {
            graphics.drawRegion(this.image, 0, 0, this.imageWidth, this.imageHeight, GameCanvas.transformTable[transform], left, top, 20);
        }
    }

    public void paintSimple(Graphics graphics, int x, int y, int anchor) {
        graphics.drawImage(this.image, x, y, anchor);
    }

    public static void getImageInfo(RoomObject roomObject, Object resourceId, int transform) {
        GameResource image = getImage(resourceId, transform);
        roomObject.width = image.imageWidth;
        roomObject.height = image.imageHeight;
        roomObject.regPointX = image.imageRegPointX;
        roomObject.regPointY = image.imageRegPointY;
    }

    public static GameResource getImageFromSetup(String resourcePath, String resourceId) {
        GameResource resource = new GameResource(1, resourceId, 0);
        try {
            DataInputStream input = new DataInputStream(Application.resourceGet(resourcePath));
            Application.find(input, "IHDR");
            resource.imageWidth = input.readInt();
            resource.imageHeight = input.readInt();
            Application.find(input, "oFFs");
            resource.imageRegPointX = input.readInt();
            resource.imageRegPointY = input.readInt();
            resource.image = Image.createImage(Application.resourceGet(resourcePath));
            input.close();
            return resource;
        } catch (Exception e) {
            return null;
        }
    }

    public static GameResource getImage(Object resourceId, int transform) {
        GameResource resource = new GameResource(1, resourceId, transform);
        int cachedIndex = imagesLRE.indexOf(resource);
        if (cachedIndex > -1) {
            resource = (GameResource) imagesLRE.elementAt(cachedIndex);
            imagesLRE.removeElement(resource);
        }
        if (resource.image == null) {
            try {
                String resourcePath = Application.loadRequest_getResourcePath(resourceId, transform);
                DataInputStream headerInput = new DataInputStream(Application.resourceGet(resourcePath));
                Application.find(headerInput, "IHDR");
                resource.imageWidth = headerInput.readInt();
                resource.imageHeight = headerInput.readInt();
                Application.find(headerInput, "oFFs");
                resource.imageRegPointX = headerInput.readInt();
                resource.imageRegPointY = headerInput.readInt();
                try {
                    headerInput.close();
                } catch (Exception e) {
                }
                DataInputStream imageInput = new DataInputStream(Application.resourceGet(resourcePath));
                int requiredFreeMemory = Application.max(Application.minFreeMemory, (resource.imageWidth * resource.imageHeight) << 2);
                int evictedImages = 0;
                while (Application.freeMemory() < requiredFreeMemory && !imagesLRE.isEmpty()) {
                    imagesLRE.removeElement(imagesLRE.lastElement());
                    evictedImages++;
                }
                resource.image = Image.createImage(imageInput);
                imageInput.close();
            } catch (Exception e2) {
            }
        }
        imagesLRE.insertElementAt(resource, 0);
        return resource;
    }
}
