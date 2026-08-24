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
        ui.show(UIStateMachine.PDA);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.INVENTORY, ui.state(), "inventory");
        ui.command(Input.TAB_RIGHT);
        eq(UIStateMachine.MAP, ui.state(), "map");
        ui.command(Input.TAB_RIGHT);
        eq(UIStateMachine.QUESTS, ui.state(), "quests");
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
        ui.show(UIStateMachine.PAUSE);
        ui.command(Input.DOWN);
        ui.command(Input.DOWN);
        ui.command(Input.DOWN);
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.SETTINGS, ui.state(), "settings");
        ui.command(Input.BACK_CMD);
        eq(UIStateMachine.PAUSE, ui.state(), "settings back");
        ui.error();
        eq(UIStateMachine.ERROR, ui.state(), "error");
        ui.command(Input.ACCEPT);
        eq(UIStateMachine.MAIN_MENU, ui.state(), "error acknowledge");
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
