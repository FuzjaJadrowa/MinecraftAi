package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex3f;

public class Dirt extends Block {

    private static int texture;

    public Dirt(int x, int y, int z) {
        super(x, y, z);
        if (texture == 0) {
            texture = TextureLoader.loadTexture("/assets/textures/block/dirt.png");
        }
    }

    @Override
    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture);

        glBegin(GL_QUADS);

        glNormal3f(0,1,0);
        glTexCoord2f(0,0); glVertex3f(0,1,0);
        glTexCoord2f(1,0); glVertex3f(1,1,0);
        glTexCoord2f(1,1); glVertex3f(1,1,1);
        glTexCoord2f(0,1); glVertex3f(0,1,1);

        glNormal3f(0,-1,0);
        glTexCoord2f(0,0); glVertex3f(0,0,0);
        glTexCoord2f(1,0); glVertex3f(1,0,0);
        glTexCoord2f(1,1); glVertex3f(1,0,1);
        glTexCoord2f(0,1); glVertex3f(0,0,1);

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