package com.minecraftai.core;

import com.minecraftai.blocks.Cobblestone;
import com.minecraftai.blocks.Dirt;
import com.minecraftai.blocks.Log;
import com.minecraftai.entity.CopperGolem;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Player {
    private float x, y, z;
    private float prevX, prevY, prevZ; // Cache for physics interpolation
    private boolean isSprinting = false;
    private float yaw, pitch;
    private final float speed = 0.07f; // Minecraft walking speed scaled for 60 Hz updates
    private float velocityY = 0;
    private final float gravity = 0.008f; // Minecraft gravity scaled for 60 Hz updates
    private final float jumpStrength = 0.14f; // Minecraft jump velocity scaled for 60 Hz updates
    private final float eyeHeight = 1.7f;
    private long lastBlockPlaceTime = 0;
    private final long blockPlaceCooldown = 200_000_000L;
    private CopperGolem copperGolem;
    private World world;

    private int selectedSlot = 0;
    private ItemStack[] inventory = new ItemStack[9];

    // System niszczenia bloków (postęp, twardość, namierzanie)
    private int targetX, targetY, targetZ;
    private float breakProgress = 0.0f;
    private Block currentTargetBlock = null;

    public Player(World world) {
        this.world = world;
        this.x = 0;
        this.y = 80;
        this.z = 0;
        this.prevX = 0;
        this.prevY = 80;
        this.prevZ = 0;
        this.yaw = 0;
        this.pitch = 0;
    }

    public void handleInput(long window) {
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            tryPlaceBlock(world);
        }
        if (glfwGetKey(window, GLFW_KEY_G) == GLFW_PRESS && copperGolem == null) {
            float spawnX = x + 2;
            float spawnY = y;
            float spawnZ = z + 2;
            copperGolem = new CopperGolem(spawnX, spawnY, spawnZ);
        }
    }

    public float getRenderX(float alpha) {
        return prevX + (x - prevX) * alpha;
    }

    public float getRenderY(float alpha) {
        return prevY + (y - prevY) * alpha;
    }

    public float getRenderZ(float alpha) {
        return prevZ + (z - prevZ) * alpha;
    }

    public void applyCameraTransform(float alpha) {
        float[] cam = getCameraLookAt(alpha);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        lookAt(cam[0], cam[1], cam[2], cam[3], cam[4], cam[5], 0, 1, 0);
    }

    public void renderEntities() {
        if (copperGolem != null) copperGolem.render();
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

    public void update(long window, double dt) {
        prevX = x;
        prevY = y;
        prevZ = z;

        // 1. Obsługa niszczenia bloku (breakProgress)
        boolean isBreaking = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        Block target = getTargetBlock(world, 4.5f);

        if (isBreaking && target != null) {
            if (currentTargetBlock != null &&
                    target.getX() == targetX &&
                    target.getY() == targetY &&
                    target.getZ() == targetZ) {

                float speedVal = getBreakSpeed(target);
                breakProgress += dt * speedVal;

                if (breakProgress >= 1.0f) {
                    world.removeBlock(targetX, targetY, targetZ);
                    ItemType drop = target.getItemDrop();
                    if (drop != null) {
                        addItem(drop);
                    }
                    breakProgress = 0.0f;
                    currentTargetBlock = null;
                }
            } else {
                currentTargetBlock = target;
                targetX = target.getX();
                targetY = target.getY();
                targetZ = target.getZ();
                breakProgress = 0.0f;
            }
        } else {
            breakProgress = 0.0f;
            currentTargetBlock = null;
        }

        // 2. Obsługa poruszania się (fizyka, kierunki, slizganie)
        float dx = 0, dz = 0;
        float inputX = 0;
        float inputZ = 0;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) inputZ += 1;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) inputZ -= 1;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) inputX -= 1;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) inputX += 1;

        isSprinting = false;
        if (inputX != 0 || inputZ != 0) {
            isSprinting = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS && inputZ > 0;

            // Modyfikator prędkości zależny od kierunku (styl Minecraft)
            float speedMultiplier = 1.0f;
            if (inputZ < 0) {
                speedMultiplier = 0.6f; // W tył (60% prędkości)
            } else if (inputZ == 0 && inputX != 0) {
                speedMultiplier = 0.8f; // W bok (80% prędkości)
            } else if (inputZ > 0 && inputX != 0) {
                speedMultiplier = 0.9f; // Diagonalnie w przód (90% prędkości)
            }

            if (isSprinting) {
                speedMultiplier *= 1.3f; // Sprint w Minecraft
            }

            double radYaw = Math.toRadians(yaw);
            double sin = Math.sin(radYaw);
            double cos = Math.cos(radYaw);

            // Normalizacja wektora wejściowego ruchu i transformacja na świat
            float length = (float) Math.sqrt(inputX * inputX + inputZ * inputZ);
            float nx = inputX / length;
            float nz = inputZ / length;

            dx = (float) (nx * cos + nz * sin) * speed * speedMultiplier;
            dz = (float) (nx * sin - nz * cos) * speed * speedMultiplier;
        }

        // Ślizganie się po ścianach (niezależne kolizje X i Z)
        if (!collides(x + dx, y, z)) {
            x += dx;
        }
        if (!collides(x, y, z + dz)) {
            z += dz;
        }

        // Grawitacja i skok
        velocityY -= gravity;
        float nextY = y + velocityY;

        if (velocityY > 0 && collides(x, nextY, z)) {
            velocityY = 0;
        } else if (velocityY < 0 && collides(x, nextY, z)) {
            velocityY = 0;
            y = (float) Math.floor(nextY) + 1.0f;
        } else {
            y = nextY;
        }

        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround()) {
            velocityY = jumpStrength;
        }
    }

    private float getBreakSpeed(Block target) {
        if (!target.isDestructible()) return 0.0f;
        String name = target.getClass().getSimpleName();
        switch (name) {
            case "Leaves":
                return 5.0f; // 0.2 sekundy
            case "GrassBlock":
            case "Dirt":
                return 2.0f; // 0.5 sekundy
            case "Log":
                return 1.25f; // 0.8 sekundy
            case "Stone":
            case "Cobblestone":
                return 0.7f; // 1.4 sekundy
            default:
                return 1.0f; // 1.0 sekunda
        }
    }

    private boolean onGround() {
        return collides(x, y - 0.05f, z);
    }

    private boolean collides(float nextX, float nextY, float nextZ) {
        float playerWidth = 0.3f;
        float playerHeight = 1.7f;

        int minX = (int) Math.floor(nextX - playerWidth);
        int maxX = (int) Math.ceil(nextX + playerWidth);
        int minY = (int) Math.floor(nextY);
        int maxY = (int) Math.ceil(nextY + playerHeight);
        int minZ = (int) Math.floor(nextZ - playerWidth);
        int maxZ = (int) Math.ceil(nextZ + playerWidth);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    Block b = world.getBlockAt(x, y, z);

                    if (b != null) {
                        if (b.collidesWithPlayer(nextX, nextY, nextZ)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void addRotation(float dx, float dy) {
        float sensitivity = 0.1f;
        yaw += dx * sensitivity;
        pitch -= dy * sensitivity;
        final float MAX_PITCH = 89.9f;
        if (pitch > MAX_PITCH) pitch = MAX_PITCH;
        if (pitch < -MAX_PITCH) pitch = -MAX_PITCH;
    }

    public float[] getCameraLookAt(float alpha) {
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));
        float eyeX = getRenderX(alpha);
        float eyeY = getRenderY(alpha) + eyeHeight;
        float eyeZ = getRenderZ(alpha);
        float lookX = eyeX + dirX;
        float lookY = eyeY + dirY;
        float lookZ = eyeZ + dirZ;
        return new float[]{eyeX, eyeY, eyeZ, lookX, lookY, lookZ};
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public float getBreakProgress() {
        return breakProgress;
    }

    public Block getCurrentTargetBlock() {
        return currentTargetBlock;
    }

    public void addItem(ItemType itemType) {
        if (itemType == null) return;

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.getType() == itemType && !stack.isFull()) {
                stack.addAmount(1);
                return;
            }
        }

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) {
                inventory[i] = new ItemStack(itemType, 1);
                return;
            }
        }
    }

    public void tryPlaceBlock(World world) {
        long now = System.nanoTime();
        if (now - lastBlockPlaceTime < blockPlaceCooldown) return;

        ItemStack heldStack = inventory[selectedSlot];
        if (heldStack == null) {
            return;
        }

        float maxDistance = 4f;

        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));

        float eyeX = x;
        float eyeY = y + eyeHeight;
        float eyeZ = z;

        float step = 0.05f;

        int prevX = (int)Math.floor(eyeX);
        int prevY = (int)Math.floor(eyeY);
        int prevZ = (int)Math.floor(eyeZ);

        for (float t = 0; t <= maxDistance; t += step) {
            float currentRayX = eyeX + dirX * t;
            float currentRayY = eyeY + dirY * t;
            float currentRayZ = eyeZ + dirZ * t;

            int currentBlockX = (int)Math.floor(currentRayX);
            int currentBlockY = (int)Math.floor(currentRayY);
            int currentBlockZ = (int)Math.floor(currentRayZ);

            if (currentBlockX != prevX || currentBlockY != prevY || currentBlockZ != prevZ) {
                Block b = world.getBlockAt(currentBlockX, currentBlockY, currentBlockZ);

                if (b != null) {
                    Block newBlock = null;
                    switch (heldStack.getType()) {
                        case DIRT:
                            newBlock = new Dirt(prevX, prevY, prevZ);
                            break;
                        case COBBLESTONE:
                            newBlock = new Cobblestone(prevX, prevY, prevZ);
                            break;
                        case LOG:
                            newBlock = new Log(prevX, prevY, prevZ);
                            break;
                    }

                    if (newBlock == null || newBlock.collidesWithPlayer(this.x, this.y, this.z)) {
                        return;
                    }

                    world.addBlock(prevX, prevY, prevZ, newBlock);
                    lastBlockPlaceTime = now;

                    heldStack.setCount(heldStack.getCount() - 1);
                    if (heldStack.getCount() <= 0) {
                        inventory[selectedSlot] = null;
                    }

                    return;
                }

                prevX = currentBlockX;
                prevY = currentBlockY;
                prevZ = currentBlockZ;
            }
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
            Block b = world.getBlockAt((int)Math.floor(checkX), (int)Math.floor(checkY), (int)Math.floor(checkZ));
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            this.selectedSlot = slot;
        }
    }

    public ItemStack[] getInventory() {
        return inventory;
    }
}
