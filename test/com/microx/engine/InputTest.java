package com.microx.engine;
import com.microx.engine.math.Fixed;
import com.microx.engine.gameplay.ItemCatalog;
import com.microx.engine.world.Collision;
import com.microx.engine.world.Player;
public final class InputTest {
    public static void main(String[] args) {
        if (!ItemCatalog.loadDefault())
            throw new AssertionError("gameplay catalog unavailable");
        normalization();
        matchingRelease();
        heldMovement();
        System.out.println("InputTest: OK");
    }
    private static void normalization() {
        eq(Input.FORWARD, Input.gameplayBit('2', 0));
        eq(Input.FORWARD, Input.gameplayBit(-1, 1));
        eq(Input.BACK, Input.gameplayBit(-2, 6));
        eq(Input.LEFT, Input.gameplayBit(-3, 2));
        eq(Input.RIGHT, Input.gameplayBit(-4, 5));
        eq(Input.FIRE, Input.gameplayBit(-5, 8));
        eq(Input.PAUSE, Input.gameplayBit(-6, 0));
        eq(Input.PAUSE, Input.gameplayBit(-7, 0));
        eq(Input.BACK_CMD, Input.command(-6, 8));
        eq(Input.MENU, Input.command(-7, 8));
        eq(Input.FORWARD, Input.gameplayBit(2, 1)); // KEmulator action-bearing code
    }
    private static void matchingRelease() {
        Input in = new Input();
        in.key(-59, 2, true, 0);
        in.key(-59, 0, false, 20); // release action may be unavailable
        eq(0, in.down());
        in.key('2', 0, true, 40);
        in.key(-1, 1, true, 40);
        in.key('2', 0, false, 60);
        eq(Input.FORWARD, in.down());
        in.key(-1, 0, false, 80);
        eq(0, in.down());
    }
    private static void heldMovement() {
        checkHeld('2', true, false);
        checkHeld('8', true, false);
        checkHeld('4', false, true);
        checkHeld('6', false, true);
        checkHeld('1', true, false);
        checkHeld('3', true, false);
    }
    private static void checkHeld(int key, boolean moves, boolean turns) {
        Player p = new Player();
        p.reset(0, 0, 0);
        Input in = new Input();
        in.key(key, true, 0);
        for (int i = 0; i < 5; i++) p.update(20, in, space());
        if (moves != (p.x != 0 || p.z != 0) || turns != (p.yaw != 0))
            throw new AssertionError("held key " + key);
        int x = p.x, z = p.z, yaw = p.yaw;
        in.key(key, false, 100);
        p.update(20, in, space());
        eq(x, p.x);
        eq(z, p.z);
        eq(yaw, p.yaw);
    }
    private static Collision space() {
        Collision c = new Collision(0, 1, 1);
        c.floor(0, 0, -Fixed.fromInt(20), Fixed.fromInt(20), -Fixed.fromInt(20), Fixed.fromInt(20),
                0);
        c.ceiling(0, 0, -Fixed.fromInt(20), Fixed.fromInt(20), -Fixed.fromInt(20),
                Fixed.fromInt(20), Fixed.fromInt(4));
        return c;
    }
    private static void eq(int expected, int actual) {
        if (expected != actual)
            throw new AssertionError(expected + " != " + actual);
    }
}
