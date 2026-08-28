package com.microx.engine.gameplay;

/** Small mutable relationship table shared by perception, combat and reputation. */
public final class FactionRelations {
    public static final int HOSTILE = -1, NEUTRAL = 0, FRIENDLY = 1, HOSTILITY_THRESHOLD = -250;
    private final byte[][] relation;
    public FactionRelations(int factions) {
        relation = new byte[factions + 1][factions + 1];
        for (int i = 1; i < relation.length; i++) relation[i][i] = FRIENDLY;
    }
    public void set(int from, int to, int value) {
        if (valid(from) && valid(to))
            relation[from][to] = (byte) value;
    }
    public int get(int from, int to) {
        return valid(from) && valid(to) ? relation[from][to] : NEUTRAL;
    }
    public boolean hostile(int from, int to) {
        return get(from, to) == HOSTILE;
    }
    public void neutralKilled(int faction, int playerFaction, Reputation reputation) {
        if (!valid(faction) || hostile(faction, playerFaction))
            return;
        reputation.add(faction, -100);
        if (reputation.get(faction) <= HOSTILITY_THRESHOLD) {
            set(faction, playerFaction, HOSTILE);
            set(playerFaction, faction, HOSTILE);
        }
    }
    private boolean valid(int faction) {
        return faction > 0 && faction < relation.length;
    }
}
