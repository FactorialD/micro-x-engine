package com.microx.engine.render;

import com.microx.engine.assets.TextureData;
import com.microx.engine.math.Fixed;

/** Integer edge-function rasterizer writing RGB and unsigned 16-bit depth. */
public final class Rasterizer {
    private int[] color;
    private short[] depth;
    private int width, height, clipL, clipT, clipR, clipB;
    void target(int[] rgb, short[] z, int w, int h) {
        color = rgb;
        depth = z;
        width = w;
        height = h;
        clip(0, 0, w - 1, h - 1);
    }
    void clip(int l, int t, int r, int b) {
        clipL = l < 0 ? 0 : l;
        clipT = t < 0 ? 0 : t;
        clipR = r >= width ? width - 1 : r;
        clipB = b >= height ? height - 1 : b;
    }
    void clear(int rgb) {
        int i;
        for (i = color.length - 1; i >= 0; i--) {
            color[i] = rgb;
            depth[i] = (short) 0xffff;
        }
    }
    boolean draw(int x0, int y0, int z0, int u0, int v0, int x1, int y1, int z1, int u1, int v1,
            int x2, int y2, int z2, int u2, int v2, TextureData texture) {
        long area = edge(x0, y0, x1, y1, x2, y2);
        if (area <= 0)
            return false;
        int minX = min(x0, x1, x2), maxX = max(x0, x1, x2), minY = min(y0, y1, y2),
            maxY = max(y0, y1, y2);
        if (minX < clipL)
            minX = clipL;
        if (minY < clipT)
            minY = clipT;
        if (maxX > clipR)
            maxX = clipR;
        if (maxY > clipB)
            maxY = clipB;
        if (minX > maxX || minY > maxY)
            return false;
        int px, py;
        boolean hit = false;
        for (py = minY; py <= maxY; py++)
            for (px = minX; px <= maxX; px++) {
                long w0 = edge(x1, y1, x2, y2, px, py), w1 = edge(x2, y2, x0, y0, px, py),
                     w2 = edge(x0, y0, x1, y1, px, py);
                if (w0 >= 0 && w1 >= 0 && w2 >= 0) {
                    long q0 = w0 * 65536L / area, q1 = w1 * 65536L / area, q2 = 65536L - q0 - q1;
                    long z = (q0 * z0 + q1 * z1 + q2 * z2) / 65536L;
                    int zz = z <= 8192
                            ? 0
                            : (z >= 16777216L ? 65534
                                              : (int) ((z - 8192L) * 65534L / (16777216L - 8192L)));
                    int at = py * width + px;
                    if (zz < (depth[at] & 0xffff)) {
                        long iz0 = (1L << 30) / z0, iz1 = (1L << 30) / z1, iz2 = (1L << 30) / z2,
                             iz = (q0 * iz0 + q1 * iz1 + q2 * iz2) / 65536L;
                        if (iz <= 0)
                            continue;
                        int u = saturate(
                                    (q0 * ((long) u0 * iz0 / 256) + q1 * ((long) u1 * iz1 / 256)
                                            + q2 * ((long) u2 * iz2 / 256))
                                    / (256L * iz)),
                            v = saturate(
                                    (q0 * ((long) v0 * iz0 / 256) + q1 * ((long) v1 * iz1 / 256)
                                            + q2 * ((long) v2 * iz2 / 256))
                                    / (256L * iz));
                        depth[at] = (short) zz;
                        color[at] = texture.sample(u, v);
                        hit = true;
                    }
                }
            }
        return hit;
    }
    private long edge(int ax, int ay, int bx, int by, int px, int py) {
        return (long) (px - ax) * (by - ay) - (long) (py - ay) * (bx - ax);
    }
    private int saturate(long n) {
        return n < Integer.MIN_VALUE ? Integer.MIN_VALUE
                                     : (n > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) n);
    }
    private int min(int a, int b, int c) {
        return a < b ? (a < c ? a : c) : (b < c ? b : c);
    }
    private int max(int a, int b, int c) {
        return a > b ? (a > c ? a : c) : (b > c ? b : c);
    }
}
