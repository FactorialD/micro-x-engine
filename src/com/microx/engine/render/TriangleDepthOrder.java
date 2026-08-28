package com.microx.engine.render;

import com.microx.engine.assets.MeshSection;

/** Allocation-free painter's-order preparation shared by the preview and its tests. */
final class TriangleDepthOrder {
    private TriangleDepthOrder() {}

    static int prepare(MeshSection[] sections, MeshSection[] triangleSections,
            int[] triangleIndexes, int[] depths, int centerX, int centerZ, int sin, int cos) {
        int count = 0;
        for (int s = 0; s < sections.length; s++) {
            MeshSection mesh = sections[s];
            for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
                int first = triangle * 3;
                int a = mesh.index(first), b = mesh.index(first + 1), c = mesh.index(first + 2);
                long total = cameraDepth(mesh, a, centerX, centerZ, sin, cos)
                        + cameraDepth(mesh, b, centerX, centerZ, sin, cos)
                        + cameraDepth(mesh, c, centerX, centerZ, sin, cos);
                triangleSections[count] = mesh;
                triangleIndexes[count] = triangle;
                depths[count] = (int) (total / 3);
                count++;
            }
        }

        // Positive depth is nearer: projectY subtracts depth from model-space y.
        // Consequently ascending depth is the required far-to-near painter's order.
        for (int i = 1; i < count; i++) {
            MeshSection section = triangleSections[i];
            int triangle = triangleIndexes[i], depth = depths[i], insertion = i;
            while (insertion > 0 && depths[insertion - 1] > depth) {
                triangleSections[insertion] = triangleSections[insertion - 1];
                triangleIndexes[insertion] = triangleIndexes[insertion - 1];
                depths[insertion] = depths[insertion - 1];
                insertion--;
            }
            triangleSections[insertion] = section;
            triangleIndexes[insertion] = triangle;
            depths[insertion] = depth;
        }
        return count;
    }

    private static long cameraDepth(
            MeshSection mesh, int vertex, int centerX, int centerZ, int sin, int cos) {
        long x = mesh.x(vertex) - centerX, z = mesh.z(vertex) - centerZ;
        return (x * sin + z * cos) >> 16;
    }
}
