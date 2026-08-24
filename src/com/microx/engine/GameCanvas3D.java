package com.microx.engine;
import javax.microedition.lcdui.*;
public final class GameCanvas3D extends GameCanvas {
 private final Engine engine; private final boolean debug;
 public GameCanvas3D(Engine e,boolean d){super(false);engine=e;debug=d;e.attach(this);setFullScreenMode(true);}
 protected void keyPressed(int key){if(key==-7){engine.togglePause();return;}engine.input.key(key,true,System.currentTimeMillis());}
 protected void keyReleased(int key){engine.input.key(key,false,System.currentTimeMillis());}
 public void renderFrame(){Graphics g=getGraphics();g.setColor(0);g.fillRect(0,0,getWidth(),getHeight());if(engine.level!=null){engine.renderer.render(g,engine.player,engine.level.world);engine.hud.paint(g,engine.player,engine.stats.fps,engine.stats.entities,engine.stats.rooms,debug);}flushGraphics();}
}
