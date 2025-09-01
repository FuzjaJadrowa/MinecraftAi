package com.minecraftai.engine;

public abstract class Block {
    protected int x, y, z;

    public Block(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public abstract void render();
}