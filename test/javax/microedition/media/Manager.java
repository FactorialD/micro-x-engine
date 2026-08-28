package javax.microedition.media;
import java.io.InputStream;
public final class Manager {
    private Manager() {}
    public static Player createPlayer(InputStream in, String mime) throws Exception {
        throw new Exception("MIDP media is unavailable in desktop tests");
    }
}
