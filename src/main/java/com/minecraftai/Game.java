package com.minecraftai;

import com.minecraftai.core.*;
import com.minecraftai.gui.*;
import com.minecraftai.renderer.*;
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
        PLAY_MENU,
        OPTIONS_MENU,
        PAUSE_MENU,
        IN_GAME,
        INVENTORY,
        DEATH
    }

    private long window;
    private Player player;
    private World world;
    private MainMenu mainMenu;
    private PlayMenu playMenu;
    private OptionsMenu optionsMenu;
    private PauseMenu pauseMenu;
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
    private float timeOfDay = 6000.0f;
    private int sunTextureID;
    private int moonTextureID;
    private int[] destroyStageTextureIDs = new int[10];

    public static boolean COMMANDS_ENABLED = false;
    public boolean isCommandConsoleOpen = false;
    private String commandInput = "";

    public static void main(String[] args) {
        Game game = new Game();
        game.run();
    }

    public void run() {
        init();
        loop();
        if (world != null && player != null && world.getWorldName() != null) {
            WorldSaveManager.saveWorld(world, player, world.getWorldName());
        }
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
        glfwSetCharCallback(window, this::charCallback);
        glfwSetScrollCallback(window, this::scrollCallback);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        TextureAtlas.init();
        sunTextureID = TextureLoader.loadTexture("/assets/textures/misc/sun.png");
        moonTextureID = TextureLoader.loadTexture("/assets/textures/misc/moon.png");
        for (int i = 0; i < 10; i++) {
            destroyStageTextureIDs[i] = TextureLoader.loadTexture("/assets/textures/block/destroy/destroy_stage_" + i + ".png");
        }

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
        WorldSaveManager.loadOptions();
        playMenu = new PlayMenu(this);
        optionsMenu = new OptionsMenu(this);
        pauseMenu = new PauseMenu(this);
        currentState = GameState.MAIN_MENU;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (currentState == GameState.IN_GAME && isCommandConsoleOpen) {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                if (key == GLFW_KEY_ESCAPE) {
                    isCommandConsoleOpen = false;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                } else if (key == GLFW_KEY_BACKSPACE) {
                    if (commandInput.length() > 0) {
                        commandInput = commandInput.substring(0, commandInput.length() - 1);
                    }
                } else if (key == GLFW_KEY_ENTER && action == GLFW_PRESS) {
                    executeCommand(commandInput);
                    isCommandConsoleOpen = false;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
            }
            return;
        }

        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            if (currentState == GameState.IN_GAME) {
                pauseGame();
            } else if (currentState == GameState.PAUSE_MENU) {
                resumeGame();
            } else if (currentState == GameState.OPTIONS_MENU) {
                optionsMenu.goBack();
            } else if (currentState == GameState.PLAY_MENU) {
                playMenu.goBack();
            } else if (currentState == GameState.INVENTORY) {
                inventoryMenu.onClose();
                currentState = GameState.IN_GAME;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }

        if (action == GLFW_PRESS) {
            if (currentState == GameState.IN_GAME) {
                if (COMMANDS_ENABLED && key == GLFW_KEY_SLASH) {
                    isCommandConsoleOpen = true;
                    commandInput = "";
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    return;
                }
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
            if (!isCommandConsoleOpen) {
                player.addRotation((float) dx, (float) dy);
            }
        } else if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.PLAY_MENU) {
            playMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.OPTIONS_MENU) {
            optionsMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.PAUSE_MENU) {
            pauseMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.INVENTORY) {
            inventoryMenu.handleMouseMove(xpos, ypos);
        } else if (currentState == GameState.DEATH) {
            deathMenu.handleMouseMove(xpos, ypos);
        }
    }

    private void mouseButtonCallback(long window, int button, int action, int mods) {
        if (currentState == GameState.MAIN_MENU) {
            mainMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.PLAY_MENU) {
            playMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.OPTIONS_MENU) {
            optionsMenu.handleMouseClick(lastX, lastY, button, action);
        } else if (currentState == GameState.PAUSE_MENU) {
            pauseMenu.handleMouseClick(lastX, lastY, button, action);
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

            frameCount++;
            fpsTimer += deltaTime;
            if (fpsTimer >= 1.0) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer -= 1.0;
            }

            if (currentState == GameState.IN_GAME || currentState == GameState.INVENTORY) {
                timeOfDay += deltaTime * 20.0f;
                if (timeOfDay >= 24000.0f) {
                    timeOfDay -= 24000.0f;
                }
            }

            if (currentState == GameState.IN_GAME) {
                accumulator += deltaTime;
                while (accumulator >= PHYSICS_STEP) {
                    player.handleInput(window, isCommandConsoleOpen);
                    player.update(window, PHYSICS_STEP, isCommandConsoleOpen);
                    world.updateDroppedItems((float) PHYSICS_STEP, player);
                    world.updateWater(PHYSICS_STEP);
                    accumulator -= PHYSICS_STEP;
                }
                if (player.isDead()) {
                    isCommandConsoleOpen = false;
                    currentState = GameState.DEATH;
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            }

            updateSkyAndLighting();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (currentState == GameState.IN_GAME) {
                renderGame((float) (accumulator / PHYSICS_STEP));
            } else if (currentState == GameState.INVENTORY) {
                renderGame(1.0f);
                inventoryMenu.render(window);
            } else if (currentState == GameState.DEATH) {
                renderGame(1.0f);
                deathMenu.render(window);
            } else if (currentState == GameState.PAUSE_MENU) {
                renderGame(1.0f);
                pauseMenu.render();
            } else if (currentState == GameState.OPTIONS_MENU) {
                if (optionsMenu.getPreviousState() == GameState.PAUSE_MENU) {
                    renderGame(1.0f);
                }
                optionsMenu.render();
            } else if (currentState == GameState.PLAY_MENU) {
                playMenu.render();
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
        float targetFov = (player != null && player.isSprinting()) ? 78.0f : 70.0f;
        float transitionFactor = Math.min(1.0f, 6.0f * lastDeltaTime);
        currentFov += (targetFov - currentFov) * transitionFactor;

        float farClip = Math.max(100.0f, World.RENDER_DISTANCE * 16.0f * 1.5f);
        perspective(currentFov, aspect, 0.1f, farClip);

        player.applyCameraTransform(alpha, cameraMode);

        float angle = (timeOfDay / 24000.0f) * 360.0f - 90.0f;
        float angleRad = (float) Math.toRadians(angle);
        float lx = (float) -Math.sin(angleRad);
        float ly = (float) Math.cos(angleRad);
        float lz = 0.0f;
        if (ly >= 0.0f) {
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{lx, ly, lz, 0.0f});
        } else {
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{-lx, -ly, -lz, 0.0f});
        }

        renderSkyEnvironment();
        world.render(player);
        player.renderPlayerModel(alpha, cameraMode);

        renderBreakingBlockOverlay();

        if (hotbar != null) {
            hotbar.render(window);
        }

        renderDebugInfo();

        renderChatMessages();

        renderCommandConsole();
    }


    private void renderBreakingBlockOverlay() {
        Block target = player.getTargetBlock(world, 4.5f);
        float progress = player.getBreakProgress();
        if (target == null) return;

        int bx = target.getX();
        int by = target.getY();
        int bz = target.getZ();
        float h = target.getBlockHeight();

        if (progress > 0.0f) {
            int stage = (int) (progress * 10.0f);
            if (stage > 9) stage = 9;
            if (stage < 0) stage = 0;

            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, destroyStageTextureIDs[stage]);

            glDisable(GL_LIGHTING);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ZERO, GL_SRC_COLOR);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz + 1.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz + 1.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz + 1.002f);

            glTexCoord2f(0.0f, 0.0f); glVertex3f(bx - 0.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(bx + 1.002f, by - 0.002f, bz - 0.002f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(bx + 1.002f, by + h + 0.002f, bz - 0.002f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(bx - 0.002f, by + h + 0.002f, bz - 0.002f);
            glEnd();

            glDisable(GL_TEXTURE_2D);
        }

        glDisable(GL_LIGHTING);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.0f, 0.0f, 0.0f, 0.4f);
        glLineWidth(2.0f);
        glBegin(GL_LINES);
        float minX = bx - 0.002f;
        float maxX = bx + 1.002f;
        float minY = by - 0.002f;
        float maxY = by + h + 0.002f;
        float minZ = bz - 0.002f;
        float maxZ = bz + 1.002f;

        glVertex3f(minX, minY, minZ); glVertex3f(maxX, minY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, minY, maxZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(minX, minY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, minY, minZ);

        glVertex3f(minX, maxY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(maxX, maxY, minZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(maxX, maxY, maxZ); glVertex3f(minX, maxY, maxZ);
        glVertex3f(minX, maxY, maxZ); glVertex3f(minX, maxY, minZ);

        glVertex3f(minX, minY, minZ); glVertex3f(minX, maxY, minZ);
        glVertex3f(maxX, minY, minZ); glVertex3f(maxX, maxY, minZ);
        glVertex3f(maxX, minY, maxZ); glVertex3f(maxX, maxY, maxZ);
        glVertex3f(minX, minY, maxZ); glVertex3f(minX, maxY, maxZ);
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

    public void setGameState(GameState state) {
        this.currentState = state;
        if (state == GameState.PLAY_MENU) {
            playMenu.refreshSlots();
        }
    }

    public void enterPlayMenu() {
        playMenu.refreshSlots();
        setGameState(GameState.PLAY_MENU);
    }

    public void enterOptionsMenu(GameState prev) {
        optionsMenu.setPreviousState(prev);
        setGameState(GameState.OPTIONS_MENU);
    }

    public void startNewOrLoadWorld(String worldName) {
        world = new World();
        world.setWorldName(worldName);
        player = new Player(world);

        if (WorldSaveManager.worldExists(worldName)) {
            WorldSaveManager.loadWorld(world, player, worldName);
        } else {
            player.respawn();
            WorldSaveManager.saveWorld(world, player, worldName);
        }

        hotbar = new Hotbar(player);
        hotbar.init();

        inventoryMenu = new InventoryMenu(player);
        inventoryMenu.init();

        deathMenu = new DeathMenu(this, player);
        deathMenu.init();

        resumeGame();
    }

    public void saveAndQuit() {
        if (world != null && player != null && world.getWorldName() != null) {
            WorldSaveManager.saveWorld(world, player, world.getWorldName());
        }
        world = null;
        player = null;
        hotbar = null;
        inventoryMenu = null;
        deathMenu = null;
        setGameState(GameState.MAIN_MENU);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    public void resumeGame() {
        currentState = GameState.IN_GAME;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetCursorPos(window, 1280 / 2.0, 720 / 2.0);
        lastX = 1280 / 2.0;
        lastY = 720 / 2.0;
    }

    public void pauseGame() {
        isCommandConsoleOpen = false;
        setGameState(GameState.PAUSE_MENU);
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

    private void updateSkyAndLighting() {
        if (player == null || world == null) return;

        float[] skyColor = getSkyColor(timeOfDay);
        glClearColor(skyColor[0], skyColor[1], skyColor[2], 0.0f);

        float angle = (timeOfDay / 24000.0f) * 360.0f - 90.0f;
        float angleRad = (float) Math.toRadians(angle);
        float lx = (float) -Math.sin(angleRad);
        float ly = (float) Math.cos(angleRad);
        float lz = 0.0f;

        float sunHeight = Math.max(0.0f, ly);
        float ambient = 0.05f + sunHeight * 0.35f;
        float diffuse = 0.15f + sunHeight * 0.65f;

        float lr, lg, lb;
        if (ly >= 0.0f) {
            lr = 1.0f - (1.0f - sunHeight) * 0.1f;
            lg = 0.95f - (1.0f - sunHeight) * 0.55f;
            lb = 0.8f - (1.0f - sunHeight) * 0.6f;
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{lx, ly, lz, 0.0f});
        } else {
            lr = 0.15f;
            lg = 0.2f;
            lb = 0.3f;
            glLightfv(GL_LIGHT0, GL_POSITION, new float[]{-lx, -ly, -lz, 0.0f});
        }

        float[] lightAmbient = {ambient, ambient, ambient, 1.0f};
        float[] lightDiffuse = {diffuse * lr, diffuse * lg, diffuse * lb, 1.0f};
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
    }

    private static float lerp(float start, float end, float f) {
        return start + f * (end - start);
    }

    private static float[] getSkyColor(float time) {
        float r, g, b;
        if (time >= 400 && time < 11600) {
            r = 0.5f; g = 0.7f; b = 1.0f;
        } else if (time >= 11600 && time < 12000) {
            float f = (time - 11600.0f) / 400.0f;
            r = lerp(0.5f, 0.9f, f);
            g = lerp(0.7f, 0.4f, f);
            b = lerp(1.0f, 0.2f, f);
        } else if (time >= 12000 && time < 12400) {
            float f = (time - 12000.0f) / 400.0f;
            r = lerp(0.9f, 0.02f, f);
            g = lerp(0.4f, 0.02f, f);
            b = lerp(0.2f, 0.05f, f);
        } else if (time >= 12400 && time < 23200) {
            r = 0.02f; g = 0.02f; b = 0.05f;
        } else if (time >= 23200 && time < 23600) {
            float f = (time - 23200.0f) / 400.0f;
            r = lerp(0.02f, 0.7f, f);
            g = lerp(0.02f, 0.4f, f);
            b = lerp(0.05f, 0.3f, f);
        } else if (time >= 23600 && time < 24000) {
            float f = (time - 23600.0f) / 400.0f;
            r = lerp(0.7f, 0.9f, f);
            g = lerp(0.4f, 0.6f, f);
            b = lerp(0.3f, 0.4f, f);
        } else {
            float f = time / 400.0f;
            r = lerp(0.9f, 0.5f, f);
            g = lerp(0.6f, 0.7f, f);
            b = lerp(0.4f, 1.0f, f);
        }
        return new float[]{r, g, b};
    }

    private void renderSkyEnvironment() {
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        glPushMatrix();
        glTranslatef(player.getX(), player.getY(), player.getZ());

        float skyAngle = (timeOfDay / 24000.0f) * 360.0f - 90.0f;
        glRotatef(skyAngle, 0.0f, 0.0f, 1.0f);

        glBindTexture(GL_TEXTURE_2D, sunTextureID);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBegin(GL_QUADS);
        float sunSize = 10.0f;
        float sunDist = 80.0f;
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-sunSize, sunDist, sunSize);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(sunSize, sunDist, sunSize);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(sunSize, sunDist, -sunSize);
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-sunSize, sunDist, -sunSize);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, moonTextureID);
        glBegin(GL_QUADS);
        float moonSize = 10.0f;
        float moonDist = -80.0f;
        glTexCoord2f(0.0f, 0.0f); glVertex3f(-moonSize, moonDist, -moonSize);
        glTexCoord2f(1.0f, 0.0f); glVertex3f(moonSize, moonDist, -moonSize);
        glTexCoord2f(1.0f, 1.0f); glVertex3f(moonSize, moonDist, moonSize);
        glTexCoord2f(0.0f, 1.0f); glVertex3f(-moonSize, moonDist, moonSize);
        glEnd();

        glPopMatrix();

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
    }

    private void charCallback(long window, int codepoint) {
        if (currentState == GameState.IN_GAME && isCommandConsoleOpen) {
            if (commandInput.length() < 100) {
                commandInput += (char) codepoint;
            }
        }
    }

    private void executeCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) return;

        String cmd = commandLine.trim();
        if (!cmd.startsWith("/")) return;

        String[] parts = cmd.substring(1).split("\\s+");
        if (parts.length == 0) return;

        String commandName = parts[0].toLowerCase();
        switch (commandName) {
            case "tp":
                if (parts.length >= 4) {
                    try {
                        float tx = Float.parseFloat(parts[1]);
                        float ty = Float.parseFloat(parts[2]);
                        float tz = Float.parseFloat(parts[3]);

                        float maxCoord = 2147483500.0f;
                        if (tx > maxCoord) tx = maxCoord;
                        if (tx < -maxCoord) tx = -maxCoord;
                        if (ty > 128.0f) ty = 128.0f;
                        if (ty < 0.0f) ty = 0.0f;
                        if (tz > maxCoord) tz = maxCoord;
                        if (tz < -maxCoord) tz = -maxCoord;

                        player.setX(tx);
                        player.setY(ty);
                        player.setZ(tz);
                        player.setVelocityY(0);
                        player.resetPrevPosition();
                    } catch (NumberFormatException e) {
                        addChatMessage("Invalid coordinate format in /tp command!");
                    }
                }
                break;
            case "speed":
                if (parts.length >= 2) {
                    String speedArg = parts[1].toLowerCase();
                    if (speedArg.equals("default")) {
                        player.setSpeed(0.07f);
                    } else {
                        try {
                            float val = Float.parseFloat(speedArg);
                            if (val < 0.01f) val = 0.01f;
                            if (val > 5.0f) val = 5.0f;
                            player.setSpeed(val);
                        } catch (NumberFormatException e) {
                            addChatMessage("Invalid speed value!");
                        }
                    }
                }
                break;
            case "god":
                if (parts.length >= 2) {
                    String arg = parts[1].toLowerCase();
                    if (arg.equals("on") || arg.equals("true") || arg.equals("1")) {
                        player.setGodMode(true);
                    } else if (arg.equals("off") || arg.equals("false") || arg.equals("0")) {
                        player.setGodMode(false);
                    } else {
                        addChatMessage("Usage: /god [on|off]");
                        break;
                    }
                } else {
                    player.setGodMode(!player.isGodMode());
                }
                if (player.isGodMode()) {
                    addChatMessage("God mode enabled!");
                } else {
                    addChatMessage("God mode disabled!");
                }
                break;
            default:
                addChatMessage("Unknown command: " + commandName);
                break;
        }
    }

    private void renderCommandConsole() {
        if (!isCommandConsoleOpen) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float currentW = width[0];
        float currentH = height[0];

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        float barY = currentH - 35;
        float barHeight = 24;
        float padding = 8;
        
        glColor4f(0.0f, 0.0f, 0.0f, 0.6f);
        glBegin(GL_QUADS);
        glVertex2f(padding, barY);
        glVertex2f(currentW - padding, barY);
        glVertex2f(currentW - padding, barY + barHeight);
        glVertex2f(padding, barY + barHeight);
        glEnd();

        glDisable(GL_BLEND);

        String cursor = (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
        String displayText = commandInput + cursor;

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        FontRenderer.drawString(displayText, padding + 6, barY + 17);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glEnable(GL_DEPTH_TEST);
    }

    private void scrollCallback(long window, double xoffset, double yoffset) {
        if (currentState == GameState.IN_GAME && !isCommandConsoleOpen) {
            int currentSlot = player.getSelectedSlot();
            if (yoffset < 0) {
                currentSlot = (currentSlot + 1) % 9;
            } else if (yoffset > 0) {
                currentSlot = (currentSlot - 1 + 9) % 9;
            }
            player.setSelectedSlot(currentSlot);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public long getWindowHandle() {
        return window;
    }

    private static class ChatMessage {
        String text;
        long timestamp;

        ChatMessage(String text) {
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final java.util.List<ChatMessage> chatMessages = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addChatMessage(String message) {
        chatMessages.add(new ChatMessage(message));
        if (chatMessages.size() > 50) {
            chatMessages.remove(0);
        }
    }

    private void renderChatMessages() {
        if (chatMessages.isEmpty()) return;

        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        float currentW = width[0];
        float currentH = height[0];

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        long now = System.currentTimeMillis();
        int maxVisible = 10;
        int startIndex = Math.max(0, chatMessages.size() - maxVisible);
        java.util.List<ChatMessage> visible = chatMessages.subList(startIndex, chatMessages.size());

        float startY = currentH - (isCommandConsoleOpen ? 60 : 35);
        float lineHeight = 20;

        for (int i = 0; i < visible.size(); i++) {
            ChatMessage msg = visible.get(i);
            long age = now - msg.timestamp;

            float alpha = 1.0f;
            if (!isCommandConsoleOpen) {
                if (age > 10000) continue;
                if (age > 8000) {
                    alpha = 1.0f - (age - 8000) / 2000.0f;
                }
            }

            float y = startY - (visible.size() - 1 - i) * lineHeight;

            float textWidth = FontRenderer.getStringWidth(msg.text);
            glColor4f(0.0f, 0.0f, 0.0f, 0.4f * alpha);
            glBegin(GL_QUADS);
            glVertex2f(8, y - 14);
            glVertex2f(8 + textWidth + 6, y - 14);
            glVertex2f(8 + textWidth + 6, y + 4);
            glVertex2f(8, y + 4);
            glEnd();

            if (msg.text.startsWith("Unknown command") || msg.text.startsWith("Invalid")) {
                glColor4f(1.0f, 0.3f, 0.3f, alpha);
            } else {
                glColor4f(1.0f, 1.0f, 1.0f, alpha);
            }
            FontRenderer.drawString(msg.text, 11, y);
        }

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glEnable(GL_DEPTH_TEST);
    }
}