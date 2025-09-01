package com.minecraftai.engine;

import com.minecraftai.blocks.GrassBlock;
import com.minecraftai.generator.Tree;

import java.util.ArrayList;
import java.util.List;

public class World {

    private List<Block> blocks = new ArrayList<>();

    public List<Block> getBlocks() {
        return blocks;
    }

    public World() {
        for (int x=0; x<64; x++) {
            for (int z=0; z<64; z++) {
                blocks.add(new GrassBlock(x, 0, z));
            }
        }
        Tree treeGen = new Tree(this);
        treeGen.generateRandomTrees(40);
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

    public int getHeightAt(int x, int z) {
        int maxY = -1;
        for (Block b : blocks) {
            if (b.getX() == x && b.getZ() == z) {
                if (b.getY() > maxY) maxY = (int)b.getY();
            }
        }
        return maxY;
    }
}