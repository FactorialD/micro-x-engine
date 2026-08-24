package com.microx.engine.combat;
import com.microx.engine.math.Fixed;import com.microx.engine.world.*;

/** Deterministic 3-D ray test against level geometry and entity hit cylinders. */
public final class HitScan {
 private int seed=0x13579bdf;public int lastYaw,lastPitch,lastDistance;
 public void setSeed(int value){seed=value;}
 private int random(){seed=seed*1103515245+12345;return(seed>>>16)&0x7fff;}
 public int fire(Player p,EntityPool pool,Collision collision,int range,int spread,int damage){
  int stance=p.crouched?spread/2:spread,aim=p.aiming?stance/2:stance;
  lastYaw=p.yaw+jitter(aim);lastPitch=p.pitch+jitter(aim);
  int horizontal=Fixed.cos(lastPitch),dx=Fixed.mul(Fixed.cos(lastYaw),horizontal),dy=Fixed.sin(lastPitch),dz=Fixed.mul(Fixed.sin(lastYaw),horizontal);
  int ox=p.x,oy=p.y+(p.crouched?Player.CROUCH_HEIGHT*3/4:Player.STANDING_HEIGHT*3/4),oz=p.z;
  int wall=collision==null?range:collision.rayDistance(ox,oy,oz,dx,dy,dz,range),best=-1,bestT=wall;
  for(int i=0;i<pool.capacity();i++)if(pool.active[i]&&pool.health[i]>0&&pool.type[i]!=EntityPool.ITEM){int t=hitCylinder(ox,oy,oz,dx,dy,dz,pool.x[i],pool.y[i],pool.z[i],pool.radius[i],Fixed.fromInt(2),bestT);if(t>=0&&t<bestT){best=i;bestT=t;}}
  lastDistance=bestT;if(best>=0){pool.health[best]-=damage;if(pool.health[best]<=0)pool.remove(best);}return best;
 }
 public int fire(Player p,EntityPool pool,int range,int spread){return fire(p,pool,null,range,spread,25);}
 private int jitter(int spread){return spread<=0?0:random()%(spread*2+1)-spread;}
 private int hitCylinder(int ox,int oy,int oz,int dx,int dy,int dz,int cx,int cy,int cz,int radius,int height,int max){
  long rx=cx-ox,rz=cz-oz,t=(rx*dx+rz*dz)>>Fixed.SHIFT;if(t<=0||t>=max)return-1;
  long qx=ox+((long)dx*t>>Fixed.SHIFT)-cx,qz=oz+((long)dz*t>>Fixed.SHIFT)-cz;if(qx*qx+qz*qz>(long)radius*radius)return-1;
  long hitY=oy+((long)dy*t>>Fixed.SHIFT);return hitY>=cy&&hitY<=cy+height?(int)t:-1;
 }
}
