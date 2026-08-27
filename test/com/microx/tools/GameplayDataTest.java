package com.microx.tools;
import com.microx.engine.gameplay.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
        System.out.println("GameplayDataTest OK");
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
