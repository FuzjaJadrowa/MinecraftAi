package com.minecraftai.engine;

import com.minecraftai.blocks.GrassBlock;

import java.util.ArrayList;
import java.util.List;

public class World {

    public List<Block> blocks = new ArrayList<>();

    public World() {
        for (int x=0; x<64; x++) {
            for (int z=0; z<64; z++) {
                blocks.add(new GrassBlock(x, 0, z));
            }
        }
    }

    public void render() {
        for (Block b : blocks) {
            b.render();
        }
    }

    public void addBlock(int x, int y, int z, Block block) {
        blocks.add(block);
    }

    public void removeBlock(int x, int y, int z) {
        blocks.removeIf(b -> b.getX() == x && b.getY() == y && b.getZ() == z);
    }
}