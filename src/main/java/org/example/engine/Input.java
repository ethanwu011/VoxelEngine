package org.example.engine;

import static org.lwjgl.glfw.GLFW.*;

public class Input {
    private static long window;
    private static final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] keyJustPressed = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] mousePressed = new boolean[8];
    private static final boolean[] mouseJustPressed = new boolean[8];

    private static double scrollAccumulator = 0;
    private static double scrollY = 0;

    public static void init(long windowId) {
        window = windowId;
        glfwSetScrollCallback(window, (w, xoff, yoff) -> scrollAccumulator += yoff);
    }

    public static void update() {
        for (int k = GLFW_KEY_SPACE; k <= GLFW_KEY_LAST; k++) {
            boolean down = glfwGetKey(window, k) == GLFW_PRESS;
            keyJustPressed[k] = down && !keyPressed[k];
            keyPressed[k] = down;
        }
        for (int b = 0; b < 8; b++) {
            boolean down = glfwGetMouseButton(window, b) == GLFW_PRESS;
            mouseJustPressed[b] = down && !mousePressed[b];
            mousePressed[b] = down;
        }
        scrollY = scrollAccumulator;
        scrollAccumulator = 0;
    }

    public static boolean isKeyDown(int key) { return keyPressed[key]; }
    public static boolean isKeyJustPressed(int key) { return keyJustPressed[key]; }
    public static boolean isMouseDown(int b) { return mousePressed[b]; }
    public static boolean isMouseJustPressed(int b) { return mouseJustPressed[b]; }
    public static double getScrollY() { return scrollY; }
}