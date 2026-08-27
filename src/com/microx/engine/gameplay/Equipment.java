package com.microx.engine.gameplay;

/** Two weapons, one armor and five artifact slots. */
public final class Equipment {
    private final short[] weapon = new short[2], artifact = new short[5];
    private short armor;
    public void clear() {
        armor = 0;
        for (int i = 0; i < weapon.length; i++) weapon[i] = 0;
        for (int i = 0; i < artifact.length; i++) artifact[i] = 0;
    }
    /** Save-game restore path; unlike equip this does not mutate the inventory. */
    public boolean restore(int kind, int slot, int id) {
        if (id != 0 && !ItemCatalog.valid(id))
            return false;
        if (kind == 0 && slot >= 0 && slot < weapon.length)
            weapon[slot] = (short) id;
        else if (kind == 1 && slot == 0)
            armor = (short) id;
        else if (kind == 2 && slot >= 0 && slot < artifact.length)
            artifact[slot] = (short) id;
        else
            return false;
        return true;
    }
    public int weapon(int slot) {
        return weapon[slot] & 65535;
    }
    public int armor() {
        return armor & 65535;
    }
    public int artifact(int slot) {
        return artifact[slot] & 65535;
    }
    public boolean equip(Inventory bag, int id, int slot) {
        if (!ItemCatalog.valid(id) || bag.count(id) < 1)
            return false;
        int type = ItemCatalog.type(id);
        if (type == ItemCatalog.TYPE_WEAPON) {
            if (slot < 0 || slot >= 2)
                return false;
            return swap(bag, weapon, slot, id);
        }
        if (type == ItemCatalog.TYPE_ARTIFACT) {
            if (slot < 0 || slot >= 5)
                return false;
            return swap(bag, artifact, slot, id);
        }
        if (type == ItemCatalog.TYPE_ARMOR && slot == 0) {
            if (armor != 0 && !bag.canAddAfterRemoving(armor, 1, id))
                return false;
            bag.remove(id, 1);
            if (armor != 0)
                bag.add(armor, 1, 100);
            armor = (short) id;
            return true;
        }
        return false;
    }
    private boolean swap(Inventory bag, short[] slots, int slot, int id) {
        int old = slots[slot] & 65535;
        if (old != 0 && !bag.canAddAfterRemoving(old, 1, id))
            return false;
        bag.remove(id, 1);
        if (old != 0)
            bag.add(old, 1, 100);
        slots[slot] = (short) id;
        return true;
    }
    public boolean use(Inventory bag, int id, PlayerStats stats) {
        if (!ItemCatalog.valid(id) || ItemCatalog.type(id) != ItemCatalog.TYPE_CONSUMABLE
                || bag.count(id) < 1)
            return false;
        bag.remove(id, 1);
        stats.health = Math.min(100, stats.health + ItemCatalog.health(id));
        stats.bleeding = Math.max(0, stats.bleeding - ItemCatalog.bleeding(id));
        return true;
    }
    public void apply(PlayerStats s) {
        s.physicalProtection = s.anomalyProtection = s.radiationProtection = 0;
        if (armor != 0)
            add(s, armor);
        for (int i = 0; i < artifact.length; i++)
            if (artifact[i] != 0)
                add(s, artifact[i]);
    }
    private void add(PlayerStats s, int id) {
        s.physicalProtection += ItemCatalog.physical(id);
        s.anomalyProtection += ItemCatalog.anomaly(id);
        s.radiationProtection += ItemCatalog.radiation(id);
    }
}
