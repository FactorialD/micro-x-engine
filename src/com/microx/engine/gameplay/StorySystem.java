package com.microx.engine.gameplay;

/** Allocation-free director for the packed main-story graph and its ending/freeplay hand-off. */
public final class StorySystem {
    private final GameplayTables tables;
    private final QuestState state;
    public StorySystem(GameplayTables data, QuestState quests) {
        tables = data;
        state = quests;
    }
    public boolean start() {
        if (state.storyNode() != 0)
            return false;
        int entry = tables.storyEntry();
        if (entry == 0)
            return false;
        state.setStoryNode(entry);
        applyObjective(entry);
        return true;
    }
    public int node() {
        return state.storyNode();
    }
    public String text() {
        int row = tables.find("story_nodes", node());
        return row < 0 ? null : tables.text(row);
    }
    public int scene() {
        return tables.numberAt(tables.find("story_nodes", node()), "scene");
    }
    public int ending() {
        return state.ending();
    }
    public boolean choose(boolean alternate) {
        int row = tables.find("story_nodes", node());
        if (row < 0 || state.ending() != 0)
            return false;
        int ending = tables.numberAt(row, "ending");
        if (ending > 0) {
            state.setEnding(ending);
            state.setObjective(-1);
            return true;
        }
        int next = tables.numberAt(row, alternate ? "alt" : "next");
        if (next == 0 && alternate)
            next = tables.numberAt(row, "next");
        if (tables.find("story_nodes", next) < 0)
            return false;
        state.setStoryNode(next);
        applyObjective(next);
        return true;
    }
    public boolean enterFreeplay(int endingId) {
        int row = tables.find("endings", endingId);
        if (row < 0 || tables.numberAt(row, "freeplay") != 1 || state.ending() == 0)
            return false;
        state.setFreeplay(true);
        state.setStoryNode(0);
        state.setObjective(-1);
        return true;
    }
    private void applyObjective(int node) {
        int row = tables.find("story_nodes", node);
        int objective = tables.numberAt(row, "objective");
        int objectiveRow = tables.find("objectives", objective);
        state.setObjective(objectiveRow < 0 ? -1 : tables.numberAt(objectiveRow, "marker"));
    }
}
