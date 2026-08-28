package com.microx.engine.world;
import com.microx.engine.combat.DamagePipeline;
import com.microx.engine.math.Fixed;
/** Deterministic AI, anomaly and corpse state transitions. */
public final class StateMachineSystem {
    private int seed;
    private static final long MELEE_RANGE2 = (long) Fixed.ONE * Fixed.ONE * 3;
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
                    mutant(p, i, player);
                }
            }
    }
    private void mutant(EntityPool p, int i, Player player) {
        boolean sees = (p.flags[i] & EntityPool.FLAG_PERCEIVES_PLAYER) != 0;
        int kind = p.archetype[i];
        if (!sees) {
            p.state[i] = EntityPool.STATE_IDLE;
            if (kind == EntityPool.MUTANT_BLOODSUCKER)
                p.flags[i] &= ~EntityPool.FLAG_VISIBLE;
            return;
        }
        if (p.timer[i] > 0)
            return;
        long d2 = distance2(p.x[i], p.z[i], player.x, player.z);
        int state = EntityPool.STATE_MELEE, cooldown = 700;
        if (kind == EntityPool.MUTANT_LEAPER && d2 > MELEE_RANGE2) {
            state = EntityPool.STATE_LEAP;
            cooldown = 1400;
        } else if (kind == EntityPool.MUTANT_PSI) {
            state = EntityPool.STATE_PSI;
            cooldown = 1800;
            int turn = (random() & 1) == 0 ? -12 : 12;
            player.yaw += turn;
            player.pitch = Fixed.clamp(player.pitch + turn / 3, -70, 70);
            DamagePipeline.apply(player, DamagePipeline.ANOMALY, 7);
        } else if (kind == EntityPool.MUTANT_AOE) {
            state = EntityPool.STATE_AOE;
            cooldown = 1600;
            if (d2 <= (long) Fixed.fromInt(5) * Fixed.fromInt(5) && player.grounded)
                DamagePipeline.apply(player, DamagePipeline.ANOMALY, 12);
        } else if (d2 < MELEE_RANGE2) {
            DamagePipeline.apply(
                    player, DamagePipeline.MELEE, kind == EntityPool.MUTANT_BLOODSUCKER ? 12 : 8);
        }
        p.state[i] = state;
        p.timer[i] = cooldown;
        if (kind == EntityPool.MUTANT_BLOODSUCKER)
            p.flags[i] |= EntityPool.FLAG_VISIBLE;
    }
    private static long distance2(int x, int z, int a, int b) {
        long dx = x - a, dz = z - b;
        return dx * dx + dz * dz;
    }
}
