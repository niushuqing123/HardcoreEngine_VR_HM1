package homework1;

import java.util.Random;

public class EngineData {
    // 强行用扁平数组存储所有 3D 数据，极其硬核
    public final int MAX_ENTITIES = 1000;
    public int count = 0;
    private static final float INITIAL_ROTATION_RANGE = 0.06f;
    private static final float INITIAL_ANGULAR_VEL_RANGE = 0.6f;
    public static final long RANDOM_SEED = 12345L;
    private final Random random = new Random(RANDOM_SEED);
    
    // index 代表方块的 ID
    public float[] xPos = new float[MAX_ENTITIES];
    public float[] yPos = new float[MAX_ENTITIES];
    public float[] zPos = new float[MAX_ENTITIES];
    public float[] size = new float[MAX_ENTITIES];
    public float[] sizeX = new float[MAX_ENTITIES];
    public float[] sizeY = new float[MAX_ENTITIES];
    public float[] sizeZ = new float[MAX_ENTITIES];
    public int[] colors = new int[MAX_ENTITIES];
    
    // 物理引擎所需的速度数组 (扁平化)
    public float[] vx = new float[MAX_ENTITIES];
    public float[] vy = new float[MAX_ENTITIES];
    public float[] vz = new float[MAX_ENTITIES];

    // 角度数组（弧度）
    public float[] rotX = new float[MAX_ENTITIES];
    public float[] rotY = new float[MAX_ENTITIES];
    public float[] rotZ = new float[MAX_ENTITIES];

    // 角速度数组（弧度/秒）
    public float[] avX = new float[MAX_ENTITIES];
    public float[] avY = new float[MAX_ENTITIES];
    public float[] avZ = new float[MAX_ENTITIES];
    public boolean[] isStatic = new boolean[MAX_ENTITIES];
    public boolean[] isSphere = new boolean[MAX_ENTITIES];
    public boolean[] isKinematic = new boolean[MAX_ENTITIES];

    // 提供一个快速添加方块的方法
    public void addCube(float x, float y, float z, float s, int hexColor) {
        addBox(x, y, z, s, s, s, hexColor, true, false);
    }
    
    // 向前兼容无isStatic参数
    public void addCube(float x, float y, float z, float s, int hexColor, boolean withInitialPerturbation) {
        addBox(x, y, z, s, s, s, hexColor, withInitialPerturbation, false);
    }

    public void addCube(float x, float y, float z, float s, int hexColor, boolean withInitialPerturbation, boolean isStaticBlock) {
        addBox(x, y, z, s, s, s, hexColor, withInitialPerturbation, isStaticBlock);
    }

    public void addBox(float x, float y, float z,
                       float sx, float sy, float sz,
                       int hexColor, boolean withInitialPerturbation, boolean isStaticBlock) {
        addGenericShape(x, y, z, sx, sy, sz, hexColor, withInitialPerturbation, isStaticBlock, false, false, false);
    }

    public int addKinematicBox(float x, float y, float z,
                               float sx, float sy, float sz,
                               int hexColor) {
        addGenericShape(x, y, z, sx, sy, sz, hexColor, false, false, true, false, false);
        return count - 1;
    }

    public void addSphere(float x, float y, float z, float radius, int hexColor, boolean isStaticBlock) {
        addGenericShape(x, y, z, radius * 2, radius * 2, radius * 2, hexColor, false, isStaticBlock, false, true, false);
    }

    public int addRotatorBox(float x, float y, float z,
                             float sx, float sy, float sz, int hexColor) {
        addGenericShape(x, y, z, sx, sy, sz, hexColor, false, false, true, false, false);
        return count - 1;
    }

    private void addGenericShape(float x, float y, float z,
                                float sx, float sy, float sz,
                                int hexColor, boolean withInitialPerturbation,
                                boolean isStaticBlock, boolean isKinematicBlock,
                                boolean sphereMode, boolean unused) {
        if (count >= MAX_ENTITIES) return; // 防止越界
        xPos[count] = x;
        yPos[count] = y;
        zPos[count] = z;
        size[count] = Math.max(sx, Math.max(sy, sz));
        sizeX[count] = sx;
        sizeY[count] = sy;
        sizeZ[count] = sz;
        colors[count] = hexColor;
        isStatic[count] = isStaticBlock;
        isSphere[count] = sphereMode;
        isKinematic[count] = isKinematicBlock;

        if (withInitialPerturbation) {
            // 初始微扰：避免所有方块完美对齐
            rotX[count] = (random.nextFloat() - 0.5f) * INITIAL_ROTATION_RANGE;
            rotY[count] = (random.nextFloat() - 0.5f) * INITIAL_ROTATION_RANGE;
            rotZ[count] = (random.nextFloat() - 0.5f) * INITIAL_ROTATION_RANGE;
            avX[count] = (random.nextFloat() - 0.5f) * INITIAL_ANGULAR_VEL_RANGE;
            avY[count] = (random.nextFloat() - 0.5f) * INITIAL_ANGULAR_VEL_RANGE;
            avZ[count] = (random.nextFloat() - 0.5f) * INITIAL_ANGULAR_VEL_RANGE;
        } else {
            rotX[count] = 0.0f;
            rotY[count] = 0.0f;
            rotZ[count] = 0.0f;
            avX[count] = 0.0f;
            avY[count] = 0.0f;
            avZ[count] = 0.0f;
        }
        count++;
    }
}
