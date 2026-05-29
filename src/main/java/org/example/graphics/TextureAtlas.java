package org.example.graphics;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.stb.STBImage.*;

public class TextureAtlas {
    private final int textureId;
    private final int tileSize;
    private final int tilesPerRow;
    private final int atlasPixels;
    private final Map<String, Integer> tileIndex = new HashMap<>();

    public TextureAtlas(String baseDir, String[] textureNames) {
        int n = textureNames.length;
        tilesPerRow = (int) Math.ceil(Math.sqrt(n));

        ByteBuffer[] images = new ByteBuffer[n];
        int[] widths = new int[n];
        int[] heights = new int[n];
        int maxSize = 16;

        stbi_set_flip_vertically_on_load(false);

        for (int i = 0; i < n; i++) {
            IntBuffer w = BufferUtils.createIntBuffer(1);
            IntBuffer h = BufferUtils.createIntBuffer(1);
            IntBuffer c = BufferUtils.createIntBuffer(1);
            String path = baseDir + "/" + textureNames[i] + ".png";
            ByteBuffer img = stbi_load(path, w, h, c, 4);
            if (img == null) {
                System.err.println("Missing texture: " + path + " (" + stbi_failure_reason() + ")");
                images[i] = null;
                widths[i] = 16;
                heights[i] = 16;
            } else {
                images[i] = img;
                widths[i] = w.get(0);
                heights[i] = h.get(0);
                if (widths[i] > maxSize) maxSize = widths[i];
                if (heights[i] > maxSize) maxSize = heights[i];
            }
            tileIndex.put(textureNames[i], i);
        }

        tileSize = maxSize;
        atlasPixels = tilesPerRow * tileSize;
        ByteBuffer atlas = BufferUtils.createByteBuffer(atlasPixels * atlasPixels * 4);

        for (int i = 0; i < n; i++) {
            int tx = i % tilesPerRow;
            int ty = i / tilesPerRow;
            ByteBuffer src = images[i];
            int srcW = widths[i];
            int srcH = heights[i];

            for (int py = 0; py < tileSize; py++) {
                for (int px = 0; px < tileSize; px++) {
                    int dstX = tx * tileSize + px;
                    int dstY = ty * tileSize + py;
                    int dstOff = (dstY * atlasPixels + dstX) * 4;

                    if (src == null) {
                        boolean checker = ((px / 4) + (py / 4)) % 2 == 0;
                        atlas.put(dstOff,     (byte) (checker ? 0xFF : 0x00));
                        atlas.put(dstOff + 1, (byte) 0);
                        atlas.put(dstOff + 2, (byte) (checker ? 0xFF : 0x00));
                        atlas.put(dstOff + 3, (byte) 0xFF);
                    } else {
                        int sx = px * srcW / tileSize;
                        int sy = py * srcH / tileSize;
                        int srcOff = (sy * srcW + sx) * 4;
                        atlas.put(dstOff,     src.get(srcOff));
                        atlas.put(dstOff + 1, src.get(srcOff + 1));
                        atlas.put(dstOff + 2, src.get(srcOff + 2));
                        atlas.put(dstOff + 3, src.get(srcOff + 3));
                    }
                }
            }
            if (src != null) stbi_image_free(src);
        }

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasPixels, atlasPixels, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlas);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Returns [u0, v0, u1, v1] in 0–1 atlas coords. */
    public float[] getUV(String name) {
        Integer idx = tileIndex.get(name);
        if (idx == null) return new float[]{0, 0, 1, 1};
        int tx = idx % tilesPerRow;
        int ty = idx / tilesPerRow;
        float pixel = 1.0f / atlasPixels;
        float inset = pixel * 0.01f;
        float u0 = tx * tileSize * pixel + inset;
        float v0 = ty * tileSize * pixel + inset;
        float u1 = (tx + 1) * tileSize * pixel - inset;
        float v1 = (ty + 1) * tileSize * pixel - inset;
        return new float[]{u0, v0, u1, v1};
    }

    public void bind() { glBindTexture(GL_TEXTURE_2D, textureId); }
    public int getId() { return textureId; }
    public void delete() { glDeleteTextures(textureId); }
}