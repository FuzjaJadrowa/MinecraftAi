package com.minecraftai.engine;

import com.minecraftai.blocks.Cobblestone;
import com.minecraftai.entity.CopperGolem;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Game {

    private long window;
    private Player player;
    private World world;
    private CopperGolem copperGolem;
    private ModelLoader.Model copperModel;

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
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        final double[] lastX = {640};
        final double[] lastY = {360};
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            double dx = xpos - lastX[0];
            double dy = ypos - lastY[0];
            lastX[0] = xpos;
            lastY[0] = ypos;
            player.addRotation((float) dx, (float) dy);
        });
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();
        glClearColor(0.5f, 0.7f, 1.0f, 0.0f);
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
        world = new World();
        player = new Player(world);
        copperModel = ModelLoader.loadModel("models/copper_golem.json", "textures/entity/copper_golem.png");
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            int[] width = new int[1];
            int[] height = new int[1];
            glfwGetFramebufferSize(window, width, height);
            float aspect = (float) width[0] / height[0];
            perspective(70.0f, aspect, 0.1f, 100.0f);
            float[] cam = player.getCameraLookAt();
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            lookAt(cam[0], cam[1], cam[2], cam[3], cam[4], cam[5], 0, 1, 0);
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
                int placeX = (int)(player.getX() + Math.sin(Math.toRadians(player.getYaw())));
                int placeY = (int) player.getY();
                int placeZ = (int)(player.getZ() - Math.cos(Math.toRadians(player.getYaw())));
                world.addBlock(placeX, placeY, placeZ, new Cobblestone(placeX, placeY, placeZ));
            }
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                player.tryBreakBlock(world);
            }
            if (glfwGetKey(window, GLFW_KEY_G) == GLFW_PRESS && copperGolem == null) {
                float spawnX = player.getX() + 1;
                float spawnY = player.getY();
                float spawnZ = player.getZ();
                copperGolem = new CopperGolem(spawnX, spawnY, spawnZ, copperModel);
            }
            player.update(window);
            world.render();
            if (copperGolem != null) copperGolem.render();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void perspective(float fov, float aspect, float zNear, float zFar) {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fov / 2));
        float xmax = ymax * aspect;
        glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
    }

    private void lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        float[] f = {centerX - eyeX, centerY - eyeY, centerZ - eyeZ};
        float fLen = (float) Math.sqrt(f[0]*f[0] + f[1]*f[1] + f[2]*f[2]);
        f[0] /= fLen; f[1] /= fLen; f[2] /= fLen;
        float[] up = {upX, upY, upZ};
        float upLen = (float) Math.sqrt(up[0]*up[0] + up[1]*up[1] + up[2]*up[2]);
        up[0]/=upLen; up[1]/=upLen; up[2]/=upLen;
        float[] s = {f[1]*up[2]-f[2]*up[1], f[2]*up[0]-f[0]*up[2], f[0]*up[1]-f[1]*up[0]};
        float sLen = (float) Math.sqrt(s[0]*s[0]+s[1]*s[1]+s[2]*s[2]);
        s[0]/=sLen; s[1]/=sLen; s[2]/=sLen;
        float[] u = {s[1]*f[2]-s[2]*f[1], s[2]*f[0]-s[0]*f[2], s[0]*f[1]-s[1]*f[0]};
        float[] m = {s[0], u[0], -f[0],0, s[1], u[1], -f[1],0, s[2], u[2], -f[2],0, 0,0,0,1};
        glLoadMatrixf(m);
        glTranslatef(-eyeX, -eyeY, -eyeZ);
    }

    private void setWindowIcon(long window) {
        try {
            BufferedImage image = ImageIO.read(new File("icon.png"));
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

            icon.free();
            memFree(pixels);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}