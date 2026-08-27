package com.microx.engine.world;
import com.microx.engine.combat.DamagePipeline;
import com.microx.engine.math.Fixed;
/** Deterministic AI, anomaly and corpse state transitions. */
public final class StateMachineSystem {
    private int seed;
    public StateMachineSystem(int value) {
        seed = value;
    }
    private int random() {
        seed = seed * 1103515245 + 12345;
        return seed >>> 1;
    }
    public void update(EntityPool p, Player player, int dt) {
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i] && (p.flags[i] & EntityPool.FLAG_UPDATE) != 0) {
                if (p.timer[i] > 0)
                    p.timer[i] -= dt;
                if (p.type[i] == EntityPool.CORPSE && p.timer[i] <= 0) {
                    p.remove(i);
                } else if (p.type[i] == EntityPool.HUMAN) {
                    if ((p.flags[i] & EntityPool.FLAG_PERCEIVES_PLAYER) != 0)
                        p.state[i] = EntityPool.STATE_COMBAT;
                    else if (p.timer[i] > 0)
                        p.state[i] = EntityPool.STATE_ALERT;
                    else
                        p.state[i] = EntityPool.STATE_IDLE;
                    if (p.state[i] == EntityPool.STATE_COMBAT && p.timer[i] <= 0) {
                        DamagePipeline.apply(player, DamagePipeline.PHYSICAL, 5);
                        p.timer[i] = 600;
                    }
                } else if (p.type[i] == EntityPool.MUTANT) {
                    if ((p.flags[i] & EntityPool.FLAG_PERCEIVES_PLAYER) != 0
                            && p.state[i] == EntityPool.STATE_IDLE)
                        p.state[i] = EntityPool.STATE_MELEE;
                    if (p.timer[i] <= 0 && p.state[i] >= EntityPool.STATE_MELEE) {
                        int mask = p.aux[i];
                        int choices = 1 + (mask & 1) + ((mask >> 1) & 1) + ((mask >> 2) & 1)
                                + ((mask >> 3) & 1),
                            pick = random() % choices, s = EntityPool.STATE_MELEE;
                        for (int b = 0; b < 4; b++)
                            if ((mask & (1 << b)) != 0 && pick-- == 0) {
                                s = EntityPool.STATE_LEAP + b;
                                break;
                            }
                        p.state[i] = s;
                        p.timer[i] = 800;
                        if (s == EntityPool.STATE_MELEE
                                && distance2(p.x[i], p.z[i], player.x, player.z)
                                        < (long) Fixed.ONE * Fixed.ONE * 3)
                            DamagePipeline.apply(player, DamagePipeline.MELEE, 8);
                    }
                }
            }
    }
    private static long distance2(int x, int z, int a, int b) {
        long dx = x - a, dz = z - b;
        return dx * dx + dz * dz;
    }
}
