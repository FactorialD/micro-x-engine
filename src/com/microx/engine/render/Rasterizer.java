package com.microx.engine.render;

import com.microx.engine.assets.TextureData;
import com.microx.engine.math.Fixed;

/** Integer edge-function rasterizer writing RGB and unsigned 16-bit depth. */
public final class Rasterizer {
    private int[] color; private short[] depth; private int width,height;
    void target(int[] rgb,short[] z,int w,int h){color=rgb;depth=z;width=w;height=h;}
    void clear(int rgb){int i;for(i=color.length-1;i>=0;i--){color[i]=rgb;depth[i]=(short)0xffff;}}
    boolean draw(int x0,int y0,int z0,int u0,int v0,int x1,int y1,int z1,int u1,int v1,
                 int x2,int y2,int z2,int u2,int v2,TextureData texture){
        long area=edge(x0,y0,x1,y1,x2,y2); if(area<=0)return false;
        int minX=min(x0,x1,x2),maxX=max(x0,x1,x2),minY=min(y0,y1,y2),maxY=max(y0,y1,y2);
        if(minX<0)minX=0;if(minY<0)minY=0;if(maxX>=width)maxX=width-1;if(maxY>=height)maxY=height-1;
        int px,py; boolean hit=false;
        for(py=minY;py<=maxY;py++)for(px=minX;px<=maxX;px++){
            long w0=edge(x1,y1,x2,y2,px,py),w1=edge(x2,y2,x0,y0,px,py),w2=edge(x0,y0,x1,y1,px,py);
            if(w0>=0&&w1>=0&&w2>=0){
                int zz=(int)((w0*(z0>>8)+w1*(z1>>8)+w2*(z2>>8))/area); if(zz<0)zz=0;if(zz>65535)zz=65535;
                int at=py*width+px;if(zz<(depth[at]&0xffff)){
                    int u=(int)((w0*u0+w1*u1+w2*u2)/area),v=(int)((w0*v0+w1*v1+w2*v2)/area);
                    depth[at]=(short)zz;color[at]=texture.sample(u,v);hit=true;
                }
            }
        }
        return hit;
    }
    private long edge(int ax,int ay,int bx,int by,int px,int py){return(long)(px-ax)*(by-ay)-(long)(py-ay)*(bx-ax);}
    private int min(int a,int b,int c){return a<b?(a<c?a:c):(b<c?b:c);} private int max(int a,int b,int c){return a>b?(a>c?a:c):(b>c?b:c);}
}
