package com.microx.engine.world;

/** Fixed structure-of-arrays store. No entity object is allocated while playing. */
public final class EntityPool {
 public static final int HUMAN=1,MUTANT=2,ITEM=3,ANOMALY=4,CORPSE=5,DOOR=6,CONTAINER=7;
 public static final int MAX_LIVE_NPCS=24,MAX_MUTANTS=24,MAX_ITEMS=48,MAX_ANOMALIES=16,MAX_CORPSES=24;
 public static final int STATE_IDLE=0,STATE_ALERT=1,STATE_COMBAT=2,STATE_MELEE=3,STATE_LEAP=4,STATE_INVISIBLE=5,STATE_PSI=6,STATE_AOE=7;
 public static final int FLAG_VISIBLE=1,FLAG_PERCEIVES_PLAYER=2,FLAG_INTERACTABLE=4,FLAG_DEAD=8,FLAG_UPDATE=16;
 public final int[] x,y,z,type,health,state,roomId,direction,radius,faction,timer,target,flags,spriteId,aux;
 public final boolean[] active;private int count;private final int[] kinds=new int[8];
 public EntityPool(int capacity){x=new int[capacity];y=new int[capacity];z=new int[capacity];type=new int[capacity];health=new int[capacity];state=new int[capacity];roomId=new int[capacity];direction=new int[capacity];radius=new int[capacity];faction=new int[capacity];timer=new int[capacity];target=new int[capacity];flags=new int[capacity];spriteId=new int[capacity];aux=new int[capacity];active=new boolean[capacity];}
 public int spawn(int t,int px,int py,int pz,int hp){if(limitReached(t))return-1;for(int i=0;i<active.length;i++)if(!active[i]){active[i]=true;type[i]=t;x[i]=px;y[i]=py;z[i]=pz;health[i]=hp;state[i]=STATE_IDLE;roomId[i]=-1;direction[i]=0;radius[i]=16384;faction[i]=0;timer[i]=0;target[i]=-1;flags[i]=FLAG_UPDATE|(t>=ITEM?FLAG_INTERACTABLE:0);spriteId[i]=t;aux[i]=0;count++;if(t<kinds.length)kinds[t]++;return i;}return-1;}
 private boolean limitReached(int t){int max=t==HUMAN?MAX_LIVE_NPCS:t==MUTANT?MAX_MUTANTS:t==ITEM?MAX_ITEMS:t==ANOMALY?MAX_ANOMALIES:t==CORPSE?MAX_CORPSES:active.length;return t<kinds.length&&kinds[t]>=max;}
 public void remove(int i){if(i>=0&&i<active.length&&active[i]){active[i]=false;if(type[i]<kinds.length)kinds[type[i]]--;count--;}}
 public int activeCount(){return count;}public int capacity(){return active.length;}public int typeCount(int t){return t>=0&&t<kinds.length?kinds[t]:0;}
}
