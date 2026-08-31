package com.microx.engine.render;

import com.microx.engine.gameplay.GameIds;
import com.microx.engine.math.Fixed;
import com.microx.engine.world.EntityPool;

/** Central, deterministic entity primitive palette and dimensions. */
public final class EntityRenderCatalog {
    public static final int UNKNOWN_ITEM = 0x8b7f72, UNKNOWN_FACTION = 0x77808a,
                            UNKNOWN_MUTANT = 0x68734d;

    private EntityRenderCatalog() {}

    public static int color(EntityPool pool, int entity) {
        int type = pool.type[entity];
        if (type == EntityPool.HUMAN) {
            int color = faction(pool.faction[entity]);
            return pool.trader[entity] ? darken(color) : color;
        }
        if (type == EntityPool.MUTANT)
            return mutant(pool.archetype[entity]);
        if (type == EntityPool.DOOR)
            return 0x725239;
        if (type == EntityPool.CONTAINER)
            return pool.aux[entity] == EntityPool.PLAYER_STASH
                    ? 0x526b45
                    : (pool.aux[entity] == EntityPool.RANDOM_STASH ? 0x806a3f : 0x675642);
        if (type == EntityPool.ITEM) {
            switch (pool.aux[entity]) {
                case GameIds.ITEM_MEDKIT:
                    return 0xc94a4a;
                case GameIds.ITEM_BANDAGE:
                    return 0xe2ddd0;
                case GameIds.ITEM_STONE:
                    return 0x77756f;
                case GameIds.ITEM_CRYSTAL:
                    return 0x55c8d0;
                case GameIds.ITEM_AMMO_9MM:
                case GameIds.ITEM_AMMO_545:
                    return 0xb49a4e;
                default:
                    return UNKNOWN_ITEM;
            }
        }
        if (type == EntityPool.ANOMALY)
            return 0x7850a0;
        if (type == EntityPool.CORPSE)
            return 0x493f3b;
        return UNKNOWN_ITEM;
    }

    public static int halfWidth(EntityPool pool, int entity) {
        int radius = pool.radius[entity] > 0 ? pool.radius[entity] : Fixed.ONE / 4;
        if (pool.type[entity] == EntityPool.DOOR)
            return Fixed.mul(radius, Fixed.fromRatio(3, 2));
        if (pool.type[entity] == EntityPool.ITEM)
            return Fixed.mul(radius,
                    pool.aux[entity] == GameIds.ITEM_RIFLE ? Fixed.fromInt(2)
                                                           : Fixed.fromRatio(3, 4));
        return radius;
    }

    public static int height(EntityPool pool, int entity) {
        int radius = pool.radius[entity] > 0 ? pool.radius[entity] : Fixed.ONE / 4;
        if (pool.type[entity] == EntityPool.HUMAN)
            return Fixed.mul(radius, Fixed.fromInt(7));
        if (pool.type[entity] == EntityPool.DOOR)
            return Fixed.mul(radius, Fixed.fromInt(8));
        if (pool.type[entity] == EntityPool.CONTAINER)
            return Fixed.mul(radius, Fixed.fromInt(3));
        if (pool.type[entity] == EntityPool.ITEM)
            return Fixed.mul(radius, Fixed.fromRatio(3, 2));
        return radius * 2;
    }

    private static int faction(int faction) {
        switch (faction) {
            case GameIds.FACTION_LONER:
                return 0x80915c;
            case GameIds.FACTION_BANDIT:
                return 0x555b61;
            case GameIds.FACTION_DUTY:
                return 0x9b493f;
            case GameIds.FACTION_FREEDOM:
                return 0x4d8655;
            default:
                return UNKNOWN_FACTION;
        }
    }

    private static int mutant(int archetype) {
        switch (archetype) {
            case EntityPool.MUTANT_BASIC:
                return 0x75834c;
            case EntityPool.MUTANT_BLOODSUCKER:
                return 0x7f3738;
            case EntityPool.MUTANT_LEAPER:
                return 0x9a7545;
            case EntityPool.MUTANT_PSI:
                return 0x66528f;
            case EntityPool.MUTANT_AOE:
                return 0x497d72;
            default:
                return UNKNOWN_MUTANT;
        }
    }

    /** Darkens RGB while preserving an optional ARGB alpha byte. */
    public static int darken(int color) {
        int alpha = color & 0xff000000;
        return alpha | ((color & 0xff0000) * 3 / 4 & 0xff0000)
                | ((color & 0x00ff00) * 3 / 4 & 0x00ff00) | ((color & 0x0000ff) * 3 / 4 & 0x0000ff);
    }
}
