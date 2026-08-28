package com.microx.engine.combat;
import com.microx.engine.gameplay.GameIds;
/** Stable IDs retained for save compatibility; balance lives in the runtime catalog. */
public final class ItemTypes {
    public static final int PISTOL = GameIds.ITEM_PISTOL, RIFLE = GameIds.ITEM_RIFLE;
    public static final int SUIT_NONE = 0, SUIT_LEATHER = GameIds.ITEM_LEATHER_ARMOR;
    public static final int ARTIFACT_NONE = 0, ARTIFACT_STONE = GameIds.ITEM_STONE,
                            ARTIFACT_CRYSTAL = GameIds.ITEM_CRYSTAL;
    private ItemTypes() {}
}
