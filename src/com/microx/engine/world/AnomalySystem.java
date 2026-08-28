package com.microx.engine.world;
import com.microx.engine.combat.DamagePipeline;
import com.microx.engine.gameplay.ItemCatalog;
/** Area triggers and deterministic artifact production. */
public final class AnomalySystem {
    public static final int DAMAGE = 0, SLOW = 1, PSI = 2, RADIATION = 3;
    private final int seed;
    private int entries;
    public AnomalySystem(int value) {
        seed = value;
    }
    public void enterLocation(EntityPool p) {
        entries++;
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i] && p.type[i] == EntityPool.ANOMALY
                    && mix(seed + entries * 31 + i) % 4 == 0) {
                int n = p.spawn(EntityPool.ITEM, p.x[i] + ((mix(seed + i) & 255) - 128) * 128,
                        p.y[i], p.z[i] + ((mix(seed + entries) & 255) - 128) * 128, 1);
                if (n >= 0) {
                    int item = artifact(mix(seed ^ entries ^ i));
                    if (item == 0) {
                        p.remove(n);
                        continue;
                    }
                    p.aux[n] = item;
                    p.spriteId[n] = 100 + item;
                    p.roomId[n] = p.roomId[i];
                }
            }
    }
    public void update(EntityPool p, Player player, int dt) {
        for (int i = 0; i < p.capacity(); i++)
            if (p.active[i] && p.type[i] == EntityPool.ANOMALY) {
                if (p.timer[i] > 0)
                    p.timer[i] -= dt;
                long dx = player.x - p.x[i], dz = player.z - p.z[i];
                if (p.timer[i] <= 0 && dx * dx + dz * dz < (long) p.radius[i] * p.radius[i]) {
                    if (p.aux[i] == DAMAGE)
                        DamagePipeline.apply(player, DamagePipeline.ANOMALY, 10);
                    else if (p.aux[i] == SLOW)
                        player.slow(2000);
                    else if (p.aux[i] == PSI)
                        player.stamina -= 10;
                    else if (p.aux[i] == RADIATION)
                        DamagePipeline.apply(player, DamagePipeline.RADIATION, 10);
                    p.timer[i] = 2000;
                }
            }
    }
    /** Select only entries declared as artifacts in the installed item table. */
    private static int artifact(int random) {
        int count = 0;
        for (int id = 1; id <= ItemCatalog.maxId(); id++)
            if (ItemCatalog.valid(id) && ItemCatalog.type(id) == ItemCatalog.TYPE_ARTIFACT)
                count++;
        if (count == 0)
            return 0;
        int selected = random % count;
        for (int id = 1; id <= ItemCatalog.maxId(); id++)
            if (ItemCatalog.valid(id) && ItemCatalog.type(id) == ItemCatalog.TYPE_ARTIFACT
                    && selected-- == 0)
                return id;
        return 0;
    }
    private static int mix(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        return x & 0x7fffffff;
    }
}
