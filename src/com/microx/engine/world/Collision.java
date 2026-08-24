package com.microx.engine.world;
import com.microx.engine.math.Fixed;

/** Allocation-free swept-cylinder collision against level edges and horizontal planes. */
public final class Collision {
    private final int[] ax, az, bx, bz, bottom, top, room;
    private final int[] fRoom, fMinX, fMaxX, fMinZ, fMaxZ, fY, cRoom, cMinX, cMaxX, cMinZ, cMaxZ,
            cY;
    private int edgeCount, floorCount, ceilingCount;
    public int resultX, resultZ, resultFloor, resultCeiling;
    public Collision(int edges, int floors, int ceilings) {
        ax = new int[edges];
        az = new int[edges];
        bx = new int[edges];
        bz = new int[edges];
        bottom = new int[edges];
        top = new int[edges];
        room = new int[edges];
        fRoom = new int[floors];
        fMinX = new int[floors];
        fMaxX = new int[floors];
        fMinZ = new int[floors];
        fMaxZ = new int[floors];
        fY = new int[floors];
        cRoom = new int[ceilings];
        cMinX = new int[ceilings];
        cMaxX = new int[ceilings];
        cMinZ = new int[ceilings];
        cMaxZ = new int[ceilings];
        cY = new int[ceilings];
    }
    public void edge(int i, int r, int x1, int z1, int x2, int z2, int lo, int hi) {
        room[i] = r;
        ax[i] = x1;
        az[i] = z1;
        bx[i] = x2;
        bz[i] = z2;
        bottom[i] = lo;
        top[i] = hi;
        if (i >= edgeCount)
            edgeCount = i + 1;
    }
    public void floor(int i, int r, int a, int b, int c, int d, int y) {
        fRoom[i] = r;
        fMinX[i] = a;
        fMaxX[i] = b;
        fMinZ[i] = c;
        fMaxZ[i] = d;
        fY[i] = y;
        if (i >= floorCount)
            floorCount = i + 1;
    }
    public void ceiling(int i, int r, int a, int b, int c, int d, int y) {
        cRoom[i] = r;
        cMinX[i] = a;
        cMaxX[i] = b;
        cMinZ[i] = c;
        cMaxZ[i] = d;
        cY[i] = y;
        if (i >= ceilingCount)
            ceilingCount = i + 1;
    }
    public int floorHeight(int x, int z) {
        int h = Integer.MIN_VALUE;
        for (int i = 0; i < floorCount; i++)
            if (x >= fMinX[i] && x <= fMaxX[i] && z >= fMinZ[i] && z <= fMaxZ[i] && fY[i] > h)
                h = fY[i];
        return h;
    }
    public int ceilingHeight(int x, int z) {
        int h = Integer.MAX_VALUE;
        for (int i = 0; i < ceilingCount; i++)
            if (x >= cMinX[i] && x <= cMaxX[i] && z >= cMinZ[i] && z <= cMaxZ[i] && cY[i] < h)
                h = cY[i];
        return h;
    }
    public boolean hasClearance(int x, int y, int z, int height) {
        return ceilingHeight(x, z) - y >= height;
    }
    /** Segment visibility against solid edge geometry at the supplied eye height. */
    public boolean lineOfSight(int x1, int y, int z1, int x2, int z2) {
        long rx = x2 - x1, rz = z2 - z1;
        for (int i = 0; i < edgeCount; i++) {
            if (y <= bottom[i] || y >= top[i])
                continue;
            long sx = bx[i] - ax[i], sz = bz[i] - az[i], den = rx * sz - rz * sx;
            if (den == 0)
                continue;
            long qx = ax[i] - x1, qz = az[i] - z1, numT = qx * sz - qz * sx,
                 numU = qx * rz - qz * rx;
            if (den < 0) {
                den = -den;
                numT = -numT;
                numU = -numU;
            }
            if (numT > 0 && numT < den && numU >= 0 && numU <= den)
                return false;
        }
        return true;
    }
    /** Distance to the nearest wall intersected by a fixed-point 3-D ray. */
    public int rayDistance(int x, int y, int z, int dx, int dy, int dz, int range) {
        int best = range;
        for (int i = 0; i < edgeCount; i++) {
            long ex = bx[i] - ax[i], ez = bz[i] - az[i], den = (long) dx * ez - (long) dz * ex;
            if (den == 0)
                continue;
            long qx = ax[i] - x, qz = az[i] - z, numT = qx * ez - qz * ex, numU = qx * dz - qz * dx;
            if (den < 0) {
                den = -den;
                numT = -numT;
                numU = -numU;
            }
            if (numT <= 0 || numU < 0 || numU > den)
                continue;
            long t = (numT << Fixed.SHIFT) / den;
            if (t >= best)
                continue;
            long hitY = y + ((long) dy * t >> Fixed.SHIFT);
            if (hitY > bottom[i] && hitY < top[i])
                best = (int) t;
        }
        return best;
    }
    public void sweep(int x, int y, int z, int dx, int dz, int radius, int height, int maxStep) {
        int nx = x, nz = z,
            parts = (Math.max(Math.abs(dx), Math.abs(dz)) / (radius > 0 ? radius : 1)) + 1;
        if (parts > 32)
            parts = 32;
        for (int part = 1; part <= parts; part++) {
            int beforeX = nx, beforeZ = nz;
            nx += (int) ((long) dx * part / parts) - (int) ((long) dx * (part - 1) / parts);
            nz += (int) ((long) dz * part / parts) - (int) ((long) dz * (part - 1) / parts);
            int oldFloor = floorHeight(beforeX, beforeZ), newFloor = floorHeight(nx, nz);
            if (newFloor == Integer.MIN_VALUE || newFloor - oldFloor > maxStep
                    || ceilingHeight(nx, nz) - newFloor < height) {
                nx = beforeX;
                nz = beforeZ;
                continue;
            }
            for (int pass = 0; pass < 2; pass++)
                for (int i = 0; i < edgeCount; i++) {
                    if (y + height <= bottom[i] || y >= top[i])
                        continue;
                    long ex = bx[i] - ax[i], ez = bz[i] - az[i], px = nx - ax[i], pz = nz - az[i],
                         len = ex * ex + ez * ez;
                    if (len == 0)
                        continue;
                    long t = (px * ex + pz * ez) * Fixed.ONE / len;
                    if (t < 0)
                        t = 0;
                    else if (t > Fixed.ONE)
                        t = Fixed.ONE;
                    long qx = ax[i] + ex * t / Fixed.ONE, qz = az[i] + ez * t / Fixed.ONE,
                         ox = nx - qx, oz = nz - qz, dist = ox * ox + oz * oz,
                         rr = (long) radius * radius;
                    if (dist < rr) {
                        long side = (long) (beforeX - ax[i]) * ez - (long) (beforeZ - az[i]) * ex;
                        long normalX = ez, normalZ = -ex;
                        if (side < 0) {
                            normalX = -normalX;
                            normalZ = -normalZ;
                        }
                        long normalLen = Math.abs(normalX) + Math.abs(normalZ);
                        if (normalLen == 0)
                            continue;
                        int push = (int) ((rr - dist) / (2 * radius + 1));
                        nx += (int) (normalX * push / normalLen);
                        nz += (int) (normalZ * push / normalLen);
                    }
                }
        }
        resultX = nx;
        resultZ = nz;
        resultFloor = floorHeight(nx, nz);
        resultCeiling = ceilingHeight(nx, nz);
    }
}
