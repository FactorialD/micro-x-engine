package com.microx.engine.gameplay;

/** Resolves NPC dialogue chains and their quest predicates from gameplay tables. */
public final class DialogueSystem {
    public static final int NONE = 0, QUEST = 1, TRADE = 2, REPAIR = 3;
    private DialogueSystem() {}
    public static int available(
            GameplayTables tables, GameplayState state, int npc, short[] output) {
        int dialog = tables.npcDialog(npc), count = 0, steps = 0;
        int[] questOccurrences = new int[33];
        while (dialog > 0 && steps++ < 32 && count < output.length) {
            String refTable = tables.dialogRefTable(dialog);
            int ref = tables.dialogRef(dialog);
            boolean allowed = false;
            if ("quests".equals(refTable) && ref > 0 && ref < questOccurrences.length) {
                int occurrence = questOccurrences[ref]++;
                int prerequisite = tables.questRequires(ref);
                boolean prerequisiteMet = prerequisite == 0
                        || state.quests.state(prerequisite) == QuestState.COMPLETE;
                int questState = state.quests.state(ref);
                allowed = tables.questRef(ref) == npc && prerequisiteMet
                        && ((questState == QuestState.LOCKED && occurrence == 0)
                                || (questState == QuestState.ACTIVE && occurrence == 1
                                        && state.quests.counter(0) >= 1));
            } else if ("npcs".equals(refTable))
                allowed = ref == npc;
            if (allowed)
                output[count++] = (short) dialog;
            dialog = tables.dialogNext(dialog);
        }
        return count;
    }
    public static int select(GameplayTables tables, GameplayState state, int npc, int dialog) {
        String refTable = tables.dialogRefTable(dialog);
        int ref = tables.dialogRef(dialog);
        if ("quests".equals(refTable) && tables.questRef(ref) == npc) {
            if (ref == GameIds.QUEST_FIND_STASH) {
                if (state.quests.state(ref) == QuestState.LOCKED)
                    return state.acceptFindStash() ? QUEST : NONE;
                return state.rewardFindStash() ? QUEST : NONE;
            }
            return NONE;
        }
        if ("npcs".equals(refTable) && ref == npc)
            return npc == GameIds.NPC_TECHNICIAN ? REPAIR : TRADE;
        return NONE;
    }
}
