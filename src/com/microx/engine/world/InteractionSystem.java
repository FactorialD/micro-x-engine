package com.microx.engine.world;
import com.microx.engine.math.Fixed;
/** One short player capsule query shared by doors, characters and loot. */
public final class InteractionSystem {
    public int query(Player player, EntityPool p) {
        int dx = Fixed.cos(player.yaw), dz = Fixed.sin(player.yaw), best = -1;
        long bestT = Long.MAX_VALUE, range = Fixed.fromInt(2);
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i] && interactive(p.type[i])) {
                long rx = p.x[i] - player.x, rz = p.z[i] - player.z,
                     t = (rx * dx + rz * dz) >> Fixed.SHIFT;
                if (t >= 0 && t <= range) {
                    long side = (rx * dz - rz * dx) >> Fixed.SHIFT;
                    if (side < 0)
                        side = -side;
                    if (side <= p.radius[i] + Player.RADIUS && t < bestT) {
                        best = i;
                        bestT = t;
                    }
                }
            }
        return best;
    }
    private static boolean interactive(int t) {
        return t == EntityPool.DOOR || t == EntityPool.HUMAN || t == EntityPool.CORPSE
                || t == EntityPool.CONTAINER || t == EntityPool.ITEM;
    }
}
