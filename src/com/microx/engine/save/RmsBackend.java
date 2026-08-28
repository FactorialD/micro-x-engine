package com.microx.engine.save;
import javax.microedition.rms.*;
/** MIDP RecordStore adapter. */
public final class RmsBackend implements RecordBackend {
    private final RecordStore store;
    private final String recoveryStatus;
    public RmsBackend(String name) throws RecordStoreException {
        store = RecordStore.openRecordStore(name, true);
        recoveryStatus = null;
    }
    private RmsBackend(String name, boolean confirmedCorrupt) throws RecordStoreException {
        RecordStore opened = null;
        String status = null;
        try {
            opened = RecordStore.openRecordStore(name, true);
        } catch (RecordStoreException failure) {
            if (!confirmedCorrupt)
                throw failure;
            if (opened != null)
                try {
                    opened.closeRecordStore();
                } catch (RecordStoreException ignored) {
                }
            RecordStore.deleteRecordStore(name);
            opened = RecordStore.openRecordStore(name, true);
            status = "DATA LOST: corrupt RecordStore was deleted and recreated: "
                    + failure.toString();
        }
        store = opened;
        recoveryStatus = status;
    }
    /**
     * Destructive fallback for a caller that has independently identified structural corruption.
     * MIDP exposes emulator-specific open failures as the same RecordStoreException used for
     * ordinary access errors, so the regular constructor deliberately never deletes data.
     */
    public static RmsBackend recoverCorruptStore(String name) throws RecordStoreException {
        return new RmsBackend(name, true);
    }
    public String recoveryStatus() {
        return recoveryStatus;
    }
    public int add(byte[] b) throws RecordStoreException {
        return store.addRecord(b, 0, b.length);
    }
    public byte[] get(int id) throws RecordStoreException {
        return store.getRecord(id);
    }
    public int[] ids() throws RecordStoreException {
        int[] a = new int[store.getNumRecords()];
        int n = 0;
        RecordEnumeration e = store.enumerateRecords(null, null, false);
        while (e.hasNextElement()) a[n++] = e.nextRecordId();
        e.destroy();
        if (n == a.length)
            return a;
        int[] exact = new int[n];
        System.arraycopy(a, 0, exact, 0, n);
        return exact;
    }
    public void close() throws RecordStoreException {
        store.closeRecordStore();
    }
}
