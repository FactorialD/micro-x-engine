package com.microx.engine.ui;

import com.microx.engine.math.Fixed;

/** Pure coordinate helpers shared by the minimap renderer and desktop tests. */
public final class MinimapTransform {
    private MinimapTransform() {}

    /** Maps a world coordinate into the inclusive pixel range [0, size - 1]. */
    public static int worldToPixel(int value, int min, int max, int size) {
        if (size <= 1)
            return 0;
        if (max <= min)
            return (size - 1) / 2;
        long pixel = ((long) value - min) * (size - 1) / ((long) max - min);
        if (pixel < 0)
            return 0;
        if (pixel >= size)
            return size - 1;
        return (int) pixel;
    }

    public static int directionX(int origin, int yaw, int length) {
        return origin + (int) ((long) Fixed.cos(yaw) * length / Fixed.ONE);
    }

    public static int directionY(int origin, int yaw, int length) {
        return origin + (int) ((long) Fixed.sin(yaw) * length / Fixed.ONE);
    }
}
