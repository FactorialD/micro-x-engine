package com.microx.engine.ui;
import javax.microedition.lcdui.*;
import com.microx.engine.world.*;
import com.microx.engine.combat.ItemTypes;
/** HUD uses only cached labels and primitive drawing during paint. */
public final class Hud {
    private static final String HP = "HP", AR = "AR", ST = "ST", AMMO = "AMMO", BLEED = "BLEED",
                                RAD = "RAD", USE = "5 USE", FPS = "FPS", ENTS = " E", ROOMS = " R";
    private static final String[] WEAPONS = {"PM", "AK-74", "TOZ-34"};
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private boolean interaction;
    public void setInteraction(boolean value) {
        interaction = value;
    }
    public void paint(Graphics g, Player p, PortalWorld world, int fps, int entities, int rooms,
            boolean debug) {
        int w = g.getClipWidth(), h = g.getClipHeight(), vx = (w - 240) / 2, vy = (h - 320) / 2;
        g.setFont(font);
        bar(g, vx + 4, vy + 5, 70, p.health, 0xb03030, HP);
        bar(g, vx + 4, vy + 18, 70, p.physicalProtection, 0x6080a0, AR);
        bar(g, vx + 4, vy + 31, 70, p.stamina, 0x70a040, ST);
        g.setColor(0xffffff);
        g.drawString(WEAPONS[p.combat.weapon], vx + 154, vy + 5, Graphics.TOP | Graphics.LEFT);
        g.drawString(AMMO, vx + 154, vy + 18, Graphics.TOP | Graphics.LEFT);
        number(g, p.combat.magazine, vx + 191, vy + 18);
        g.drawChar('/', vx + 207, vy + 18, Graphics.TOP | Graphics.LEFT);
        number(g, p.combat.reserve, vx + 213, vy + 18);
        if (p.bleeding > 0) {
            g.setColor(0xff6060);
            g.drawString(BLEED, vx + 4, vy + 46, Graphics.TOP | Graphics.LEFT);
        }
        if (p.radiation > 0) {
            g.setColor(0x80d050);
            g.drawString(RAD, vx + 50, vy + 46, Graphics.TOP | Graphics.LEFT);
        }
        if (interaction) {
            g.setColor(0xffffff);
            g.drawString(USE, w / 2, vy + 210, Graphics.TOP | Graphics.HCENTER);
        }
        map(g, world, p, vx + 184, vy + 250);
        int cx = w / 2, cy = h / 2;
        g.setColor(0xffffff);
        g.drawLine(cx - 5, cy, cx + 5, cy);
        g.drawLine(cx, cy - 5, cx, cy + 5);
        if (debug) {
            g.drawString(FPS, vx + 4, vy + 302, Graphics.TOP | Graphics.LEFT);
            number(g, fps, vx + 28, vy + 302);
            g.drawString(ENTS, vx + 47, vy + 302, Graphics.TOP | Graphics.LEFT);
            number(g, entities, vx + 61, vy + 302);
            g.drawString(ROOMS, vx + 80, vy + 302, Graphics.TOP | Graphics.LEFT);
            number(g, rooms, vx + 94, vy + 302);
        }
    }
    private void bar(Graphics g, int x, int y, int width, int value, int color, String label) {
        g.setColor(0x202020);
        g.fillRect(x + 19, y, width, 8);
        g.setColor(color);
        g.fillRect(x + 19, y, width * clamp(value) / 100, 8);
        g.setColor(0xffffff);
        g.drawString(label, x, y - 3, Graphics.TOP | Graphics.LEFT);
    }
    private int clamp(int n) {
        return n < 0 ? 0 : n > 100 ? 100 : n;
    }
    private void number(Graphics g, int n, int x, int y) {
        if (n < 0) {
            g.drawChar('-', x, y, Graphics.TOP | Graphics.LEFT);
            x += 6;
            n = -n;
        }
        if (n >= 100)
            g.drawChar((char) ('0' + n / 100 % 10), x, y, Graphics.TOP | Graphics.LEFT);
        if (n >= 10)
            g.drawChar((char) ('0' + n / 10 % 10), x + 6, y, Graphics.TOP | Graphics.LEFT);
        g.drawChar((char) ('0' + n % 10), x + 12, y, Graphics.TOP | Graphics.LEFT);
    }
    private void map(Graphics g, PortalWorld world, Player p, int x, int y) {
        g.setColor(0x101810);
        g.fillRect(x, y, 52, 46);
        if (world == null)
            return;
        int current = world.findRoom(p.x, p.z);
        for (int i = 0; i < world.visibleCount() && i < 9; i++) {
            int room = world.visibleRoom(i), px = x + 5 + (room % 3) * 14,
                py = y + 4 + ((room / 3) % 3) * 12;
            g.setColor(room == current ? 0xe0d060 : 0x608060);
            g.fillRect(px, py, 9, 8);
        }
    }
}
