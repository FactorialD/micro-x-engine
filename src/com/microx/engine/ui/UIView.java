package com.microx.engine.ui;
import javax.microedition.lcdui.*;
import com.microx.engine.gameplay.*;
import com.microx.engine.world.*;

/** Responsive, allocation-conscious painter for every modal game screen. */
public final class UIView {
    private static final String[] TITLES = {"MICRO X", "GAME", "PAUSED", "PDA", "INVENTORY", "MAP",
            "QUESTS", "DIALOGUE", "TRADE", "LOOT", "SETTINGS", "RESOURCE ERROR", "ПРО ГРУ", "SCENE",
            "ENDING", "ARENA", "CYCLIC QUEST", "FREEPLAY"};
    private static final String[] MAIN = {"NEW GAME", "LOAD GAME", "SETTINGS", "ПРО ГРУ", "EXIT"},
                                  PAUSE = {"RESUME", "SAVE", "LOAD", "SETTINGS", "MAIN MENU"},
                                  PDA = {"INVENTORY", "MAP", "QUESTS", "BACK"},
                                  SETTINGS = {"VOLUME", "RESOLUTION", "DEBUG", "CONTROLS",
                                          "SENSITIVITY"};
    private static final String[] QUEST_STATE = {"LOCKED", "ACTIVE", "COMPLETE", "FAILED"};
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private GameplayState gameplay;
    private GameplayTables tables;
    private Player player;
    private LevelLoader level;
    private String location;
    private int faction;
    private boolean repair;
    private String result;
    private String containerTitle;
    private StorySystem story;
    private CutsceneSystem cutscene;
    private CyclicQuestSystem cyclic;
    private ArenaSystem arena;

