package com.microx.engine.render;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import com.microx.engine.assets.MeshSection;
import com.microx.engine.assets.TestGeometry;
import com.microx.engine.math.Fixed;

/** Debug-only, allocation-at-open rotating preview for authored test and location geometry. */
public final class TestScene {
    private static final String[] PATHS = {"/test/cube/geometry.txt", "/test/pyramid/geometry.txt",
            "/levels/cordon/geometry.txt", "/levels/garbage/geometry.txt"};
    private static final String[] NAMES = {"CUBE", "PYRAMID", "CORDON", "GARBAGE"};
    private MeshSection[] sections;
    private int selected, centerX, centerY, centerZ, extent = Fixed.ONE;

    public boolean open(int index) {
        if (index < 0 || index >= PATHS.length)
            return false;
        InputStream in = getClass().getResourceAsStream(PATHS[index]);
        if (in == null)
            return false;
        try {
            sections = TestGeometry.read(in);
        } catch (IOException invalid) {
            sections = null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
        if (sections == null)
            return false;
        selected = index;
        bounds();
        return true;
    }

    private void bounds() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int s = 0; s < sections.length; s++)
            for (int i = 0; i < sections[s].vertexCount(); i++) {
                MeshSection m = sections[s];
                if (m.x(i) < minX)
                    minX = m.x(i);
                if (m.x(i) > maxX)
                    maxX = m.x(i);
                if (m.y(i) < minY)
                    minY = m.y(i);
                if (m.y(i) > maxY)
                    maxY = m.y(i);
                if (m.z(i) < minZ)
                    minZ = m.z(i);
                if (m.z(i) > maxZ)
                    maxZ = m.z(i);
            }
        centerX = minX / 2 + maxX / 2;
        centerY = minY / 2 + maxY / 2;
        centerZ = minZ / 2 + maxZ / 2;
        extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        if (extent < 1)
            extent = 1;
    }

    public void paint(Graphics g, int width, int height, long now) {
        g.setColor(0x101820);
        g.fillRect(0, 0, width, height);
        if (sections == null)
            return;
        int angle = (int) ((now / 16) % 360), sin = Fixed.sin(angle), cos = Fixed.cos(angle);
        int scale = Math.min(width, height) * 3 / 4;
        for (int s = 0; s < sections.length; s++) {
            MeshSection m = sections[s];
            g.setColor(m.color());
            for (int t = 0; t < m.triangleCount(); t++) {
                int a = m.index(t * 3), b = m.index(t * 3 + 1), c = m.index(t * 3 + 2);
                int ax = projectX(m, a, sin, cos, scale, width),
                    ay = projectY(m, a, sin, cos, scale, height);
                int bx = projectX(m, b, sin, cos, scale, width),
                    by = projectY(m, b, sin, cos, scale, height);
                int cx = projectX(m, c, sin, cos, scale, width),
                    cy = projectY(m, c, sin, cos, scale, height);
                g.fillTriangle(ax, ay, bx, by, cx, cy);
                g.setColor(0x202020);
                g.drawLine(ax, ay, bx, by);
                g.drawLine(bx, by, cx, cy);
                g.drawLine(cx, cy, ax, ay);
                g.setColor(m.color());
            }
        }
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xffffff);
        g.drawString(NAMES[selected], width / 2, 5, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0x809090);
        g.drawString("BACK", width / 2, height - 18, Graphics.TOP | Graphics.HCENTER);
    }

    private int projectX(MeshSection m, int i, int sin, int cos, int scale, int width) {
        long x = m.x(i) - centerX, z = m.z(i) - centerZ;
        long rotated = (x * cos - z * sin) >> 16;
        return width / 2 + (int) (rotated * scale / extent);
    }
    private int projectY(MeshSection m, int i, int sin, int cos, int scale, int height) {
        long x = m.x(i) - centerX, y = m.y(i) - centerY, z = m.z(i) - centerZ;
        long depth = (x * sin + z * cos) >> 16;
        return height / 2 - (int) ((y - depth / 3) * scale / extent);
    }
}
