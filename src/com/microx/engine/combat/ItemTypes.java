package com.microx.engine.combat;

/** Compact immutable type tables. Inventory entries store only a table id and count. */
public final class ItemTypes {
 public static final int PISTOL=0,RIFLE=1,SHOTGUN=2;
 public static final int BULLET_9MM=0,BULLET_545=1,SHELL_12G=2;
 public static final int SUIT_NONE=0,SUIT_LEATHER=1,SUIT_STALKER=2;
 public static final int MEDKIT=0,BANDAGE=1,ANTIRAD=2;
 public static final int ARTIFACT_NONE=0,ARTIFACT_STONE=1,ARTIFACT_CRYSTAL=2;

 public static final byte[] WEAPON_AMMO={BULLET_9MM,BULLET_545,SHELL_12G};
 public static final byte[] WEAPON_MAGAZINE={12,30,6};
 public static final short[] WEAPON_DAMAGE={24,31,55};
 public static final short[] WEAPON_RANGE={24,40,18};
 public static final short[] WEAPON_COOLDOWN_MS={260,100,700};
 public static final short[] WEAPON_RELOAD_MS={1200,1700,2100};
 public static final byte[] WEAPON_SPREAD={3,2,5};
 public static final byte[] WEAPON_DURABILITY_COST={1,1,2};
 public static final byte[] AMMO_DAMAGE_BONUS={0,2,5};
 public static final byte[] SUIT_PHYSICAL={0,25,45};
 public static final byte[] SUIT_ANOMALY={0,10,30};
 public static final byte[] SUIT_RADIATION={0,5,40};
 public static final byte[] CONSUMABLE_HEALTH={60,0,0};
 public static final byte[] CONSUMABLE_BLEEDING={0,60,0};
 public static final byte[] CONSUMABLE_RADIATION={0,0,70};
 public static final byte[] ARTIFACT_PHYSICAL={0,5,0};
 public static final byte[] ARTIFACT_ANOMALY={0,20,5};
 public static final byte[] ARTIFACT_RADIATION={0,0,25};
 private ItemTypes(){}
}
