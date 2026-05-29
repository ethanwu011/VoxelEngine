package org.example.graphics;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private int id;
    private int width;
    private int height;

    public Texture(String path) {
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer channelsBuffer = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(false);

        ByteBuffer image = stbi_load(path, widthBuffer, heightBuffer, channelsBuffer, 4);

        if (image == null) {
            throw new RuntimeException("Failed to load texture: " + path + "\n" + stbi_failure_reason());
        }

        width = widthBuffer.get(0);
        height = heightBuffer.get(0);

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);

        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                image
        );

        stbi_image_free(image);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void delete() {
        glDeleteTextures(id);
    }

    public int getId() {
        return id;
    }
}