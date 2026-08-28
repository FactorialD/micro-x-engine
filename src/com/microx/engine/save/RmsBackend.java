package com.microx.engine.save;
import javax.microedition.rms.*;
/** MIDP RecordStore adapter. */
public final class RmsBackend implements RecordBackend {
    /** Small seam used by host-side tests without a MIDP RecordStore implementation. */
    public interface RmsAdapter {
        RecordBackend open(String name) throws RecordStoreException;
        void delete(String name) throws RecordStoreException;
    }
    private static final class MidpAdapter implements RmsAdapter {
        public RecordBackend open(String name) throws RecordStoreException {
            return new MidpRecords(RecordStore.openRecordStore(name, true));
        }
        public void delete(String name) throws RecordStoreException {
            RecordStore.deleteRecordStore(name);
        }
    }
    private static final class MidpRecords implements RecordBackend {
        private final RecordStore store;
        MidpRecords(RecordStore value) {
            store = value;
        }
        public int add(byte[] b) throws Exception {
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
    private static final RmsAdapter MIDP = new MidpAdapter();
    private final RecordBackend store;
    private final String recoveryStatus;
    public RmsBackend(String name) throws Exception {
        this(name, MIDP, false);
    }
    private RmsBackend(String name, RmsAdapter adapter, boolean confirmedCorrupt) throws Exception {
        RecordBackend opened = null;
        String status = null;
        if (!confirmedCorrupt) {
            opened = adapter.open(name);
        } else {
            try {
                // A damaged store can still open on some RMS implementations. Never delete it
                // while that successfully opened handle remains live.
                opened = adapter.open(name);
            } catch (RecordStoreException ignored) {
            }
            if (opened != null) {
                try {
                    opened.close();
                } catch (Exception ignored) {
                    // Deletion is the authoritative close for an unusable store. A broken
                    // handle must not prevent the one centralized recovery attempt.
                }
                opened = null;
            }
            adapter.delete(name);
            opened = adapter.open(name);
            status = "DATA LOST: confirmed corrupt RecordStore was deleted and recreated: " + name;
        }
        store = opened;
        recoveryStatus = status;
    }
    /**
     * Destructive fallback for a caller that has independently identified structural corruption.
     * MIDP exposes emulator-specific open failures as the same RecordStoreException used for
     * ordinary access errors, so the regular constructor deliberately never deletes data.
     */
    public static RmsBackend recoverCorruptStore(String name) throws Exception {
        return recoverCorruptStore(name, MIDP);
    }
    /** Test seam used by the save recovery coordinator after an unusable store is detected. */
    public static RmsBackend recoverCorruptStore(String name, RmsAdapter adapter) throws Exception {
        return new RmsBackend(name, adapter, true);
    }
    /** Test seam for verifying ordinary open failures remain non-destructive. */
    public static RmsBackend open(String name, RmsAdapter adapter) throws Exception {
        return new RmsBackend(name, adapter, false);
    }
    public String recoveryStatus() {
        return recoveryStatus;
    }
    public int add(byte[] b) throws Exception {
        return store.add(b);
    }
    public byte[] get(int id) throws Exception {
        return store.get(id);
    }
    public int[] ids() throws Exception {
        return store.ids();
    }
    public void close() throws Exception {
        store.close();
    }
}
