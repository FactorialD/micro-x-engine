package javax.microedition.rms;
public interface RecordEnumeration {
    boolean hasNextElement();
    int nextRecordId();
    void destroy();
}
