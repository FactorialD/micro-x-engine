package com.microx.engine.world;
import com.microx.engine.math.Fixed;
public final class Collision {
 private int minX,maxX,minZ,maxZ;
 public void setBounds(int a,int b,int c,int d){minX=a;maxX=b;minZ=c;maxZ=d;}
 public int clipX(int x){return Fixed.clamp(x,minX,maxX);} public int clipZ(int z){return Fixed.clamp(z,minZ,maxZ);}
 public int floorHeight(int x,int z){return x>Fixed.fromInt(4)?Fixed.fromInt(1):0;}
}
