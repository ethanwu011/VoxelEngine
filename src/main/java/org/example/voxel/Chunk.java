package org.example.voxel;

import org.example.graphics.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 64;
    public static final int SIZE_Z = 16;

    private final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];
    private final int chunkX, chunkZ;
    private final World world;

    private int vao = 0, vbo = 0, vertexCount = 0;
    private boolean dirty = true, uploaded = false;

    // Local UVs (0–1) per face vertex, same order as faceVerts.
    private static final float[][][] FACE_UVS = {
            {{0,0},{0,1},{1,1}, {1,1},{1,0},{0,0}}, // 0 top
            {{0,0},{1,0},{1,1}, {1,1},{0,1},{0,0}}, // 1 bottom
            {{0,1},{0,0},{1,0}, {1,0},{1,1},{0,1}}, // 2 +X
            {{1,1},{0,1},{0,0}, {0,0},{1,0},{1,1}}, // 3 -X
            {{1,1},{0,1},{0,0}, {0,0},{1,0},{1,1}}, // 4 +Z
            {{0,1},{0,0},{1,0}, {1,0},{1,1},{0,1}}  // 5 -Z
    };

    public Chunk(World world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }

    private int idx(int x, int y, int z) { return x + y * SIZE_X + z * SIZE_X * SIZE_Y; }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return Block.AIR;
        return blocks[idx(x, y, z)];
    }

    public void setBlockRaw(int x, int y, int z, byte b) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return;
        blocks[idx(x, y, z)] = b;
    }

    public void setBlock(int x, int y, int z, byte b) {
        setBlockRaw(x, y, z, b);
        dirty = true;
        if (x == 0)          world.markDirty(chunkX - 1, chunkZ);
        if (x == SIZE_X - 1) world.markDirty(chunkX + 1, chunkZ);
        if (z == 0)          world.markDirty(chunkX, chunkZ - 1);
        if (z == SIZE_Z - 1) world.markDirty(chunkX, chunkZ + 1);
    }

    public void markDirty() { dirty = true; }

    private byte neighborBlock(int x, int y, int z) {
        if (y < 0 || y >= SIZE_Y) return Block.AIR;
        if (x >= 0 && x < SIZE_X && z >= 0 && z < SIZE_Z) return getBlock(x, y, z);
        return world.getBlockGlobal(chunkX * SIZE_X + x, y, chunkZ * SIZE_Z + z);
    }

    public void buildMesh(TextureAtlas atlas) {
        if (!dirty) return;

        List<Float> verts = new ArrayList<>();
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    byte b = getBlock(x, y, z);
                    if (b == Block.AIR) continue;
                    boolean leaf = (b == Block.LEAVES);
                    // Leaves draw all faces (so you can see into the foliage); solids face-cull normally.
                    if (leaf || !Block.isOpaque(neighborBlock(x, y + 1, z))) addFace(verts, x, y, z, 0, b, atlas);
                    if (leaf || !Block.isOpaque(neighborBlock(x, y - 1, z))) addFace(verts, x, y, z, 1, b, atlas);
                    if (leaf || !Block.isOpaque(neighborBlock(x + 1, y, z))) addFace(verts, x, y, z, 2, b, atlas);
                    if (leaf || !Block.isOpaque(neighborBlock(x - 1, y, z))) addFace(verts, x, y, z, 3, b, atlas);
                    if (leaf || !Block.isOpaque(neighborBlock(x, y, z + 1))) addFace(verts, x, y, z, 4, b, atlas);
                    if (leaf || !Block.isOpaque(neighborBlock(x, y, z - 1))) addFace(verts, x, y, z, 5, b, atlas);
                }
            }
        }

        float[] arr = new float[verts.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = verts.get(i);

        if (vao == 0) {
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            int stride = 8 * Float.BYTES;
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
            glEnableVertexAttribArray(2);
            glBindVertexArray(0);
        }

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, arr, GL_STATIC_DRAW);

        vertexCount = arr.length / 8;
        dirty = false;
        uploaded = true;
    }

    private void addFace(List<Float> verts, int x, int y, int z, int face, byte block, TextureAtlas atlas) {
        String tex = Block.getTexture(block, face);
        float[] uv = atlas.getUV(tex);
        float[] tint = Block.getTint(block, face);

        float shade;
        switch (face) {
            case 0: shade = 1.00f; break;
            case 1: shade = 0.55f; break;
            case 2: case 3: shade = 0.75f; break;
            default: shade = 0.85f;
        }
        float r = tint[0] * shade, g = tint[1] * shade, b = tint[2] * shade;

        float[][] vp = faceVerts(face, x, y, z);
        float[][] localUV = FACE_UVS[face];
        for (int i = 0; i < 6; i++) {
            float u = uv[0] + localUV[i][0] * (uv[2] - uv[0]);
            float v = uv[1] + localUV[i][1] * (uv[3] - uv[1]);
            verts.add(vp[i][0]); verts.add(vp[i][1]); verts.add(vp[i][2]);
            verts.add(u);        verts.add(v);
            verts.add(r);        verts.add(g);        verts.add(b);
        }
    }

    private float[][] faceVerts(int face, int x, int y, int z) {
        float x0 = x, y0 = y, z0 = z, x1 = x + 1, y1 = y + 1, z1 = z + 1;
        switch (face) {
            case 0: return new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z1},{x1,y1,z0},{x0,y1,z0}};
            case 1: return new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x1,y0,z1},{x0,y0,z1},{x0,y0,z0}};
            case 2: return new float[][]{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y1,z1},{x1,y0,z1},{x1,y0,z0}};
            case 3: return new float[][]{{x0,y0,z0},{x0,y0,z1},{x0,y1,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}};
            case 4: return new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x1,y1,z1},{x0,y1,z1},{x0,y0,z1}};
            case 5: return new float[][]{{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y1,z0},{x1,y0,z0},{x0,y0,z0}};
            default: return new float[0][];
        }
    }

    public void render() {
        if (!uploaded || vertexCount == 0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
        }
    }
}