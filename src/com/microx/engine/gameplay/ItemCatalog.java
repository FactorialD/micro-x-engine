package com.microx.engine.gameplay;

/** Compact tables indexed by stable item id. Zero is deliberately invalid. */
public final class ItemCatalog {
 public static final byte TYPE_WEAPON=1,TYPE_ARMOR=2,TYPE_CONSUMABLE=3,TYPE_ARTIFACT=4,TYPE_AMMO=5;
 public static final int MAX_ID=9;
 public static final byte[] TYPE={0,TYPE_WEAPON,TYPE_WEAPON,TYPE_ARMOR,TYPE_CONSUMABLE,TYPE_CONSUMABLE,TYPE_ARTIFACT,TYPE_ARTIFACT,TYPE_AMMO,TYPE_AMMO};
 public static final byte[] CELLS={0,3,5,6,1,1,1,1,1,1};
 public static final short[] STACK={0,1,1,1,5,10,1,1,120,120};
 public static final short[] VALUE={0,500,2400,1800,300,80,900,1500,2,4};
 public static final byte[] HEALTH={0,0,0,0,60,0,0,0,0,0};
 public static final byte[] BLEEDING={0,0,0,0,0,60,0,0,0,0};
 public static final byte[] PHYSICAL={0,0,0,25,0,0,5,0,0,0};
 public static final byte[] ANOMALY={0,0,0,10,0,0,20,5,0,0};
 public static final byte[] RADIATION={0,0,0,5,0,0,-5,25,0,0};
 private ItemCatalog(){}
 public static boolean valid(int id){return id>0&&id<=MAX_ID&&TYPE[id]!=0;}
}
