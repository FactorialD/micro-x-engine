package javax.microedition.lcdui;

/** Minimal desktop-test surface for the software renderer's MIDP presentation call. */
public class Graphics {
    private final int width, height;
    private int[] pixels;
    public Graphics(int w, int h) {
        width = w;
        height = h;
    }
    public int getClipWidth() {
        return width;
    }
    public int getClipHeight() {
        return height;
    }
    public void setColor(int color) {}
    public void fillRect(int x, int y, int w, int h) {}
    public void drawRGB(
            int[] rgb, int offset, int scanlength, int x, int y, int w, int h, boolean alpha) {
        pixels = new int[w * h];
        for (int row = 0; row < h; row++)
            System.arraycopy(rgb, offset + row * scanlength, pixels, row * w, w);
    }
    public int[] pixels() {
        return pixels;
    }
}
