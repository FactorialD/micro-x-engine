package com.microx.tools;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Desktop converter and strict validator for editable assets. */
public final class AssetConverter {
    private static final int MAGIC = 0x4d584c32, VERSION = 3, MAX_FIXED = 32767;
    // GameplayTables wire-format contract. Keep these values synchronized with
    // GameplayTables and sdk/python/microx_editor/data.py.
    static final int GAMEPLAY_MAGIC = 0x4d584732, MAX_GAMEPLAY_TABLES = 16, MAX_GAMEPLAY_ROWS = 256,
                     MAX_GAMEPLAY_TOTAL_ROWS = 1024, MAX_GAMEPLAY_BYTES = 32768,
                     MAX_MODIFIED_UTF_BYTES = 65535;
    private AssetConverter() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2)
            throw new IllegalArgumentException("usage: AssetConverter <source-dir> <output-dir>");
        final Path source = Paths.get(args[0]).toAbsolutePath().normalize(),
                   output = Paths.get(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(output);
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> convert(source, output, path));
        }
    }
    private static void convert(Path root, Path output, Path input) {
        try {
            Path relative = root.relativize(input);
            String name = relative.getFileName().toString();
            if (isLevel(relative))
                validateAndCopyLevel(input, output.resolve(relative));
            else if (name.equals("geometry.txt")) {
                Path temporary = Files.createTempFile("microx-geometry-", ".validation");
                try {
                    writeModel(input, temporary);
                } finally {
                    Files.deleteIfExists(temporary);
                }
                Path target = output.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            } else if (name.equals("textures.png"))
                writeTexture(input, output.resolve(replaceSuffix(relative, ".png", ".tex")));
            else if (name.endsWith(".mid")) {
                Path target = output.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    private static boolean isLevel(Path relative) {
        return relative.getNameCount() == 3 && "levels".equals(relative.getName(0).toString())
                && "level.txt".equals(relative.getFileName().toString());
    }
    private static Path replaceSuffix(Path p, String old, String n) {
        String v = p.toString();
        return Paths.get(v.substring(0, v.length() - old.length()) + n);
    }
    /** Optional desktop-only binary export; Java ME reads the source UTF-8 tables directly. */
    public static void writeGameplayData(Path data, Path output) throws IOException {
        Map<String, List<DataRow>> tables = readData(data);
        validateGameplayShape(tables);
        validateReferences(tables);
        validateStringSizes(tables);
        validateDialogCycles(tables);
        validateQuestPrerequisites(tables);
        validateStoryContent(tables);
        validateRecordStoreLimits(tables);
        Files.createDirectories(output.getParent());
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(output))) {
            out.writeInt(GAMEPLAY_MAGIC);
            out.writeByte(tables.size());
            for (Map.Entry<String, List<DataRow>> e : tables.entrySet()) {
                out.writeUTF(e.getKey());
                out.writeShort(e.getValue().size());
                for (DataRow r : e.getValue()) {
                    out.writeShort(r.id);
                    out.writeUTF(r.key);
                    out.writeUTF(r.text);
                    out.writeUTF(r.meta);
                }
            }
        }
    }
    static Map<String, List<DataRow>> readData(Path root) throws IOException {
        Map<String, List<DataRow>> result = new TreeMap<String, List<DataRow>>();
        Map<String, Path> tablePaths = new HashMap<String, Path>();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = new ArrayList<Path>();
            for (Iterator<Path> all = paths.filter(Files::isRegularFile).sorted().iterator();
                    all.hasNext();) {
                Path p = all.next();
                String name = p.getFileName().toString();
                if (name.endsWith(".data"))
                    throw new IOException(
                            p + ": legacy .data gameplay file is forbidden; rename it to .txt");
                if (name.endsWith(".txt"))
                    files.add(p);
            }
            for (Iterator<Path> it = files.iterator(); it.hasNext();) {
                Path p = it.next();
                String fileName = p.getFileName().toString();
                String table = fileName.substring(0, fileName.length() - 4);
                List<DataRow> rows = new ArrayList<DataRow>();
                int line = 0;
                Set<Integer> ids = new HashSet<Integer>();
                Set<String> keys = new HashSet<String>();
                for (String raw : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    line++;
                    if (raw.trim().length() == 0 || raw.trim().startsWith("#"))
                        continue;
                    String[] q = raw.split("\\|", -1);
                    if (q.length < 3 || q.length > 4)
                        throw new IOException(p + ":" + line + ": expected id|key|description");
                    int id = parseInt(q[0], p, line, "id");
                    if (id <= 0 || id > 65535 || !ids.add(Integer.valueOf(id)))
                        throw new IOException(p + ":" + line + ": duplicate or invalid stable id");
                    if (q[1].length() == 0 || !keys.add(q[1]))
                        throw new IOException(p + ":" + line + ": duplicate or empty stable key");
                    rows.add(new DataRow(id, q[1], q[2], q.length > 3 ? q[3] : "", p, line));
                }
                Path previous = tablePaths.put(table, p);
                if (previous != null)
                    throw new IOException(
                            "duplicate table name " + table + ": " + previous + " and " + p);
                result.put(table, rows);
            }
        }
        return result;
    }
    public static void validateReferences(Map<String, List<DataRow>> t) throws IOException {
        validateItemMetadata(t);
        validateStashLoot(t);
        validateMetadataSchemas(t);
        Set<String> all = new HashSet<String>();
        for (Map.Entry<String, List<DataRow>> e : t.entrySet())
            for (DataRow r : e.getValue()) all.add(e.getKey() + ":" + r.id);
        for (List<DataRow> rows : t.values())
            for (DataRow r : rows)
                for (String token : r.meta.split(","))
                    if (token.startsWith("ref=")) {
                        String ref = token.substring(4);
                        if (!all.contains(ref))
                            throw r.error("unknown reference " + ref);
                    }
    }
    private static void validateStashLoot(Map<String, List<DataRow>> tables) throws IOException {
        List<DataRow> rows = tables.get("stash_loot"), items = tables.get("items");
        if (rows == null || rows.isEmpty())
            throw new IOException("stash_loot table is required");
        Set<Integer> itemIds = new HashSet<Integer>();
        if (items != null)
            for (DataRow item : items) itemIds.add(Integer.valueOf(item.id));
        for (DataRow row : rows) {
            int tier = metaInt(row, "tier", 0), weight = metaInt(row, "weight", 0),
                item = metaInt(row, "item", 0), min = metaInt(row, "min", 0),
                max = metaInt(row, "max", -1);
            if (tier < 1 || tier > 16 || weight < 1 || weight > 100 || min < 1 || max < min
                    || !itemIds.contains(Integer.valueOf(item)))
                throw row.error("invalid deterministic stash loot metadata");
        }
    }
    private static void validateMetadataSchemas(Map<String, List<DataRow>> tables)
            throws IOException {
        for (Map.Entry<String, List<DataRow>> table : tables.entrySet())
            for (DataRow row : table.getValue()) {
                Map<String, String> values = metadata(row);
                if ("items".equals(table.getKey()))
                    continue;
                Set<String> allowed = new HashSet<String>();
                if ("dialogs".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("next", "ref"));
                else if ("quests".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("requires", "ref", "description", "objective"));
                else if ("story_nodes".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("entry", "next", "alt", "quest", "objective",
                            "reward", "scene", "ending", "location", "spawn"));
                else if ("slides".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("scene", "order", "duration"));
                else if ("objectives".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("quest", "marker", "kind", "target", "amount"));
                else if ("rewards".equals(table.getKey()))
                    allowed.addAll(
                            Arrays.asList("money", "item", "amount", "faction", "reputation"));
                else if ("endings".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("freeplay", "location", "spawn", "scene"));
                else if ("cyclic_quests".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("objective", "reward", "cooldown", "next"));
                else if ("arena".equals(table.getKey()))
                    allowed.addAll(Arrays.asList(
                            "fee", "waves", "weapon", "ammo", "reward", "location", "returnSpawn"));
                else if ("npcs".equals(table.getKey()))
                    allowed.add("ref");
                else if ("traders".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("faction", "money", "stock"));
                else if ("relationships".equals(table.getKey()))
                    allowed.addAll(Arrays.asList("from", "to", "relation"));
                else if ("stash_loot".equals(table.getKey()))
                    allowed.addAll(
                            Arrays.asList("tier", "weight", "item", "min", "max", "location"));
                for (String key : values.keySet())
                    if (!allowed.contains(key))
                        throw row.error("unknown metadata key " + key);
                if (values.containsKey("next"))
                    range(row, values, "next", 1, 65535);
                if (values.containsKey("requires"))
                    range(row, values, "requires", 1, 65535);
                if ("traders".equals(table.getKey())) {
                    if (find(tables.get("npcs"), row.id) == null)
                        throw row.error("trader profile does not match a stable NPC id");
                    rangeRequired(row, values, "faction", 1, 65535);
                    int faction = Integer.parseInt(values.get("faction"));
                    if (find(tables.get("factions"), faction) == null)
                        throw row.error("unknown trader faction " + faction);
                    rangeRequired(row, values, "money", 0, 32767);
                    required(row, values, "stock");
                    String[] entries = values.get("stock").split(";");
                    for (String entry : entries) {
                        String[] pair = entry.split(":");
                        if (pair.length != 2)
                            throw row.error("invalid stock entry " + entry);
                        int item = parseInt(pair[0], row.file, row.line, "stock item");
                        int amount = parseInt(pair[1], row.file, row.line, "stock amount");
                        if (find(tables.get("items"), item) == null || amount < 1 || amount > 32767)
                            throw row.error("invalid stock entry " + entry);
                    }
                }
                if ("relationships".equals(table.getKey())) {
                    rangeRequired(row, values, "from", 1, 65535);
                    rangeRequired(row, values, "to", 1, 65535);
                    rangeRequired(row, values, "relation", -1, 1);
                    if (find(tables.get("factions"), Integer.parseInt(values.get("from"))) == null
                            || find(tables.get("factions"), Integer.parseInt(values.get("to")))
                                    == null)
                        throw row.error("unknown faction in relationship");
                }
            }
    }
    private static void validateItemMetadata(Map<String, List<DataRow>> tables) throws IOException {
        List<DataRow> items = tables.get("items");
        if (items == null || items.size() == 0)
            throw new IOException("required items table is missing or empty");
        Set<Integer> itemIds = new HashSet<Integer>();
        for (DataRow row : items) itemIds.add(Integer.valueOf(row.id));
        String[] common = {"type", "cells", "stack", "value"};
        for (DataRow row : items) {
            Map<String, String> m = metadata(row);
            for (String key : common) required(row, m, key);
            String type = m.get("type");
            range(row, m, "cells", 1, 64);
            range(row, m, "stack", 1, 32767);
            range(row, m, "value", 0, 32767);
            Set<String> allowed = new HashSet<String>(Arrays.asList(common));
            if ("weapon".equals(type)) {
                String[] keys = {"ammo", "magazine", "damage", "range", "cooldown", "reload",
                        "spread", "durability"};
                allowed.addAll(Arrays.asList(keys));
                for (String key : keys) required(row, m, key);
                int ammo = range(row, m, "ammo", 1, 65535);
                if (!itemIds.contains(Integer.valueOf(ammo)))
                    throw row.error("unknown ammo item " + ammo);
                range(row, m, "magazine", 1, 255);
                range(row, m, "damage", 1, 32767);
                range(row, m, "range", 1, 32767);
                range(row, m, "cooldown", 1, 32767);
                range(row, m, "reload", 1, 32767);
                range(row, m, "spread", 0, 90);
                range(row, m, "durability", 1, 100);
            } else if ("armor".equals(type) || "artifact".equals(type)) {
                allowed.addAll(Arrays.asList("physical", "anomaly", "radiation"));
                rangeRequired(row, m, "physical", -100, 100);
                rangeRequired(row, m, "anomaly", -100, 100);
                rangeRequired(row, m, "radiation", -100, 100);
            } else if ("consumable".equals(type)) {
                allowed.addAll(Arrays.asList("health", "bleeding", "radiation"));
                rangeRequired(row, m, "health", 0, 100);
                rangeRequired(row, m, "bleeding", 0, 100);
                rangeRequired(row, m, "radiation", -100, 100);
            } else if ("ammo".equals(type)) {
                allowed.add("damageBonus");
                rangeRequired(row, m, "damageBonus", -1000, 1000);
            } else if (!"bolt".equals(type) && !"detector".equals(type))
                throw row.error("unknown item type " + type);
            for (String key : m.keySet())
                if (!allowed.contains(key))
                    throw row.error("unknown metadata key " + key);
        }
        for (DataRow row : items)
            if ("weapon".equals(metadata(row).get("type"))) {
                int ammo = Integer.parseInt(metadata(row).get("ammo"));
                if (!"ammo".equals(metadata(find(items, ammo)).get("type")))
                    throw row.error("ammo does not reference an ammo item");
            }
    }
    private static DataRow find(List<DataRow> rows, int id) {
        for (DataRow r : rows)
            if (r.id == id)
                return r;
        return null;
    }
    private static Map<String, String> metadata(DataRow row) throws IOException {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (row.meta.length() == 0)
            return values;
        for (String token : row.meta.split(",", -1)) {
            int at = token.indexOf('=');
            if (at < 1 || at == token.length() - 1)
                throw row.error("invalid metadata token " + token);
            String key = token.substring(0, at), value = token.substring(at + 1);
            if (values.put(key, value) != null)
                throw row.error("duplicate metadata key " + key);
        }
        return values;
    }
    private static void required(DataRow r, Map<String, String> m, String k) throws IOException {
        if (!m.containsKey(k))
            throw r.error("missing metadata key " + k);
    }
    private static int rangeRequired(DataRow r, Map<String, String> m, String k, int lo, int hi)
            throws IOException {
        required(r, m, k);
        return range(r, m, k, lo, hi);
    }
    private static int range(DataRow r, Map<String, String> m, String k, int lo, int hi)
            throws IOException {
        int n = parseInt(m.get(k), r.file, r.line, k);
        if (n < lo || n > hi)
            throw r.error(k + " outside " + lo + ".." + hi);
        return n;
    }
    public static void validateStringSizes(Map<String, List<DataRow>> t) throws IOException {
        for (List<DataRow> rows : t.values())
            for (DataRow r : rows) {
                if (r.key.length() < 1 || r.key.length() > 32)
                    throw r.error("key exceeds 32 characters");
                if (r.text.getBytes(StandardCharsets.UTF_8).length > 240)
                    throw r.error("description exceeds 240 UTF-8 bytes");
            }
    }
    public static void validateDialogCycles(Map<String, List<DataRow>> t) throws IOException {
        List<DataRow> rows = t.get("dialogs");
        if (rows == null)
            return;
        Map<Integer, Integer> next = new HashMap<Integer, Integer>();
        for (DataRow r : rows) {
            int n = metaInt(r, "next", -1);
            if (n >= 0)
                next.put(Integer.valueOf(r.id), Integer.valueOf(n));
        }
        for (DataRow start : rows) {
            Set<Integer> seen = new HashSet<Integer>();
            int n = start.id;
            while (next.containsKey(Integer.valueOf(n))) {
                if (!seen.add(Integer.valueOf(n)))
                    throw start.error("unconditional dialog cycle");
                n = next.get(Integer.valueOf(n)).intValue();
            }
        }
    }
    public static void validateQuestPrerequisites(Map<String, List<DataRow>> t) throws IOException {
        List<DataRow> rows = t.get("quests");
        if (rows == null)
            return;
        Map<Integer, DataRow> byId = new HashMap<Integer, DataRow>();
        for (DataRow r : rows) byId.put(Integer.valueOf(r.id), r);
        for (DataRow start : rows) {
            Set<Integer> seen = new HashSet<Integer>();
            int n = start.id;
            while (n > 0) {
                if (!seen.add(Integer.valueOf(n)))
                    throw start.error("quest prerequisite cycle");
                DataRow r = byId.get(Integer.valueOf(n));
                if (r == null)
                    throw start.error("unknown quest prerequisite " + n);
                n = metaInt(r, "requires", -1);
            }
        }
    }
    /** Validates the complete narrative graph before it can reach a device. */
    public static void validateStoryContent(Map<String, List<DataRow>> t) throws IOException {
        List<DataRow> nodes = t.get("story_nodes"), endings = t.get("endings");
        if (nodes == null || nodes.size() == 0)
            throw new IOException("required story_nodes table is missing or empty");
        if (endings == null || endings.size() == 0)
            throw new IOException("required endings table is missing or empty");
        Map<Integer, DataRow> graph = new HashMap<Integer, DataRow>();
        DataRow entry = null;
        for (DataRow row : nodes) {
            graph.put(Integer.valueOf(row.id), row);
            if (metaInt(row, "entry", 0) == 1) {
                if (entry != null)
                    throw row.error("multiple story entries");
                entry = row;
            }
        }
        if (entry == null)
            throw new IOException("story has no entry node");
        Set<Integer> reachable = new HashSet<Integer>();
        visitStory(entry, graph, reachable);
        if (reachable.size() != nodes.size())
            throw new IOException("unreachable story node");
        boolean realEnding = false, freeplay = false;
        for (DataRow row : endings) {
            if (metaInt(row, "freeplay", 0) == 0)
                realEnding = true;
            if (metaInt(row, "freeplay", 0) == 1 && metadata(row).containsKey("location"))
                freeplay = true;
        }
        if (!realEnding)
            throw new IOException("story needs at least one real ending");
        if (!freeplay)
            throw new IOException("story needs a freeplay transition");
        for (DataRow row : nodes) {
            int ending = metaInt(row, "ending", 0);
            if (ending > 0 && find(endings, ending) == null)
                throw row.error("unknown ending " + ending);
            for (String edge : new String[] {"next", "alt"}) {
                int n = metaInt(row, edge, 0);
                if (n > 0 && !graph.containsKey(Integer.valueOf(n)))
                    throw row.error("unknown " + edge + " node " + n);
            }
        }
        for (DataRow row : nodes)
            if (!hasExit(row.id, graph, new HashSet<Integer>(), new HashSet<Integer>()))
                throw row.error("story cycle has no ending exit");
    }
    private static void visitStory(DataRow row, Map<Integer, DataRow> graph, Set<Integer> seen)
            throws IOException {
        if (!seen.add(Integer.valueOf(row.id)))
            return;
        for (String edge : new String[] {"next", "alt"}) {
            int n = metaInt(row, edge, 0);
            if (n > 0 && graph.containsKey(Integer.valueOf(n)))
                visitStory(graph.get(Integer.valueOf(n)), graph, seen);
        }
    }
    private static boolean hasExit(int id, Map<Integer, DataRow> graph, Set<Integer> path,
            Set<Integer> dead) throws IOException {
        DataRow row = graph.get(Integer.valueOf(id));
        if (metaInt(row, "ending", 0) > 0)
            return true;
        if (dead.contains(Integer.valueOf(id)) || !path.add(Integer.valueOf(id)))
            return false;
        boolean exit = false;
        for (String edge : new String[] {"next", "alt"}) {
            int n = metaInt(row, edge, 0);
            if (n > 0 && graph.containsKey(Integer.valueOf(n)))
                exit |= hasExit(n, graph, new HashSet<Integer>(path), dead);
        }
        if (!exit)
            dead.add(Integer.valueOf(id));
        return exit;
    }

    public static void validateRecordStoreLimits(Map<String, List<DataRow>> t) throws IOException {
        long bytes = 4 + 1;
        for (Map.Entry<String, List<DataRow>> table : t.entrySet()) {
            bytes += utfSize(table.getKey(), "table name " + table.getKey()) + 2;
            for (DataRow r : table.getValue()) {
                bytes += 2;
                bytes += utfSize(r.key, r.file + ":" + r.line + ": key");
                bytes += utfSize(r.text, r.file + ":" + r.line + ": description");
                bytes += utfSize(r.meta, r.file + ":" + r.line + ": metadata");
            }
        }
        if (bytes > MAX_GAMEPLAY_BYTES)
            throw new IOException("gameplay tables exceed " + MAX_GAMEPLAY_BYTES
                    + "-byte RecordStore budget (serialized size " + bytes + ")");
    }
    private static void validateGameplayShape(Map<String, List<DataRow>> tables)
            throws IOException {
        if (tables.size() > MAX_GAMEPLAY_TABLES)
            throw new IOException("gameplay table count exceeds " + MAX_GAMEPLAY_TABLES);
        int total = 0;
        for (Map.Entry<String, List<DataRow>> table : tables.entrySet()) {
            utfSize(table.getKey(), "table name " + table.getKey());
            if (table.getValue().size() > MAX_GAMEPLAY_ROWS)
                throw new IOException(
                        "table " + table.getKey() + " exceeds " + MAX_GAMEPLAY_ROWS + " rows");
            total += table.getValue().size();
        }
        if (total > MAX_GAMEPLAY_TOTAL_ROWS)
            throw new IOException("gameplay row count exceeds " + MAX_GAMEPLAY_TOTAL_ROWS);
    }
    /** Bytes emitted by DataOutput.writeUTF, including its unsigned-short prefix. */
    private static int utfSize(String value, String label) throws IOException {
        int bytes = 0;
        for (int i = 0; i < value.length(); i++) {
            int c = value.charAt(i);
            bytes += c >= 0x0001 && c <= 0x007f ? 1 : c <= 0x07ff ? 2 : 3;
            if (bytes > MAX_MODIFIED_UTF_BYTES)
                throw new IOException(label + " exceeds modified UTF-8 limit of "
                        + MAX_MODIFIED_UTF_BYTES + " bytes");
        }
        return bytes + 2;
    }
    private static int metaInt(DataRow r, String name, int fallback) throws IOException {
        for (String s : r.meta.split(","))
            if (s.startsWith(name + "="))
                return parseInt(s.substring(name.length() + 1), r.file, r.line, name);
        return fallback;
    }
    public static final class DataRow {
        public final int id, line;
        public final String key, text, meta;
        public final Path file;
        DataRow(int i, String k, String x, String m, Path f, int l) {
            id = i;
            key = k;
            text = x;
            meta = m;
            file = f;
            line = l;
        }
        IOException error(String s) {
            return new IOException(file + ":" + line + ": " + s);
        }
    }
    public static void validateAndCopyLevel(Path input, Path output) throws IOException {
        Tokens t = new Tokens(input);
        t.expect("MXL2");
        t.expect("environment");
        int sky = t.color(), wall = t.color(), floorColor = t.color();
        if (sky == wall || sky == floorColor || wall == floorColor)
            t.fail("sky, wall and floor colors must be distinct");
        writePaletteTexture(output.resolveSibling("textures.tex"), sky, wall, floorColor);
        t.expect("counts");
        int rooms = t.count(1, 256), floors = t.count(1, 1024), ceilings = t.count(1, 1024),
            edges = t.count(0, 2048), portals = t.count(0, 1024), spawns = t.count(1, 256),
            transitions = t.count(0, 256), entities = t.count(0, 1024), capacity = t.count(1, 1024);
        if (entities > capacity)
            t.fail("entity pool capacity exceeded");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(MAGIC);
        out.writeShort(VERSION);
        int[] counts = {
                rooms, floors, ceilings, edges, portals, spawns, transitions, entities, capacity};
        for (int n : counts) out.writeShort(n);
        int i;
        for (i = 0; i < rooms; i++) {
            t.expect("room");
            int a = t.fixed(), b = t.fixed(), c = t.fixed(), d = t.fixed();
            ordered(t, a, b, c, d);
            out.writeInt(a);
            out.writeInt(b);
            out.writeInt(c);
            out.writeInt(d);
        }
        for (i = 0; i < floors; i++) {
            t.expect("floor");
            out.writeShort(t.index(rooms));
            bounds(t, out);
            out.writeInt(t.fixed());
        }
        for (i = 0; i < ceilings; i++) {
            t.expect("ceiling");
            out.writeShort(t.index(rooms));
            bounds(t, out);
            out.writeInt(t.fixed());
        }
        for (i = 0; i < edges; i++) {
            t.expect("edge");
            out.writeShort(t.index(rooms));
            for (int q = 0; q < 6; q++) out.writeInt(t.fixed());
        }
        int[] reverse = new int[portals], portalTransition = new int[portals];
        for (i = 0; i < portals; i++) {
            t.expect("portal");
            out.writeShort(t.id());
            out.writeShort(t.index(rooms));
            out.writeShort(t.index(rooms));
            int a = t.fixed(), b = t.fixed(), c = t.fixed(), d = t.fixed(), e = t.fixed(),
                f = t.fixed();
            if (a > b || c > d || e > f)
                t.fail("unordered portal bounds");
            out.writeInt(a);
            out.writeInt(b);
            out.writeInt(c);
            out.writeInt(d);
            out.writeInt(e);
            out.writeInt(f);
            reverse[i] = t.signedIndex(portals);
            portalTransition[i] = t.signedIndex(transitions);
            out.writeShort(reverse[i]);
            out.writeShort(portalTransition[i]);
        }
        for (i = 0; i < portals; i++)
            if (reverse[i] >= 0 && reverse[reverse[i]] != i)
                t.fail("portal reverse link is not bidirectional");
        for (i = 0; i < spawns; i++) {
            t.expect("spawn");
            out.writeShort(t.id());
            out.writeShort(t.index(rooms));
            out.writeInt(t.fixed());
            out.writeInt(t.fixed());
            out.writeInt(t.fixed());
            out.writeShort(t.range(-32768, 32767));
        }
        for (i = 0; i < transitions; i++) {
            t.expect("transition");
            out.writeShort(t.id());
            out.writeShort(t.id());
            String location = t.next();
            if (!location.matches("[A-Za-z0-9_-]{1,64}"))
                t.fail("invalid location identifier");
            out.writeUTF(location);
        }
        for (i = 0; i < entities; i++) {
            t.expect("entity");
            out.writeInt(t.id());
            out.writeShort(t.id());
            out.writeInt(t.fixed());
            out.writeInt(t.fixed());
            out.writeInt(t.fixed());
            out.writeShort(t.id());
            out.writeShort(t.id()); // faction
            out.writeShort(t.id()); // sprite
            out.writeShort(t.id()); // gameplay aux/content id
        }
        if (t.hasNext())
            t.fail("unexpected token " + t.next());
        out.close();
        Files.createDirectories(output.getParent());
        Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
    }
    private static void writePaletteTexture(Path output, int sky, int wall, int floor)
            throws IOException {
        Files.createDirectories(output.getParent());
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(output))) {
            out.writeInt(0x4d585432);
            out.writeShort(2);
            out.writeShort(3);
            int[] colors = {floor, wall, wall};
            for (int color : colors) {
                out.writeShort(1);
                out.writeShort(1);
                out.writeShort(1);
                out.writeByte(color >> 16);
                out.writeByte(color >> 8);
                out.writeByte(color);
                out.writeByte(0);
            }
        }
    }
    private static void bounds(Tokens t, DataOutputStream out) throws IOException {
        int a = t.fixed(), b = t.fixed(), c = t.fixed(), d = t.fixed();
        ordered(t, a, b, c, d);
        out.writeInt(a);
        out.writeInt(b);
        out.writeInt(c);
        out.writeInt(d);
    }
    private static void ordered(Tokens t, int a, int b, int c, int d) throws IOException {
        if (a > b || c > d)
            t.fail("unordered bounds");
    }
    public static void writeModel(Path input, Path output) throws IOException {
        List<float[]> positions = new ArrayList<float[]>(), texcoords = new ArrayList<float[]>();
        Map<String, Material> materials = new HashMap<String, Material>();
        Map<String, Section> sections = new LinkedHashMap<String, Section>();
        int room = 0, texture = Integer.MIN_VALUE, color = 0xff00ff, lineNo = 0;
        String material = "default";
        materials.put(material, new Material(texture, color));
        for (String raw : Files.readAllLines(input, StandardCharsets.US_ASCII)) {
            lineNo++;
            String line = raw.trim();
            if (line.startsWith("# microx room ")) {
                room = parseNonNegative(line.substring(14).trim(), input, lineNo, "room");
                continue;
            }
            if (line.startsWith("# microx material ")) {
                String[] p = line.substring(18).trim().split("\\s+");
                if (p.length < 1 || p.length > 3)
                    throw objError(input, lineNo,
                            "material metadata needs NAME and optional texture=/color=");
                int materialTexture = Integer.MIN_VALUE, materialColor = 0xff00ff;
                boolean hasTexture = false, hasColor = false;
                for (int i = 1; i < p.length; i++)
                    if (p[i].startsWith("texture=")) {
                        materialTexture =
                                parseSignedShort(p[i].substring(8), input, lineNo, "texture");
                        hasTexture = true;
                    } else if (p[i].startsWith("color=")) {
                        materialColor = parseRgb(p[i].substring(6), input, lineNo);
                        hasColor = true;
                    } else
                        throw objError(input, lineNo,
                                "material attributes are texture=ID and color=RRGGBB");
                if (hasTexture && !hasColor)
                    throw objError(input, lineNo, "textured material requires fallback color");
                materials.put(p[0], new Material(materialTexture, materialColor));
                continue;
            }
            int comment = line.indexOf('#');
            if (comment >= 0)
                line = line.substring(0, comment).trim();
            if (line.length() == 0)
                continue;
            String[] p = line.split("\\s+");
            if ("v".equals(p[0])) {
                if (p.length < 4)
                    throw objError(input, lineNo, "vertex needs x y z");
                positions.add(new float[] {finite(p[1], input, lineNo), finite(p[2], input, lineNo),
                        finite(p[3], input, lineNo)});
            } else if ("vt".equals(p[0])) {
                if (p.length < 3)
                    throw objError(input, lineNo, "texture coordinate needs u v");
                texcoords.add(
                        new float[] {finite(p[1], input, lineNo), finite(p[2], input, lineNo)});
            } else if ("usemtl".equals(p[0])) {
                if (p.length != 2)
                    throw objError(input, lineNo, "usemtl needs one name");
                material = p[1];
                Material definition = materials.get(material);
                if (definition == null)
                    throw objError(input, lineNo, "unknown material; declare metadata first");
                texture = definition.texture;
                color = definition.color;
            } else if ("o".equals(p[0]) || "g".equals(p[0])) {
                if (p.length > 1 && p[1].startsWith("room_"))
                    room = parseNonNegative(p[1].substring(5), input, lineNo, "room");
            } else if ("f".equals(p[0])) {
                if (p.length < 4)
                    throw objError(input, lineNo, "face needs at least three corners");
                String key = room + "/" + texture + "/" + color;
                Section section = sections.get(key);
                if (section == null) {
                    section = new Section(room, texture, color);
                    sections.put(key, section);
                }
                int[] polygon = new int[p.length - 1];
                for (int i = 1; i < p.length; i++) {
                    String[] q = p[i].split("/", -1);
                    if (q.length < 2 || q[0].length() == 0 || q[1].length() == 0)
                        throw objError(input, lineNo, "faces require v/vt corners");
                    int vi = resolveObj(q[0], positions.size(), input, lineNo),
                        ti = resolveObj(q[1], texcoords.size(), input, lineNo);
                    polygon[i - 1] =
                            section.vertex(positions.get(vi), texcoords.get(ti), input, lineNo);
                }
                for (int i = 1; i < polygon.length - 1; i++) {
                    int ia = polygon[0], ib = polygon[i], ic = polygon[i + 1];
                    if (section.degenerate(ia, ib, ic))
                        throw objError(input, lineNo, "degenerate triangle/winding");
                    section.indices.add(Integer.valueOf(ia));
                    section.indices.add(Integer.valueOf(ib));
                    section.indices.add(Integer.valueOf(ic));
                }
            }
        }
        if (positions.size() == 0 || sections.size() == 0)
            throw new IOException(input + ": OBJ has no renderable faces");
        if (sections.size() > 65535)
            throw new IOException(input + ": too many mesh sections");
        Files.createDirectories(output.getParent());
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(output))) {
            out.writeInt(0x4d584d32);
            out.writeShort(2);
            out.writeShort(sections.size());
            for (Section x : sections.values()) {
                out.writeShort(x.room);
                out.writeShort(x.texture);
                out.writeInt(x.color);
                out.writeShort(x.xyz.size() / 3);
                out.writeShort(x.indices.size() / 3);
                for (Integer n : x.xyz) out.writeInt(n.intValue());
                for (Integer n : x.uv) out.writeInt(n.intValue());
                for (Integer n : x.indices) out.writeShort(n.intValue());
            }
        }
    }
    private static final class Section {
        final int room, texture, color;
        final List<Integer> xyz = new ArrayList<Integer>(), uv = new ArrayList<Integer>(),
                            indices = new ArrayList<Integer>();
        final Map<String, Integer> vertices = new LinkedHashMap<String, Integer>();
        Section(int r, int t, int c) {
            room = r;
            texture = t;
            color = c;
        }
        int vertex(float[] p, float[] t, Path file, int line) throws IOException {
            int x = fixed(p[0], file, line), y = fixed(p[1], file, line),
                z = fixed(p[2], file, line), u = fixed(t[0], file, line),
                v = fixed(t[1], file, line);
            String key = x + "," + y + "," + z + "," + u + "," + v;
            Integer old = vertices.get(key);
            if (old != null)
                return old.intValue();
            int n = vertices.size();
            if (n >= 65535)
                throw objError(file, line, "section has more than 65535 vertices");
            vertices.put(key, Integer.valueOf(n));
            xyz.add(Integer.valueOf(x));
            xyz.add(Integer.valueOf(y));
            xyz.add(Integer.valueOf(z));
            uv.add(Integer.valueOf(u));
            uv.add(Integer.valueOf(v));
            return n;
        }
        boolean degenerate(int a, int b, int c) {
            long ax = xyz.get(a * 3), ay = xyz.get(a * 3 + 1), az = xyz.get(a * 3 + 2),
                 bx = xyz.get(b * 3), by = xyz.get(b * 3 + 1), bz = xyz.get(b * 3 + 2),
                 cx = xyz.get(c * 3), cy = xyz.get(c * 3 + 1), cz = xyz.get(c * 3 + 2);
            long ux = bx - ax, uy = by - ay, uz = bz - az, vx = cx - ax, vy = cy - ay, vz = cz - az;
            return uy * vz - uz * vy == 0 && uz * vx - ux * vz == 0 && ux * vy - uy * vx == 0;
        }
    }
    private static final class Material {
        final int texture, color;
        Material(int textureId, int fallbackColor) {
            texture = textureId;
            color = fallbackColor;
        }
    }
    private static int parseRgb(String s, Path file, int line) throws IOException {
        if (s.length() != 6)
            throw objError(file, line, "RGB888 must be six hexadecimal digits");
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            throw objError(file, line, "invalid RGB888");
        }
    }
    private static int resolveObj(String value, int size, Path file, int line) throws IOException {
        int n;
        try {
            n = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw objError(file, line, "invalid OBJ index");
        }
        if (n == 0)
            throw objError(file, line, "OBJ indices are never zero");
        int result = n > 0 ? n - 1 : size + n;
        if (result < 0 || result >= size)
            throw objError(file, line, "OBJ index out of range");
        return result;
    }
    private static int materialTexture(String name, Path file, int line) throws IOException {
        int split = name.lastIndexOf('_');
        if (split < 0)
            throw objError(
                    file, line, "unknown material; declare '# microx material NAME TEXTURE_ID'");
        return parseSignedShort(name.substring(split + 1), file, line, "texture");
    }
    private static int parseNonNegative(String s, Path f, int l, String what) throws IOException {
        int n = parseInt(s, f, l, what);
        if (n < 0 || n > 65535)
            throw objError(f, l, what + " out of range");
        return n;
    }
    private static int parseSignedShort(String s, Path f, int l, String what) throws IOException {
        int n = parseInt(s, f, l, what);
        if (n < -32768 || n > 32767)
            throw objError(f, l, what + " out of range");
        return n;
    }
    private static int parseInt(String s, Path f, int l, String what) throws IOException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw objError(f, l, "invalid " + what);
        }
    }
    private static float finite(String s, Path f, int l) throws IOException {
        try {
            float n = Float.parseFloat(s);
            if (Float.isNaN(n) || Float.isInfinite(n))
                throw objError(f, l, "non-finite number");
            return n;
        } catch (NumberFormatException e) {
            throw objError(f, l, "invalid number");
        }
    }
    private static int fixed(float n, Path f, int l) throws IOException {
        double q = Math.rint(n * 65536.0);
        if (q < Integer.MIN_VALUE || q > Integer.MAX_VALUE)
            throw objError(f, l, "fixed-point overflow");
        return (int) q;
    }
    private static IOException objError(Path f, int l, String m) {
        return new IOException(f + ":" + l + ": " + m);
    }
    public static void writeTexture(Path input, Path output) throws IOException {
        BufferedImage im = ImageIO.read(input.toFile());
        if (im == null)
            throw new IOException("Unreadable texture atlas: " + input);
        int w = im.getWidth(), h = im.getHeight();
        if (w <= 0 || h <= 0 || w > 256 || h > 256 || (long) w * h > 65536)
            throw new IOException("Texture atlas dimensions exceed 256x256: " + input);
        LinkedHashMap<Integer, Integer> colors = new LinkedHashMap<Integer, Integer>();
        byte[] pixels = new byte[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Integer rgb = Integer.valueOf(im.getRGB(x, y) & 0xffffff), index = colors.get(rgb);
                if (index == null) {
                    if (colors.size() == 256)
                        throw new IOException("Texture atlas has more than 256 colors: " + input);
                    index = Integer.valueOf(colors.size());
                    colors.put(rgb, index);
                }
                pixels[y * w + x] = (byte) index.intValue();
            }
        long footprint = 16L + colors.size() * 4L + pixels.length;
        if (footprint > 96 * 1024L)
            throw new IOException("Texture atlas runtime footprint exceeds 96 KiB: " + input);
        Files.createDirectories(output.getParent());
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(output))) {
            out.writeInt(0x4d585432);
            out.writeShort(2);
            out.writeShort(1);
            out.writeShort(w);
            out.writeShort(h);
            out.writeShort(colors.size());
            for (Integer rgb : colors.keySet()) {
                int c = rgb.intValue();
                out.writeByte(c >> 16);
                out.writeByte(c >> 8);
                out.writeByte(c);
            }
            out.write(pixels);
        }
    }
    private static final class Tokens {
        final List<String> v = new ArrayList<String>();
        int p;
        final Path file;
        Tokens(Path f) throws IOException {
            file = f;
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                int c = line.indexOf('#');
                if (c >= 0)
                    line = line.substring(0, c);
                for (String s : line.trim().split("\\s+"))
                    if (s.length() > 0)
                        v.add(s);
            }
        }
        String next() throws IOException {
            if (p >= v.size())
                fail("unexpected end");
            return v.get(p++);
        }
        boolean hasNext() {
            return p < v.size();
        }
        void expect(String s) throws IOException {
            if (!s.equals(next()))
                fail("expected " + s);
        }
        int number() throws IOException {
            try {
                return Integer.parseInt(next());
            } catch (NumberFormatException e) {
                fail("invalid integer");
                return 0;
            }
        }
        int range(int a, int b) throws IOException {
            int n = number();
            if (n < a || n > b)
                fail("value out of range");
            return n;
        }
        int count(int a, int b) throws IOException {
            return range(a, b);
        }
        int index(int n) throws IOException {
            return range(0, n - 1);
        }
        int signedIndex(int n) throws IOException {
            return range(-1, n - 1);
        }
        int id() throws IOException {
            return range(0, 65535);
        }
        int fixed() throws IOException {
            int n = range(-MAX_FIXED, MAX_FIXED);
            return n * 65536;
        }
        int color() throws IOException {
            String value = next();
            if (!value.matches("[0-9A-Fa-f]{6}"))
                fail("expected RGB color");
            return Integer.parseInt(value, 16);
        }
        void fail(String m) throws IOException {
            throw new IOException(file + ": " + m + " at token " + p);
        }
    }
}
