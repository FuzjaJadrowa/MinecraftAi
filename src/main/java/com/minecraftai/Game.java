package com.minecraftai;

import com.minecraftai.core.*;
import com.minecraftai.gui.*;
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
    public enum GameState {
        MAIN_MENU,
        IN_GAME,
        INVENTORY,
        DEATH
    }

    private long window;
    private Player player;
    private World world;
    private MainMenu mainMenu;
    private Hotbar hotbar;
    private InventoryMenu inventoryMenu;
    private DeathMenu deathMenu;
    private GameState currentState;
    private double lastX, lastY;
    private float currentFov = 70.0f;
    private int cameraMode = 0;
    private float lastDeltaTime = 0.016f;
    private boolean showDebug = false;
    private int fps = 0;
    private int frameCount = 0;
    private double fpsTimer = 0.0;

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
            } else if (currentState == GameState.INVENTORY) {
                inventoryMenu.onClose();
                currentState = GameState.IN_GAME;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }

        if (action == GLFW_PRESS) {
            if (currentState == GameState.IN_GAME) {
                if (key == GLFW_KEY_F3) {
                    showDebug = !showDebug;
                }
                if (key == GLFW_KEY_F5) {
                    cameraMode = (cameraMode + 1) % 3;
                }
                if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
                    int slotIndex = key - GLFW_KEY_1;
                    player.setSelectedSlot(slotIndex);
                }
                if (key == GLFW_KEY_E) {
                    currentState = GameState.INVENTORY;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            } else if (currentState == GameState.INVENTORY) {
                if (key == GLFW_KEY_E) {
                    inventoryMenu.onClose();
                    currentState = GameState.IN_GAME;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
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
        } else if (currentState == GameState.INVENTORY) {
            inventoryMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.DEATH) {
            deathMenu.handleMouseMove(xpos, ypos);
        }
    }

    private void mouseButtonCallback(long window, int button, int action, int mods) {
        if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.INVENTORY) {
            inventoryMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.DEATH) {
            deathMenu.handleMouseClick(lastX, lastY, button, action);
        }
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double accumulator = 0.0;
        final double PHYSICS_STEP = 1.0 / 60.0;

        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            if (deltaTime > 0.1) deltaTime = 0.1;
            this.lastDeltaTime = (float) deltaTime;

            // Obliczanie FPS
            frameCount++;
            fpsTimer += deltaTime;
            if (fpsTimer >= 1.0) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer -= 1.0;
            }

            if (currentState == GameState.IN_GAME) {
                accumulator += deltaTime;
                while (accumulator >= PHYSICS_STEP) {
                    player.handleInput(window);
                    player.update(window, PHYSICS_STEP);
                    world.updateDroppedItems((float) PHYSICS_STEP, player);
                    accumulator -= PHYSICS_STEP;
                }
                if (player.isDead()) {
                    currentState = GameState.DEATH;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            }

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (currentState == GameState.IN_GAME) {
                renderGame((float) (accumulator / PHYSICS_STEP));
            } else if (currentState == GameState.INVENTORY) {
                renderGame(1.0f);
                inventoryMenu.render(window);
            } else if (currentState == GameState.DEATH) {
                renderGame(1.0f);
                deathMenu.render(window);
            } else {
                renderMainMenu();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderGame(float alpha) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float aspect = (float) width[0] / height[0];
        glViewport(0, 0, width[0], height[0]);
        // Płynny efekt zmiany FOV podczas sprintu (Minecraft sprint FOV effect) z delta time
        float targetFov = (player != null && player.isSprinting()) ? 78.0f : 70.0f;
        float transitionFactor = Math.min(1.0f, 6.0f * lastDeltaTime);
        currentFov += (targetFov - currentFov) * transitionFactor;

        // Dynamiczne dopasowanie zFar do Render Distance dla uniknięcia czarnej linii odcięcia terenu
        float farClip = Math.max(100.0f, World.RENDER_DISTANCE * 16.0f * 1.5f);
        perspective(currentFov, aspect, 0.1f, farClip);

        player.applyCameraTransform(alpha, cameraMode);
        world.render(player);
        player.renderPlayerModel(alpha, cameraMode);

        // Rysowanie ramki zaznaczenia i nakładki pękania bloku
        renderBreakingBlockOverlay();

        if (hotbar != null) {
            hotbar.render(window);
        }

        // Renderowanie ekranu debugowania F3
        renderDebugInfo();
    }

    private void renderBreakingBlockOverlay() {
        Block target = player.getCurrentTargetBlock();
        float progress = player.getBreakProgress();
        if (target == null) return;

        int bx = target.getX();
        int by = target.getY();
        int bz = target.getZ();
        float h = target.getBlockHeight();

        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (progress > 0.0f) {
            glColor4f(0.0f, 0.0f, 0.0f, progress * 0.7f);
            glBegin(GL_QUADS);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);

            glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);

            glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);

            glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glEnd();
        }

        glColor4f(0.0f, 0.0f, 0.0f, 0.4f);
        glLineWidth(2.0f);
        glBegin(GL_LINES);
        glVertex3f(bx, by, bz); glVertex3f(bx + 1, by, bz);
        glVertex3f(bx + 1, by, bz); glVertex3f(bx + 1, by, bz + 1);
        glVertex3f(bx + 1, by, bz + 1); glVertex3f(bx, by, bz + 1);
        glVertex3f(bx, by, bz + 1); glVertex3f(bx, by, bz);
        glVertex3f(bx, by + h, bz); glVertex3f(bx + 1, by + h, bz);
        glVertex3f(bx + 1, by + h, bz); glVertex3f(bx + 1, by + h, bz + 1);
        glVertex3f(bx + 1, by + h, bz + 1); glVertex3f(bx, by + h, bz + 1);
        glVertex3f(bx, by + h, bz + 1); glVertex3f(bx, by + h, bz);
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

            inventoryMenu = new InventoryMenu(player);
            inventoryMenu.init();

            deathMenu = new DeathMenu(this, player);
            deathMenu.init();
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

    private void renderDebugInfo() {
        if (!showDebug) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width[0], height[0], 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        String posText = "Block: " + px + " " + py + " " + pz;
        String fpsText = fps + " FPS";

        FontRenderer.drawStringRegular(posText, 10, 25);
        FontRenderer.drawStringRegular(fpsText, 10, 50);

        glEnable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    public Player getPlayer() {
        return player;
    }

    public long getWindowHandle() {
        return window;
    }
}