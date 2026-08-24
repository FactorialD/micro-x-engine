package com.microx.engine;
import javax.microedition.lcdui.*;
public final class GameCanvas3D extends GameCanvas {
 private final Engine engine; private final boolean debug;
 public GameCanvas3D(Engine e,boolean d){super(false);engine=e;debug=d;e.attach(this);setFullScreenMode(true);}
 protected void keyPressed(int key){if(key==-7){engine.togglePause();return;}engine.input.key(key,true,System.currentTimeMillis());}
 protected void keyReleased(int key){engine.input.key(key,false,System.currentTimeMillis());}
 public void renderFrame(){Graphics g=getGraphics();if(engine.level!=null){engine.renderer.render(g,engine.player,engine.level.world);engine.stats.submittedTriangles=engine.renderer.submittedTriangles();engine.stats.clippedTriangles=engine.renderer.clippedTriangles();engine.stats.drawnTriangles=engine.renderer.drawnTriangles();engine.hud.paint(g,engine.player,engine.stats.fps,engine.stats.entities,engine.stats.rooms,debug);}flushGraphics();}
}
