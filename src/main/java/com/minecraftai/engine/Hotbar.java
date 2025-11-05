package com.minecraftai.engine;

import com.minecraftai.engine.ItemStack;
import com.minecraftai.engine.ItemType;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.*;

public class Hotbar {
    private int hotbarTextureID;
    private Player player;

    private static final float ASPECT_RATIO = 1330.0f / 150.0f;
    private static final float HOTBAR_WIDTH_RATIO = 0.5f;
    private static final float BOTTOM_MARGIN_PX = 10.0f;

    public Hotbar(Player player) {
        this.player = player;
    }

    public void init() {
        hotbarTextureID = TextureLoader.loadTexture("/assets/textures/misc/hotbar.png");
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
        float slotWidth = barWidth / 9.0f;
        float slotPadding = slotWidth * 0.20f;
        float itemSize = slotWidth - (slotPadding * 2);

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack == null) {
                continue;
            }

            ItemType type = stack.getType();
            int textureId = type.getTextureId();

            float itemX = barX + (i * slotWidth) + slotPadding;
            float itemY = barY + slotPadding;

            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            drawTexturedQuad(itemX, itemY, itemSize, itemSize);
            glDisable(GL_TEXTURE_2D);

            int count = stack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidth(countStr);

                float textX = (itemX + itemSize) - textWidth - 2;
                float textY = (itemY + itemSize) - 2;

                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawString(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawString(countStr, textX, textY);
            }
        }


        int selectedSlot = player.getSelectedSlot();
        float borderX = barX + (selectedSlot * slotWidth);

        glLineWidth(2.5f);
        glColor4f(1.0f, 0.0f, 0.0f, 1.0f);

        glBegin(GL_LINE_LOOP);
        glVertex2f(borderX, barY);
        glVertex2f(borderX + slotWidth, barY);
        glVertex2f(borderX + slotWidth, barY + barHeight);
        glVertex2f(borderX, barY + barHeight);
        glEnd();

        glLineWidth(1.0f);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDisable(GL_BLEND);

        restore3DRendering();
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