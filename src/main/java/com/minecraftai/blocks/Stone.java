package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

public class Stone extends Block {
    private static int texture;
    public Stone(int x, int y, int z) {
        super(x, y, z);
        if (texture == 0) {
            texture = TextureLoader.loadTexture("/assets/textures/block/stone.png");
        }
    }

    @Override
    public int getTextureID(Face face) {
        return texture;
    }
}