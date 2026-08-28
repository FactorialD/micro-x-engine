package com.microx.engine.gameplay;

/** Deterministic selector for repeatable quest templates; its cursor is persisted in QuestState. */
public final class CyclicQuestSystem {
    private static final int ACTIVE_COUNTER = 30, AVAILABLE_COUNTER = 31;
    private final GameplayTables tables;
    private final QuestState state;
    public CyclicQuestSystem(GameplayTables data, QuestState quests) {
        tables = data;
        state = quests;
    }
    public int current() {
        int size = tables.tableSize("cyclic_quests");
        if (size == 0)
            return 0;
        int seed = state.cyclicSeed();
        if (seed < 0)
            seed = -seed;
        return tables.id(tables.tableRow("cyclic_quests", seed % size));
    }
    public String text() {
        int row = tables.find("cyclic_quests", current());
        return row < 0 ? null : tables.text(row);
    }
    public int reward() {
        return tables.numberAt(tables.find("cyclic_quests", current()), "reward");
    }
    /** Issues the current template once its data-driven cooldown has elapsed. */
    public boolean issue(int cycle) {
        if (state.counter(ACTIVE_COUNTER) != 0 || cycle < state.counter(AVAILABLE_COUNTER))
            return false;
        state.restoreCounter(ACTIVE_COUNTER, 1);
        return current() != 0;
    }
    public boolean active() {
        return state.counter(ACTIVE_COUNTER) != 0;
    }
    public int cooldownRemaining(int cycle) {
        int left = state.counter(AVAILABLE_COUNTER) - cycle;
        return left > 0 ? left : 0;
    }
    public boolean complete(int cycle) {
        if (!active())
            return false;
        int row = tables.find("cyclic_quests", current());
        state.setCyclicSeed(state.cyclicSeed() + 1);
        state.restoreCounter(ACTIVE_COUNTER, 0);
        state.restoreCounter(
                AVAILABLE_COUNTER, Math.min(32767, cycle + tables.numberAt(row, "cooldown")));
        return true;
    }
    public void complete() {
        state.setCyclicSeed(state.cyclicSeed() + 1);
    }
}
