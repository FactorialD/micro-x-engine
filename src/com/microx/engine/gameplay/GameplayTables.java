package com.microx.engine.gameplay;
import java.io.IOException;
import java.io.InputStream;

/** Immutable-at-runtime gameplay descriptions read directly from UTF-8 text tables. */
public final class GameplayTables {
    public static final int MAX_TABLES = 16, MAX_ROWS = 256, MAX_TOTAL_ROWS = 1024;
    public static final String DEFAULT_RESOURCE = "/data/gameplay/gameplay.txt";
    private String[] table, key, text, meta;
    private short[] id, first, count;
    private byte[] itemType, cells, health, bleeding, physical, anomaly, radiation, spread,
            durability;
    private short[] stack, value, ammo, magazine, damage, range, cooldown, reload, damageBonus;

    /** Loads the table registry and every /data/&lt;table&gt;/&lt;table&gt;.txt resource it names. */
    public boolean load(String registryResource) {
        clear();
        InputStream registry = getClass().getResourceAsStream(registryResource);
        if (registry == null)
            return false;
        try {
            String[] registryLines = lines(registry);
            String[] names = new String[MAX_TABLES];
            int tables = 0;
            for (int i = 0; i < registryLines.length; i++) {
                String line = registryLines[i].trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;
                String[] fields = columns(line);
                if (fields == null || fields[1].length() == 0 || tables == MAX_TABLES)
                    return false;
                String name = fields[1];
                for (int j = 0; j < tables; j++)
                    if (name.equals(names[j]))
                        return false;
                names[tables++] = name;
            }
            if (tables == 0)
                return false;
            table = new String[tables];
            first = new short[tables];
            count = new short[tables];
            id = new short[MAX_TOTAL_ROWS];
            key = new String[MAX_TOTAL_ROWS];
            text = new String[MAX_TOTAL_ROWS];
            meta = new String[MAX_TOTAL_ROWS];
            int total = 0;
            for (int t = 0; t < tables; t++) {
                String resource = "/data/" + names[t] + "/" + names[t] + ".txt";
                InputStream stream = getClass().getResourceAsStream(resource);
                if (stream == null)
                    return false;
                String[] rows = lines(stream);
                table[t] = names[t];
                first[t] = (short) total;
                int rowsInTable = 0;
                for (int lineNumber = 0; lineNumber < rows.length; lineNumber++) {
                    String line = rows[lineNumber].trim();
                    if (line.length() == 0 || line.charAt(0) == '#')
                        continue;
                    String[] fields = columns(line);
                    if (fields == null || rowsInTable == MAX_ROWS || total == MAX_TOTAL_ROWS)
                        return false;
                    int stableId = Integer.parseInt(fields[0]);
                    if (stableId <= 0 || stableId > 65535)
                        return false;
                    for (int previous = first[t] & 65535; previous < total; previous++)
                        if ((id[previous] & 65535) == stableId || key[previous].equals(fields[1]))
                            return false;
                    id[total] = (short) stableId;
                    key[total] = fields[1];
                    text[total] = fields[2];
                    meta[total] = fields[3];
                    total++;
                    rowsInTable++;
                }
                count[t] = (short) rowsInTable;
            }
            return buildItems();
        } catch (Exception invalid) {
            clear();
            return false;
        }
    }

    private static String[] columns(String line) {
        String[] result = new String[4];
        int from = 0;
        for (int i = 0; i < 3; i++) {
            int separator = line.indexOf('|', from);
            if (separator < 0) {
                if (i < 2)
                    return null;
                result[i] = line.substring(from);
                result[3] = "";
                return result;
            }
            result[i] = line.substring(from, separator);
            from = separator + 1;
        }
        if (line.indexOf('|', from) >= 0)
            return null;
        result[3] = line.substring(from);
        return result;
    }

