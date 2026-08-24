package com.microx.engine.render;

import com.microx.engine.assets.MeshSection;
import com.microx.engine.math.Fixed;

/** Saturating Q16.16 world-to-view transform into reusable arrays. */
public final class VertexTransformer {
    private int[] x = new int[0], y = new int[0], z = new int[0];

    void reserve(int count) {
        if (x.length >= count) return;
        x = new int[count]; y = new int[count]; z = new int[count];
    }
    void transform(MeshSection mesh, RenderCamera camera) {
        int i;
        for (i = 0; i < mesh.vertexCount(); i++) {
            int dx = Fixed.sub(mesh.x(i), camera.x);
            int dz = Fixed.sub(mesh.z(i), camera.z);
            x[i] = Fixed.add(Fixed.mul(dx, camera.cos), Fixed.mul(dz, camera.sin));
            y[i] = Fixed.sub(mesh.y(i), camera.y);
            z[i] = Fixed.sub(Fixed.mul(dz, camera.cos), Fixed.mul(dx, camera.sin));
        }
    }
    int x(int i) { return x[i]; } int y(int i) { return y[i]; } int z(int i) { return z[i]; }
}
