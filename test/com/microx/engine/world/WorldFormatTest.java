package com.microx.engine.world;
import com.microx.engine.math.Fixed;
import com.microx.tools.AssetConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Dependency-free desktop regression tests for converter and runtime world modules. */
public final class WorldFormatTest {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("mxl2-test");
        Path source = dir.resolve("level.txt");
        Files.write(source, level().getBytes(StandardCharsets.UTF_8));
        AssetConverter.validateAndCopyLevel(source, dir.resolve("copy/level.txt"));
        byte[] valid = Files.readAllBytes(source);
        LevelLoader loader = new LevelLoader();
        ok(loader.load(new ByteArrayInputStream(valid)), "valid text");
        ok(loader.world.portalReverse(0) == 1, "reverse portal");
        ok(loader.findTransition(100) == 0, "transition id");
        ok(EntityPool.PLAYER_STASH != EntityPool.RANDOM_STASH
                        && EntityPool.RANDOM_STASH != EntityPool.FIXED_CONTAINER,
                "container subtypes are unambiguous");
        wallSlideAndStep(loader.collision);
        ok(loader.world.crossedPortal(Fixed.fromInt(4), Fixed.fromInt(1), 0, Fixed.fromInt(5),
                   Fixed.fromInt(1),
                   0) == 0,
                "room crossing");
        byte[] truncated =
                level().substring(0, level().lastIndexOf(" 3\n")).getBytes(StandardCharsets.UTF_8);
        PortalWorld published = loader.world;
        ok(!loader.load(new ByteArrayInputStream(truncated)), "truncated rejected");
        ok(loader.world == published, "failed load is atomic");
        byte[] badHeader = "MXL3\ncounts 1 1 1 0 0 1 0 0 1\n".getBytes(StandardCharsets.UTF_8);
        ok(!new LevelLoader().load(new ByteArrayInputStream(badHeader)), "header rejected");
        Files.write(source,
                level().replace(
                               "portal 11 1 0 4 6 1 3 -1 1 0 -1", "portal 11 9 0 4 6 1 3 -1 1 0 -1")
                        .getBytes(StandardCharsets.UTF_8));
        boolean rejected = false;
        try {
            AssetConverter.validateAndCopyLevel(source, dir.resolve("copy/level.txt"));
        } catch (IOException expected) {
            rejected = true;
        }
        ok(rejected, "bad room reference rejected");
        System.out.println("WorldFormatTest: OK");
    }
    private static void wallSlideAndStep(Collision c) {
        int one = Fixed.ONE;
        c.sweep(0, 0, 0, Fixed.fromInt(6), Fixed.fromInt(2), one / 4, Fixed.fromInt(2), one / 2);
        ok(c.resultX < Fixed.fromInt(5), "wall blocks sweep");
        ok(c.resultZ > 0, "wall slide retains tangent motion");
        c.sweep(Fixed.fromInt(3), 0, 0, Fixed.fromInt(2), 0, one / 4, Fixed.fromInt(2), one / 2);
        ok(c.resultX < Fixed.fromInt(4), "high step rejected");
        ok(c.ceilingHeight(0, 0) == Fixed.fromInt(4), "ceiling query");
        ok(!c.hasClearance(0, Fixed.fromInt(3), 0, Fixed.fromInt(2)), "low ceiling clearance");
    }
    private static void ok(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }
    private static String level() {
        return "MXL2\ncounts 2 3 2 7 2 2 1 1 8\nroom -5 5 -5 5\nroom 5 15 -5 5\nfloor 0 -5 4 -5 5 0\nfloor 0 4 5 -5 5 1\nfloor 1 5 15 -5 5 1\nceiling 0 -5 5 -5 5 4\nceiling 1 5 15 -5 5 4\nedge 0 -5 -5 5 -5 0 4\nedge 0 -5 5 5 5 0 4\nedge 0 -5 -5 -5 5 0 4\nedge 0 5 -5 5 -1 0 4\nedge 0 5 1 5 5 0 4\nedge 1 5 -5 15 -5 0 4\nedge 1 5 5 15 5 0 4\nportal 10 0 1 4 6 1 3 -1 1 1 0\nportal 11 1 0 4 6 1 3 -1 1 0 -1\nspawn 0 0 0 0 0 0\nspawn 1 1 7 1 0 180\ntransition 100 1 test\nentity 77 1 10 1 0 100 2 11 3\n";
    }
}
