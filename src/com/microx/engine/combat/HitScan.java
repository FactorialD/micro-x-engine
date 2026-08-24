package com.microx.engine.combat;
import com.microx.engine.math.Fixed; import com.microx.engine.world.*;
public final class HitScan {
 private int seed=0x13579bdf;
 private int random(){seed=seed*1103515245+12345;return(seed>>>16)&0x7fff;}
 public int fire(Player p,EntityPool pool,int range,int spread){int angle=p.yaw+(random()%(spread*2+1))-spread;int dx=Fixed.cos(angle),dz=Fixed.sin(angle),best=-1;long bestT=Long.MAX_VALUE;for(int i=0;i<pool.capacity();i++)if(pool.active[i]){long rx=pool.x[i]-p.x,rz=pool.z[i]-p.z;long t=(rx*dx+rz*dz)>>Fixed.SHIFT;if(t>0&&t<range){long side=((rx*dz-rz*dx)>>Fixed.SHIFT);if(side<0)side=-side;if(side<Fixed.ONE&&t<bestT){best=i;bestT=t;}}}if(best>=0){pool.health[best]-=25;if(pool.health[best]<=0)pool.remove(best);}return best;}
}
