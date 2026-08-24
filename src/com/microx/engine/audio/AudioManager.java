package com.microx.engine.audio;
import java.io.InputStream;import javax.microedition.media.*;import javax.microedition.media.control.VolumeControl;
/** Owns MIDI/SFX players. Muting closes media and never opens a resource stream. */
public final class AudioManager {private Player music,sfx;private int volume;
 public void setVolume(int value){volume=value<0?0:value>10?10:value;if(volume==0)release();else{apply(music);apply(sfx);}}
 public int volume(){return volume;}
 public void enterLocation(String name){close(music);music=null;if(volume==0)return;InputStream in=getClass().getResourceAsStream("/levels/"+name+"/music.mid");if(in!=null)try{music=Manager.createPlayer(in,"audio/midi");music.realize();apply(music);music.setLoopCount(-1);music.start();}catch(Exception ignored){close(music);music=null;}}
 public void playSfx(String resource,String mime){close(sfx);sfx=null;if(volume==0)return;InputStream in=getClass().getResourceAsStream(resource);if(in!=null)try{sfx=Manager.createPlayer(in,mime);sfx.realize();apply(sfx);sfx.start();}catch(Exception ignored){close(sfx);sfx=null;}}
 public void leaveLocation(){close(music);music=null;close(sfx);sfx=null;}public void release(){leaveLocation();}
 private void apply(Player p){if(p==null)return;try{VolumeControl c=(VolumeControl)p.getControl("VolumeControl");if(c!=null)c.setLevel(volume*10);}catch(Exception ignored){}}
 private void close(Player p){if(p!=null)p.close();}
}
