package com.minecraftai.gui;

import com.minecraftai.core.Player;
import com.minecraftai.core.ItemStack;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.TextureLoader;
import com.minecraftai.core.FontRenderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class InventoryMenu {
    private Player player;
    private int inventoryTextureID;
    private ItemStack cursorStack = null;

    private double mouseX, mouseY;

    public InventoryMenu(Player player) {
        this.player = player;
    }

    public void init() {
        inventoryTextureID = TextureLoader.loadTexture("/assets/textures/gui/inventory.png");
    }

    public void handleMouseMove(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void handleMouseClick(double mouseX, double mouseY, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            int clickedSlot = getClickedSlot(mouseX, mouseY);
            if (clickedSlot != -1) {
                ItemStack[] inventory = player.getInventory();
                ItemStack slotStack = inventory[clickedSlot];

                if (cursorStack == null) {
                    cursorStack = slotStack;
                    inventory[clickedSlot] = null;
                } else {
                    if (slotStack == null) {
                        inventory[clickedSlot] = cursorStack;
                        cursorStack = null;
                    } else if (slotStack.getType() == cursorStack.getType()) {
                        int remaining = slotStack.addAmount(cursorStack.getCount());
                        if (remaining == 0) {
                            cursorStack = null;
                        } else {
                            cursorStack.setCount(remaining);
                        }
                    } else {
                        inventory[clickedSlot] = cursorStack;
                        cursorStack = slotStack;
                    }
                }
            }
        }
    }

    public void onClose() {
        if (cursorStack != null) {
            boolean fit = player.addItemStack(cursorStack);
            if (!fit) {
                player.getWorld().spawnDroppedItem(player.getX(), player.getY() + 0.5f, player.getZ(), cursorStack.getType());
            }
            cursorStack = null;
        }
    }

    public void render(long windowHandle) {
        setup2DRendering(windowHandle);

        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        float currentW = w[0];
        float currentH = h[0];

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.0f, 0.0f, 0.0f, 0.4f);
        drawSolidQuad(0, 0, currentW, currentH);

        float guiScale = 3.0f;
        float invWidth = 176.0f * guiScale;
        float invHeight = 166.0f * guiScale;
        float invX = (currentW - invWidth) / 2.0f;
        float invY = (currentH - invHeight) / 2.0f;

        glEnable(GL_TEXTURE_2D);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, inventoryTextureID);

        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f); glVertex2f(invX, invY);
        glTexCoord2f(176.0f / 256.0f, 0.0f); glVertex2f(invX + invWidth, invY);
        glTexCoord2f(176.0f / 256.0f, 166.0f / 256.0f); glVertex2f(invX + invWidth, invY + invHeight);
        glTexCoord2f(0.0f, 166.0f / 256.0f); glVertex2f(invX, invY + invHeight);
        glEnd();

        glDisable(GL_TEXTURE_2D);

        float previewCenterX = invX + 51.0f * guiScale;
        float previewCenterY = invY + 54.0f * guiScale;
        player.renderPlayerModelGUI(previewCenterX, previewCenterY, 20.0f * guiScale, mouseX, mouseY, currentW, currentH);

        setup2DRendering(windowHandle);

        ItemStack[] inventory = player.getInventory();
        float slotSize = 18.0f * guiScale;
        float innerSize = 16.0f * guiScale;

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack == null) continue;

            float slotX = 0;
            float slotY = 0;

            if (i < 9) {
                slotX = invX + (8.0f + i * 18.0f) * guiScale;
                slotY = invY + 142.0f * guiScale;
            } else {
                int col = (i - 9) % 9;
                int row = (i - 9) / 9;
                slotX = invX + (8.0f + col * 18.0f) * guiScale;
                slotY = invY + (84.0f + row * 18.0f) * guiScale;
            }

            float itemX = slotX + 1.0f * guiScale;
            float itemY = slotY + 1.0f * guiScale;

            float centerX = itemX + innerSize / 2.0f;
            float centerY = itemY + innerSize / 2.0f;

            GuiRenderer.draw3DBlock(centerX, centerY, innerSize, stack.getType(), currentW, currentH);

            int count = stack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);

                float textX = (itemX + innerSize) - textWidth - 2;
                float textY = (itemY + innerSize) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        if (cursorStack != null) {
            float floatSize = 16.0f * guiScale;
            float centerX = (float) mouseX;
            float centerY = (float) mouseY;

            GuiRenderer.draw3DBlock(centerX, centerY, floatSize, cursorStack.getType(), currentW, currentH);

            int count = cursorStack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);

                float textX = centerX + (floatSize / 2.0f) - textWidth - 2;
                float textY = centerY + (floatSize / 2.0f) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        glDisable(GL_BLEND);
        restore3DRendering();
    }

    private int getClickedSlot(double mouseX, double mouseY) {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(glfwGetCurrentContext(), w, h);
        float currentW = w[0];
        float currentH = h[0];

        float guiScale = 3.0f;
        float invWidth = 176.0f * guiScale;
        float invHeight = 166.0f * guiScale;
        float invX = (currentW - invWidth) / 2.0f;
        float invY = (currentH - invHeight) / 2.0f;

        float slotSize = 18.0f * guiScale;

        for (int i = 0; i < 9; i++) {
            float slotX = invX + (8.0f + i * 18.0f) * guiScale;
            float slotY = invY + 142.0f * guiScale;

            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                return i;
            }
        }

        for (int i = 9; i < 36; i++) {
            int col = (i - 9) % 9;
            int row = (i - 9) / 9;
            float slotX = invX + (8.0f + col * 18.0f) * guiScale;
            float slotY = invY + (84.0f + row * 18.0f) * guiScale;

            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                return i;
            }
        }

        return -1;
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

    private void drawSolidQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
    }
}