package homework1;

import java.util.Random;

/**
 * @AI_AGENT_INSTRUCTIONS:
 * 1. STRICTLY NO EXTERNAL LIBRARIES. Do not use java.util.* except maybe Random.
 * 2. ALL computations must use the flat arrays provided in EngineData. DO NOT create 'Cube' or 'Vector3' objects.
 * 3. Use Semi-Implicit Euler for rigid body integration.
 * 4. Implement a highly optimized spatial hash or brute-force grid for SPH fluid collisions if necessary.
 */
public class PhysicsCore {
    private EngineData data;

    // Frame counter for debug output
    private long frameCount = 0;

    // Physics constants
    private static final float GRAVITY = -1500.0f;  // Acceleration on Y-axis
    private static final float FLOOR_Y = 0.0f;      // Floor position
    private static final float BOUNCE_DAMPING = 0.6f; // Energy loss on bounce
    private static final float AIR_RESISTANCE = 0.998f; // Slight damping per frame
    private static final float ANGULAR_DAMPING = 0.998f; // Slight angular damping per frame
    private static final float VELOCITY_THRESHOLD = 5.0f; // Stop jittering below this velocity
    private static final float ANGULAR_IMPULSE_FACTOR = 0.1f;
    private static final float MIN_SEPARATION_EPSILON = 0.001f;
    private final Random random = new Random(EngineData.RANDOM_SEED);

    public PhysicsCore(EngineData data) {
        this.data = data;
    }

