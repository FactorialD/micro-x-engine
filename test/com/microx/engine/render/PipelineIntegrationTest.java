package com.microx.engine.render;

import com.microx.engine.assets.MeshSection;
import com.microx.engine.assets.TestGeometry;
import com.microx.engine.math.Fixed;
import java.io.FileInputStream;
import javax.microedition.lcdui.Graphics;

/** Order-independence and preview camera integration checks for the shared depth pipeline. */
public final class PipelineIntegrationTest {
    public static void main(String[] args) throws Exception {
        overlappingTrianglesAreOrderIndependent();
        cubeBoundaryAnglesAreOrderIndependent();
        System.out.println("PipelineIntegrationTest: OK");
    }

    private static void overlappingTrianglesAreOrderIndependent() {
        MeshSection near = triangle(0, 0x00ff00);
        MeshSection far = triangle(Fixed.ONE, 0xff0000);
        int[] forward = render(new MeshSection[] {far, near}, 0);
        int[] reverse = render(new MeshSection[] {near, far}, 0);
        equal(forward, reverse, "overlapping triangle section order");
        ok(forward[32 * 64 + 32] == 0x00ff00, "near triangle wins depth test");
    }

    private static void cubeBoundaryAnglesAreOrderIndependent() throws Exception {
        MeshSection[] cube = cube();
        MeshSection[] reversed = reverseSectionsAndTriangles(cube);
        int[] angles = {44, 45, 46, 134, 135, 136};
        for (int i = 0; i < angles.length; i++)
            equal(render(cube, angles[i]), render(reversed, angles[i]),
                    "cube order at " + angles[i] + " degrees");
    }

    private static int[] render(MeshSection[] meshes, int angle) {
        FrameCoordinator frame = new FrameCoordinator();
        frame.configure(64, 64, 2 * 1024 * 1024, 0);
        Graphics graphics = new Graphics(64, 64);
        frame.renderPreview(graphics, meshes, null, 0, 0, 0, Fixed.fromInt(2), angle);
        return graphics.pixels();
    }

    private static MeshSection triangle(int z, int color) {
        int[] p = {-Fixed.ONE, -Fixed.ONE, z, Fixed.ONE, -Fixed.ONE, z, -Fixed.ONE, Fixed.ONE, z};
        return new MeshSection(0, -1, color, p, new int[6], new short[] {0, 1, 2});
    }

    private static MeshSection[] cube() throws Exception {
        FileInputStream input = new FileInputStream("res/test/cube/geometry.txt");
        try {
            return TestGeometry.read(input);
        } finally {
            input.close();
        }
    }

    private static MeshSection[] reverseSectionsAndTriangles(MeshSection[] source) {
        MeshSection[] result = new MeshSection[source.length];
        for (int i = 0; i < source.length; i++) {
            MeshSection mesh = source[source.length - 1 - i];
            short[] indices = new short[mesh.triangleCount() * 3];
            for (int t = 0; t < mesh.triangleCount(); t++)
                for (int v = 0; v < 3; v++)
                    indices[t * 3 + v] = (short) mesh.index((mesh.triangleCount() - 1 - t) * 3 + v);
            int[] xyz = new int[mesh.vertexCount() * 3], uv = new int[mesh.vertexCount() * 2];
            for (int v = 0; v < mesh.vertexCount(); v++) {
                xyz[v * 3] = mesh.x(v);
                xyz[v * 3 + 1] = mesh.y(v);
                xyz[v * 3 + 2] = mesh.z(v);
                uv[v * 2] = mesh.u(v);
                uv[v * 2 + 1] = mesh.v(v);
            }
            result[i] = new MeshSection(0, -1, mesh.color(), xyz, uv, indices);
        }
        return result;
    }
    private static void equal(int[] a, int[] b, String message) {
        ok(a != null && b != null && a.length == b.length, message + " size");
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                throw new AssertionError(message);
    }
    private static void ok(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }
}
