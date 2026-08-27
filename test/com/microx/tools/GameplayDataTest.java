package com.microx.tools;
import com.microx.engine.gameplay.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class GameplayDataTest {
    public static void main(String[] args) throws Exception {
        Path source = Paths.get("assets-src/data"), d = Files.createTempDirectory("microx-data");
        copy(source, d);
        Path out = d.resolve("gameplay.dat");
        AssetConverter.writeGameplayData(d, out);
        GameplayTables first = new GameplayTables();
        ok(first.load(Files.newInputStream(out)) && first.hasRequiredData(),
                "complete data rejected");
        int original = first.value(GameIds.ITEM_MEDKIT);
        Path items = d.resolve("items/items.txt");
        String text = new String(Files.readAllBytes(items), StandardCharsets.UTF_8);
        Files.write(items, text.replace("value=300", "value=777").getBytes(StandardCharsets.UTF_8));
        AssetConverter.writeGameplayData(d, out);
        GameplayTables changed = new GameplayTables();
        changed.load(Files.newInputStream(out));
        ok(changed.value(GameIds.ITEM_MEDKIT) == 777
                        && changed.value(GameIds.ITEM_MEDKIT) != original,
                "text-only balance edit did not reach runtime catalog");
        Files.delete(items);
        rejected(d, out, "missing items table accepted");
        copy(source, d);
        items = d.resolve("items/items.txt");
        text = new String(Files.readAllBytes(items), StandardCharsets.UTF_8);
        Files.write(
                items, text.replace("1|pistol|", "10|pistol|").getBytes(StandardCharsets.UTF_8));
        AssetConverter.writeGameplayData(d, out);
        GameplayTables missingCore = new GameplayTables();
        missingCore.load(Files.newInputStream(out));
        ok(!missingCore.hasRequiredData(), "missing core item ID accepted");
        Files.write(d.resolve("old.data"), "1|old|Old\n".getBytes(StandardCharsets.UTF_8));
        rejected(d, out, "legacy .data accepted");
        formatLimits(source);
        System.out.println("GameplayDataTest OK");
    }
    private static void formatLimits(Path source) throws Exception {
        Path d = Files.createTempDirectory("microx-data-limits"), out = d.resolve("gameplay.dat");
        copy(source, d);
        for (int i = 0; i < 12; i++) writeRows(d.resolve("extra/t" + i + ".txt"), 0);
        rejected(d, out, "17 tables accepted");

        d = Files.createTempDirectory("microx-data-rows"); copy(source, d); out=d.resolve("x.dat");
        writeRows(d.resolve("extra/large.txt"), 257);
        rejected(d, out, "257 rows accepted");
        d = Files.createTempDirectory("microx-data-total"); copy(source, d); out=d.resolve("x.dat");
        for (int i=0;i<5;i++) writeRows(d.resolve("extra/t"+i+".txt"), 205);
        rejected(d, out, "more than 1024 rows accepted");

        Path columns=Files.createTempDirectory("microx-data-columns");
        Files.write(columns.resolve("items.txt"), "1|k|d|m|extra\n".getBytes(StandardCharsets.UTF_8));
        rejected(columns, columns.resolve("x.dat"), "fifth column accepted");
        Files.delete(columns.resolve("items.txt"));
        Path a=columns.resolve("a/same.txt"), b=columns.resolve("b/same.txt");
        Files.createDirectories(a.getParent()); Files.createDirectories(b.getParent());
        Files.write(a,"1|a|A\n".getBytes(StandardCharsets.UTF_8));
        Files.write(b,"2|b|B\n".getBytes(StandardCharsets.UTF_8));
        try { AssetConverter.readData(columns); throw new AssertionError("duplicate basename accepted"); }
        catch (IOException e) { ok(e.getMessage().contains(a.toString()) && e.getMessage().contains(b.toString()), "duplicate error omits paths"); }

        Map<String,List<AssetConverter.DataRow>> packed=new TreeMap<String,List<AssetConverter.DataRow>>();
        Path fake=Paths.get("unicode.txt");
        char[] huge = new char[65536]; Arrays.fill(huge, 'x');
        packed.put("items", Arrays.asList(new AssetConverter.DataRow(1,"nul\0é€😀","d",new String(huge),fake,1)));
        try { AssetConverter.validateRecordStoreLimits(packed); throw new AssertionError("oversized metadata accepted"); }
        catch (IOException e) { ok(e.getMessage().contains("modified UTF-8"), "wrong modified UTF error"); }
        packed.put("items", Arrays.asList(new AssetConverter.DataRow(1,"nul\0é€😀","d","",fake,1)));
        AssetConverter.validateRecordStoreLimits(packed);
    }
    private static void writeRows(Path file, int count) throws IOException {
        Files.createDirectories(file.getParent()); StringBuilder text=new StringBuilder();
        for(int i=1;i<=count;i++) text.append(i).append("|k").append(i).append("|d\n");
        Files.write(file,text.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static void rejected(Path d, Path out, String message) throws Exception {
        boolean failed = false;
        try {
            AssetConverter.writeGameplayData(d, out);
        } catch (IOException e) {
            failed = true;
        }
        ok(failed, message);
    }
    private static void copy(Path from, Path to) throws IOException {
        try (java.util.stream.Stream<Path> s = Files.walk(from)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                Path q = to.resolve(from.relativize(p).toString());
                if (Files.isDirectory(p))
                    Files.createDirectories(q);
                else
                    Files.copy(p, q, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    private static void ok(boolean v, String message) {
        if (!v)
            throw new AssertionError(message);
    }
}