    private static String[] lines(InputStream stream) throws IOException {
        StringBuffer content = new StringBuffer();
        try {
            int firstByte = stream.read();
            while (firstByte >= 0) {
                int value;
                if (firstByte < 128) {
                    value = firstByte;
                } else if ((firstByte & 224) == 192) {
                    int b = continuation(stream);
                    value = ((firstByte & 31) << 6) | b;
                    if (value < 128)
                        throw new IOException("overlong UTF-8");
                } else if ((firstByte & 240) == 224) {
                    int b = continuation(stream), c = continuation(stream);
                    value = ((firstByte & 15) << 12) | (b << 6) | c;
                    if (value < 2048 || value >= 55296 && value <= 57343)
                        throw new IOException("invalid UTF-8");
                } else if ((firstByte & 248) == 240) {
                    int b = continuation(stream), c = continuation(stream),
                        d = continuation(stream);
                    value = ((firstByte & 7) << 18) | (b << 12) | (c << 6) | d;
                    if (value < 65536 || value > 1114111)
                        throw new IOException("invalid UTF-8");
                    value -= 65536;
                    content.append((char) (55296 | value >> 10));
                    value = 56320 | value & 1023;
                } else {
                    throw new IOException("invalid UTF-8");
                }
                if (!(content.length() == 0 && value == 65279))
                    content.append((char) value);
                firstByte = stream.read();
            }
        } finally {
            stream.close();
        }
        int count = 1;
        for (int i = 0; i < content.length(); i++)
            if (content.charAt(i) == '\n')
                count++;
        String[] result = new String[count];
        int start = 0, line = 0;
        for (int i = 0; i <= content.length(); i++)
            if (i == content.length() || content.charAt(i) == '\n') {
                int end = i;
                if (end > start && content.charAt(end - 1) == '\r')
                    end--;
                result[line++] = content.substring(start, end);
                start = i + 1;
            }
        return result;
    }

