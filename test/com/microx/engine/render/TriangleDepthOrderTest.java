package com.microx.engine.render;

import com.microx.engine.assets.MeshSection;
import com.microx.engine.math.Fixed;

public final class TriangleDepthOrderTest {
    public static void main(String[] args) {
        MeshSection far = triangleAt(1, -Fixed.ONE);
        MeshSection near = triangleAt(2, Fixed.ONE);
        MeshSection[] fileOrder = {near, far};
        MeshSection[] ordered = new MeshSection[2];
        int[] triangles = new int[2], depths = new int[2];

        int count = TriangleDepthOrder.prepare(
                fileOrder, ordered, triangles, depths, 0, 0, Fixed.sin(0), Fixed.cos(0));
        check(count == 2, "all sections contribute triangles");
        check(ordered[0] == far && ordered[1] == near,
                "near triangle is painted after far triangle regardless of file order");

        TriangleDepthOrder.prepare(
                fileOrder, ordered, triangles, depths, 0, 0, Fixed.sin(180), Fixed.cos(180));
        check(ordered[0] == near && ordered[1] == far,
                "painter order changes when the camera turns around");
        System.out.println("TriangleDepthOrderTest OK");
    }

    private static MeshSection triangleAt(int room, int z) {
        int[] positions = {-Fixed.ONE, 0, z, Fixed.ONE, 0, z, 0, Fixed.ONE, z};
        return new MeshSection(room, 0, positions, new int[6], new short[] {0, 1, 2});
    }

    private static void check(boolean value, String label) {
        if (!value)
            throw new AssertionError(label);
    }
}
