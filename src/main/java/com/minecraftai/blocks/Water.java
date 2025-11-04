package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

import static org.lwjgl.opengl.GL11.*;

public class Water extends Block {
    private static int texture;
    private static final float WATER_ALPHA = 0.7f;
    private static final float WATER_LEVEL_OFFSET = 0.125f;

    public Water(int x, int y, int z) {
        super(x, y, z);

        this.isTransparent = true;
        this.blockHeight = 1.0f - WATER_LEVEL_OFFSET; // np. 0.875f

        if (texture == 0) {
            texture = TextureLoader.loadTexture("/assets/textures/block/water.png");
        }
    }

    @Override
    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);
        setTransparent(WATER_ALPHA);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture);

        glBegin(GL_QUADS);

        glNormal3f(0,1,0);
        glTexCoord2f(0,0); glVertex3f(0, blockHeight, 0);
        glTexCoord2f(1,0); glVertex3f(1, blockHeight, 0);
        glTexCoord2f(1,1); glVertex3f(1, blockHeight, 1);
        glTexCoord2f(0,1); glVertex3f(0, blockHeight, 1);

        glNormal3f(0,-1,0);
        glTexCoord2f(0,0); glVertex3f(0,0,0);
        glTexCoord2f(1,0); glVertex3f(1,0,0);
        glTexCoord2f(1,1); glVertex3f(1,0,1);
        glTexCoord2f(0,1); glVertex3f(0,0,1);

        glNormal3f(0,0,1);
        glTexCoord2f(0,0); glVertex3f(0,0,1);
        glTexCoord2f(1,0); glVertex3f(1,0,1);
        glTexCoord2f(1,1); glVertex3f(1, blockHeight, 1);
        glTexCoord2f(0,1); glVertex3f(0, blockHeight, 1);

        glNormal3f(0,0,-1);
        glTexCoord2f(0,0); glVertex3f(0,0,0);
        glTexCoord2f(1,0); glVertex3f(1,0,0);
        glTexCoord2f(1,1); glVertex3f(1, blockHeight, 0);
        glTexCoord2f(0,1); glVertex3f(0, blockHeight, 0);

        glNormal3f(-1,0,0);
        glTexCoord2f(0,0); glVertex3f(0,0,0);
        glTexCoord2f(1,0); glVertex3f(0,0,1);
        glTexCoord2f(1,1); glVertex3f(0, blockHeight, 1);
        glTexCoord2f(0,1); glVertex3f(0, blockHeight, 0);

        glNormal3f(1,0,0);
        glTexCoord2f(0,0); glVertex3f(1,0,0);
        glTexCoord2f(1,0); glVertex3f(1,0,1);
        glTexCoord2f(1,1); glVertex3f(1, blockHeight, 1);
        glTexCoord2f(0,1); glVertex3f(1, blockHeight, 0);

        glEnd();
        glDisable(GL_TEXTURE_2D);
        setOpaque();
        glPopMatrix();
    }
}