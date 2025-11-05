package com.minecraftai.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;

public class World {
    private Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    public SimplexNoise noiseGen;
    public static final int BASE_Y = 64;
    public static final int WATER_LEVEL = 64;
    public static final double TERRAIN_SCALE = 0.015;
    public static final double CAVE_SCALE = 0.04;

    public static int RENDER_DISTANCE = 4;

    public World() {
        this.noiseGen = new SimplexNoise(new Random().nextInt(10000));
    }

    public Chunk getOrLoadChunk(int chunkX, int chunkZ) {
        String key = chunkX + "_" + chunkZ;

        Chunk chunk = chunks.computeIfAbsent(key, k -> {
            return new Chunk(chunkX, chunkZ);
        });

        if (!chunk.isGenerated()) {
            chunk.generate(this, noiseGen);
        }

        return chunk;
    }

    public void render(Player player) {
        int playerChunkX = (int) Math.floor(player.getX() / Chunk.CHUNK_SIZE_X);
        int playerChunkZ = (int) Math.floor(player.getZ() / Chunk.CHUNK_SIZE_Z);

        List<Chunk> visibleChunks = new ArrayList<>();
        for (int x = playerChunkX - RENDER_DISTANCE; x <= playerChunkX + RENDER_DISTANCE; x++) {
            for (int z = playerChunkZ - RENDER_DISTANCE; z <= playerChunkZ + RENDER_DISTANCE; z++) {
                visibleChunks.add(getOrLoadChunk(x, z));
            }
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);

        for (Chunk chunk : visibleChunks) {
            chunk.renderOpaque(this);
        }

        visibleChunks.sort((a, b) -> Double.compare(
                b.getDistanceToPlayer(player),
                a.getDistanceToPlayer(player)
        ));

        glEnable(GL_BLEND);
        glDepthMask(false);

        for (Chunk chunk : visibleChunks) {
            chunk.renderTransparent(this);
        }

        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    public Block getBlockAt(int globalX, int globalY, int globalZ) {
        if (globalY < 0 || globalY >= Chunk.CHUNK_SIZE_Y) {
            return null;
        }

        int chunkX = (int) Math.floor((double) globalX / Chunk.CHUNK_SIZE_X);
        int chunkZ = (int) Math.floor((double) globalZ / Chunk.CHUNK_SIZE_Z);

        Chunk chunk = getOrLoadChunk(chunkX, chunkZ);

        int localX = globalX % Chunk.CHUNK_SIZE_X;
        if (localX < 0) localX += Chunk.CHUNK_SIZE_X;

        int localZ = globalZ % Chunk.CHUNK_SIZE_Z;
        if (localZ < 0) localZ += Chunk.CHUNK_SIZE_Z;

        return chunk.getBlock(localX, globalY, localZ);
    }

    public void setBlockAt(int globalX, int globalY, int globalZ, Block block) {
        if (globalY < 0 || globalY >= Chunk.CHUNK_SIZE_Y) return;

        int chunkX = (int) Math.floor((double) globalX / Chunk.CHUNK_SIZE_X);
        int chunkZ = (int) Math.floor((double) globalZ / Chunk.CHUNK_SIZE_Z);

        Chunk chunk = getOrLoadChunk(chunkX, chunkZ);
        if (chunk == null) return;

        int localX = globalX % Chunk.CHUNK_SIZE_X;
        if (localX < 0) localX += Chunk.CHUNK_SIZE_X;

        int localZ = globalZ % Chunk.CHUNK_SIZE_Z;
        if (localZ < 0) localZ += Chunk.CHUNK_SIZE_Z;

        chunk.setBlock(localX, globalY, localZ, block, true);

        Chunk neighbor;
        if (localX == 0) {
            neighbor = chunks.get((chunkX - 1) + "_" + chunkZ);
            if (neighbor != null) neighbor.markDirty();
        } else if (localX == Chunk.CHUNK_SIZE_X - 1) {
            neighbor = chunks.get((chunkX + 1) + "_" + chunkZ);
            if (neighbor != null) neighbor.markDirty();
        }

        if (localZ == 0) {
            neighbor = chunks.get(chunkX + "_" + (chunkZ - 1));
            if (neighbor != null) neighbor.markDirty();
        } else if (localZ == Chunk.CHUNK_SIZE_Z - 1) {
            neighbor = chunks.get(chunkX + "_" + (chunkZ + 1));
            if (neighbor != null) neighbor.markDirty();
        }
    }

    public void addBlock(int x, int y, int z, Block block) {
        setBlockAt(x, y, z, block);
    }

    public void removeBlock(int x, int y, int z) {
        setBlockAt(x, y, z, null);
    }

    public static class SimplexNoise {
        private static final double STRETCH_CONSTANT_2D = -0.211324865405187;
        private static final double SQUISH_CONSTANT_2D = 0.366025403784439;
        private static final double STRETCH_CONSTANT_3D = -1.0 / 6.0;
        private static final double SQUISH_CONSTANT_3D = 1.0 / 3.0;

        private static final double NORM_CONSTANT_2D = 47.0;
        private static final double NORM_CONSTANT_3D = 103.0;

        private short[] perm;
        private short[] permGradIndex3D;

        private static byte[] gradients2D = new byte[]{
                5, 2, 2, 5,
                -5, 2, -2, 5,
                5, -2, 2, -5,
                -5, -2, -2, -5,
        };

        private static byte[] gradients3D = new byte[]{
                -11, 4, 4, -4, 11, 4, -4, 4, 11,
                11, 4, 4, 4, 11, 4, 4, 4, 11,
                -11, -4, 4, -4, -11, 4, -4, -4, 11,
                11, -4, 4, 4, -11, 4, 4, -4, 11,
                -11, 4, -4, -4, 11, -4, -4, 4, -11,
                11, 4, -4, 4, 11, -4, 4, 4, -11,
                -11, -4, -4, -4, -11, -4, -4, -4, -11,
                11, -4, -4, 4, -11, -4, 4, -4, -11,
        };

        public SimplexNoise(long seed) {
            perm = new short[256];
            permGradIndex3D = new short[256];

            short[] source = new short[256];
            for (short i = 0; i < 256; i++)
                source[i] = i;

            Random rand = new Random(seed);

            for (int i = 255; i >= 0; i--) {
                int r = rand.nextInt(i + 1);
                short temp = source[r];
                source[r] = source[i];
                perm[i] = temp;
                permGradIndex3D[i] = (short) ((perm[i] % (gradients3D.length / 3)) * 3);
            }
        }

        private static int fastFloor(double x) {
            int i = (int) x;
            return (x < i) ? (i - 1) : i;
        }

        private double extrapolate(int xsb, int ysb, double dx, double dy) {
            int index = perm[(perm[xsb & 0xFF] + ysb) & 0xFF] & 0x0E;
            return gradients2D[index] * dx + gradients2D[index + 1] * dy;
        }

        private double extrapolate(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
            int index = permGradIndex3D[(perm[(perm[xsb & 0xFF] + ysb) & 0xFF] + zsb) & 0xFF];
            return gradients3D[index] * dx + gradients3D[index + 1] * dy + gradients3D[index + 2] * dz;
        }

        public double noise(double x, double y) {
            double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
            double xs = x + stretchOffset;
            double ys = y + stretchOffset;

            int xsb = fastFloor(xs);
            int ysb = fastFloor(ys);

            double squishOffset = (xsb + ysb) * SQUISH_CONSTANT_2D;
            double xB = xsb + squishOffset;
            double yB = ysb + squishOffset;

            double xins = xs - xsb;
            double yins = ys - ysb;

            double inSum = xins + yins;

            double dx0 = x - xB;
            double dy0 = y - yB;

            double dx_ext, dy_ext;
            int xsv_ext, ysv_ext;

            double value = 0;

            double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
            }

            double attn1, attn2;
            if (xins > yins) {
                xsv_ext = xsb + 1;
                ysv_ext = ysb + 0;
                dx_ext = dx0 - 1 - SQUISH_CONSTANT_2D;
                dy_ext = dy0 - 0 - SQUISH_CONSTANT_2D;
            } else {
                xsv_ext = xsb + 0;
                ysv_ext = ysb + 1;
                dx_ext = dx0 - 0 - SQUISH_CONSTANT_2D;
                dy_ext = dy0 - 1 - SQUISH_CONSTANT_2D;
            }

            attn1 = 2 - dx_ext * dx_ext - dy_ext * dy_ext;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsv_ext, ysv_ext, dx_ext, dy_ext);
            }

            dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
            dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
            xsv_ext = xsb + 1;
            ysv_ext = ysb + 1;

            attn2 = 2 - dx_ext * dx_ext - dy_ext * dy_ext;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsv_ext, ysv_ext, dx_ext, dy_ext);
            }

            return value / NORM_CONSTANT_2D;
        }

        public double noise(double x, double y, double z) {
            double stretchOffset = (x + y + z) * STRETCH_CONSTANT_3D;
            double xs = x + stretchOffset;
            double ys = y + stretchOffset;
            double zs = z + stretchOffset;

            int xsb = fastFloor(xs);
            int ysb = fastFloor(ys);
            int zsb = fastFloor(zs);

            double squishOffset = (xsb + ysb + zsb) * SQUISH_CONSTANT_3D;
            double xB = xsb + squishOffset;
            double yB = ysb + squishOffset;
            double zB = zsb + squishOffset;

            double xins = xs - xsb;
            double yins = ys - ysb;
            double zins = zs - zsb;

            double inSum = xins + yins + zins;

            double dx0 = x - xB;
            double dy0 = y - yB;
            double dz0 = z - zB;

            double value = 0;
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb, ysb, zsb, dx0, dy0, dz0);
            }

            double attn_ext;
            double dx_ext, dy_ext, dz_ext;
            int xsv_ext, ysv_ext, zsv_ext;

            if (xins >= yins) {
                if (xins >= zins) {
                    xsv_ext = xsb + 1; ysv_ext = ysb + 0; zsv_ext = zsb + 0;
                    dx_ext = dx0 - 1 - SQUISH_CONSTANT_3D; dy_ext = dy0 - 0 - SQUISH_CONSTANT_3D; dz_ext = dz0 - 0 - SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext = xsb + 0; ysv_ext = ysb + 0; zsv_ext = zsb + 1;
                    dx_ext = dx0 - 0 - SQUISH_CONSTANT_3D; dy_ext = dy0 - 0 - SQUISH_CONSTANT_3D; dz_ext = dz0 - 1 - SQUISH_CONSTANT_3D;
                }
            } else {
                if (yins >= zins) {
                    xsv_ext = xsb + 0; ysv_ext = ysb + 1; zsv_ext = zsb + 0;
                    dx_ext = dx0 - 0 - SQUISH_CONSTANT_3D; dy_ext = dy0 - 1 - SQUISH_CONSTANT_3D; dz_ext = dz0 - 0 - SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext = xsb + 0; ysv_ext = ysb + 0; zsv_ext = zsb + 1;
                    dx_ext = dx0 - 0 - SQUISH_CONSTANT_3D; dy_ext = dy0 - 0 - SQUISH_CONSTANT_3D; dz_ext = dz0 - 1 - SQUISH_CONSTANT_3D;
                }
            }
            attn_ext = 2 - dx_ext * dx_ext - dy_ext * dy_ext - dz_ext * dz_ext;
            if (attn_ext > 0) {
                attn_ext *= attn_ext;
                value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, zsv_ext, dx_ext, dy_ext, dz_ext);
            }

            if (xins < yins) {
                if (xins < zins) {
                    xsv_ext = xsb + 1; ysv_ext = ysb + 1; zsv_ext = zsb + 0;
                    dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_3D; dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_3D; dz_ext = dz0 - 0 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext = xsb + 1; ysv_ext = ysb + 0; zsv_ext = zsb + 1;
                    dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_3D; dy_ext = dy0 - 0 - 2 * SQUISH_CONSTANT_3D; dz_ext = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
                }
            } else {
                if (yins < zins) {
                    xsv_ext = xsb + 1; ysv_ext = ysb + 1; zsv_ext = zsb + 0;
                    dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_3D; dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_3D; dz_ext = dz0 - 0 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext = xsb + 0; ysv_ext = ysb + 1; zsv_ext = zsb + 1;
                    dx_ext = dx0 - 0 - 2 * SQUISH_CONSTANT_3D; dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_3D; dz_ext = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
                }
            }
            attn_ext = 2 - dx_ext * dx_ext - dy_ext * dy_ext - dz_ext * dz_ext;
            if (attn_ext > 0) {
                attn_ext *= attn_ext;
                value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, zsv_ext, dx_ext, dy_ext, dz_ext);
            }

            dx_ext = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
            dy_ext = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
            dz_ext = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
            xsv_ext = xsb + 1;
            ysv_ext = ysb + 1;
            zsv_ext = zsb + 1;
            attn_ext = 2 - dx_ext * dx_ext - dy_ext * dy_ext - dz_ext * dz_ext;
            if (attn_ext > 0) {
                attn_ext *= attn_ext;
                value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, zsv_ext, dx_ext, dy_ext, dz_ext);
            }

            return value / NORM_CONSTANT_3D;
        }
    }
}