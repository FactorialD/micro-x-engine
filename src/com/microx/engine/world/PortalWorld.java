package com.microx.engine.world;

/** Fixed-size room graph. Portal bounds are retained for rendering and crossing tests. */
public final class PortalWorld {
    private final int[] minX, maxX, minZ, maxZ, firstPortal, portalCount;
    private final int[] portalFrom, portalTo, portalNext, portalId, portalReverse, portalTransition;
    private final int[] pMinX, pMaxX, pMinY, pMaxY, pMinZ, pMaxZ;
    private final boolean[] visited;
    private final int[] queue, depth, visible, queueL, queueT, queueR, queueB, visibleL, visibleT,
            visibleR, visibleB;
    private int visibleCount;
    public PortalWorld(int rooms, int portals) {
        minX = new int[rooms];
        maxX = new int[rooms];
        minZ = new int[rooms];
        maxZ = new int[rooms];
        firstPortal = new int[rooms];
        portalCount = new int[rooms];
        portalFrom = new int[portals];
        portalTo = new int[portals];
        portalNext = new int[portals];
        portalId = new int[portals];
        portalReverse = new int[portals];
        portalTransition = new int[portals];
        pMinX = new int[portals];
        pMaxX = new int[portals];
        pMinY = new int[portals];
        pMaxY = new int[portals];
        pMinZ = new int[portals];
        pMaxZ = new int[portals];
        visited = new boolean[rooms];
        queue = new int[rooms];
        depth = new int[rooms];
        visible = new int[rooms];
        queueL = new int[rooms];
        queueT = new int[rooms];
        queueR = new int[rooms];
        queueB = new int[rooms];
        visibleL = new int[rooms];
        visibleT = new int[rooms];
        visibleR = new int[rooms];
        visibleB = new int[rooms];
        for (int i = 0; i < rooms; i++) firstPortal[i] = -1;
    }
    public void room(int i, int a, int b, int c, int d) {
        minX[i] = a;
        maxX[i] = b;
        minZ[i] = c;
        maxZ[i] = d;
    }
    public void portal(int i, int id, int from, int to, int a, int b, int c, int d, int e, int f,
            int reverse, int transition) {
        portalId[i] = id;
        portalFrom[i] = from;
        portalTo[i] = to;
        pMinX[i] = a;
        pMaxX[i] = b;
        pMinY[i] = c;
        pMaxY[i] = d;
        pMinZ[i] = e;
        pMaxZ[i] = f;
        portalReverse[i] = reverse;
        portalTransition[i] = transition;
        portalNext[i] = firstPortal[from];
        firstPortal[from] = i;
        portalCount[from]++;
    }
    public int findRoom(int x, int z) {
        for (int i = 0; i < minX.length; i++)
            if (x >= minX[i] && x <= maxX[i] && z >= minZ[i] && z <= maxZ[i])
                return i;
        return -1;
    }
    public int crossedPortal(int oldX, int oldY, int oldZ, int x, int y, int z) {
        int room = findRoom(oldX, oldZ);
        if (room < 0)
            return -1;
        for (int p = firstPortal[room]; p >= 0; p = portalNext[p])
            if (x >= pMinX[p] && x <= pMaxX[p] && y >= pMinY[p] && y <= pMaxY[p] && z >= pMinZ[p]
                    && z <= pMaxZ[p])
                return p;
        return -1;
    }
    public int portalTo(int p) {
        return portalTo[p];
    }
    public int portalId(int p) {
        return portalId[p];
    }
    public int portalReverse(int p) {
        return portalReverse[p];
    }
    public int portalTransition(int p) {
        return portalTransition[p];
    }
    public int updateVisibility(int x, int z) {
        return updateVisibility(x, 0, z, 0, 65536, 120, 160, 240, 320);
    }
    /** Traverses only portals whose projected bounds intersect the inherited clip rectangle. */
    public int updateVisibility(
            int x, int y, int z, int sin, int cos, int focalX, int focalY, int width, int height) {
        for (int i = 0; i < visited.length; i++) visited[i] = false;
        visibleCount = 0;
        int start = findRoom(x, z);
        if (start < 0)
            return 0;
        int head = 0, tail = 1;
        queue[0] = start;
        depth[0] = 0;
        queueL[0] = 0;
        queueT[0] = 0;
        queueR[0] = width - 1;
        queueB[0] = height - 1;
        visited[start] = true;
        while (head < tail) {
            int at = head++, r = queue[at], d = depth[at], l = queueL[at], t = queueT[at],
                rr = queueR[at], bb = queueB[at];
            visible[visibleCount] = r;
            visibleL[visibleCount] = l;
            visibleT[visibleCount] = t;
            visibleR[visibleCount] = rr;
            visibleB[visibleCount++] = bb;
            if (d >= 4)
                continue;
            for (int p = firstPortal[r]; p >= 0; p = portalNext[p]) {
                int n = portalTo[p];
                if (visited[n])
                    continue;
                int pl = width, pt = height, pr = -1, pb = -1;
                for (int ix = 0; ix < 2; ix++)
                    for (int iy = 0; iy < 2; iy++)
                        for (int iz = 0; iz < 2; iz++) {
                            int wx = ix == 0 ? pMinX[p] : pMaxX[p],
                                wy = iy == 0 ? pMinY[p] : pMaxY[p],
                                wz = iz == 0 ? pMinZ[p] : pMaxZ[p];
                            long dx = (long) wx - x, dz = (long) wz - z;
                            long vx = (dx * cos + dz * sin) / 65536,
                                 vz = (dz * cos - dx * sin) / 65536;
                            if (vz < 8192)
                                continue;
                            long sx = width / 2L + vx * focalX / vz,
                                 sy = height / 2L - ((long) wy - y) * focalY / vz;
                            int px = clamp(sx, -32768, 32767), py = clamp(sy, -32768, 32767);
                            if (px < pl)
                                pl = px;
                            if (px > pr)
                                pr = px;
                            if (py < pt)
                                pt = py;
                            if (py > pb)
                                pb = py;
                        }
                if (pl < l)
                    pl = l;
                if (pt < t)
                    pt = t;
                if (pr > rr)
                    pr = rr;
                if (pb > bb)
                    pb = bb;
                if (pl <= pr && pt <= pb) {
                    visited[n] = true;
                    queue[tail] = n;
                    depth[tail] = d + 1;
                    queueL[tail] = pl;
                    queueT[tail] = pt;
                    queueR[tail] = pr;
                    queueB[tail] = pb;
                    tail++;
                }
            }
        }
        return visibleCount;
    }
    private static int clamp(long v, int a, int b) {
        return v < a ? a : (v > b ? b : (int) v);
    }
    public int visibleRoom(int i) {
        return visible[i];
    }
    public int visibleCount() {
        return visibleCount;
    }
    public int visibleLeft(int i) {
        return visibleL[i];
    }
    public int visibleTop(int i) {
        return visibleT[i];
    }
    public int visibleRight(int i) {
        return visibleR[i];
    }
    public int visibleBottom(int i) {
        return visibleB[i];
    }
}
