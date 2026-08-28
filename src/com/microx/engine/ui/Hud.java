package com.microx.engine.ui;
import javax.microedition.lcdui.*;
import com.microx.engine.Telemetry;
import com.microx.engine.world.*;
import com.microx.engine.combat.ItemTypes;
/** HUD uses only cached labels and primitive drawing during paint. */
public final class Hud {
    private static final String HP = "HP", AR = "AR", ST = "ST", AMMO = "AMMO", BLEED = "BLEED",
                                RAD = "RAD", USE = "5 USE", FPS = "FPS ", UP = "UP ", RP = " RP ",
                                TRI = "TRI ", TEX = "TEX ", MESH = " MESH ", HEAP = "HEAP ",
                                PEAK = "PEAK ", RENDERER = "REND ", PERCENT = "REND% ",
                                LOC = "LOC ", NPC = "NPC ", MUT = " MUT ", ITEM = "ITEM ",
                                ANOM = " ANOM ", CORPSE = "CORPSE ", ROOMS = " ROOM ",
                                DROP = " DROP ", SOURCE = "SRC ", POS = "POS ", ROOM = " R ",
                                KEY = "KEY ", ACTION = " A ", DOWN = " D ";
    private static final String[] WEAPONS = {"PM", "AK-74", "TOZ-34"};
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private boolean interaction;
    public void setInteraction(boolean value) {
        interaction = value;
    }
    public void paint(Graphics g, Player p, PortalWorld world, Telemetry stats, String location,
            String source, boolean debug) {
        int w = g.getClipWidth(), h = g.getClipHeight(), vx = Math.max(0, (w - 240) / 2),
            vy = Math.max(0, (h - 320) / 2);
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
        map(g, world, p, Math.max(2, w - 56), Math.max(55, h - 70));
        int cx = w / 2, cy = h / 2;
        g.setColor(0xffffff);
        g.drawLine(cx - 5, cy, cx + 5, cy);
        g.drawLine(cx, cy - 5, cx, cy + 5);
        if (debug) {
            debug(g, stats, p, world, location, source, vx + 3, vy + 58);
        }
    }
    private void debug(Graphics g, Telemetry s, Player player, PortalWorld world, String location,
            String source, int x, int y) {
        int line = font.getHeight(), width = 150, rows = 15, p;

        g.setColor(0x101010);
        g.fillRect(x, y, width, rows * line);
        g.setColor(0xffffff);
        p = label(g, FPS, x + 2, y);
        value(g, s.fps, p, y);
        y += line;
        p = label(g, UP, x + 2, y);
        p = value(g, s.updateP95, p, y);
        p = label(g, RP, p, y);
        value(g, s.renderP95, p, y);
        y += line;
        p = label(g, TRI, x + 2, y);
        p = value(g, s.submittedTriangles, p, y);
        p = slash(g, p, y);
        p = value(g, s.clippedTriangles, p, y);
        p = slash(g, p, y);
        value(g, s.drawnTriangles, p, y);
        y += line;
        p = label(g, TEX, x + 2, y);
        p = value(g, s.textureCount, p, y);
        p = label(g, MESH, p, y);
        value(g, s.meshSectionCount, p, y);
        y += line;
        p = label(g, HEAP, x + 2, y);
        p = value(g, Telemetry.kib(s.totalMemory - s.freeMemory), p, y);
        p = slash(g, p, y);
        value(g, Telemetry.kib(s.totalMemory), p, y);
        y += line;
        p = label(g, PEAK, x + 2, y);
        value(g, Telemetry.kib(s.peakUsedMemory), p, y);
        y += line;
        p = label(g, RENDERER, x + 2, y);
        p = value(g, Telemetry.kib(s.rendererUsedBytes), p, y);
        p = slash(g, p, y);
        value(g, Telemetry.kib(s.rendererBudgetBytes), p, y);
        y += line;
        p = label(g, PERCENT, x + 2, y);
        p = value(g, s.rendererBudgetPercent, p, y);
        g.drawChar('%', p, y, Graphics.TOP | Graphics.LEFT);
        y += line;
        p = label(g, LOC, x + 2, y);
        g.drawString(location == null ? "-" : location, p, y, Graphics.TOP | Graphics.LEFT);
        y += line;
        p = label(g, SOURCE, x + 2, y);
        g.drawString(source == null ? "-" : source, p, y, Graphics.TOP | Graphics.LEFT);
        y += line;
        p = label(g, POS, x + 2, y);
        p = value(g, player.x, p, y);
        p = slash(g, p, y);
        p = value(g, player.y, p, y);
        p = slash(g, p, y);
        p = value(g, player.z, p, y);
        p = label(g, ROOM, p, y);
        value(g, world.findRoom(player.x, player.z), p, y);
        y += line;
        p = label(g, NPC, x + 2, y);
        p = value(g, s.npcCount, p, y);
        p = label(g, MUT, p, y);
        value(g, s.mutantCount, p, y);
        y += line;
        p = label(g, ITEM, x + 2, y);
        p = value(g, s.itemCount, p, y);
        p = label(g, ANOM, p, y);
        value(g, s.anomalyCount, p, y);
        y += line;
        p = label(g, CORPSE, x + 2, y);
        value(g, s.corpseCount, p, y);
        y += line;
        p = label(g, ROOMS, x + 2, y);
        p = value(g, s.rooms, p, y);
        p = label(g, DROP, p, y);
        value(g, s.droppedFixedSteps, p, y);
        y += line;
        p = label(g, KEY, x + 2, y);
        p = value(g, s.lastRawKey, p, y);
        p = label(g, ACTION, p, y);
        p = value(g, s.lastGameAction, p, y);
        p = label(g, DOWN, p, y);
        value(g, s.inputDown, p, y);
    }
    private int label(Graphics g, String text, int x, int y) {
        g.drawString(text, x, y, Graphics.TOP | Graphics.LEFT);
        return x + font.stringWidth(text);
    }
    private int slash(Graphics g, int x, int y) {
        g.drawChar('/', x, y, Graphics.TOP | Graphics.LEFT);
        return x + font.charWidth('/');
    }
    /** Draws directly from the negative domain, including Long.MIN_VALUE, without a buffer. */
    private int value(Graphics g, long number, int x, int y) {
        boolean negative = number < 0;
        long n = negative ? number : -number, power = -1;
        if (negative) {
            g.drawChar('-', x, y, Graphics.TOP | Graphics.LEFT);
            x += font.charWidth('-');
        }
        while (power >= n / 10) power *= 10;
        while (power != 0) {
            int digit = (int) (n / power);
            char c = (char) ('0' + digit);
            g.drawChar(c, x, y, Graphics.TOP | Graphics.LEFT);
            x += font.charWidth(c);
            n %= power;
            power /= 10;
        }
        return x;
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
