package homework1;

import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.Timer;

public class Main {
    // Explosion strength scalar used by PhysicsCore.applyExplosionImpulse.
    private static final float EXPLOSION_FORCE = 85000f;
    private static final float FIXED_DT = 1.0f / 60.0f;
    private static final float MAX_FRAME_DT = 0.25f;
    private static final int MAX_SUBSTEPS_PER_FRAME = 4;
    private static final float ISO_A = 0.866025f;
    private static final float ISO_B = 0.5f;
    private static final int VIEW_Y_OFFSET = 150;
    private static final float TARGET_PHYSICS_FPS = 1.0f / FIXED_DT;
    private static final float FPS_SMOOTHING_FACTOR = 0.9f;
    private static final float MIN_FRAME_TIME = 1.0e-6f;
    private static final int ENTER_BUTTON_COLOR = 0xD72F2F;
    private static final int RETURN_BUTTON_COLOR = 0x2FA84F;
    private static final int WALL_COLOR_A = 0xD7DDE6;
    private static final int WALL_COLOR_B = 0xBEC8D4;
    private static final float BUTTON_HIT_RADIUS_SCALE = 0.70f;
    private static final float WALL_DIAGONAL_DEPTH = 320.0f; // x+z plane constant used as wall center Z.
    private static final float WALL_BASE_Y = 170.0f;

    private static final class SceneSetup {
        EngineData data;
        int enterButtonIndex;
        float wallCenterX;
        float wallCenterY;
        float wallCenterZ;
    }
    
    public static void main(String[] args) {
        // 1. 初始化数据引擎 (Model 层)
        SceneSetup setup = createInitialScene();
        EngineData data = setup.data;
        
        // 3. 将数据层丢给渲染画布 (View 层)
        RasterCanvas canvas = new RasterCanvas();
        
        // 4. 装载并显示窗口
        JFrame frame = new JFrame("Hardcore Engine v0.3 - AI Agent Ready");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(canvas);
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 5. 启动核心引擎与交互！
        engineStart(canvas, setup);
    }

