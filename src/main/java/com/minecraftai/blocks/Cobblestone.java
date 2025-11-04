package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

public class Cobblestone extends Block {
    private static int texture;
    public Cobblestone(int x, int y, int z) {
        super(x, y, z);
        if (texture == 0) {
            texture = TextureLoader.loadTexture("/assets/textures/block/cobblestone.png");
        }
    }

    @Override
    public int getTextureID(Face face) {
        return texture;
    }

    @Override
    public void render() {}
}