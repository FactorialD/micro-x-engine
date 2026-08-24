package com.microx.engine.render;

import javax.microedition.lcdui.Graphics;
import com.microx.engine.assets.*;
import com.microx.engine.math.Fixed;
import com.microx.engine.world.*;

/** Executes clear, portal selection, transform, clipping, culling, raster and presentation. */
public final class FrameCoordinator {
    private int[] rgb; private short[] depth; private int width,height,outputWidth,outputHeight;
    private AssetManager assets; private final RenderCamera camera=new RenderCamera();
    private final VertexTransformer transformer=new VertexTransformer(); private final Clipper clipper=new Clipper();
    private final Rasterizer rasterizer=new Rasterizer();
    int submittedTriangles,clippedTriangles,drawnTriangles;
    void setAssets(AssetManager value){assets=value;}
    void prepareAssets(){if(assets!=null)transformer.reserve(assets.maximumLocationVertices());}
    void configure(int w,int h,int budget){
        outputWidth=w;outputHeight=h;if(w<=0||h<=0)return;
        while((long)w*h*6L>budget){w=(w+1)/2;h=(h+1)/2;}
        if(w==width&&h==height&&rgb!=null)return;
        try{rgb=new int[w*h];depth=new short[w*h];}catch(OutOfMemoryError e){w=(w+1)/2;h=(h+1)/2;rgb=new int[w*h];depth=new short[w*h];}
        width=w;height=h;rasterizer.target(rgb,depth,w,h);
    }
    void render(Graphics g,Player player,PortalWorld world){
        if(rgb==null)return;submittedTriangles=clippedTriangles=drawnTriangles=0;rasterizer.clear(0x182030);camera.update(player,width,height);
        if(assets!=null){int rooms=world.visibleCount(),r,s;for(r=0;r<rooms;r++){int room=world.visibleRoom(r);for(s=0;s<assets.locationSectionCount();s++){MeshSection mesh=assets.locationSection(s);if(mesh.room()==room)draw(mesh,assets.texture(mesh.texture()));}}}
        if(width!=outputWidth||height!=outputHeight){g.setColor(0);g.fillRect(0,0,outputWidth,outputHeight);}
        g.drawRGB(rgb,0,width,(outputWidth-width)/2,(outputHeight-height)/2,width,height,false);
    }
    private void draw(MeshSection mesh,TextureData texture){
        transformer.transform(mesh,camera);int t;
        for(t=0;t<mesh.triangleCount();t++){submittedTriangles++;int i0=mesh.index(t*3),i1=mesh.index(t*3+1),i2=mesh.index(t*3+2);
            int n=clipper.clip(transformer.x(i0),transformer.y(i0),transformer.z(i0),mesh.u(i0),mesh.v(i0),transformer.x(i1),transformer.y(i1),transformer.z(i1),mesh.u(i1),mesh.v(i1),transformer.x(i2),transformer.y(i2),transformer.z(i2),mesh.u(i2),mesh.v(i2),camera.near);
            if(n<3){clippedTriangles++;continue;}if(n!=3)clippedTriangles++;int fan;for(fan=1;fan<n-1;fan++)projectAndDraw(0,fan,fan+1,texture);
        }
    }
    private void projectAndDraw(int a,int b,int c,TextureData texture){
        int z0=clipper.value(a,2),z1=clipper.value(b,2),z2=clipper.value(c,2);
        int x0=width/2+Fixed.toInt(Fixed.div(Fixed.mul(clipper.value(a,0),camera.focalX),z0));
        int y0=height/2-Fixed.toInt(Fixed.div(Fixed.mul(clipper.value(a,1),camera.focalY),z0));
        int x1=width/2+Fixed.toInt(Fixed.div(Fixed.mul(clipper.value(b,0),camera.focalX),z1));
        int y1=height/2-Fixed.toInt(Fixed.div(Fixed.mul(clipper.value(b,1),camera.focalY),z1));
        int x2=width/2+Fixed.toInt(Fixed.div(Fixed.mul(clipper.value(c,0),camera.focalX),z2));
        int y2=height/2-Fixed.toInt(Fixed.div(Fixed.mul(clipper.value(c,1),camera.focalY),z2));
        long facing=(long)(x2-x0)*(y1-y0)-(long)(y2-y0)*(x1-x0);if(facing<=0){clippedTriangles++;return;}
        if(rasterizer.draw(x0,y0,z0,clipper.value(a,3),clipper.value(a,4),x1,y1,z1,clipper.value(b,3),clipper.value(b,4),x2,y2,z2,clipper.value(c,3),clipper.value(c,4),texture))drawnTriangles++;
    }
    int width(){return width;}int height(){return height;}void release(){rgb=null;depth=null;assets=null;width=height=0;}
}
