package com.microx.engine.assets;

import java.io.InputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/** Manages optional MIDP media only; geometry is loaded by the software renderer pipeline. */
public final class AssetManager {
    private Player music;

    public boolean loadLocation(String name, int volume) {
        unloadLocation();
        if (volume > 0) {
            InputStream stream = getClass().getResourceAsStream("/levels/" + name + "/music.mid");
            if (stream != null) try {
                music = Manager.createPlayer(stream, "audio/midi");
                music.realize();
            } catch (Exception ignored) { music = null; }
        }
        return true;
    }

    public void unloadLocation() {
        if (music != null) { music.close(); music = null; }
    }
}
