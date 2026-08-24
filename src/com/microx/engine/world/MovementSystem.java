package com.microx.engine.world;
import com.microx.engine.math.Fixed;
/** Collision-aware steering with separation; all scratch values remain scalar. */
public final class MovementSystem {
 public void update(EntityPool p,Player player,Collision c,int dt){int step=Fixed.mul(Fixed.fromInt(2),Fixed.div(Fixed.fromInt(dt),Fixed.fromInt(1000)));for(int i=0;i<p.capacity();i++)if(p.active[i]&&(p.flags[i]&EntityPool.FLAG_UPDATE)!=0&&(p.state[i]==EntityPool.STATE_COMBAT||p.state[i]>=EntityPool.STATE_MELEE)){long dx=player.x-p.x[i],dz=player.z-p.z[i];int sx=dx>0?step:-step,sz=dz>0?step:-step;for(int j=0;j<p.capacity();j++)if(j!=i&&p.active[j]){long ox=p.x[i]-p.x[j],oz=p.z[i]-p.z[j],rr=p.radius[i]+p.radius[j];if(ox*ox+oz*oz<(long)rr*rr){sx+=ox>0?step/2:-step/2;sz+=oz>0?step/2:-step/2;}}c.sweep(p.x[i],p.y[i],p.z[i],sx,sz,p.radius[i],Fixed.fromInt(2),Fixed.ONE/2);p.x[i]=c.resultX;p.z[i]=c.resultZ;}}
}
