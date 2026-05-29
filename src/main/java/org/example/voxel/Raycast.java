package org.example.voxel;

import org.joml.Vector3f;

public class Raycast {
    public static class Hit {
        public int blockX, blockY, blockZ;
        public int faceX, faceY, faceZ;
        public byte block;
    }

    public static Hit cast(World world, Vector3f origin, Vector3f dir, float maxDist) {
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        float dx = dir.x, dy = dir.y, dz = dir.z;
        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;

        float tDeltaX = dx == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        float tDeltaY = dy == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        float tDeltaZ = dz == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);

        float tMaxX = dx == 0 ? Float.POSITIVE_INFINITY : ((dx > 0 ? (x + 1) : x) - origin.x) / dx;
        float tMaxY = dy == 0 ? Float.POSITIVE_INFINITY : ((dy > 0 ? (y + 1) : y) - origin.y) / dy;
        float tMaxZ = dz == 0 ? Float.POSITIVE_INFINITY : ((dz > 0 ? (z + 1) : z) - origin.z) / dz;

        int faceAxis = -1; // 0=x, 1=y, 2=z
        float t = 0;

        while (t < maxDist) {
            byte b = world.getBlockGlobal(x, y, z);
            if (Block.isSolid(b)) {
                Hit hit = new Hit();
                hit.blockX = x; hit.blockY = y; hit.blockZ = z; hit.block = b;
                if (faceAxis == 0) { hit.faceX = -stepX; }
                else if (faceAxis == 1) { hit.faceY = -stepY; }
                else if (faceAxis == 2) { hit.faceZ = -stepZ; }
                return hit;
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX; x += stepX; tMaxX += tDeltaX; faceAxis = 0;
            } else if (tMaxY < tMaxZ) {
                t = tMaxY; y += stepY; tMaxY += tDeltaY; faceAxis = 1;
            } else {
                t = tMaxZ; z += stepZ; tMaxZ += tDeltaZ; faceAxis = 2;
            }
        }
        return null;
    }
}