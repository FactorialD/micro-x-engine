package com.microx.engine.world;
import com.microx.engine.math.Fixed;
import com.microx.engine.gameplay.ItemCatalog;
public final class SimulationTest {
    public static void main(String[] args) {
        if (!ItemCatalog.loadDefault())
            throw new AssertionError("gameplay catalog unavailable");
        statesAndSight();
        corpseAndCapacity();
        determinism();
        mutantArchetypes();
        squadBudget();
        anomalies();
        System.out.println("SimulationTest OK");
    }
    private static void mutantArchetypes() {
        for (int kind = EntityPool.MUTANT_BASIC; kind <= EntityPool.MUTANT_AOE; kind++) {
            EntityPool p = new EntityPool(8);
            int m = p.spawn(EntityPool.MUTANT, 0, 0, 0, 50);
            p.archetype[m] = kind;
            p.flags[m] |= EntityPool.FLAG_PERCEIVES_PLAYER;
            Player player = new Player();
            player.reset(kind == EntityPool.MUTANT_LEAPER ? Fixed.fromInt(6) : Fixed.ONE, 0, 0);
            int health = player.health, yaw = player.yaw;
            new StateMachineSystem(9).update(p, player, 20);
            int expected = kind == EntityPool.MUTANT_LEAPER ? EntityPool.STATE_LEAP
                    : kind == EntityPool.MUTANT_PSI         ? EntityPool.STATE_PSI
                    : kind == EntityPool.MUTANT_AOE         ? EntityPool.STATE_AOE
                                                            : EntityPool.STATE_MELEE;
            eq(expected, p.state[m]);
            if (kind != EntityPool.MUTANT_LEAPER)
                ok(player.health < health);
            if (kind == EntityPool.MUTANT_PSI)
                ok(player.yaw != yaw);
            if (kind == EntityPool.MUTANT_BLOODSUCKER)
                ok((p.flags[m] & EntityPool.FLAG_VISIBLE) != 0);
        }
        EntityPool aoe = new EntityPool(2);
        int m = aoe.spawn(EntityPool.MUTANT, 0, 0, 0, 10);
        aoe.archetype[m] = EntityPool.MUTANT_AOE;
        aoe.flags[m] |= EntityPool.FLAG_PERCEIVES_PLAYER;
        Player jumping = new Player();
        jumping.reset(Fixed.ONE, Fixed.ONE, 0);
        jumping.grounded = false;
        new StateMachineSystem(1).update(aoe, jumping, 20);
        eq(100, jumping.health);
    }
    private static void squadBudget() {
        EntityPool p = new EntityPool(96);
        Player player = new Player();
        PortalWorld world = new PortalWorld(1, 0);
        world.room(0, Fixed.fromInt(-100), Fixed.fromInt(100), Fixed.fromInt(-100),
                Fixed.fromInt(100));
        world.updateVisibility(0, 0);
        WorldSystems systems = new WorldSystems(77);
        for (int i = 0; i < 1000; i++) systems.update(p, player, space(), world, 20);
        ok(p.typeCount(EntityPool.MUTANT) <= EntityPool.MAX_MUTANTS);
        ok(p.activeCount() <= p.capacity());
    }
    private static void anomalies() {
        EntityPool entities = new EntityPool(16);
        int anomaly = entities.spawn(EntityPool.ANOMALY, 0, 0, 0, 1);
        entities.aux[anomaly] = AnomalySystem.SLOW;
        entities.radius[anomaly] = Fixed.fromInt(2);
        Player player = new Player();
        AnomalySystem system = new AnomalySystem(7);
        system.update(entities, player, 20);
        ok(player.slowTimer > 0);
        for (int attempt = 0; attempt < 32 && entities.typeCount(EntityPool.ITEM) == 0; attempt++)
            system.enterLocation(entities);
        for (int i = 0; i < entities.capacity(); i++)
            if (entities.active[i] && entities.type[i] == EntityPool.ITEM) {
                int item = entities.aux[i] & 65535;
                ok(ItemCatalog.valid(item));
                eq(ItemCatalog.TYPE_ARTIFACT, ItemCatalog.type(item));
                return;
            }
        throw new AssertionError("artifact was not spawned");
    }
    private static void statesAndSight() {
        Collision c = space();
        Player pl = new Player();
        pl.reset(Fixed.fromInt(3), 0, 0);
        EntityPool p = new EntityPool(64);
        int h = p.spawn(EntityPool.HUMAN, 0, 0, 0, 100);
        p.direction[h] = 0;
        new PerceptionSystem().update(p, pl, c);
        StateMachineSystem s = new StateMachineSystem(7);
        s.update(p, pl, 20);
        eq(EntityPool.STATE_COMBAT, p.state[h]);
        Collision wall = space();
        wall.edge(0, 0, Fixed.ONE, Fixed.fromInt(-1), Fixed.ONE, Fixed.ONE, 0, Fixed.fromInt(3));
        ok(!wall.lineOfSight(0, Fixed.ONE, 0, Fixed.fromInt(2), 0));
    }
    private static void corpseAndCapacity() {
        EntityPool p = new EntityPool(64);
        for (int i = 0; i < EntityPool.MAX_LIVE_NPCS; i++)
            ok(p.spawn(EntityPool.HUMAN, i, 0, 0, 1) >= 0);
        eq(-1, p.spawn(EntityPool.HUMAN, 0, 0, 0, 1));
        int c = p.spawn(EntityPool.CORPSE, 0, 0, 0, 1);
        p.timer[c] = 1;
        new StateMachineSystem(1).update(p, new Player(), 20);
        ok(!p.active[c]);
    }
    private static void determinism() {
        EntityPool a = new EntityPool(32), b = new EntityPool(32);
        int ia = a.spawn(EntityPool.MUTANT, 0, 0, 0, 10),
            ib = b.spawn(EntityPool.MUTANT, 0, 0, 0, 10);
        a.state[ia] = b.state[ib] = EntityPool.STATE_MELEE;
        a.aux[ia] = b.aux[ib] = 15;
        StateMachineSystem sa = new StateMachineSystem(99), sb = new StateMachineSystem(99);
        Player pa = new Player(), pb = new Player();
        for (int i = 0; i < 20; i++) {
            sa.update(a, pa, 800);
            sb.update(b, pb, 800);
        }
        eq(a.state[ia], b.state[ib]);
        eq(pa.health, pb.health);
    }
    private static Collision space() {
        Collision c = new Collision(1, 1, 1);
        c.floor(0, 0, Fixed.fromInt(-10), Fixed.fromInt(10), Fixed.fromInt(-10), Fixed.fromInt(10),
                0);
        c.ceiling(0, 0, Fixed.fromInt(-10), Fixed.fromInt(10), Fixed.fromInt(-10),
                Fixed.fromInt(10), Fixed.fromInt(4));
        return c;
    }
    private static void eq(int a, int b) {
        if (a != b)
            throw new AssertionError(a + " != " + b);
    }
    private static void ok(boolean v) {
        if (!v)
            throw new AssertionError();
    }
}
