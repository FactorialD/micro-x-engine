package com.microx.engine.render;

import com.microx.engine.math.Fixed;
import com.microx.engine.world.EntityPool;

/** Allocation-free procedural entity meshes submitted to the common triangle/depth pipeline. */
public final class EntityBillboardRenderer {
    private static final int[] BOX = {
            -1, 0, -1, 1, 0, -1, 1, 1, -1, -1, 1, -1, -1, 0, 1, 1, 0, 1, 1, 1, 1, -1, 1, 1};
    private static final short[] BOX_TRI = {0, 3, 2, 0, 2, 1, 4, 5, 6, 4, 6, 7, 0, 4, 7, 0, 7, 3, 1,
            2, 6, 1, 6, 5, 3, 7, 6, 3, 6, 2, 0, 1, 5, 0, 5, 4};
    private static final int[] SPHERE = {0, 1, 0, 0, -1, 0, -1, 0, 0, 1, 0, 0, 0, 0, -1, 0, 0, 1};
    private static final short[] SPHERE_TRI = {
            0, 2, 4, 0, 4, 3, 0, 3, 5, 0, 5, 2, 1, 4, 2, 1, 3, 4, 1, 5, 3, 1, 2, 5};
    private final int[] vx = new int[8], vy = new int[8], vz = new int[8];

    void render(EntityPool pool, int room, RenderCamera camera, Clipper clipper,
            Rasterizer rasterizer, int width, int height) {
        for (int entity = 0; entity < pool.capacity(); entity++)
            if (pool.active[entity] && (pool.flags[entity] & EntityPool.FLAG_VISIBLE) != 0
                    && (room == -2
                            || (room < 0 ? pool.roomId[entity] < 0 : pool.roomId[entity] == room)))
                primitive(pool, entity, camera, clipper, rasterizer, width, height);
    }

    private void primitive(EntityPool pool, int entity, RenderCamera camera, Clipper clipper,
            Rasterizer rasterizer, int width, int height) {
        boolean sphere = pool.type[entity] == EntityPool.MUTANT;
        int[] vertices = sphere ? SPHERE : BOX;
        short[] triangles = sphere ? SPHERE_TRI : BOX_TRI;
        int radius = pool.radius[entity] > 0 ? pool.radius[entity] : Fixed.ONE / 4;
        int sx = sphere ? radius : EntityRenderCatalog.halfWidth(pool, entity);
        int sy = sphere ? radius : EntityRenderCatalog.height(pool, entity);
        int sz = sphere ? radius : (pool.type[entity] == EntityPool.DOOR ? radius / 3 : sx);
        int centerY = sphere ? Fixed.add(pool.y[entity], radius) : pool.y[entity];
        int sin = Fixed.sin(pool.direction[entity]), cos = Fixed.cos(pool.direction[entity]);
        int count = vertices.length / 3;
        for (int i = 0; i < count; i++) {
            int lx = vertices[i * 3] * sx;
            int ly = vertices[i * 3 + 1] * sy;
            int lz = vertices[i * 3 + 2] * sz;
            int wx = Fixed.add(pool.x[entity], Fixed.add(Fixed.mul(lx, cos), Fixed.mul(lz, sin)));
            int wz = Fixed.add(pool.z[entity], Fixed.sub(Fixed.mul(lz, cos), Fixed.mul(lx, sin)));
            int dx = Fixed.sub(wx, camera.x), dz = Fixed.sub(wz, camera.z);
            vx[i] = Fixed.add(Fixed.mul(dx, camera.cos), Fixed.mul(dz, camera.sin));
            vy[i] = Fixed.sub(Fixed.add(centerY, ly), camera.y);
            vz[i] = Fixed.sub(Fixed.mul(dz, camera.cos), Fixed.mul(dx, camera.sin));
        }
        int color = EntityRenderCatalog.color(pool, entity);
        for (int t = 0; t < triangles.length; t += 3) {
            int a = triangles[t], b = triangles[t + 1], c = triangles[t + 2];
            int n = clipper.clip(vx[a], vy[a], vz[a], 0, 0, vx[b], vy[b], vz[b], 0, 0, vx[c], vy[c],
                    vz[c], 0, 0, camera.near);
            for (int fan = 1; fan < n - 1; fan++)
                draw(clipper, rasterizer, 0, fan, fan + 1, camera, width, height, color);
        }
    }

    private void draw(Clipper c, Rasterizer r, int a, int b, int d, RenderCamera camera, int width,
            int height, int color) {
        int za = c.value(a, 2), zb = c.value(b, 2), zd = c.value(d, 2);
        int xa = project(c.value(a, 0), camera.focalX, za, width / 2);
        int ya = project(Fixed.neg(c.value(a, 1)), camera.focalY, za, height / 2);
        int xb = project(c.value(b, 0), camera.focalX, zb, width / 2);
        int yb = project(Fixed.neg(c.value(b, 1)), camera.focalY, zb, height / 2);
        int xd = project(c.value(d, 0), camera.focalX, zd, width / 2);
        int yd = project(Fixed.neg(c.value(d, 1)), camera.focalY, zd, height / 2);
        if ((long) (xd - xa) * (yb - ya) - (long) (yd - ya) * (xb - xa) > 0)
            r.draw(xa, ya, za, 0, 0, xb, yb, zb, 0, 0, xd, yd, zd, 0, 0, null, color);
    }

    private int project(int value, int focal, int z, int center) {
        long n = center + (long) value * focal / z;
        return n < -32768 ? -32768 : (n > 32767 ? 32767 : (int) n);
    }
}
