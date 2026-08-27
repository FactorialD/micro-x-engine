package com.microx.engine.ui;
import javax.microedition.lcdui.*;
import com.microx.engine.gameplay.*;
/** Menu painter; all displayed strings are allocated once at class loading. */
public final class UIView {
    private static final String[] TITLES = {"MICRO X", "GAME", "PAUSED", "PDA", "INVENTORY", "MAP",
            "QUESTS", "DIALOGUE", "TRADE", "LOOT", "SETTINGS", "LOAD ERROR"};
    private static final String[] MAIN = {"NEW GAME", "LOAD", "SETTINGS", "EXIT"},
                                  PAUSE = {"RESUME", "SAVE", "LOAD", "SETTINGS", "MAIN MENU"},
                                  PDA = {"INVENTORY", "MAP", "QUESTS", "BACK"},
                                  SETTINGS = {"VOLUME", "RESOLUTION", "DEBUG", "CONTROLS"};
    private static final String[] EMPTY = {"NO ENTRIES"};
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private GameplayState gameplay;
    private int faction;
    private boolean repair;
    private String result;
    public void bind(GameplayState state, int npcFaction, boolean repairMode, String message) {
        gameplay = state;
        faction = npcFaction;
        repair = repairMode;
        result = message;
    }
    public void paint(Graphics g, UIStateMachine ui, UISettings settings) {
        int w = g.getClipWidth(), h = g.getClipHeight();
        g.setColor(0x101820);
        g.fillRect(0, 0, w, h);
        g.setFont(font);
        g.setColor(0xe0d080);
        g.drawString(TITLES[ui.state()], w / 2, 22, Graphics.TOP | Graphics.HCENTER);
        String[] items = items(ui.state());
        int dynamic = ui.listSize();
        int count = dynamic > 0 && items == EMPTY ? dynamic : items.length;
        for (int i = 0; i < count; i++) {
            g.setColor(i == ui.selection() ? 0xffffff : 0x809090);
            if (dynamic > 0 && items == EMPTY
                    && (ui.state() == UIStateMachine.TRADE || ui.state() == UIStateMachine.LOOT
                            || ui.state() == UIStateMachine.INVENTORY))
                drawInventoryRow(g, ui, i, w, 48 + i * 16);
            else if (dynamic > 0 && items == EMPTY)
                drawNumber(g, ui.listBuffer()[i] & 65535, w / 2, 62 + i * 18);
            else
                g.drawString(items[i], w / 2, 62 + i * 24, Graphics.TOP | Graphics.HCENTER);
        }
        if (ui.state() == UIStateMachine.SETTINGS) {
            value(g, settings.volume, 190, 62);
            value(g, settings.resolution, 190, 86);
            value(g, settings.debug ? 1 : 0, 190, 110);
            value(g, settings.controls, 190, 134);
        }
        if (gameplay != null
                && (ui.state() == UIStateMachine.TRADE || ui.state() == UIStateMachine.LOOT)) {
            g.setColor(0xe0d080);
            g.drawString(
                    "YOU $" + gameplay.inventory.money(), 4, h - 30, Graphics.TOP | Graphics.LEFT);
            Inventory other = ui.state() == UIStateMachine.TRADE ? gameplay.trader : gameplay.loot;
            g.drawString(
                    (ui.state() == UIStateMachine.TRADE ? "NPC" : "CORPSE") + " $" + other.money(),
                    w - 4, h - 30, Graphics.TOP | Graphics.RIGHT);
            if (result != null)
                g.drawString(result, w / 2, h - 15, Graphics.TOP | Graphics.HCENTER);
        }
    }
    private void drawInventoryRow(Graphics g, UIStateMachine ui, int row, int w, int y) {
        int id = ui.listBuffer()[row] & 65535;
        Inventory bag = ui.state() == UIStateMachine.INVENTORY || ui.sideAt(row) == 1
                ? gameplay.inventory
                : (ui.state() == UIStateMachine.TRADE ? gameplay.trader : gameplay.loot);
        int price = 0;
        if (ui.state() == UIStateMachine.TRADE)
            price = repair
                    ? TradeSystem.repairPrice(gameplay.inventory, id, 100)
                    : (ui.sideAt(row) == 1
                                      ? TradeSystem.sellPrice(id, 1, faction, gameplay.reputation)
                                      : TradeSystem.buyPrice(id, 1, faction, gameplay.reputation));
        String line = ItemCatalog.name(id) + " x" + bag.count(id) + " " + bag.conditionOf(id) + "%"
                + (price > 0 ? " $" + price : "");
        g.drawString(line, ui.sideAt(row) == 1 ? 4 : w - 4, y,
                Graphics.TOP | (ui.sideAt(row) == 1 ? Graphics.LEFT : Graphics.RIGHT));
    }
    private void drawNumber(Graphics g, int value, int x, int y) {
        int divisor = 1;
        while (value / divisor >= 10) divisor *= 10;
        int left = x - 4;
        do {
            g.drawChar((char) ('0' + value / divisor % 10), left, y, Graphics.TOP | Graphics.LEFT);
            left += 8;
            divisor /= 10;
        } while (divisor > 0);
    }
    private String[] items(int state) {
        if (state == UIStateMachine.MAIN_MENU)
            return MAIN;
        if (state == UIStateMachine.PAUSE)
            return PAUSE;
        if (state == UIStateMachine.PDA)
            return PDA;
        if (state == UIStateMachine.SETTINGS)
            return SETTINGS;
        return EMPTY;
    }
    private void value(Graphics g, int n, int x, int y) {
        g.drawChar((char) ('0' + n), x, y, Graphics.TOP | Graphics.LEFT);
    }
}
