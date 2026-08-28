package com.microx.engine;
import com.microx.engine.assets.*;
import com.microx.engine.audio.*;
import com.microx.engine.combat.*;
import com.microx.engine.render.*;
import com.microx.engine.gameplay.*;
import com.microx.engine.save.*;
import com.microx.engine.ui.*;
import com.microx.engine.world.*;
public final class Engine implements Runnable {
    public static final int STOPPED = 0, RUNNING = 1, PAUSED = 2, SHUTDOWN = 3;
    private static final int STEP = 20, MAX_DELTA = 200, MAX_STEPS = 5;
    private volatile int state = STOPPED;
    private Thread thread;
    private GameCanvas3D canvas;
    public final Input input = new Input();
    public final Player player = new Player();
    public final Telemetry stats = new Telemetry();
    public final Hud hud = new Hud();
    public final SoftwareRenderer renderer = new SoftwareRenderer();
    public SaveData persistent = new SaveData();
    public final GameplayState gameplay = new GameplayState();
    private final AssetManager assets = new AssetManager();
    private final AudioManager audio = new AudioManager();
    private final HitScan hit = new HitScan();
    private final InteractionSystem interaction = new InteractionSystem();
    private final WorldSystems systems = new WorldSystems(0x4d58534d);
    private GameplayTables tables;
    private StorySystem story;
    private CutsceneSystem cutscene;
    private CyclicQuestSystem cyclic;
    private ArenaSystem arena;
    private SaveStore saves;
    private SettingsStore settings;
    private String location = "cordon";
    private int actorFaction;
    private boolean repairing;
    private String operationResult;
    private int containerSubtype;
    public LevelLoader level;
    public void attach(GameCanvas3D c) {
        canvas = c;
        renderer.configure(c.getWidth(), c.getHeight());
        renderer.setAssets(assets);
        try {
            saves = new SaveStore(new RmsBackend("microx-saves"));
            settings = new SettingsStore(new RmsBackend("microx-settings"));
            settings.load(c.settings);
            audio.setVolume(c.settings.volume);
            player.setTurnSensitivity(c.settings.sensitivity);
        } catch (Exception ignored) {
            saves = null;
            settings = null;
        }
    }
    public synchronized boolean start() {
        if (state == SHUTDOWN)
            return false;
        if (thread != null) {
            resume();
            return true;
        }
        tables = new GameplayTables();
        if (!tables.load(GameplayTables.DEFAULT_RESOURCE) || !ItemCatalog.install(tables))
            return false;
        tables.installFactionRelations(systems.relations());
        story = new StorySystem(tables, gameplay.quests);
        cutscene = new CutsceneSystem(tables);
        cyclic = new CyclicQuestSystem(tables, gameplay.quests);
        arena = new ArenaSystem(tables);
        persistent = new SaveData();
        persistent.seed = 0x4d58534d;
        if (!loadLocation("cordon", 0, false))
            return false;
        renderer.load();
        state = RUNNING;
        thread = new Thread(this);
        thread.start();
        return true;
    }
    public synchronized boolean loadGame() {
        if (saves == null)
            return false;
        try {
            SaveData loaded = saves.load(0);
            if (!loadLocation(loaded.location, loaded.spawn, false))
                return false;
            persistent = loaded;
            gameplay.copyPersistentFrom(loaded.gameplay);
            restorePlayer(loaded);
            applyEntityDeltas(loaded);
            return true;
        } catch (SaveException invalid) {
            return false;
        }
    }
    public synchronized boolean saveGame() {
        if (saves == null || level == null)
            return false;
        try {
            capture();
            saves.save(persistent);
            return true;
        } catch (SaveException failure) {
            return false;
        }
    }
    private void capture() {
        persistent.slot = 0;
        persistent.sequence++;
        persistent.savedAt = System.currentTimeMillis();
        persistent.location = location;
        persistent.x = player.x;
        persistent.y = player.y;
        persistent.z = player.z;
        persistent.yaw = player.yaw;
        persistent.pitch = player.pitch;
        persistent.health = player.health;
        persistent.armor = player.physicalProtection;
        persistent.stamina = player.stamina;
        persistent.bleeding = player.bleeding;
        persistent.radiation = player.radiation;
        persistent.weapon = player.combat.weapon;
        persistent.magazine = player.combat.magazine;
        for (int i = 0; i < persistent.reserveAmmo.length; i++)
            persistent.reserveAmmo[i] = player.reserveAmmo[i];
        persistent.spawn = level.nearestSpawn(player.x, player.z);
        persistent.gameplay.copyPersistentFrom(gameplay);
        persistent.clearEntities();
        for (int i = 0; i < level.entities.capacity(); i++)
            if (level.entities.stableId[i] > 0
                    && (!level.entities.active[i]
                            || level.entities.state[i] != EntityPool.STATE_IDLE
                            || (level.entities.flags[i] & EntityPool.FLAG_DEAD) != 0))
                persistent.addEntityDelta(level.entities.stableId[i],
                        (!level.entities.active[i] ? Integer.MIN_VALUE
                                                   : (level.entities.type[i] << 24)
                                                | (level.entities.state[i] << 16)
                                                | (level.entities.flags[i] & 65535)));
    }
    private void applyEntityDeltas(SaveData s) {
        for (int n = 0; n < s.entityCount; n++) {
            int i = level.entities.findStableAny(s.entityId[n]);
            if (i >= 0) {
                if (s.entityFlags[n] == Integer.MIN_VALUE) {
                    level.entities.remove(i);
                    continue;
                }
                level.entities.flags[i] = s.entityFlags[n] & 65535;
                level.entities.state[i] = (s.entityFlags[n] >>> 16) & 255;
                int type = (s.entityFlags[n] >>> 24) & 127;
                if (type != 0)
                    level.entities.restoreType(i, type);
            }
        }
    }
    private void restorePlayer(SaveData s) {
        player.x = s.x;
        player.y = s.y;
        player.z = s.z;
        player.yaw = s.yaw;
        player.pitch = s.pitch;
        player.health = s.health;
        player.stamina = s.stamina;
        player.bleeding = s.bleeding;
        player.radiation = s.radiation;
        player.combat.equip(s.weapon);
        player.combat.magazine = s.magazine;
        for (int i = 0; i < s.reserveAmmo.length; i++) player.reserveAmmo[i] = s.reserveAmmo[i];
        player.ammo = s.magazine;
        gameplay.equipment.apply(player);
        level.world.updateVisibility(player.x, player.z);
    }
    private boolean loadLocation(String name, int spawn, boolean autosave) {
        LevelLoader candidate = new LevelLoader();
        if (!candidate.load("/levels/" + name + "/level.txt"))
            return false;
        if (!candidate.selectSpawn(spawn)) {
            candidate.clear();
            return false;
        }
        LevelLoader oldLevel = level;
        String oldLocation = location;
        int oldX = player.x, oldY = player.y, oldZ = player.z, oldYaw = player.yaw;
        try {
            if (!assets.loadLocation(name, 0)) {
                candidate.clear();
                return false;
            }
        } catch (RuntimeException failure) {
            assets.loadLocation(oldLocation, 0);
            candidate.clear();
            return false;
        }
        audio.leaveLocation();
        location = name;
        level = candidate;
        player.reset(candidate.startX, candidate.startY, candidate.startZ);
        player.yaw = candidate.startYaw;
        candidate.world.updateVisibility(player.x, player.z);
        systems.enter(candidate.entities);
        audio.enterLocation(name);
        if (autosave && !saveGame()) {
            level = oldLevel;
            location = oldLocation;
            player.reset(oldX, oldY, oldZ);
            player.yaw = oldYaw;
            assets.loadLocation(oldLocation, 0);
            systems.enter(oldLevel.entities);
            audio.enterLocation(oldLocation);
            candidate.clear();
            return false;
        }
        if (oldLevel != null)
            oldLevel.clear();
        return true;
    }
    public void applySettings() {
        renderer.setResolutionMode(canvas.settings.resolution);
        audio.setVolume(canvas.settings.volume);
        player.setTurnSensitivity(canvas.settings.sensitivity);
        audio.enterLocation(location);
        if (settings != null)
            try {
                settings.save(canvas.settings);
            } catch (SaveException ignored) {
            }
    }
    public synchronized void pause() {
        if (state == RUNNING)
            state = PAUSED;
    }
    public synchronized void resume() {
        if (state == PAUSED) {
            state = RUNNING;
            notifyAll();
        }
    }
    public void togglePause() {
        if (state == PAUSED)
            resume();
        else
            pause();
    }
    public void shutdown() {
        Thread old;
        synchronized (this) {
            state = SHUTDOWN;
            notifyAll();
            old = thread;
        }
        if (old != null && old != Thread.currentThread()) {
            long deadline = System.currentTimeMillis() + 1000;
            while (old.isAlive()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                    break;
                try {
                    Thread.sleep(remaining < 20 ? remaining : 20);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
        renderer.release();
        assets.release();
        audio.release();
        if (saves != null)
            saves.close();
        if (settings != null)
            settings.close();
        thread = null;
    }
    public void run() {
        long previous = System.currentTimeMillis(), acc = 0;
        while (state != SHUTDOWN) {
            synchronized (this) {
                while (state == PAUSED) try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                if (state == SHUTDOWN)
                    break;
            }
            long now = System.currentTimeMillis(), delta = now - previous;
            previous = now;
            if (delta > MAX_DELTA)
                delta = MAX_DELTA;
            if (delta < 0)
                delta = 0;
            acc += delta;
            int steps = 0, totalUpdate = 0;
            while (acc >= STEP && steps < MAX_STEPS) {
                long u = System.currentTimeMillis();
                update(STEP, u);
                totalUpdate += (int) (System.currentTimeMillis() - u);
                acc -= STEP;
                steps++;
            }
            if (acc >= STEP) {
                int dropped = (int) (acc / STEP);
                stats.droppedFixedSteps += dropped;
                acc %= STEP;
            }
            long r = System.currentTimeMillis();
            canvas.renderFrame();
            int render = (int) (System.currentTimeMillis() - r);
            stats.timing(totalUpdate, render);
            stats.rendererBudget(assets.residentBytes()
                            + renderer.internalWidth() * renderer.internalHeight() * 6,
                    renderer.memoryBudget());
            stats.textureCount = assets.residentTextureCount();
            stats.meshSectionCount = assets.residentSectionCount();
            stats.frame(now);
            long sleep = STEP - (System.currentTimeMillis() - now);
            if (sleep > 1)
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ignored) {
                }
        }
        state = STOPPED;
    }
    private void update(int ms, long now) {
        input.update(now);
        if (canvas.gameplayBlocked()) {
            input.endUpdate();
            return;
        }
        player.aiming = (input.down() & Input.AIM) != 0;
        int oldX = player.x, oldY = player.y, oldZ = player.z;
        player.update(ms, input, level.collision);
        int portal = level.world.crossedPortal(oldX, oldY, oldZ, player.x, player.y, player.z);
        if (portal >= 0 && level.world.portalTransition(portal) >= 0) {
            int t = level.world.portalTransition(portal);
            loadLocation(level.transitionLocation[t], level.transitionSpawn[t], true);
        }
        stats.rooms = level.world.updateVisibility(player.x, player.z);
        systems.update(level.entities, player, level.collision, level.world, ms);
        if ((input.pressed() & Input.WEAPON) != 0)
            player.combat.equip(
                    player.combat.weapon == ItemTypes.PISTOL ? ItemTypes.RIFLE : ItemTypes.PISTOL);
        int selected = interaction.query(player, level.entities);
        hud.setInteraction(selected >= 0);
        if ((input.pressed() & Input.FIRE) != 0) {
            if (selected >= 0) {
                interact(selected);
            } else if (player.combat.state == CombatState.JAMMED)
                player.combat.clearJam();
            else if (player.combat.trigger())
                hit.fire(player, level.entities, level.collision,
                        com.microx.engine.math.Fixed.fromInt(
                                ItemCatalog.range(player.combat.weapon)),
                        ItemCatalog.spread(player.combat.weapon),
                        ItemCatalog.damage(player.combat.weapon)
                                + ItemCatalog.damageBonus(ItemCatalog.ammo(player.combat.weapon)),
                        systems.relations(), gameplay.reputation);
        }
        player.ammo = player.combat.magazine;
        stats.entities = level.entities.activeCount();
        stats.npcCount = level.entities.typeCount(EntityPool.HUMAN);
        stats.mutantCount = level.entities.typeCount(EntityPool.MUTANT);
        stats.itemCount = level.entities.typeCount(EntityPool.ITEM);
        stats.anomalyCount = level.entities.typeCount(EntityPool.ANOMALY);
        stats.corpseCount = level.entities.typeCount(EntityPool.CORPSE);
        stats.locationName = location;
        input.endUpdate();
    }
    private void interact(int i) {
        EntityPool e = level.entities;
        int type = e.type[i], stable = e.stableId[i], content = e.aux[i] & 65535;
        if (gameplay.quests.objective() == stable && completeObjective(stable))
            return;
        if (type == EntityPool.HUMAN) {
            gameplay.actorId = content == 0 ? GameIds.NPC_SIDOROVICH : content;
            actorFaction = e.faction[i];
            short[] rows = new short[32];
            int count = DialogueSystem.available(tables, gameplay, gameplay.actorId, rows);
            canvas.ui.fillList(rows, count);
            canvas.ui.show(UIStateMachine.DIALOGUE);
        } else if (type == EntityPool.CORPSE || type == EntityPool.CONTAINER) {
            containerSubtype = type == EntityPool.CONTAINER ? content : EntityPool.FIXED_CONTAINER;
            if (type == EntityPool.CONTAINER && stable == GameIds.CONTAINER_FIND_STASH)
                gameplay.foundStash(stable);
            gameplay.containerId = stable;
            gameplay.loot.clear();
            operationResult = null;
            if (gameplay.containers.restore(stable, gameplay.loot)) {
                // Complete persisted state is authoritative after the first logical opening.
            } else if (type == EntityPool.CORPSE) {
                LootSystem.generateCorpse(gameplay.loot, stable, persistent.seed, locationId(),
                        e.faction[i], corpseRank(e, i));
                applyContainerDeltas(stable, gameplay.loot);
                gameplay.containers.capture(stable, gameplay.loot);
            } else if (containerSubtype == EntityPool.RANDOM_STASH) {
                StashLootSystem.generate(gameplay.loot, tables, persistent.seed, stable,
                        locationId(), location, e.spriteId[i] >= 18 ? 2 : 1);
                applyContainerDeltas(stable, gameplay.loot); // migrates old quantity-only saves
                gameplay.containers.capture(stable, gameplay.loot);
            } else {
                // Player chests deliberately start empty and are independent by stable id.
                gameplay.containers.capture(stable, gameplay.loot);
            }
            fillLoot();
            canvas.ui.show(UIStateMachine.LOOT);
        } else if (type == EntityPool.ITEM) {
            int item = content == 0 ? GameIds.ITEM_STONE : content;
            if (gameplay.inventory.add(item, 1, 100)) {
                gameplay.containers.put(stable, item, -1);
                e.remove(i);
            }
        } else if (type == EntityPool.DOOR) {
            e.state[i] = e.state[i] == EntityPool.STATE_IDLE ? EntityPool.STATE_ALERT
                                                             : EntityPool.STATE_IDLE;
        }
    }
    private static void fillInventory(UIStateMachine ui, Inventory bag) {
        short[] rows = new short[bag.slots()];
        int n = 0;
        for (int i = 0; i < bag.slots(); i++)
            if (bag.idAt(i) != 0)
                rows[n++] = (short) bag.idAt(i);
        ui.fillList(rows, n);
    }
    public void openPda() {
        fillInventory(canvas.ui, gameplay.inventory);
        canvas.ui.show(UIStateMachine.INVENTORY);
    }
    private void fillTrade() {
        short[] rows = new short[64];
        byte[] sides = new byte[64];
        int n = 0;
        if (!repairing)
            for (int i = 0; i < gameplay.trader.slots() && n < rows.length; i++)
                if (gameplay.trader.idAt(i) != 0) {
                    rows[n] = (short) gameplay.trader.idAt(i);
                    sides[n++] = 0;
                }
        for (int i = 0; i < gameplay.inventory.slots() && n < rows.length; i++)
            if (gameplay.inventory.idAt(i) != 0) {
                rows[n] = (short) gameplay.inventory.idAt(i);
                sides[n++] = 1;
            }
        canvas.ui.fillTrade(rows, sides, n);
    }
    private void fillLoot() {
        short[] rows = new short[64];
        byte[] sides = new byte[64];
        int n = 0;
        for (int i = 0; i < gameplay.loot.slots() && n < rows.length; i++)
            if (gameplay.loot.idAt(i) != 0) {
                rows[n] = (short) gameplay.loot.idAt(i);
                sides[n++] = 0;
            }
        for (int i = 0; i < gameplay.inventory.slots() && n < rows.length; i++)
            if (gameplay.inventory.idAt(i) != 0) {
                rows[n] = (short) gameplay.inventory.idAt(i);
                sides[n++] = 1;
            }
        canvas.ui.fillTrade(rows, sides, n);
    }
    public void uiAction(int screen, int selection, boolean alternate) {
        short[] list = canvas.ui.listBuffer();
        int id = selection < canvas.ui.listSize() ? list[selection] & 65535 : 0;
        if (screen == UIStateMachine.CUTSCENE) {
            advanceCutscene();
        } else if (screen == UIStateMachine.ENDING) {
            enterFreeplay();
        } else if (screen == UIStateMachine.ARENA) {
            leaveArena(false);
        } else if (screen == UIStateMachine.CYCLIC_QUEST) {
            canvas.ui.show(UIStateMachine.GAMEPLAY);
        } else if (screen == UIStateMachine.FREEPLAY) {
            canvas.ui.show(UIStateMachine.GAMEPLAY);
        } else if (screen == UIStateMachine.DIALOGUE) {
            int action = DialogueSystem.select(tables, gameplay, gameplay.actorId, id);
            if (action == DialogueSystem.TRADE || action == DialogueSystem.REPAIR) {
                repairing = action == DialogueSystem.REPAIR;
                operationResult = null;
                if (!repairing && gameplay.traderActorId != gameplay.actorId) {
                    if (!tables.traderProfile(gameplay.actorId, gameplay.trader))
                        return;
                    gameplay.traderActorId = gameplay.actorId;
                }
                if (actorFaction == 0)
                    actorFaction = tables.npcFaction(gameplay.actorId);
                fillTrade();
                canvas.ui.show(UIStateMachine.TRADE);
            } else {
                short[] rows = new short[32];
                int count = DialogueSystem.available(tables, gameplay, gameplay.actorId, rows);
                canvas.ui.fillList(rows, count);
            }
        } else if (screen == UIStateMachine.TRADE && id != 0) {
            boolean success;
            if (repairing)
                success = TradeSystem.repair(gameplay.inventory, id, 100);
            else if (canvas.ui.sideAt(selection) == 1)
                success = TradeSystem.sell(gameplay.inventory, gameplay.trader, id, 1, actorFaction,
                        gameplay.reputation);
            else
                success = TradeSystem.buy(gameplay.inventory, gameplay.trader, id, 1, actorFaction,
                        gameplay.reputation);
            operationResult = success ? (repairing ? "REPAIRED" : "TRADE OK") : "FAILED";
            fillTrade();
        } else if (screen == UIStateMachine.LOOT && id != 0) {
            int direction = canvas.ui.sideAt(selection) == 1 ? 1 : -1;
            Inventory source = direction < 0 ? gameplay.loot : gameplay.inventory;
            Inventory target = direction < 0 ? gameplay.inventory : gameplay.loot;
            if (source.moveTo(target, id, 1)) {
                if (!gameplay.containers.capture(gameplay.containerId, gameplay.loot)) {
                    target.moveTo(source, id, 1);
                    operationResult = "STORAGE FULL";
                } else
                    operationResult = "TRANSFER OK";
                fillLoot();
            } else
                operationResult = "INVENTORY FULL";
        } else if (screen == UIStateMachine.INVENTORY && id != 0) {
            if (ItemCatalog.type(id) == ItemCatalog.TYPE_CONSUMABLE)
                gameplay.equipment.use(gameplay.inventory, id, player);
            else if (ItemCatalog.type(id) == ItemCatalog.TYPE_DETECTOR)
                player.detectorActive = !player.detectorActive;
            else if (ItemCatalog.type(id) == ItemCatalog.TYPE_BOLT
                    && gameplay.inventory.remove(id, 1))
                probeAnomalies();
            else
                gameplay.equipment.equip(gameplay.inventory, id, alternate ? 1 : 0, player);
        }
    }
    /** Runtime event: starts the main graph and presents its optional opening scene. */
    public boolean startStory() {
        if (story == null || !story.start())
            return false;
        showStoryScene();
        return true;
    }
    /** Runtime event emitted by interaction/travel/destruction objectives. */
    public boolean completeObjective(int marker) {
        if (story == null || gameplay.quests.objective() != marker || !story.choose(false))
            return false;
        showStoryScene();
        return true;
    }
    /** Runtime event for an explicit branch choice. */
    public boolean chooseStoryBranch(boolean alternate) {
        if (story == null || !story.choose(alternate))
            return false;
        showStoryScene();
        return true;
    }
    private void showStoryScene() {
        int scene = story.scene();
        if (scene != 0 && cutscene.start(scene))
            canvas.ui.show(story.ending() == 0 ? UIStateMachine.CUTSCENE : UIStateMachine.ENDING);
        else if (story.ending() != 0)
            canvas.ui.show(UIStateMachine.ENDING);
    }
    /** Runtime event: advances the currently presented slideshow frame. */
    public boolean advanceCutscene() {
        if (cutscene != null && cutscene.next())
            return true;
        canvas.ui.show(story != null && story.ending() != 0 ? UIStateMachine.ENDING
                                                            : UIStateMachine.GAMEPLAY);
        return false;
    }
    /** Runtime event that hands a completed ending back to the persistent world. */
    public boolean enterFreeplay() {
        if (story == null || !story.enterFreeplay(3))
            return false;
        int row = tables.find("endings", 3);
        String target = tables.fieldAt(row, "location");
        if (target != null && !loadLocation(target, tables.numberAt(row, "spawn"), true))
            return false;
        canvas.ui.show(UIStateMachine.FREEPLAY);
        return true;
    }
    /** Runtime event: issues a repeatable quest if its persisted cooldown permits it. */
    public boolean issueCyclicQuest(int cycle) {
        if (cyclic == null || !cyclic.issue(cycle))
            return false;
        canvas.ui.show(UIStateMachine.CYCLIC_QUEST);
        return true;
    }
    public boolean completeCyclicQuest(int cycle) {
        return cyclic != null && cyclic.complete(cycle);
    }
    /** Runtime event: atomically swaps the player's inventory for the configured arena kit. */
    public boolean enterArena(int id) {
        int row = tables == null ? -1 : tables.find("arena", id);
        int returnSpawn = level == null ? 0 : level.nearestSpawn(player.x, player.z);
        if (row < 0 || !arena.enter(id, gameplay, location, returnSpawn))
            return false;
        String target = tables.fieldAt(row, "location");
        if (target == null || !loadLocation(target, 0, false)) {
            arena.leave(gameplay, false);
            return false;
        }
        gameplay.equipment.apply(player);
        canvas.ui.show(UIStateMachine.ARENA);
        return true;
    }
    /** Runtime event: reports a cleared wave and returns victorious fighters after the last one. */
    public boolean arenaWaveComplete() {
        if (arena == null || !arena.clearWave())
            return false;
        return leaveArena(true);
    }
    public boolean leaveArena(boolean victorious) {
        if (arena == null || !arena.active())
            return false;
        String target = arena.returnLocation();
        int spawn = arena.returnSpawn();
        if (!arena.leave(gameplay, victorious))
            return false;
        gameplay.equipment.apply(player);
        boolean loaded = loadLocation(target, spawn, true);
        canvas.ui.show(loaded ? UIStateMachine.GAMEPLAY : UIStateMachine.ERROR);
        return loaded;
    }
    /** A thrown bolt forces nearby anomalies to reveal their next activation. */
    private void probeAnomalies() {
        EntityPool e = level.entities;
        long range = (long) com.microx.engine.math.Fixed.fromInt(4)
                * com.microx.engine.math.Fixed.fromInt(4);
        for (int i = 0; i < e.capacity(); i++)
            if (e.active[i] && e.type[i] == EntityPool.ANOMALY) {
                long dx = player.x - e.x[i], dz = player.z - e.z[i];
                if (dx * dx + dz * dz <= range)
                    e.timer[i] = 0;
            }
    }
    private void applyContainerDeltas(int container, Inventory inventory) {
        for (int item = 1; item <= ItemCatalog.maxId(); item++) {
            int delta = gameplay.containers.get(container, item);
            if (delta < 0)
                inventory.remove(item, Math.min(inventory.count(item), -delta));
            else if (delta > 0)
                inventory.add(item, delta, 100);
        }
    }
    private int locationId() {
        int hash = 0;
        for (int i = 0; i < location.length(); i++) hash = hash * 31 + location.charAt(i);
        return hash;
    }
    private static int corpseRank(EntityPool e, int i) {
        return e.spriteId[i] >= 20 ? 2 : 1;
    }
    public int tradeFaction() {
        return actorFaction;
    }
    public boolean repairMode() {
        return repairing;
    }
    public String tradeResult() {
        return operationResult;
    }
    public String containerTitle() {
        return containerSubtype == EntityPool.PLAYER_STASH ? "PLAYER CHEST" : "STASH";
    }
    public GameplayTables gameplayTables() {
        return tables;
    }
    public StorySystem storySystem() {
        return story;
    }
    public CutsceneSystem cutsceneSystem() {
        return cutscene;
    }
    public CyclicQuestSystem cyclicQuestSystem() {
        return cyclic;
    }
    public ArenaSystem arenaSystem() {
        return arena;
    }
    public String locationName() {
        return location;
    }
    public int state() {
        return state;
    }
}
