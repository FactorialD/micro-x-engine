package com.microx.engine.world;

import java.io.IOException;
import java.io.InputStream;
import com.microx.engine.math.Fixed;

/** Loads the human-readable, whitespace separated MXL1 level format. */
public final class LevelLoader {
    public PortalWorld world;
    public EntityPool entities;
    public int startX, startY, startZ;

    public boolean load(String path) {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream(path);
            if (stream == null) return false;
            TokenReader in = new TokenReader(stream);
            if (!"MXL1".equals(in.next())) return false;
            in.expect("rooms"); int roomCount = in.nextInt();
            in.expect("portals"); int portalCount = in.nextInt();
            in.expect("capacity"); int capacity = in.nextInt();
            if (roomCount <= 0 || portalCount < 0 || capacity <= 0) return false;

            world = new PortalWorld(roomCount, portalCount);
            int i;
            for (i = 0; i < roomCount; i++) {
                in.expect("room");
                world.room(i, Fixed.fromInt(in.nextInt()), Fixed.fromInt(in.nextInt()),
                        Fixed.fromInt(in.nextInt()), Fixed.fromInt(in.nextInt()));
            }
            for (i = 0; i < portalCount; i++) {
                in.expect("portal");
                int from = in.nextInt(), to = in.nextInt();
                if (from < 0 || from >= roomCount || to < 0 || to >= roomCount) return false;
                world.portal(i, from, to);
            }
            in.expect("start");
            startX = Fixed.fromInt(in.nextInt());
            startY = Fixed.fromInt(in.nextInt());
            startZ = Fixed.fromInt(in.nextInt());
            entities = new EntityPool(capacity);
            in.expect("entities"); int entityCount = in.nextInt();
            if (entityCount < 0 || entityCount > capacity) return false;
            for (i = 0; i < entityCount; i++) {
                in.expect("entity");
                if (entities.spawn(in.nextInt(), Fixed.fromInt(in.nextInt()),
                        Fixed.fromInt(in.nextInt()), Fixed.fromInt(in.nextInt()),
                        in.nextInt()) < 0) return false;
            }
            return true;
        } catch (IOException e) {
            clear(); return false;
        } catch (NumberFormatException e) {
            clear(); return false;
        } finally {
            if (stream != null) try { stream.close(); } catch (IOException ignored) {}
        }
    }

    private void clear() { world = null; entities = null; }

    /** Tiny loader-only tokenizer; comments begin with '#'. */
    private static final class TokenReader {
        private final InputStream in;
        private final StringBuffer token = new StringBuffer(16);
        TokenReader(InputStream in) { this.in = in; }
        String next() throws IOException {
            token.setLength(0);
            int c;
            do {
                c = in.read();
                if (c == '#') while (c >= 0 && c != '\n') c = in.read();
            } while (c >= 0 && c <= ' ');
            if (c < 0) throw new IOException("Unexpected end of level");
            while (c > ' ' && c != '#') { token.append((char)c); c = in.read(); }
            if (c == '#') while (c >= 0 && c != '\n') c = in.read();
            return token.toString();
        }
        int nextInt() throws IOException { return Integer.parseInt(next()); }
        void expect(String expected) throws IOException {
            if (!expected.equals(next())) throw new IOException("Expected " + expected);
        }
    }
}
