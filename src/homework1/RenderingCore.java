package homework1;

// 光栅渲染核心实现。

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import javax.swing.JPanel;

@SuppressWarnings("serial")
class RasterCanvas extends JPanel {

    public static final int RENDER_W = 640;
    public static final int RENDER_H = 640;

    public static final float CAMERA_Y = -210.0f;
    public static final float CAMERA_Z = -740.0f;
    public static final float CAMERA_FOV = (float) Math.toRadians(60.0);
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

    private void drawTriangle(float[] v0, float[] v1, float[] v2, int color) {
        int minX = Math.max(0, (int) Math.floor(Math.min(v0[0], Math.min(v1[0], v2[0]))) - 1);
        int maxX = Math.min(RENDER_W - 1, (int) Math.ceil(Math.max(v0[0], Math.max(v1[0], v2[0]))) + 1);
        int minY = Math.max(0, (int) Math.floor(Math.min(v0[1], Math.min(v1[1], v2[1]))) - 1);
        int maxY = Math.min(RENDER_H - 1, (int) Math.ceil(Math.max(v0[1], Math.max(v1[1], v2[1]))) + 1);
        if (minX > maxX || minY > maxY) {
            return;
        }

        float area = (v2[0] - v0[0]) * (v1[1] - v0[1]) - (v2[1] - v0[1]) * (v1[0] - v0[0]);
        if (Math.abs(area) <= W_EPSILON) {
            return;
        }

        boolean areaPositive = area > 0.0f;
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = (px - v1[0]) * (v2[1] - v1[1]) - (py - v1[1]) * (v2[0] - v1[0]);
                float w1 = (px - v2[0]) * (v0[1] - v2[1]) - (py - v2[1]) * (v0[0] - v2[0]);
                float w2 = (px - v0[0]) * (v1[1] - v0[1]) - (py - v0[1]) * (v1[0] - v0[0]);
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

    private void drawSphereInternal(EngineData data, int index, Matrix4f viewProjMatrix) {
        float radius = data.sizeX[index] * 0.5f;
        Matrix4f model = Matrix4f.createTranslation(data.xPos[index], data.yPos[index], data.zPos[index]);
        Matrix4f mvp = Matrix4f.multiply(viewProjMatrix, model);
        int color = data.colors[index];
        int rings = 8;
        int sectors = 8;

        for (int r = 0; r < rings; r++) {
            float phi1 = (float) (Math.PI * r / rings);
            float phi2 = (float) (Math.PI * (r + 1) / rings);
            for (int s = 0; s < sectors; s++) {
                float theta1 = (float) (2.0 * Math.PI * s / sectors);
                float theta2 = (float) (2.0 * Math.PI * (s + 1) / sectors);

                float[][] v = new float[4][3];
                float x0 = (float) (radius * Math.sin(phi1) * Math.cos(theta1));
                float y0 = (float) (radius * Math.cos(phi1));
                float z0 = (float) (radius * Math.sin(phi1) * Math.sin(theta1));
                float[] clip0 = mvp.transform(x0, y0, z0, 1.0f);
                if (Math.abs(clip0[3]) > W_EPSILON) {
                    float invW0 = 1.0f / clip0[3];
                    float ndcX0 = clip0[0] * invW0;
                    float ndcY0 = clip0[1] * invW0;
                    float ndcZ0 = clip0[2] * invW0;
                    if (ndcZ0 >= -1.0f && ndcZ0 <= 1.0f) {
                        v[0] = new float[]{
                            (ndcX0 * 0.5f + 0.5f) * (RENDER_W - 1),
                            (1.0f - (ndcY0 * 0.5f + 0.5f)) * (RENDER_H - 1),
                            ndcZ0
                        };
                    }
                }
                float x1 = (float) (radius * Math.sin(phi1) * Math.cos(theta2));
                float y1 = (float) (radius * Math.cos(phi1));
                float z1 = (float) (radius * Math.sin(phi1) * Math.sin(theta2));
                float[] clip1 = mvp.transform(x1, y1, z1, 1.0f);
                if (Math.abs(clip1[3]) > W_EPSILON) {
                    float invW1 = 1.0f / clip1[3];
                    float ndcX1 = clip1[0] * invW1;
                    float ndcY1 = clip1[1] * invW1;
                    float ndcZ1 = clip1[2] * invW1;
                    if (ndcZ1 >= -1.0f && ndcZ1 <= 1.0f) {
                        v[1] = new float[]{
                            (ndcX1 * 0.5f + 0.5f) * (RENDER_W - 1),
                            (1.0f - (ndcY1 * 0.5f + 0.5f)) * (RENDER_H - 1),
                            ndcZ1
                        };
                    }
                }
                float x2 = (float) (radius * Math.sin(phi2) * Math.cos(theta2));
                float y2 = (float) (radius * Math.cos(phi2));
                float z2 = (float) (radius * Math.sin(phi2) * Math.sin(theta2));
                float[] clip2 = mvp.transform(x2, y2, z2, 1.0f);
                if (Math.abs(clip2[3]) > W_EPSILON) {
                    float invW2 = 1.0f / clip2[3];
                    float ndcX2 = clip2[0] * invW2;
                    float ndcY2 = clip2[1] * invW2;
                    float ndcZ2 = clip2[2] * invW2;
                    if (ndcZ2 >= -1.0f && ndcZ2 <= 1.0f) {
                        v[2] = new float[]{
                            (ndcX2 * 0.5f + 0.5f) * (RENDER_W - 1),
                            (1.0f - (ndcY2 * 0.5f + 0.5f)) * (RENDER_H - 1),
                            ndcZ2
                        };
                    }
                }
                float x3 = (float) (radius * Math.sin(phi2) * Math.cos(theta1));
                float y3 = (float) (radius * Math.cos(phi2));
                float z3 = (float) (radius * Math.sin(phi2) * Math.sin(theta1));
                float[] clip3 = mvp.transform(x3, y3, z3, 1.0f);
                if (Math.abs(clip3[3]) > W_EPSILON) {
                    float invW3 = 1.0f / clip3[3];
                    float ndcX3 = clip3[0] * invW3;
                    float ndcY3 = clip3[1] * invW3;
                    float ndcZ3 = clip3[2] * invW3;
                    if (ndcZ3 >= -1.0f && ndcZ3 <= 1.0f) {
                        v[3] = new float[]{
                            (ndcX3 * 0.5f + 0.5f) * (RENDER_W - 1),
                            (1.0f - (ndcY3 * 0.5f + 0.5f)) * (RENDER_H - 1),
                            ndcZ3
                        };
                    }
                }

                if (v[0] != null && v[1] != null && v[2] != null) drawTriangle(v[0], v[1], v[2], color);
                if (v[0] != null && v[2] != null && v[3] != null) drawTriangle(v[0], v[2], v[3], color);
            }
        }
    }

    public void drawSolidCube(EngineData data, int index, Matrix4f viewProjMatrix) {
        if (data.isSphere[index]) {
            drawSphereInternal(data, index, viewProjMatrix);
            return;
        }
        float boxSizeX = data.sizeX[index];
        float boxSizeY = data.sizeY[index];
        float boxSizeZ = data.sizeZ[index];
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
            float lx = CUBE_VERTICES[v][0] * boxSizeX;
            float ly = CUBE_VERTICES[v][1] * boxSizeY;
            float lz = CUBE_VERTICES[v][2] * boxSizeZ;
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
                float shade = 1.0f - Math.min(tri / 2, MAX_FACE_SHADE_INDEX) * FACE_SHADE_STEP;
                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;
                int rr = Math.max(0, Math.min(255, Math.round(r * shade)));
                int gg = Math.max(0, Math.min(255, Math.round(g * shade)));
                int bb = Math.max(0, Math.min(255, Math.round(b * shade)));
                int faceColor = (rr << 16) | (gg << 8) | bb;
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
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
}

final class Matrix4f {
    private final float[] m;

    public Matrix4f(float[] values) {
        if (values == null || values.length != 16) {
            throw new IllegalArgumentException("Matrix4f requires exactly 16 values.");
        }
        this.m = values.clone();
    }

    public float[] values() {
        return m.clone();
    }

    public static Matrix4f identity() {
        return new Matrix4f(new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public static Matrix4f createTranslation(float x, float y, float z) {
        return new Matrix4f(new float[]{
                1.0f, 0.0f, 0.0f, x,
                0.0f, 1.0f, 0.0f, y,
                0.0f, 0.0f, 1.0f, z,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public static Matrix4f createRotationX(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new Matrix4f(new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, c, -s, 0.0f,
                0.0f, s, c, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public static Matrix4f createRotationY(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new Matrix4f(new float[]{
                c, 0.0f, s, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                -s, 0.0f, c, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public static Matrix4f createRotationZ(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new Matrix4f(new float[]{
                c, -s, 0.0f, 0.0f,
                s, c, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public static Matrix4f createPerspectiveProjection(float fov, float aspectRatio, float zNear, float zFar) {
        float f = 1.0f / (float) Math.tan(fov * 0.5f);
        float rangeInv = 1.0f / (zNear - zFar);
        return new Matrix4f(new float[]{
                f / aspectRatio, 0.0f, 0.0f, 0.0f,
                0.0f, f, 0.0f, 0.0f,
                0.0f, 0.0f, (zFar + zNear) * rangeInv, (2.0f * zFar * zNear) * rangeInv,
                0.0f, 0.0f, -1.0f, 0.0f
        });
    }

    public static Matrix4f multiply(Matrix4f a, Matrix4f b) {
        float[] out = new float[16];
        for (int row = 0; row < 4; row++) {
            int rowBase = row * 4;
            for (int col = 0; col < 4; col++) {
                out[rowBase + col] =
                        a.m[rowBase] * b.m[col]
                                + a.m[rowBase + 1] * b.m[4 + col]
                                + a.m[rowBase + 2] * b.m[8 + col]
                                + a.m[rowBase + 3] * b.m[12 + col];
            }
        }
        return new Matrix4f(out);
    }

    public float[] transform(float x, float y, float z, float w) {
        return new float[]{
                m[0] * x + m[1] * y + m[2] * z + m[3] * w,
                m[4] * x + m[5] * y + m[6] * z + m[7] * w,
                m[8] * x + m[9] * y + m[10] * z + m[11] * w,
                m[12] * x + m[13] * y + m[14] * z + m[15] * w
        };
    }
}
