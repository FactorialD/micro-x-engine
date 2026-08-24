package com.microx.engine.assets;
import java.io.*; import javax.microedition.m3g.*; import javax.microedition.media.*;
public final class AssetManager {
 private Object3D[] objects; private Player music; private String location;
 public boolean loadLocation(String name,int volume){unloadLocation();location=name;try{objects=Loader.load("/levels/"+name+"/world.m3g");}catch(Exception e){objects=null;}if(volume>0){try{music=Manager.createPlayer(getClass().getResourceAsStream("/levels/"+name+"/music.mid"),"audio/midi");music.realize();}catch(Exception e){music=null;}}return objects!=null;}
 public Object3D[] objects(){return objects;}
 public void unloadLocation(){if(music!=null){music.close();music=null;}objects=null;location=null;}
}
