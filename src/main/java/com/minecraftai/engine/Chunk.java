package com.minecraftai.engine;

import com.minecraftai.blocks.*;
import com.minecraftai.generator.Tree;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

public class Chunk {

    public static final int CHUNK_SIZE_X = 8;
    public static final int CHUNK_SIZE_Y = 128;
    public static final int CHUNK_SIZE_Z = 8;

    private Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
    private int worldX, worldZ;
    private static final Random random = new Random();

    private int displayListIdOpaque = -1;
    private int displayListIdTransparent = -1;

    private boolean needsRebuild = true;

    public Chunk(int chunkX, int chunkZ, World world, World.SimplexNoise noiseGen) {
        this.worldX = chunkX;
        this.worldZ = chunkZ;

        int startX = chunkX * CHUNK_SIZE_X;
        int startZ = chunkZ * CHUNK_SIZE_Z;

        int[][] surfaceHeights = new int[CHUNK_SIZE_X][CHUNK_SIZE_Z];
        boolean[][] isGrass = new boolean[CHUNK_SIZE_X][CHUNK_SIZE_Z];

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                int globalX = startX + x;
                int globalZ = startZ + z;

                double terrainNoise = noiseGen.noise(globalX * World.TERRAIN_SCALE, globalZ * World.TERRAIN_SCALE);
                int surfaceHeight = (terrainNoise < 0) ?
                        (World.BASE_Y + (int) (terrainNoise * 4.0)) :
                        (World.BASE_Y + (int) (terrainNoise * 20.0));

                surfaceHeights[x][z] = surfaceHeight;

                for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                    if (y > surfaceHeight) {
                        if (y <= World.WATER_LEVEL && surfaceHeight < World.WATER_LEVEL - 1) {
                            setBlock(x, y, z, new Water(globalX, y, globalZ), false);
                        } else {
                            blocks[x][y][z] = null;
                        }
                        continue;
                    }

                    if (y < World.BASE_Y - 5) {
                        double caveNoise = noiseGen.noise(globalX * World.CAVE_SCALE, y * World.CAVE_SCALE * 2.0, globalZ * World.CAVE_SCALE);
                        if (caveNoise > 0.65) {
                            blocks[x][y][z] = null;
                            continue;
                        }
                    }

                    if (y == surfaceHeight) {
                        if (y >= World.WATER_LEVEL) {
                            setBlock(x, y, z, new GrassBlock(globalX, y, globalZ), false);
                            isGrass[x][z] = true;
                        } else {
                            setBlock(x, y, z, new Dirt(globalX, y, globalZ), false);
                        }
                    } else if (y > surfaceHeight - 4) {
                        setBlock(x, y, z, new Dirt(globalX, y, globalZ), false);
                    } else {
                        setBlock(x, y, z, new Stone(globalX, y, globalZ), false);
                    }
                }
            }
        }

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                if (isGrass[x][z] && random.nextInt(100) == 0) {
                    int globalX = startX + x;
                    int globalZ = startZ + z;
                    int y = surfaceHeights[x][z] + 1;

                    Tree.generateTree(this.blocks, x, y, z, globalX, globalZ);
                }
            }
        }

        this.needsRebuild = true;
    }

    public double getDistanceToPlayer(Player player) {
        double chunkCenterX = (worldX * CHUNK_SIZE_X) + (CHUNK_SIZE_X / 2.0);
        double chunkCenterZ = (worldZ * CHUNK_SIZE_Z) + (CHUNK_SIZE_Z / 2.0);

        double dx = chunkCenterX - player.getX();
        double dz = chunkCenterZ - player.getZ();

        return dx * dx + dz * dz;
    }

    private void checkAndRebuild(World world) {
        if (needsRebuild) {
            rebuildMeshes(world);
            needsRebuild = false;
        }
    }

    public void renderOpaque(World world) {
        checkAndRebuild(world);
        if (displayListIdOpaque > -1) {
            glCallList(displayListIdOpaque);
        }
    }

    public void renderTransparent(World world) {
        checkAndRebuild(world);
        if (displayListIdTransparent > -1) {
            glCallList(displayListIdTransparent);
        }
    }

    private void rebuildMeshes(World world) {
        if (displayListIdOpaque > -1) {
            glDeleteLists(displayListIdOpaque, 1);
        }
        if (displayListIdTransparent > -1) {
            glDeleteLists(displayListIdTransparent, 1);
        }

        displayListIdOpaque = glGenLists(1);
        glNewList(displayListIdOpaque, GL_COMPILE);

        glEnable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(worldX * CHUNK_SIZE_X, 0, worldZ * CHUNK_SIZE_Z);

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    Block currentBlock = blocks[x][y][z];
                    if (currentBlock != null && !currentBlock.isTransparent()) {
                        renderBlockFaces(currentBlock, x, y, z, world);
                    }
                }
            }
        }

        glPopMatrix();
        glDisable(GL_TEXTURE_2D);
        glEndList();

        displayListIdTransparent = glGenLists(1);
        glNewList(displayListIdTransparent, GL_COMPILE);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glPushMatrix();
        glTranslatef(worldX * CHUNK_SIZE_X, 0, worldZ * CHUNK_SIZE_Z);

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    Block currentBlock = blocks[x][y][z];
                    if (currentBlock != null && currentBlock.isTransparent()) {
                        currentBlock.setTransparent(0.7f);
                        renderBlockFaces(currentBlock, x, y, z, world);
                        currentBlock.setOpaque();
                    }
                }
            }
        }

        glPopMatrix();

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glEndList();
    }

    private void renderBlockFaces(Block current, int x, int y, int z, World world) {
        int globalX = worldX * CHUNK_SIZE_X + x;
        int globalZ = worldZ * CHUNK_SIZE_Z + z;
        float h = current.blockHeight;

        Block neighbor = (y + 1 >= CHUNK_SIZE_Y) ? null : blocks[x][y + 1][z];
        if (shouldRenderFace(current, neighbor)) {
            glBindTexture(GL_TEXTURE_2D, current.getTextureID(Block.Face.TOP));
            glBegin(GL_QUADS);
            glNormal3f(0, 1, 0);
            glTexCoord2f(0, 0); glVertex3f(x, y + h, z);
            glTexCoord2f(1, 0); glVertex3f(x + 1, y + h, z);
            glTexCoord2f(1, 1); glVertex3f(x + 1, y + h, z + 1);
            glTexCoord2f(0, 1); glVertex3f(x, y + h, z + 1);
            glEnd();
        }

        neighbor = (y - 1 < 0) ? null : blocks[x][y - 1][z];
        if (shouldRenderFace(current, neighbor)) {
            glBindTexture(GL_TEXTURE_2D, current.getTextureID(Block.Face.BOTTOM));
            glBegin(GL_QUADS);
            glNormal3f(0, -1, 0);
            glTexCoord2f(0, 0); glVertex3f(x, y, z);
            glTexCoord2f(1, 0); glVertex3f(x + 1, y, z);
            glTexCoord2f(1, 1); glVertex3f(x + 1, y, z + 1);
            glTexCoord2f(0, 1); glVertex3f(x, y, z + 1);
            glEnd();
        }

        neighbor = (x + 1 >= CHUNK_SIZE_X) ? world.getBlockAt(globalX + 1, y, globalZ) : blocks[x + 1][y][z];
        if (shouldRenderFace(current, neighbor)) {
            glBindTexture(GL_TEXTURE_2D, current.getTextureID(Block.Face.EAST));
            glBegin(GL_QUADS);
            glNormal3f(1, 0, 0);
            glTexCoord2f(0, 0); glVertex3f(x + 1, y, z);
            glTexCoord2f(1, 0); glVertex3f(x + 1, y, z + 1);
            glTexCoord2f(1, 1); glVertex3f(x + 1, y + h, z + 1);
            glTexCoord2f(0, 1); glVertex3f(x + 1, y + h, z);
            glEnd();
        }

        neighbor = (x - 1 < 0) ? world.getBlockAt(globalX - 1, y, globalZ) : blocks[x - 1][y][z];
        if (shouldRenderFace(current, neighbor)) {
            glBindTexture(GL_TEXTURE_2D, current.getTextureID(Block.Face.WEST));
            glBegin(GL_QUADS);
            glNormal3f(-1, 0, 0);
            glTexCoord2f(0, 0); glVertex3f(x, y, z);
            glTexCoord2f(1, 0); glVertex3f(x, y, z + 1);
            glTexCoord2f(1, 1); glVertex3f(x, y + h, z + 1);
            glTexCoord2f(0, 1); glVertex3f(x, y + h, z);
            glEnd();
        }

        neighbor = (z + 1 >= CHUNK_SIZE_Z) ? world.getBlockAt(globalX, y, globalZ + 1) : blocks[x][y][z + 1];
        if (shouldRenderFace(current, neighbor)) {
            glBindTexture(GL_TEXTURE_2D, current.getTextureID(Block.Face.NORTH));
            glBegin(GL_QUADS);
            glNormal3f(0, 0, 1);
            glTexCoord2f(0, 0); glVertex3f(x, y, z + 1);
            glTexCoord2f(1, 0); glVertex3f(x + 1, y, z + 1);
            glTexCoord2f(1, 1); glVertex3f(x + 1, y + h, z + 1);
            glTexCoord2f(0, 1); glVertex3f(x, y + h, z + 1);
            glEnd();
        }

        neighbor = (z - 1 < 0) ? world.getBlockAt(globalX, y, globalZ - 1) : blocks[x][y][z - 1];
        if (shouldRenderFace(current, neighbor)) {
            glBindTexture(GL_TEXTURE_2D, current.getTextureID(Block.Face.SOUTH));
            glBegin(GL_QUADS);
            glNormal3f(0, 0, -1);
            glTexCoord2f(0, 0); glVertex3f(x, y, z);
            glTexCoord2f(1, 0); glVertex3f(x + 1, y, z);
            glTexCoord2f(1, 1); glVertex3f(x + 1, y + h, z);
            glTexCoord2f(0, 1); glVertex3f(x, y + h, z);
            glEnd();
        }
    }

    private boolean shouldRenderFace(Block current, Block neighbor) {
        if (neighbor == null) {
            return true;
        }
        if (neighbor.isTransparent() && !current.isTransparent()) {
            return true;
        }
        if (current.isTransparent() && neighbor.isSolid()) {
            return true;
        }
        if (current.isTransparent() && neighbor.isTransparent()) {
            return false;
        }
        return !neighbor.isSolid();
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return null;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, Block block, boolean markDirty) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return;
        }
        blocks[x][y][z] = block;
        if (markDirty) {
            this.needsRebuild = true;
        }
    }

    public void markDirty() {
        this.needsRebuild = true;
    }
}