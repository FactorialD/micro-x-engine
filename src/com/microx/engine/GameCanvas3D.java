package com.microx.engine;
import javax.microedition.lcdui.*;
import com.microx.engine.ui.*;
public final class GameCanvas3D extends GameCanvas {
    private final Engine engine;
    public final UIStateMachine ui = new UIStateMachine();
    public final UISettings settings = new UISettings();
    private final UIView view = new UIView();
    private boolean started;
    public GameCanvas3D(Engine e, boolean d) {
        super(false);
        engine = e;
        settings.debug = d;
        e.attach(this);
        setFullScreenMode(true);
    }
    protected void keyPressed(int key) {
        if (ui.state() == UIStateMachine.GAMEPLAY) {
            if (key == '*') {
                ui.show(UIStateMachine.PDA);
                return;
            }
            if (key == -6 || key == -7) {
                ui.show(UIStateMachine.PAUSE);
                return;
            }
            engine.input.key(key, true, System.currentTimeMillis());
            return;
        }
        int cmd = Input.command(key);
        if (ui.state() == UIStateMachine.SETTINGS
                && (cmd == Input.UI_LEFT || cmd == Input.UI_RIGHT)) {
            settings.change(ui.selection(), cmd == Input.UI_RIGHT ? 1 : -1);
            return;
        }
        ui.command(cmd);
        handleAction();
        renderFrame();
    }
    protected void keyReleased(int key) {
        engine.input.key(key, false, System.currentTimeMillis());
    }
    private void handleAction() {
        int action = ui.action();
        if (action == UIStateMachine.ACTION_START && !started) {
            started = true;
            try {
                if (!engine.start()) {
                    started = false;
                    ui.error();
                }
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
    }
    public boolean gameplayBlocked() {
        return ui.modal();
    }
    public void showInitial() {
        renderFrame();
    }
    public void renderFrame() {
        Graphics g = getGraphics();
        if (engine.level != null) {
            engine.renderer.render(g, engine.player, engine.level.world, engine.level.entities);
            engine.stats.submittedTriangles = engine.renderer.submittedTriangles();
            engine.stats.clippedTriangles = engine.renderer.clippedTriangles();
            engine.stats.drawnTriangles = engine.renderer.drawnTriangles();
            engine.hud.paint(g, engine.player, engine.level.world, engine.stats.fps,
                    engine.stats.entities, engine.stats.rooms, settings.debug);
        }
        if (ui.state() != UIStateMachine.GAMEPLAY)
            view.paint(g, ui, settings);
        flushGraphics();
    }
}
