package com.minecraftai.engine;

public class ItemStack {
    private ItemType type;
    private int count;

    public static final int MAX_STACK_SIZE = 64;

    public ItemStack(ItemType type, int count) {
        this.type = type;
        this.count = count;
    }

    public ItemType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int addAmount(int amount) {
        int canAdd = MAX_STACK_SIZE - this.count;
        int toAdd = Math.min(amount, canAdd);
        this.count += toAdd;
        return amount - toAdd;
    }

    public boolean isFull() {
        return this.count >= MAX_STACK_SIZE;
    }
}