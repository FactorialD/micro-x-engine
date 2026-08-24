package com.microx.engine.world;
import com.microx.engine.math.Fixed;
/** Fixed-step scheduler: near=50 Hz, visible=25 Hz, far=5 Hz. */
public final class WorldSystems {
 private final PerceptionSystem perception=new PerceptionSystem();private final MovementSystem movement=new MovementSystem();private final StateMachineSystem states;private final AnomalySystem anomalies;private int tick;
 public WorldSystems(int seed){states=new StateMachineSystem(seed);anomalies=new AnomalySystem(seed);}
 public void enter(EntityPool p){anomalies.enterLocation(p);}
 public void update(EntityPool p,Player player,Collision c,PortalWorld world,int dt){tick++;for(int i=0;i<p.capacity();i++)if(p.active[i]){p.roomId[i]=world.findRoom(p.x[i],p.z[i]);long dx=p.x[i]-player.x,dz=p.z[i]-player.z;p.flags[i]=(p.flags[i]&~EntityPool.FLAG_VISIBLE)|(visible(p.roomId[i],world)?EntityPool.FLAG_VISIBLE:0);int divisor=dx*dx+dz*dz<(long)Fixed.fromInt(10)*Fixed.fromInt(10)?1:((p.flags[i]&EntityPool.FLAG_VISIBLE)!=0?2:10);p.aux[i]=(p.aux[i]&65535)|(divisor<<16);if(tick%divisor==0)p.flags[i]|=EntityPool.FLAG_UPDATE;else p.flags[i]&=~EntityPool.FLAG_UPDATE;}
  perception.update(p,player,c);states.update(p,player,dt);anomalies.update(p,player,dt);movement.update(p,player,c,dt);
 }
 private boolean visible(int room,PortalWorld w){for(int i=0;i<w.visibleCount();i++)if(w.visibleRoom(i)==room)return true;return false;}
}
