package org.example.voxel;

public class Block {
    public static final byte AIR = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte SAND = 4;
    public static final byte WOOD = 5;
    public static final byte LEAVES = 6;
    public static final byte COBBLESTONE = 7;
    public static final byte PLANKS = 8;

    public static final String[] ATLAS_TEXTURES = {
            "grass_block_top", "grass_block_side", "dirt", "stone", "sand",
            "oak_log", "oak_log_top", "oak_leaves", "cobblestone", "oak_planks"
    };

    // face: 0=top(+Y), 1=bottom(-Y), 2=+X, 3=-X, 4=+Z, 5=-Z
    public static String getTexture(byte block, int face) {
        switch (block) {
            case GRASS:
                if (face == 0) return "grass_block_top";
                if (face == 1) return "dirt";
                return "grass_block_side";
            case DIRT:        return "dirt";
            case STONE:       return "stone";
            case SAND:        return "sand";
            case WOOD:
                if (face == 0 || face == 1) return "oak_log_top";
                return "oak_log";
            case LEAVES:      return "oak_leaves";
            case COBBLESTONE: return "cobblestone";
            case PLANKS:      return "oak_planks";
            default:          return "stone";
        }
    }

    /** Multiplied with the sampled texel so we can tint colormap textures. */
    public static float[] getTint(byte block, int face) {
        switch (block) {
            case GRASS:
                if (face == 0) return new float[]{0.50f, 0.85f, 0.35f};
                return new float[]{1, 1, 1};
            case LEAVES: return new float[]{0.40f, 0.75f, 0.30f};
            default:     return new float[]{1, 1, 1};
        }
    }

    public static boolean isSolid(byte b) { return b != AIR; }

    /** Leaves cull as non-opaque so we can see through them between adjacent leaf blocks. */
    public static boolean isOpaque(byte b) { return b != AIR && b != LEAVES; }
}