package com.microx.engine.audio;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
public final class AudioManagerTest {
    public static void main(String[] args) {
        Source source = new Source();
        Factory factory = new Factory();
        AudioManager audio = new AudioManager(source, factory);

        audio.enterLocation("cordon");
        eq(0, source.opens, "muted location does not open a resource");
        audio.setVolume(0);
        eq(0, source.opens, "unchanged muted volume does not open a resource");
        audio.setVolume(5);
        eq(1, source.opens, "unmuting makes one start attempt");
        eq(1, factory.creates, "unmuting creates one player");
        audio.setVolume(7);
        eq(1, source.opens, "nonzero volume change does not reopen music");
        eq(1, factory.creates, "nonzero volume change does not restart music");
        eq(70, factory.media.level, "new volume is applied to the open player");
        audio.setVolume(7);
        eq(1, source.opens, "unchanged volume does not reopen music");
        eq(1, factory.creates, "unchanged volume does not recreate player");
        audio.setVolume(0);
        audio.setVolume(3);
        eq(2, source.opens, "unmuting can reload music closed by muting");
        eq(2, factory.creates, "unmuting recreates a player closed by muting");

        Source missing = new Source();
        missing.present = false;
        AudioManager silentLocation = new AudioManager(missing, factory);
        silentLocation.setVolume(4);
        silentLocation.enterLocation("garbage");
        silentLocation.setVolume(6);
        eq(1, missing.opens, "missing music is cached for the location");
        System.out.println("AudioManagerTest OK");
    }
    private static final class Source implements AudioManager.ResourceSource {
        int opens;
        boolean present = true;
        public InputStream open(String resource) {
            opens++;
            return present ? new ByteArrayInputStream(new byte[] {0}) : null;
        }
    }
    private static final class Factory implements AudioManager.MediaFactory {
        int creates;
        FakeMedia media;
        public AudioManager.Media create(InputStream in, String mime) {
            creates++;
            media = new FakeMedia();
            return media;
        }
    }
    private static final class FakeMedia implements AudioManager.Media {
        int level;
        public void realize() {}
        public void setVolume(int value) { level = value; }
        public void setLoopCount(int count) {}
        public void start() {}
        public void close() {}
    }
    private static void eq(int wanted, int got, String label) {
        if (wanted != got)
            throw new AssertionError(label + ": " + got);
    }
}
