package com.microx.engine.render;

import javax.microedition.lcdui.Graphics;
import com.microx.engine.world.Player;
import com.microx.engine.world.PortalWorld;

/** MIDP Graphics presentation path; it has no optional JSR-184 dependency. */
public final class SoftwareRenderer {
    public void load() {}

    public void render(Graphics g, Player player, PortalWorld portals) {
        int w = g.getClipWidth(), h = g.getClipHeight();
        int horizon = h / 2;
        g.setColor(0x182030);
        g.fillRect(0, 0, w, horizon);
        g.setColor(0x302c24);
        g.fillRect(0, horizon, w, h - horizon);
        g.setColor(0x605848);
        int rooms = portals.visibleCount();
        for (int i = 0; i < rooms && i < 4; i++) {
            int inset = 18 + i * 14;
            g.drawRect(inset, horizon - 70 + i * 10, w - inset * 2, 140 - i * 20);
        }
    }

    public void release() {}
}
