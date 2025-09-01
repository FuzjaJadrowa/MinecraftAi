package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

import static org.lwjgl.opengl.GL11.*;

public class GrassBlock extends Block {

    private static int grassTexture;
    private static int dirtTexture;

    public GrassBlock(int x, int y, int z) {
        super(x, y, z);

        if (grassTexture == 0) {
            grassTexture = TextureLoader.loadTexture("/textures/block/grass.png");
            dirtTexture = TextureLoader.loadTexture("/textures/block/dirt.png");
        }
    }

    @Override
    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);

        glEnable(GL_TEXTURE_2D);

        glBindTexture(GL_TEXTURE_2D, grassTexture);
        glBegin(GL_QUADS);
        glNormal3f(0, 1, 0);
        glTexCoord2f(0, 0); glVertex3f(0,1,0);
        glTexCoord2f(1, 0); glVertex3f(1,1,0);
        glTexCoord2f(1, 1); glVertex3f(1,1,1);
        glTexCoord2f(0, 1); glVertex3f(0,1,1);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, dirtTexture);
        glBegin(GL_QUADS);
        glNormal3f(0, -1, 0);
        glTexCoord2f(0, 0); glVertex3f(0,0,0);
        glTexCoord2f(1, 0); glVertex3f(1,0,0);
        glTexCoord2f(1, 1); glVertex3f(1,0,1);
        glTexCoord2f(0, 1); glVertex3f(0,0,1);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, dirtTexture);
        glBegin(GL_QUADS);

        glNormal3f(0,0,1);
        glTexCoord2f(0,0); glVertex3f(0,0,1);
        glTexCoord2f(1,0); glVertex3f(1,0,1);
        glTexCoord2f(1,1); glVertex3f(1,1,1);
        glTexCoord2f(0,1); glVertex3f(0,1,1);

        glNormal3f(0,0,-1);
        glTexCoord2f(0,0); glVertex3f(0,0,0);
        glTexCoord2f(1,0); glVertex3f(1,0,0);
        glTexCoord2f(1,1); glVertex3f(1,1,0);
        glTexCoord2f(0,1); glVertex3f(0,1,0);

        glNormal3f(-1,0,0);
        glTexCoord2f(0,0); glVertex3f(0,0,0);
        glTexCoord2f(1,0); glVertex3f(0,0,1);
        glTexCoord2f(1,1); glVertex3f(0,1,1);
        glTexCoord2f(0,1); glVertex3f(0,1,0);

        glNormal3f(1,0,0);
        glTexCoord2f(0,0); glVertex3f(1,0,0);
        glTexCoord2f(1,0); glVertex3f(1,0,1);
        glTexCoord2f(1,1); glVertex3f(1,1,1);
        glTexCoord2f(0,1); glVertex3f(1,1,0);

        glEnd();

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }
}