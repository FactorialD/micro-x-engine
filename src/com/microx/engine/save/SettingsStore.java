package com.microx.engine.save;
import java.io.*;
import com.microx.engine.ui.UISettings;
/** Small independently checksummed settings record. */
public final class SettingsStore {
    private static final int MAGIC = 0x4d585354;
    private final RecordBackend records;
    private String diagnostic;
    public SettingsStore(RecordBackend b) {
        records = b;
    }
    public void save(UISettings s) throws SaveException {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(raw);
            out.writeInt(MAGIC);
            out.writeByte(2);
            out.writeByte(s.volume);
            out.writeByte(s.resolution);
            out.writeByte(s.controls);
            out.writeBoolean(s.debug);
            out.writeByte(s.sensitivity);
            out.flush();
            byte[] body = raw.toByteArray();
            out.writeInt(SaveCodec.checksum(body, 0, body.length));
            out.flush();
            records.add(raw.toByteArray());
        } catch (Exception e) {
            throw new SaveException("settings write failed: " + e.toString(), e);
        }
    }
    public boolean load(UISettings s) {
        diagnostic = null;
        try {
            int[] ids = records.ids();
            for (int i = ids.length - 1; i >= 0; i--) {
                byte[] b = records.get(ids[i]);
                int version = b.length > 5 ? b[4] & 255 : 0;
                int body = version == 1 ? 9 : version == 2 ? 10 : 0;
                if (body == 0 || b.length != body + 4 || read(b, 0) != MAGIC
                        || SaveCodec.checksum(b, 0, body) != read(b, body))
                    continue;
                s.volume = b[5] & 255;
                s.resolution = b[6] & 255;
                s.controls = b[7] & 255;
                s.debug = b[8] != 0;
                s.sensitivity = version >= 2 ? b[9] & 255 : 5;
                return true;
            }
        } catch (Exception failure) {
            diagnostic = "settings read failed: " + failure.toString();
        }
        return false;
    }
    public String diagnostic() {
        return diagnostic;
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
