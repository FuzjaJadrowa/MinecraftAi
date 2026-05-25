package com.minecraftai.core;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.*;

public class Hotbar {
    private int hotbarTextureID;
    private int selectionTextureID;
    private Player player;

    private static final float ASPECT_RATIO = 182.0f / 22.0f;
    private static final float HOTBAR_WIDTH_RATIO = 0.5f;
    private static final float BOTTOM_MARGIN_PX = 0.0f; // Attached to the bottom of the screen

    public Hotbar(Player player) {
        this.player = player;
    }

    public void init() {
        hotbarTextureID = TextureLoader.loadTexture("/assets/textures/gui/hotbar.png");
        selectionTextureID = TextureLoader.loadTexture("/assets/textures/gui/hotbar_selection.png");
    }

    public void render(long windowHandle) {
        setup2DRendering(windowHandle);

        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        float currentW = w[0];
        float currentH = h[0];

        float barWidth = currentW * HOTBAR_WIDTH_RATIO;
        float barHeight = barWidth / ASPECT_RATIO;
        float barX = (currentW - barWidth) / 2;
        float barY = currentH - barHeight - BOTTOM_MARGIN_PX;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glBindTexture(GL_TEXTURE_2D, hotbarTextureID);
        drawTexturedQuad(barX, barY, barWidth, barHeight);

        glDisable(GL_TEXTURE_2D);

        ItemStack[] inventory = player.getInventory();
        float scale = barWidth / 182.0f;
        float slotWidth = 20.0f * scale;
        float itemSize = 16.0f * scale;

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack == null) {
                continue;
            }

            ItemType type = stack.getType();

            float slotX = barX + (1.0f + i * 20.0f) * scale;
            float slotY = barY + 1.0f * scale;

            float itemX = slotX + 2.0f * scale;
            float itemY = slotY + 2.0f * scale;

            // Render block as a 3D cube inside the hotbar slot
            float centerX = itemX + itemSize / 2.0f;
            float centerY = itemY + itemSize / 2.0f;
            draw3DBlock(centerX, centerY, itemSize, type, currentW, currentH);

            int count = stack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);

                float textX = (itemX + itemSize) - textWidth - 2;
                float textY = (itemY + itemSize) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        int selectedSlot = player.getSelectedSlot();
        float selectedSlotX = barX + (1.0f + selectedSlot * 20.0f) * scale;

        float selX = selectedSlotX - 2.0f * scale;
        float selY = barY - 1.0f * scale;
        float selWidth = 24.0f * scale;
        float selHeight = 23.0f * scale;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND); // Ensure blending is enabled for selection frame borders
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, selectionTextureID);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedQuad(selX, selY, selWidth, selHeight);
        glDisable(GL_TEXTURE_2D);

        glDisable(GL_BLEND);
        restore3DRendering();
    }

    private void draw3DBlock(float centerX, float centerY, float size, ItemType type, float currentW, float currentH) {
        float r = size * 0.23f; // Scale factor to fit cube inside slot margins

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

        // TOP face (1.0f brightness)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.TOP)),
                 -r, -r, -r,
                  r, -r, -r,
                  r, -r,  r,
                 -r, -r,  r);

        // BOTTOM face (0.5f brightness)
        glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.BOTTOM)),
                 -r,  r, -r,
                  r,  r, -r,
                  r,  r,  r,
                 -r,  r,  r);

        // EAST face (0.6f brightness)
        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.EAST)),
                  r, -r, -r,
                  r, -r,  r,
                  r,  r,  r,
                  r,  r, -r);

        // WEST face (0.6f brightness)
        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.WEST)),
                 -r, -r, -r,
                 -r, -r,  r,
                 -r,  r,  r,
                 -r,  r, -r);

        // NORTH face (0.8f brightness)
        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.NORTH)),
                 -r, -r, -r,
                  r, -r, -r,
                  r,  r, -r,
                 -r,  r, -r);

        // SOUTH face (0.8f brightness)
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

    private void drawFace(float[] uv, float x0, float y0, float z0,
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

    private int getBlockTextureIndex(ItemType type, Block.Face face) {
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

    private void setup2DRendering(long windowHandle) {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(windowHandle, width, height);
        glViewport(0, 0, width[0], height[0]);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width[0], height[0], 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
    }

    private void restore3DRendering() {
        glEnable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawTexturedQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(x, y);
        glTexCoord2f(1, 0); glVertex2f(x + w, y);
        glTexCoord2f(1, 1); glVertex2f(x + w, y + h);
        glTexCoord2f(0, 1); glVertex2f(x, y + h);
        glEnd();
    }
}