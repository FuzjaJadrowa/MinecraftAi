package com.minecraftai.blocks;

import com.minecraftai.engine.Block;
import com.minecraftai.engine.TextureLoader;

import static org.lwjgl.opengl.GL11.*;

public class Log extends Block {

    private static int logtopTexture;
    private static int logTexture;

    public Log(int x, int y, int z) {
        super(x, y, z);

        if (logtopTexture == 0) {
            logtopTexture = TextureLoader.loadTexture("/textures/block/log_top.png");
            logTexture = TextureLoader.loadTexture("/textures/block/log.png");
        }
    }

    @Override
    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);

        glEnable(GL_TEXTURE_2D);

        glBindTexture(GL_TEXTURE_2D, logtopTexture);
        glBegin(GL_QUADS);
        glNormal3f(0, 1, 0);
        glTexCoord2f(0, 0); glVertex3f(0,1,0);
        glTexCoord2f(1, 0); glVertex3f(1,1,0);
        glTexCoord2f(1, 1); glVertex3f(1,1,1);
        glTexCoord2f(0, 1); glVertex3f(0,1,1);
        glEnd();

        glNormal3f(0, -1, 0);
        glTexCoord2f(0, 0); glVertex3f(0,0,0);
        glTexCoord2f(1, 0); glVertex3f(1,0,0);
        glTexCoord2f(1, 1); glVertex3f(1,0,1);
        glTexCoord2f(0, 1); glVertex3f(0,0,1);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, logTexture);
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