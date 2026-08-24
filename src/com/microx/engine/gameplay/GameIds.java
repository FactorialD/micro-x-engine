package com.microx.engine.gameplay;

/** Stable save-game and data-file identifiers. Never renumber released values. */
public final class GameIds {
    public static final int ITEM_PISTOL = 1, ITEM_RIFLE = 2, ITEM_LEATHER_ARMOR = 3,
                            ITEM_MEDKIT = 4, ITEM_BANDAGE = 5, ITEM_STONE = 6, ITEM_CRYSTAL = 7,
                            ITEM_AMMO_9MM = 8, ITEM_AMMO_545 = 9;
    public static final int NPC_SIDOROVICH = 1, NPC_WOLF = 2, NPC_TECHNICIAN = 3;
    public static final int FACTION_LONER = 1, FACTION_BANDIT = 2, FACTION_DUTY = 3,
                            FACTION_FREEDOM = 4;
    public static final int DIALOG_INTRO = 1, DIALOG_TRADE = 2, DIALOG_REPAIR = 3;
    public static final int QUEST_FIND_STASH = 1, QUEST_REPORT_WOLF = 2;
    private GameIds() {}
}
