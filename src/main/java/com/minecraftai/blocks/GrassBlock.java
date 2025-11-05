package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.ItemType;
import com.minecraftai.engine.TextureLoader;

public class GrassBlock extends Block {
    private static int grassTexture;
    private static int dirtTexture;

    public GrassBlock(int x, int y, int z) {
        super(x, y, z);

        if (grassTexture == 0) {
            grassTexture = TextureLoader.loadTexture("/assets/textures/block/grass.png");
            dirtTexture = TextureLoader.loadTexture("/assets/textures/block/dirt.png");
        }
    }

    @Override
    public int getTextureID(Face face) {
        switch (face) {
            case TOP:
                return grassTexture;
            default:
                return dirtTexture;
        }
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.DIRT;
    }
}