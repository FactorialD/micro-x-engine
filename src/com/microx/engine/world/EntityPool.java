package com.microx.engine.world;
public final class EntityPool {
 public final int[] x,y,z,type,health,state; public final boolean[] active; private int count;
 public EntityPool(int capacity){x=new int[capacity];y=new int[capacity];z=new int[capacity];type=new int[capacity];health=new int[capacity];state=new int[capacity];active=new boolean[capacity];}
 public int spawn(int t,int px,int py,int pz,int hp){for(int i=0;i<active.length;i++)if(!active[i]){active[i]=true;type[i]=t;x[i]=px;y[i]=py;z[i]=pz;health[i]=hp;state[i]=0;count++;return i;}return -1;}
 public void remove(int i){if(i>=0&&i<active.length&&active[i]){active[i]=false;count--;}}
 public int activeCount(){return count;} public int capacity(){return active.length;}
}
