package homework1;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class RasterCanvas extends JPanel {
    public static final int RENDER_W = 320;
    public static final int RENDER_H = 240;
    private static final float CAMERA_Y = -200.0f;
    private static final float CAMERA_Z = -800.0f;
    private static final float CAMERA_FOV = (float) Math.toRadians(60.0);
    private static final float CAMERA_NEAR = 0.1f;
    private static final float CAMERA_FAR = 5000.0f;
    private static final float W_EPSILON = 1.0e-6f;
    private static final float[] BASE_CUBE_VERTICES = {
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f
    };
    private static final int[][] CUBE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private final BufferedImage image;
    private final int[] pixels;
    private final float[] zBuffer;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;

    public RasterCanvas() {
        image = new BufferedImage(RENDER_W, RENDER_H, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        zBuffer = new float[pixels.length];
        viewMatrix = Matrix4f.createTranslation(0.0f, CAMERA_Y, CAMERA_Z);
        projectionMatrix = Matrix4f.createPerspectiveProjection(
                CAMERA_FOV, (float) RENDER_W / (float) RENDER_H, CAMERA_NEAR, CAMERA_FAR);
        setBackground(new java.awt.Color(0x1E1E1E));
    }

    public void clearBuffers(int hexColor) {
        Arrays.fill(pixels, hexColor);
        Arrays.fill(zBuffer, Float.MAX_VALUE);
    }

    public void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            if (x >= 0 && x < RENDER_W && y >= 0 && y < RENDER_H) {
                pixels[y * RENDER_W + x] = color;
            }
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    public void drawWireframeCube(EngineData data, int index, Matrix4f viewProjMatrix) {
        float cubeSize = data.size[index];
        Matrix4f model = Matrix4f.multiply(
                Matrix4f.createTranslation(data.xPos[index], data.yPos[index], data.zPos[index]),
                Matrix4f.multiply(
                        Matrix4f.createRotationZ(data.rotZ[index]),
                        Matrix4f.multiply(
                                Matrix4f.createRotationY(data.rotY[index]),
                                Matrix4f.createRotationX(data.rotX[index]))));
        Matrix4f mvp = Matrix4f.multiply(viewProjMatrix, model);

        int[] screenX = new int[8];
        int[] screenY = new int[8];
        boolean[] valid = new boolean[8];
        for (int v = 0; v < 8; v++) {
            int base = v * 3;
            float lx = BASE_CUBE_VERTICES[base] * cubeSize;
            float ly = BASE_CUBE_VERTICES[base + 1] * cubeSize;
            float lz = BASE_CUBE_VERTICES[base + 2] * cubeSize;
            float[] clip = mvp.transform(lx, ly, lz, 1.0f);
            float w = clip[3];
            if (Math.abs(w) <= W_EPSILON) {
                valid[v] = false;
                continue;
            }
            float invW = 1.0f / w;
            float ndcX = clip[0] * invW;
            float ndcY = clip[1] * invW;
            float ndcZ = clip[2] * invW;
            if (ndcZ < -1.0f || ndcZ > 1.0f) {
                valid[v] = false;
                continue;
            }
            screenX[v] = (int) ((ndcX * 0.5f + 0.5f) * (RENDER_W - 1));
            screenY[v] = (int) ((1.0f - (ndcY * 0.5f + 0.5f)) * (RENDER_H - 1));
            valid[v] = true;
        }

        int color = data.colors[index];
        for (int[] edge : CUBE_EDGES) {
            int v0 = edge[0];
            int v1 = edge[1];
            if (valid[v0] && valid[v1]) {
                drawLine(screenX[v0], screenY[v0], screenX[v1], screenY[v1], color);
            }
        }
    }

    public void renderWireframes(EngineData data) {
        Matrix4f viewProjMatrix = Matrix4f.multiply(projectionMatrix, viewMatrix);
        for (int i = 0; i < data.count; i++) {
            drawWireframeCube(data, i, viewProjMatrix);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
}
