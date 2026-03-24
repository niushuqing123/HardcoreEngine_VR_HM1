package homework1;

import java.awt.*;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class IsoCanvas extends JPanel {
    private static final float ISO_A = 0.866025f;
    private static final float ISO_B = 0.5f;
    private static final int VIEW_Y_OFFSET = 150;
    private static final Color BACKGROUND_TOP = new Color(0x293241);
    private static final Color BACKGROUND_BOTTOM = new Color(0x141B24);
    private static final Color GROUND_COLOR = new Color(0x2F5D3A);
    private static final Color GROUND_EDGE_COLOR = new Color(0x233E2B);
    private static final Color BOUNDS_COLOR = new Color(120, 230, 255, 150);
    private static final Color HUD_BG = new Color(0, 0, 0, 130);
    private static final Color HUD_TEXT = new Color(0xEAF4FF);
    private static final Color ROOM_BACKGROUND = new Color(0xF4F6FA);
    private static final Color ROOM_FLOOR = new Color(0xE3E8EF);
    private static final Color ROOM_FLOOR_EDGE = new Color(0xCED6E2);
    private static final Color BUTTON_OUTLINE = new Color(0x101010);
    private static final float SCENE_BOUNDS_INITIAL_MAX_HEIGHT = 180.0f;
    private static final int HUD_CORNER_RADIUS = 12;
    
    // 渲染器需要持有数据的引用
    private EngineData data;
    private float renderFps = 0.0f;
    private float physicsFps = 0.0f;
    private int blockCount = 0;
    private boolean insideRoomMode = false;
    private int enterButtonIndex = -1;
    private int returnButtonIndex = -1;
    
    public IsoCanvas(EngineData data) {
        this.data = data;
        this.blockCount = data.count;
        this.setBackground(new Color(0x1E1E1E));
    }

    public void setData(EngineData data) {
        this.data = data;
        this.blockCount = data.count;
    }

    public void setDebugStats(int blockCount, float renderFps, float physicsFps) {
        this.blockCount = blockCount;
        this.renderFps = renderFps;
        this.physicsFps = physicsFps;
    }

    public void setInsideRoomMode(boolean insideRoomMode) {
        this.insideRoomMode = insideRoomMode;
    }

    public void setMenuButtonIndices(int enterButtonIndex, int returnButtonIndex) {
        this.enterButtonIndex = enterButtonIndex;
        this.returnButtonIndex = returnButtonIndex;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
 
        // 获取窗口中心点
        int cx = getWidth() / 2;
        int cy = getHeight() / 2 + VIEW_Y_OFFSET; // 整体下移一点

        drawGround(g2d, cx, cy);
        drawSceneBounds(g2d, cx, cy);

        // 核心：根据 x + z - y 做深度排序（画家算法），值小的更远，先画
        int[] order = new int[data.count];
        float[] depth = new float[data.count];
        for (int i = 0; i < data.count; i++) {
            order[i] = i;
            depth[i] = data.xPos[i] + data.zPos[i] - data.yPos[i];
        }
        insertionSortByDepth(order, depth, data.count);

        for (int n = 0; n < data.count; n++) {
            int i = order[n];
            drawGroundShadow(g2d, data.xPos[i], data.zPos[i], data.size[i], cx, cy);
        }

        for (int n = 0; n < data.count; n++) {
            int i = order[n];
            drawIsoCube(g2d, data.xPos[i], data.yPos[i], data.zPos[i], data.size[i],
                    data.rotX[i], data.rotY[i], data.rotZ[i], data.colors[i], cx, cy,
                    i == enterButtonIndex || i == returnButtonIndex);
        }

        drawHud(g2d);
    }

    private int[] project(float x, float y, float z, int cx, int cy) {
        int screenX = (int) ((x - z) * ISO_A) + cx;
        int screenY = (int) ((x + z) * ISO_B - y) + cy;
        return new int[]{screenX, screenY};
    }

    private void drawBackground(Graphics2D g) {
        if (insideRoomMode) {
            g.setColor(ROOM_BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        GradientPaint gradient = new GradientPaint(
                0, 0, BACKGROUND_TOP,
                0, getHeight(), BACKGROUND_BOTTOM
        );
        Paint oldPaint = g.getPaint();
        g.setPaint(gradient);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(oldPaint);
    }

    private void drawGround(Graphics2D g, int cx, int cy) {
        int worldHalfExtent = 700;
        int[] p0 = project(-worldHalfExtent, 0.0f, -worldHalfExtent, cx, cy);
        int[] p1 = project(worldHalfExtent, 0.0f, -worldHalfExtent, cx, cy);
        int[] p2 = project(worldHalfExtent, 0.0f, worldHalfExtent, cx, cy);
        int[] p3 = project(-worldHalfExtent, 0.0f, worldHalfExtent, cx, cy);

        int[] xs = new int[]{p0[0], p1[0], p2[0], p3[0]};
        int[] ys = new int[]{p0[1], p1[1], p2[1], p3[1]};
        g.setColor(insideRoomMode ? ROOM_FLOOR : GROUND_COLOR);
        g.fillPolygon(xs, ys, 4);
        g.setColor(insideRoomMode ? ROOM_FLOOR_EDGE : GROUND_EDGE_COLOR);
        g.drawPolygon(xs, ys, 4);
    }

    private void drawGroundShadow(Graphics2D g, float x, float z, float s, int cx, int cy) {
        int[] center = project(x + s * 0.5f, 0.0f, z + s * 0.5f, cx, cy);
        int shadowW = Math.max(10, (int) (s * 0.9f));
        int shadowH = Math.max(6, (int) (s * 0.35f));
        g.setColor(new Color(0, 0, 0, 45));
        g.fillOval(center[0] - shadowW / 2, center[1] - shadowH / 2, shadowW, shadowH);
    }

    private void drawSceneBounds(Graphics2D g, int cx, int cy) {
        if (data.count <= 0) {
            return;
        }
        float minX = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = SCENE_BOUNDS_INITIAL_MAX_HEIGHT;
        float maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < data.count; i++) {
            float x0 = data.xPos[i];
            float y1 = data.yPos[i] + data.size[i];
            float z0 = data.zPos[i];
            float x1 = x0 + data.size[i];
            float z1 = z0 + data.size[i];
            if (x0 < minX) minX = x0;
            if (z0 < minZ) minZ = z0;
            if (x1 > maxX) maxX = x1;
            if (y1 > maxY) maxY = y1;
            if (z1 > maxZ) maxZ = z1;
        }
        float pad = 8.0f;
        minX -= pad;
        minZ -= pad;
        maxX += pad;
        maxZ += pad;
        drawWireBox(g, minX, 0.0f, minZ, maxX, maxY, maxZ, cx, cy);
    }

    private void drawWireBox(Graphics2D g, float minX, float minY, float minZ,
                             float maxX, float maxY, float maxZ, int cx, int cy) {
        float[][] points = new float[][]{
                {minX, minY, minZ}, {maxX, minY, minZ}, {maxX, minY, maxZ}, {minX, minY, maxZ},
                {minX, maxY, minZ}, {maxX, maxY, minZ}, {maxX, maxY, maxZ}, {minX, maxY, maxZ}
        };
        int[][] p2 = new int[8][2];
        for (int i = 0; i < 8; i++) {
            p2[i] = project(points[i][0], points[i][1], points[i][2], cx, cy);
        }
        int[][] edges = new int[][]{
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(BOUNDS_COLOR);
        for (int[] edge : edges) {
            g.drawLine(p2[edge[0]][0], p2[edge[0]][1], p2[edge[1]][0], p2[edge[1]][1]);
        }
        g.setStroke(oldStroke);
    }

    private void drawHud(Graphics2D g) {
        int panelX = 16;
        int panelY = 16;
        int panelW = 270;
        int panelH = 76;
        g.setColor(HUD_BG);
        g.fillRoundRect(panelX, panelY, panelW, panelH, HUD_CORNER_RADIUS, HUD_CORNER_RADIUS);
        g.setColor(HUD_TEXT);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        g.drawString("Blocks: " + blockCount, panelX + 12, panelY + 24);
        g.drawString(String.format("Render FPS: %.1f", renderFps), panelX + 12, panelY + 44);
        g.drawString(String.format("Physics FPS: %.1f", physicsFps), panelX + 12, panelY + 64);
    }

    private void drawIsoCube(Graphics2D g, float x, float y, float z, float s,
                             float rotX, float rotY, float rotZ, int hexColor, int cx, int cy,
                             boolean menuButton) {
        int r = (hexColor >> 16) & 0xFF, gg = (hexColor >> 8) & 0xFF, b = hexColor & 0xFF;
        Color topColor = new Color(r, gg, b);
        Color midColor = new Color((int) (r * 0.75f), (int) (gg * 0.75f), (int) (b * 0.75f));
        Color darkColor = new Color((int) (r * 0.55f), (int) (gg * 0.55f), (int) (b * 0.55f));
        Color darkerColor = new Color((int) (r * 0.4f), (int) (gg * 0.4f), (int) (b * 0.4f));
        Color[] faceColors = new Color[]{
                darkColor,      // +X
                midColor,       // -X
                topColor,       // +Y
                darkerColor,    // -Y
                darkColor,      // +Z
                midColor        // -Z
        };

        float hs = s * 0.5f;

        // 顶点以立方体中心为原点（局部空间）
        float[][] local = new float[][]{
                {-hs, -hs, -hs}, // 0
                { hs, -hs, -hs}, // 1
                { hs,  hs, -hs}, // 2
                {-hs,  hs, -hs}, // 3
                {-hs, -hs,  hs}, // 4
                { hs, -hs,  hs}, // 5
                { hs,  hs,  hs}, // 6
                {-hs,  hs,  hs}  // 7
        };
        int[][] faces = new int[][]{
                {1, 2, 6, 5}, // +X
                {0, 4, 7, 3}, // -X
                {3, 7, 6, 2}, // +Y
                {0, 1, 5, 4}, // -Y
                {4, 5, 6, 7}, // +Z
                {0, 3, 2, 1}  // -Z
        };
        float[][] localNormals = new float[][]{
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0},
                {0, 0, 1},
                {0, 0, -1}
        };

        float viewX = 1.0f;
        float viewY = 1.0f;
        float viewZ = 1.0f;

        float sinX = (float) Math.sin(rotX), cosX = (float) Math.cos(rotX);
        float sinY = (float) Math.sin(rotY), cosY = (float) Math.cos(rotY);
        float sinZ = (float) Math.sin(rotZ), cosZ = (float) Math.cos(rotZ);

        int[][] p = new int[local.length][2];
        float[] worldX = new float[local.length];
        float[] worldY = new float[local.length];
        float[] worldZ = new float[local.length];
        float cx3d = x + hs;
        float cy3d = y + hs;
        float cz3d = z + hs;

        for (int i = 0; i < local.length; i++) {
            float vx = local[i][0];
            float vy = local[i][1];
            float vz = local[i][2];

            float[] rotated = rotateXYZ(vx, vy, vz, sinX, cosX, sinY, cosY, sinZ, cosZ);
            worldX[i] = cx3d + rotated[0];
            worldY[i] = cy3d + rotated[1];
            worldZ[i] = cz3d + rotated[2];
            p[i] = project(worldX[i], worldY[i], worldZ[i], cx, cy);
        }

        int[] faceOrder = new int[faces.length];
        float[] faceDepth = new float[faces.length];
        for (int f = 0; f < faces.length; f++) {
            faceOrder[f] = f;
            int[] face = faces[f];
            float avgX = (worldX[face[0]] + worldX[face[1]] + worldX[face[2]] + worldX[face[3]]) * 0.25f;
            float avgY = (worldY[face[0]] + worldY[face[1]] + worldY[face[2]] + worldY[face[3]]) * 0.25f;
            float avgZ = (worldZ[face[0]] + worldZ[face[1]] + worldZ[face[2]] + worldZ[face[3]]) * 0.25f;
            faceDepth[f] = avgX + avgZ - avgY;
        }
        insertionSortByDepth(faceOrder, faceDepth, faces.length);

        for (int oi = 0; oi < faceOrder.length; oi++) {
            int f = faceOrder[oi];
            float[] rotatedNormal = rotateXYZ(
                    localNormals[f][0], localNormals[f][1], localNormals[f][2],
                    sinX, cosX, sinY, cosY, sinZ, cosZ
            );
            float dot = rotatedNormal[0] * viewX + rotatedNormal[1] * viewY + rotatedNormal[2] * viewZ;
            if (dot >= 0.0f) {
                continue;
            }

            int[] face = faces[f];
            int[] xs = new int[]{p[face[0]][0], p[face[1]][0], p[face[2]][0], p[face[3]][0]};
            int[] ys = new int[]{p[face[0]][1], p[face[1]][1], p[face[2]][1], p[face[3]][1]};

            g.setColor(faceColors[f]);
            g.fillPolygon(xs, ys, 4);
            g.setColor(menuButton ? BUTTON_OUTLINE : new Color(0x111111));
            g.drawPolygon(xs, ys, 4);
        }
    }

    private float[] rotateXYZ(float vx, float vy, float vz,
                              float sinX, float cosX, float sinY, float cosY, float sinZ, float cosZ) {
        // Rotate around X
        float rx = vx;
        float ry = vy * cosX - vz * sinX;
        float rz = vy * sinX + vz * cosX;

        // Rotate around Y
        float r2x = rx * cosY + rz * sinY;
        float r2y = ry;
        float r2z = -rx * sinY + rz * cosY;

        // Rotate around Z
        float r3x = r2x * cosZ - r2y * sinZ;
        float r3y = r2x * sinZ + r2y * cosZ;
        float r3z = r2z;
        return new float[]{r3x, r3y, r3z};
    }

    private void insertionSortByDepth(int[] order, float[] depth, int length) {
        for (int i = 1; i < length; i++) {
            int idx = order[i];
            float d = depth[idx];
            int j = i - 1;
            while (j >= 0 && depth[order[j]] > d) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = idx;
        }
    }
}
