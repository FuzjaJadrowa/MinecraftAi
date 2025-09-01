package com.minecraftai.entity;

import com.minecraftai.engine.ModelLoader;

import static org.lwjgl.opengl.GL11.*;

public class CopperGolem {
    private float x, y, z;
    private ModelLoader.Model model;

    public CopperGolem(float x, float y, float z, ModelLoader.Model model) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.model = model;
    }

    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);
        glScalef(0.0625f, 0.0625f, 0.0625f);
        glBindTexture(GL_TEXTURE_2D, model.textureId);
        glEnable(GL_TEXTURE_2D);

        for (ModelLoader.Cube c : model.cubes) {
            glBegin(GL_QUADS);
            // front
            glTexCoord2f(c.u0, c.v0); glVertex3f(c.x, c.y, c.z + c.depth);
            glTexCoord2f(c.u1, c.v0); glVertex3f(c.x + c.width, c.y, c.z + c.depth);
            glTexCoord2f(c.u1, c.v1); glVertex3f(c.x + c.width, c.y + c.height, c.z + c.depth);
            glTexCoord2f(c.u0, c.v1); glVertex3f(c.x, c.y + c.height, c.z + c.depth);
            glEnd();
        }

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public void setPosition(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }
}