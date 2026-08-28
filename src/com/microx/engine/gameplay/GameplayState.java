package com.microx.engine.gameplay;

/** Explicit memory budget for the complete persistent RPG state. */
public final class GameplayState {
    public final Inventory inventory = new Inventory(32, 48);
    public final Equipment equipment = new Equipment();
    public final Reputation reputation = new Reputation(8);
    public final QuestState quests = new QuestState(32, 4, 32);
    public final ContainerDelta containers = new ContainerDelta(128);
    /** Bounded working inventories backing the currently open trade/loot screens. */
    public final Inventory trader = new Inventory(24, 40);
    public final Inventory loot = new Inventory(24, 40);
    public int actorId, containerId, traderActorId;

    public boolean acceptFindStash() {
        return quests.transition(GameIds.QUEST_FIND_STASH, 0, 1, 0, 0, -1, -1, 0, 1);
    }
    public boolean foundStash(int stableId) {
        if (stableId != GameIds.CONTAINER_FIND_STASH
                || quests.state(GameIds.QUEST_FIND_STASH) != QuestState.ACTIVE || quests.flag(0))
            return false;
        quests.setFlag(0, true);
        quests.addCounter(0, 1);
        return true;
    }
    public boolean rewardFindStash() {
        if (!quests.dialogAllowed(GameIds.QUEST_FIND_STASH, 1, -1, 0, 1))
            return false;
        if (!inventory.add(GameIds.ITEM_MEDKIT, 1, 100))
            return false;
        inventory.setMoney(inventory.money() + 500);
        reputation.add(GameIds.FACTION_LONER, 10);
        return quests.transition(GameIds.QUEST_FIND_STASH, 1, 2, 0, 0, -1, 0, 1, 0);
    }
    public void copyPersistentFrom(GameplayState source) {
        inventory.clear();
        inventory.setMoney(source.inventory.money());
        for (int i = 0; i < source.inventory.slots(); i++)
            if (source.inventory.idAt(i) != 0)
                inventory.add(source.inventory.idAt(i), source.inventory.countAt(i),
                        source.inventory.durabilityAt(i));
        equipment.clear();
        equipment.restore(0, 0, source.equipment.weapon(0));
        equipment.restore(0, 1, source.equipment.weapon(1));
        equipment.restore(1, 0, source.equipment.armor());
        for (int i = 0; i < 5; i++) equipment.restore(2, i, source.equipment.artifact(i));
        quests.copyFrom(source.quests);
        for (int i = 1; i <= reputation.size(); i++) reputation.set(i, source.reputation.get(i));
        containers.clear();
        for (int i = 0; i < source.containers.initializedCount(); i++)
            containers.markInitialized(source.containers.initializedAt(i));
        for (int i = 0; i < source.containers.capacity(); i++)
            if (source.containers.occupied(i))
                containers.restoreRecord(source.containers.containerAt(i),
                        source.containers.itemAt(i), source.containers.deltaAt(i),
                        source.containers.durabilityAt(i));
    }
}
