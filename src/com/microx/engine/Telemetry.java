package com.microx.engine;
public final class Telemetry {
 public int fps,updateMs,renderMs,entities,rooms;public long freeMemory,totalMemory;private int frames;private long epoch;
 public void frame(long now){frames++;if(now-epoch>=1000){fps=frames;frames=0;epoch=now;Runtime r=Runtime.getRuntime();freeMemory=r.freeMemory();totalMemory=r.totalMemory();}}
}
