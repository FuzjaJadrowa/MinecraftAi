package com.minecraftai.engine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private enum GameState {
        MAIN_MENU,
        IN_GAME
    }

    private long window;
    private Player player;
    private World world;
    private MainMenu mainMenu;
    private GameState currentState;
    private double lastX, lastY;

    public void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");
        window = glfwCreateWindow(1280, 720, "Minecraft AI", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Window creation failed");
        setWindowIcon(window, "assets/textures/misc/icon.png");
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);
        lastX = xpos[0];
        lastY = ypos[0];

        glfwSetKeyCallback(window, this::keyCallback);
        glfwSetCursorPosCallback(window, this::cursorPosCallback);
        glfwSetMouseButtonCallback(window, this::mouseButtonCallback);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        float[] lightAmbient = {0.2f, 0.2f, 0.2f, 1.0f};
        float[] lightDiffuse = {0.8f, 0.8f, 0.8f, 1.0f};
        float[] lightPosition = {0.0f, 10.0f, 10.0f, 1.0f};
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
        glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        glClearColor(0.5f, 0.7f, 1.0f, 0.0f);

        mainMenu = new MainMenu(this);
        FontRenderer.initFont();
        currentState = GameState.MAIN_MENU;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            if (currentState == GameState.IN_GAME) {
                pauseGame();
            } else if (currentState == GameState.MAIN_MENU && player != null) {
                resumeGame();
            }
        }
    }

    private void cursorPosCallback(long window, double xpos, double ypos) {
        double dx = xpos - lastX;
        double dy = ypos - lastY;
        lastX = xpos;
        lastY = ypos;

        if (currentState == GameState.IN_GAME) {
            player.addRotation((float) dx, (float) dy);
        } else if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseMove(xpos, ypos);
        }
    }

    private void mouseButtonCallback(long window, int button, int action, int mods) {
        if (currentState == GameState.IN_GAME) {
            player.handleInput(window);
        } else if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseClick(lastX, lastY, button, action);
        }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (currentState == GameState.IN_GAME) {
                renderGame();
            } else {
                renderMainMenu();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderGame() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float aspect = (float) width[0] / height[0];
        glViewport(0, 0, width[0], height[0]);
        perspective(70.0f, aspect, 0.1f, 100.0f);

        player.applyCameraTransform();
        player.handleInput(window);
        player.update(window);
        world.render(player);
        player.renderEntities();
    }

    private void renderMainMenu() {
        mainMenu.render();
    }

    public void startGame() {
        if (world == null) {
            world = new World();
            player = new Player(world);
        }
        resumeGame();
    }

    public void resumeGame() {
        currentState = GameState.IN_GAME;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetCursorPos(window, 1280 / 2.0, 720 / 2.0);
        lastX = 1280 / 2.0;
        lastY = 720 / 2.0;
    }

    public void pauseGame() {
        currentState = GameState.MAIN_MENU;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    public void quitGame() {
        glfwSetWindowShouldClose(window, true);
    }

    private void perspective(float fov, float aspect, float zNear, float zFar) {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fov / 2));
        float xmax = ymax * aspect;
        glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
    }

    public static void setWindowIcon(long window, String resourcePath) {
        try {
            InputStream stream = Game.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                System.err.println("Nie znaleziono zasobu: " + resourcePath);
                return;
            }
            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixelsRaw = new int[width * height];
            image.getRGB(0, 0, width, height, pixelsRaw, 0, width);
            ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixelsRaw[y * width + x];
                    pixels.put((byte) ((pixel >> 16) & 0xFF));
                    pixels.put((byte) ((pixel >> 8) & 0xFF));
                    pixels.put((byte) (pixel & 0xFF));
                    pixels.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            pixels.flip();
            GLFWImage.Buffer icon = GLFWImage.malloc(1);
            icon.width(width);
            icon.height(height);
            icon.pixels(pixels);
            glfwSetWindowIcon(window, icon);
            icon.free();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public long getWindowHandle() {
        return window;
    }
}