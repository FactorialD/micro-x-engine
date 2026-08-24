package com.microx.engine;
public final class Telemetry {
 public int fps,updateMs,renderMs,updateP50,updateP95,renderP50,renderP95,entities,rooms,submittedTriangles,clippedTriangles,drawnTriangles,droppedFixedSteps,rendererBudgetBytes,rendererUsedBytes,rendererBudgetPercent;public long freeMemory,totalMemory,peakUsedMemory;private int frames,samples;private long epoch;private final short[] updates=new short[64],renders=new short[64],scratch=new short[64];
 public void timing(int update,int render){updateMs=update;renderMs=render;int p=samples&63;updates[p]=(short)Math.min(32767,update);renders[p]=(short)Math.min(32767,render);samples++;}
 public void rendererBudget(int used,int budget){rendererUsedBytes=used;rendererBudgetBytes=budget;rendererBudgetPercent=budget<=0?0:(int)Math.min(100L,(long)used*100/budget);}
 public void frame(long now){frames++;if(now-epoch>=1000){fps=frames;frames=0;epoch=now;Runtime r=Runtime.getRuntime();freeMemory=r.freeMemory();totalMemory=r.totalMemory();long used=totalMemory-freeMemory;if(used>peakUsedMemory)peakUsedMemory=used;int n=Math.min(samples,64);updateP50=percentile(updates,n,50);updateP95=percentile(updates,n,95);renderP50=percentile(renders,n,50);renderP95=percentile(renders,n,95);}}
 private int percentile(short[] source,int n,int percent){if(n==0)return 0;for(int i=0;i<n;i++)scratch[i]=source[i];for(int i=1;i<n;i++){short v=scratch[i];int j=i-1;while(j>=0&&scratch[j]>v){scratch[j+1]=scratch[j];j--;}scratch[j+1]=v;}return scratch[((n-1)*percent)/100];}
}
