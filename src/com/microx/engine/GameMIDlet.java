package com.microx.engine;
import javax.microedition.midlet.MIDlet; import javax.microedition.lcdui.*;
public final class GameMIDlet extends MIDlet {
 private Engine engine; private GameCanvas3D canvas;
 protected void startApp(){if(engine==null){engine=new Engine();canvas=new GameCanvas3D(engine,"true".equals(getAppProperty("MicroX-Debug")));Display.getDisplay(this).setCurrent(canvas);canvas.showInitial();}else engine.resume();}
 protected void pauseApp(){if(engine!=null)engine.pause();}
 protected void destroyApp(boolean unconditional){if(engine!=null)engine.shutdown();engine=null;canvas=null;}
}
