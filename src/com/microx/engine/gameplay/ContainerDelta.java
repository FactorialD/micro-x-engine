package com.microx.engine.gameplay;

/**
 * Bounded persistent container store.  Despite the historical class name, version 4 saves store
 * complete container inventories (including condition), not quantity deltas.  Containers are
 * keyed by their globally unique entity stable id; player chests are therefore location-local and
 * independent.
 */
public final class ContainerDelta {
    private final int[] container;
    private final short[] item, amount, durability;
    private final int[] initialized;
    private int initializedCount;
    public ContainerDelta(int capacity) {
        container = new int[capacity];
        item = new short[capacity];
        amount = new short[capacity];
        durability = new short[capacity];
        initialized = new int[32];
    }
    public int size() {
        int n = 0;
        for (int i = 0; i < container.length; i++) if (container[i] != 0) n++;
        return n;
    }
    /** Legacy delta accessor, retained solely to migrate version 2/3 saves. */
    public boolean put(int containerId, int itemId, int delta) {
        if (containerId <= 0 || !ItemCatalog.valid(itemId) || delta < -32768 || delta > 32767)
            return false;
        for (int i = 0; i < container.length; i++)
            if (container[i] == containerId && item[i] == itemId) {
                amount[i] = (short) delta;
                durability[i] = 100;
                return true;
            }
        for (int i = 0; i < container.length; i++)
            if (container[i] == 0) {
                container[i] = containerId; item[i] = (short) itemId;
                amount[i] = (short) delta; durability[i] = 100;
                return true;
            }
        return false;
    }
    public int get(int containerId, int itemId) {
        for (int i = 0; i < container.length; i++)
            if (container[i] == containerId && item[i] == itemId) return amount[i];
        return 0;
    }
    public boolean initialized(int id) {
        for (int i = 0; i < initializedCount; i++) if (initialized[i] == id) return true;
        return false;
    }
    public boolean markInitialized(int id) {
        if (id <= 0) return false;
        if (initialized(id)) return true;
        if (initializedCount >= initialized.length) return false;
        initialized[initializedCount++] = id;
        return true;
    }
    public boolean capture(int id, Inventory inventory) {
        int needed = 0, available = 0;
        for (int i = 0; i < inventory.slots(); i++) if (inventory.idAt(i) != 0) needed++;
        for (int i = 0; i < container.length; i++)
            if (container[i] == 0 || container[i] == id) available++;
        if (id <= 0 || needed > available || !markInitialized(id)) return false;
        removeContainer(id);
        for (int s = 0; s < inventory.slots(); s++) if (inventory.idAt(s) != 0) {
            int i = free();
            container[i] = id; item[i] = (short) inventory.idAt(s);
            amount[i] = (short) inventory.countAt(s);
            durability[i] = (short) inventory.durabilityAt(s);
        }
        return true;
    }
    public boolean restore(int id, Inventory inventory) {
        if (!initialized(id)) return false;
        inventory.clear();
        for (int i = 0; i < container.length; i++)
            if (container[i] == id && amount[i] > 0
                    && !inventory.add(item[i] & 65535, amount[i] & 65535, durability[i] & 65535))
                return false;
        return true;
    }
    private int free() { for (int i = 0; i < container.length; i++) if (container[i] == 0) return i; return -1; }
    private void removeContainer(int id) {
        for (int i = 0; i < container.length; i++) if (container[i] == id) {
            container[i] = 0; item[i] = amount[i] = durability[i] = 0;
        }
    }
    public int capacity() { return container.length; }
    public boolean occupied(int i) { return container[i] != 0; }
    public int containerAt(int i) { return container[i]; }
    public int itemAt(int i) { return item[i] & 65535; }
    public int deltaAt(int i) { return amount[i]; }
    public int durabilityAt(int i) { return durability[i] & 65535; }
    public int initializedCount() { return initializedCount; }
    public int initializedAt(int i) { return initialized[i]; }
    public boolean restoreRecord(int id, int itemId, int count, int condition) {
        if (id <= 0 || !ItemCatalog.valid(itemId) || count <= 0 || condition < 0 || condition > 100)
            return false;
        int i = free();
        if (i < 0) return false;
        container[i] = id; item[i] = (short) itemId; amount[i] = (short) count;
        durability[i] = (short) condition;
        return true;
    }
    public void clear() {
        for (int i = 0; i < container.length; i++) container[i] = 0;
        initializedCount = 0;
    }
}
