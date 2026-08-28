package com.microx.engine.assets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Vector;

/** Strict, allocation-at-load-time parser for the runtime geometry.txt contract. */
final class GeometryLoader {
    private static final int NO_TEXTURE = -2147483648;
    private GeometryLoader() {}

    static MeshSection[] read(InputStream stream) throws IOException {
        Vector positions = new Vector(), texcoords = new Vector(), sections = new Vector();
        Hashtable materials = new Hashtable(), byKey = new Hashtable();
        Material current = new Material(NO_TEXTURE, 0xff00ff);
        materials.put("default", current);
        Reader reader = new InputStreamReader(stream, "UTF-8");
        int room = 0, lineNo = 0;
        StringBuffer line = new StringBuffer();
        for (;;) {
            int ch = reader.read();
            if (ch != '\n' && ch != -1) {
                if (ch != '\r')
                    line.append((char) ch);
                continue;
            }
            lineNo++;
            String raw = line.toString().trim();
            line.setLength(0);
            if (raw.startsWith("# microx room ")) {
                room = nonNegative(raw.substring(14).trim(), lineNo, "room");
            } else if (raw.startsWith("# microx material ")) {
                String[] p = words(raw.substring(18).trim());
                if (p.length < 1 || p.length > 3)
                    fail(lineNo, "material needs NAME and optional texture=/color=");
                int texture = NO_TEXTURE, color = 0xff00ff;
                boolean hasTexture = false, hasColor = false;
                for (int i = 1; i < p.length; i++) {
                    if (p[i].startsWith("texture=")) {
                        texture = signedShort(p[i].substring(8), lineNo, "texture");
                        hasTexture = true;
                    } else if (p[i].startsWith("color=")) {
                        color = rgb(p[i].substring(6), lineNo);
                        hasColor = true;
                    } else
                        fail(lineNo, "material attributes are texture=ID and color=RRGGBB");
                }
                if (hasTexture && !hasColor)
                    fail(lineNo, "textured material requires fallback color");
                materials.put(p[0], new Material(texture, color));
            } else {
                int comment = raw.indexOf('#');
                if (comment >= 0)
                    raw = raw.substring(0, comment).trim();
                if (raw.length() != 0) {
                    String[] p = words(raw);
                    if ("v".equals(p[0])) {
                        if (p.length != 4)
                            fail(lineNo, "vertex needs x y z");
                        positions.addElement(new int[] {
                                fixed(p[1], lineNo), fixed(p[2], lineNo), fixed(p[3], lineNo)});
                    } else if ("vt".equals(p[0])) {
                        if (p.length != 3)
                            fail(lineNo, "texture coordinate needs u v");
                        texcoords.addElement(new int[] {fixed(p[1], lineNo), fixed(p[2], lineNo)});
                    } else if ("usemtl".equals(p[0])) {
                        if (p.length != 2 || materials.get(p[1]) == null)
                            fail(lineNo, "unknown material");
                        current = (Material) materials.get(p[1]);
                    } else if ("o".equals(p[0]) || "g".equals(p[0])) {
                        if (p.length > 1 && p[1].startsWith("room_"))
                            room = nonNegative(p[1].substring(5), lineNo, "room");
                    } else if ("f".equals(p[0])) {
                        if (p.length < 4)
                            fail(lineNo, "face needs at least three corners");
                        String key = room + "/" + current.texture + "/" + current.color;
                        Section s = (Section) byKey.get(key);
                        if (s == null) {
                            s = new Section(room, current.texture, current.color);
                            byKey.put(key, s);
                            sections.addElement(s);
                        }
                        int[] polygon = new int[p.length - 1];
                        for (int i = 1; i < p.length; i++)
                            polygon[i - 1] = s.vertex(p[i], positions, texcoords, lineNo);
                        for (int i = 1; i < polygon.length - 1; i++)
                            s.triangle(polygon[0], polygon[i], polygon[i + 1], lineNo);
                    }
                }
            }
            if (ch == -1)
                break;
        }
        if (sections.size() == 0)
            fail(lineNo, "geometry has no renderable faces");
        MeshSection[] result = new MeshSection[sections.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = ((Section) sections.elementAt(i)).mesh();
        return result;
    }

    private static final class Material {
        final int texture, color;
        Material(int t, int c) {
            texture = t;
            color = c;
        }
    }
    private static final class Section {
        final int room, texture, color;
        final Vector xyz = new Vector(), uv = new Vector(), indices = new Vector();
        final Hashtable vertices = new Hashtable();
        Section(int r, int t, int c) {
            room = r;
            texture = t;
            color = c;
        }
        int vertex(String corner, Vector ps, Vector ts, int line) throws IOException {
            int slash = corner.indexOf('/');
            if (slash < 1)
                fail(line, "faces require v/vt corners");
            int pi = index(corner.substring(0, slash), ps.size(), line),
                end = corner.indexOf('/', slash + 1);
            String tv = end < 0 ? corner.substring(slash + 1) : corner.substring(slash + 1, end);
            if (tv.length() == 0)
                fail(line, "faces require v/vt corners");
            int ti = index(tv, ts.size(), line);
            String key = pi + "/" + ti;
            Integer old = (Integer) vertices.get(key);
            if (old != null)
                return old.intValue();
            int n = vertices.size();
            if (n >= 65535)
                fail(line, "section has too many vertices");
            vertices.put(key, new Integer(n));
            int[] p = (int[]) ps.elementAt(pi), t = (int[]) ts.elementAt(ti);
            for (int i = 0; i < 3; i++) xyz.addElement(new Integer(p[i]));
            for (int i = 0; i < 2; i++) uv.addElement(new Integer(t[i]));
            return n;
        }
        void triangle(int a, int b, int c, int line) throws IOException {
            long ax = value(xyz, a * 3), ay = value(xyz, a * 3 + 1), az = value(xyz, a * 3 + 2),
                 bx = value(xyz, b * 3), by = value(xyz, b * 3 + 1), bz = value(xyz, b * 3 + 2),
                 cx = value(xyz, c * 3), cy = value(xyz, c * 3 + 1), cz = value(xyz, c * 3 + 2);
            long ux = bx - ax, uy = by - ay, uz = bz - az, vx = cx - ax, vy = cy - ay, vz = cz - az;
            if (uy * vz - uz * vy == 0 && uz * vx - ux * vz == 0 && ux * vy - uy * vx == 0)
                fail(line, "degenerate triangle");
            indices.addElement(new Integer(a));
            indices.addElement(new Integer(b));
            indices.addElement(new Integer(c));
        }
        MeshSection mesh() {
            int[] p = new int[xyz.size()], t = new int[uv.size()];
            short[] ix = new short[indices.size()];
            for (int i = 0; i < p.length; i++) p[i] = value(xyz, i);
            for (int i = 0; i < t.length; i++) t[i] = value(uv, i);
            for (int i = 0; i < ix.length; i++) ix[i] = (short) value(indices, i);
            return new MeshSection(room, texture, color, p, t, ix);
        }
    }
    private static int value(Vector v, int i) {
        return ((Integer) v.elementAt(i)).intValue();
    }
    private static int index(String s, int size, int line) throws IOException {
        int n = number(s, line, "index");
        if (n == 0)
            fail(line, "OBJ index is zero");
        n = n > 0 ? n - 1 : size + n;
        if (n < 0 || n >= size)
            fail(line, "OBJ index out of range");
        return n;
    }
    private static int fixed(String s, int line) throws IOException {
        float f;
        try {
            f = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            fail(line, "invalid coordinate");
            return 0;
        }
        if (f != f || f > 32767.999f || f < -32768f)
            fail(line, "coordinate overflow");
        return (int) (f * 65536.0f);
    }
    private static int rgb(String s, int line) throws IOException {
        if (s.length() != 6)
            fail(line, "RGB888 must be six hexadecimal digits");
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            fail(line, "invalid RGB888");
            return 0;
        }
    }
    private static int signedShort(String s, int l, String w) throws IOException {
        int n = number(s, l, w);
        if (n < -32768 || n > 32767)
            fail(l, w + " out of range");
        return n;
    }
    private static int nonNegative(String s, int l, String w) throws IOException {
        int n = number(s, l, w);
        if (n < 0 || n > 65535)
            fail(l, w + " out of range");
        return n;
    }
    private static int number(String s, int l, String w) throws IOException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            fail(l, "invalid " + w);
            return 0;
        }
    }
    private static String[] words(String s) {
        Vector v = new Vector();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            int b = i;
            while (i < s.length() && !Character.isWhitespace(s.charAt(i))) i++;
            if (b < i)
                v.addElement(s.substring(b, i));
        }
        String[] r = new String[v.size()];
        v.copyInto(r);
        return r;
    }
    private static void fail(int line, String message) throws IOException {
        throw new IOException("line " + line + ": " + message);
    }
}