    public void bind(GameplayState state, GameplayTables data, Player p, LevelLoader map,
            String locationName, int npcFaction, boolean repairMode, String message,
            String boundContainerTitle) {
        gameplay = state;
        tables = data;
        player = p;
        level = map;
        location = locationName;
        faction = npcFaction;
        repair = repairMode;
        result = message;
        containerTitle = boundContainerTitle;
    }
    public void bindNarrative(StorySystem storySystem, CutsceneSystem cutsceneSystem,
            CyclicQuestSystem cyclicSystem, ArenaSystem arenaSystem) {
        story = storySystem;
        cutscene = cutsceneSystem;
        cyclic = cyclicSystem;
        arena = arenaSystem;
    }
    public void paint(Graphics g, UIStateMachine ui, UISettings settings) {
        int w = g.getClipWidth(), h = g.getClipHeight();
        g.setColor(0x101820);
        g.fillRect(0, 0, w, h);
        g.setFont(font);
        g.setColor(0xe0d080);
        g.drawString(TITLES[ui.state()], w / 2, 5, Graphics.TOP | Graphics.HCENTER);
        drawTabs(g, ui.state(), w);
        if (ui.state() == UIStateMachine.INVENTORY)
            drawInventory(g, ui, w, h);
        else if (ui.state() == UIStateMachine.MAP)
            drawMap(g, w, h);
        else if (ui.state() == UIStateMachine.QUESTS)
            drawQuests(g, w, h);
        else if (ui.state() == UIStateMachine.DIALOGUE)
            drawDialogue(g, ui, w, h);
        else if (ui.state() == UIStateMachine.TRADE || ui.state() == UIStateMachine.LOOT)
            drawTransfer(g, ui, w, h);
        else if (ui.state() == UIStateMachine.CUTSCENE)
            drawText(g, cutscene == null ? null : cutscene.text(), w, h);
        else if (ui.state() == UIStateMachine.ENDING)
            drawText(g, story == null ? null : story.text(), w, h);
        else if (ui.state() == UIStateMachine.ARENA)
            drawText(g, arena == null ? null : "WAVE " + arena.wave() + "/" + arena.waves(), w, h);
        else if (ui.state() == UIStateMachine.CYCLIC_QUEST)
            drawText(g, cyclic == null ? null : cyclic.text(), w, h);
        else if (ui.state() == UIStateMachine.FREEPLAY)
            drawText(g, "FREEPLAY", w, h);
        else if (ui.state() == UIStateMachine.ABOUT)
            drawAbout(g, w);
        else
            drawMenu(g, ui, settings, w, h);
    }
    private void drawText(Graphics g, String text, int w, int h) {
        g.setColor(0xffffff);
        g.drawString(text == null ? "..." : text, w / 2, h / 2, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0x809090);
        g.drawString("OK", w / 2, h - 24, Graphics.TOP | Graphics.HCENTER);
    }
    private void drawTabs(Graphics g, int state, int w) {
        if (state < UIStateMachine.INVENTORY || state > UIStateMachine.QUESTS)
            return;
        String[] tabs = {"INV", "MAP", "QUEST"};
        for (int i = 0; i < 3; i++) {
            g.setColor(state == UIStateMachine.INVENTORY + i ? 0xffffff : 0x607070);
            g.drawString(tabs[i], (i * 2 + 1) * w / 6, 22, Graphics.TOP | Graphics.HCENTER);
        }
    }
    private void drawMenu(Graphics g, UIStateMachine ui, UISettings settings, int w, int h) {
        String[] items = ui.state() == UIStateMachine.MAIN_MENU ? MAIN
                : ui.state() == UIStateMachine.PAUSE            ? PAUSE
                : ui.state() == UIStateMachine.PDA              ? PDA
                : ui.state() == UIStateMachine.SETTINGS         ? SETTINGS
                                                                : new String[] {"OK"};
        int gap = Math.max(14, Math.min(24, (h - 48) / Math.max(1, items.length)));
        for (int i = 0; i < items.length; i++) {
            int y = 42 + i * gap;
            g.setColor(i == ui.selection() ? 0xffffff : 0x809090);
            g.drawString(items[i], w / 2, y, Graphics.TOP | Graphics.HCENTER);
            if (ui.state() == UIStateMachine.SETTINGS) {
                int value = i == 0 ? settings.volume
                        : i == 1   ? settings.resolution
                        : i == 2   ? (settings.debug ? 1 : 0)
                        : i == 3   ? settings.controls
                                   : settings.sensitivity;
                number(g, value, w - 22, y);
            }
        }
    }
    private void drawInventory(Graphics g, UIStateMachine ui, int w, int h) {
        if (gameplay == null)
            return;
        int top = 40, cell = Math.max(10, Math.min(18, (w - 82) / 8));
        int cols = Math.max(4, (w - 82) / cell), x0 = 4;
        int column = 0, row = 0;
        for (int i = 0; i < ui.listSize(); i++) {
            int id = ui.listBuffer()[i] & 65535, cells = Math.max(1, ItemCatalog.cells(id));
            if (column + cells > cols) {
                column = 0;
                row++;
            }
            int x = x0 + column * cell, y = top + row * (cell + 3);
            g.setColor(i == ui.selection() ? 0xffffff : 0x607060);
            g.drawRect(x, y, Math.min(w - x - 1, cells * cell - 2), cell);
            g.drawString(ItemCatalog.name(id), x + 2, y + 1, Graphics.TOP | Graphics.LEFT);
            column += cells;
        }
        int sx = Math.max(w - 74, w * 2 / 3), y = top;
        slot(g, "W1", gameplay.equipment.weapon(0), sx, y, w - sx - 3);
        slot(g, "W2", gameplay.equipment.weapon(1), sx, y += 22, w - sx - 3);
        slot(g, "AR", gameplay.equipment.armor(), sx, y += 22, w - sx - 3);
        for (int i = 0; i < 5 && y + 18 < h; i++)
            slot(g, "A" + (i + 1), gameplay.equipment.artifact(i), sx, y += 19, w - sx - 3);
    }
    private void slot(Graphics g, String label, int id, int x, int y, int width) {
        g.setColor(0x708070);
        g.drawRect(x, y, width, 17);
        g.drawString(label, x + 2, y + 1, Graphics.TOP | Graphics.LEFT);
        if (id != 0)
            g.drawString(ItemCatalog.name(id), x + width - 2, y + 1, Graphics.TOP | Graphics.RIGHT);
    }
    private void drawMap(Graphics g, int w, int h) {
        int cx = w / 2, cy = h / 2;
        g.setColor(0x304830);
        g.fillRect(8, 42, w - 16, h - 54);
        g.setColor(0xe0d080);
        g.drawString(location == null ? "?" : location, cx, 46, Graphics.TOP | Graphics.HCENTER);
        if (level != null && level.transitionLocation != null)
            for (int i = 0; i < level.transitionLocation.length; i++) {
                int x = 18 + (i % 3) * (w - 36) / 3, y = 76 + (i / 3) * 27;
                g.setColor(0x90b090);
                g.drawLine(cx, cy, x + 12, y + 6);
                g.drawString(level.transitionLocation[i], x, y, Graphics.TOP | Graphics.LEFT);
            }
        g.setColor(0xffffff);
        g.fillRect(cx - 2, cy - 2, 5, 5);
        g.drawString("PLAYER", cx, cy + 6, Graphics.TOP | Graphics.HCENTER);
        if (gameplay != null && gameplay.quests.objective() >= 0) {
            g.setColor(0xffd040);
            g.drawString("OBJECTIVE " + gameplay.quests.objective(), 12, h - 35,
                    Graphics.TOP | Graphics.LEFT);
        }
        if (gameplay != null && gameplay.quests.flag(0)) {
            g.setColor(0x80d0ff);
            g.drawString("STASH FOUND", w - 12, h - 35, Graphics.TOP | Graphics.RIGHT);
        }
    }
    private void drawQuests(Graphics g, int w, int h) {
        if (gameplay == null || tables == null)
            return;
        int y = 42;
        for (int id = 1; id <= gameplay.quests.questCapacity() && y < h - 18; id++) {
            int state = gameplay.quests.state(id);
            String title = tables.questText(id);
            if (title == null)
                continue;
            g.setColor(state == QuestState.ACTIVE ? 0xffffff : 0x809090);
            g.drawString(
                    title + " [" + QUEST_STATE[state] + "]", 5, y, Graphics.TOP | Graphics.LEFT);
            y += 15;
            g.drawString(tables.questDescription(id), 10, y, Graphics.TOP | Graphics.LEFT);
            y += 15;
            if (state == QuestState.ACTIVE) {
                g.setColor(0xe0d080);
                g.drawString(tables.questObjective(id), 10, y, Graphics.TOP | Graphics.LEFT);
                y += 17;
            }
        }
    }
    private void drawDialogue(Graphics g, UIStateMachine ui, int w, int h) {
        if (gameplay == null || tables == null)
            return;
        g.setColor(0x405050);
        g.fillRect(5, 39, 42, 50);
        g.setColor(0xffffff);
        g.drawString("NPC " + gameplay.actorId, 26, 56, Graphics.TOP | Graphics.HCENTER);
        g.drawString(tables.npcName(gameplay.actorId), 53, 40, Graphics.TOP | Graphics.LEFT);
        int first = ui.listSize() == 0 ? 0 : ui.listBuffer()[0] & 65535;
        if (first != 0)
            g.drawString(tables.dialogText(first), 53, 59, Graphics.TOP | Graphics.LEFT);
        int y = 99;
        for (int i = 0; i < ui.listSize() && y < h - 14; i++, y += 20) {
            g.setColor(i == ui.selection() ? 0xffffff : 0x809090);
            g.drawString((i + 1) + ". " + tables.dialogText(ui.listBuffer()[i] & 65535), 8, y,
                    Graphics.TOP | Graphics.LEFT);
        }
    }
    private void drawTransfer(Graphics g, UIStateMachine ui, int w, int h) {
        if (gameplay == null)
            return;
        g.setColor(0xe0d080);
        g.drawString("YOU $" + gameplay.inventory.money(), 4, 31, Graphics.TOP | Graphics.LEFT);
        Inventory other = ui.state() == UIStateMachine.TRADE ? gameplay.trader : gameplay.loot;
        String otherTitle = ui.state() == UIStateMachine.TRADE ? "NPC" : containerTitle;
        g.drawString(otherTitle + " (" + itemCount(other) + ") $" + other.money(), w - 4, 31,
                Graphics.TOP | Graphics.RIGHT);
        int leftY = 50, rightY = 50;
        for (int i = 0; i < ui.listSize(); i++) {
            int id = ui.listBuffer()[i] & 65535;
            boolean yours = ui.sideAt(i) == 1;
            Inventory bag = yours ? gameplay.inventory : other;
            int price = ui.state() == UIStateMachine.TRADE
                    ? (repair ? TradeSystem.repairPrice(gameplay.inventory, id, 100)
                              : (yours ? TradeSystem.sellPrice(id, 1, faction, gameplay.reputation)
                                       : TradeSystem.buyPrice(id, 1, faction, gameplay.reputation)))
                    : 0;
            int y = yours ? leftY : rightY;
            g.setColor(i == ui.selection() ? 0xffffff : 0x809090);
            g.drawString(
                    ItemCatalog.name(id) + " x" + bag.count(id) + (price > 0 ? " $" + price : ""),
                    yours ? 4 : w - 4, y, Graphics.TOP | (yours ? Graphics.LEFT : Graphics.RIGHT));
            if (yours)
                leftY += 16;
            else
                rightY += 16;
        }
        if (result != null)
            g.drawString(result, w / 2, h - 16, Graphics.TOP | Graphics.HCENTER);
    }
    private static int itemCount(Inventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.slots(); i++) count += inventory.countAt(i);
        return count;
    }
    private void drawAbout(Graphics g, int w) {
        g.setColor(0xc0c8b0);
        g.drawString("MICRO X ENGINE", w / 2, 48, Graphics.TOP | Graphics.HCENTER);
        g.drawString("J2ME survival action RPG", w / 2, 70, Graphics.TOP | Graphics.HCENTER);
        g.drawString("Keypad edition", w / 2, 92, Graphics.TOP | Graphics.HCENTER);
        g.drawString("Back: left soft key", w / 2, 126, Graphics.TOP | Graphics.HCENTER);
    }
    private void number(Graphics g, int value, int x, int y) {
        g.drawString(String.valueOf(value), x, y, Graphics.TOP | Graphics.RIGHT);
    }
}
