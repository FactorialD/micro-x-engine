package com.microx.engine;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;

import com.microx.engine.ui.*;
import com.microx.engine.render.TestScene;
public final class GameCanvas3D extends GameCanvas {
    private final Engine engine;
    public final UIStateMachine ui = new UIStateMachine();
    public final UISettings settings = new UISettings();
    private final UIView view = new UIView();
    private final TestScene testScene = new TestScene();
    private boolean started;
    public GameCanvas3D(Engine e, boolean d) {
        super(false);
        engine = e;
        settings.debug = d;
        ui.setDebugMenu(d);
        e.attach(this);
        // attach() restores persisted settings, so use the effective value rather than only the
        // manifest default passed to the constructor.
        ui.setDebugMenu(settings.debug);
        setFullScreenMode(true);
    }
    protected void keyPressed(int key) {
        int gameAction = gameAction(key);
        engine.stats.input(key, gameAction, engine.input.down());
        if (ui.state() == UIStateMachine.GAMEPLAY) {
            if (key == '*') {
                engine.openPda();
                return;
            }
            if (key == -6 || key == -7) {
                ui.show(UIStateMachine.PAUSE);
                return;
            }
            engine.input.key(key, gameAction, true, System.currentTimeMillis());
            engine.stats.input(key, gameAction, engine.input.down());
            return;
        }
        int cmd = Input.command(key, gameAction);
        if (ui.state() == UIStateMachine.SETTINGS
                && (cmd == Input.UI_LEFT || cmd == Input.UI_RIGHT)) {
            settings.change(ui.selection(), cmd == Input.UI_RIGHT ? 1 : -1);
            ui.setDebugMenu(settings.debug);
            renderFrame();
            return;
        }
        ui.command(cmd);
        handleAction();
        renderFrame();
    }
    protected void keyReleased(int key) {
        int gameAction = gameAction(key);
        engine.input.key(key, gameAction, false, System.currentTimeMillis());
        engine.stats.input(key, gameAction, engine.input.down());
    }
    private int gameAction(int key) {
        try {
            return getGameAction(key);
        } catch (IllegalArgumentException unsupported) {
            return 0;
        }
    }
    private void handleAction() {
        int action = ui.action();
        if (action == UIStateMachine.ACTION_START) {
            try {
                if (!engine.startNewGame()) {
                    started = false;
                    ui.error();
                } else
                    started = true;
            } catch (Throwable failure) {
                started = false;
                ui.error();
            }
        } else if (action == UIStateMachine.ACTION_LOAD) {
            if (!started) {
                started = engine.start();
            }
            if (!started || !engine.loadGame())
                ui.error();
            else
                ui.show(UIStateMachine.GAMEPLAY);
        } else if (action == UIStateMachine.ACTION_SAVE) {
            if (!engine.saveGame())
                ui.error();
        } else if (action == UIStateMachine.ACTION_APPLY_SETTINGS)
            engine.applySettings();
        else if (action == UIStateMachine.ACTION_QUIT)
            engine.shutdown();
        else if (action == UIStateMachine.ACTION_LIST_ACCEPT)
            engine.uiAction(ui.state(), ui.selection(), false);
        else if (action == UIStateMachine.ACTION_LIST_ALT)
            engine.uiAction(ui.state(), ui.selection(), true);
        else if (action == UIStateMachine.ACTION_TEST_OPEN) {
            if (!testScene.open(ui.selection()))
                ui.error();
        }
    }
    public boolean gameplayBlocked() {
        return ui.modal();
    }
    public void showInitial() {
        renderFrame();
    }
    public void renderFrame() {
        Graphics g = getGraphics();
        if (ui.state() == UIStateMachine.TEST_VIEW) {
            testScene.paint(g, getWidth(), getHeight(), System.currentTimeMillis());
        } else if (engine.level != null) {
            engine.renderer.render(g, engine.player, engine.level.world, engine.level.entities);
            engine.stats.submittedTriangles = engine.renderer.submittedTriangles();
            engine.stats.clippedTriangles = engine.renderer.clippedTriangles();
            engine.stats.drawnTriangles = engine.renderer.drawnTriangles();
            engine.hud.paint(g, engine.player, engine.level.world, engine.stats,
                    engine.locationName(), engine.stateSource(), settings.debug);
        }
        view.bind(engine.gameplay, engine.gameplayTables(), engine.player, engine.level,
                engine.locationName(), engine.tradeFaction(), engine.repairMode(),
                engine.tradeResult(), engine.containerTitle());
        view.bindNarrative(engine.storySystem(), engine.cutsceneSystem(),
                engine.cyclicQuestSystem(), engine.arenaSystem());
        if (ui.state() != UIStateMachine.GAMEPLAY)
            view.paint(g, ui, settings);
        flushGraphics();
    }
}
