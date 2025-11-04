package com.minecraftai.engine;

import com.minecraftai.blocks.GrassBlock;
import com.minecraftai.blocks.Stone;
import com.minecraftai.blocks.Dirt;
import com.minecraftai.blocks.Water;
import com.minecraftai.generator.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World {

    private List<Block> blocks = new ArrayList<>();
    private SimplexNoise noiseGen;

    private static final int WORLD_SIZE_XZ = 2;
    private static final int BASE_Y = 64;
    private static final int WATER_LEVEL = 64;
    private static final int MIN_Y = 0;
    private static final double TERRAIN_SCALE = 0.015;
    private static final double CAVE_SCALE = 0.04;

    public List<Block> getBlocks() {
        return blocks;
    }

    public World() {
        this.noiseGen = new SimplexNoise(new Random().nextInt());

        for (int x = -WORLD_SIZE_XZ / 2; x < WORLD_SIZE_XZ / 2; x++) {
            for (int z = -WORLD_SIZE_XZ / 2; z < WORLD_SIZE_XZ / 2; z++) {
                double terrainNoise = noiseGen.noise(x * TERRAIN_SCALE, z * TERRAIN_SCALE); // Wartość od -1 do 1
                int surfaceHeight;
                if (terrainNoise < 0) {
                    surfaceHeight = BASE_Y + (int) (terrainNoise * 5.0);
                } else {
                    surfaceHeight = BASE_Y + (int) (terrainNoise * 20.0);
                }

                for (int y = MIN_Y; y <= surfaceHeight; y++) {
                    if (y < BASE_Y - 5) {
                        double caveNoise = noiseGen.noise(x * CAVE_SCALE, y * CAVE_SCALE * 2.0, z * CAVE_SCALE);

                        if (caveNoise > 0.6) {
                            continue;
                        }
                    }

                    if (y == surfaceHeight) {
                        if (y >= WATER_LEVEL) {
                            blocks.add(new GrassBlock(x, y, z));
                        } else {
                            blocks.add(new Dirt(x, y, z));
                        }
                    } else if (y > surfaceHeight - 4) {
                        blocks.add(new Dirt(x, y, z));
                    } else {
                        blocks.add(new Stone(x, y, z));
                    }
                }

                if (surfaceHeight < WATER_LEVEL) {

                    for (int y_water = surfaceHeight + 1; y_water <= WATER_LEVEL; y_water++) {
                        blocks.add(new Water(x, y_water, z));
                    }
                }

            }
        }

        Tree treeGen = new Tree(this);
        treeGen.generateRandomTrees(40);
    }

    public void render() {
        for (Block b : blocks) {
            b.render();
        }
    }

    public void addBlock(int x, int y, int z, Block block) {
        blocks.add(block);
    }

    public void removeBlock(int x, int y, int z) {
        blocks.removeIf(b -> b.getX() == x && b.getY() == y && b.getZ() == z);
    }

    public int getHeightAt(int x, int z) {
        int maxY = -1;
        for (Block b : blocks) {
            if (b.getX() == x && b.getZ() == z) {
                if (!(b instanceof Water)) {
                    if (b.getY() > maxY) maxY = (int)b.getY();
                }
            }
        }
        return maxY;
    }

    private static class SimplexNoise {
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