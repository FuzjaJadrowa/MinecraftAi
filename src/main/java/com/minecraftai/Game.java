package com.minecraftai;

import com.minecraftai.core.*;
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

    private Hotbar hotbar;

    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }

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

        // Initialize our optimized Texture Atlas
        TextureAtlas.init();

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

        if (currentState == GameState.IN_GAME && action == GLFW_PRESS) {
            if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                int slotIndex = key - GLFW_KEY_1;
                player.setSelectedSlot(slotIndex);
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
        if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseClick(lastX, lastY, button, action);
        }
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;
        final double PHYSICS_STEP = 1.0 / 60.0; // Aktualizacja fizyki z częstotliwością 60 Hz

        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            // Zabezpieczenie przed "spiral of death" w przypadku nagłego spadku klatek
            if (deltaTime > 0.1) deltaTime = 0.1;

            if (currentState == GameState.IN_GAME) {
                accumulator += deltaTime;
                while (accumulator >= PHYSICS_STEP) {
                    player.handleInput(window);
                    player.update(window, PHYSICS_STEP);
                    accumulator -= PHYSICS_STEP;
                }
            }

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
        world.render(player);
        player.renderEntities();

        // Rysowanie ramki zaznaczenia i nakładki pękania bloku
        renderBreakingBlockOverlay();

        if (hotbar != null) {
            hotbar.render(window);
        }
    }

    private void renderBreakingBlockOverlay() {
        Block target = player.getCurrentTargetBlock();
        float progress = player.getBreakProgress();
        if (target == null) return;

        int bx = target.getX();
        int by = target.getY();
        int bz = target.getZ();
        float h = target.getBlockHeight();

        // Renderowanie linii zaznaczenia i nakładki z-fighting
        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 1. Nakładka pękania (stopniowo ciemniejący czarny sześcian)
        if (progress > 0.0f) {
            glColor4f(0.0f, 0.0f, 0.0f, progress * 0.7f); // max 70% czerni
            glBegin(GL_QUADS);
            // GÓRA (TOP) z lekkim offsetem 0.002f dla uniknięcia z-fighting
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            // DÓŁ (BOTTOM)
            glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);

            // WSCHÓD (EAST)
            glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);

            // ZACHÓD (WEST)
            glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);

            // PÓŁNOC (NORTH)
            glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            // POŁUDNIE (SOUTH)
            glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glEnd();
        }

        // 2. Kontur zaznaczonego bloku (czarne cienkie linie)
        glColor4f(0.0f, 0.0f, 0.0f, 0.4f);
        glLineWidth(2.0f);
        glBegin(GL_LINES);
        // Dolny kwadrat
        glVertex3f(bx, by, bz); glVertex3f(bx + 1, by, bz);
        glVertex3f(bx + 1, by, bz); glVertex3f(bx + 1, by, bz + 1);
        glVertex3f(bx + 1, by, bz + 1); glVertex3f(bx, by, bz + 1);
        glVertex3f(bx, by, bz + 1); glVertex3f(bx, by, bz);
        // Górny kwadrat
        glVertex3f(bx, by + h, bz); glVertex3f(bx + 1, by + h, bz);
        glVertex3f(bx + 1, by + h, bz); glVertex3f(bx + 1, by + h, bz + 1);
        glVertex3f(bx + 1, by + h, bz + 1); glVertex3f(bx, by + h, bz + 1);
        glVertex3f(bx, by + h, bz + 1); glVertex3f(bx, by + h, bz);
        // Pionowe słupki
        glVertex3f(bx, by, bz); glVertex3f(bx, by + h, bz);
        glVertex3f(bx + 1, by, bz); glVertex3f(bx + 1, by + h, bz);
        glVertex3f(bx + 1, by, bz + 1); glVertex3f(bx + 1, by + h, bz + 1);
        glVertex3f(bx, by, bz + 1); glVertex3f(bx, by + h, bz + 1);
        glEnd();
        glLineWidth(1.0f);

        glDisable(GL_BLEND);
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderMainMenu() {
        mainMenu.render();
    }

    public void startGame() {
        if (world == null) {
            world = new World();
            player = new Player(world);

            hotbar = new Hotbar(player);
            hotbar.init();
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