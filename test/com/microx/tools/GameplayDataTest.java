package com.microx.tools;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
public final class GameplayDataTest {
    public static void main(String[] args) throws Exception {
        Path d = Files.createTempDirectory("microx-data");
        Path q = d.resolve("quests.txt");
        Files.write(q, ("1|a|A|requires=2\n2|b|B|requires=1\n").getBytes(StandardCharsets.UTF_8));
        boolean failed = false;
        try {
            AssetConverter.writeGameplayData(d, d.resolve("out.dat"));
        } catch (java.io.IOException expected) {
            failed = true;
        }
        if (!failed)
            throw new AssertionError("bad quest references accepted");
        Files.write(q, "1|ключ|Опис\n".getBytes(StandardCharsets.UTF_8));
        AssetConverter.writeGameplayData(d, d.resolve("out.dat"));
        if (!Files.exists(d.resolve("out.dat"))) throw new AssertionError("UTF-8 .txt not packed");
        Files.write(d.resolve("old.data"), "1|old|Old\n".getBytes(StandardCharsets.UTF_8));
        failed = false;
        try { AssetConverter.writeGameplayData(d, d.resolve("out.dat")); }
        catch (java.io.IOException expected) {
            failed = expected.getMessage().contains("legacy .data");
        }
        if (!failed) throw new AssertionError("legacy .data not clearly rejected");
        Files.delete(d.resolve("old.data"));
        Files.write(q, "1|a|A|ref=missing:7\n".getBytes(StandardCharsets.UTF_8));
        failed = false;
        try { AssetConverter.writeGameplayData(d, d.resolve("out.dat")); }
        catch (java.io.IOException expected) { failed = expected.getMessage().contains("unknown reference"); }
        if (!failed) throw new AssertionError("invalid reference accepted");
        System.out.println("GameplayDataTest OK");
    }
}
