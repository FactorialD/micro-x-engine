package com.microx.engine.render;

import com.microx.engine.math.Fixed;
import com.microx.engine.world.Player;

/** Allocation-free Q16.16 camera; near=0.125 m and normalized depth far=256 m. */
public final class RenderCamera {
    int x, y, z, sin, cos, focalX, focalY;
    final int near = Fixed.ONE / 8;

    void update(Player player, int width, int height) {
        x = player.x;
        y = player.y + (Fixed.ONE * 3 / 2);
        z = player.z;
        sin = Fixed.sin(player.yaw);
        cos = Fixed.cos(player.yaw);
        focalX = width * Fixed.ONE / 2;
        focalY = height * Fixed.ONE / 2;
    }

    /** Places the normal world camera on an orbit which keeps the complete model in view. */
    void preview(
            int centerX, int centerY, int centerZ, int radius, int angle, int width, int height) {
        focalX = width * Fixed.ONE / 2;
        focalY = height * Fixed.ONE / 2;
        // Reserve 20% on each axis.  Using the enclosing-sphere radius means the same
        // distance is valid at every orbit angle, including the nearest point to the eye.
        int usableX = Math.max(1, width * 2 / 5);
        int usableY = Math.max(1, height * 2 / 5);
        int fitX = Fixed.mul(radius, Fixed.div(focalX, Fixed.fromInt(usableX)));
        int fitY = Fixed.mul(radius, Fixed.div(focalY, Fixed.fromInt(usableY)));
        int clearance = Math.max(near, Math.max(fitX, fitY));
        int distance = Fixed.add(radius, clearance);
        sin = Fixed.sin(angle);
        cos = Fixed.cos(angle);
        x = Fixed.add(centerX, Fixed.mul(sin, distance));
        y = centerY;
        z = Fixed.sub(centerZ, Fixed.mul(cos, distance));
    }
}
