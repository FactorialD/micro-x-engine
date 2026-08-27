package com.microx.engine.gameplay;

/** Isolated arena session with an entry fee, temporary equipment and atomic state restoration. */
public final class ArenaSystem {
    private final GameplayTables tables;
    private final GameplayState backup = new GameplayState();
    private boolean active;
    private int arena, wave, returnSpawn;
    private String returnLocation;
    public ArenaSystem(GameplayTables data) {
        tables = data;
    }
    public boolean enter(int id, GameplayState player, String location, int spawn) {
        int row = tables.find("arena", id), fee = tables.numberAt(row, "fee");
        if (active || row < 0 || player.inventory.money() < fee)
            return false;
        backup.copyPersistentFrom(player);
        backup.inventory.setMoney(backup.inventory.money() - fee);
        returnLocation = location;
        returnSpawn = spawn;
        player.inventory.clear();
        player.inventory.setMoney(backup.inventory.money());
        player.equipment.clear();
        int weapon = tables.numberAt(row, "weapon");
        if (!player.inventory.add(weapon, 1, 100)
                || !player.equipment.equip(player.inventory, weapon, 0)) {
            player.copyPersistentFrom(backup);
            return false;
        }
        arena = id;
        wave = 1;
        active = true;
        return true;
    }
    public boolean active() {
        return active;
    }
    public int wave() {
        return wave;
    }
    public int waves() {
        return tables.numberAt(tables.find("arena", arena), "waves");
    }
    /** Returns true once the last configured wave is cleared. */
    public boolean clearWave() {
        if (!active)
            return false;
        if (wave < waves()) {
            wave++;
            return false;
        }
        return true;
    }
    public boolean leave(GameplayState player, boolean victorious) {
        if (!active)
            return false;
        int arenaRow = tables.find("arena", arena);
        player.copyPersistentFrom(backup);
        if (victorious) {
            int reward = tables.numberAt(arenaRow, "reward");
            int rewardRow = tables.find("rewards", reward);
            player.inventory.setMoney(
                    player.inventory.money() + tables.numberAt(rewardRow, "money"));
            int item = tables.numberAt(rewardRow, "item"),
                amount = tables.numberAt(rewardRow, "amount");
            if (item > 0 && amount > 0)
                player.inventory.add(item, amount, 100);
        }
        active = false;
        return true;
    }
    public String returnLocation() {
        return returnLocation;
    }
    public int returnSpawn() {
        return returnSpawn;
    }
}
