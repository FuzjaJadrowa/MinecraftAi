package com.minecraftai.engine;

import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private float x, y, z;

    public Player() {
        this.x = 8;
        this.y = 2;
        this.z = 8;
    }

    public void update(long window) {
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) z -= 0.1f;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) z += 0.1f;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) x -= 0.1f;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) x += 0.1f;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
}