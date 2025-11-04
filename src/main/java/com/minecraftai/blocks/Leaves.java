package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

public class Leaves extends Block {
    private static int texture;
    public Leaves(int x, int y, int z) {
        super(x, y, z);
        if (texture == 0) {
            texture = TextureLoader.loadTexture("/assets/textures/block/leaves.png");
        }
    }

    @Override
    public int getTextureID(Face face) {
        return texture;
    }

    @Override
    public void render() {}
}