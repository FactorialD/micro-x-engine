package com.microx.engine.save;
import java.io.*;
import com.microx.engine.gameplay.*;

/** Versioned, checksummed content format. */
public final class SaveCodec {
    public static final int MAGIC = 0x4d585356, FORMAT_VERSION = 4, CONTENT_VERSION = 1;
    private static final int MAX_BYTES = 32768;
    public byte[] encode(SaveData s) throws SaveException {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(raw);
            out.writeInt(MAGIC);
            out.writeShort(FORMAT_VERSION);
            out.writeShort(CONTENT_VERSION);
            out.writeByte(s.slot);
            out.writeInt(s.sequence);
            out.writeLong(s.savedAt);
            out.writeInt(s.seed);
            out.writeUTF(s.location);
            out.writeShort(s.spawn);
            out.writeInt(s.x);
            out.writeInt(s.y);
            out.writeInt(s.z);
            out.writeShort(s.yaw);
            out.writeShort(s.pitch);
            out.writeShort(s.health);
            out.writeShort(s.armor);
            out.writeShort(s.stamina);
            out.writeShort(s.bleeding);
            out.writeShort(s.radiation);
            out.writeByte(s.weapon);
            out.writeShort(s.magazine);
            out.writeByte(s.reserveAmmo.length);
            for (int i = 0; i < s.reserveAmmo.length; i++) out.writeShort(s.reserveAmmo[i]);
            writeGameplay(out, s.gameplay);
            QuestState narrative = s.gameplay.quests;
            out.writeShort(narrative.storyNode());
            out.writeShort(narrative.ending());
            out.writeBoolean(narrative.freeplay());
            out.writeInt(narrative.cyclicSeed());
            out.writeByte(s.entityCount);
            for (int i = 0; i < s.entityCount; i++) {
                out.writeInt(s.entityId[i]);
                out.writeInt(s.entityFlags[i]);
            }
            out.flush();
            byte[] body = raw.toByteArray();
            DataOutputStream checked = new DataOutputStream(raw);
            checked.writeInt(checksum(body, 0, body.length));
            checked.flush();
            return raw.toByteArray();
        } catch (IOException e) {
            throw new SaveException("cannot encode save", e);
        }
    }
    public SaveData decode(byte[] bytes) throws SaveException {
        if (bytes == null || bytes.length < 20 || bytes.length > MAX_BYTES)
            throw new SaveException("invalid save length");
        int stored = readInt(bytes, bytes.length - 4),
            actual = checksum(bytes, 0, bytes.length - 4);
        if (stored != actual)
            throw new SaveException("save checksum mismatch");
        try {
            DataInputStream in =
                    new DataInputStream(new ByteArrayInputStream(bytes, 0, bytes.length - 4));
            if (in.readInt() != MAGIC)
                throw new SaveException("not a MicroX save");
            int format = in.readUnsignedShort(), content = in.readUnsignedShort();
            if (format != FORMAT_VERSION && format != 2 && format != 3)
                throw new SaveException("unsupported save format " + format);
            if (content != CONTENT_VERSION)
                throw new SaveException(
                        "unsupported content version " + content + "; no migration is registered");
            SaveData s = new SaveData();
            s.slot = in.readUnsignedByte();
            s.sequence = in.readInt();
            s.savedAt = in.readLong();
            s.seed = in.readInt();
            s.location = in.readUTF();
            if (s.location.length() < 1 || s.location.length() > SaveData.MAX_LOCATION)
                throw new SaveException("invalid location metadata");
            s.spawn = in.readUnsignedShort();
            s.x = in.readInt();
            s.y = in.readInt();
            s.z = in.readInt();
            s.yaw = in.readShort();
            s.pitch = in.readShort();
            s.health = in.readShort();
            s.armor = in.readShort();
            s.stamina = in.readShort();
            s.bleeding = in.readShort();
            s.radiation = in.readShort();
            s.weapon = in.readUnsignedByte();
            s.magazine = in.readUnsignedShort();
            int ammo = in.readUnsignedByte();
            if (ammo > s.reserveAmmo.length)
                throw new SaveException("too many reserve ammo entries");
            for (int i = 0; i < ammo; i++) s.reserveAmmo[i] = in.readUnsignedShort();
            readGameplay(in, s.gameplay, format);
            if (format >= 3) {
                s.gameplay.quests.setStoryNode(in.readUnsignedShort());
                s.gameplay.quests.setEnding(in.readUnsignedShort());
                s.gameplay.quests.setFreeplay(in.readBoolean());
                s.gameplay.quests.setCyclicSeed(in.readInt());
            }
            s.entityCount = in.readUnsignedByte();
            if (s.entityCount > SaveData.MAX_ENTITY_DELTAS)
                throw new SaveException("too many entity deltas");
            for (int i = 0; i < s.entityCount; i++) {
                s.entityId[i] = in.readInt();
                s.entityFlags[i] = in.readInt();
            }
            if (in.available() != 0)
                throw new SaveException("trailing save content");
            return s;
        } catch (IOException e) {
            throw new SaveException("truncated save", e);
        }
    }
    private void writeGameplay(DataOutputStream out, GameplayState g) throws IOException {
        Inventory b = g.inventory;
        out.writeInt(b.money());
        out.writeByte(b.slots());
        for (int i = 0; i < b.slots(); i++) {
            out.writeShort(b.idAt(i));
            out.writeShort(b.countAt(i));
            out.writeByte(b.durabilityAt(i));
        }
        Equipment e = g.equipment;
        out.writeShort(e.weapon(0));
        out.writeShort(e.weapon(1));
        out.writeShort(e.armor());
        for (int i = 0; i < 5; i++) out.writeShort(e.artifact(i));
        QuestState q = g.quests;
        out.writeByte(q.questCapacity());
        for (int i = 1; i <= q.questCapacity(); i++) out.writeByte(q.state(i));
        out.writeShort(q.objective());
        out.writeShort(q.flagCapacity());
        for (int i = 0; i < q.flagCapacity(); i++)
            if (q.flag(i))
                out.writeShort(i);
        out.writeShort(65535);
        out.writeByte(q.counterCapacity());
        for (int i = 0; i < q.counterCapacity(); i++) out.writeShort(q.counter(i));
        out.writeByte(g.reputation.size());
        for (int i = 1; i <= g.reputation.size(); i++) out.writeShort(g.reputation.get(i));
        out.writeByte(g.containers.size());
        for (int i = 0; i < g.containers.capacity(); i++)
            if (g.containers.occupied(i)) {
                out.writeInt(g.containers.containerAt(i));
                out.writeShort(g.containers.itemAt(i));
                out.writeShort(g.containers.deltaAt(i));
                out.writeByte(g.containers.durabilityAt(i));
            }
        out.writeByte(g.containers.initializedCount());
        for (int i = 0; i < g.containers.initializedCount(); i++)
            out.writeInt(g.containers.initializedAt(i));
    }
    private void readGameplay(DataInputStream in, GameplayState g, int format)
            throws IOException, SaveException {
        g.inventory.clear();
        int money = in.readInt(), slots = in.readUnsignedByte();
        if (money < 0 || slots > g.inventory.slots())
            throw new SaveException("invalid inventory metadata");
        g.inventory.setMoney(money);
        for (int i = 0; i < slots; i++) {
            int id = in.readUnsignedShort(), count = in.readUnsignedShort(),
                condition = in.readUnsignedByte();
            if (id != 0 && !g.inventory.add(id, count, condition))
                throw new SaveException("invalid inventory item");
        }
        Equipment e = g.equipment;
        e.clear();
        e.restore(0, 0, in.readUnsignedShort());
        e.restore(0, 1, in.readUnsignedShort());
        e.restore(1, 0, in.readUnsignedShort());
        for (int i = 0; i < 5; i++) e.restore(2, i, in.readUnsignedShort());
        QuestState q = g.quests;
        q.clear();
        int quests = in.readUnsignedByte();
        if (quests > q.questCapacity())
            throw new SaveException("too many quests");
        for (int i = 1; i <= quests; i++) q.restoreState(i, in.readUnsignedByte());
        q.setObjective(in.readShort());
        int flags = in.readUnsignedShort();
        if (flags > q.flagCapacity())
            throw new SaveException("too many quest flags");
        int flag;
        while ((flag = in.readUnsignedShort()) != 65535) {
            if (flag >= flags)
                throw new SaveException("invalid quest flag");
            q.setFlag(flag, true);
        }
        int counters = in.readUnsignedByte();
        if (counters > q.counterCapacity())
            throw new SaveException("too many counters");
        for (int i = 0; i < counters; i++) q.restoreCounter(i, in.readUnsignedShort());
        int reps = in.readUnsignedByte();
        if (reps > g.reputation.size())
            throw new SaveException("too many factions");
        for (int i = 1; i <= reps; i++) g.reputation.set(i, in.readShort());
        g.containers.clear();
        int deltas = in.readUnsignedByte();
        if (deltas > g.containers.capacity())
            throw new SaveException("too many container deltas");
        for (int i = 0; i < deltas; i++) {
            int container = in.readInt(), item = in.readUnsignedShort(), amount = in.readShort();
            if (format >= 4) {
                int durability = in.readUnsignedByte();
                if (!g.containers.restoreRecord(container, item, amount, durability))
                    throw new SaveException("invalid container record");
            } else if (!g.containers.put(container, item, amount))
                throw new SaveException("invalid legacy container delta");
        }
        if (format >= 4) {
            int initialized = in.readUnsignedByte();
            if (initialized > 32)
                throw new SaveException("too many initialized containers");
            for (int i = 0; i < initialized; i++)
                if (!g.containers.markInitialized(in.readInt()))
                    throw new SaveException("invalid initialized container");
        }
    }
    public static int checksum(byte[] b, int off, int len) {
        int crc = -1;
        for (int i = off; i < off + len; i++) {
            crc ^= b[i] & 255;
            for (int bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) != 0 ? 0xedb88320 : 0);
        }
        return ~crc;
    }
    private int readInt(byte[] b, int p) {
        return ((b[p] & 255) << 24) | ((b[p + 1] & 255) << 16) | ((b[p + 2] & 255) << 8)
                | (b[p + 3] & 255);
    }
}
