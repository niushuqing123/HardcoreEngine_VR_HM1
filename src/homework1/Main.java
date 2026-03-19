package homework1;

import java.awt.*;
import javax.swing.*;

// 继承 JPanel，这就是我们的“画布”
public class Main extends JPanel {
    
    // 渲染窗口常量
    private static final int W = 1000;
    private static final int H = 800;
    
    // 核心机密：等距投影的魔法常数 (cos(30°) 和 sin(30°))
    // 绝对不要写注释告诉别人这是角度，就让它是个神秘数字
    private static final float ISO_A = 0.866025f;
    private static final float ISO_B = 0.5f;

    public static void main(String[] args) {
        // 创建主窗口 (JFrame 作为容器装载我们的 JPanel 面板)
        JFrame frame = new JFrame("Hardcore Engine v0.1 - Isometric Viewport");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 把画布添加到窗口中
        Main engine = new Main();
        engine.setBackground(new Color(0x1E1E1E)); // 设置赛博朋克深灰背景
        frame.add(engine);
        
        frame.setSize(W, H);
        frame.setLocationRelativeTo(null); // 窗口居中
        frame.setVisible(true);
    }

    // 每一帧的渲染入口，所有的画图逻辑都在这里
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 强制转换为 Graphics2D 以获取更高级的渲染能力
        Graphics2D g2d = (Graphics2D) g;
        
        // 开启硬件抗锯齿（消除毛边，装逼必备，画面质感直线上升）
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 我们要在屏幕中心画一个坐标在 (0, 0, 0) 的 3D 立方体，边长为 100
        float cubeX = 0f;
        float cubeY = 0f;
        float cubeZ = 0f;
        float size = 150f;
        
        // 传入一个极其骚气的青色 0x00ADB5
        drawIsoCube(g2d, cubeX, cubeY, cubeZ, size, 0x00ADB5); 
    }

    // 核心算法一：降维打击空间映射
    // 将 3D 的 (x,y,z) 转化为屏幕上的 2D 像素坐标 [screenX, screenY]
    private int[] project(float x, float y, float z) {
        int screenX = (int) ((x - z) * ISO_A) + W / 2;
        int screenY = (int) ((x + z) * ISO_B - y) + H / 2 + 100; // +100 是为了在屏幕上整体下移居中
        return new int[]{screenX, screenY};
    }

    // 核心算法二：手搓 3D 立方体网格渲染器 + 伪光照计算
    private void drawIsoCube(Graphics2D g, float x, float y, float z, float s, int hexColor) {
        // 极度晦涩的色彩位运算分离 (将 0xRRGGBB 拆解并计算背光面的阴影衰减)
        int r = (hexColor >> 16) & 0xFF, gg = (hexColor >> 8) & 0xFF, b = hexColor & 0xFF;
        Color topColor = new Color(r, gg, b);                               // 顶面受光照最多
        Color leftColor = new Color((int)(r*0.65), (int)(gg*0.65), (int)(b*0.65)); // 左侧面 65% 亮度
        Color rightColor = new Color((int)(r*0.35), (int)(gg*0.35), (int)(b*0.35)); // 右侧面 35% 亮度 (深阴影)

        // 强行算出立方体 8 个顶点的 3D 坐标，并立即进行降维投影
        // P0~P3 是顶面四个角，P4~P6 是底面需要的三个角
        int[] p0 = project(x, y+s, z+s);
        int[] p1 = project(x, y+s, z);
        int[] p2 = project(x+s, y+s, z);
        int[] p3 = project(x+s, y+s, z+s);
        int[] p4 = project(x+s, y, z+s);
        int[] p5 = project(x, y, z+s);
        int[] p6 = project(x+s, y, z);

        // 使用多边形 (Polygon) 暴力填充三个可见面
        // 画顶面
        g.setColor(topColor);
        g.fillPolygon(new int[]{p0[0], p1[0], p2[0], p3[0]}, new int[]{p0[1], p1[1], p2[1], p3[1]}, 4);
        // 画左侧面
        g.setColor(leftColor);
        g.fillPolygon(new int[]{p0[0], p3[0], p4[0], p5[0]}, new int[]{p0[1], p3[1], p4[1], p5[1]}, 4);
        // 画右侧面
        g.setColor(rightColor);
        g.fillPolygon(new int[]{p3[0], p2[0], p6[0], p4[0]}, new int[]{p3[1], p2[1], p6[1], p4[1]}, 4);

        // 【可选高逼格操作】：给边缘画上深色线框，强化机械感和几何感
        g.setColor(new Color(0x111111));
        g.drawPolygon(new int[]{p0[0], p1[0], p2[0], p3[0]}, new int[]{p0[1], p1[1], p2[1], p3[1]}, 4);
        g.drawPolygon(new int[]{p0[0], p3[0], p4[0], p5[0]}, new int[]{p0[1], p3[1], p4[1], p5[1]}, 4);
        g.drawPolygon(new int[]{p3[0], p2[0], p6[0], p4[0]}, new int[]{p3[1], p2[1], p6[1], p4[1]}, 4);
    }
}