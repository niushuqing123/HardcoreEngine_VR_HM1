package homework1;

public class PhysicsCore {
    private final EngineData data;
    private final RigidBodyPhysics rigidBodyPhysics;
    private final int[] bodyHandles;
    private int mappedCount = 0;
    private long frameCount = 0;

    private static final float GRAVITY = -1500.0f;
    private static final float MIN_HALF_EXTENT = 0.001f;
    private static final float GROUND_HALF_HEIGHT = 25.0f;
    private static final float GROUND_HALF_EXTENT = 5000.0f;
    private static final float EXPLOSION_RADIUS = 1200.0f;
    private static final float EXPLOSION_RADIUS_SQ = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
    private static final float MIN_EXPLOSION_DIST = 0.001f;
    private static final float EXPLOSION_DISTANCE_OFFSET = 50.0f;
    private static final float EPSILON = 1e-6f;

    public PhysicsCore(EngineData data) {
        this.data = data;
        this.bodyHandles = new int[data.MAX_ENTITIES];
        for (int i = 0; i < bodyHandles.length; i++) {
            bodyHandles[i] = -1;
        }
        RigidBodyPhysics.Config config = new RigidBodyPhysics.Config();
        config.gravity = new RigidBodyPhysics.Vec3(0.0f, GRAVITY, 0.0f);
        this.rigidBodyPhysics = new RigidBodyPhysics(config);
        addStaticGroundBody();
        syncBodiesToPhysics();
    }

    public void stepSimulation(float dt) {
        if (dt <= 0.0f) {
            return;
        }

        syncBodiesToPhysics();
        rigidBodyPhysics.step(dt);
        writeBackBodyStates();

        frameCount++;
        if (frameCount % 60 == 0 && data.count > 0) {
            System.out.printf("[PhysDebug] Frame: %d | Entity[0] Y: %.2f | VY: %.2f\n",
                              frameCount, data.yPos[0], data.vy[0]);
        }
    }

    public void applyExplosionImpulse(float impactX, float impactY, float impactZ, float force) {
        if (force <= 0.0f) {
            return;
        }

        syncBodiesToPhysics();
        for (int i = 0; i < mappedCount; i++) {
            if (data.isStatic[i] || data.isKinematic[i]) {
                continue;
            }
            int handle = bodyHandles[i];
            RigidBodyPhysics.BodyState state = rigidBodyPhysics.getBodyState(handle);
            if (state == null) {
                continue;
            }

            float dx = state.position.x - impactX;
            float dy = state.position.y - impactY;
            float dz = state.position.z - impactZ;
            float distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > EXPLOSION_RADIUS_SQ) {
                continue;
            }

            float dist = (float) Math.sqrt(Math.max(distSq, MIN_EXPLOSION_DIST));
            float nx;
            float ny;
            float nz;
            if (dist <= MIN_EXPLOSION_DIST) {
                nx = 0.0f;
                ny = 1.0f;
                nz = 0.0f;
            } else {
                nx = dx / dist;
                ny = dy / dist;
                nz = dz / dist;
            }

            float linearFalloff = 1.0f - (dist / EXPLOSION_RADIUS);
            if (linearFalloff <= 0.0f) {
                continue;
            }
            float deltaV = (force * linearFalloff) / (dist + EXPLOSION_DISTANCE_OFFSET);
            float upBoost = deltaV * 0.25f;

            RigidBodyPhysics.Vec3 newLinear = new RigidBodyPhysics.Vec3(
                    state.linearVelocity.x + nx * deltaV,
                    state.linearVelocity.y + ny * deltaV + upBoost,
                    state.linearVelocity.z + nz * deltaV
            );
            rigidBodyPhysics.setBodyVelocity(handle, newLinear, state.angularVelocity);
            rigidBodyPhysics.wakeBody(handle);
        }

