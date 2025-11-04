package com.minecraftai.engine;

import com.minecraftai.blocks.*; // Importujemy wszystkie typy bloków

import static org.lwjgl.opengl.GL11.*;

public class Chunk {
    public static final int CHUNK_SIZE_X = 8;
    public static final int CHUNK_SIZE_Y = 128;
    public static final int CHUNK_SIZE_Z = 8;

    private Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
    private int worldX, worldZ;

    public Chunk(int chunkX, int chunkZ, World world, World.SimplexNoise noiseGen) {
        this.worldX = chunkX;
        this.worldZ = chunkZ;

        int startX = chunkX * CHUNK_SIZE_X;
        int startZ = chunkZ * CHUNK_SIZE_Z;

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int z = 0; z < CHUNK_SIZE_Z; z++) {

                int globalX = startX + x;
                int globalZ = startZ + z;
                double terrainNoise = noiseGen.noise(globalX * World.TERRAIN_SCALE, globalZ * World.TERRAIN_SCALE);
                int surfaceHeight;
                if (terrainNoise < 0) {
                    surfaceHeight = World.BASE_Y + (int) (terrainNoise * 5.0);
                } else {
                    surfaceHeight = World.BASE_Y + (int) (terrainNoise * 20.0);
                }

                for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                    if (y > surfaceHeight) {

                        if (y <= World.WATER_LEVEL && surfaceHeight < World.WATER_LEVEL - 1) {
                            setBlock(x, y, z, new Water(globalX, y, globalZ));
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
                            setBlock(x, y, z, new GrassBlock(globalX, y, globalZ));
                        } else {
                            setBlock(x, y, z, new Dirt(globalX, y, globalZ));
                        }
                    } else if (y > surfaceHeight - 4) {
                        setBlock(x, y, z, new Dirt(globalX, y, globalZ));
                    } else {
                        setBlock(x, y, z, new Stone(globalX, y, globalZ));
                    }
                }
            }
        }
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return null;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return;
        }
        blocks[x][y][z] = block;
    }

    public void render(World world) {
        glEnable(GL_TEXTURE_2D);

        // TODO: Dalsza optymalizacja przez "Batch Rendering" (sortowanie tekstur)

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    Block currentBlock = blocks[x][y][z];
                    if (currentBlock == null || currentBlock.isTransparent()) {
                        continue;
                    }
                    renderBlockFaces(currentBlock, world);
                }
            }
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    Block currentBlock = blocks[x][y][z];
                    if (currentBlock == null || !currentBlock.isTransparent()) {
                        continue;
                    }
                    renderBlockFaces(currentBlock, world);
                }
            }
        }
        glDisable(GL_BLEND);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDisable(GL_TEXTURE_2D);
    }

    private void renderBlockFaces(Block currentBlock, World world) {
        int globalX = currentBlock.getX();
        int globalY = currentBlock.getY();
        int globalZ = currentBlock.getZ();

        glPushMatrix();
        glTranslatef(globalX, globalY, globalZ);

        float h = currentBlock.blockHeight;

        Block neighborUp = world.getBlockAt(globalX, globalY + 1, globalZ);
        if (shouldRenderFace(currentBlock, neighborUp)) {
            glBindTexture(GL_TEXTURE_2D, currentBlock.getTextureID(Block.Face.TOP));
            glBegin(GL_QUADS);
            glNormal3f(0, 1, 0);
            glTexCoord2f(0, 0); glVertex3f(0, h, 0);
            glTexCoord2f(1, 0); glVertex3f(1, h, 0);
            glTexCoord2f(1, 1); glVertex3f(1, h, 1);
            glTexCoord2f(0, 1); glVertex3f(0, h, 1);
            glEnd();
        }

        Block neighborDown = world.getBlockAt(globalX, globalY - 1, globalZ);
        if (shouldRenderFace(currentBlock, neighborDown)) {
            glBindTexture(GL_TEXTURE_2D, currentBlock.getTextureID(Block.Face.BOTTOM));
            glBegin(GL_QUADS);
            glNormal3f(0, -1, 0);
            glTexCoord2f(0, 0); glVertex3f(0, 0, 0);
            glTexCoord2f(1, 0); glVertex3f(1, 0, 0);
            glTexCoord2f(1, 1); glVertex3f(1, 0, 1);
            glTexCoord2f(0, 1); glVertex3f(0, 0, 1);
            glEnd();
        }

        Block neighborFront = world.getBlockAt(globalX, globalY, globalZ + 1);
        if (shouldRenderFace(currentBlock, neighborFront)) {
            glBindTexture(GL_TEXTURE_2D, currentBlock.getTextureID(Block.Face.NORTH));
            glBegin(GL_QUADS);
            glNormal3f(0, 0, 1);
            glTexCoord2f(0, 0); glVertex3f(0, 0, 1);
            glTexCoord2f(1, 0); glVertex3f(1, 0, 1);
            glTexCoord2f(1, 1); glVertex3f(1, h, 1);
            glTexCoord2f(0, 1); glVertex3f(0, h, 1);
            glEnd();
        }

        Block neighborBack = world.getBlockAt(globalX, globalY, globalZ - 1);
        if (shouldRenderFace(currentBlock, neighborBack)) {
            glBindTexture(GL_TEXTURE_2D, currentBlock.getTextureID(Block.Face.SOUTH));
            glBegin(GL_QUADS);
            glNormal3f(0, 0, -1);
            glTexCoord2f(0, 0); glVertex3f(0, 0, 0);
            glTexCoord2f(1, 0); glVertex3f(1, 0, 0);
            glTexCoord2f(1, 1); glVertex3f(1, h, 0);
            glTexCoord2f(0, 1); glVertex3f(0, h, 0);
            glEnd();
        }

        Block neighborLeft = world.getBlockAt(globalX - 1, globalY, globalZ);
        if (shouldRenderFace(currentBlock, neighborLeft)) {
            glBindTexture(GL_TEXTURE_2D, currentBlock.getTextureID(Block.Face.WEST));
            glBegin(GL_QUADS);
            glNormal3f(-1, 0, 0);
            glTexCoord2f(0, 0); glVertex3f(0, 0, 0);
            glTexCoord2f(1, 0); glVertex3f(0, 0, 1);
            glTexCoord2f(1, 1); glVertex3f(0, h, 1);
            glTexCoord2f(0, 1); glVertex3f(0, h, 0);
            glEnd();
        }

        Block neighborRight = world.getBlockAt(globalX + 1, globalY, globalZ);
        if (shouldRenderFace(currentBlock, neighborRight)) {
            glBindTexture(GL_TEXTURE_2D, currentBlock.getTextureID(Block.Face.EAST));
            glBegin(GL_QUADS);
            glNormal3f(1, 0, 0);
            glTexCoord2f(0, 0); glVertex3f(1, 0, 0);
            glTexCoord2f(1, 0); glVertex3f(1, 0, 1);
            glTexCoord2f(1, 1); glVertex3f(1, h, 1);
            glTexCoord2f(0, 1); glVertex3f(1, h, 0);
            glEnd();
        }

        glPopMatrix();
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
}