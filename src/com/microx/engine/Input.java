package com.microx.engine;

/** Physical-key normalizer. UI code consumes commands; gameplay consumes bits. */
public final class Input {
    public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8, STRAFE_LEFT = 16,
                            STRAFE_RIGHT = 32, FIRE = 64, AIM = 128, CROUCH = 256, JUMP = 512,
                            WEAPON = 1024, PDA = 2048, PAUSE = 4096;
    public static final int NONE = 0, UP = 1, DOWN = 2, UI_LEFT = 3, UI_RIGHT = 4, ACCEPT = 5,
                            BACK_CMD = 6, MENU = 7, TAB_LEFT = 8, TAB_RIGHT = 9;
    private int down, pressed, doubleTap, held;
    private final int[] physicalKeys = new int[16], physicalBits = new int[16];
    private final long[] last = new long[13], since = new long[13];
    public synchronized void key(int key, boolean on, long now) {
        key(key, 0, on, now);
    }
    /** Handles a raw key and its MIDP game action as one physical key. */
    public synchronized void key(int key, int action, boolean on, long now) {
        int slot = physicalSlot(key), b;
        if (!on && slot >= 0)
            b = physicalBits[slot];
        else
            b = gameplayBit(key, action);
        if (b == 0)
            return;
        int i = index(b);
        if (on) {
            if (slot < 0) {
                slot = freePhysicalSlot();
                if (slot >= 0) {
                    physicalKeys[slot] = key;
                    physicalBits[slot] = b;
                }
            }
            if ((down & b) == 0) {
                pressed |= b;
                if (now - last[i] <= 280)
                    doubleTap |= b;
                last[i] = now;
                since[i] = now;
            }
            down |= b;
        } else {
            if (slot >= 0) {
                physicalKeys[slot] = 0;
                physicalBits[slot] = 0;
            }
            if (!physicalDown(b)) {
                down &= ~b;
                held &= ~b;
            }
        }
    }
    public synchronized void update(long now) {
        for (int i = 0, b = 1; i < 13; i++, b <<= 1)
            if ((down & b) != 0 && now - since[i] >= 450)
                held |= b;
    }
    public synchronized void endUpdate() {
        pressed = doubleTap = 0;
    }
    public int down() {
        return down;
    }
    public int pressed() {
        return pressed;
    }
    public int doubleTapped() {
        return doubleTap;
    }
    public int held() {
        return held;
    }
    private int index(int b) {
        int i = 0;
        while ((b >>= 1) != 0) i++;
        return i;
    }
    /** Includes common Nokia/Sony Ericsson game-action and soft-key codes. */
    public static int command(int k) {
        return command(k, 0);
    }
    public static int command(int k, int action) {
        // Some handsets report FIRE as the game action for their soft keys.  A useful raw
        // soft-key code is therefore authoritative and must be considered first.
        if (k == -6)
            return BACK_CMD;
        if (k == -7)
            return MENU;
        int actionCommand = commandForAction(action);
        if (actionCommand != NONE)
            return actionCommand;
        switch (k) {
            case '2':
            case -1:
                return UP;
            case '8':
            case -2:
                return DOWN;
            case '4':
            case -3:
                return UI_LEFT;
            case '6':
            case -4:
                return UI_RIGHT;
            case '5':
            case -5:
                return ACCEPT;
            case '*':
                return TAB_LEFT;
            case '#':
                return TAB_RIGHT;
            default:
                return NONE;
        }
    }
    public static int gameplayBit(int k) {
        return gameplayBit(k, 0);
    }
    public static int gameplayBit(int k, int action) {
        int actionBit = gameplayBitForAction(action);
        if (actionBit != 0)
            return actionBit;
        switch (k) {
            case '2':
            case -1:
                return FORWARD;
            case '8':
            case -2:
                return BACK;
            case '4':
            case -3:
                return LEFT;
            case '6':
            case -4:
                return RIGHT;
            case '1':
                return STRAFE_LEFT;
            case '3':
                return STRAFE_RIGHT;
            case '5':
            case -5:
                return FIRE;
            case '7':
                return AIM;
            case '9':
                return CROUCH;
            case '0':
                return JUMP;
            case '#':
                return WEAPON;
            case '*':
                return PDA;
            case -6:
            case -7:
                return PAUSE;
            default:
                return 0;
        }
    }
    /* MIDP Canvas game-action values: UP=1, LEFT=2, RIGHT=5, DOWN=6, FIRE=8. */
    private static int gameplayBitForAction(int action) {
        switch (action) {
            case 1:
                return FORWARD;
            case 6:
                return BACK;
            case 2:
                return LEFT;
            case 5:
                return RIGHT;
            case 8:
                return FIRE;
            default:
                return 0;
        }
    }
    private static int commandForAction(int action) {
        switch (action) {
            case 1:
                return UP;
            case 6:
                return DOWN;
            case 2:
                return UI_LEFT;
            case 5:
                return UI_RIGHT;
            case 8:
                return ACCEPT;
            default:
                return NONE;
        }
    }
    private int physicalSlot(int key) {
        for (int i = 0; i < physicalBits.length; i++)
            if (physicalBits[i] != 0 && physicalKeys[i] == key)
                return i;
        return -1;
    }
    private int freePhysicalSlot() {
        for (int i = 0; i < physicalBits.length; i++)
            if (physicalBits[i] == 0)
                return i;
        return -1;
    }
    private boolean physicalDown(int bit) {
        for (int i = 0; i < physicalBits.length; i++)
            if (physicalBits[i] == bit)
                return true;
        return false;
    }
}
