package com.microx.engine.combat;
import com.microx.engine.gameplay.ItemCatalog;
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
        if (ItemCatalog.valid(weapon))
            magazine = ItemCatalog.magazine(weapon);
    }
    public void equip(int item) {
        if (!ItemCatalog.valid(item) || ItemCatalog.type(item) != ItemCatalog.TYPE_WEAPON)
            return;
        weapon = item;
        magazine = ItemCatalog.magazine(item);
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
        int jam = durability < 60 ? (60 - durability) / 3 : 0;
        if (((seed >>> 16) & 255) < jam) {
            state = JAMMED;
            return false;
        }
        magazine--;
        durability -= ItemCatalog.durability(weapon);
        if (durability < 0)
            durability = 0;
        state = COOLDOWN;
        timer = ItemCatalog.cooldown(weapon);
        return true;
    }
    public void clearJam() {
        if (state == JAMMED) {
            state = CLEARING;
            timer = 700;
        }
    }
    public void startReload() {
        if (state == READY && magazine < ItemCatalog.magazine(weapon) && reserve > 0) {
            state = RELOADING;
            timer = ItemCatalog.reload(weapon);
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
                    int need = ItemCatalog.magazine(weapon) - magazine,
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
