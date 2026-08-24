package com.microx.engine.save;
import java.util.Vector;
public final class SaveStoreTest {
    private static final class Memory implements RecordBackend {
        final Vector data = new Vector();
        public int add(byte[] b) {
            data.addElement(b);
            return data.size();
        }
        public int[] ids() {
            int[] r = new int[data.size()];
            for (int i = 0; i < r.length; i++) r[i] = i + 1;
            return r;
        }
        public byte[] get(int id) {
            return (byte[]) data.elementAt(id - 1);
        }
        public void close() {}
        void corrupt(int id) {
            byte[] b = get(id);
            b[b.length - 1] ^= 1;
        }
    }
    public static void main(String[] args) throws Exception {
        Memory m = new Memory();
        SaveStore store = new SaveStore(m);
        SaveData s = new SaveData();
        s.slot = 0;
        s.sequence = 1;
        s.seed = 42;
        s.x = 100;
        s.gameplay.inventory.add(1, 1, 88);
        s.gameplay.quests.setFlag(7, true);
        s.gameplay.reputation.set(1, -20);
        s.gameplay.containers.put(99, 1, -1);
        s.addEntityDelta(77, 3);
        store.save(s);
        s.sequence = 2;
        s.x = 200;
        store.save(s);
        eq(200, store.load(0).x, "newest");
        m.corrupt(3);
        eq(100, store.load(0).x, "fallback after corrupt prepared record");
        int before = m.data.size();
        m.add(new byte[] {1, 2, 3});
        eq(100, store.load(0).x, "interrupted prepare is ignored");
        if (m.data.size() != before + 1)
            throw new AssertionError();
        System.out.println("SaveStoreTest OK");
    }
    private static void eq(int expected, int actual, String label) {
        if (expected != actual)
            throw new AssertionError(label + ": " + actual);
    }
}
