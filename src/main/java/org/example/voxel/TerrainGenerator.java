package org.example.voxel;

public class TerrainGenerator {
    private final Noise noise;

    public TerrainGenerator(long seed) {
        this.noise = new Noise(seed);
    }

    public void generate(Chunk chunk) {
        int cx = chunk.getChunkX();
        int cz = chunk.getChunkZ();

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int wx = cx * Chunk.SIZE_X + x;
                int wz = cz * Chunk.SIZE_Z + z;

                float height = noise.fbm(wx * 0.02f, wz * 0.02f, 4, 0.5f);
                int h = 16 + (int) (height * 16);
                if (h < 1) h = 1;
                if (h >= Chunk.SIZE_Y) h = Chunk.SIZE_Y - 1;

                for (int y = 0; y < h - 4; y++) chunk.setBlockRaw(x, y, z, Block.STONE);
                for (int y = Math.max(0, h - 4); y < h; y++) chunk.setBlockRaw(x, y, z, Block.DIRT);
                if (h > 0 && h < Chunk.SIZE_Y) chunk.setBlockRaw(x, h, z, Block.GRASS);
            }
        }

        // Trees (kept inside chunk borders to avoid cross-chunk pain)
        for (int x = 2; x < Chunk.SIZE_X - 2; x++) {
            for (int z = 2; z < Chunk.SIZE_Z - 2; z++) {
                int wx = cx * Chunk.SIZE_X + x;
                int wz = cz * Chunk.SIZE_Z + z;

                float t = noise.noise2D(wx * 7.3f + 137.1f, wz * 7.3f + 49.5f);
                if (t <= 0.96f) continue;

                int gy = -1;
                for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
                    if (chunk.getBlock(x, y, z) == Block.GRASS) { gy = y; break; }
                }
                if (gy < 0 || gy + 6 >= Chunk.SIZE_Y) continue;

                for (int i = 1; i <= 4; i++) chunk.setBlockRaw(x, gy + i, z, Block.WOOD);

                for (int lx = -2; lx <= 2; lx++) {
                    for (int lz = -2; lz <= 2; lz++) {
                        for (int ly = 3; ly <= 5; ly++) {
                            if (lx == 0 && lz == 0 && ly < 5) continue;
                            int bx = x + lx, by = gy + ly, bz = z + lz;
                            if (bx >= 0 && bx < Chunk.SIZE_X && bz >= 0 && bz < Chunk.SIZE_Z && by < Chunk.SIZE_Y) {
                                if (chunk.getBlock(bx, by, bz) == Block.AIR) {
                                    chunk.setBlockRaw(bx, by, bz, Block.LEAVES);
                                }
                            }
                        }
                    }
                }
                chunk.setBlockRaw(x, gy + 5, z, Block.LEAVES);
            }
        }
    }
}