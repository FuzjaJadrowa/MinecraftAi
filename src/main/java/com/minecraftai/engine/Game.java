package com.minecraftai.engine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {

    private long window;
    private Player player;
    private World world;

    public void run() {
        init();
        loop();

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Nie udało się zainicjalizować GLFW");

        window = glfwCreateWindow(800, 600, "Minecraft AI Clone", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Nie udało się stworzyć okna");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // vsync
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

        player = new Player();
        world = new World();
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            perspective(70.0f, 800f / 600f, 0.1f, 100.0f);

            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            lookAt(
                    player.getX(), player.getY() + 5, player.getZ() + 15,
                    8, 0, 8,
                    0, 1, 0
            );

            player.update(window);

            world.render();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void perspective(float fov, float aspect, float zNear, float zFar) {
        float ymax = zNear * (float) Math.tan(Math.toRadians(fov / 2));
        float xmax = ymax * aspect;
        glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
    }

    private void lookAt(
            float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ,
            float upX, float upY, float upZ) {

        float[] forward = {
                centerX - eyeX,
                centerY - eyeY,
                centerZ - eyeZ
        };
        float fLen = (float) Math.sqrt(forward[0]*forward[0] + forward[1]*forward[1] + forward[2]*forward[2]);
        forward[0] /= fLen; forward[1] /= fLen; forward[2] /= fLen;

        float[] up = { upX, upY, upZ };
        float upLen = (float) Math.sqrt(up[0]*up[0] + up[1]*up[1] + up[2]*up[2]);
        up[0] /= upLen; up[1] /= upLen; up[2] /= upLen;

        float[] side = {
                forward[1]*up[2] - forward[2]*up[1],
                forward[2]*up[0] - forward[0]*up[2],
                forward[0]*up[1] - forward[1]*up[0]
        };

        up[0] = side[1]*forward[2] - side[2]*forward[1];
        up[1] = side[2]*forward[0] - side[0]*forward[2];
        up[2] = side[0]*forward[1] - side[1]*forward[0];

        float[] m = {
                side[0],   up[0],   -forward[0],   0,
                side[1],   up[1],   -forward[1],   0,
                side[2],   up[2],   -forward[2],   0,
                0,         0,        0,            1
        };

        glMultMatrixf(m);
        glTranslatef(-eyeX, -eyeY, -eyeZ);
    }
}