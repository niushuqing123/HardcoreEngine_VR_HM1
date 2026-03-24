package homework1;

import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.Timer;

public class Main {
    private static final float SPAWN_HEIGHT_MIN = 400f;
    private static final float SPAWN_HEIGHT_RANGE = 400f;
    // Explosion strength scalar used by PhysicsCore.applyExplosionImpulse.
    private static final float EXPLOSION_FORCE = 60000f;
    private static final float FIXED_DT = 1.0f / 60.0f;
    private static final float MAX_FRAME_DT = 0.25f;
    private static final int MAX_SUBSTEPS_PER_FRAME = 4;
    private static final float ISO_A = 0.866025f;
    private static final float ISO_B = 0.5f;
    private static final int VIEW_Y_OFFSET = 150;
    private static final float TARGET_PHYSICS_FPS = 1.0f / FIXED_DT;
    private static final float FPS_SMOOTHING_FACTOR = 0.9f;
    private static final float MIN_FRAME_TIME = 1.0e-6f;
    
    public static void main(String[] args) {
        // 1. 初始化数据引擎 (Model 层)
        EngineData data = createInitialScene();
        
        // 3. 将数据层丢给渲染画布 (View 层)
        IsoCanvas canvas = new IsoCanvas(data);
        
        // 4. 装载并显示窗口
        JFrame frame = new JFrame("Hardcore Engine v0.3 - AI Agent Ready");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(canvas);
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 5. 启动核心引擎与交互！
        engineStart(canvas, data);
    }

    // 核心调度器：包含物理主循环和输入监听
    public static void engineStart(IsoCanvas canvas, EngineData data) {
        final class LoopState {
            long lastTickNanos;
            float accumulator = 0.0f;
            float renderFpsSmoothed = 0.0f;
            float physicsFpsSmoothed = 0.0f;
            PhysicsCore physicsCore;
            EngineData engineData;
        }
        final LoopState loopState = new LoopState();
        
        // 初始化物理核心 (你刚才新建的包含 TODO 的占位类)
        loopState.engineData = data;
        loopState.physicsCore = new PhysicsCore(data);

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
            while (loopState.accumulator >= FIXED_DT && substeps < MAX_SUBSTEPS_PER_FRAME) {
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
            canvas.setDebugStats(loopState.engineData.count, loopState.renderFpsSmoothed, loopState.physicsFpsSmoothed);
            canvas.repaint(); // 触发 IsoCanvas 重新画图
        });
        loopState.lastTickNanos = System.nanoTime();
        gameLoop.start();

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    EngineData resetData = createInitialScene();
                    loopState.engineData = resetData;
                    loopState.physicsCore = new PhysicsCore(resetData);
                    canvas.setData(resetData);
                    canvas.setDebugStats(loopState.engineData.count,
                            loopState.renderFpsSmoothed, loopState.physicsFpsSmoothed);
                    canvas.repaint();
                }
            }
        });
 
        // 2. 交互监听器占位 - 给云端 AI 留的外包作业
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();

                int cx = canvas.getWidth() / 2;
                int cy = canvas.getHeight() / 2 + VIEW_Y_OFFSET;
                float u = (mouseX - cx) / ISO_A;
                float v = (mouseY - cy) / ISO_B;
                float worldX = 0.5f * (u + v);
                float worldZ = 0.5f * (v - u);

                loopState.physicsCore.applyExplosionImpulse(worldX, 0.0f, worldZ, EXPLOSION_FORCE);
                System.out.println("[Debug脚手架] 鼠标点击了屏幕坐标: X=" + mouseX + ", Y=" + mouseY);
                canvas.requestFocusInWindow();
            }
        });
    }

    private static EngineData createInitialScene() {
        EngineData data = new EngineData();
        Random random = new Random(EngineData.RANDOM_SEED);

        float cubeSize = 50f;
        for (int z = 4; z >= 0; z--) {
            for (int x = 0; x < 5; x++) {
                float realX = x * (cubeSize + 2);
                float realZ = z * (cubeSize + 2);
                float realY = SPAWN_HEIGHT_MIN + random.nextFloat() * SPAWN_HEIGHT_RANGE;
                int color = ((x + z) % 2 == 0) ? 0x00ADB5 : 0x008A93;
                data.addCube(realX, realY, realZ, cubeSize, color);
            }
        }
        return data;
    }
}
