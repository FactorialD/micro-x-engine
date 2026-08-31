package com.microx.engine.world;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;
import com.microx.engine.gameplay.GameIds;

/** Streaming loader for the authored UTF-8 MXL2 level text. Publishes only complete levels. */
public final class LevelLoader {
    public PortalWorld world;
    public Collision collision;
    public EntityPool entities;
    public int startX, startY, startZ, startYaw;
    public int skyColor, wallColor, floorColor;
    public int[] transitionId, transitionSpawn, spawnId, spawnX, spawnY, spawnZ, spawnYaw;
    public String[] transitionLocation;
    public boolean load(String path) {
        InputStream in = getClass().getResourceAsStream(path);
        return in != null && load(in);
    }
    public boolean load(InputStream stream) {
        try {
            Tokens in = new Tokens(stream);
            in.expect("MXL2");
            in.expect("environment");
            int sky = in.color(), wall = in.color(), floorColorValue = in.color();
            if (sky == wall || sky == floorColorValue || wall == floorColorValue)
                return false;
            in.expect("counts");
            int rooms = in.count(1, 256), floors = in.count(1, 1024), ceilings = in.count(1, 1024),
                edges = in.count(0, 2048), portals = in.count(0, 1024), spawns = in.count(1, 256),
                transitions = in.count(0, 256), entityCount = in.count(0, 1024),
                capacity = in.count(1, 1024);
            if (entityCount > capacity)
                return false;
            PortalWorld w = new PortalWorld(rooms, portals);
            Collision c = new Collision(edges, floors, ceilings);
            EntityPool e = new EntityPool(capacity);
            int i;
            for (i = 0; i < rooms; i++) {
                in.expect("room");
                int a = in.fixed(), b = in.fixed(), d = in.fixed(), f = in.fixed();
                if (a > b || d > f)
                    return false;
                w.room(i, a, b, d, f);
            }
            for (i = 0; i < floors; i++) {
                in.expect("floor");
                int r = in.index(rooms), a = in.fixed(), b = in.fixed(), d = in.fixed(),
                    f = in.fixed(), y = in.fixed();
                if (a > b || d > f)
                    return false;
                c.floor(i, r, a, b, d, f, y);
            }
            for (i = 0; i < ceilings; i++) {
                in.expect("ceiling");
                int r = in.index(rooms), a = in.fixed(), b = in.fixed(), d = in.fixed(),
                    f = in.fixed(), y = in.fixed();
                if (a > b || d > f)
                    return false;
                c.ceiling(i, r, a, b, d, f, y);
            }
            for (i = 0; i < edges; i++) {
                in.expect("edge");
                c.edge(i, in.index(rooms), in.fixed(), in.fixed(), in.fixed(), in.fixed(),
                        in.fixed(), in.fixed());
            }
            int[] reverse = new int[portals];
            for (i = 0; i < portals; i++) {
                in.expect("portal");
                int id = in.id(), from = in.index(rooms), to = in.index(rooms), a = in.fixed(),
                    b = in.fixed(), d = in.fixed(), f = in.fixed(), g = in.fixed(), h = in.fixed(),
                    rev = in.signedIndex(portals), transition = in.signedIndex(transitions);
                if (a > b || d > f || g > h)
                    return false;
                reverse[i] = rev;
                w.portal(i, id, from, to, a, b, d, f, g, h, rev, transition);
            }
            for (i = 0; i < portals; i++)
                if (reverse[i] >= 0 && reverse[reverse[i]] != i)
                    return false;
            int sx = 0, sy = 0, sz = 0, syaw = 0;
            int[] si = new int[spawns], sxx = new int[spawns], syy = new int[spawns],
                  szz = new int[spawns], sya = new int[spawns];
            for (i = 0; i < spawns; i++) {
                in.expect("spawn");
                int id = in.id();
                in.index(rooms);
                int x = in.fixed(), y = in.fixed(), z = in.fixed(), yaw = in.range(-32768, 32767);
                si[i] = id;
                sxx[i] = x;
                syy[i] = y;
                szz[i] = z;
                sya[i] = yaw;
                if (id == 0) {
                    sx = x;
                    sy = y;
                    sz = z;
                    syaw = yaw;
                }
            }
            int[] ti = new int[transitions], ts = new int[transitions];
            String[] tl = new String[transitions];
            for (i = 0; i < transitions; i++) {
                in.expect("transition");
                ti[i] = in.id();
                ts[i] = in.id();
                tl[i] = in.location();
            }
            for (i = 0; i < entityCount; i++) {
                in.expect("entity");
                int entity = e.spawn(in.id(), in.id(), in.fixed(), in.fixed(), in.fixed(), in.id());
                if (entity < 0)
                    return false;
                e.faction[entity] = in.id();
                e.spriteId[entity] = in.id();
                e.aux[entity] = in.id();
                e.trader[entity] = e.type[entity] == EntityPool.HUMAN
                        && (e.aux[entity] == GameIds.NPC_SIDOROVICH
                                || e.aux[entity] == GameIds.NPC_TECHNICIAN);
            }
            if (in.hasNext())
                return false;
            skyColor = sky;
            wallColor = wall;
            floorColor = floorColorValue;
            world = w;
            collision = c;
            entities = e;
            startX = sx;
            startY = sy;
            startZ = sz;
            startYaw = syaw;
            transitionId = ti;
            transitionSpawn = ts;
            transitionLocation = tl;
            spawnId = si;
            spawnX = sxx;
            spawnY = syy;
            spawnZ = szz;
            spawnYaw = sya;
            return true;
        } catch (IOException ex) {
            return false;
        } catch (RuntimeException ex) {
            return false;
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }
    private static final class Tokens {
        private final Vector values = new Vector();
        private int position;
        Tokens(InputStream stream) throws IOException {
            Reader reader = new InputStreamReader(stream, "UTF-8");
            StringBuffer token = new StringBuffer();
            boolean comment = false;
            int ch;
            while ((ch = reader.read()) >= 0) {
                if (comment) {
                    if (ch == '\n' || ch == '\r')
                        comment = false;
                } else if (ch == '#') {
                    add(token);
                    comment = true;
                } else if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                    add(token);
                } else
                    token.append((char) ch);
            }
            add(token);
        }
        private void add(StringBuffer token) {
            if (token.length() > 0) {
                values.addElement(token.toString());
                token.setLength(0);
            }
        }
        boolean hasNext() {
            return position < values.size();
        }
        String next() throws IOException {
            if (!hasNext())
                throw new IOException("unexpected end of level");
            return (String) values.elementAt(position++);
        }
        void expect(String expected) throws IOException {
            if (!expected.equals(next()))
                throw new IOException("unexpected level record");
        }
        int number() throws IOException {
            try {
                return Integer.parseInt(next());
            } catch (NumberFormatException invalid) {
                throw new IOException("invalid integer");
            }
        }
        int range(int low, int high) throws IOException {
            int value = number();
            if (value < low || value > high)
                throw new IOException("value out of range");
            return value;
        }
        int count(int low, int high) throws IOException {
            return range(low, high);
        }
        int index(int count) throws IOException {
            return range(0, count - 1);
        }
        int signedIndex(int count) throws IOException {
            return range(-1, count - 1);
        }
        int id() throws IOException {
            return range(0, 65535);
        }
        int fixed() throws IOException {
            return range(-32767, 32767) * 65536;
        }
        int color() throws IOException {
            String value = next();
            if (value.length() != 6)
                throw new IOException("invalid RGB color");
            try {
                return Integer.parseInt(value, 16);
            } catch (NumberFormatException invalid) {
                throw new IOException("invalid RGB color");
            }
        }
        String location() throws IOException {
            String value = next();
            if (value.length() < 1 || value.length() > 64)
                throw new IOException("invalid location");
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                            || c == '_' || c == '-'))
                    throw new IOException("invalid location");
            }
            return value;
        }
    }
    public boolean selectSpawn(int id) {
        if (spawnId != null)
            for (int i = 0; i < spawnId.length; i++)
                if (spawnId[i] == id) {
                    startX = spawnX[i];
                    startY = spawnY[i];
                    startZ = spawnZ[i];
                    startYaw = spawnYaw[i];
                    return true;
                }
        return false;
    }
    public int findTransition(int id) {
        if (transitionId != null)
            for (int i = 0; i < transitionId.length; i++)
                if (transitionId[i] == id)
                    return i;
        return -1;
    }
    public int nearestSpawn(int x, int z) {
        int best = 0;
        long distance = Long.MAX_VALUE;
        for (int i = 0; spawnId != null && i < spawnId.length; i++) {
            long dx = spawnX[i] - x, dz = spawnZ[i] - z, d = dx * dx + dz * dz;
            if (d < distance) {
                distance = d;
                best = spawnId[i];
            }
        }
        return best;
    }
    public void clear() {
        world = null;
        collision = null;
        entities = null;
        transitionId = null;
        transitionSpawn = null;
        transitionLocation = null;
        spawnId = spawnX = spawnY = spawnZ = spawnYaw = null;
    }
}
