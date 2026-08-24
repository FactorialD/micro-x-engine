package com.microx.engine.world;
import com.microx.engine.math.Fixed;
public final class SimulationTest {
 public static void main(String[] args){statesAndSight();corpseAndCapacity();determinism();System.out.println("SimulationTest OK");}
 private static void statesAndSight(){Collision c=space();Player pl=new Player();pl.reset(Fixed.fromInt(3),0,0);EntityPool p=new EntityPool(64);int h=p.spawn(EntityPool.HUMAN,0,0,0,100);p.direction[h]=0;new PerceptionSystem().update(p,pl,c);StateMachineSystem s=new StateMachineSystem(7);s.update(p,pl,20);eq(EntityPool.STATE_COMBAT,p.state[h]);Collision wall=space();wall.edge(0,0,Fixed.ONE,Fixed.fromInt(-1),Fixed.ONE,Fixed.ONE,0,Fixed.fromInt(3));ok(!wall.lineOfSight(0,Fixed.ONE,0,Fixed.fromInt(2),0));}
 private static void corpseAndCapacity(){EntityPool p=new EntityPool(64);for(int i=0;i<EntityPool.MAX_LIVE_NPCS;i++)ok(p.spawn(EntityPool.HUMAN,i,0,0,1)>=0);eq(-1,p.spawn(EntityPool.HUMAN,0,0,0,1));int c=p.spawn(EntityPool.CORPSE,0,0,0,1);p.timer[c]=1;new StateMachineSystem(1).update(p,new Player(),20);ok(!p.active[c]);}
 private static void determinism(){EntityPool a=new EntityPool(32),b=new EntityPool(32);int ia=a.spawn(EntityPool.MUTANT,0,0,0,10),ib=b.spawn(EntityPool.MUTANT,0,0,0,10);a.state[ia]=b.state[ib]=EntityPool.STATE_MELEE;a.aux[ia]=b.aux[ib]=15;StateMachineSystem sa=new StateMachineSystem(99),sb=new StateMachineSystem(99);Player pa=new Player(),pb=new Player();for(int i=0;i<20;i++){sa.update(a,pa,800);sb.update(b,pb,800);}eq(a.state[ia],b.state[ib]);eq(pa.health,pb.health);}
 private static Collision space(){Collision c=new Collision(1,1,1);c.floor(0,0,Fixed.fromInt(-10),Fixed.fromInt(10),Fixed.fromInt(-10),Fixed.fromInt(10),0);c.ceiling(0,0,Fixed.fromInt(-10),Fixed.fromInt(10),Fixed.fromInt(-10),Fixed.fromInt(10),Fixed.fromInt(4));return c;}
 private static void eq(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}private static void ok(boolean v){if(!v)throw new AssertionError();}
}
