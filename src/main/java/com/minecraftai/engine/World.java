package com.minecraftai.engine;

import com.minecraftai.blocks.GrassBlock;

public class World {
    private Block[][] blocks;

    public World() {
        blocks = new Block[16][16];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                blocks[x][z] = new GrassBlock(x, 0, z);
            }
        }
    }

    public void render() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                blocks[x][z].render();
            }
        }
    }
}