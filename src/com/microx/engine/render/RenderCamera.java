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
}
