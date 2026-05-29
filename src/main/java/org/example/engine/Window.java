package org.example.engine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private int width;
    private int height;
    private String title;
    private long id;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void create() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Could not initialize GLFW");
        }

        id = glfwCreateWindow(width, height, title, NULL, NULL);

        if (id == NULL) {
            throw new RuntimeException("Could not create window");
        }

        glfwMakeContextCurrent(id);
        glfwSwapInterval(1);
        glfwShowWindow(id);

        GL.createCapabilities();
    }

    public void update() {
        glfwSwapBuffers(id);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(id);
    }

    public void close() {
        glfwSetWindowShouldClose(id, true);
    }

    public long getId() {
        return id;
    }
    public void setTitle(String title) {
        glfwSetWindowTitle(id, title);
    }

    public void destroy() {
        glfwDestroyWindow(id);
        glfwTerminate();
    }
}