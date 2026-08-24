package com.microx.engine.gameplay;

public final class GameplayTest {
    public static void main(String[] args) {
        inventory();
        equipment();
        loot();
        trade();
        quests();
        System.out.println("GameplayTest OK");
    }
    private static void inventory() {
        Inventory a = new Inventory(2, 2);
        ok(a.add(GameIds.ITEM_MEDKIT, 5, 100));
        ok(a.add(GameIds.ITEM_MEDKIT, 5, 100));
        no(a.add(GameIds.ITEM_MEDKIT, 1, 100));
        eq(10, a.count(GameIds.ITEM_MEDKIT));
        Inventory b = new Inventory(1, 1);
        no(a.moveTo(b, GameIds.ITEM_MEDKIT, 6));
        eq(10, a.count(GameIds.ITEM_MEDKIT));
        ok(a.moveTo(b, GameIds.ITEM_MEDKIT, 5));
        eq(5, b.count(GameIds.ITEM_MEDKIT));
    }
    private static void equipment() {
        Inventory bag = new Inventory(12, 30);
        bag.add(GameIds.ITEM_PISTOL, 1, 80);
        bag.add(GameIds.ITEM_RIFLE, 1, 80);
        bag.add(GameIds.ITEM_LEATHER_ARMOR, 1, 100);
        bag.add(GameIds.ITEM_STONE, 1, 100);
        bag.add(GameIds.ITEM_MEDKIT, 1, 100);
        Equipment e = new Equipment();
        ok(e.equip(bag, GameIds.ITEM_PISTOL, 0));
        ok(e.equip(bag, GameIds.ITEM_RIFLE, 1));
        ok(e.equip(bag, GameIds.ITEM_LEATHER_ARMOR, 0));
        ok(e.equip(bag, GameIds.ITEM_STONE, 0));
        PlayerStats s = new PlayerStats();
        s.health = 20;
        e.apply(s);
        eq(30, s.physicalProtection);
        ok(e.use(bag, GameIds.ITEM_MEDKIT, s));
        eq(80, s.health);
    }
    private static void loot() {
        Inventory a = new Inventory(8, 20), b = new Inventory(8, 20);
        ok(LootSystem.generateCorpse(a, 77, 123, 2, 1, 2));
        ok(LootSystem.generateCorpse(b, 77, 123, 2, 1, 2));
        for (int i = 1; i <= ItemCatalog.MAX_ID; i++) eq(a.count(i), b.count(i));
        no(LootSystem.generateCorpse(a, 77, 123, 2, 1, 2));
    }
    private static void trade() {
        Inventory player = new Inventory(8, 20), npc = new Inventory(8, 20);
        player.setMoney(1000);
        npc.add(GameIds.ITEM_MEDKIT, 2, 100);
        Reputation r = new Reputation(4);
        ok(TradeSystem.buy(player, npc, GameIds.ITEM_MEDKIT, 1, 1, r));
        eq(700, player.money());
        int money = player.money();
        no(TradeSystem.buy(player, npc, GameIds.ITEM_RIFLE, 1, 1, r));
        eq(money, player.money());
        player.add(GameIds.ITEM_PISTOL, 1, 50);
        ok(TradeSystem.repair(player, GameIds.ITEM_PISTOL, 100));
        eq(100, player.conditionOf(GameIds.ITEM_PISTOL));
    }
    private static void quests() {
        QuestState q = new QuestState(4, 1, 4);
        ok(q.setFlag(2, true));
        ok(q.transition(1, 0, 1, 0, 0, 2, -1, 0, 17));
        eq(17, q.objective());
        q.addCounter(0, 3);
        ok(q.dialogAllowed(1, 1, 2, 0, 3));
        ok(q.transition(1, 1, 2, 0, 0, -1, 0, 3, 0));
        eq(-1, q.objective());
    }
    private static void ok(boolean b) {
        if (!b)
            throw new AssertionError();
    }
    private static void no(boolean b) {
        ok(!b);
    }
    private static void eq(int a, int b) {
        if (a != b)
            throw new AssertionError(a + " != " + b);
    }
}
