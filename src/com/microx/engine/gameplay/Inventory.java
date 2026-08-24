package com.microx.engine.gameplay;

/** Allocation-free bounded inventory. Mutating operations are all-or-nothing. */
public final class Inventory {
 private final short[] ids,counts,durability;private final int cellLimit;private int money;
 public Inventory(int slots,int cells){if(slots<1||cells<1)throw new IllegalArgumentException();ids=new short[slots];counts=new short[slots];durability=new short[slots];cellLimit=cells;}
 public int slots(){return ids.length;}public int idAt(int i){check(i);return ids[i]&65535;}public int countAt(int i){check(i);return counts[i]&65535;}public int durabilityAt(int i){check(i);return durability[i]&65535;}
 public int money(){return money;}public boolean setMoney(int value){if(value<0)return false;money=value;return true;}public int cellsUsed(){int n=0;for(int i=0;i<ids.length;i++)if(ids[i]!=0)n+=ItemCatalog.CELLS[ids[i]];return n;}
 public void clear(){for(int i=0;i<ids.length;i++)ids[i]=counts[i]=durability[i]=0;money=0;}
 public int count(int id){int n=0;for(int i=0;i<ids.length;i++)if(ids[i]==id)n+=counts[i]&65535;return n;}
 public boolean canAdd(int id,int amount){if(!ItemCatalog.valid(id)||amount<=0)return false;int left=amount;for(int i=0;i<ids.length;i++)if(ids[i]==id)left-=Math.min(left,ItemCatalog.STACK[id]-(counts[i]&65535));int freeCells=cellLimit-cellsUsed();for(int i=0;i<ids.length&&left>0;i++)if(ids[i]==0&&freeCells>=ItemCatalog.CELLS[id]){left-=Math.min(left,ItemCatalog.STACK[id]);freeCells-=ItemCatalog.CELLS[id];}return left==0;}
 public boolean canAddAfterRemoving(int addId,int amount,int removeId){if(count(removeId)<1)return false;int slot=-1;for(int i=ids.length-1;i>=0;i--)if(ids[i]==removeId){slot=i;break;}short oldId=ids[slot],oldCount=counts[slot],oldDurability=durability[slot];counts[slot]--;if(counts[slot]==0){ids[slot]=0;durability[slot]=0;}boolean result=canAdd(addId,amount);ids[slot]=oldId;counts[slot]=oldCount;durability[slot]=oldDurability;return result;}
 public boolean add(int id,int amount,int condition){if(!canAdd(id,amount)||condition<0||condition>100)return false;int left=amount;for(int i=0;i<ids.length&&left>0;i++)if(ids[i]==id&&(counts[i]&65535)<ItemCatalog.STACK[id]){int n=Math.min(left,ItemCatalog.STACK[id]-(counts[i]&65535));counts[i]+=n;left-=n;}for(int i=0;i<ids.length&&left>0;i++)if(ids[i]==0){int n=Math.min(left,ItemCatalog.STACK[id]);ids[i]=(short)id;counts[i]=(short)n;durability[i]=(short)condition;left-=n;}return true;}
 public boolean remove(int id,int amount){if(amount<=0||count(id)<amount)return false;int left=amount;for(int i=ids.length-1;i>=0&&left>0;i--)if(ids[i]==id){int n=Math.min(left,counts[i]&65535);counts[i]-=n;left-=n;if(counts[i]==0){ids[i]=0;durability[i]=0;}}return true;}
 public boolean moveTo(Inventory target,int id,int amount){if(target==null||amount<=0||count(id)<amount||!target.canAdd(id,amount))return false;int condition=conditionOf(id);remove(id,amount);target.add(id,amount,condition);return true;}
 public int conditionOf(int id){for(int i=0;i<ids.length;i++)if(ids[i]==id)return durability[i]&65535;return 100;}
 public boolean setCondition(int id,int value){if(value<0||value>100)return false;for(int i=0;i<ids.length;i++)if(ids[i]==id){durability[i]=(short)value;return true;}return false;}
 private void check(int i){if(i<0||i>=ids.length)throw new IndexOutOfBoundsException();}
}
