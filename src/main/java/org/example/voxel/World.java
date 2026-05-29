package org.example.voxel;

import org.example.graphics.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class World {
    private static final int RADIUS = 4;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final TerrainGenerator generator;
    private final TextureAtlas atlas;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public World(long seed, TextureAtlas atlas) {
        this.atlas = atlas;
        generator = new TerrainGenerator(seed);

        for (int cx = -RADIUS; cx < RADIUS; cx++) {
            for (int cz = -RADIUS; cz < RADIUS; cz++) {
                Chunk c = new Chunk(this, cx, cz);
                generator.generate(c);
                chunks.put(key(cx, cz), c);
            }
        }
        for (Chunk c : chunks.values()) c.buildMesh(atlas);
    }

    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }

    public Chunk getChunk(int cx, int cz) { return chunks.get(key(cx, cz)); }

    public byte getBlockGlobal(int x, int y, int z) {
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = getChunk(cx, cz);
        if (c == null) return Block.AIR;
        return c.getBlock(Math.floorMod(x, Chunk.SIZE_X), y, Math.floorMod(z, Chunk.SIZE_Z));
    }

    public void setBlockGlobal(int x, int y, int z, byte b) {
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = getChunk(cx, cz);
        if (c == null) return;
        c.setBlock(Math.floorMod(x, Chunk.SIZE_X), y, Math.floorMod(z, Chunk.SIZE_Z), b);
    }

    public void markDirty(int cx, int cz) {
        Chunk c = getChunk(cx, cz);
        if (c != null) c.markDirty();
    }

    public void update() {
        for (Chunk c : chunks.values()) c.buildMesh(atlas);
    }

    public void render(int modelLocation) {
        for (Chunk c : chunks.values()) {
            Matrix4f model = new Matrix4f().translation(
                    c.getChunkX() * Chunk.SIZE_X, 0, c.getChunkZ() * Chunk.SIZE_Z);
            matrixBuffer.clear();
            model.get(matrixBuffer);
            glUniformMatrix4fv(modelLocation, false, matrixBuffer);
            c.render();
        }
    }

    public void cleanup() { for (Chunk c : chunks.values()) c.cleanup(); }

    public int getGroundY(int x, int z) {
        for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
            if (Block.isSolid(getBlockGlobal(x, y, z))) return y;
        }
        return 0;
    }
}