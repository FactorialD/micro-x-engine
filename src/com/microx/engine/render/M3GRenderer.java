package com.microx.engine.render;
import javax.microedition.lcdui.Graphics; import javax.microedition.m3g.*; import com.microx.engine.assets.AssetManager; import com.microx.engine.world.*; import com.microx.engine.math.Fixed;
public final class M3GRenderer {
 private final Graphics3D g3d=Graphics3D.getInstance(); private World world; private Camera camera; private Background background; private final Transform transform=new Transform();
 public void load(AssetManager assets){release();Object3D[] a=assets.objects();if(a!=null)for(int i=0;i<a.length;i++)if(a[i] instanceof World){world=(World)a[i];break;}if(world==null)world=new World();camera=new Camera();camera.setPerspective(60,0.75f,1,100);world.addChild(camera);world.setActiveCamera(camera);background=new Background();background.setColor(0x182030);world.setBackground(background);}
 public void render(Graphics g,Player p,PortalWorld portals){if(world==null)return;transform.setIdentity();transform.postRotate(p.yaw,0,1,0);transform.postTranslate(-p.x/(float)Fixed.ONE,-(p.y+Fixed.fromInt(2))/(float)Fixed.ONE,-p.z/(float)Fixed.ONE);camera.setTransform(transform);g3d.bindTarget(g);try{g3d.render(world);}finally{g3d.releaseTarget();}}
 public void release(){if(world!=null&&camera!=null)world.removeChild(camera);world=null;camera=null;background=null;}
}
