package com.microx.engine.ui;
import com.microx.engine.Input;
public final class UIStateMachineTest {
    public static void main(String[] args) {
        UIStateMachine ui = new UIStateMachine();
        eq(UIStateMachine.MAIN_MENU, ui.state(), "initial");
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.GAMEPLAY, ui.state(), "start");
        eq(UIStateMachine.ACTION_START, ui.action(), "start action");
        ui.command(Input.MENU);
        eq(UIStateMachine.PAUSE, ui.state(), "pause");
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.GAMEPLAY, ui.state(), "resume");
        ui.show(UIStateMachine.INVENTORY);
        eq(UIStateMachine.INVENTORY, ui.state(), "inventory");
        ui.command(Input.TAB_RIGHT);
        eq(UIStateMachine.MAP, ui.state(), "map");
        ui.command(Input.TAB_RIGHT);
        eq(UIStateMachine.QUESTS, ui.state(), "quests");
        ui.command(Input.TAB_RIGHT);
        eq(UIStateMachine.INVENTORY, ui.state(), "tabs only contain three screens");
        ui.command(Input.TAB_LEFT);
        eq(UIStateMachine.QUESTS, ui.state(), "reverse tab wrap");
        ui.show(UIStateMachine.DIALOGUE);
        ui.fillList(new short[] {1, 2}, 2);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.ACTION_LIST_ACCEPT, ui.action(), "dialogue command");
        check(ui.modal(), "dialogue modal");
        ui.show(UIStateMachine.TRADE);
        check(ui.modal(), "trade modal");
        ui.show(UIStateMachine.LOOT);
        check(ui.modal(), "loot modal");
        ui.show(UIStateMachine.GAMEPLAY);
        check(!ui.modal(), "gameplay nonmodal");
        ui.show(UIStateMachine.CUTSCENE);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.ACTION_LIST_ACCEPT, ui.action(), "cutscene advance action");
        ui.show(UIStateMachine.ARENA);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.ACTION_LIST_ACCEPT, ui.action(), "arena leave action");
        ui.show(UIStateMachine.CYCLIC_QUEST);
        check(ui.modal(), "cyclic quest notification modal");
        ui.show(UIStateMachine.FREEPLAY);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.ACTION_LIST_ACCEPT, ui.action(), "freeplay acknowledge action");
        ui.show(UIStateMachine.PAUSE);
        ui.command(Input.DOWN);
        ui.command(Input.DOWN);
        ui.command(Input.DOWN);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.SETTINGS, ui.state(), "settings");
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.ACTION_APPLY_SETTINGS, ui.action(), "settings confirmation action");
        ui.command(Input.BACK_CMD);
        eq(UIStateMachine.PAUSE, ui.state(), "settings back");
        ui.error();
        eq(UIStateMachine.ERROR, ui.state(), "error");
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.MAIN_MENU, ui.state(), "error acknowledge");
        ui.command(Input.DOWN);
        ui.command(Input.DOWN);
        ui.command(Input.DOWN);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.ABOUT, ui.state(), "about menu item");
        ui.command(Input.BACK_CMD);
        eq(UIStateMachine.MAIN_MENU, ui.state(), "about back");
        ui.setDebugMenu(true);
        for (int i = 0; i < 4; i++) ui.command(Input.DOWN);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.TEST_MENU, ui.state(), "debug test menu");
        ui.command(Input.DOWN);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.TEST_VIEW, ui.state(), "test preview");
        eq(UIStateMachine.ACTION_TEST_OPEN, ui.action(), "test preview action");
        ui.command(Input.BACK_CMD);
        eq(UIStateMachine.TEST_MENU, ui.state(), "preview back to tests");
        ui.command(Input.BACK_CMD);
        eq(UIStateMachine.MAIN_MENU, ui.state(), "tests back to main");
        UISettings settings = new UISettings();
        settings.change(4, 20);
        eq(10, settings.sensitivity, "sensitivity upper clamp");
        settings.change(4, -20);
        eq(1, settings.sensitivity, "sensitivity lower clamp");
        System.out.println("UIStateMachineTest OK");
    }
    private static void eq(int wanted, int got, String label) {
        if (wanted != got)
            throw new AssertionError(label + ": " + got);
    }
    private static void check(boolean value, String label) {
        if (!value)
            throw new AssertionError(label);
    }
}