    /**
     * Main physics simulation step using Semi-Implicit Euler Integration.
     *
     * Semi-Implicit Euler (also called Symplectic Euler):
     * 1. Update velocity first: v_new = v_old + a * dt
     * 2. Update position using new velocity: p_new = p_old + v_new * dt
     *
     * This method is more stable than explicit Euler for physics simulation.
     *
     * @param dt Delta time in seconds (typically 0.016f for 60 FPS)
     */
    public void stepSimulation(float dt) {
        // Iterate through all active entities
        for (int i = 0; i < data.count; i++) {
            // ===== STEP 1: Apply Gravity =====
            // Gravity only affects Y-axis velocity
            float ay = GRAVITY;

            // ===== STEP 2: Semi-Implicit Euler - Update Velocity First =====
            // v_new = v_old + a * dt
            data.vy[i] += ay * dt;

            // ===== STEP 3: Apply Air Resistance (Optional but stabilizing) =====
            // Slightly dampen all velocities to prevent accumulation of numerical errors
            data.vx[i] *= AIR_RESISTANCE;
            data.vy[i] *= AIR_RESISTANCE;
            data.vz[i] *= AIR_RESISTANCE;

            // Angular damping
            data.avX[i] *= ANGULAR_DAMPING;
            data.avY[i] *= ANGULAR_DAMPING;
            data.avZ[i] *= ANGULAR_DAMPING;

            // ===== STEP 4: Semi-Implicit Euler - Update Position Using New Velocity =====
            // p_new = p_old + v_new * dt
            data.xPos[i] += data.vx[i] * dt;
            data.yPos[i] += data.vy[i] * dt;
            data.zPos[i] += data.vz[i] * dt;

            // ===== STEP 4B: Semi-Implicit Euler - Update Rotation Using Angular Velocity =====
            // angle_new = angle_old + av_new * dt
            data.rotX[i] += data.avX[i] * dt;
            data.rotY[i] += data.avY[i] * dt;
            data.rotZ[i] += data.avZ[i] * dt;

            // ===== STEP 5: Floor Collision Detection and Response =====
            // Check if entity has penetrated the floor (Y < 0)
            if (data.yPos[i] < FLOOR_Y) {
                float impactVy = data.vy[i];

                // Resolution: Snap position to floor exactly
                data.yPos[i] = FLOOR_Y;

                // Response: Bounce with damping
                data.vy[i] = -data.vy[i] * BOUNCE_DAMPING;

                // Collision-induced torque: randomized angular impulse from impact speed
                float impact = Math.abs(impactVy) * ANGULAR_IMPULSE_FACTOR;
                data.avX[i] += (random.nextFloat() - 0.5f) * impact;
                data.avY[i] += (random.nextFloat() - 0.5f) * impact;
                data.avZ[i] += (random.nextFloat() - 0.5f) * impact;

                // Anti-jitter: If velocity is very small, stop the entity
                // This prevents endless tiny bounces due to floating-point precision
                if (Math.abs(data.vy[i]) < VELOCITY_THRESHOLD) {
                    data.vy[i] = 0.0f;
                }
            }
        }

        // ===== STEP 6: Inter-cube collision (sphere-sphere approximation) =====
        // Radius simplification: r = size * 0.5
        for (int i = 0; i < data.count - 1; i++) {
            float xi = data.xPos[i];
            float yi = data.yPos[i];
            float zi = data.zPos[i];
            float ri = data.size[i] * 0.5f;

            for (int j = i + 1; j < data.count; j++) {
                float dx = data.xPos[j] - xi;
                float dy = data.yPos[j] - yi;
                float dz = data.zPos[j] - zi;

                float distSq = dx * dx + dy * dy + dz * dz;
                float rj = data.size[j] * 0.5f;
                float minDist = ri + rj;
                float minDistSq = minDist * minDist;

                if (distSq >= minDistSq) {
                    continue;
                }

                float dist = (float) Math.sqrt(distSq);
                float nx;
                float ny;
                float nz;

                if (dist < MIN_SEPARATION_EPSILON) {
                    // Perfect overlap fallback normal
                    nx = 1.0f;
                    ny = 0.0f;
                    nz = 0.0f;
                } else {
                    nx = dx / dist;
                    ny = dy / dist;
                    nz = dz / dist;
                }

                // Relative velocity along collision normal
                float rvx = data.vx[j] - data.vx[i];
                float rvy = data.vy[j] - data.vy[i];
                float rvz = data.vz[j] - data.vz[i];
                float velAlongNormal = rvx * nx + rvy * ny + rvz * nz;

                // Only resolve if moving toward each other
                if (velAlongNormal < 0.0f) {
                    // Equal-mass elastic collision (restitution = 1)
                    float impulse = -velAlongNormal;
                    data.vx[i] -= impulse * nx;
                    data.vy[i] -= impulse * ny;
                    data.vz[i] -= impulse * nz;
                    data.vx[j] += impulse * nx;
                    data.vy[j] += impulse * ny;
                    data.vz[j] += impulse * nz;
                }

                // Positional correction to prevent persistent overlap/clumping
                float overlap = minDist - dist;
                if (overlap > 0.0f) {
                    float correction = Math.max(overlap * 0.5f, MIN_SEPARATION_EPSILON);
                    data.xPos[i] -= nx * correction;
                    data.yPos[i] -= ny * correction;
                    data.zPos[i] -= nz * correction;
                    data.xPos[j] += nx * correction;
                    data.yPos[j] += ny * correction;
                    data.zPos[j] += nz * correction;
                }
            }
        }
 
        // ===== STEP 7: Debug Output Every 60 Frames =====
        frameCount++;
        if (frameCount % 60 == 0 && data.count > 0) {
            // Output status of first entity (index 0) for verification
            System.out.printf("[PhysDebug] Frame: %d | Entity[0] Y: %.2f | VY: %.2f\n",
                              frameCount, data.yPos[0], data.vy[0]);
        }
    }

    // [占位符]：由云端 AI 实现基于大锤敲击的冲量爆炸
    public void applyExplosionImpulse(float impactX, float impactY, float impactZ, float force) {
        // TODO: AI Agent, calculate distance from impact point to all cubes
        // Apply explosive velocity to data.velX, data.velY, data.velZ based on inverse square law
    }
}
