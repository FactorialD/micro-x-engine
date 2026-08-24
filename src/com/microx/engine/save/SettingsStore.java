package com.microx.engine.save;
import java.io.*;
import com.microx.engine.ui.UISettings;
/** Small independently checksummed settings record. */
public final class SettingsStore {
    private static final int MAGIC = 0x4d585354;
    private final RecordBackend records;
    public SettingsStore(RecordBackend b) {
        records = b;
    }
    public void save(UISettings s) throws SaveException {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(raw);
            out.writeInt(MAGIC);
            out.writeByte(1);
            out.writeByte(s.volume);
            out.writeByte(s.resolution);
            out.writeByte(s.controls);
            out.writeBoolean(s.debug);
            out.flush();
            byte[] body = raw.toByteArray();
            out.writeInt(SaveCodec.checksum(body, 0, body.length));
            out.flush();
            records.add(raw.toByteArray());
        } catch (Exception e) {
            throw new SaveException("settings write failed", e);
        }
    }
    public boolean load(UISettings s) {
        try {
            int[] ids = records.ids();
            for (int i = ids.length - 1; i >= 0; i--) {
                byte[] b = records.get(ids[i]);
                if (b.length != 13 || read(b, 0) != MAGIC
                        || SaveCodec.checksum(b, 0, 9) != read(b, 9))
                    continue;
                s.volume = b[5] & 255;
                s.resolution = b[6] & 255;
                s.controls = b[7] & 255;
                s.debug = b[8] != 0;
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
    public void close() {
        try {
            records.close();
        } catch (Exception ignored) {
        }
    }
    private int read(byte[] b, int p) {
        return ((b[p] & 255) << 24) | ((b[p + 1] & 255) << 16) | ((b[p + 2] & 255) << 8)
                | (b[p + 3] & 255);
    }
}
