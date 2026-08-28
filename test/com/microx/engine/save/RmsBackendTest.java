package com.microx.engine.save;
import javax.microedition.rms.RecordStoreException;
import java.util.Vector;
public final class RmsBackendTest {
    private static final class Records implements RecordBackend {
        boolean closed;
        final Vector data = new Vector();
        public int add(byte[] value) {
            data.addElement(value);
            return data.size();
        }
        public int[] ids() {
            int[] ids = new int[data.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = i + 1;
            return ids;
        }
        public byte[] get(int id) {
            return (byte[]) data.elementAt(id - 1);
        }
        public void close() {
            closed = true;
        }
    }
    private static final class Adapter implements RmsBackend.RmsAdapter {
        Records existing = new Records(), recreated;
        boolean failOpen, deleted, openedAfterDelete;
        public RecordBackend open(String name) throws RecordStoreException {
            if (deleted) {
                openedAfterDelete = true;
                recreated = new Records();
                return recreated;
            }
            if (failOpen)
                throw new RecordStoreException("access denied or corrupt");
            return existing;
        }
        public void delete(String name) {
            deleted = true;
        }
    }
    public static void main(String[] args) throws Exception {
        Adapter normal = new Adapter();
        RmsBackend opened = RmsBackend.open("normal", normal);
        check(!normal.deleted, "normal open deleted data");
        opened.close();

        Adapter denied = new Adapter();
        denied.failOpen = true;
        try {
            RmsBackend.open("denied", denied);
            throw new AssertionError("access failure hidden");
        } catch (RecordStoreException expected) {
        }
        check(!denied.deleted, "access failure deleted data");

        Adapter corrupt = new Adapter();
        RmsBackend recovered = RmsBackend.recoverCorruptStore("corrupt", corrupt);
        check(corrupt.existing.closed, "open corrupt handle was not closed");
        check(corrupt.deleted && corrupt.openedAfterDelete, "store was not recreated");
        check(recovered.recoveryStatus().indexOf("DATA LOST") >= 0, "diagnostic missing");
        check(corrupt.recreated.data.size() == 0, "old records survived recovery");
        SaveStore clean = new SaveStore(recovered);
        try {
            clean.load(0);
            throw new AssertionError("recreated store was not empty");
        } catch (SaveException expected) {
            check(expected.isEmptySlot(), "recreated store reported corruption");
        }
        SaveData fresh = new SaveData();
        fresh.sequence = 1;
        fresh.seed = 123;
        clean.save(fresh);
        check(clean.load(0).seed == 123, "new game did not round trip after recovery");

        Adapter brokenOpen = new Adapter();
        brokenOpen.failOpen = true;
        RmsBackend recreated = RmsBackend.recoverCorruptStore("corrupt", brokenOpen);
        check(brokenOpen.deleted && brokenOpen.openedAfterDelete, "failed open not recreated");
        recreated.close();
    }
    private static void check(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }
}
