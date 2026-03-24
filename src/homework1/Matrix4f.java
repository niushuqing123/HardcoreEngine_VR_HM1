package homework1;

public final class Matrix4f {
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