    // 核心调度器：包含物理主循环和输入监听
    public static void engineStart(RasterCanvas canvas, SceneSetup setup) {
        final class LoopState {
            long lastTickNanos;
            float accumulator = 0.0f;
            float renderFpsSmoothed = 0.0f;
            float physicsFpsSmoothed = 0.0f;
            PhysicsCore physicsCore;
            EngineData engineData;
            int enterButtonIndex = -1;
            int returnButtonIndex = -1;
            float wallCenterX;
            float wallCenterY;
            float wallCenterZ;
            boolean physicsActive = false;
            final Random random = new Random(EngineData.RANDOM_SEED + 2026L);
        }
        final LoopState loopState = new LoopState();
        
        // 初始化物理核心 (你刚才新建的包含 TODO 的占位类)
        loopState.engineData = setup.data;
        loopState.physicsCore = null;
        loopState.enterButtonIndex = setup.enterButtonIndex;
        loopState.wallCenterX = setup.wallCenterX;
        loopState.wallCenterY = setup.wallCenterY;
        loopState.wallCenterZ = setup.wallCenterZ;
        Runnable resetScene = () -> {
            SceneSetup resetSetup = createInitialScene();
            loopState.engineData = resetSetup.data;
            loopState.physicsCore = null;
            loopState.physicsActive = false;
            loopState.enterButtonIndex = resetSetup.enterButtonIndex;
            loopState.returnButtonIndex = -1;
            loopState.wallCenterX = resetSetup.wallCenterX;
            loopState.wallCenterY = resetSetup.wallCenterY;
            loopState.wallCenterZ = resetSetup.wallCenterZ;
            canvas.clearBuffers(0x1E1E1E);
            canvas.repaint();
        };

        // 1. 游戏主循环 (Game Loop) - 锁定约 60 FPS (16ms)
        Timer gameLoop = new Timer(16, e -> {
            long now = System.nanoTime();
            float frameDt = (now - loopState.lastTickNanos) * 1.0e-9f;
            loopState.lastTickNanos = now;
            frameDt = Math.min(frameDt, MAX_FRAME_DT);
            loopState.accumulator += frameDt;
            if (frameDt > 0.0f) {
                float frameFpsInstant = 1.0f / frameDt;
                loopState.renderFpsSmoothed = (loopState.renderFpsSmoothed <= 0.0f)
                        ? frameFpsInstant
                        : loopState.renderFpsSmoothed * FPS_SMOOTHING_FACTOR
                        + frameFpsInstant * (1.0f - FPS_SMOOTHING_FACTOR);
            }

            int substeps = 0;
            while (loopState.physicsActive && loopState.accumulator >= FIXED_DT && substeps < MAX_SUBSTEPS_PER_FRAME) {
                loopState.physicsCore.stepSimulation(FIXED_DT);
                loopState.accumulator -= FIXED_DT;
                substeps++;
            }
            if (substeps == MAX_SUBSTEPS_PER_FRAME) {
                loopState.accumulator = 0.0f;
            }
            float simulatedSeconds = substeps * FIXED_DT;
            float physicsFpsInstant = (simulatedSeconds / Math.max(frameDt, MIN_FRAME_TIME)) * TARGET_PHYSICS_FPS;
            loopState.physicsFpsSmoothed = (loopState.physicsFpsSmoothed <= 0.0f)
                    ? physicsFpsInstant
                    : loopState.physicsFpsSmoothed * FPS_SMOOTHING_FACTOR
                    + physicsFpsInstant * (1.0f - FPS_SMOOTHING_FACTOR);
            canvas.clearBuffers(0x1E1E1E);
            Matrix4f viewProjMatrix = canvas.getViewProjMatrix();
            for (int i = 0; i < loopState.engineData.count; i++) {
                canvas.drawSolidCube(loopState.engineData, i, viewProjMatrix);
            }
            canvas.repaint(); // 触发 RasterCanvas 重新画图
        });
        loopState.lastTickNanos = System.nanoTime();
        gameLoop.start();

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    resetScene.run();
                }
            }
        });
 
        // 2. 交互监听器占位 - 给云端 AI 留的外包作业
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();

                if (loopState.returnButtonIndex < 0) {
                    if (isCubeClicked(loopState.engineData, loopState.enterButtonIndex, mouseX, mouseY, canvas)) {
                        loopState.returnButtonIndex = pickReturnButtonIndex(
                                loopState.engineData, loopState.enterButtonIndex, loopState.random);
                        if (loopState.returnButtonIndex >= 0) {
                            loopState.engineData.colors[loopState.returnButtonIndex] = RETURN_BUTTON_COLOR;
                        }
                        loopState.physicsCore = new PhysicsCore(loopState.engineData);
                        loopState.physicsActive = true;
                        loopState.physicsCore.applyExplosionImpulse(
                                loopState.wallCenterX, loopState.wallCenterY, loopState.wallCenterZ, EXPLOSION_FORCE);
                        canvas.requestFocusInWindow();
                        return;
                    }
                } else if (isCubeClicked(loopState.engineData, loopState.returnButtonIndex, mouseX, mouseY, canvas)) {
                    resetScene.run();
                    canvas.requestFocusInWindow();
                    return;
                }

                int cx = canvas.getWidth() / 2;
                int cy = canvas.getHeight() / 2 + VIEW_Y_OFFSET;
                float u = (mouseX - cx) / ISO_A;
                float v = (mouseY - cy) / ISO_B;
                float worldX = 0.5f * (u + v);
                float worldZ = 0.5f * (v - u);

                if (loopState.physicsActive && loopState.physicsCore != null) {
                    loopState.physicsCore.applyExplosionImpulse(worldX, 0.0f, worldZ, EXPLOSION_FORCE);
                }
                canvas.requestFocusInWindow();
            }
        });
    }

    private static SceneSetup createInitialScene() {
        SceneSetup setup = new SceneSetup();
        EngineData data = new EngineData();

        float cubeSize = 44f;
        float spacing = cubeSize + 2f;
        int wallColumns = 12;
        int wallRows = 8;
        float wallCenterX = WALL_DIAGONAL_DEPTH * 0.5f;
        float wallCenterZ = WALL_DIAGONAL_DEPTH;
        float wallOriginX = wallCenterX - ((wallColumns - 1) * spacing) * 0.5f;

        int enterColumn = wallColumns / 2;
        int enterRow = wallRows / 2;

        for (int row = 0; row < wallRows; row++) {
            for (int column = 0; column < wallColumns; column++) {
                float realX = wallOriginX + column * spacing;
                float realY = WALL_BASE_Y + row * spacing;
                // Place cubes on x + z = wallCenterZ so the wall faces the isometric camera head-on.
                float realZ = wallCenterZ - realX;
                int color = ((column + row) % 2 == 0) ? WALL_COLOR_A : WALL_COLOR_B;
                int indexBeforeAdd = data.count;
                data.addCube(realX, realY, realZ, cubeSize, color, false);
                if (column == enterColumn && row == enterRow) {
                    setup.enterButtonIndex = indexBeforeAdd;
                }
            }
        }
        if (setup.enterButtonIndex >= 0 && setup.enterButtonIndex < data.count) {
            data.colors[setup.enterButtonIndex] = ENTER_BUTTON_COLOR;
        }

        setup.data = data;
        setup.wallCenterX = wallCenterX;
        setup.wallCenterY = WALL_BASE_Y + (wallRows - 1) * spacing * 0.5f;
        setup.wallCenterZ = wallCenterZ + cubeSize * 0.5f;
        return setup;
    }

    private static int pickReturnButtonIndex(EngineData data, int enterButtonIndex, Random random) {
        if (data.count <= 1) {
            return -1;
        }
        int idx = random.nextInt(data.count - 1);
        if (idx >= enterButtonIndex) {
            idx += 1;
        }
        return idx;
    }

    private static boolean isCubeClicked(EngineData data, int cubeIndex, int mouseX, int mouseY, RasterCanvas canvas) {
        if (cubeIndex < 0 || cubeIndex >= data.count) {
            return false;
        }
        float centerX = data.xPos[cubeIndex] + data.size[cubeIndex] * 0.5f;
        float centerY = data.yPos[cubeIndex] + data.size[cubeIndex] * 0.5f;
        float centerZ = data.zPos[cubeIndex] + data.size[cubeIndex] * 0.5f;
        int cx = canvas.getWidth() / 2;
        int cy = canvas.getHeight() / 2 + VIEW_Y_OFFSET;
        int screenX = (int) ((centerX - centerZ) * ISO_A) + cx;
        int screenY = (int) ((centerX + centerZ) * ISO_B - centerY) + cy;
        float dx = mouseX - screenX;
        float dy = mouseY - screenY;
        float hitRadius = data.size[cubeIndex] * BUTTON_HIT_RADIUS_SCALE;
        return dx * dx + dy * dy <= hitRadius * hitRadius;
    }
}
