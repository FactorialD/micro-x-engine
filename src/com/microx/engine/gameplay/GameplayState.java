package com.microx.engine.gameplay;

/** Explicit memory budget for the complete persistent RPG state. */
public final class GameplayState {
    public final Inventory inventory = new Inventory(32, 48);
    public final Equipment equipment = new Equipment();
    public final Reputation reputation = new Reputation(8);
    public final QuestState quests = new QuestState(32, 4, 32);
    public final ContainerDelta containers = new ContainerDelta(64);
    public final PlayerStats stats = new PlayerStats();
}
