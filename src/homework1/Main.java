package homework1;

import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.Timer;

public class Main {
    private static final float EXPLOSION_FORCE = 300000.0f;
    private static final float FIXED_DT = 1.0f / 60.0f;
    private static final float MAX_FRAME_DT = 0.25f;
    private static final int MAX_SUBSTEPS_PER_FRAME = 4;
    private static final float TARGET_PHYSICS_FPS = 1.0f / FIXED_DT;
    private static final float FPS_SMOOTHING_FACTOR = 0.9f;
    private static final float MIN_FRAME_TIME = 1.0e-6f;
    private static final int ENTER_BUTTON_COLOR = 0xD72F2F;
    private static final int RETURN_BUTTON_COLOR = 0x2FA84F;
    private static final int WALL_COLOR_A = 0xD7DDE6;
    private static final int WALL_COLOR_B = 0xBEC8D4;
    private static final int ROOM_WALL_COLOR = 0xF3F3F3;
    private static final int ROOM_FLOOR_COLOR = 0xB8B8B8;
    private static final float BUTTON_HIT_PADDING = 10.0f;
    private static final float CUBE_SIZE = 60.0f;
    private static final int WALL_COLUMNS = 11;
    private static final int WALL_ROWS = 8;
    private static final int ROOM_DEPTH_CUBES = 10;
    private static final float WALL_BASE_Y = 30.0f;
    private static final float WALL_FRONT_Z = 380.0f;
    private static final float AIR_WALL_THICKNESS = 12.0f;
    private static final float ROOM_SIDE_THICKNESS = 32.0f;
    private static final float ROOM_BACK_THICKNESS = 32.0f;
    private static final float ROOM_FLOOR_THICKNESS = 32.0f;
    private static final float FAN_SPIN_SPEED   = 1.8f;  // rad/s, ceiling fan
    private static final float BLADE_SPIN_SPEED = 3.0f;  // rad/s, blender blade (减速)
    private static final float[][] UNIT_CUBE_VERTICES = {
            {-0.5f, -0.5f, -0.5f},
            {0.5f, -0.5f, -0.5f},
            {0.5f, 0.5f, -0.5f},
            {-0.5f, 0.5f, -0.5f},
            {-0.5f, -0.5f, 0.5f},
            {0.5f, -0.5f, 0.5f},
            {0.5f, 0.5f, 0.5f},
            {-0.5f, 0.5f, 0.5f}
    };

    private static final class SceneSetup {
        EngineData data;
        int enterButtonIndex;
        float wallCenterX;
        float wallCenterY;
        float wallImpactZ;
        int fanIndex   = -1;
        int bladeIndex = -1;
        // 单开口盒子（不含前侧可破碎墙）的 5 面房间墙体索引
        int[] roomPanelIndices = new int[5];
        int roomPanelCount = 0;
        // 滚筒旋转的轴心（房间中心 XY）
        float drumPivotX = 0.0f;
        float drumPivotY = 0.0f;
    }
    
    public static void main(String[] args) {
        // 1. 初始化数据引擎 (Model 层)
        SceneSetup setup = createInitialScene();
        
        // 3. 将数据层丢给渲染画布 (View 层)
        RasterCanvas canvas = new RasterCanvas();
        
        // 4. 装载并显示窗口
        JFrame frame = new JFrame("Hardcore Engine v0.3 - AI Agent Ready");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(canvas);
        // 窗口改为正方形以匹配渲染比例
        frame.setSize(900, 900);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 5. 启动核心引擎与交互！
        engineStart(canvas, setup);
    }

