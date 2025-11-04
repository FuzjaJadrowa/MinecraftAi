package com.minecraftai.engine;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class MainMenu {
    private static final String BACKGROUND_PATH = "/assets/textures/misc/background.png";
    private static final String LOGO_PATH = "/assets/textures/misc/logo.png";

    private Game game;
    private int backgroundTextureID;
    private int logoTextureID;
    private final float[] playButtonRect = { 1280 / 2 - 150, 350, 300, 60 };
    private final float[] quitButtonRect = { 1280 / 2 - 150, 450, 300, 60 };

    private boolean isPlayHovered = false;
    private boolean isQuitHovered = false;

    public MainMenu(Game game) {
        this.game = game;
        this.backgroundTextureID = TextureLoader.loadTexture(BACKGROUND_PATH);
        this.logoTextureID = TextureLoader.loadTexture(LOGO_PATH);
    }

    public void handleMouseMove(double x, double y) {
        isPlayHovered = isMouseOver(x, y, playButtonRect);
        isQuitHovered = isMouseOver(x, y, quitButtonRect);
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (isPlayHovered) {
                if (game.getPlayer() != null) {
                    game.resumeGame();
                } else {
                    game.startGame();
                }
            } else if (isQuitHovered) {
                game.quitGame();
            }
        }
    }

    public void render() {
        setup2DRendering();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), w, h);
        float currentW = w[0];
        float currentH = h[0];
        float logoWidth = 512;
        float logoHeight = 128;
        float logoX = (currentW - logoWidth) / 2;
        float logoY = currentH * 0.15f;
        float buttonWidth = 300;
        float buttonHeight = 60;
        float buttonX = (currentW - buttonWidth) / 2;
        float playY = currentH * 0.50f;
        float quitY = playY + buttonHeight + 20;

        playButtonRect[0] = buttonX; playButtonRect[1] = playY; playButtonRect[2] = buttonWidth; playButtonRect[3] = buttonHeight;
        quitButtonRect[0] = buttonX; quitButtonRect[1] = quitY; quitButtonRect[2] = buttonWidth; quitButtonRect[3] = buttonHeight;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, backgroundTextureID);
        drawTexturedQuad(0, 0, currentW, currentH);
        glBindTexture(GL_TEXTURE_2D, logoTextureID);
        drawTexturedQuad(logoX, logoY, logoWidth, logoHeight);
        glDisable(GL_TEXTURE_2D);

        if (isPlayHovered) {
            glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
        } else {
            glColor4f(0.8f, 0.8f, 0.8f, 0.3f);
        }
        drawSolidQuad(buttonX, playY, buttonWidth, buttonHeight);
        if (isQuitHovered) {
            glColor4f(1.0f, 0.5f, 0.5f, 0.6f);
        } else {
            glColor4f(0.8f, 0.2f, 0.2f, 0.3f);
        }
        drawSolidQuad(buttonX, quitY, buttonWidth, buttonHeight);
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
        float playTextX = buttonX + (buttonWidth / 2) - 40;
        float quitTextX = buttonX + (buttonWidth / 2) - 35;
        float textYOffset = (buttonHeight / 2) + (FontRenderer.FONT_HEIGHT / 2);

        FontRenderer.drawString("PLAY", playTextX, playY + textYOffset);
        FontRenderer.drawString("QUIT", quitTextX, quitY + textYOffset);

        glDisable(GL_BLEND);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        restore3DRendering();
    }

    private void setup2DRendering() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), width, height);

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

    private void drawSolidQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
    }

    private boolean isMouseOver(double mouseX, double mouseY, float[] rect) {
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] &&
                mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}