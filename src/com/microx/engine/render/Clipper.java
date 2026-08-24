package com.microx.engine.render;

import com.microx.engine.math.Fixed;

/** Sutherland-Hodgman near/frustum clipper with fixed reusable scratch. */
public final class Clipper {
    private final int[] a = new int[40], b = new int[40]; // x,y,z,u,v; max eight vertices
    private int count;

    int clip(int x0,int y0,int z0,int u0,int v0,int x1,int y1,int z1,int u1,int v1,
             int x2,int y2,int z2,int u2,int v2,int near) {
        put(a,0,x0,y0,z0,u0,v0); put(a,1,x1,y1,z1,u1,v1); put(a,2,x2,y2,z2,u2,v2); count=3;
        count=plane(a,b,count,0,near); if(count<3)return 0; // z-near
        count=plane(b,a,count,1,0); if(count<3)return 0;    // x+z >= 0
        count=plane(a,b,count,2,0); if(count<3)return 0;    // z-x >= 0
        count=plane(b,a,count,3,0); if(count<3)return 0;    // y+z >= 0
        count=plane(a,b,count,4,0);                         // z-y >= 0
        return count;
    }
    private int plane(int[] in,int[] out,int n,int kind,int near){
        int outCount=0,i;
        for(i=0;i<n;i++){
            int ci=i*5,pi=((i+n-1)%n)*5,cd=distance(in,ci,kind,near),pd=distance(in,pi,kind,near);
            boolean cin=cd>=0,pin=pd>=0;
            if(cin!=pin){int t=Fixed.div(pd,Fixed.sub(pd,cd)); interpolate(in,pi,ci,out,outCount++,t);}
            if(cin){copy(in,ci,out,outCount++);}
        }
        return outCount;
    }
    private int distance(int[] p,int o,int kind,int near){
        if(kind==0)return Fixed.sub(p[o+2],near); if(kind==1)return Fixed.add(p[o],p[o+2]);
        if(kind==2)return Fixed.sub(p[o+2],p[o]); if(kind==3)return Fixed.add(p[o+1],p[o+2]);
        return Fixed.sub(p[o+2],p[o+1]);
    }
    private void interpolate(int[] s,int p,int c,int[] d,int di,int t){int o=di*5,j;for(j=0;j<5;j++)d[o+j]=Fixed.lerp(s[p+j],s[c+j],t);}
    private void copy(int[] s,int so,int[] d,int di){int o=di*5,j;for(j=0;j<5;j++)d[o+j]=s[so+j];}
    private void put(int[] d,int i,int x,int y,int z,int u,int v){i*=5;d[i]=x;d[i+1]=y;d[i+2]=z;d[i+3]=u;d[i+4]=v;}
    int value(int vertex,int component){return b[vertex*5+component];}
}