    private static int continuation(InputStream stream) throws IOException {
        int value = stream.read();
        if ((value & 192) != 128)
            throw new IOException("truncated UTF-8");
        return value & 63;
    }
    public int find(String tableName, int stableId) {
        for (int t = 0; t < table.length; t++)
            if (table[t].equals(tableName))
                for (int i = first[t] & 65535, end = i + (count[t] & 65535); i < end; i++)
                    if ((id[i] & 65535) == stableId)
                        return i;
        return -1;
    }
    public String key(int row) {
        return key[row];
    }
    public String text(int row) {
        return text[row];
    }
    public String meta(int row) {
        return meta[row];
    }
    public int id(int row) {
        return id[row] & 65535;
    }
    public int dialogRow(int dialog) {
        return find("dialogs", dialog);
    }
    public int dialogNext(int dialog) {
        int row = dialogRow(dialog);
        return row < 0 ? 0 : number(meta[row], "next");
    }
    public String dialogText(int dialog) {
        int row = dialogRow(dialog);
        return row < 0 ? null : text[row];
    }
    public String dialogRefTable(int dialog) {
        return referencePart(dialogRow(dialog), true);
    }
    public int dialogRef(int dialog) {
        return referenceNumber(dialogRow(dialog));
    }
    public int npcDialog(int npc) {
        return referenced("npcs", npc, "dialogs");
    }
    public String itemName(int item) {
        int row = find("items", item);
        return row < 0 ? "?" : text[row];
    }
    /** Loads the inventory profile belonging to a stable NPC id. */
    public boolean traderProfile(int npc, Inventory inventory) {
        int row = find("traders", npc);
        if (row < 0)
            return false;
        inventory.clear();
        inventory.setMoney(number(meta[row], "money"));
        String stock = field(meta[row], "stock");
        int from = 0;
        while (stock != null && from < stock.length()) {
            int end = stock.indexOf(';', from);
            if (end < 0)
                end = stock.length();
            String entry = stock.substring(from, end);
            int colon = entry.indexOf(':');
            if (colon < 1
                    || !inventory.add(Integer.parseInt(entry.substring(0, colon)),
                            Integer.parseInt(entry.substring(colon + 1)), 100))
                return false;
            from = end + 1;
        }
        return true;
    }
    public int npcFaction(int npc) {
        int row = find("traders", npc);
        return row < 0 ? 0 : number(meta[row], "faction");
    }
    public void installFactionRelations(FactionRelations relations) {
        for (int t = 0; t < table.length; t++)
            if ("relationships".equals(table[t]))
                for (int i = first[t] & 65535, end = i + (count[t] & 65535); i < end; i++)
                    relations.set(number(meta[i], "from"), number(meta[i], "to"),
                            number(meta[i], "relation"));
    }
    public int questRequires(int quest) {
        int row = find("quests", quest);
        return row < 0 ? 0 : number(meta[row], "requires");
    }
    public String questText(int quest) {
        int row = find("quests", quest);
        return row < 0 ? null : text[row];
    }
    public String questDescription(int quest) {
        int row = find("quests", quest);
        String value = row < 0 ? null : field(meta[row], "description");
        return value == null ? questText(quest) : value;
    }
    public String questObjective(int quest) {
        int row = find("quests", quest);
        return row < 0 ? null : field(meta[row], "objective");
    }
    public String npcName(int npc) {
        int row = find("npcs", npc);
        return row < 0 ? "NPC" : text[row];
    }
    public String questRefTable(int quest) {
        return referencePart(find("quests", quest), true);
    }
    public int questRef(int quest) {
        return referenceNumber(find("quests", quest));
    }
    public int tableSize(String tableName) {
        for (int t = 0; t < table.length; t++)
            if (table[t].equals(tableName))
                return count[t] & 65535;
        return 0;
    }
    public int tableRow(String tableName, int offset) {
        for (int t = 0; t < table.length; t++)
            if (table[t].equals(tableName) && offset >= 0 && offset < (count[t] & 65535))
                return (first[t] & 65535) + offset;
        return -1;
    }
    public int numberAt(int row, String name) {
        return row < 0 ? 0 : number(meta[row], name);
    }
    public String fieldAt(int row, String name) {
        return row < 0 ? null : field(meta[row], name);
    }
    public int storyEntry() {
        int n = tableSize("story_nodes");
        for (int i = 0; i < n; i++) {
            int row = tableRow("story_nodes", i);
            if (numberAt(row, "entry") == 1)
                return id(row);
        }
        return 0;
    }
    private int referenced(String tableName, int stableId, String expectedTable) {
        int row = find(tableName, stableId);
        if (row < 0 || !expectedTable.equals(referencePart(row, true)))
            return 0;
        return referenceNumber(row);
    }
    private String referencePart(int row, boolean tablePart) {
        if (row < 0)
            return null;
        String ref = field(meta[row], "ref");
        int colon = ref == null ? -1 : ref.indexOf(':');
        return colon < 0 ? null : (tablePart ? ref.substring(0, colon) : ref.substring(colon + 1));
    }
    private int referenceNumber(int row) {
        String value = referencePart(row, false);
        return value == null ? 0 : Integer.parseInt(value);
    }
    private boolean buildItems() {
        int max = 0, tableIndex = -1;
        for (int t = 0; t < table.length; t++)
            if ("items".equals(table[t]))
                tableIndex = t;
        if (tableIndex < 0)
            return false;
        int start = first[tableIndex] & 65535, end = start + (count[tableIndex] & 65535);
        for (int i = start; i < end; i++)
            if ((id[i] & 65535) > max)
                max = id[i] & 65535;
        itemType = new byte[max + 1];
        cells = new byte[max + 1];
        health = new byte[max + 1];
        bleeding = new byte[max + 1];
        physical = new byte[max + 1];
        anomaly = new byte[max + 1];
        radiation = new byte[max + 1];
        spread = new byte[max + 1];
        durability = new byte[max + 1];
        stack = new short[max + 1];
        value = new short[max + 1];
        ammo = new short[max + 1];
        magazine = new short[max + 1];
        damage = new short[max + 1];
        range = new short[max + 1];
        cooldown = new short[max + 1];
        reload = new short[max + 1];
        damageBonus = new short[max + 1];
        for (int row = start; row < end; row++) {
            int item = id[row] & 65535;
            String type = field(meta[row], "type");
            itemType[item] = (byte) ("weapon".equals(type) ? 1
                            : "armor".equals(type)         ? 2
                            : "consumable".equals(type)    ? 3
                            : "artifact".equals(type)      ? 4
                            : "ammo".equals(type)          ? 5
                            : "bolt".equals(type)          ? 6
                            : "detector".equals(type)      ? 7
                                                           : 0);
            if (itemType[item] == 0)
                return false;
            cells[item] = (byte) number(meta[row], "cells");
            stack[item] = (short) number(meta[row], "stack");
            value[item] = (short) number(meta[row], "value");
            health[item] = (byte) number(meta[row], "health");
            bleeding[item] = (byte) number(meta[row], "bleeding");
            physical[item] = (byte) number(meta[row], "physical");
            anomaly[item] = (byte) number(meta[row], "anomaly");
            radiation[item] = (byte) number(meta[row], "radiation");
            ammo[item] = (short) number(meta[row], "ammo");
            magazine[item] = (short) number(meta[row], "magazine");
            damage[item] = (short) number(meta[row], "damage");
            range[item] = (short) number(meta[row], "range");
            cooldown[item] = (short) number(meta[row], "cooldown");
            reload[item] = (short) number(meta[row], "reload");
            spread[item] = (byte) number(meta[row], "spread");
            durability[item] = (byte) number(meta[row], "durability");
            damageBonus[item] = (short) number(meta[row], "damageBonus");
        }
        return true;
    }
    private static String field(String metadata, String name) {
        String prefix = name + "=";
        int from = 0;
        while (from <= metadata.length()) {
            int end = metadata.indexOf(',', from);
            if (end < 0)
                end = metadata.length();
            if (metadata.regionMatches(false, from, prefix, 0, prefix.length()))
                return metadata.substring(from + prefix.length(), end);
            from = end + 1;
        }
        return null;
    }
    private static int number(String metadata, String name) {
        String v = field(metadata, name);
        return v == null ? 0 : Integer.parseInt(v);
    }
    private void clear() {
        table = null;
        key = text = meta = null;
        id = first = count = null;
        itemType = null;
    }
    public boolean hasRequiredData() {
        return itemType != null && containsCoreIds() && validItem(GameIds.ITEM_PISTOL)
                && validItem(GameIds.ITEM_RIFLE) && validItem(GameIds.ITEM_LEATHER_ARMOR)
                && validItem(GameIds.ITEM_MEDKIT) && validItem(GameIds.ITEM_BANDAGE)
                && validItem(GameIds.ITEM_STONE) && validItem(GameIds.ITEM_CRYSTAL)
                && validItem(GameIds.ITEM_AMMO_9MM) && validItem(GameIds.ITEM_AMMO_545);
    }
    public boolean validItem(int item) {
        return item > 0 && item < itemType.length && itemType[item] != 0;
    }
    public int maxItemId() {
        return itemType.length - 1;
    }
    public int itemType(int i) {
        return itemType[i];
    }
    public int cells(int i) {
        return cells[i] & 255;
    }
    public int stack(int i) {
        return stack[i] & 65535;
    }
    public int value(int i) {
        return value[i] & 65535;
    }
    public int health(int i) {
        return health[i];
    }
    public int bleeding(int i) {
        return bleeding[i];
    }
    public int physical(int i) {
        return physical[i];
    }
    public int anomaly(int i) {
        return anomaly[i];
    }
    public int radiation(int i) {
        return radiation[i];
    }
    public int ammo(int i) {
        return ammo[i] & 65535;
    }
    public int magazine(int i) {
        return magazine[i] & 65535;
    }
    public int damage(int i) {
        return damage[i];
    }
    public int range(int i) {
        return range[i];
    }
    public int cooldown(int i) {
        return cooldown[i] & 65535;
    }
    public int reload(int i) {
        return reload[i] & 65535;
    }
    public int spread(int i) {
        return spread[i];
    }
    public int durability(int i) {
        return durability[i];
    }
    public int damageBonus(int i) {
        return damageBonus[i];
    }
    public boolean containsCoreIds() {
        if (table == null)
            return false;
        return find("npcs", GameIds.NPC_SIDOROVICH) >= 0 && find("npcs", GameIds.NPC_WOLF) >= 0
                && find("npcs", GameIds.NPC_TECHNICIAN) >= 0
                && find("factions", GameIds.FACTION_LONER) >= 0
                && find("dialogs", GameIds.DIALOG_INTRO) >= 0
                && find("dialogs", GameIds.DIALOG_TRADE) >= 0
                && find("dialogs", GameIds.DIALOG_REPAIR) >= 0
                && find("dialogs", GameIds.DIALOG_STASH_REPORT) >= 0
                && find("quests", GameIds.QUEST_FIND_STASH) >= 0;
    }
}
