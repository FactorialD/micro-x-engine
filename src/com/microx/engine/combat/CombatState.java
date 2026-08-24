package com.microx.engine.combat;

/** Allocation-free weapon state machine, advanced only by fixed simulation steps. */
public final class CombatState {
    public static final int READY = 0, COOLDOWN = 1, RELOADING = 2, JAMMED = 3, CLEARING = 4;
    public int weapon = ItemTypes.PISTOL, magazine, reserve = 48, durability = 100, state = READY,
               timer;
    private int seed;
    public CombatState() {
        this(0x51f15e);
    }
    public CombatState(int rngSeed) {
        seed = rngSeed;
        magazine = ItemTypes.WEAPON_MAGAZINE[weapon];
    }
    public void equip(int type) {
        if (type < 0 || type >= ItemTypes.WEAPON_MAGAZINE.length)
            return;
        weapon = type;
        magazine = ItemTypes.WEAPON_MAGAZINE[type];
        state = READY;
        timer = 0;
    }
    public boolean trigger() {
        if (state != READY)
            return false;
        if (magazine <= 0) {
            startReload();
            return false;
        }
        seed = seed * 1103515245 + 12345;
        int jamChance = durability < 60 ? (60 - durability) / 3 : 0;
        if (((seed >>> 16) & 255) < jamChance) {
            state = JAMMED;
            return false;
        }
        magazine--;
        durability -= ItemTypes.WEAPON_DURABILITY_COST[weapon];
        if (durability < 0)
            durability = 0;
        state = COOLDOWN;
        timer = ItemTypes.WEAPON_COOLDOWN_MS[weapon];
        return true;
    }
    public void clearJam() {
        if (state == JAMMED) {
            state = CLEARING;
            timer = 700;
        }
    }
    public void startReload() {
        if (state == READY && magazine < ItemTypes.WEAPON_MAGAZINE[weapon] && reserve > 0) {
            state = RELOADING;
            timer = ItemTypes.WEAPON_RELOAD_MS[weapon];
        }
    }
    public void update(int ms) {
        if (state == COOLDOWN || state == RELOADING || state == CLEARING) {
            timer -= ms;
            if (timer <= 0) {
                int old = state;
                state = READY;
                timer = 0;
                if (old == RELOADING) {
                    int need = ItemTypes.WEAPON_MAGAZINE[weapon] - magazine,
                        take = need < reserve ? need : reserve;
                    magazine += take;
                    reserve -= take;
                }
            }
        }
        if (state == READY && magazine == 0 && reserve > 0)
            startReload();
    }
}
