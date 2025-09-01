package com.minecraftai.engine;

public abstract class Block {
    protected int x, y, z;

    public Block(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public abstract void render();
}