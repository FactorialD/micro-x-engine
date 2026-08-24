package com.microx.engine.gameplay;

/** Bounded persistent deviations from seed-generated container contents. */
public final class ContainerDelta {
    private final int[] keys, container;
    private final short[] item, amount;
    public ContainerDelta(int capacity) {
        keys = new int[capacity];
        container = new int[capacity];
        item = new short[capacity];
        amount = new short[capacity];
    }
    public int size() {
        int n = 0;
        for (int i = 0; i < keys.length; i++)
            if (keys[i] != 0)
                n++;
        return n;
    }
    public boolean put(int containerId, int itemId, int delta) {
        if (containerId <= 0 || !ItemCatalog.valid(itemId) || delta < -32768 || delta > 32767)
            return false;
        int key = (containerId * 31) ^ itemId;
        for (int i = 0; i < keys.length; i++)
            if (keys[i] == key && container[i] == containerId && item[i] == itemId) {
                amount[i] = (short) delta;
                if (delta == 0) {
                    keys[i] = 0;
                    container[i] = 0;
                    item[i] = 0;
                }
                return true;
            }
        for (int i = 0; i < keys.length; i++)
            if (keys[i] == 0) {
                keys[i] = key;
                container[i] = containerId;
                item[i] = (short) itemId;
                amount[i] = (short) delta;
                return true;
            }
        return false;
    }
    public int get(int containerId, int itemId) {
        int key = (containerId * 31) ^ itemId;
        for (int i = 0; i < keys.length; i++)
            if (keys[i] == key && container[i] == containerId && item[i] == itemId)
                return amount[i];
        return 0;
    }
    public int capacity() {
        return keys.length;
    }
    public boolean occupied(int i) {
        return keys[i] != 0;
    }
    public int containerAt(int i) {
        return container[i];
    }
    public int itemAt(int i) {
        return item[i] & 65535;
    }
    public int deltaAt(int i) {
        return amount[i];
    }
    public void clear() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = container[i] = 0;
            item[i] = amount[i] = 0;
        }
    }
}
