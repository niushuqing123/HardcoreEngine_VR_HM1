package homework1;

import java.awt.*;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class IsoCanvas extends JPanel {
    private static final float ISO_A = 0.866025f;
    private static final float ISO_B = 0.5f;
    
    // 渲染器需要持有数据的引用
    private EngineData data;
    
    public IsoCanvas(EngineData data) {
        this.data = data;
        this.setBackground(new Color(0x1E1E1E));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 获取窗口中心点
        int cx = getWidth() / 2;
        int cy = getHeight() / 2 + 150; // 整体下移一点

        // 核心：遍历数据层的所有方块并渲染
        // 注意：2.5D 必须从后往前画 (画家算法)，这里我们假设添加顺序已经是正确的
        for (int i = 0; i < data.count; i++) {
            drawIsoCube(g2d, data.xPos[i], data.yPos[i], data.zPos[i], data.size[i], data.colors[i], cx, cy);
        }
    }

    private int[] project(float x, float y, float z, int cx, int cy) {
        int screenX = (int) ((x - z) * ISO_A) + cx;
        int screenY = (int) ((x + z) * ISO_B - y) + cy;
        return new int[]{screenX, screenY};
    }

    private void drawIsoCube(Graphics2D g, float x, float y, float z, float s, int hexColor, int cx, int cy) {
        int r = (hexColor >> 16) & 0xFF, gg = (hexColor >> 8) & 0xFF, b = hexColor & 0xFF;
        Color topColor = new Color(r, gg, b);
        Color leftColor = new Color((int)(r*0.65), (int)(gg*0.65), (int)(b*0.65));
        Color rightColor = new Color((int)(r*0.35), (int)(gg*0.35), (int)(b*0.35));

        int[] p0 = project(x, y+s, z+s, cx, cy);
        int[] p1 = project(x, y+s, z, cx, cy);
        int[] p2 = project(x+s, y+s, z, cx, cy);
        int[] p3 = project(x+s, y+s, z+s, cx, cy);
        int[] p4 = project(x+s, y, z+s, cx, cy);
        int[] p5 = project(x, y, z+s, cx, cy);
        int[] p6 = project(x+s, y, z, cx, cy);

        g.setColor(topColor);
        g.fillPolygon(new int[]{p0[0], p1[0], p2[0], p3[0]}, new int[]{p0[1], p1[1], p2[1], p3[1]}, 4);
        g.setColor(leftColor);
        g.fillPolygon(new int[]{p0[0], p3[0], p4[0], p5[0]}, new int[]{p0[1], p3[1], p4[1], p5[1]}, 4);
        g.setColor(rightColor);
        g.fillPolygon(new int[]{p3[0], p2[0], p6[0], p4[0]}, new int[]{p3[1], p2[1], p6[1], p4[1]}, 4);

        g.setColor(new Color(0x111111));
        g.drawPolygon(new int[]{p0[0], p1[0], p2[0], p3[0]}, new int[]{p0[1], p1[1], p2[1], p3[1]}, 4);
        g.drawPolygon(new int[]{p0[0], p3[0], p4[0], p5[0]}, new int[]{p0[1], p3[1], p4[1], p5[1]}, 4);
        g.drawPolygon(new int[]{p3[0], p2[0], p6[0], p4[0]}, new int[]{p3[1], p2[1], p6[1], p4[1]}, 4);
    }
}
