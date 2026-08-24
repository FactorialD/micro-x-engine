package com.microx.engine.world;

/** Fixed-size room graph. Portal bounds are retained for rendering and crossing tests. */
public final class PortalWorld {
 private final int[] minX,maxX,minZ,maxZ,firstPortal,portalCount;
 private final int[] portalFrom,portalTo,portalNext,portalId,portalReverse,portalTransition;
 private final int[] pMinX,pMaxX,pMinY,pMaxY,pMinZ,pMaxZ;
 private final boolean[] visited; private final int[] queue,depth,visible; private int visibleCount;
 public PortalWorld(int rooms,int portals){minX=new int[rooms];maxX=new int[rooms];minZ=new int[rooms];maxZ=new int[rooms];firstPortal=new int[rooms];portalCount=new int[rooms];portalFrom=new int[portals];portalTo=new int[portals];portalNext=new int[portals];portalId=new int[portals];portalReverse=new int[portals];portalTransition=new int[portals];pMinX=new int[portals];pMaxX=new int[portals];pMinY=new int[portals];pMaxY=new int[portals];pMinZ=new int[portals];pMaxZ=new int[portals];visited=new boolean[rooms];queue=new int[rooms];depth=new int[rooms];visible=new int[rooms];for(int i=0;i<rooms;i++)firstPortal[i]=-1;}
 public void room(int i,int a,int b,int c,int d){minX[i]=a;maxX[i]=b;minZ[i]=c;maxZ[i]=d;}
 public void portal(int i,int id,int from,int to,int a,int b,int c,int d,int e,int f,int reverse,int transition){portalId[i]=id;portalFrom[i]=from;portalTo[i]=to;pMinX[i]=a;pMaxX[i]=b;pMinY[i]=c;pMaxY[i]=d;pMinZ[i]=e;pMaxZ[i]=f;portalReverse[i]=reverse;portalTransition[i]=transition;portalNext[i]=firstPortal[from];firstPortal[from]=i;portalCount[from]++;}
 public int findRoom(int x,int z){for(int i=0;i<minX.length;i++)if(x>=minX[i]&&x<=maxX[i]&&z>=minZ[i]&&z<=maxZ[i])return i;return -1;}
 public int crossedPortal(int oldX,int oldY,int oldZ,int x,int y,int z){int room=findRoom(oldX,oldZ);if(room<0)return-1;for(int p=firstPortal[room];p>=0;p=portalNext[p])if(x>=pMinX[p]&&x<=pMaxX[p]&&y>=pMinY[p]&&y<=pMaxY[p]&&z>=pMinZ[p]&&z<=pMaxZ[p])return p;return-1;}
 public int portalTo(int p){return portalTo[p];} public int portalId(int p){return portalId[p];} public int portalReverse(int p){return portalReverse[p];} public int portalTransition(int p){return portalTransition[p];}
 public int updateVisibility(int x,int z){for(int i=0;i<visited.length;i++)visited[i]=false;visibleCount=0;int start=findRoom(x,z);if(start<0)return 0;int head=0,tail=1;queue[0]=start;depth[0]=0;visited[start]=true;while(head<tail){int r=queue[head];int d=depth[head++];visible[visibleCount++]=r;if(d>=4)continue;for(int p=firstPortal[r];p>=0;p=portalNext[p]){int n=portalTo[p];if(!visited[n]){visited[n]=true;queue[tail]=n;depth[tail++]=d+1;}}}return visibleCount;}
 public int visibleRoom(int i){return visible[i];} public int visibleCount(){return visibleCount;}
}
