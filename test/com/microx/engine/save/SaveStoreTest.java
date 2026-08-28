package com.microx.engine.save;
import java.util.Vector;
import com.microx.engine.gameplay.ItemCatalog;
import com.microx.engine.ui.UISettings;
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
    private static final class Failing implements RecordBackend {
        public int add(byte[] b) throws Exception {
            throw new Exception("disk full");
        }
        public int[] ids() throws Exception {
            throw new Exception("store unavailable");
        }
        public byte[] get(int id) throws Exception {
            throw new Exception("store unavailable");
        }
        public void close() {}
    }
    public static void main(String[] args) throws Exception {
        if (!ItemCatalog.loadDefault())
            throw new AssertionError("gameplay catalog unavailable");
        codecValidation();
        unavailableStoresAreIndependent();
        emptySlotIsNotCorruption();
        committedFailuresAreUnusable();
        Memory m = new Memory();
        SaveStore store = new SaveStore(m);
        SaveData s = new SaveData();
        s.slot = 0;
        s.sequence = 1;
        s.seed = 42;
        s.x = 100;
        s.gameplay.inventory.add(1, 1, 88);
        s.gameplay.inventory.add(2, 1, 70);
        s.gameplay.equipment.equip(s.gameplay.inventory, 2, 0);
        s.reserveAmmo[1] = 120;
        s.gameplay.quests.setFlag(7, true);
        s.gameplay.quests.setStoryNode(3);
        s.gameplay.quests.setEnding(2);
        s.gameplay.quests.setCyclicSeed(99);
        s.gameplay.reputation.set(1, -20);
        com.microx.engine.gameplay.Inventory chest =
                new com.microx.engine.gameplay.Inventory(24, 40);
        chest.add(2, 1, 43);
        s.gameplay.containers.capture(31012, chest);
        s.addEntityDelta(77, 3);
        store.save(s);
        s.sequence = 2;
        s.x = 200;
        store.save(s);
        SaveData newest = store.load(0);
        eq(200, newest.x, "newest");
        eq(2, newest.gameplay.equipment.weapon(0), "equipment restored");
        eq(120, newest.reserveAmmo[1], "reserve ammo restored");
        eq(3, newest.gameplay.quests.storyNode(), "story restored");
        eq(2, newest.gameplay.quests.ending(), "ending restored");
        eq(99, newest.gameplay.quests.cyclicSeed(), "cyclic seed restored");
        chest.clear();
        if (!newest.gameplay.containers.restore(31012, chest))
            throw new AssertionError();
        eq(43, chest.conditionOf(2), "container durability restored");
        m.corrupt(3);
        SaveData fallback = store.load(0);
        eq(100, fallback.x, "fallback after corrupt prepared record");
        if (!fallback.recovered)
            throw new AssertionError("fallback source not reported");
        chest.clear();
        if (!fallback.gameplay.containers.restore(31012, chest))
            throw new AssertionError();
        eq(1, chest.count(2), "fallback container has no duplicated item");
        eq(43, chest.conditionOf(2), "fallback container durability");
        int before = m.data.size();
        m.add(new byte[] {1, 2, 3});
        eq(100, store.load(0).x, "interrupted prepare is ignored");
        if (m.data.size() != before + 1)
            throw new AssertionError();
        System.out.println("SaveStoreTest OK");
    }
    private static void emptySlotIsNotCorruption() throws Exception {
        try {
            new SaveStore(new Memory()).load(0);
            throw new AssertionError("empty slot loaded");
        } catch (SaveException expected) {
            if (!expected.isEmptySlot())
                throw new AssertionError("empty slot reported as corruption");
        }
    }
    private static void committedFailuresAreUnusable() throws Exception {
        try {
            new SaveStore(new Failing()).load(0);
            throw new AssertionError("record access failure loaded");
        } catch (SaveException expected) {
            if (expected.isEmptySlot() || expected.getMessage().indexOf("RecordStore read") < 0)
                throw new AssertionError("record access failure signal lost");
        }
        Memory checksum = savedMemory();
        checksum.corrupt(1);
        unusable(checksum, "checksum");

        Memory metadata = savedMemory();
        metadata.get(2)[9] = 0x7f;
        unusable(metadata, "metadata");

        Memory decode = savedMemory();
        decode.get(1)[13] = 0;
        int length = readInt(decode.get(1), 9);
        int sum = SaveCodec.checksum(decode.get(1), 13, length);
        writeInt(decode.get(1), 13 + length, sum);
        writeInt(decode.get(2), 13, sum);
        unusable(decode, "decode");

        Memory malformedCommit = savedMemory();
        byte[] commit = malformedCommit.get(2);
        byte[] shortCommit = new byte[commit.length - 1];
        System.arraycopy(commit, 0, shortCommit, 0, shortCommit.length);
        malformedCommit.data.setElementAt(shortCommit, 1);
        unusable(malformedCommit, "commit structure");
    }
    private static Memory savedMemory() throws Exception {
        Memory memory = new Memory();
        SaveData data = new SaveData();
        data.sequence = 1;
        data.seed = 9;
        new SaveStore(memory).save(data);
        return memory;
    }
    private static void unusable(Memory memory, String label) throws Exception {
        try {
            new SaveStore(memory).load(0);
            throw new AssertionError(label + " failure loaded");
        } catch (SaveException expected) {
            if (expected.isEmptySlot())
                throw new AssertionError(label + " failure reported as empty");
        }
    }
    private static int readInt(byte[] b, int p) {
        return ((b[p] & 255) << 24) | ((b[p + 1] & 255) << 16) | ((b[p + 2] & 255) << 8)
                | (b[p + 3] & 255);
    }
    private static void writeInt(byte[] b, int p, int value) {
        b[p] = (byte) (value >>> 24);
        b[p + 1] = (byte) (value >>> 16);
        b[p + 2] = (byte) (value >>> 8);
        b[p + 3] = (byte) value;
    }
    private static void unavailableStoresAreIndependent() throws Exception {
        SaveStore unavailableSaves = new SaveStore(new Failing());
        try {
            unavailableSaves.save(new SaveData());
            throw new AssertionError("failed save accepted");
        } catch (SaveException expected) {
            if (expected.getMessage().indexOf("disk full") < 0)
                throw new AssertionError("write cause missing from diagnostic");
        }
        Memory settingsRecords = new Memory();
        SettingsStore settings = new SettingsStore(settingsRecords);
        UISettings written = new UISettings();
        written.volume = 37;
        settings.save(written);
        UISettings loaded = new UISettings();
        if (!settings.load(loaded) || loaded.volume != 37)
            throw new AssertionError("settings depend on failed save backend");

        SettingsStore unavailableSettings = new SettingsStore(new Failing());
        if (unavailableSettings.load(new UISettings()) || unavailableSettings.diagnostic() == null)
            throw new AssertionError("settings read failure has no diagnostic");
    }
    private static void codecValidation() throws Exception {
        SaveCodec codec = new SaveCodec();
        SaveData data = new SaveData();
        data.location = "laboratory";
        data.spawn = 70;
        data.yaw = 90;
        codec.decode(codec.encode(data));
        data.location = "missing_level";
        rejected(codec, data, "unknown location");
        data.location = "laboratory";
        data.spawn = 71;
        rejected(codec, data, "unknown spawn");
        data.spawn = 70;
        data.x = Integer.MIN_VALUE;
        rejected(codec, data, "fixed coordinate overflow");
        data.x = 0;
        data.pitch = 90;
        rejected(codec, data, "pitch outside camera range");
    }
    private static void rejected(SaveCodec codec, SaveData data, String label) throws Exception {
        try {
            codec.decode(codec.encode(data));
            throw new AssertionError(label + " accepted");
        } catch (SaveException expected) {
        }
    }
    private static void eq(int expected, int actual, String label) {
        if (expected != actual)
            throw new AssertionError(label + ": " + actual);
    }
}
