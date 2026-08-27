package com.microx.tools;

import com.microx.engine.world.LevelLoader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Static integration contract for the two mandatory authored locations. */
public final class LocationPackageTest {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]), generated = Paths.get(args[1]);
        LevelLoader cordon = load(generated, "cordon", false),
                    garbage = load(generated, "garbage", false);
        transition(cordon, 1000, "garbage", 20, garbage);
        transition(garbage, 2000, "cordon", 10, cordon);
        ok(cordon.world.portalTransition(0) == cordon.findTransition(1000),
                "Cordon portal has transition metadata");
        ok(garbage.world.portalTransition(0) == garbage.findTransition(2000),
                "Garbage portal has transition metadata");
        ok(cordon.entities.capacity() >= 12 && garbage.entities.capacity() >= 12,
                "location entity capacity");
        entity(cordon, 10001, 1, 2, 11, 2);
        entity(cordon, 10002, 1, 1, 12, 1);
        entity(cordon, 10003, 7, 0, 17, 4);
        entity(garbage, 20001, 2, 3, 21, 1);
        entity(garbage, 20002, 4, 0, 31, 0);
        entity(garbage, 20003, 3, 0, 41, 6);
        String engine =
                new String(Files.readAllBytes(root.resolve("src/com/microx/engine/Engine.java")),
                        StandardCharsets.UTF_8);
        ok(engine.contains(
                   "loadLocation(level.transitionLocation[t], level.transitionSpawn[t], true)"),
                "cross-location transitions request autosave");
        System.out.println("LocationPackageTest: OK");
    }
    private static LevelLoader load(Path generated, String name, boolean hasMedia)
            throws Exception {
        Path directory = generated.resolve("levels").resolve(name);
        for (String resource : new String[] {"level.lvl", "geometry.mesh"})
            ok(Files.size(directory.resolve(resource)) > 0, name + " runtime " + resource);
        for (String resource : new String[] {"textures.tex", "music.mid"})
            ok(Files.exists(directory.resolve(resource)) == hasMedia,
                    name + " optional runtime " + resource);
        LevelLoader level = new LevelLoader();
        ok(level.load(new FileInputStream(directory.resolve("level.lvl").toFile())),
                name + " level");
        return level;
    }
    private static void transition(
            LevelLoader from, int id, String target, int spawn, LevelLoader destination) {
        int index = from.findTransition(id);
        ok(index >= 0, "transition " + id);
        ok(target.equals(from.transitionLocation[index]), "transition target " + target);
        ok(from.transitionSpawn[index] == spawn, "transition target spawn " + spawn);
        ok(destination.selectSpawn(spawn), "destination defines spawn " + spawn);
    }
    private static void entity(
            LevelLoader level, int stable, int type, int faction, int sprite, int aux) {
        int i = level.entities.findStable(stable);
        ok(i >= 0 && level.entities.type[i] == type && level.entities.faction[i] == faction
                        && level.entities.spriteId[i] == sprite && level.entities.aux[i] == aux,
                "entity metadata " + stable);
    }
    private static void ok(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }
}
