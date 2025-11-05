package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.ItemType;
import com.minecraftai.engine.TextureLoader;

public class Water extends Block {
    private static int texture;

    public Water(int x, int y, int z) {
        super(x, y, z);
        this.isTransparent = true;
        this.blockHeight = 0.875f;
        this.isDestructible = false;

        if (texture == 0) {
            texture = TextureLoader.loadTexture("/assets/textures/block/water.png");
        }
    }

    @Override
    public int getTextureID(Face face) {
        return texture;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean collidesWithPlayer(float playerX, float playerY, float playerZ) {
        return false;
    }

    @Override
    public ItemType getItemDrop() {
        return null;
    }
}