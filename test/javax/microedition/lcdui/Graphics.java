package javax.microedition.lcdui;

/** Minimal desktop-test surface for the software renderer's MIDP presentation call. */
public class Graphics {
    private final int width, height;
    private int[] pixels;
    private int color;
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
    public void setColor(int value) {
        color = value;
    }
    public void fillRect(int x, int y, int w, int h) {
        if (pixels == null)
            pixels = new int[width * height];
        int left = x < 0 ? 0 : x, top = y < 0 ? 0 : y;
        int right = x + w > width ? width : x + w, bottom = y + h > height ? height : y + h;
        for (int row = top; row < bottom; row++)
            for (int column = left; column < right; column++) pixels[row * width + column] = color;
    }
    public void drawRGB(
            int[] rgb, int offset, int scanlength, int x, int y, int w, int h, boolean alpha) {
        if (pixels == null || pixels.length != width * height)
            pixels = new int[width * height];
        for (int row = 0; row < h; row++)
            if (y + row >= 0 && y + row < height) {
                int left = x < 0 ? -x : 0;
                int right = x + w > width ? width - x : w;
                if (right > left)
                    System.arraycopy(rgb, offset + row * scanlength + left, pixels,
                            (y + row) * width + x + left, right - left);
            }
    }
    public int[] pixels() {
        return pixels;
    }
}
