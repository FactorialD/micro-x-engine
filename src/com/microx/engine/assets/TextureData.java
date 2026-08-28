package com.microx.engine.assets;

import com.microx.engine.math.Fixed;

/** Immutable MXT2 indexed texture with wrapping Q16.16 coordinates. */
public final class TextureData {
    private final int width, height;
    private final int[] palette;
    private final byte[] pixels;
    public TextureData(int w, int h, int[] colors, byte[] indices) {
        if (w <= 0 || h <= 0 || w > 256 || h > 256 || colors == null || colors.length < 1
                || colors.length > 256 || indices == null || indices.length != w * h)
            throw new IllegalArgumentException("invalid indexed texture");
        width = w;
        height = h;
        palette = colors;
        pixels = indices;
        for (int i = 0; i < indices.length; i++)
            if ((indices[i] & 255) >= colors.length)
                throw new IllegalArgumentException("palette index out of range");
    }
    public int sample(int u, int v) {
        int x = Fixed.floorToInt(u) % width, y = Fixed.floorToInt(v) % height;
        if (x < 0)
            x += width;
        if (y < 0)
            y += height;
        return palette[pixels[y * width + x] & 255];
    }
    public int memoryBytes() {
        return 16 + palette.length * 4 + pixels.length;
    }
}
