package com.microx.engine.gameplay;

/** Cursor over static slideshow frames; all text and timing remain in gameplay.dat. */
public final class CutsceneSystem {
    private final GameplayTables tables;
    private int scene, order;
    public CutsceneSystem(GameplayTables data) {
        tables = data;
    }
    public boolean start(int id) {
        scene = id;
        order = 1;
        return row() >= 0;
    }
    public String text() {
        int r = row();
        return r < 0 ? null : tables.text(r);
    }
    public int duration() {
        return tables.numberAt(row(), "duration");
    }
    public boolean next() {
        order++;
        return row() >= 0;
    }
    public int scene() {
        return scene;
    }
    private int row() {
        int n = tables.tableSize("slides");
        for (int i = 0; i < n; i++) {
            int r = tables.tableRow("slides", i);
            if (tables.numberAt(r, "scene") == scene && tables.numberAt(r, "order") == order)
                return r;
        }
        return -1;
    }
}
