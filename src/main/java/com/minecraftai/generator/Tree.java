package com.minecraftai.generator;

import com.minecraftai.blocks.Leaves;
import com.minecraftai.blocks.Log;
import com.minecraftai.engine.Block;
import com.minecraftai.engine.Chunk;

import java.util.Random;

public class Tree {
   private static final Random random = new Random();

    public static void generateTree(Block[][][] blocks, int localX, int localY, int localZ, int globalX, int globalZ) {
        int treeHeight = 4 + random.nextInt(3); // Wysokość 4-6
        if (localY + treeHeight + 1 >= Chunk.CHUNK_SIZE_Y) {
            return;
        }

        for (int i = 0; i < treeHeight; i++) {
            int currentY = localY + i;
            blocks[localX][currentY][localZ] = new Log(globalX, currentY, globalZ);
        }

        int leafStartY = localY + treeHeight - 2;
        int leafTopY = localY + treeHeight + 1;

        for (int y = leafStartY; y < leafTopY; y++) {
            int radius = (y == leafTopY - 1) ? 1 : 2;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
                        continue;
                    }

                    int currentLocalX = localX + x;
                    int currentLocalZ = localZ + z;

                    if (currentLocalX < 0 || currentLocalX >= Chunk.CHUNK_SIZE_X ||
                            currentLocalZ < 0 || currentLocalZ >= Chunk.CHUNK_SIZE_Z) {
                        continue;
                    }

                    if (blocks[currentLocalX][y][currentLocalZ] == null) {
                        int currentGlobalX = globalX + x;
                        int currentGlobalZ = globalZ + z;
                        blocks[currentLocalX][y][currentLocalZ] = new Leaves(currentGlobalX, y, currentGlobalZ);
                    }
                }
            }
        }
    }
}