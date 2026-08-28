package javax.microedition.rms;
public class RecordStore {
    public static RecordStore openRecordStore(String name, boolean create)
            throws RecordStoreException { throw new RecordStoreException("host stub"); }
    public static void deleteRecordStore(String name) throws RecordStoreException {}
    public int addRecord(byte[] data, int offset, int length) throws RecordStoreException { return 0; }
    public byte[] getRecord(int id) throws RecordStoreException { return null; }
    public int getNumRecords() throws RecordStoreException { return 0; }
    public RecordEnumeration enumerateRecords(Object filter, Object comparator, boolean keepUpdated)
            throws RecordStoreException { return null; }
    public void closeRecordStore() throws RecordStoreException {}
}
