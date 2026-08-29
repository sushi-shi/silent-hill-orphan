package defpackage;

import java.io.DataInputStream;

/** Named reconstruction of original class {@code LoadRequest}. */
class ResourceRequest {
    public static final int TYPE_SCRIPT = 1;
    public static final int TYPE_GFX_STRING = 2;
    public static final int TYPE_GFX_INTEGER = 3;
    public static final int TYPE_SFX = 4;
    public static final int TYPE_ROOM = 5;
    public int type;
    public int integerID;
    public String stringID;
    public int imageTransform;

    ResourceRequest(int requestType, String resourceId) {
        this.type = requestType;
        this.stringID = resourceId;
    }

    ResourceRequest(Object resourceId, int transform) {
        try {
            this.integerID = ((Integer) resourceId).intValue();
            this.type = 3;
        } catch (Exception e) {
            this.stringID = (String) resourceId;
            this.type = 2;
        }
    }

    public Object getID() {
        return this.type == 3 ? new Integer(this.integerID) : this.stringID;
    }

    public String getDescription() {
        return null;
    }

    public boolean equals(Object candidate) {
        try {
            ResourceRequest request = (ResourceRequest) candidate;
            if (this.type != request.type) {
                return false;
            }
            return (this.stringID == request.stringID || this.stringID.equals(request.stringID)) && this.integerID == request.integerID && this.imageTransform == request.imageTransform;
        } catch (Exception e) {
            return false;
        }
    }

    public static ResourceRequest createFromInputStream(DataInputStream input) {
        try {
            int requestType = input.readUnsignedByte();
            switch (requestType) {
                case 2:
                    return new ResourceRequest(Application.readString(input), input.readUnsignedByte());
                case 3:
                    int resourceId = input.readUnsignedByte();
                    return new ResourceRequest(new Integer(resourceId), input.readUnsignedByte());
                default:
                    return new ResourceRequest(requestType, Application.readString(input));
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String getResourcePath() {
        switch (this.type) {
            case 1:
                return new StringBuffer().append(Application.gameId).append("/scr/").append(this.stringID).append(".bin").toString();
            case 2:
                return new StringBuffer().append(Application.gameId).append("/gfx/transform").append(this.imageTransform).append("/").append(this.stringID).append(".png").toString();
            case 3:
                return new StringBuffer().append(Application.gameId).append("/gfx/transform").append(this.imageTransform).append("/").append(this.integerID).append(".png").toString();
            case 4:
                return new StringBuffer().append(Application.gameId).append("/sfx/").append("mid").append("/").append(this.stringID).append(".").append("mid").toString();
            case 5:
                return new StringBuffer().append(Application.gameId).append("/rom/").append(this.stringID).append(".bin").toString();
            default:
                return null;
        }
    }

    public String toString() {
        return getResourcePath();
    }
}
