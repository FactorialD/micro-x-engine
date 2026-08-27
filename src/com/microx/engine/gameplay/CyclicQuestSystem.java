package com.microx.engine.gameplay;

/** Deterministic selector for repeatable quest templates; its cursor is persisted in QuestState. */
public final class CyclicQuestSystem {
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
    public void complete() {
        state.setCyclicSeed(state.cyclicSeed() + 1);
    }
}
