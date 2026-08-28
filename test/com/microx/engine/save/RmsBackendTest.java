package com.microx.engine.save;
import javax.microedition.rms.RecordStoreException;
public final class RmsBackendTest {
    private static final class Records implements RecordBackend {
        boolean closed;
        public int add(byte[] data) { return 1; }
        public int[] ids() { return new int[0]; }
        public byte[] get(int id) { return null; }
        public void close() { closed = true; }
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
            if (failOpen) throw new RecordStoreException("access denied or corrupt");
            return existing;
        }
        public void delete(String name) { deleted = true; }
    }
    public static void main(String[] args) throws Exception {
        Adapter normal = new Adapter();
        RmsBackend opened = RmsBackend.open("normal", normal);
        check(!normal.deleted, "normal open deleted data");
        opened.close();

        Adapter denied = new Adapter();
        denied.failOpen = true;
        try { RmsBackend.open("denied", denied); throw new AssertionError("access failure hidden"); }
        catch (RecordStoreException expected) {}
        check(!denied.deleted, "access failure deleted data");

        Adapter corrupt = new Adapter();
        RmsBackend recovered = RmsBackend.recoverCorruptStore("corrupt", corrupt);
        check(corrupt.existing.closed, "open corrupt handle was not closed");
        check(corrupt.deleted && corrupt.openedAfterDelete, "store was not recreated");
        check(recovered.recoveryStatus().indexOf("DATA LOST") >= 0, "diagnostic missing");

        Adapter brokenOpen = new Adapter();
        brokenOpen.failOpen = true;
        RmsBackend recreated = RmsBackend.recoverCorruptStore("corrupt", brokenOpen);
        check(brokenOpen.deleted && brokenOpen.openedAfterDelete, "failed open not recreated");
        recreated.close();
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
