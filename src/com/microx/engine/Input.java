package com.microx.engine;

/** Physical-key normalizer. UI code consumes commands; gameplay consumes bits. */
public final class Input {
 public static final int FORWARD=1,BACK=2,LEFT=4,RIGHT=8,STRAFE_LEFT=16,STRAFE_RIGHT=32,FIRE=64,AIM=128,CROUCH=256,JUMP=512,WEAPON=1024,PDA=2048,PAUSE=4096;
 public static final int NONE=0,UP=1,DOWN=2,UI_LEFT=3,UI_RIGHT=4,ACCEPT=5,BACK_CMD=6,MENU=7,TAB_LEFT=8,TAB_RIGHT=9;
 private int down,pressed,doubleTap,held;private final long[] last=new long[13],since=new long[13];
 public synchronized void key(int key,boolean on,long now){int b=gameplayBit(key);if(b==0)return;int i=index(b);if(on){if((down&b)==0){pressed|=b;if(now-last[i]<=280)doubleTap|=b;last[i]=now;since[i]=now;}down|=b;}else{down&=~b;held&=~b;}}
 public synchronized void update(long now){for(int i=0,b=1;i<13;i++,b<<=1)if((down&b)!=0&&now-since[i]>=450)held|=b;}
 public synchronized void endUpdate(){pressed=doubleTap=0;}
 public int down(){return down;}public int pressed(){return pressed;}public int doubleTapped(){return doubleTap;}public int held(){return held;}
 private int index(int b){int i=0;while((b>>=1)!=0)i++;return i;}
 /** Includes common Nokia/Sony Ericsson game-action and soft-key codes. */
 public static int command(int k){switch(k){case '2':case -1:return UP;case '8':case -2:return DOWN;case '4':case -3:return UI_LEFT;case '6':case -4:return UI_RIGHT;case '5':case -5:return ACCEPT;case -6:return BACK_CMD;case -7:return MENU;case '*':return TAB_LEFT;case '#':return TAB_RIGHT;default:return NONE;}}
 public static int gameplayBit(int k){switch(k){case '2':case -1:return FORWARD;case '8':case -2:return BACK;case '4':case -3:return LEFT;case '6':case -4:return RIGHT;case '1':return STRAFE_LEFT;case '3':return STRAFE_RIGHT;case '5':case -5:return FIRE;case '7':return AIM;case '9':return CROUCH;case '0':return JUMP;case '#':return WEAPON;case '*':return PDA;case -6:case -7:return PAUSE;default:return 0;}}
}
