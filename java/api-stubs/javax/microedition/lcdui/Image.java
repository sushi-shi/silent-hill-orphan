package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;

public class Image {
    public static int oracleStringCreateCalls;
    public static String oracleStringCreateName;
    public static Image oracleStringCreatedImage;

    protected Image() {}

    public static Image createImage(int width, int height) {
        return new Image();
    }

    public static Image createImage(String name) throws IOException {
        oracleStringCreateCalls++;
        oracleStringCreateName = name;
        oracleStringCreatedImage = new Image();
        return oracleStringCreatedImage;
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return new Image();
    }

    public static Image createImage(Image source) {
        return new Image();
    }

    public static Image createImage(InputStream stream) throws IOException {
        return new Image();
    }

    public static void oracleResetStringCreate() {
        oracleStringCreateCalls = 0;
        oracleStringCreateName = null;
        oracleStringCreatedImage = null;
    }

    public Graphics getGraphics() {
        return new Graphics();
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {}
}
