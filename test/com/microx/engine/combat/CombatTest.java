package com.microx.engine.combat;
import com.microx.engine.Input;
import com.microx.engine.math.Fixed;
import com.microx.engine.world.*;
import com.microx.engine.gameplay.ItemCatalog;
public final class CombatTest {
    public static void main(String[] a) {
        if (!ItemCatalog.loadDefault())
            throw new AssertionError("gameplay catalog unavailable");
        wallAndNearest();
        seededSpread();
        magazineReloadAndJam();
        damageAndEffects();
        stamina();
        System.out.println("CombatTest: OK");
    }
    private static void wallAndNearest() {
        Player p = player();
        EntityPool e = new EntityPool(8);
        int near = e.spawn(EntityPool.HUMAN, Fixed.fromInt(4), 0, 0, 100),
            far = e.spawn(EntityPool.HUMAN, Fixed.fromInt(7), 0, 0, 100);
        HitScan h = new HitScan();
        eq(near, h.fire(p, e, space(false), Fixed.fromInt(20), 0, 20));
        eq(80, e.health[near]);
        Collision wall = space(true);
        eq(-1, h.fire(p, e, wall, Fixed.fromInt(20), 0, 20));
        eq(100, e.health[far]);
    }
    private static void seededSpread() {
        Player a = player(), b = player();
        HitScan x = new HitScan(), y = new HitScan();
        x.setSeed(77);
        y.setSeed(77);
        x.fire(a, new EntityPool(1), space(false), Fixed.fromInt(20), 9, 1);
        y.fire(b, new EntityPool(1), space(false), Fixed.fromInt(20), 9, 1);
        eq(x.lastYaw, y.lastYaw);
        eq(x.lastPitch, y.lastPitch);
    }
    private static void magazineReloadAndJam() {
        CombatState c = new CombatState(1);
        c.magazine = 1;
        c.reserve = 3;
        ok(c.trigger());
        c.update(300);
        ok(c.state == CombatState.RELOADING);
        c.update(1200);
        eq(3, c.magazine);
        eq(0, c.reserve);
        CombatState j = new CombatState(1);
        j.durability = 0;
        for (int i = 0; i < 40 && j.state != CombatState.JAMMED; i++) {
            j.trigger();
            j.update(300);
        }
        eq(CombatState.JAMMED, j.state);
        j.clearJam();
        j.update(700);
        eq(CombatState.READY, j.state);
    }
    private static void damageAndEffects() {
        Player p = player();
        p.suit = ItemTypes.SUIT_LEATHER;
        p.armor = 20;
        int dealt = DamagePipeline.apply(p, DamagePipeline.PHYSICAL, 40);
        ok(dealt < 40);
        ok(p.bleeding > 0);
        DamagePipeline.apply(p, DamagePipeline.RADIATION, 50);
        ok(p.radiation < 50);
        int hp = p.health;
        p.update(1000, new Input(), space(false));
        ok(p.health < hp);
    }
    private static void stamina() {
        Player p = player();
        Input in = new Input();
        in.key('2', true, 0);
        in.update(500);
        int before = p.stamina;
        p.update(20, in, space(false));
        ok(p.stamina < before);
    }
    private static Player player() {
        Player p = new Player();
        p.reset(0, 0, 0);
        return p;
    }
    private static Collision space(boolean wall) {
        Collision c = new Collision(1, 1, 1);
        c.floor(0, 0, Fixed.fromInt(-20), Fixed.fromInt(20), Fixed.fromInt(-20), Fixed.fromInt(20),
                0);
        c.ceiling(0, 0, Fixed.fromInt(-20), Fixed.fromInt(20), Fixed.fromInt(-20),
                Fixed.fromInt(20), Fixed.fromInt(4));
        if (wall)
            c.edge(0, 0, Fixed.fromInt(2), -Fixed.ONE, Fixed.fromInt(2), Fixed.ONE, 0,
                    Fixed.fromInt(3));
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
