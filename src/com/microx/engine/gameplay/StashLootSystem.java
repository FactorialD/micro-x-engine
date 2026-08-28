package com.microx.engine.gameplay;

/** Deterministic, order-independent random-stash generation from gameplay.dat. */
public final class StashLootSystem {
    private StashLootSystem() {}
    public static boolean generate(Inventory out, GameplayTables tables, int seed, int stableId,
            int locationId, String location, int tier) {
        out.clear();
        int n = tables.tableSize("stash_loot");
        for (int i = 0; i < n; i++) {
            int row = tables.tableRow("stash_loot", i);
            String limited = tables.fieldAt(row, "location");
            if (tables.numberAt(row, "tier") != tier
                    || (limited != null && limited.length() > 0 && !limited.equals(location)))
                continue;
            int item = tables.numberAt(row, "item"), min = tables.numberAt(row, "min"),
                max = tables.numberAt(row, "max"), weight = tables.numberAt(row, "weight");
            int random = mix(seed ^ stableId * 0x45d9f3b ^ locationId * 0x27d4eb2d
                    ^ tables.id(row) * 0x165667b1);
            if ((random & 0x7fffffff) % 100 < weight) {
                int amount = min + ((random >>> 8) & 0x7fffffff) % (max - min + 1);
                if (!out.add(item, amount, 100)) return false;
            }
        }
        return true;
    }
    private static int mix(int x) {
        x ^= x >>> 16; x *= 0x7feb352d; x ^= x >>> 15; x *= 0x846ca68b; return x ^ (x >>> 16);
    }
}
