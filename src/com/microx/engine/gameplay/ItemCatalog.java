package com.microx.engine.gameplay;

/** Process-wide, validated runtime item catalog. Values originate only in gameplay.dat. */
public final class ItemCatalog {
    public static final byte TYPE_WEAPON = 1, TYPE_ARMOR = 2, TYPE_CONSUMABLE = 3,
                             TYPE_ARTIFACT = 4, TYPE_AMMO = 5;
    private static GameplayTables data;
    private ItemCatalog() {}
    public static boolean install(GameplayTables tables) {
        if (tables == null || !tables.hasRequiredData())
            return false;
        data = tables;
        return true;
    }
    public static boolean loadDefault() {
        GameplayTables tables = new GameplayTables();
        return tables.load("/data/gameplay.dat") && install(tables);
    }
    private static GameplayTables get() {
        if (data == null)
            throw new IllegalStateException("item catalog not loaded");
        return data;
    }
    public static boolean valid(int id) {
        return data != null && data.validItem(id);
    }
    public static int maxId() {
        return get().maxItemId();
    }
    public static int type(int id) {
        return get().itemType(id);
    }
    public static int cells(int id) {
        return get().cells(id);
    }
    public static int stack(int id) {
        return get().stack(id);
    }
    public static int value(int id) {
        return get().value(id);
    }
    public static int health(int id) {
        return get().health(id);
    }
    public static int bleeding(int id) {
        return get().bleeding(id);
    }
    public static int physical(int id) {
        return id == 0 ? 0 : get().physical(id);
    }
    public static int anomaly(int id) {
        return id == 0 ? 0 : get().anomaly(id);
    }
    public static int radiation(int id) {
        return id == 0 ? 0 : get().radiation(id);
    }
    public static int ammo(int id) {
        return get().ammo(id);
    }
    public static int magazine(int id) {
        return get().magazine(id);
    }
    public static int damage(int id) {
        return get().damage(id);
    }
    public static int range(int id) {
        return get().range(id);
    }
    public static int cooldown(int id) {
        return get().cooldown(id);
    }
    public static int reload(int id) {
        return get().reload(id);
    }
    public static int spread(int id) {
        return get().spread(id);
    }
    public static int durability(int id) {
        return get().durability(id);
    }
    public static int damageBonus(int id) {
        return get().damageBonus(id);
    }
}
