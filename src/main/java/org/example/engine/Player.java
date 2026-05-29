package org.example.engine;

import org.example.voxel.Block;
import org.example.voxel.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private final Vector3f position;
    private final Vector3f velocity = new Vector3f();

    private float yaw = -90.0f, pitch = 0.0f;
    private double lastMouseX = 0, lastMouseY = 0;
    private boolean firstMouse = true;
    private boolean onGround = false;

    private static final float WIDTH = 0.6f;
    private static final float HEIGHT = 1.8f;
    private static final float EYE_HEIGHT = 1.62f;
    private static final float GRAVITY = 28.0f;
    private static final float JUMP_VELOCITY = 9.0f;
    private static final float WALK_SPEED = 5.0f;

    public Player(float x, float y, float z) {
        position = new Vector3f(x, y, z);
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getEyePosition() { return new Vector3f(position.x, position.y + EYE_HEIGHT, position.z); }

    public void update(float deltaTime, World world) {
        Vector3f front = getFront();
        Vector3f flat = new Vector3f(front.x, 0, front.z);
        if (flat.lengthSquared() > 0) flat.normalize();
        Vector3f right = new Vector3f(flat).cross(new Vector3f(0, 1, 0)).normalize();

        Vector3f move = new Vector3f();
        if (Input.isKeyDown(GLFW_KEY_W)) move.add(flat);
        if (Input.isKeyDown(GLFW_KEY_S)) move.sub(flat);
        if (Input.isKeyDown(GLFW_KEY_D)) move.add(right);
        if (Input.isKeyDown(GLFW_KEY_A)) move.sub(right);
        if (move.lengthSquared() > 0) move.normalize();

        velocity.x = move.x * WALK_SPEED;
        velocity.z = move.z * WALK_SPEED;

        if (onGround && Input.isKeyDown(GLFW_KEY_SPACE)) velocity.y = JUMP_VELOCITY;

        velocity.y -= GRAVITY * deltaTime;
        if (velocity.y < -50) velocity.y = -50;

        onGround = false;
        moveAxis(0, velocity.x * deltaTime, world);
        moveAxis(2, velocity.z * deltaTime, world);
        moveAxis(1, velocity.y * deltaTime, world);
    }

    private void moveAxis(int axis, float delta, World world) {
        if (delta == 0) return;
        float[] pos = {position.x, position.y, position.z};
        pos[axis] += delta;

        float eps = 0.001f;
        float minX = pos[0] - WIDTH / 2 + eps;
        float maxX = pos[0] + WIDTH / 2 - eps;
        float minY = pos[1] + eps;
        float maxY = pos[1] + HEIGHT - eps;
        float minZ = pos[2] - WIDTH / 2 + eps;
        float maxZ = pos[2] + WIDTH / 2 - eps;

        if (collides(world, minX, maxX, minY, maxY, minZ, maxZ)) {
            if (axis == 1) {
                if (delta < 0) onGround = true;
                velocity.y = 0;
            }
            return;
        }
        if (axis == 0) position.x = pos[0];
        if (axis == 1) position.y = pos[1];
        if (axis == 2) position.z = pos[2];
    }

    private boolean collides(World world, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        int bx0 = (int) Math.floor(minX), bx1 = (int) Math.floor(maxX);
        int by0 = (int) Math.floor(minY), by1 = (int) Math.floor(maxY);
        int bz0 = (int) Math.floor(minZ), bz1 = (int) Math.floor(maxZ);
        for (int x = bx0; x <= bx1; x++)
            for (int y = by0; y <= by1; y++)
                for (int z = bz0; z <= bz1; z++)
                    if (Block.isSolid(world.getBlockGlobal(x, y, z))) return true;
        return false;
    }

    public void handleMouse(float xpos, float ypos) {
        if (firstMouse) {
            lastMouseX = xpos; lastMouseY = ypos;
            firstMouse = false;
        }
        float xo = (float) (xpos - lastMouseX);
        float yo = (float) (lastMouseY - ypos);
        lastMouseX = xpos; lastMouseY = ypos;
        float sens = 0.1f;
        yaw += xo * sens;
        pitch += yo * sens;
        if (pitch > 89) pitch = 89;
        if (pitch < -89) pitch = -89;
    }

    public Vector3f getFront() {
        Vector3f f = new Vector3f();
        f.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        f.y = (float) Math.sin(Math.toRadians(pitch));
        f.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        return f.normalize();
    }

    public Matrix4f getViewMatrix() {
        Vector3f eye = getEyePosition();
        return new Matrix4f().lookAt(eye, new Vector3f(eye).add(getFront()), new Vector3f(0, 1, 0));
    }
}