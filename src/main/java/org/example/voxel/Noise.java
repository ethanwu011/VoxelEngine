package org.example.voxel;

public class Noise {
    private final long seed;

    public Noise(long seed) { this.seed = seed; }

    private float hash(int x, int z) {
        long h = (long) x * 374761393L + (long) z * 668265263L + seed * 1274126177L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        return (h & 0xFFFFFFFL) / (float) 0xFFFFFFFL;
    }

    private float smooth(float t) { return t * t * (3 - 2 * t); }

    public float noise2D(float x, float z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        float fx = x - x0;
        float fz = z - z0;

        float a = hash(x0, z0);
        float b = hash(x0 + 1, z0);
        float c = hash(x0, z0 + 1);
        float d = hash(x0 + 1, z0 + 1);

        float sx = smooth(fx);
        float sz = smooth(fz);

        float ab = a + (b - a) * sx;
        float cd = c + (d - c) * sx;
        return ab + (cd - ab) * sz;
    }

    public float fbm(float x, float z, int octaves, float persistence) {
        float total = 0, frequency = 1, amplitude = 1, max = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        return total / max;
    }
}