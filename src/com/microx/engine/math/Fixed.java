package com.microx.engine.math;

/** Q12 fixed point operations. Tables are built once, never in the frame loop. */
public final class Fixed {
    public static final int SHIFT = 12, ONE = 1 << SHIFT, MASK = ONE - 1;
    private static final int[] SIN = new int[360];
    static {
        // Integer recurrence using Q12 sin(1 degree)=71 and cos(1 degree)=4095.
        int s=0,c=ONE;
        for (int i=0;i<360;i++) { SIN[i]=s; int ns=(int)(((long)s*4095+(long)c*71)>>SHIFT); c=(int)(((long)c*4095-(long)s*71)>>SHIFT); s=ns; }
    }
    private Fixed() {}
    public static int fromInt(int n) { return n << SHIFT; }
    public static int toInt(int n) { return n >> SHIFT; }
    public static int mul(int a,int b) { return (int)(((long)a*b)>>SHIFT); }
    public static int div(int a,int b) { if(b==0) return a<0?Integer.MIN_VALUE:Integer.MAX_VALUE; return (int)(((long)a<<SHIFT)/b); }
    public static int clamp(int v,int lo,int hi) { return v<lo?lo:(v>hi?hi:v); }
    public static int sin(int degrees) { degrees%=360; if(degrees<0)degrees+=360; return SIN[degrees]; }
    public static int cos(int degrees) { return sin(degrees+90); }
}
