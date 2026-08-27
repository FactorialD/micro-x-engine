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
    private SaveStore saves;
    private SettingsStore settings;
    private String location = "test";
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
        GameplayTables tables = new GameplayTables();
        if (!tables.load("/data/gameplay.dat") || !ItemCatalog.install(tables))
            return false;
        persistent = new SaveData();
        persistent.seed = 0x4d58534d;
        if (!loadLocation("test", 0, false))
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
        persistent.armor = player.armor;
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
                                                   : (level.entities.state[i] << 16)
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
                level.entities.state[i] = s.entityFlags[n] >>> 16;
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
        player.armor = s.armor;
        player.stamina = s.stamina;
        player.bleeding = s.bleeding;
        player.radiation = s.radiation;
        player.combat.equip(s.weapon);
        player.combat.magazine = s.magazine;
        for (int i = 0; i < s.reserveAmmo.length; i++) player.reserveAmmo[i] = s.reserveAmmo[i];
        player.ammo = s.magazine;
        level.world.updateVisibility(player.x, player.z);
    }
    private boolean loadLocation(String name, int spawn, boolean autosave) {
        if (autosave && !saveGame())
            return false;
        LevelLoader candidate = new LevelLoader();
        if (!candidate.load("/levels/" + name + "/level.lvl"))
            return false;
        audio.leaveLocation();
        assets.release();
        if (!candidate.selectSpawn(spawn) || !assets.loadLocation(name, 0)) {
            candidate.clear();
            return false;
        }
        location = name;
        level = candidate;
        player.reset(candidate.startX, candidate.startY, candidate.startZ);
        player.yaw = candidate.startYaw;
        candidate.world.updateVisibility(player.x, player.z);
        systems.enter(candidate.entities);
        audio.enterLocation(name);
        return true;
    }
    public void applySettings() {
        renderer.setResolutionMode(canvas.settings.resolution);
        audio.setVolume(canvas.settings.volume);
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
        if (old != null && old != Thread.currentThread())
            try {
                old.join(1000);
            } catch (InterruptedException ignored) {
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
                                + ItemCatalog.damageBonus(ItemCatalog.ammo(player.combat.weapon)));
        }
        player.ammo = player.combat.magazine;
        stats.entities = level.entities.activeCount();
        input.endUpdate();
    }
    private void interact(int i) {
        EntityPool e = level.entities;
        int type = e.type[i], stable = e.stableId[i], content = e.aux[i];
        if (type == EntityPool.HUMAN) {
            gameplay.actorId = content == 0 ? GameIds.NPC_SIDOROVICH : content;
            short[] rows = {(short) GameIds.DIALOG_INTRO, (short) GameIds.DIALOG_TRADE};
            canvas.ui.fillList(rows, rows.length);
            canvas.ui.show(UIStateMachine.DIALOGUE);
        } else if (type == EntityPool.CORPSE || type == EntityPool.CONTAINER) {
            gameplay.containerId = stable;
            gameplay.loot.clear();
            if (content != 0 && gameplay.containers.get(stable, content) >= 0)
                gameplay.loot.add(content, 1, 100);
            fillInventory(canvas.ui, gameplay.loot);
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
    public void uiAction(int screen, int selection, boolean alternate) {
        short[] list = canvas.ui.listBuffer();
        int id = selection < canvas.ui.listSize() ? list[selection] & 65535 : 0;
        if (screen == UIStateMachine.DIALOGUE) {
            if (id == GameIds.DIALOG_INTRO) {
                if (gameplay.quests.state(GameIds.QUEST_FIND_STASH) == 0)
                    gameplay.acceptFindStash();
                else
                    gameplay.rewardFindStash();
            } else {
                gameplay.trader.clear();
                gameplay.trader.setMoney(5000);
                gameplay.trader.add(GameIds.ITEM_MEDKIT, 2, 100);
                fillInventory(canvas.ui, gameplay.trader);
                canvas.ui.show(UIStateMachine.TRADE);
            }
        } else if (screen == UIStateMachine.TRADE && id != 0) {
            if (alternate && gameplay.actorId == GameIds.NPC_TECHNICIAN)
                TradeSystem.repair(gameplay.inventory, id, 100);
            else if (alternate)
                TradeSystem.sell(
                        gameplay.inventory, gameplay.trader, id, 1, 1, gameplay.reputation);
            else
                TradeSystem.buy(gameplay.inventory, gameplay.trader, id, 1, 1, gameplay.reputation);
        } else if (screen == UIStateMachine.LOOT && id != 0
                && gameplay.loot.moveTo(gameplay.inventory, id, 1)) {
            gameplay.containers.put(gameplay.containerId, id, -1);
            fillInventory(canvas.ui, gameplay.loot);
        } else if (screen == UIStateMachine.INVENTORY && id != 0) {
            if (ItemCatalog.type(id) == ItemCatalog.TYPE_CONSUMABLE)
                gameplay.equipment.use(gameplay.inventory, id, gameplay.stats);
            else
                gameplay.equipment.equip(gameplay.inventory, id, alternate ? 1 : 0);
        }
    }
    public int state() {
        return state;
    }
}
