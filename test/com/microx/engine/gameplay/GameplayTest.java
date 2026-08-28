package com.microx.engine.gameplay;
import com.microx.engine.save.*;
import com.microx.engine.world.Player;
import com.microx.engine.combat.DamagePipeline;

public final class GameplayTest {
    public static void main(String[] args) {
        if (!ItemCatalog.loadDefault())
            throw new AssertionError("gameplay catalog unavailable");
        inventory();
        equipment();
        fiveArtifactSave();
        loot();
        deterministicStashesAndPlayerStorage();
        trade();
        quests();
        verticalSlice();
        tableDrivenScenario();
        tradeRepairAndCorpseSaveLoad();
        narrativeAndArena();
        System.out.println("GameplayTest OK");
    }
    private static void deterministicStashesAndPlayerStorage() {
        GameplayTables tables = new GameplayTables();
        ok(tables.load("/data/gameplay.dat"));
        Inventory a = new Inventory(24, 40), b = new Inventory(24, 40), c = new Inventory(24, 40);
        ok(StashLootSystem.generate(a, tables, 77, 31001, 1234, "cordon", 1));
        ok(StashLootSystem.generate(b, tables, 77, 31001, 1234, "cordon", 1));
        ok(StashLootSystem.generate(c, tables, 77, 31002, 1234, "cordon", 1));
        for (int i = 1; i <= ItemCatalog.maxId(); i++) eq(a.count(i), b.count(i));
        GameplayState state = new GameplayState();
        Inventory chest = new Inventory(24, 40), restored = new Inventory(24, 40);
        ok(chest.add(GameIds.ITEM_PISTOL, 1, 37));
        ok(state.containers.capture(31001, chest));
        chest.clear(); ok(chest.add(GameIds.ITEM_MEDKIT, 2, 100));
        ok(state.containers.capture(31002, chest));
        ok(state.containers.restore(31001, restored));
        eq(37, restored.conditionOf(GameIds.ITEM_PISTOL));
        eq(0, restored.count(GameIds.ITEM_MEDKIT));
    }
    private static void fiveArtifactSave() {
        SaveData save = new SaveData();
        for (int i = 0; i < 5; i++)
            ok(save.gameplay.equipment.restore(
                    2, i, (i & 1) == 0 ? GameIds.ITEM_STONE : GameIds.ITEM_CRYSTAL));
        try {
            SaveData loaded = new SaveCodec().decode(new SaveCodec().encode(save));
            for (int i = 0; i < 5; i++)
                eq((i & 1) == 0 ? GameIds.ITEM_STONE : GameIds.ITEM_CRYSTAL,
                        loaded.gameplay.equipment.artifact(i));
        } catch (SaveException failure) {
            throw new AssertionError(failure.toString());
        }
    }
    private static void tradeRepairAndCorpseSaveLoad() {
        GameplayTables tables = new GameplayTables();
        ok(tables.load("/data/gameplay.dat"));
        GameplayState g = new GameplayState();
        g.inventory.setMoney(4000);
        ok(tables.traderProfile(GameIds.NPC_SIDOROVICH, g.trader));
        eq(GameIds.FACTION_LONER, tables.npcFaction(GameIds.NPC_SIDOROVICH));
        ok(TradeSystem.buy(g.inventory, g.trader, GameIds.ITEM_MEDKIT, 1,
                tables.npcFaction(GameIds.NPC_SIDOROVICH), g.reputation));
        ok(TradeSystem.sell(g.inventory, g.trader, GameIds.ITEM_MEDKIT, 1,
                tables.npcFaction(GameIds.NPC_SIDOROVICH), g.reputation));
        g.inventory.add(GameIds.ITEM_PISTOL, 1, 20);
        int quoted = TradeSystem.repairPrice(g.inventory, GameIds.ITEM_PISTOL, 100);
        ok(quoted > 0);
        ok(TradeSystem.repair(g.inventory, GameIds.ITEM_PISTOL, 100));
        eq(100, g.inventory.conditionOf(GameIds.ITEM_PISTOL));

        Inventory corpse = new Inventory(24, 40);
        ok(LootSystem.generateCorpse(corpse, 20001, 77, 1234, GameIds.FACTION_DUTY, 2));
        int taken = corpse.idAt(0), original = corpse.count(taken);
        ok(corpse.moveTo(g.inventory, taken, 1));
        ok(g.containers.capture(20001, corpse));
        SaveData save = new SaveData();
        save.gameplay.copyPersistentFrom(g);
        try {
            SaveData loaded = new SaveCodec().decode(new SaveCodec().encode(save));
            Inventory searchedAgain = new Inventory(24, 40);
            ok(loaded.gameplay.containers.restore(20001, searchedAgain));
            eq(original - 1, searchedAgain.count(taken));
        } catch (SaveException failure) {
            throw new AssertionError(failure.toString());
        }
    }
    private static void verticalSlice() {
        GameplayState g = new GameplayState();
        g.inventory.setMoney(1000);
        ok(g.acceptFindStash());
        no(g.foundStash(999));
        ok(g.foundStash(GameIds.CONTAINER_FIND_STASH));
        no(g.foundStash(GameIds.CONTAINER_FIND_STASH));
        ok(g.rewardFindStash());
        eq(2, g.quests.state(GameIds.QUEST_FIND_STASH));
        g.trader.add(GameIds.ITEM_BANDAGE, 1, 100);
        g.trader.setMoney(1000);
        ok(TradeSystem.buy(g.inventory, g.trader, GameIds.ITEM_BANDAGE, 1, GameIds.FACTION_LONER,
                g.reputation));
        ok(TradeSystem.sell(g.inventory, g.trader, GameIds.ITEM_MEDKIT, 1, GameIds.FACTION_LONER,
                g.reputation));
        g.inventory.add(GameIds.ITEM_PISTOL, 1, 40);
        ok(TradeSystem.repair(g.inventory, GameIds.ITEM_PISTOL, 100));
    }
    private static void tableDrivenScenario() {
        GameplayTables tables = new GameplayTables();
        ok(tables.load("/data/gameplay.dat"));
        eq(GameIds.DIALOG_INTRO, tables.npcDialog(GameIds.NPC_SIDOROVICH));
        eq(GameIds.DIALOG_STASH_REPORT, tables.dialogNext(GameIds.DIALOG_INTRO));
        ok(tables.dialogText(GameIds.DIALOG_INTRO).length() > 0);
        ok(tables.questText(GameIds.QUEST_FIND_STASH).length() > 0);
        eq(GameIds.NPC_SIDOROVICH, tables.questRef(GameIds.QUEST_FIND_STASH));

        GameplayState g = new GameplayState();
        short[] dialogs = new short[8];
        int count = DialogueSystem.available(tables, g, GameIds.NPC_SIDOROVICH, dialogs);
        eq(2, count);
        eq(GameIds.DIALOG_INTRO, dialogs[0] & 65535);
        eq(DialogueSystem.QUEST,
                DialogueSystem.select(tables, g, GameIds.NPC_SIDOROVICH, dialogs[0] & 65535));
        eq(1, g.quests.state(GameIds.QUEST_FIND_STASH));
        ok(g.foundStash(GameIds.CONTAINER_FIND_STASH));

        count = DialogueSystem.available(tables, g, GameIds.NPC_SIDOROVICH, dialogs);
        eq(2, count);
        eq(GameIds.DIALOG_STASH_REPORT, dialogs[0] & 65535);
        eq(DialogueSystem.QUEST,
                DialogueSystem.select(tables, g, GameIds.NPC_SIDOROVICH, dialogs[0] & 65535));
        eq(QuestState.COMPLETE, g.quests.state(GameIds.QUEST_FIND_STASH));
        eq(500, g.inventory.money());
        eq(1, g.inventory.count(GameIds.ITEM_MEDKIT));
        eq(10, g.reputation.get(GameIds.FACTION_LONER));
    }
    private static void narrativeAndArena() {
        GameplayTables tables = new GameplayTables();
        ok(tables.load("/data/gameplay.dat"));
        GameplayState g = new GameplayState();
        StorySystem story = new StorySystem(tables, g.quests);
        ok(story.start());
        eq(1, story.node());
        eq(10003, g.quests.objective());
        CutsceneSystem scene = new CutsceneSystem(tables);
        ok(scene.start(story.scene()));
        ok(scene.text().length() > 0);
        ok(scene.next());
        no(scene.next());
        ok(story.choose(false));
        eq(2, story.node());
        ok(story.choose(true));
        eq(4, story.node());
        ok(story.choose(false));
        eq(1, story.ending());
        GameplayState survivor = new GameplayState();
        StorySystem survivorStory = new StorySystem(tables, survivor.quests);
        ok(survivorStory.start());
        ok(survivorStory.choose(false));
        ok(survivorStory.choose(false));
        eq(3, survivorStory.node());
        ok(survivorStory.choose(false));
        eq(5, survivorStory.node());
        ok(survivorStory.choose(false));
        eq(2, survivorStory.ending());
        ok(survivorStory.enterFreeplay(3));
        ok(survivor.quests.freeplay());

        CyclicQuestSystem cyclic = new CyclicQuestSystem(tables, survivor.quests);
        eq(1, cyclic.current());
        ok(cyclic.issue(10));
        no(cyclic.issue(10));
        ok(cyclic.complete(10));
        eq(2, cyclic.current());
        eq(3, cyclic.cooldownRemaining(10));
        no(cyclic.issue(12));
        ok(cyclic.issue(13));

        GameplayState fighter = new GameplayState();
        fighter.inventory.setMoney(1000);
        fighter.inventory.add(GameIds.ITEM_MEDKIT, 1, 100);
        ArenaSystem arena = new ArenaSystem(tables);
        ok(arena.enter(1, fighter, "garbage", 20));
        eq(500, fighter.inventory.money());
        no(arena.clearWave());
        no(arena.clearWave());
        ok(arena.clearWave());
        ok(arena.leave(fighter, true));
        eq(1700, fighter.inventory.money());
        eq(1, fighter.inventory.count(GameIds.ITEM_MEDKIT));
        eq(2, fighter.inventory.count(GameIds.ITEM_BANDAGE));
        eq(20, arena.returnSpawn());

        GameplayState quitter = new GameplayState();
        quitter.inventory.setMoney(1000);
        quitter.inventory.add(GameIds.ITEM_MEDKIT, 1, 100);
        ok(arena.enter(1, quitter, "cordon", 10));
        ok(arena.leave(quitter, false));
        eq(500, quitter.inventory.money());
        eq(1, quitter.inventory.count(GameIds.ITEM_MEDKIT));
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
        Player s = new Player();
        s.health = 20;
        e.apply(s);
        eq(30, s.physicalProtection);
        s.health = 100;
        int before = s.health;
        DamagePipeline.apply(s, DamagePipeline.PHYSICAL, 40);
        eq(before - 28, s.health);
        s.health = 20;
        ok(e.use(bag, GameIds.ITEM_MEDKIT, s));
        eq(80, s.health);
        ok(e.unequip(bag, 2, 0, s));
        eq(25, s.physicalProtection);
    }
    private static void loot() {
        Inventory a = new Inventory(8, 20), b = new Inventory(8, 20);
        ok(LootSystem.generateCorpse(a, 77, 123, 2, 1, 2));
        ok(LootSystem.generateCorpse(b, 77, 123, 2, 1, 2));
        for (int i = 1; i <= ItemCatalog.maxId(); i++) eq(a.count(i), b.count(i));
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
