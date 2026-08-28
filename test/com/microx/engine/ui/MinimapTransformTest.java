package com.microx.engine.ui;

import com.microx.engine.world.PortalWorld;

public final class MinimapTransformTest {
    public static void main(String[] args) {
        equal("negative minimum", 0, MinimapTransform.worldToPixel(-100, -100, 100, 52));
        equal("player movement", 38, MinimapTransform.worldToPixel(50, -100, 100, 52));
        equal("maximum", 51, MinimapTransform.worldToPixel(100, -100, 100, 52));
        equal("below range", 0, MinimapTransform.worldToPixel(-200, -100, 100, 52));
        equal("zero range", 25, MinimapTransform.worldToPixel(123, 7, 7, 52));

        equal("yaw 0 x", 15, MinimapTransform.directionX(10, 0, 5));
        equal("yaw 0 z", 10, MinimapTransform.directionY(10, 0, 5));
        equal("yaw 90 x", 10, MinimapTransform.directionX(10, 90, 5));
        equal("yaw 90 z", 15, MinimapTransform.directionY(10, 90, 5));
        equal("yaw 180 x", 5, MinimapTransform.directionX(10, 180, 5));
        equal("yaw 180 z", 10, MinimapTransform.directionY(10, 180, 5));
        equal("yaw 270 x", 10, MinimapTransform.directionX(10, 270, 5));
        equal("yaw 270 z", 5, MinimapTransform.directionY(10, 270, 5));

        PortalWorld world = new PortalWorld(2, 1);
        world.room(0, -90, -50, -40, -10);
        world.room(1, 20, 80, 10, 70);
        world.portal(0, 917, 0, 1, -55, -45, -10, 10, -20, 20, -1, 0);
        equal("room count", 2, world.roomCount());
        equal("non-sequential portal id", 917, world.portalId(0));
        equal("second room geometry", 80, world.roomMaxX(1));
    }

    private static void equal(String name, int expected, int actual) {
        if (expected != actual)
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
    }
}
