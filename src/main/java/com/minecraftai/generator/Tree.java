package com.minecraftai.generator;

import com.minecraftai.blocks.Leaves;
import com.minecraftai.blocks.Log;
import com.minecraftai.engine.World;

import java.util.Random;

public class Tree {

    private World world;
    private Random random = new Random();

    public Tree(World world) {
        this.world = world;
    }

    public void generateRandomTrees(int count) {
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(64);
            int z = random.nextInt(64);
            int y = world.getHeightAt(x, z);

            if (y < 0) continue;

            generateTree(x, y, z);
        }
    }

    private void generateTree(int x, int y, int z) {
        int treeHeight = 3 + random.nextInt(3);

        for (int i = 0; i < treeHeight; i++) {
            world.addBlock(x, y + i, z, new Log(x, y + i, z));
        }

        int leafStart = y + treeHeight - 2;
        int leafEnd = y + treeHeight;
        for (int lx = -1; lx <= 1; lx++) {
            for (int ly = 0; ly <= 2; ly++) {
                for (int lz = -1; lz <= 1; lz++) {
                    if (Math.abs(lx) == 1 && Math.abs(lz) == 1 && ly == 2) continue;
                    world.addBlock(x + lx, leafStart + ly, z + lz, new Leaves(x + lx, leafStart + ly, z + lz));
                }
            }
        }
    }
}