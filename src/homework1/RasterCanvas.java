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
    private static final int MAX_FACE_SHADE_INDEX = 5;
    private static final float FACE_SHADE_STEP = 0.10f;
    private static final float[][] CUBE_VERTICES = {
            {-0.5f, -0.5f, -0.5f},
            {0.5f, -0.5f, -0.5f},
            {0.5f, 0.5f, -0.5f},
            {-0.5f, 0.5f, -0.5f},
            {-0.5f, -0.5f, 0.5f},
            {0.5f, -0.5f, 0.5f},
            {0.5f, 0.5f, 0.5f},
            {-0.5f, 0.5f, 0.5f}
    };
    private static final int[][] CUBE_TRIANGLES = {
            {0, 1, 2}, {2, 3, 0},
            {4, 6, 5}, {6, 4, 7},
            {0, 3, 7}, {7, 4, 0},
            {1, 5, 6}, {6, 2, 1},
            {3, 2, 6}, {6, 7, 3},
            {0, 4, 5}, {5, 1, 0}
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

    private static float edgeFunction(float ax, float ay, float bx, float by, float px, float py) {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
    }

    private static int shadeFaceColor(int baseColor, int faceIndex) {
        float shade = 1.0f - Math.min(faceIndex, MAX_FACE_SHADE_INDEX) * FACE_SHADE_STEP;
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        int rr = Math.max(0, Math.min(255, Math.round(r * shade)));
        int gg = Math.max(0, Math.min(255, Math.round(g * shade)));
        int bb = Math.max(0, Math.min(255, Math.round(b * shade)));
        return (rr << 16) | (gg << 8) | bb;
    }

    private void drawTriangle(float[] v0, float[] v1, float[] v2, int color) {
        int minX = Math.max(0, (int) Math.floor(Math.min(v0[0], Math.min(v1[0], v2[0]))));
        int maxX = Math.min(RENDER_W - 1, (int) Math.ceil(Math.max(v0[0], Math.max(v1[0], v2[0]))));
        int minY = Math.max(0, (int) Math.floor(Math.min(v0[1], Math.min(v1[1], v2[1]))));
        int maxY = Math.min(RENDER_H - 1, (int) Math.ceil(Math.max(v0[1], Math.max(v1[1], v2[1]))));
        if (minX > maxX || minY > maxY) {
            return;
        }

        float area = edgeFunction(v0[0], v0[1], v1[0], v1[1], v2[0], v2[1]);
        if (Math.abs(area) <= W_EPSILON) {
            return;
        }

        boolean areaPositive = area > 0.0f;
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = edgeFunction(v1[0], v1[1], v2[0], v2[1], px, py);
                float w1 = edgeFunction(v2[0], v2[1], v0[0], v0[1], px, py);
                float w2 = edgeFunction(v0[0], v0[1], v1[0], v1[1], px, py);
                boolean inside = areaPositive
                        ? (w0 >= 0.0f && w1 >= 0.0f && w2 >= 0.0f)
                        : (w0 <= 0.0f && w1 <= 0.0f && w2 <= 0.0f);
                if (!inside) {
                    continue;
                }

                float invArea = 1.0f / area;
                float b0 = w0 * invArea;
                float b1 = w1 * invArea;
                float b2 = w2 * invArea;
                float interpolatedZ = b0 * v0[2] + b1 * v1[2] + b2 * v2[2];
                int pixelIndex = y * RENDER_W + x;
                if (interpolatedZ < zBuffer[pixelIndex]) {
                    zBuffer[pixelIndex] = interpolatedZ;
                    pixels[pixelIndex] = color;
                }
            }
        }
    }

    public void drawSolidCube(EngineData data, int index, Matrix4f viewProjMatrix) {
        float cubeSize = data.size[index];
        Matrix4f model = Matrix4f.multiply(
                Matrix4f.createTranslation(data.xPos[index], data.yPos[index], data.zPos[index]),
                Matrix4f.multiply(
                        Matrix4f.createRotationZ(data.rotZ[index]),
                        Matrix4f.multiply(
                                Matrix4f.createRotationY(data.rotY[index]),
                                Matrix4f.createRotationX(data.rotX[index]))));
        Matrix4f mvp = Matrix4f.multiply(viewProjMatrix, model);

        float[][] transformed = new float[CUBE_VERTICES.length][];
        for (int v = 0; v < 8; v++) {
            float lx = CUBE_VERTICES[v][0] * cubeSize;
            float ly = CUBE_VERTICES[v][1] * cubeSize;
            float lz = CUBE_VERTICES[v][2] * cubeSize;
            float[] clip = mvp.transform(lx, ly, lz, 1.0f);
            float w = clip[3];
            if (Math.abs(w) <= W_EPSILON) {
                continue;
            }
            float invW = 1.0f / w;
            float ndcX = clip[0] * invW;
            float ndcY = clip[1] * invW;
            float ndcZ = clip[2] * invW;
            if (ndcZ < -1.0f || ndcZ > 1.0f) {
                continue;
            }
            float screenX = (ndcX * 0.5f + 0.5f) * (RENDER_W - 1);
            float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * (RENDER_H - 1);
            transformed[v] = new float[]{screenX, screenY, ndcZ};
        }

        int baseColor = data.colors[index];
        for (int tri = 0; tri < CUBE_TRIANGLES.length; tri++) {
            int i0 = CUBE_TRIANGLES[tri][0];
            int i1 = CUBE_TRIANGLES[tri][1];
            int i2 = CUBE_TRIANGLES[tri][2];
            float[] v0 = transformed[i0];
            float[] v1 = transformed[i1];
            float[] v2 = transformed[i2];
            if (v0 != null && v1 != null && v2 != null) {
                int faceColor = shadeFaceColor(baseColor, tri / 2);
                drawTriangle(v0, v1, v2, faceColor);
            }
        }
    }

    public Matrix4f getViewProjMatrix() {
        return Matrix4f.multiply(projectionMatrix, viewMatrix);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
}
