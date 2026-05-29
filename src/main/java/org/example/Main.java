package org.example;


import org.example.engine.Hotbar;
import org.example.engine.Input;
import org.example.engine.Player;
import org.example.engine.Window;
import org.example.graphics.TextureAtlas;
import org.example.graphics.UIRenderer;
import org.example.voxel.Block;
import org.example.voxel.Raycast;
import org.example.voxel.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

public class Main {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final String BLOCK_TEX_DIR = "assets/minecraft/textures/block";

    private org.example.graphics.Texture hotbarTexture;
    private org.example.graphics.Texture hotbarSelectionTexture;
    private org.example.graphics.Texture crosshairTexture;

    private Window window;
    private Player player;
    private World world;
    private Hotbar hotbar;
    private UIRenderer uiRenderer;
    private TextureAtlas atlas;

    private int worldShader;
    private int modelLocation, viewLocation, projectionLocation, atlasSamplerLocation;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public static void main(String[] args) { new Main().run(); }

    public void run() { init(); loop(); cleanup(); }

    private void init() {
        window = new Window(WIDTH, HEIGHT, "Minecraft Remake");
        window.create();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        glfwSetInputMode(window.getId(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        Input.init(window.getId());

        atlas = new TextureAtlas(BLOCK_TEX_DIR, Block.ATLAS_TEXTURES);

        worldShader = createWorldShader();
        modelLocation = glGetUniformLocation(worldShader, "model");
        viewLocation = glGetUniformLocation(worldShader, "view");
        projectionLocation = glGetUniformLocation(worldShader, "projection");
        atlasSamplerLocation = glGetUniformLocation(worldShader, "atlasTexture");

        world = new World(System.currentTimeMillis(), atlas);
        int spawnY = world.getGroundY(0, 0) + 2;
        player = new Player(0.5f, spawnY, 0.5f);

        glfwSetCursorPosCallback(window.getId(), (w, x, y) -> player.handleMouse((float) x, (float) y));

        hotbar = new Hotbar();
        uiRenderer = new UIRenderer(WIDTH, HEIGHT);
        hotbarTexture = new org.example.graphics.Texture(
                "assets/minecraft/textures/gui/sprites/hud/hotbar.png");
        hotbarSelectionTexture = new org.example.graphics.Texture(
                "assets/minecraft/textures/gui/sprites/hud/hotbar_selection.png");
        crosshairTexture = new org.example.graphics.Texture(
                "assets/minecraft/textures/gui/sprites/hud/crosshair.png");
    }

    private void loop() {
        double lastTime = glfwGetTime();
        double fpsTimer = 0;
        int frames = 0;
        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            if (dt > 0.1f) dt = 0.1f;
            lastTime = now;

            fpsTimer += dt;
            frames++;
            if (fpsTimer >= 1.0) {
                window.setTitle("Minecraft Remake - FPS: " + frames);
                frames = 0; fpsTimer = 0;
            }

            update(dt);
            render();
            window.update();
        }
    }

    private void update(float dt) {
        Input.update();
        if (Input.isKeyDown(GLFW_KEY_ESCAPE)) window.close();

        for (int i = 0; i < 9; i++) {
            if (Input.isKeyJustPressed(GLFW_KEY_1 + i)) hotbar.setSelectedSlot(i);
        }
        double scroll = Input.getScrollY();
        if (scroll != 0) hotbar.scroll(scroll > 0 ? -1 : 1);

        player.update(dt, world);

        Vector3f eye = player.getEyePosition();
        Vector3f dir = player.getFront();
        Raycast.Hit hit = Raycast.cast(world, eye, dir, 6.0f);

        if (Input.isMouseJustPressed(GLFW_MOUSE_BUTTON_LEFT) && hit != null) {
            world.setBlockGlobal(hit.blockX, hit.blockY, hit.blockZ, Block.AIR);
        }
        if (Input.isMouseJustPressed(GLFW_MOUSE_BUTTON_RIGHT) && hit != null) {
            int px = hit.blockX + hit.faceX;
            int py = hit.blockY + hit.faceY;
            int pz = hit.blockZ + hit.faceZ;
            Vector3f p = player.getPosition();
            float minX = p.x - 0.3f, maxX = p.x + 0.3f;
            float minY = p.y,        maxY = p.y + 1.8f;
            float minZ = p.z - 0.3f, maxZ = p.z + 0.3f;
            boolean overlap = !(maxX <= px || minX >= px + 1
                    || maxY <= py || minY >= py + 1
                    || maxZ <= pz || minZ >= pz + 1);
            if (!overlap) world.setBlockGlobal(px, py, pz, hotbar.getSelected());
        }

        world.update();
    }

    private void render() {
        glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderWorld();
        renderUI();
    }

    private void renderWorld() {
        glUseProgram(worldShader);
        Matrix4f proj = new Matrix4f().perspective(
                (float) Math.toRadians(70), (float) WIDTH / HEIGHT, 0.1f, 300f);
        uploadMatrix(projectionLocation, proj);
        uploadMatrix(viewLocation, player.getViewMatrix());

        glActiveTexture(GL_TEXTURE0);
        atlas.bind();
        glUniform1i(atlasSamplerLocation, 0);

        world.render(modelLocation);
        glUseProgram(0);
    }

    private void renderUI() {
        uiRenderer.begin();

        // --- Crosshair ---
        float crossSize = 24;
        uiRenderer.draw(crosshairTexture,
                WIDTH / 2f - crossSize / 2f,
                HEIGHT / 2f - crossSize / 2f,
                crossSize, crossSize);

        // --- Hotbar background (real Minecraft texture: 182x22) ---
        float scale = 3f;
        float hotbarW = 182 * scale;
        float hotbarH = 22 * scale;
        float hbX = (WIDTH - hotbarW) / 2f;
        float hbY = HEIGHT - hotbarH - 16;

        uiRenderer.draw(hotbarTexture, hbX, hbY, hotbarW, hotbarH);

        // --- Block icons inside each slot ---
        // Each slot is 20px wide in the 182-wide texture, first slot starts at x=1
        float slotPx = hotbarW / 182f * 20f;
        float slotStartX = hotbarW / 182f * 1f;
        float slotStartY = hotbarH / 22f * 1f;

        for (int i = 0; i < 9; i++) {
            float cx = hbX + slotStartX + (i + 0.5f) * slotPx;
            float cy = hbY + slotStartY + 0.5f * slotPx;
            drawIsoBlockIcon(hotbar.getSlot(i), cx, cy, slotPx * 0.85f);
        }

        // --- Selection overlay (24x24, drawn centered over selected slot) ---
        int sel = hotbar.getSelectedSlot();
        float selPx = hotbarW / 182f * 24f;
        float selOffset = hotbarW / 182f * -1f; // selection texture is offset -1 from slot
        float selX = hbX + slotStartX + sel * slotPx + selOffset;
        float selY = hbY + slotStartY + selOffset;
        uiRenderer.draw(hotbarSelectionTexture, selX-3, selY-3, selPx, selPx);

        uiRenderer.end();
    }
    /** Draws a small isometric 3-face block icon at (cx, cy) using atlas textures. */
    private void drawIsoBlockIcon(byte block, float cx, float cy, float size) {
        if (block == Block.AIR) return;

        float[] topUV  = atlas.getUV(Block.getTexture(block, 0));
        float[] sideUV = atlas.getUV(Block.getTexture(block, 2));
        float[] topTint  = Block.getTint(block, 0);
        float[] sideTint = Block.getTint(block, 2);

        float r = size / 2f;
        float h = r * 0.866f;

        float Tx = cx,        Ty = cy - r;
        float TRx = cx + h,   TRy = cy - r / 2f;
        float BRx = cx + h,   BRy = cy + r / 2f;
        float Bx = cx,        By = cy + r;
        float BLx = cx - h,   BLy = cy + r / 2f;
        float TLx = cx - h,   TLy = cy - r / 2f;
        float Cx = cx,        Cy = cy;

        int id = atlas.getId();

        // Top rhombus (T -> TR -> C -> TL)
        uiRenderer.drawTexturedQuad(id,
                Tx,  Ty,  topUV[0], topUV[1],
                TRx, TRy, topUV[2], topUV[1],
                Cx,  Cy,  topUV[2], topUV[3],
                TLx, TLy, topUV[0], topUV[3],
                topTint[0], topTint[1], topTint[2], 1f);

        // Right face (lighter side)
        float rs = 0.85f;
        uiRenderer.drawTexturedQuad(id,
                TRx, TRy, sideUV[0], sideUV[1],
                BRx, BRy, sideUV[0], sideUV[3],
                Bx,  By,  sideUV[2], sideUV[3],
                Cx,  Cy,  sideUV[2], sideUV[1],
                sideTint[0] * rs, sideTint[1] * rs, sideTint[2] * rs, 1f);

        // Left face (darker side)
        float ls = 0.62f;
        uiRenderer.drawTexturedQuad(id,
                TLx, TLy, sideUV[0], sideUV[1],
                Cx,  Cy,  sideUV[2], sideUV[1],
                Bx,  By,  sideUV[2], sideUV[3],
                BLx, BLy, sideUV[0], sideUV[3],
                sideTint[0] * ls, sideTint[1] * ls, sideTint[2] * ls, 1f);
    }

    private void uploadMatrix(int loc, Matrix4f m) {
        matrixBuffer.clear();
        m.get(matrixBuffer);
        glUniformMatrix4fv(loc, false, matrixBuffer);
    }

    private int createWorldShader() {
        String vs = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aUV;
            layout (location = 2) in vec3 aColor;
            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;
            out vec2 vUV;
            out vec3 vColor;
            void main() {
                gl_Position = projection * view * model * vec4(aPos, 1.0);
                vUV = aUV;
                vColor = aColor;
            }
            """;
        String fs = """
            #version 330 core
            in vec2 vUV;
            in vec3 vColor;
            uniform sampler2D atlasTexture;
            out vec4 FragColor;
            void main() {
                vec4 c = texture(atlasTexture, vUV);
                if (c.a < 0.5) discard;
                FragColor = vec4(c.rgb * vColor, 1.0);
            }
            """;

        int v = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(v, vs); glCompileShader(v);
        if (glGetShaderi(v, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Vertex shader failed:\n" + glGetShaderInfoLog(v));

        int f = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(f, fs); glCompileShader(f);
        if (glGetShaderi(f, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Fragment shader failed:\n" + glGetShaderInfoLog(f));

        int p = glCreateProgram();
        glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader link failed:\n" + glGetProgramInfoLog(p));

        glDeleteShader(v); glDeleteShader(f);
        return p;
    }

    private void cleanup() {
        world.cleanup();
        uiRenderer.cleanup();
        atlas.delete();
        glDeleteProgram(worldShader);
        hotbarTexture.delete();
        hotbarSelectionTexture.delete();
        crosshairTexture.delete();
        window.destroy();
    }
}