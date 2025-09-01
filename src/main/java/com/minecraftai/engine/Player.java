package com.minecraftai.engine;

import static org.lwjgl.glfw.GLFW.*;

public class Player {

    private float x, y, z;
    private float yaw, pitch;
    private final float speed = 0.06f;
    private float velocityY = 0;
    private final float gravity = 0.002f;
    private final float jumpStrength = 0.08f;
    private final float eyeHeight = 1.7f;
    private long lastBlockBreakTime = 0;
    private final long blockBreakCooldown = 200_000_000L;

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

        velocityY -= gravity;
        if (!collides(x, y + velocityY, z)) {
            y += velocityY;
        } else {
            velocityY = 0;
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

    public Block getTargetBlock(World world, float maxDistance) {
        float eyeX = x;
        float eyeY = y + 1.7f;
        float eyeZ = z;

        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);

        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));

        float step = 0.1f;
        for (float t = 0; t <= maxDistance; t += step) {
            float checkX = eyeX + dirX * t;
            float checkY = eyeY + dirY * t;
            float checkZ = eyeZ + dirZ * t;

            for (Block b : world.getBlocks()) {
                if ((int)checkX == b.getX() && (int)checkY == b.getY() && (int)checkZ == b.getZ()) {
                    return b;
                }
            }
        }
        return null;
    }

    private boolean onGround() {
        return collides(x, y - 0.05f, z);
    }

    private boolean collides(float nextX, float nextY, float nextZ) {
        for (Block b : world.getBlocks()) {
            if (b.collidesWithPlayer(nextX, nextY, nextZ)) {
                return true;
            }
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

    public float[] getCameraLookAt() {
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);

        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));

        float eyeX = x;
        float eyeY = y + eyeHeight;
        float eyeZ = z;

        float lookX = eyeX + dirX;
        float lookY = eyeY + dirY;
        float lookZ = eyeZ + dirZ;

        return new float[]{eyeX, eyeY, eyeZ, lookX, lookY, lookZ};
    }

    public void tryBreakBlock(World world) {
        long now = System.nanoTime();
        if (now - lastBlockBreakTime < blockBreakCooldown) {
            return;
        }

        Block target = getTargetBlock(world, 3f);
        if (target != null) {
            world.removeBlock(target.getX(), target.getY(), target.getZ());
            lastBlockBreakTime = now;
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getYaw() { return yaw; }
}