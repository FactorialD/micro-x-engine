package com.microx.engine.world;
/** Area triggers and deterministic artifact production. */
public final class AnomalySystem {
 public static final int DAMAGE=0,SLOW=1,PSI=2;private final int seed;private int entries;
 public AnomalySystem(int value){seed=value;}
 public void enterLocation(EntityPool p){entries++;for(int i=0;i<p.capacity();i++)if(p.active[i]&&p.type[i]==EntityPool.ANOMALY&&mix(seed+entries*31+i)%4==0){int n=p.spawn(EntityPool.ITEM,p.x[i]+((mix(seed+i)&255)-128)*128,p.y[i],p.z[i]+((mix(seed+entries)&255)-128)*128,1);if(n>=0){p.spriteId[n]=100+(mix(seed^entries^i)%8);p.roomId[n]=p.roomId[i];}}}
 public void update(EntityPool p,Player player,int dt){for(int i=0;i<p.capacity();i++)if(p.active[i]&&p.type[i]==EntityPool.ANOMALY){if(p.timer[i]>0)p.timer[i]-=dt;long dx=player.x-p.x[i],dz=player.z-p.z[i];if(p.timer[i]<=0&&dx*dx+dz*dz<(long)p.radius[i]*p.radius[i]){if(p.aux[i]==DAMAGE)player.health-=10;else if(p.aux[i]==PSI)player.stamina-=10;p.timer[i]=2000;}}}
 private static int mix(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;return x&0x7fffffff;}
}
