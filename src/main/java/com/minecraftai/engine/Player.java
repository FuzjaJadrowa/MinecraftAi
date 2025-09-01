package com.minecraftai.engine;

import static org.lwjgl.glfw.GLFW.*;

public class Player {

    private float x, y, z;
    private float yaw, pitch;
    private final float speed = 0.1f;
    private float velocityY = 0;
    private final float gravity = 0.02f;
    private final float jumpStrength = 0.25f;

    private World world;

    public Player(World world) {
        this.world = world;
        this.x = 8;
        this.y = 2;
        this.z = 8;
        this.yaw = 0;
        this.pitch = 0;
    }

    public void update(long window) {
        float dx = 0, dz = 0;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            dx += (float) Math.sin(Math.toRadians(yaw)) * speed;
            dz -= (float) Math.cos(Math.toRadians(yaw)) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            dx -= (float) Math.sin(Math.toRadians(yaw)) * speed;
            dz += (float) Math.cos(Math.toRadians(yaw)) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            dx -= (float) Math.cos(Math.toRadians(yaw)) * speed;
            dz -= (float) Math.sin(Math.toRadians(yaw)) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            dx += (float) Math.cos(Math.toRadians(yaw)) * speed;
            dz += (float) Math.sin(Math.toRadians(yaw)) * speed;
        }

        if (glfwGetKey(window, GLFW_KEY_T) == GLFW_PRESS) {
            x = 8f;
            z = 8f;
            y = 2f;
            velocityY = 0;
        }

        if (!collides(x + dx, y, z + dz)) {
            x += dx;
            z += dz;
        }

        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround()) {
            velocityY = jumpStrength;
        }

        velocityY -= gravity;
        if (!collides(x, y + velocityY, z)) {
            y += velocityY;
        } else {
            if (velocityY < 0) {
                y = (float) Math.floor(y);
            } else {
                y = (float) Math.ceil(y) - 0.1f;
            }
            velocityY = 0;
        }
    }

    private boolean onGround() {
        return collides(x, y - 0.05f, z);
    }

    private boolean collides(float nextX, float nextY, float nextZ) {
        int blockX = (int) nextX;
        int blockZ = (int) nextZ;
        if (blockX >= 0 && blockX < 64 && blockZ >= 0 && blockZ < 64) {
            float blockTop = 1.0f;
            if (nextY < blockTop) return true;
        }
        return false;
    }

    public void addRotation(float dx, float dy) {
        float sensitivity = 0.1f;
        yaw += dx * sensitivity;
        pitch -= dy * sensitivity;

        if (pitch > 90) pitch = 90;
        if (pitch < -90) pitch = -90;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}