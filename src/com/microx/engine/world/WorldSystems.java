package com.microx.engine.world;
import com.microx.engine.math.Fixed;
import com.microx.engine.gameplay.FactionRelations;
/** Fixed-step scheduler: near=50 Hz, visible=25 Hz, far=5 Hz. */
public final class WorldSystems {
    private final PerceptionSystem perception = new PerceptionSystem();
    private final MovementSystem movement = new MovementSystem();
    private final StateMachineSystem states;
    private final AnomalySystem anomalies;
    private int tick;
    private int spawnSeed, nextSquadId = 30000;
    private final FactionRelations relations = new FactionRelations(8);
    public WorldSystems(int seed) {
        states = new StateMachineSystem(seed);
        anomalies = new AnomalySystem(seed);
        spawnSeed = seed;
    }
    public FactionRelations relations() {
        return relations;
    }
    public void enter(EntityPool p) {
        anomalies.enterLocation(p);
    }
    public void update(EntityPool p, Player player, Collision c, PortalWorld world, int dt) {
        tick++;
        updateSquads(p, player);
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i]) {
                p.roomId[i] = world.findRoom(p.x[i], p.z[i]);
                long dx = p.x[i] - player.x, dz = p.z[i] - player.z;
                boolean cloaked = p.type[i] == EntityPool.MUTANT
                        && p.archetype[i] == EntityPool.MUTANT_BLOODSUCKER
                        && p.state[i] != EntityPool.STATE_MELEE;
                p.flags[i] = (p.flags[i] & ~EntityPool.FLAG_VISIBLE)
                        | (!cloaked && visible(p.roomId[i], world) ? EntityPool.FLAG_VISIBLE : 0);
                int divisor = dx * dx + dz * dz < (long) Fixed.fromInt(10) * Fixed.fromInt(10)
                        ? 1
                        : ((p.flags[i] & EntityPool.FLAG_VISIBLE) != 0 ? 2 : 10);
                p.aux[i] = (p.aux[i] & 65535) | (divisor << 16);
                if (tick % divisor == 0)
                    p.flags[i] |= EntityPool.FLAG_UPDATE;
                else
                    p.flags[i] &= ~EntityPool.FLAG_UPDATE;
            }
        perception.setRelations(relations);
        perception.update(p, player, c);
        states.update(p, player, dt);
        anomalies.update(p, player, dt);
        movement.update(p, player, c, dt);
    }
    /** Bounded deterministic ambient squads; aux stores their positive squad id. */
    private void updateSquads(EntityPool p, Player player) {
        if (tick % 50 != 0)
            return;
        long far2 = (long) Fixed.fromInt(35) * Fixed.fromInt(35);
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i] && p.aux[i] >= 30000) {
                long dx = p.x[i] - player.x, dz = p.z[i] - player.z;
                if (dx * dx + dz * dz > far2)
                    p.remove(i);
            }
        if (p.typeCount(EntityPool.MUTANT) + 3 > EntityPool.MAX_MUTANTS)
            return;
        spawnSeed = spawnSeed * 1103515245 + 12345;
        int squad = nextSquadId++, angle = (spawnSeed >>> 1) % 360;
        int baseX = player.x + Fixed.mul(Fixed.cos(angle), Fixed.fromInt(20));
        int baseZ = player.z + Fixed.mul(Fixed.sin(angle), Fixed.fromInt(20));
        for (int n = 0; n < 3; n++) {
            int i = p.spawn(EntityPool.MUTANT, baseX + n * Fixed.ONE, player.y, baseZ, 40);
            if (i < 0)
                break;
            p.aux[i] = squad;
            p.archetype[i] = (spawnSeed + n & 3) + 1;
        }
    }
    private boolean visible(int room, PortalWorld w) {
        for (int i = 0; i < w.visibleCount(); i++)
            if (w.visibleRoom(i) == room)
                return true;
        return false;
    }
}
