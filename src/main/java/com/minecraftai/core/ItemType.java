package com.minecraftai.core;

import com.minecraftai.renderer.TextureLoader;

public enum ItemType {
    DIRT,
    COBBLESTONE,
    LOG,
    PLANKS,
    CRAFTING_TABLE,
    STICK,
    FURNACE,
    STONE;

    private int textureId = 0;

    public int getTextureId() {
        if (textureId == 0) {
            switch (this) {
                case DIRT:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/dirt.png");
                    break;
                case COBBLESTONE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/cobblestone.png");
                    break;
                case LOG:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/log.png");
                    break;
                case PLANKS:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/planks.png");
                    break;
                case CRAFTING_TABLE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/crafting_table_front.png");
                    break;
                case STICK:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/stick.png");
                    break;
                case FURNACE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/furnace_front.png");
                    break;
                case STONE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/stone.png");
                    break;
            }
        }
        return textureId;
    }
}