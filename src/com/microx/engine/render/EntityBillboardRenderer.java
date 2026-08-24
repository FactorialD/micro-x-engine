package com.microx.engine.render;
import com.microx.engine.math.Fixed;import com.microx.engine.world.*;
/** Depth-tested, room-visible, distance-culled entity billboard pass. */
public final class EntityBillboardRenderer {
 private static final int FAR=Fixed.fromInt(40);
 public void render(int[] rgb,short[] depth,int width,int height,RenderCamera c,EntityPool p){for(int i=0;i<p.capacity();i++)if(p.active[i]&&(p.flags[i]&EntityPool.FLAG_VISIBLE)!=0){long dx=(long)p.x[i]-c.x,dz=(long)p.z[i]-c.z,vx=(dx*c.cos+dz*c.sin)/Fixed.ONE,vz=(dz*c.cos-dx*c.sin)/Fixed.ONE;if(vz<c.near||vz>FAR)continue;int sx=width/2+(int)(vx*c.focalX/vz),sy=height/2-(int)(((long)p.y[i]+Fixed.ONE-c.y)*c.focalY/vz),size=(int)((long)c.focalY*Fixed.ONE/vz);if(size<1)size=1;if(size>48)size=48;int color=0xff000000|((p.spriteId[i]*1103515245)>>>8&0xffffff),d=(int)(vz>>12);for(int yy=sy-size;yy<=sy;yy++)if(yy>=0&&yy<height)for(int xx=sx-size/2;xx<=sx+size/2;xx++)if(xx>=0&&xx<width){int at=yy*width+xx;if(d<(depth[at]&65535)){depth[at]=(short)d;rgb[at]=color;}}}}
}
