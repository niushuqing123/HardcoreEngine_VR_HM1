package homework1;

public class EngineData {
    // 强行用扁平数组存储所有 3D 数据，极其硬核
    public final int MAX_ENTITIES = 1000;
    public int count = 0;
    
    // index 代表方块的 ID
    public float[] xPos = new float[MAX_ENTITIES];
    public float[] yPos = new float[MAX_ENTITIES];
    public float[] zPos = new float[MAX_ENTITIES];
    public float[] size = new float[MAX_ENTITIES];
    public int[] colors = new int[MAX_ENTITIES];
    
    // 物理引擎所需的速度数组 (扁平化)
    public float[] vx = new float[MAX_ENTITIES];
    public float[] vy = new float[MAX_ENTITIES];
    public float[] vz = new float[MAX_ENTITIES];
    
    // 提供一个快速添加方块的方法
    public void addCube(float x, float y, float z, float s, int hexColor) {
        if (count >= MAX_ENTITIES) return; // 防止越界
        xPos[count] = x;
        yPos[count] = y;
        zPos[count] = z;
        size[count] = s;
        colors[count] = hexColor;
        count++;
    }
}