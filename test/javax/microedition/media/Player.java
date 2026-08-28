package javax.microedition.media;
public interface Player {
    void realize() throws Exception;
    Object getControl(String type);
    void setLoopCount(int count);
    void start() throws Exception;
    void close();
}
