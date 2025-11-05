package com.minecraftai.engine;

public enum ItemType {
    DIRT,
    COBBLESTONE,
    LOG;

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
            }
        }
        return textureId;
    }
}