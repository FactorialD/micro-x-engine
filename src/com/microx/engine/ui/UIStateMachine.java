package com.microx.engine.ui;
import com.microx.engine.Input;

/** Allocation-free navigation model shared by the MIDP view and desktop tests. */
public final class UIStateMachine {
    public static final int MAIN_MENU = 0, GAMEPLAY = 1, PAUSE = 2, PDA = 3, INVENTORY = 4, MAP = 5,
                            QUESTS = 6, DIALOGUE = 7, TRADE = 8, LOOT = 9, SETTINGS = 10,
                            ERROR = 11;
    public static final int ACTION_NONE = 0, ACTION_START = 1, ACTION_QUIT = 2,
                            ACTION_APPLY_SETTINGS = 3, ACTION_LOAD = 4, ACTION_SAVE = 5;
    private static final byte[] MODAL = {1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private int state = MAIN_MENU, previous = GAMEPLAY, selection, action;
    private final short[] list = new short[32];
    private int listSize;
    public int state() {
        return state;
    }
    public int selection() {
        return selection;
    }
    public int action() {
        int a = action;
        action = 0;
        return a;
    }
    public boolean modal() {
        return MODAL[state] != 0;
    }
    public short[] listBuffer() {
        return list;
    }
    public int listSize() {
        return listSize;
    }
    public void fillList(short[] source, int count) {
        listSize = count < list.length ? count : list.length;
        for (int i = 0; i < listSize; i++) list[i] = source[i];
        if (selection >= listSize)
            selection = listSize == 0 ? 0 : listSize - 1;
    }
    public void show(int next) {
        if (next < 0 || next > ERROR)
            return;
        if (next != GAMEPLAY && state == GAMEPLAY)
            previous = state;
        state = next;
        selection = 0;
    }
    public void error() {
        state = ERROR;
        selection = 0;
    }
    public void command(int command) {
        int count = itemCount();
        if (command == Input.UP && count > 0)
            selection = (selection + count - 1) % count;
        else if (command == Input.DOWN && count > 0)
            selection = (selection + 1) % count;
        else if (command == Input.TAB_LEFT || command == Input.TAB_RIGHT) {
            if (state == PDA || state == INVENTORY || state == MAP || state == QUESTS) {
                int d = command == Input.TAB_RIGHT ? 1 : 3;
                state = INVENTORY + (state - INVENTORY + d) % 4;
                selection = 0;
            }
        } else if (command == Input.MENU) {
            if (state == GAMEPLAY)
                show(PAUSE);
            else
                back();
        } else if (command == Input.BACK_CMD)
            back();
        else if (command == Input.ACCEPT)
            accept();
    }
    private void back() {
        if (state == MAIN_MENU || state == ERROR)
            return;
        if (state == SETTINGS) {
            state = previous;
            selection = 0;
        } else
            state = GAMEPLAY;
    }
    private void accept() {
        if (state == MAIN_MENU) {
            if (selection == 0) {
                state = GAMEPLAY;
                action = ACTION_START;
            } else if (selection == 1) {
                state = GAMEPLAY;
                action = ACTION_LOAD;
            } else if (selection == 2) {
                previous = MAIN_MENU;
                state = SETTINGS;
            } else
                action = ACTION_QUIT;
        } else if (state == PAUSE) {
            if (selection == 0)
                state = GAMEPLAY;
            else if (selection == 1)
                action = ACTION_SAVE;
            else if (selection == 2)
                action = ACTION_LOAD;
            else if (selection == 3) {
                previous = PAUSE;
                state = SETTINGS;
            } else
                state = MAIN_MENU;
        } else if (state == PDA)
            state = INVENTORY;
        else if (state == SETTINGS)
            action = ACTION_APPLY_SETTINGS;
        else if (state == ERROR)
            state = MAIN_MENU;
    }
    private int itemCount() {
        if (state == MAIN_MENU)
            return 4;
        if (state == PAUSE)
            return 5;
        if (state == SETTINGS)
            return 4;
        if (state == PDA)
            return 4;
        return listSize;
    }
}
