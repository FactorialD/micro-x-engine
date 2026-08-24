package com.microx.engine.gameplay;

/** Atomic economy operations; validation always precedes mutation. */
public final class TradeSystem {
    private TradeSystem() {}
    public static boolean buy(
            Inventory buyer, Inventory seller, int item, int amount, int faction, Reputation rep) {
        if (amount <= 0 || seller.count(item) < amount || !buyer.canAdd(item, amount))
            return false;
        int price = cost(item, amount, rep.buyPercent(faction));
        if (buyer.money() < price)
            return false;
        buyer.setMoney(buyer.money() - price);
        seller.setMoney(seller.money() + price);
        seller.moveTo(buyer, item, amount);
        return true;
    }
    public static boolean sell(
            Inventory seller, Inventory buyer, int item, int amount, int faction, Reputation rep) {
        if (amount <= 0 || seller.count(item) < amount || !buyer.canAdd(item, amount))
            return false;
        int price = cost(item, amount, rep.sellPercent(faction));
        if (buyer.money() < price)
            return false;
        buyer.setMoney(buyer.money() - price);
        seller.setMoney(seller.money() + price);
        seller.moveTo(buyer, item, amount);
        return true;
    }
    public static boolean repair(Inventory owner, int item, int mechanicPercent) {
        int condition = owner.conditionOf(item);
        if (owner.count(item) < 1 || condition >= 100 || mechanicPercent < 1)
            return false;
        int price = (ItemCatalog.VALUE[item] * (100 - condition) * mechanicPercent + 9999) / 10000;
        if (owner.money() < price)
            return false;
        owner.setMoney(owner.money() - price);
        owner.setCondition(item, 100);
        return true;
    }
    private static int cost(int item, int amount, int percent) {
        long n = (long) ItemCatalog.VALUE[item] * amount * percent;
        return (int) ((n + 99) / 100);
    }
}
