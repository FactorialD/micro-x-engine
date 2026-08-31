package com.microx.engine.world;

/** Fixed structure-of-arrays store. No entity object is allocated while playing. */
public final class EntityPool {
    public static final int HUMAN = 1, MUTANT = 2, ITEM = 3, ANOMALY = 4, CORPSE = 5, DOOR = 6,
                            CONTAINER = 7;
    /** Explicit aux values for type 7; it is never an abstract quest marker. */
    public static final int PLAYER_STASH = 1, RANDOM_STASH = 2, FIXED_CONTAINER = 3;
    public static final int MAX_LIVE_NPCS = 24, MAX_MUTANTS = 24, MAX_ITEMS = 48,
                            MAX_ANOMALIES = 16, MAX_CORPSES = 24;
    public static final int STATE_IDLE = 0, STATE_ALERT = 1, STATE_COMBAT = 2, STATE_MELEE = 3,
                            STATE_LEAP = 4, STATE_INVISIBLE = 5, STATE_PSI = 6, STATE_AOE = 7;
    public static final int FLAG_VISIBLE = 1, FLAG_PERCEIVES_PLAYER = 2, FLAG_INTERACTABLE = 4,
                            FLAG_DEAD = 8, FLAG_UPDATE = 16;
    public static final int MUTANT_BASIC = 0, MUTANT_BLOODSUCKER = 1, MUTANT_LEAPER = 2,
                            MUTANT_PSI = 3, MUTANT_AOE = 4;
    public final int[] stableId, x, y, z, type, health, state, roomId, direction, radius, faction,
            timer, target, flags, spriteId, aux, archetype;
    public final boolean[] active, trader;
    private int count;
    private final int[] kinds = new int[8];
    public EntityPool(int capacity) {
        stableId = new int[capacity];
        x = new int[capacity];
        y = new int[capacity];
        z = new int[capacity];
        type = new int[capacity];
        health = new int[capacity];
        state = new int[capacity];
        roomId = new int[capacity];
        direction = new int[capacity];
        radius = new int[capacity];
        faction = new int[capacity];
        timer = new int[capacity];
        target = new int[capacity];
        flags = new int[capacity];
        spriteId = new int[capacity];
        aux = new int[capacity];
        archetype = new int[capacity];
        active = new boolean[capacity];
        trader = new boolean[capacity];
    }
    public int spawn(int t, int px, int py, int pz, int hp) {
        return spawn(0, t, px, py, pz, hp);
    }
    public int spawn(int id, int t, int px, int py, int pz, int hp) {
        if (id < 0 || (id != 0 && findStable(id) >= 0))
            return -1;
        if (limitReached(t))
            return -1;
        for (int i = 0; i < active.length; i++)
            if (!active[i]) {
                active[i] = true;
                stableId[i] = id;
                type[i] = t;
                x[i] = px;
                y[i] = py;
                z[i] = pz;
                health[i] = hp;
                state[i] = STATE_IDLE;
                roomId[i] = -1;
                direction[i] = 0;
                radius[i] = 16384;
                faction[i] = 0;
                timer[i] = 0;
                target[i] = -1;
                flags[i] = FLAG_UPDATE | (t >= ITEM ? FLAG_INTERACTABLE : 0);
                spriteId[i] = t;
                aux[i] = 0;
                archetype[i] = MUTANT_BASIC;
                trader[i] = false;
                count++;
                if (t < kinds.length)
                    kinds[t]++;
                return i;
            }
        return -1;
    }
    public int findStable(int id) {
        if (id <= 0)
            return -1;
        for (int i = 0; i < active.length; i++)
            if (active[i] && stableId[i] == id)
                return i;
        return -1;
    }
    public int findStableAny(int id) {
        if (id <= 0)
            return -1;
        for (int i = 0; i < active.length; i++)
            if (stableId[i] == id)
                return i;
        return -1;
    }
    private boolean limitReached(int t) {
        int max = t == HUMAN   ? MAX_LIVE_NPCS
                : t == MUTANT  ? MAX_MUTANTS
                : t == ITEM    ? MAX_ITEMS
                : t == ANOMALY ? MAX_ANOMALIES
                : t == CORPSE  ? MAX_CORPSES
                               : active.length;
        return t < kinds.length && kinds[t] >= max;
    }
    public void remove(int i) {
        if (i >= 0 && i < active.length && active[i]) {
            active[i] = false;
            if (type[i] < kinds.length)
                kinds[type[i]]--;
            count--;
        }
    }
    public int activeCount() {
        return count;
    }
    public int capacity() {
        return active.length;
    }
    public int typeCount(int t) {
        return t >= 0 && t < kinds.length ? kinds[t] : 0;
    }
    /** Keeps the stable entity and its metadata so corpse loot can survive save/load. */
    public boolean killToCorpse(int i) {
        if (i < 0 || i >= active.length || !active[i] || (type[i] != HUMAN && type[i] != MUTANT))
            return false;
        kinds[type[i]]--;
        type[i] = CORPSE;
        kinds[CORPSE]++;
        health[i] = 0;
        state[i] = STATE_IDLE;
        flags[i] = FLAG_DEAD | FLAG_INTERACTABLE | FLAG_VISIBLE;
        timer[i] = 0;
        return true;
    }
    public void restoreType(int i, int restored) {
        if (i >= 0 && i < active.length && active[i] && restored > 0 && restored < kinds.length
                && type[i] != restored) {
            kinds[type[i]]--;
            type[i] = restored;
            kinds[restored]++;
        }
    }
}
