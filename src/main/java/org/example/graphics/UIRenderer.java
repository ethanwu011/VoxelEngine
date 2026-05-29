package org.example.graphics;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UIRenderer {
    private int vao, vbo, shaderProgram;
    private int projectionLocation, textureLocation, tintLocation, useTextureLocation;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final int screenWidth, screenHeight;

    public UIRenderer(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        createQuad();
        shaderProgram = createShaderProgram();
        projectionLocation = glGetUniformLocation(shaderProgram, "projection");
        textureLocation = glGetUniformLocation(shaderProgram, "uiTexture");
        tintLocation = glGetUniformLocation(shaderProgram, "tintColor");
        useTextureLocation = glGetUniformLocation(shaderProgram, "useTexture");
    }

    private void createQuad() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void begin() {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(shaderProgram);
        Matrix4f projection = new Matrix4f().ortho2D(0, screenWidth, screenHeight, 0);
        matrixBuffer.clear();
        projection.get(matrixBuffer);
        glUniformMatrix4fv(projectionLocation, false, matrixBuffer);
        glUniform1i(textureLocation, 0);
    }

    public void draw(Texture texture, float x, float y, float width, float height) {
        glUniform1i(useTextureLocation, 1);
        glUniform4f(tintLocation, 1, 1, 1, 1);
        glActiveTexture(GL_TEXTURE0);
        texture.bind();
        drawQuad(x, y, width, height);
    }

    public void drawSolid(float x, float y, float w, float h, float r, float g, float b, float a) {
        glUniform1i(useTextureLocation, 0);
        glUniform4f(tintLocation, r, g, b, a);
        drawQuad(x, y, w, h);
    }

    public void drawTexturedQuad(int textureId,
                                 float x0, float y0, float u0, float v0,
                                 float x1, float y1, float u1, float v1,
                                 float x2, float y2, float u2, float v2,
                                 float x3, float y3, float u3, float v3,
                                 float r, float g, float b, float a) {
        glUniform1i(useTextureLocation, 1);
        glUniform4f(tintLocation, r, g, b, a);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);

        float[] verts = {
                x0, y0, u0, v0,
                x1, y1, u1, v1,
                x2, y2, u2, v2,
                x2, y2, u2, v2,
                x3, y3, u3, v3,
                x0, y0, u0, v0
        };
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verts);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    private void drawQuad(float x, float y, float w, float h) {
        float x1 = x, y1 = y, x2 = x + w, y2 = y + h;
        float[] verts = {
                x1, y1, 0, 0,  x2, y1, 1, 0,  x2, y2, 1, 1,
                x2, y2, 1, 1,  x1, y2, 0, 1,  x1, y1, 0, 0
        };
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verts);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }


    public void end() {
        glUseProgram(0);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private int createShaderProgram() {
        String vs = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec2 aUV;
            uniform mat4 projection;
            out vec2 texCoord;
            void main() {
                texCoord = aUV;
                gl_Position = projection * vec4(aPos, 0.0, 1.0);
            }
            """;
        String fs = """
            #version 330 core
            in vec2 texCoord;
            uniform sampler2D uiTexture;
            uniform vec4 tintColor;
            uniform int useTexture;
            out vec4 FragColor;
            void main() {
                if (useTexture == 1) {
                    FragColor = texture(uiTexture, texCoord) * tintColor;
                } else {
                    FragColor = tintColor;
                }
            }
            """;

        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs); glCompileShader(v);
        if (glGetShaderi(v, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("UI vs failed:\n" + glGetShaderInfoLog(v));

        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs); glCompileShader(f);
        if (glGetShaderi(f, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("UI fs failed:\n" + glGetShaderInfoLog(f));

        int p = glCreateProgram();
        glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("UI link failed:\n" + glGetProgramInfoLog(p));

        glDeleteShader(v); glDeleteShader(f);
        return p;
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteProgram(shaderProgram);
    }
}