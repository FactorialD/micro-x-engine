package com.microx.engine.audio;
import java.io.InputStream;
import javax.microedition.media.*;
import javax.microedition.media.control.VolumeControl;
/** Owns MIDI/SFX players. Muting closes media and never opens a resource stream. */
public final class AudioManager {
    public interface ResourceSource {
        InputStream open(String resource);
    }
    public interface Media {
        void realize() throws Exception;
        void setVolume(int level) throws Exception;
        void setLoopCount(int count) throws Exception;
        void start() throws Exception;
        void close();
    }
    public interface MediaFactory {
        Media create(InputStream in, String mime) throws Exception;
    }
    private static final class MidpMedia implements Media {
        private final Player player;
        MidpMedia(Player player) {
            this.player = player;
        }
        public void realize() throws Exception {
            player.realize();
        }
        public void setVolume(int level) {
            VolumeControl control = (VolumeControl) player.getControl("VolumeControl");
            if (control != null)
                control.setLevel(level);
        }
        public void setLoopCount(int count) {
            player.setLoopCount(count);
        }
        public void start() throws Exception {
            player.start();
        }
        public void close() {
            player.close();
        }
    }
    private static final class DefaultFactory implements MediaFactory {
        public Media create(InputStream in, String mime) throws Exception {
            return new MidpMedia(Manager.createPlayer(in, mime));
        }
    }
    private final ResourceSource resources;
    private final MediaFactory mediaFactory;
    private Media music, sfx;
    private int volume;
    private String musicLocation;
    private boolean musicChecked;
    public AudioManager() {
        resources = new ResourceSource() {
            public InputStream open(String resource) {
                return AudioManager.class.getResourceAsStream(resource);
            }
        };
        mediaFactory = new DefaultFactory();
    }
    public AudioManager(ResourceSource resources, MediaFactory mediaFactory) {
        this.resources = resources;
        this.mediaFactory = mediaFactory;
    }
    /** Applies a setting change without restarting media that is already playing. */
    public void setVolume(int value) {
        int next = value < 0 ? 0 : value > 10 ? 10 : value;
        if (next == volume)
            return;
        int previous = volume;
        volume = next;
        if (volume == 0) {
            if (music != null)
                musicChecked = false;
            close(music);
            music = null;
            close(sfx);
            sfx = null;
        } else {
            apply(music);
            apply(sfx);
            if (previous == 0 && music == null && musicLocation != null && !musicChecked)
                loadMusic();
        }
    }
    public int volume() {
        return volume;
    }
    public void enterLocation(String name) {
        close(music);
        music = null;
        musicLocation = name;
        musicChecked = false;
        if (volume != 0)
            loadMusic();
    }
    private void loadMusic() {
        musicChecked = true;
        InputStream in = resources.open("/levels/" + musicLocation + "/music.mid");
        if (in != null)
            try {
                music = mediaFactory.create(in, "audio/midi");
                music.realize();
                apply(music);
                music.setLoopCount(-1);
                music.start();
            } catch (Exception ignored) {
                close(music);
                music = null;
            }
    }
    public void playSfx(String resource, String mime) {
        close(sfx);
        sfx = null;
        if (volume == 0)
            return;
        InputStream in = resources.open(resource);
        if (in != null)
            try {
                sfx = mediaFactory.create(in, mime);
                sfx.realize();
                apply(sfx);
                sfx.start();
            } catch (Exception ignored) {
                close(sfx);
                sfx = null;
            }
    }
    public void leaveLocation() {
        close(music);
        music = null;
        musicLocation = null;
        musicChecked = false;
        close(sfx);
        sfx = null;
    }
    public void release() {
        leaveLocation();
    }
    private void apply(Media media) {
        if (media == null)
            return;
        try {
            media.setVolume(volume * 10);
        } catch (Exception ignored) {
        }
    }
    private void close(Media media) {
        if (media != null)
            media.close();
    }
}
