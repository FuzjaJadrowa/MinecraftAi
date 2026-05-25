package com.minecraftai.gui;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.TextureAtlas;

import static org.lwjgl.opengl.GL11.*;

public class GuiRenderer {

    public static void draw3DBlock(float centerX, float centerY, float size, ItemType type, float currentW, float currentH) {
        float r = size * 0.23f;

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -100.0f, 100.0f);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);

        glTranslatef(centerX, centerY, 0.0f);
        glRotatef(-30.0f, 1.0f, 0.0f, 0.0f);
        glRotatef(45.0f, 0.0f, 1.0f, 0.0f);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.TOP)),
                 -r, -r, -r,
                  r, -r, -r,
                  r, -r,  r,
                 -r, -r,  r);

        glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.BOTTOM)),
                 -r,  r, -r,
                  r,  r, -r,
                  r,  r,  r,
                 -r,  r,  r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.EAST)),
                  r, -r, -r,
                  r, -r,  r,
                  r,  r,  r,
                  r,  r, -r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.WEST)),
                 -r, -r, -r,
                 -r, -r,  r,
                 -r,  r,  r,
                 -r,  r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.NORTH)),
                 -r, -r, -r,
                  r, -r, -r,
                  r,  r, -r,
                 -r,  r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.SOUTH)),
                 -r, -r,  r,
                  r, -r,  r,
                  r,  r,  r,
                 -r,  r,  r);

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    private static void drawFace(float[] uv, float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3) {
        glBegin(GL_QUADS);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x0, y0, z0);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x1, y1, z1);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y2, z2);
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x3, y3, z3);
        glEnd();
    }

    private static int getBlockTextureIndex(ItemType type, Block.Face face) {
        switch (type) {
            case DIRT:
                return 1;
            case COBBLESTONE:
                return 3;
            case LOG:
                if (face == Block.Face.TOP || face == Block.Face.BOTTOM) {
                    return 5;
                } else {
                    return 4;
                }
            default:
                return 0;
        }
    }
}