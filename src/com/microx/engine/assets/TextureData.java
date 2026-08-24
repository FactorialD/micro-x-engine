package com.microx.engine.assets;

import com.microx.engine.math.Fixed;

/** RGB texture with wrapping Q16.16 coordinates. */
public final class TextureData {
    private final int width,height;private final int[] pixels;
    public TextureData(int w,int h,int[] rgb){width=w;height=h;pixels=rgb;}
    public int sample(int u,int v){int x=Fixed.floorToInt(u)%width,y=Fixed.floorToInt(v)%height;if(x<0)x+=width;if(y<0)y+=height;return pixels[y*width+x];}
}
