package com.minecraftai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;

public class World {
    private Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private PerlinNoise noiseGen;
    public static final int BASE_Y = 64;
    public static final int WATER_LEVEL = 64;
    public static final double TERRAIN_SCALE = 0.015;
    public static final double CAVE_SCALE = 0.04;

    public static int RENDER_DISTANCE = 4;

    public World() {
        this.noiseGen = new PerlinNoise(new Random().nextLong());
    }

    public Chunk getOrLoadChunk(int chunkX, int chunkZ) {
        String key = chunkX + "_" + chunkZ;

        Chunk chunk = chunks.computeIfAbsent(key, k -> {
            return new Chunk(chunkX, chunkZ);
        });

        if (!chunk.isGenerated()) {
            chunk.generate(this);
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

    /**
     * Generuje wielooktawowy szum Perlina (Fractional Brownian Motion)
     * dla urozmaiconego i naturalnego wyglądu terenu.
     */
    public double getTerrainNoise(double x, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 0.005; // Niska częstotliwość bazowa dla łagodnych wzgórz
        double maxValue = 0.0;

        // 4 oktawy szumu
        for (int i = 0; i < 4; i++) {
            total += noiseGen.noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= 0.55; // Persistence
            frequency *= 2.0;  // Lacunarity
        }

        return total / maxValue;
    }

    /**
     * Metoda pomocnicza dla szumu jaskiń.
     */
    public double getCaveNoise(double x, double y, double z) {
        return noiseGen.noise(x * CAVE_SCALE, y * CAVE_SCALE * 2.0, z * CAVE_SCALE);
    }
}
