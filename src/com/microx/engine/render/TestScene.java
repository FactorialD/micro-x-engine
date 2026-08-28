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

    public void paint(SoftwareRenderer renderer, Graphics g, int width, int height, long now) {
        if (sections == null)
            return;
        int angle = (int) ((now / 16) % 360);
        renderer.renderPreview(g, sections, null, centerX, centerY, centerZ, extent, angle);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xffffff);
        g.drawString(NAMES[selected], width / 2, 5, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0x809090);
        g.drawString("BACK", width / 2, height - 18, Graphics.TOP | Graphics.HCENTER);
    }
}