        writeBackBodyStates();
    }

    /** 外部调用：每帧驱动 kinematic 刚体到新的 Y 轴旋转角度（直接改写物理变换） */
    public void driveKinematicBody(int entityIdx, float yAngle, float angularSpeed) {
        if (entityIdx < 0 || entityIdx >= mappedCount) return;
        if (!data.isKinematic[entityIdx]) return;
        int handle = bodyHandles[entityIdx];
        if (handle < 0) return;
        float halfAngle = yAngle * 0.5f;
        float sy = (float) Math.sin(halfAngle);
        float cy = (float) Math.cos(halfAngle);
        RigidBodyPhysics.Vec3 pos = new RigidBodyPhysics.Vec3(
                data.xPos[entityIdx], data.yPos[entityIdx], data.zPos[entityIdx]);
        RigidBodyPhysics.Quat rot = new RigidBodyPhysics.Quat(0.0f, sy, 0.0f, cy);
        rigidBodyPhysics.setBodyTransform(handle, pos, rot);
        // 关键修复：设置角速度，使碰撞求解器能正确计算接触点相对速度
        RigidBodyPhysics.Vec3 linVel = new RigidBodyPhysics.Vec3(0.0f, 0.0f, 0.0f);
        RigidBodyPhysics.Vec3 angVel = new RigidBodyPhysics.Vec3(0.0f, angularSpeed, 0.0f);
        rigidBodyPhysics.setBodyVelocity(handle, linVel, angVel);
    }

    /** 驱动绕 Z 轴旋转且沿轨道移动的 kinematic 刚体。 */
    public void driveKinematicBodyZ(int entityIdx,
                                    float x, float y, float z,
                                    float zAngle,
                                    float linearVx, float linearVy, float linearVz,
                                    float angularSpeedZ) {
        if (entityIdx < 0 || entityIdx >= mappedCount) return;
        if (!data.isKinematic[entityIdx]) return;
        int handle = bodyHandles[entityIdx];
        if (handle < 0) return;
        float halfAngle = zAngle * 0.5f;
        float sz = (float) Math.sin(halfAngle);
        float cz = (float) Math.cos(halfAngle);
        RigidBodyPhysics.Vec3 pos = new RigidBodyPhysics.Vec3(x, y, z);
        RigidBodyPhysics.Quat rot = new RigidBodyPhysics.Quat(0.0f, 0.0f, sz, cz);
        rigidBodyPhysics.setBodyTransform(handle, pos, rot);
        RigidBodyPhysics.Vec3 linVel = new RigidBodyPhysics.Vec3(linearVx, linearVy, linearVz);
        RigidBodyPhysics.Vec3 angVel = new RigidBodyPhysics.Vec3(0.0f, 0.0f, angularSpeedZ);
        rigidBodyPhysics.setBodyVelocity(handle, linVel, angVel);
    }

    public void addStaticBoxBody(float centerX, float centerY, float centerZ,
                                 float halfExtentX, float halfExtentY, float halfExtentZ) {
        RigidBodyPhysics.BodyDesc desc = new RigidBodyPhysics.BodyDesc();
        desc.motionType = RigidBodyPhysics.MotionType.STATIC;
        desc.shapeType = RigidBodyPhysics.ShapeType.BOX;
        desc.boxHalfExtents = new RigidBodyPhysics.Vec3(halfExtentX, halfExtentY, halfExtentZ);
        desc.position = new RigidBodyPhysics.Vec3(centerX, centerY, centerZ);
        desc.friction = 0.8f;
        rigidBodyPhysics.addBody(desc);
    }

    private void addStaticGroundBody() {
        RigidBodyPhysics.BodyDesc ground = new RigidBodyPhysics.BodyDesc();
        ground.motionType = RigidBodyPhysics.MotionType.STATIC;
        ground.shapeType = RigidBodyPhysics.ShapeType.BOX;
        ground.boxHalfExtents = new RigidBodyPhysics.Vec3(GROUND_HALF_EXTENT, GROUND_HALF_HEIGHT, GROUND_HALF_EXTENT);
        ground.position = new RigidBodyPhysics.Vec3(0.0f, -GROUND_HALF_HEIGHT, 0.0f);
        ground.friction = 0.8f;
        rigidBodyPhysics.addBody(ground);
    }

    private void syncBodiesToPhysics() {
        while (mappedCount < data.count) {
            int i = mappedCount;
            float halfX = Math.max(data.sizeX[i] * 0.5f, MIN_HALF_EXTENT);
            float halfY = Math.max(data.sizeY[i] * 0.5f, MIN_HALF_EXTENT);
            float halfZ = Math.max(data.sizeZ[i] * 0.5f, MIN_HALF_EXTENT);

            RigidBodyPhysics.BodyDesc desc = new RigidBodyPhysics.BodyDesc();
            RigidBodyPhysics.MotionType mtype;
            if (data.isStatic[i]) {
                mtype = RigidBodyPhysics.MotionType.STATIC;
            } else if (data.isKinematic[i]) {
                mtype = RigidBodyPhysics.MotionType.KINEMATIC;
            } else {
                mtype = RigidBodyPhysics.MotionType.DYNAMIC;
            }
            desc.motionType = mtype;
            if (data.isSphere[i]) {
                desc.shapeType = RigidBodyPhysics.ShapeType.SPHERE;
                desc.sphereRadius = data.size[i] * 0.5f;
            } else {
                desc.shapeType = RigidBodyPhysics.ShapeType.BOX;
                desc.boxHalfExtents = new RigidBodyPhysics.Vec3(halfX, halfY, halfZ);
            }
            desc.position = new RigidBodyPhysics.Vec3(
                data.xPos[i],
                data.yPos[i],
                data.zPos[i]
            );
            desc.linearVelocity = new RigidBodyPhysics.Vec3(data.vx[i], data.vy[i], data.vz[i]);
            desc.angularVelocity = new RigidBodyPhysics.Vec3(data.avX[i], data.avY[i], data.avZ[i]);
            desc.friction = 0.6f;
            desc.linearDamping = 0.05f;
            desc.angularDamping = 0.05f;

            bodyHandles[i] = rigidBodyPhysics.addBody(desc);
            mappedCount++;
        }
    }

    private void writeBackBodyStates() {
        for (int i = 0; i < mappedCount; i++) {
            // Kinematic bodies are driven externally; skip write-back to avoid overwriting
            if (data.isKinematic[i]) {
                continue;
            }
            RigidBodyPhysics.BodyState state = rigidBodyPhysics.getBodyState(bodyHandles[i]);
            if (state == null) {
                continue;
            }

            data.xPos[i] = state.position.x;
            data.yPos[i] = state.position.y;
            data.zPos[i] = state.position.z;
            data.vx[i] = state.linearVelocity.x;
            data.vy[i] = state.linearVelocity.y;
            data.vz[i] = state.linearVelocity.z;
            data.avX[i] = state.angularVelocity.x;
            data.avY[i] = state.angularVelocity.y;
            data.avZ[i] = state.angularVelocity.z;

            eulerFromQuatXYZ(state.rotation, i);
        }
    }

    private void eulerFromQuatXYZ(RigidBodyPhysics.Quat q, int index) {
        float x = q.x;
        float y = q.y;
        float z = q.z;
        float w = q.w;

        float sinrCosp = 2.0f * (w * x + y * z);
        float cosrCosp = 1.0f - 2.0f * (x * x + y * y);
        data.rotX[index] = (float) Math.atan2(sinrCosp, cosrCosp);

        float sinp = 2.0f * (w * y - z * x);
        if (Math.abs(sinp) >= 1.0f) {
            data.rotY[index] = (float) Math.copySign(Math.PI / 2.0, sinp);
        } else {
            data.rotY[index] = (float) Math.asin(sinp);
        }

        float sinyCosp = 2.0f * (w * z + x * y);
        float cosyCosp = 1.0f - 2.0f * (y * y + z * z);
        data.rotZ[index] = (float) Math.atan2(sinyCosp, cosyCosp);
        if (Math.abs(data.rotX[index]) < EPSILON) data.rotX[index] = 0.0f;
        if (Math.abs(data.rotY[index]) < EPSILON) data.rotY[index] = 0.0f;
        if (Math.abs(data.rotZ[index]) < EPSILON) data.rotZ[index] = 0.0f;
    }
}
