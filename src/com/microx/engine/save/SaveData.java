package com.microx.engine.save;
import com.microx.engine.gameplay.GameplayState;

/** Fixed-size mutable save snapshot. Strings and sparse arrays are deliberately bounded for CLDC. */
public final class SaveData {
    public static final int MAX_LOCATION = 31, MAX_ENTITY_DELTAS = 64;
    private static final String[] LOCATIONS = {"cordon", "garbage", "depot", "laboratory", "arena",
            "agroprom", "bar", "wild_territory", "yantar", "army_warehouses", "radar", "pripyat",
            "cnpp"};
    public final GameplayState gameplay = new GameplayState();
    public int slot, sequence;
    public long savedAt;
    public int seed;
    public String location = "cordon";
    /** Runtime-only marker set when SaveStore had to skip a newer committed transaction. */
    public boolean recovered;
    public int spawn;
    public int x, y, z, yaw, pitch, health, armor, stamina, bleeding, radiation, weapon, magazine;
    /** Reserve ammunition by runtime weapon/ammunition family. */
    public final int[] reserveAmmo = new int[8];
    public int entityCount;
    public final int[] entityId = new int[MAX_ENTITY_DELTAS],
                       entityFlags = new int[MAX_ENTITY_DELTAS];
    public void clearEntities() {
        entityCount = 0;
    }
    public boolean addEntityDelta(int id, int flags) {
        if (id <= 0 || entityCount >= MAX_ENTITY_DELTAS)
            return false;
        entityId[entityCount] = id;
        entityFlags[entityCount++] = flags;
        return true;
    }
    public static boolean validLocation(String name) {
        if (name == null)
            return false;
        for (int i = 0; i < LOCATIONS.length; i++)
            if (LOCATIONS[i].equals(name))
                return true;
        return false;
    }
    /** Spawn ids are authored contracts, not array indexes. */
    public static boolean validSpawn(String location, int spawn) {
        if (spawn == 0)
            return validLocation(location);
        if ("cordon".equals(location))
            return spawn == 20 || spawn == 80 || spawn == 130;
        if ("garbage".equals(location))
            return spawn == 10 || spawn == 30 || spawn == 40;
        if ("depot".equals(location))
            return spawn == 20 || spawn == 30 || spawn == 50;
        if ("laboratory".equals(location))
            return spawn == 10 || spawn == 70 || spawn == 90;
        if ("arena".equals(location))
            return spawn == 10 || spawn == 120;
        if ("agroprom".equals(location))
            return spawn == 20 || spawn == 40;
        if ("bar".equals(location))
            return spawn == 40 || spawn == 60;
        if ("wild_territory".equals(location))
            return spawn == 50 || spawn == 70;
        if ("yantar".equals(location))
            return spawn == 60 || spawn == 80;
        if ("army_warehouses".equals(location))
            return spawn == 80 || spawn == 100;
        if ("radar".equals(location))
            return spawn == 90 || spawn == 110;
        if ("pripyat".equals(location))
            return spawn == 100 || spawn == 120;
        if ("cnpp".equals(location))
            return spawn == 110 || spawn == 130;
        return false;
    }
}
