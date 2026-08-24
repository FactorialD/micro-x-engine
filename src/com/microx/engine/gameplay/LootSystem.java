package com.microx.engine.gameplay;

/** Seed-only loot generation: results do not depend on call order or platform RNG. */
public final class LootSystem {
 private LootSystem(){}
 public static boolean generateCorpse(Inventory corpse,int corpseId,int worldSeed,int location,int faction,int rank){if(corpse.count(GameIds.ITEM_BANDAGE)>0||corpse.count(GameIds.ITEM_AMMO_9MM)>0||corpse.count(GameIds.ITEM_AMMO_545)>0)return false;int x=mix(worldSeed^corpseId*1103515245^location*8191^faction*131^rank);int weapon=rank>=2?GameIds.ITEM_RIFLE:GameIds.ITEM_PISTOL;int ammo=weapon==GameIds.ITEM_RIFLE?GameIds.ITEM_AMMO_545:GameIds.ITEM_AMMO_9MM;int rounds=6+(x&15)+rank*4;if(!corpse.canAdd(weapon,1)||!corpse.canAdd(ammo,rounds))return false;corpse.add(weapon,1,45+((x>>>8)&31));corpse.add(ammo,rounds,100);if((x&3)==0&&corpse.canAdd(GameIds.ITEM_BANDAGE,1))corpse.add(GameIds.ITEM_BANDAGE,1,100);return true;}
 public static boolean drop(Inventory owner,Inventory ground,int item,int amount){return owner.moveTo(ground,item,amount);}public static boolean pickup(Inventory ground,Inventory owner,int item,int amount){return ground.moveTo(owner,item,amount);}
 private static int mix(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;x*=0x846ca68b;return x^(x>>>16);}
}
