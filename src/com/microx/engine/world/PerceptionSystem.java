package com.microx.engine.world;
import com.microx.engine.math.Fixed;
import com.microx.engine.gameplay.FactionRelations;
/** Allocation-free human cone/LOS perception pass. */
public final class PerceptionSystem {
    public static final int RANGE = Fixed.ONE * 18, CONE_COS = Fixed.ONE / 2;
    private FactionRelations relations;
    public PerceptionSystem() {}
    public PerceptionSystem(FactionRelations value) {
        relations = value;
    }
    public void setRelations(FactionRelations value) {
        relations = value;
    }
    public void update(EntityPool p, Player player, Collision collision) {
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i] && (p.flags[i] & EntityPool.FLAG_UPDATE) != 0
                    && (p.type[i] == EntityPool.HUMAN || p.type[i] == EntityPool.MUTANT)) {
                long dx = player.x - p.x[i], dz = player.z - p.z[i], d2 = dx * dx + dz * dz;
                boolean seen = false;
                if (d2 <= (long) RANGE * RANGE) {
                    long dot = (dx * Fixed.cos(p.direction[i]) + dz * Fixed.sin(p.direction[i]))
                            >> Fixed.SHIFT;
                    seen = dot > 0
                            && dot * dot
                                    >= ((long) CONE_COS * CONE_COS / Fixed.ONE) * d2 / Fixed.ONE
                            && collision.lineOfSight(
                                    p.x[i], p.y[i] + Fixed.ONE, p.z[i], player.x, player.z);
                }
                boolean hostile = p.type[i] == EntityPool.MUTANT || relations == null
                        || relations.hostile(p.faction[i], player.faction);
                if (seen && hostile) {
                    p.flags[i] |= EntityPool.FLAG_PERCEIVES_PLAYER;
                    p.target[i] = -2;
                    if (p.type[i] == EntityPool.HUMAN)
                        p.timer[i] = 2000;
                } else
                    p.flags[i] &= ~EntityPool.FLAG_PERCEIVES_PLAYER;
            }
    }
}
