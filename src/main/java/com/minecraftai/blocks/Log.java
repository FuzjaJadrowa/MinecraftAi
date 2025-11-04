package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

public class Log extends Block {
    private static int logtopTexture;
    private static int logTexture;
    public Log(int x, int y, int z) {
        super(x, y, z);
        if (logtopTexture == 0) {
            logtopTexture = TextureLoader.loadTexture("/assets/textures/block/log_top.png");
            logTexture = TextureLoader.loadTexture("/assets/textures/block/log.png");
        }
    }

    @Override
    public int getTextureID(Face face) {
        switch (face) {
            case TOP, BOTTOM:
                return logtopTexture;
            default:
                return logTexture;
        }
    }
}