package com.microx.engine.gameplay;

/** Numeric flags/counters plus data-driven quest transitions and dialog predicates. */
public final class QuestState {
 public static final int LOCKED=0,ACTIVE=1,COMPLETE=2,FAILED=3;
 private final byte[] states;private final int[] flags;private final short[] counters;private int objective=-1;
 public QuestState(int quests,int flagWords,int counterCount){states=new byte[quests+1];flags=new int[flagWords];counters=new short[counterCount];}
 public int state(int quest){return quest>0&&quest<states.length?states[quest]:LOCKED;}public int objective(){return objective;}public void setObjective(int marker){objective=marker;}
 public boolean flag(int id){return id>=0&&id<flags.length*32&&(flags[id>>>5]&(1<<(id&31)))!=0;}public boolean setFlag(int id,boolean on){if(id<0||id>=flags.length*32)return false;if(on)flags[id>>>5]|=1<<(id&31);else flags[id>>>5]&=~(1<<(id&31));return true;}
 public int counter(int id){return id>=0&&id<counters.length?counters[id]:0;}public boolean addCounter(int id,int delta){if(id<0||id>=counters.length)return false;int n=counters[id]+delta;if(n<0||n>32767)return false;counters[id]=(short)n;return true;}
 public boolean transition(int quest,int from,int to,int requiredQuest,int requiredState,int flagId,int counterId,int minimum,int marker){if(quest<=0||quest>=states.length||states[quest]!=from)return false;if(requiredQuest>0&&state(requiredQuest)!=requiredState)return false;if(flagId>=0&&!flag(flagId))return false;if(counterId>=0&&counter(counterId)<minimum)return false;states[quest]=(byte)to;objective=to==ACTIVE?marker:-1;return true;}
 public boolean dialogAllowed(int requiredQuest,int requiredState,int flagId,int counterId,int minimum){return(requiredQuest<=0||state(requiredQuest)==requiredState)&&(flagId<0||flag(flagId))&&(counterId<0||counter(counterId)>=minimum);}
 public int questCapacity(){return states.length-1;}public int flagCapacity(){return flags.length*32;}public int counterCapacity(){return counters.length;}
 public void restoreState(int id,int value){if(id>0&&id<states.length&&value>=LOCKED&&value<=FAILED)states[id]=(byte)value;}
 public void restoreCounter(int id,int value){if(id>=0&&id<counters.length&&value>=0&&value<=32767)counters[id]=(short)value;}
 public void clear(){for(int i=0;i<states.length;i++)states[i]=0;for(int i=0;i<flags.length;i++)flags[i]=0;for(int i=0;i<counters.length;i++)counters[i]=0;objective=-1;}
}
