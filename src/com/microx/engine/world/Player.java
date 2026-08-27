package com.microx.engine.world;
import com.microx.engine.Input;
import com.microx.engine.combat.*;
import com.microx.engine.math.Fixed;
public final class Player {
    public static final int RADIUS = Fixed.ONE / 4, STANDING_HEIGHT = Fixed.fromInt(2),
                            CROUCH_HEIGHT = Fixed.ONE, STEP_HEIGHT = Fixed.ONE / 2;
    /** Authoritative runtime vitals and equipment-derived protections. */
    public int x, y, z, vx, vy, vz, yaw, pitch, health = 100, stamina = 100, bleeding, radiation,
                                                physicalProtection, anomalyProtection,
                                                radiationProtection;
    public int bleedTimer, radiationTimer, slowTimer;
    public int faction = 1;
    public boolean grounded = true, crouched, aiming, sprinting, detectorActive;
    public final CombatState combat = new CombatState();
    public final int[] reserveAmmo = new int[8];
    /** Compatibility mirror; combat.magazine is authoritative. */ public int ammo = 30;
    public void reset(int px, int py, int pz) {
        x = px;
        y = py;
        z = pz;
        vx = vy = vz = 0;
        yaw = pitch = 0;
        health = 100;
        stamina = 100;
        bleeding = radiation = bleedTimer = radiationTimer = slowTimer = 0;
        physicalProtection = anomalyProtection = radiationProtection = 0;
        grounded = true;
        crouched = aiming = sprinting = detectorActive = false;
        combat.equip(ItemTypes.PISTOL);
        for (int i = 0; i < reserveAmmo.length; i++) reserveAmmo[i] = 0;
        ammo = combat.magazine;
    }
    public void update(int dt, Input in, Collision c) {
        combat.update(dt);
        ammo = combat.magazine;
        if (slowTimer > 0)
            slowTimer = Math.max(0, slowTimer - dt);
        int keys = in.down(), speed = slowTimer > 0 ? Fixed.fromInt(3) / 2 : Fixed.fromInt(3);
        sprinting = ((in.doubleTapped() | in.held()) & Input.FORWARD) != 0 && !crouched && !aiming
                && stamina > 0;
        if (sprinting) {
            speed = Fixed.fromInt(5);
            stamina -= Math.max(1, dt / 40);
        } else if (stamina < 100)
            stamina += Math.max(1, dt / 200);
        int f = 0, s = 0;
        if ((keys & Input.FORWARD) != 0)
            f++;
        if ((keys & Input.BACK) != 0)
            f--;
        if ((keys & Input.STRAFE_RIGHT) != 0)
            s++;
        if ((keys & Input.STRAFE_LEFT) != 0)
            s--;
        int turn = 90 * dt / 1000;
        if ((keys & Input.LEFT) != 0)
            yaw -= turn;
        if ((keys & Input.RIGHT) != 0)
            yaw += turn;
        int step = Fixed.mul(speed, Fixed.div(Fixed.fromInt(dt), Fixed.fromInt(1000)));
        vx = Fixed.mul(Fixed.cos(yaw), f * step) - Fixed.mul(Fixed.sin(yaw), s * step);
        vz = Fixed.mul(Fixed.sin(yaw), f * step) + Fixed.mul(Fixed.cos(yaw), s * step);
        boolean wantsCrouch = (keys & Input.CROUCH) != 0;
        if (!wantsCrouch && crouched && !c.hasClearance(x, y, z, STANDING_HEIGHT))
            wantsCrouch = true;
        crouched = wantsCrouch;
        int height = crouched ? CROUCH_HEIGHT : STANDING_HEIGHT;
        c.sweep(x, y, z, vx, vz, RADIUS, height, STEP_HEIGHT);
        x = c.resultX;
        z = c.resultZ;
        int floor = c.resultFloor;
        if ((in.pressed() & Input.JUMP) != 0 && grounded && stamina >= 10) {
            vy = Fixed.fromInt(5);
            grounded = false;
            stamina -= 10;
        }
        int seconds = Fixed.div(Fixed.fromInt(dt), Fixed.fromInt(1000));
        if (!grounded) {
            y += Fixed.mul(vy, seconds);
            vy -= Fixed.mul(Fixed.fromInt(10), seconds);
        }
        if (floor != Integer.MIN_VALUE && y <= floor) {
            y = floor;
            vy = 0;
            grounded = true;
        } else if (floor == Integer.MIN_VALUE || y > floor)
            grounded = false;
        int ceiling = c.resultCeiling;
        if (ceiling != Integer.MAX_VALUE && y + height > ceiling) {
            y = ceiling - height;
            if (vy > 0)
                vy = 0;
        }
        updateEffects(dt);
        stamina = Fixed.clamp(stamina, 0, 100);
    }
    public void slow(int duration) {
        if (duration > slowTimer)
            slowTimer = duration;
    }
    private void updateEffects(int dt) {
        if (bleeding > 0) {
            bleedTimer += dt;
            while (bleedTimer >= 1000) {
                bleedTimer -= 1000;
                health -= 1;
                bleeding--;
            }
        }
        if (radiation > 0) {
            radiationTimer += dt;
            while (radiationTimer >= 1000) {
                radiationTimer -= 1000;
                health -= Math.max(1, radiation / 25);
            }
        }
        if (health < 0)
            health = 0;
    }
}
