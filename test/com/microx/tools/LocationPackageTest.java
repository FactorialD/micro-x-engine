package com.microx.tools;

import com.microx.engine.world.EntityPool;
import com.microx.engine.world.LevelLoader;
import java.io.FileInputStream;
import java.nio.file.*;
import java.util.*;

/** Whole-world static contract for authored MXL2 location packages. */
public final class LocationPackageTest {
    public static void main(String[] args) throws Exception {
        Path generated = Paths.get(args[1]);
        String[] names = {"cordon", "garbage", "agroprom", "depot", "bar", "wild_territory",
                "yantar", "laboratory", "army_warehouses", "radar", "pripyat", "cnpp", "arena"};
        Map<String, LevelLoader> levels = new LinkedHashMap<String, LevelLoader>();
        Set<Integer> entityIds = new HashSet<Integer>(), portalIds = new HashSet<Integer>();
        for (String name : names) levels.put(name, load(generated, name));
        for (Map.Entry<String, LevelLoader> entry : levels.entrySet()) {
            String name = entry.getKey();
            LevelLoader level = entry.getValue();
            int humans = 0, player = 0, random = 0;
            for (int i = 0; i < level.entities.capacity(); i++)
                if (level.entities.active[i]) {
                    ok(entityIds.add(Integer.valueOf(level.entities.stableId[i])),
                            "globally unique entity " + level.entities.stableId[i]);
                    if (level.entities.type[i] == EntityPool.HUMAN)
                        humans++;
                    if (level.entities.type[i] == EntityPool.CONTAINER
                            && level.entities.aux[i] == EntityPool.PLAYER_STASH)
                        player++;
                    if (level.entities.type[i] == EntityPool.CONTAINER
                            && level.entities.aux[i] == EntityPool.RANDOM_STASH)
                        random++;
                }
            ok(humans >= 1, name + " interactive human");
            ok(player == 1, name + " exactly one player chest");
            ok(random >= 1, name + " random stash");
            for (int p = 0; p < level.transitionId.length; p++) {
                ok(portalIds.add(Integer.valueOf(level.world.portalId(p))), "unique portal id");
                String target = level.transitionLocation[p];
                LevelLoader destination = levels.get(target);
                ok(destination != null, name + " target directory " + target);
                ok(destination.selectSpawn(level.transitionSpawn[p]), "target spawn exists");
                boolean reciprocal = false;
                for (int q = 0; q < destination.transitionId.length; q++)
                    if (name.equals(destination.transitionLocation[q]))
                        reciprocal = true;
                ok(reciprocal, name + " <-> " + target);
                ok(level.world.portalTransition(p) == p, name + " portal transition binding");
            }
        }
        Set<String> reached = new HashSet<String>();
        reached.add("cordon");
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String from : new ArrayList<String>(reached))
                for (String to : levels.get(from).transitionLocation)
                    if (reached.add(to))
                        changed = true;
        }
        ok(reached.size() == levels.size(), "all locations reachable from cordon");
        System.out.println("LocationPackageTest: OK (" + levels.size() + " locations)");
    }
    private static LevelLoader load(Path generated, String name) throws Exception {
        Path directory = generated.resolve("levels").resolve(name);
        ok(Files.size(directory.resolve("level.txt")) > 0, name + " level.txt");
        ok(Files.size(directory.resolve("geometry.mesh")) > 0, name + " geometry.mesh");
        LevelLoader level = new LevelLoader();
        ok(level.load(new FileInputStream(directory.resolve("level.txt").toFile())), name);
        return level;
    }
    private static void ok(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }
}