    // 核心调度器：包含物理主循环和输入监听
    public static void engineStart(RasterCanvas canvas, SceneSetup setup) {
        final class LoopState {
            long lastTickNanos;
            long lastStatsPrintNanos;
            float accumulator = 0.0f;
            float renderFpsSmoothed = 0.0f;
            float physicsFpsSmoothed = 0.0f;
            PhysicsCore physicsCore;
            EngineData engineData;
            int enterButtonIndex = -1;
            int returnButtonIndex = -1;
            int returnButtonOriginalColor = -1;
            long lastReturnButtonSwapNanos = 0L;
            float wallCenterX;
            float wallCenterY;
            float wallImpactZ;
            boolean physicsActive = false;
            final Random random = new Random(EngineData.RANDOM_SEED + 2026L);
            int fanIndex   = -1;
            int bladeIndex = -1;
            float fanAngle   = 0.0f;
            float bladeAngle = 0.0f;
            // 滚筒洗衣机动画状态（点击红块后开始）
            int[] roomPanelIndices = new int[0];
            float drumPivotX = 0.0f;
            float drumPivotY = 0.0f;
            float drumAngle     = 0.0f;  // 当前最新旋转总量（rad）
            float drumAngleBase = 0.0f;  // 本次滚动开始时的角度
            float drumPhaseTimer = 0.0f; // 本阶段已过时间（s）
            float drumStartDelay = 0.0f; // 点击后延迟开始滚动（s）
            boolean drumRolling  = false; // true=正在滚动，false=正在等待
        }
        final LoopState loopState = new LoopState();
        
        // 初始化物理核心 (你刚才新建的包含 TODO 的占位类)
        loopState.engineData = setup.data;
        loopState.physicsCore = null;
        loopState.enterButtonIndex = setup.enterButtonIndex;
        loopState.wallCenterX = setup.wallCenterX;
        loopState.wallCenterY = setup.wallCenterY;
        loopState.wallImpactZ = setup.wallImpactZ;
        loopState.fanIndex   = setup.fanIndex;
        loopState.bladeIndex = setup.bladeIndex;
        loopState.roomPanelIndices = java.util.Arrays.copyOf(setup.roomPanelIndices, setup.roomPanelCount);
        loopState.drumPivotX = setup.drumPivotX;
        loopState.drumPivotY = setup.drumPivotY;
        Runnable resetScene = () -> {
            SceneSetup resetSetup = createInitialScene();
            loopState.engineData = resetSetup.data;
            loopState.physicsCore = null;
            loopState.physicsActive = false;
            loopState.enterButtonIndex = resetSetup.enterButtonIndex;
            loopState.returnButtonIndex = -1;
            loopState.wallCenterX = resetSetup.wallCenterX;
            loopState.wallCenterY = resetSetup.wallCenterY;
            loopState.wallImpactZ = resetSetup.wallImpactZ;
            loopState.fanIndex   = resetSetup.fanIndex;
            loopState.bladeIndex = resetSetup.bladeIndex;
            loopState.fanAngle   = 0.0f;
            loopState.bladeAngle = 0.0f;
            loopState.roomPanelIndices = java.util.Arrays.copyOf(resetSetup.roomPanelIndices, resetSetup.roomPanelCount);
            loopState.drumPivotX     = resetSetup.drumPivotX;
            loopState.drumPivotY     = resetSetup.drumPivotY;
            loopState.drumAngle      = 0.0f;
            loopState.drumAngleBase  = 0.0f;
            loopState.drumPhaseTimer = 0.0f;
            loopState.drumRolling    = false;
            loopState.drumStartDelay = 0.0f;
            loopState.returnButtonOriginalColor = -1;
            loopState.lastReturnButtonSwapNanos = 0L;
            canvas.clearBuffers(0xD7E3F2);
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
                // Advance kinematic angles before each physics step
                loopState.fanAngle   += FAN_SPIN_SPEED   * FIXED_DT;
                loopState.bladeAngle += BLADE_SPIN_SPEED * FIXED_DT;
                if (loopState.fanIndex   >= 0) loopState.physicsCore.driveKinematicBody(loopState.fanIndex,   loopState.fanAngle, FAN_SPIN_SPEED);
                if (loopState.bladeIndex >= 0) loopState.physicsCore.driveKinematicBody(loopState.bladeIndex, loopState.bladeAngle, BLADE_SPIN_SPEED);
                if (loopState.roomPanelIndices.length > 0) {
                    // 如果存在点击后的初始延迟，先消耗它
                    if (loopState.drumStartDelay > 0.0f) {
                        loopState.drumStartDelay = Math.max(0.0f, loopState.drumStartDelay - FIXED_DT);
                    } else {
                        loopState.drumPhaseTimer += FIXED_DT;
                        final float drumRollDuration = 1.0f;
                        final float drumWaitDuration = 2.0f; // 改为 2 秒间隔
                        final float drumStep = (float) (Math.PI * 0.5);
                        if (loopState.drumRolling) {
                            float t = Math.min(loopState.drumPhaseTimer / drumRollDuration, 1.0f);
                            float tEased = t * t * (3.0f - 2.0f * t);
                            float targetAngle = loopState.drumAngleBase + tEased * drumStep;
                            float deltaAngle = targetAngle - loopState.drumAngle;
                            if (Math.abs(deltaAngle) > 1.0e-7f) {
                                applyDrumRotation(loopState.engineData, loopState.physicsCore, loopState.roomPanelIndices,
                                        loopState.drumPivotX, loopState.drumPivotY, deltaAngle, FIXED_DT);
                                loopState.drumAngle = targetAngle;
                            }
                            if (loopState.drumPhaseTimer >= drumRollDuration) {
                                float snapped = loopState.drumAngleBase + drumStep;
                                float snapDelta = snapped - loopState.drumAngle;
                                if (Math.abs(snapDelta) > 1.0e-7f) {
                                    applyDrumRotation(loopState.engineData, loopState.physicsCore, loopState.roomPanelIndices,
                                            loopState.drumPivotX, loopState.drumPivotY, snapDelta, FIXED_DT);
                                    loopState.drumAngle = snapped;
                                }
                                loopState.drumRolling = false;
                                loopState.drumPhaseTimer = 0.0f;
                            }
                        } else if (loopState.drumPhaseTimer >= drumWaitDuration) {
                            loopState.drumRolling = true;
                            loopState.drumAngleBase = loopState.drumAngle;
                            loopState.drumPhaseTimer = 0.0f;
                        }
                    }
                }
                loopState.physicsCore.stepSimulation(FIXED_DT);
                loopState.accumulator -= FIXED_DT;
                substeps++;
            }
            // Also rotate visually when physics is not yet active (before explosion)
            if (!loopState.physicsActive) {
                loopState.fanAngle   += FAN_SPIN_SPEED   * frameDt;
                loopState.bladeAngle += BLADE_SPIN_SPEED * frameDt;
            }
            // Update visual rotation in EngineData so the renderer picks it up
            if (loopState.fanIndex   >= 0) loopState.engineData.rotY[loopState.fanIndex]   = loopState.fanAngle;
            if (loopState.bladeIndex >= 0) loopState.engineData.rotY[loopState.bladeIndex] = loopState.bladeAngle;

            if (substeps == MAX_SUBSTEPS_PER_FRAME) {
                loopState.accumulator = 0.0f;
            }
            float simulatedSeconds = substeps * FIXED_DT;
            float physicsFpsInstant = (simulatedSeconds / Math.max(frameDt, MIN_FRAME_TIME)) * TARGET_PHYSICS_FPS;
            loopState.physicsFpsSmoothed = (loopState.physicsFpsSmoothed <= 0.0f)
                    ? physicsFpsInstant
                    : loopState.physicsFpsSmoothed * FPS_SMOOTHING_FACTOR
                    + physicsFpsInstant * (1.0f - FPS_SMOOTHING_FACTOR);

            if (now - loopState.lastStatsPrintNanos >= 1_000_000_000L) {
                System.out.printf(
                        "[Debug] renderFPS=%.1f physicsFPS=%.1f entities=%d physicsActive=%s%n",
                        loopState.renderFpsSmoothed,
                        loopState.physicsActive ? loopState.physicsFpsSmoothed : 0.0f,
                        loopState.engineData.count,
                        loopState.physicsActive);
                loopState.lastStatsPrintNanos = now;
            }

            // 绿色方块每秒随机换一个
                if (loopState.physicsActive && loopState.returnButtonIndex >= 0
                    && (now - loopState.lastReturnButtonSwapNanos) >= 2_000_000_000L) {
                // 恢复旧率色
                loopState.engineData.colors[loopState.returnButtonIndex] = loopState.returnButtonOriginalColor;
                int newReturnIdx = -1;
                if (loopState.engineData.count > 1) {
                    int maxAttempts = 50;
                    for (int attempt = 0; attempt < maxAttempts; attempt++) {
                        int candidate = loopState.random.nextInt(loopState.engineData.count);
                        if (candidate != loopState.enterButtonIndex
                                && !loopState.engineData.isStatic[candidate]
                                && !loopState.engineData.isKinematic[candidate]) {
                            newReturnIdx = candidate;
                            break;
                        }
                    }
                }
                if (newReturnIdx >= 0) {
                    loopState.returnButtonOriginalColor = loopState.engineData.colors[newReturnIdx];
                    loopState.returnButtonIndex = newReturnIdx;
                    loopState.engineData.colors[newReturnIdx] = RETURN_BUTTON_COLOR;
                }
                loopState.lastReturnButtonSwapNanos = now;
            }

            canvas.clearBuffers(0xD7E3F2);
            Matrix4f viewProjMatrix = canvas.getViewProjMatrix();
            for (int i = 0; i < loopState.engineData.count; i++) {
                canvas.drawSolidCube(loopState.engineData, i, viewProjMatrix);
            }
            canvas.repaint(); // 触发 RasterCanvas 重新画图
        });
        loopState.lastTickNanos = System.nanoTime();
        loopState.lastStatsPrintNanos = loopState.lastTickNanos;
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
                        int newIdx = -1;
                        if (loopState.engineData.count > 1) {
                            int maxAttempts = 50;
                            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                                int candidate = loopState.random.nextInt(loopState.engineData.count);
                                if (candidate != loopState.enterButtonIndex
                                        && !loopState.engineData.isStatic[candidate]
                                        && !loopState.engineData.isKinematic[candidate]) {
                                    newIdx = candidate;
                                    break;
                                }
                            }
                        }
                        if (newIdx >= 0) {
                            // 保存旧颜色再涂绿
                            loopState.returnButtonOriginalColor = loopState.engineData.colors[newIdx];
                            loopState.returnButtonIndex = newIdx;
                            loopState.engineData.colors[newIdx] = RETURN_BUTTON_COLOR;
                            loopState.lastReturnButtonSwapNanos = System.nanoTime();
                        }
                        loopState.physicsCore = new PhysicsCore(loopState.engineData);
                        // 精确贴合开口，使空气墙像一块玻璃板封住前侧方块墙
                        loopState.physicsCore.addStaticBoxBody(
                            loopState.wallCenterX,
                            loopState.wallCenterY,
                            WALL_FRONT_Z + AIR_WALL_THICKNESS * 0.5f,
                            WALL_COLUMNS * CUBE_SIZE * 0.5f,
                            WALL_ROWS * CUBE_SIZE * 0.5f,
                            AIR_WALL_THICKNESS * 0.5f);
                        loopState.physicsActive = true;
                        // 点击后延迟 2 秒再开始滚动
                        loopState.drumStartDelay = 2.0f;
                        loopState.drumRolling = false;
                        loopState.drumPhaseTimer = 0.0f;
                        loopState.drumAngleBase = loopState.drumAngle;
                        loopState.physicsCore.applyExplosionImpulse(
                            loopState.wallCenterX, loopState.wallCenterY, loopState.wallImpactZ, EXPLOSION_FORCE);
                        canvas.requestFocusInWindow();
                        return;
                    }
                } else if (isCubeClicked(loopState.engineData, loopState.returnButtonIndex, mouseX, mouseY, canvas)) {
                    resetScene.run();
                    canvas.requestFocusInWindow();
                    return;
                }

                if (loopState.physicsActive && loopState.physicsCore != null) {
                    float ndcX = (mouseX / (float) canvas.getWidth()) * 2.0f - 1.0f;
                    float ndcY = 1.0f - (mouseY / (float) canvas.getHeight()) * 2.0f;
                    float aspect = (float) RasterCanvas.RENDER_W / (float) RasterCanvas.RENDER_H;
                    float focalLength = 1.0f / (float) Math.tan(RasterCanvas.CAMERA_FOV * 0.5f);
                    float viewZ = loopState.wallImpactZ + RasterCanvas.CAMERA_Z;
                    float viewX = ndcX * (-viewZ) * aspect / focalLength;
                    float viewY = ndcY * (-viewZ) / focalLength;
                    loopState.physicsCore.applyExplosionImpulse(
                            viewX,
                            viewY - RasterCanvas.CAMERA_Y,
                            loopState.wallImpactZ,
                            EXPLOSION_FORCE * 0.5f);
                }
                canvas.requestFocusInWindow();
            }
        });
    }

    private static SceneSetup createInitialScene() {
        SceneSetup setup = new SceneSetup();
        EngineData data = new EngineData();

        float wallSpanX = (WALL_COLUMNS - 1) * CUBE_SIZE;
        float wallOriginX = -wallSpanX * 0.5f;
        float wallOuterWidth = WALL_COLUMNS * CUBE_SIZE;
        float wallOuterHeight = WALL_ROWS * CUBE_SIZE;
        // 内部空间保持严格正方形（以较小边为准，保证方块墙能尽可能完整）
        float roomInnerSize = Math.min(wallOuterWidth, wallOuterHeight);
        float roomInnerWidth = roomInnerSize;
        float roomInnerHeight = roomInnerSize;
        float roomInnerDepth = ROOM_DEPTH_CUBES * CUBE_SIZE;
        int enterColumn = WALL_COLUMNS / 2;
        int enterRow = WALL_ROWS / 2;

        for (int row = 0; row < WALL_ROWS; row++) {
            for (int column = 0; column < WALL_COLUMNS; column++) {
                float realX = wallOriginX + column * CUBE_SIZE;
                float realY = WALL_BASE_Y + row * CUBE_SIZE;
                float realZ = WALL_FRONT_Z;
                int color = ((column + row) % 2 == 0) ? WALL_COLOR_A : WALL_COLOR_B;
                int indexBeforeAdd = data.count;
                data.addCube(realX, realY, realZ, CUBE_SIZE, color, false, false);
                if (column == enterColumn && row == enterRow) {
                    setup.enterButtonIndex = indexBeforeAdd;
                }
            }
        }
        if (setup.enterButtonIndex >= 0 && setup.enterButtonIndex < data.count) {
            data.colors[setup.enterButtonIndex] = ENTER_BUTTON_COLOR;
        }

        float roomCenterZ = WALL_FRONT_Z - roomInnerDepth * 0.5f;
        float fanBarW    = wallOuterWidth * 0.75f;
        float fanBarH    = CUBE_SIZE * 0.18f * 2.0f; // 加宽/加高为原来的2倍
        float fanBarD    = CUBE_SIZE * 0.35f * 2.0f;
        float fanY       = (WALL_BASE_Y + wallOuterHeight) - CUBE_SIZE * 0.6f; // near ceiling
        setup.fanIndex   = data.addRotatorBox(0.0f, fanY, roomCenterZ,
                                              fanBarW, fanBarH, fanBarD, 0x4A90D9);

        float bladeBarW  = wallOuterWidth * 0.65f;
        // 将宽和高设为相同且放大一点，避免过细；这里用基于方块尺寸的统一厚度
        float bladeThickness = CUBE_SIZE * 0.6f; // 同时作为 H 和 D
        float bladeBarH  = bladeThickness;
        float bladeBarD  = bladeThickness;
        // 将刀片中心贴着场景底部（底面与地面齐） —— 地面顶面 Y = 0，因此中心 Y = half thickness
        float bladeY     = bladeBarH * 0.5f;
        setup.bladeIndex = data.addRotatorBox(0.0f, bladeY, roomCenterZ,
                              bladeBarW, bladeBarH, bladeBarD, 0xE87722);


        float sideWallOffsetX = roomInnerWidth * 0.5f + ROOM_SIDE_THICKNESS * 0.5f;
        float sideWallCenterY = WALL_BASE_Y + roomInnerHeight * 0.5f - CUBE_SIZE * 0.5f;
        float backWallCenterZ = WALL_FRONT_Z - roomInnerDepth - ROOM_BACK_THICKNESS * 0.5f;
        float floorCenterY = -ROOM_FLOOR_THICKNESS * 0.5f;
        float ceilingCenterY = roomInnerHeight + ROOM_FLOOR_THICKNESS * 0.5f;

        setup.roomPanelIndices[setup.roomPanelCount++] = data.count;
        data.addKinematicBox(
                -sideWallOffsetX,
                sideWallCenterY,
                roomCenterZ,
                ROOM_SIDE_THICKNESS,
            wallOuterHeight,
                roomInnerDepth + ROOM_BACK_THICKNESS,
            ROOM_WALL_COLOR);
        setup.roomPanelIndices[setup.roomPanelCount++] = data.count;
        data.addKinematicBox(
                sideWallOffsetX,
                sideWallCenterY,
                roomCenterZ,
                ROOM_SIDE_THICKNESS,
            wallOuterHeight,
                roomInnerDepth + ROOM_BACK_THICKNESS,
            ROOM_WALL_COLOR);
        setup.roomPanelIndices[setup.roomPanelCount++] = data.count;
        data.addKinematicBox(
                0.0f,
                sideWallCenterY,
                backWallCenterZ,
            wallOuterWidth,
            wallOuterHeight,
                ROOM_BACK_THICKNESS,
            ROOM_WALL_COLOR);
        setup.roomPanelIndices[setup.roomPanelCount++] = data.count;
        data.addKinematicBox(
                0.0f,
                floorCenterY,
                roomCenterZ,
            wallOuterWidth,
                ROOM_FLOOR_THICKNESS,
                roomInnerDepth + ROOM_BACK_THICKNESS,
            ROOM_FLOOR_COLOR);
        setup.roomPanelIndices[setup.roomPanelCount++] = data.count;
        data.addKinematicBox(
                0.0f,
                ceilingCenterY,
                roomCenterZ,
            wallOuterWidth,
                ROOM_FLOOR_THICKNESS,
                roomInnerDepth + ROOM_BACK_THICKNESS,
            ROOM_WALL_COLOR);
        // 滚筒旋转轴心 = 房间内部 XY 中心
        setup.drumPivotX = 0.0f;
        setup.drumPivotY = sideWallCenterY;

        setup.data = data;
        setup.wallCenterX = 0.0f;
        setup.wallCenterY = WALL_BASE_Y + (WALL_ROWS - 1) * CUBE_SIZE * 0.5f;
        setup.wallImpactZ = WALL_FRONT_Z + CUBE_SIZE * 1.75f;
        return setup;
    }

    private static void applyDrumRotation(EngineData data, PhysicsCore physicsCore, int[] panelIndices,
                                          float pivotX, float pivotY, float deltaAngle, float dt) {
        float cosA = (float) Math.cos(deltaAngle);
        float sinA = (float) Math.sin(deltaAngle);
        float angularSpeed = dt > 1.0e-7f ? deltaAngle / dt : 0.0f;
        for (int idx : panelIndices) {
            if (idx < 0 || idx >= data.count) continue;
            float oldX = data.xPos[idx];
            float oldY = data.yPos[idx];
            float dx = oldX - pivotX;
            float dy = oldY - pivotY;
            float newX = pivotX + dx * cosA - dy * sinA;
            float newY = pivotY + dx * sinA + dy * cosA;
            data.xPos[idx] = newX;
            data.yPos[idx] = newY;
            data.rotZ[idx] += deltaAngle;
            float linearVx = dt > 1.0e-7f ? (newX - oldX) / dt : 0.0f;
            float linearVy = dt > 1.0e-7f ? (newY - oldY) / dt : 0.0f;
            physicsCore.driveKinematicBodyZ(idx, newX, newY, data.zPos[idx], data.rotZ[idx],
                    linearVx, linearVy, 0.0f, angularSpeed);
        }
    }

    private static boolean isCubeClicked(EngineData data, int cubeIndex, int mouseX, int mouseY, RasterCanvas canvas) {
        if (cubeIndex < 0 || cubeIndex >= data.count) {
            return false;
        }

        float sizeX = data.sizeX[cubeIndex];
        float sizeY = data.sizeY[cubeIndex];
        float sizeZ = data.sizeZ[cubeIndex];
        Matrix4f model = Matrix4f.multiply(
                Matrix4f.createTranslation(data.xPos[cubeIndex], data.yPos[cubeIndex], data.zPos[cubeIndex]),
                Matrix4f.multiply(
                        Matrix4f.createRotationZ(data.rotZ[cubeIndex]),
                        Matrix4f.multiply(
                                Matrix4f.createRotationY(data.rotY[cubeIndex]),
                                Matrix4f.createRotationX(data.rotX[cubeIndex]))));
        Matrix4f mvp = Matrix4f.multiply(canvas.getViewProjMatrix(), model);

        // 优先使用投影中心点 + 自适应半径命中检测（对翻滚中的方块准确均适用）
        float[] centerClip = mvp.transform(0.0f, 0.0f, 0.0f, 1.0f);
        float cw = centerClip[3];
        if (cw > 1.0e-6f) {
            float cNdcX = centerClip[0] / cw;
            float cNdcY = centerClip[1] / cw;
            float cNdcZ = centerClip[2] / cw;
            if (cNdcZ >= -1.0f && cNdcZ <= 1.0f) {
                float cScreenX = (cNdcX * 0.5f + 0.5f) * canvas.getWidth();
                float cScreenY = (1.0f - (cNdcY * 0.5f + 0.5f)) * canvas.getHeight();
                float dx = mouseX - cScreenX;
                float dy = mouseY - cScreenY;
                // 屏幕半径：基于方块最大尺寸和透视深度自动估算
                float maxSize = Math.max(sizeX, Math.max(sizeY, sizeZ));
                float projRadius = maxSize * canvas.getWidth() / (2.0f * cw);
                float clickRadius = Math.max(projRadius * 0.65f, BUTTON_HIT_PADDING + 20.0f);
                if (dx * dx + dy * dy <= clickRadius * clickRadius) {
                    return true;
                }
            }
        }

        // 备用：8 顶点屏幕 AABB（处理中心点在相机后方的极端情况）
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        boolean hasProjectedVertex = false;

        for (float[] vertex : UNIT_CUBE_VERTICES) {
            float[] clip = mvp.transform(vertex[0] * sizeX, vertex[1] * sizeY, vertex[2] * sizeZ, 1.0f);
            float w = clip[3];
            if (w <= 1.0e-6f) {
                continue;
            }
            float ndcX = clip[0] / w;
            float ndcY = clip[1] / w;
            float ndcZ = clip[2] / w;
            if (ndcZ < -1.0f || ndcZ > 1.0f) {
                continue;
            }
            float screenX = (ndcX * 0.5f + 0.5f) * canvas.getWidth();
            float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * canvas.getHeight();
            minX = Math.min(minX, screenX);
            maxX = Math.max(maxX, screenX);
            minY = Math.min(minY, screenY);
            maxY = Math.max(maxY, screenY);
            hasProjectedVertex = true;
        }

        if (!hasProjectedVertex) {
            return false;
        }

        return mouseX >= minX - BUTTON_HIT_PADDING
                && mouseX <= maxX + BUTTON_HIT_PADDING
                && mouseY >= minY - BUTTON_HIT_PADDING
                && mouseY <= maxY + BUTTON_HIT_PADDING;
    }

}
