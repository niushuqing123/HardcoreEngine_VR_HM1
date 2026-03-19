package homework1;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.Timer;

public class Main {
    private static final float SPAWN_HEIGHT_MIN = 400f;
    private static final float SPAWN_HEIGHT_RANGE = 400f;
    
    public static void main(String[] args) {
        // 1. 初始化数据引擎 (Model 层)
        EngineData data = new EngineData();
        Random random = new Random(EngineData.RANDOM_SEED);
        
        // 2. 生成测试场景：一个 5x5 的地面阵列
        float cubeSize = 50f;
        // 极其讲究的嵌套遍历顺序：从后往前，确保远处的先画，解决深度遮挡
        for (int z = 4; z >= 0; z--) {
            for (int x = 0; x < 5; x++) {
                float realX = x * (cubeSize + 2);
                float realZ = z * (cubeSize + 2);
                float realY = SPAWN_HEIGHT_MIN + random.nextFloat() * SPAWN_HEIGHT_RANGE;
                
                int color = ((x + z) % 2 == 0) ? 0x00ADB5 : 0x008A93; 
                data.addCube(realX, realY, realZ, cubeSize, color);
            }
        }
        
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
        
        // 初始化物理核心 (你刚才新建的包含 TODO 的占位类)
        PhysicsCore physics = new PhysicsCore(data);

        // 1. 游戏主循环 (Game Loop) - 锁定约 60 FPS (16ms)
        Timer gameLoop = new Timer(16, e -> {
            physics.stepSimulation(0.016f); // 推进物理时间 (目前是空壳，等 AI 来写)
            canvas.repaint();               // 触发 IsoCanvas 重新画图
        });
        gameLoop.start();

        // 2. 交互监听器占位 - 给云端 AI 留的外包作业
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();
                
                // TODO: AI Agent, implement Inverse Isometric Raycasting here!
                // Map screen (mouseX, mouseY) back to the 3D grid and highlight the clicked cube.
                // Or trigger the explosion at the clicked location.
                System.out.println("[Debug脚手架] 鼠标点击了屏幕坐标: X=" + mouseX + ", Y=" + mouseY);
            }
        });
    }
}
