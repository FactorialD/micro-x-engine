package com.microx.engine.combat;
import com.microx.engine.world.Player;
import com.microx.engine.gameplay.ItemCatalog;

/** Single mitigation and status-effect path for every damage source. */
public final class DamagePipeline {
    public static final int PHYSICAL = 0, ANOMALY = 1, RADIATION = 2, MELEE = 3;
    private DamagePipeline() {}
    public static int apply(Player p, int type, int amount) {
        if (amount <= 0)
            return 0;
        int mitigation;
        if (type == RADIATION) {
            mitigation = ItemCatalog.radiation(p.suit) + ItemCatalog.radiation(p.artifact);
            int dose = amount * (100 - clamp(mitigation)) / 100;
            p.radiation += dose;
            return dose;
        }
        if (type == ANOMALY)
            mitigation = ItemCatalog.anomaly(p.suit) + ItemCatalog.anomaly(p.artifact);
        else
            mitigation = ItemCatalog.physical(p.suit) + ItemCatalog.physical(p.artifact) + p.armor;
        int dealt = amount * (100 - clamp(mitigation)) / 100;
        if (dealt == 0 && amount > 0)
            dealt = 1;
        p.health -= dealt;
        if (p.health < 0)
            p.health = 0;
        if ((type == PHYSICAL || type == MELEE) && dealt >= 8)
            p.bleeding += dealt / 4;
        return dealt;
    }
    private static int clamp(int value) {
        return value < 0 ? 0 : value > 90 ? 90 : value;
    }
}
