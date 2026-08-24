package com.microx.engine.save;
import com.microx.engine.gameplay.GameplayState;

/** Fixed-size mutable save snapshot. Strings and sparse arrays are deliberately bounded for CLDC. */
public final class SaveData {
 public static final int MAX_LOCATION=31,MAX_ENTITY_DELTAS=64;
 public final GameplayState gameplay=new GameplayState();
 public int slot,sequence;public long savedAt;public int seed;public String location="test";public int spawn;
 public int x,y,z,yaw,pitch,health,armor,stamina,bleeding,radiation,weapon,magazine;
 public int entityCount;public final int[] entityId=new int[MAX_ENTITY_DELTAS],entityFlags=new int[MAX_ENTITY_DELTAS];
 public void clearEntities(){entityCount=0;}
 public boolean addEntityDelta(int id,int flags){if(id<=0||entityCount>=MAX_ENTITY_DELTAS)return false;entityId[entityCount]=id;entityFlags[entityCount++]=flags;return true;}
}
