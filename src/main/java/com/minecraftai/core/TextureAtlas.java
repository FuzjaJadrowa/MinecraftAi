package com.minecraftai.core;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TextureAtlas {
    private static final String[] TEXTURE_PATHS = {
        "/assets/textures/block/grass.png",        // 0
        "/assets/textures/block/dirt.png",         // 1
        "/assets/textures/block/stone.png",        // 2
        "/assets/textures/block/cobblestone.png",  // 3
        "/assets/textures/block/log.png",          // 4
        "/assets/textures/block/log_top.png",      // 5
        "/assets/textures/block/leaves.png",       // 6
        "/assets/textures/block/water.png",        // 7
        "/assets/textures/block/bedrock.png"       // 8
    };

    private static int atlasTextureID = 0;
    private static final int TEXTURE_COUNT = TEXTURE_PATHS.length;

    public static void init() {
        if (atlasTextureID != 0) return;

        try {
            BufferedImage[] images = new BufferedImage[TEXTURE_COUNT];
            int tileW = 16;
            int tileH = 16;

            for (int i = 0; i < TEXTURE_COUNT; i++) {
                try (InputStream is = TextureAtlas.class.getResourceAsStream(TEXTURE_PATHS[i])) {
                    if (is == null) throw new IOException("Nie znaleziono tekstury: " + TEXTURE_PATHS[i]);
                    images[i] = ImageIO.read(is);
                    if (i == 0) {
                        tileW = images[i].getWidth();
                        tileH = images[i].getHeight();
                    }
                }
            }

            int atlasW = tileW * TEXTURE_COUNT;
            int atlasH = tileH;
            BufferedImage atlasImage = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = atlasImage.createGraphics();

            for (int i = 0; i < TEXTURE_COUNT; i++) {
                g.drawImage(images[i], i * tileW, 0, tileW, tileH, null);
            }
            g.dispose();

            int[] pixels = new int[atlasW * atlasH];
            atlasImage.getRGB(0, 0, atlasW, atlasH, pixels, 0, atlasW);

            ByteBuffer buffer = ByteBuffer.allocateDirect(atlasW * atlasH * 4);
            for (int y = 0; y < atlasH; y++) {
                for (int x = 0; x < atlasW; x++) {
                    int pixel = pixels[y * atlasW + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            buffer.flip();

            atlasTextureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, atlasTextureID);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasW, atlasH, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        } catch (IOException e) {
            throw new RuntimeException("Nie udało się utworzyć atlasu tekstur", e);
        }
    }

    public static int getAtlasTextureID() {
        if (atlasTextureID == 0) {
            init();
        }
        return atlasTextureID;
    }

    /**
     * Zwraca współrzędne UV dla danego indeksu tekstury.
     * Zwraca tablicę {uMin, vMin, uMax, vMax} z lekkim wcięciem (margin),
     * aby zapobiec przenikaniu krawędzi (texture bleeding) w OpenGL.
     */
    public static float[] getUV(int index) {
        float uMin = (index + 0.001f) / (float) TEXTURE_COUNT;
        float uMax = (index + 0.999f) / (float) TEXTURE_COUNT;
        float vMin = 0.001f;
        float vMax = 0.999f;
        return new float[]{uMin, vMin, uMax, vMax};
    }
}
