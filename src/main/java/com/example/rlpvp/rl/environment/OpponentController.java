package com.example.rlpvp.rl.environment;

import java.util.Random;

public class OpponentController {
    public enum OpponentType {
        STATIONARY,
        STRAFE_SLOW,
        STRAFE_MEDIUM,
        STRAFE_FAST,
        AGGRESSIVE,
        SMART,
        SELF_PLAY
    }

    private OpponentType type = OpponentType.STATIONARY;
    private float[] position = new float[3];
    private float[] velocity = new float[3];
    private float health = 20f;
    private float yaw = 0f;
    private float pitch = 0f;
    private int attackCooldown = 0;
    private int moveTimer = 0;
    private int movePattern = 0;
    private final Random random = new Random();

    public void setType(OpponentType type) {
        this.type = type;
    }

    public void reset(float[] spawnPos) {
        System.arraycopy(spawnPos, 0, position, 0, 3);
        velocity[0] = velocity[1] = velocity[2] = 0f;
        health = 20f;
        yaw = 0f;
        pitch = 0f;
        attackCooldown = 0;
        moveTimer = 0;
        movePattern = random.nextInt(4);
    }

    public void tick(float[] agentPos) {
        float dx = agentPos[0] - position[0];
        float dz = agentPos[2] - position[2];
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (attackCooldown > 0) attackCooldown--;

        switch (type) {
            case STATIONARY:
                stationaryBehavior(dist, dx, dz);
                break;
            case STRAFE_SLOW:
                strafeSlowBehavior(dist, dx, dz);
                break;
            case STRAFE_MEDIUM:
                strafeMediumBehavior(dist, dx, dz);
                break;
            case STRAFE_FAST:
                strafeFastBehavior(dist, dx, dz);
                break;
            case AGGRESSIVE:
                aggressiveBehavior(agentPos, dist, dx, dz);
                break;
            case SMART:
                smartBehavior(agentPos, dist, dx, dz);
                break;
            case SELF_PLAY:
                break;
        }
    }

    private void stationaryBehavior(float dist, float dx, float dz) {
        velocity[0] = velocity[2] = 0f;
        if (attackCooldown == 0 && dist < 3.5f) {
            attackCooldown = 25;
        }
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void strafeSlowBehavior(float dist, float dx, float dz) {
        if (dist > 4f) {
            velocity[0] = dx / dist * 0.1f;
            velocity[2] = dz / dist * 0.1f;
        } else {
            velocity[0] = -dz / dist * 0.08f;
            velocity[2] = dx / dist * 0.08f;
            if (attackCooldown == 0 && dist < 3.5f) {
                attackCooldown = 20;
            }
        }
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void strafeMediumBehavior(float dist, float dx, float dz) {
        moveTimer--;
        if (moveTimer <= 0) {
            movePattern = random.nextInt(4);
            moveTimer = 30 + random.nextInt(30);
        }

        if (dist > 5f) {
            velocity[0] = dx / dist * 0.15f;
            velocity[2] = dz / dist * 0.15f;
        } else if (dist < 3f) {
            velocity[0] = -dx / dist * 0.1f;
            velocity[2] = -dz / dist * 0.1f;
        } else {
            switch (movePattern) {
                case 0: velocity[0] = -dz / dist * 0.12f; velocity[2] = dx / dist * 0.12f; break;
                case 1: velocity[0] = dz / dist * 0.12f; velocity[2] = -dx / dist * 0.12f; break;
                case 2: velocity[0] = dx / dist * 0.1f; velocity[2] = dz / dist * 0.1f; break;
                case 3: velocity[0] = -dx / dist * 0.1f; velocity[2] = -dz / dist * 0.1f; break;
            }
        }

        if (attackCooldown == 0 && dist < 3.5f) {
            attackCooldown = 15;
        }
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void strafeFastBehavior(float dist, float dx, float dz) {
        moveTimer--;
        if (moveTimer <= 0) {
            movePattern = random.nextInt(5);
            moveTimer = 15 + random.nextInt(20);
        }

        if (dist > 5f) {
            velocity[0] = dx / dist * 0.2f;
            velocity[2] = dz / dist * 0.2f;
        } else if (dist < 3f) {
            velocity[0] = -dx / dist * 0.15f;
            velocity[2] = -dz / dist * 0.15f;
        } else {
            switch (movePattern) {
                case 0: velocity[0] = -dz / dist * 0.18f; velocity[2] = dx / dist * 0.18f; break;
                case 1: velocity[0] = dz / dist * 0.18f; velocity[2] = -dx / dist * 0.18f; break;
                case 2: velocity[0] = dx / dist * 0.15f; velocity[2] = dz / dist * 0.15f; break;
                case 3: velocity[0] = -dx / dist * 0.15f; velocity[2] = -dz / dist * 0.15f; break;
                case 4: velocity[0] = -dz / dist * 0.15f; velocity[2] = dx / dist * 0.15f; velocity[1] = 0.3f; break;
            }
        }

        if (attackCooldown == 0 && dist < 3.5f && random.nextFloat() < 0.4f) {
            attackCooldown = 12;
        }
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void aggressiveBehavior(float[] agentPos, float dist, float dx, float dz) {
        moveTimer--;
        if (moveTimer <= 0) {
            movePattern = random.nextInt(6);
            moveTimer = 10 + random.nextInt(20);
        }

        switch (movePattern) {
            case 0: velocity[0] = dx / dist * 0.25f; velocity[2] = dz / dist * 0.25f; break;
            case 1: velocity[0] = -dx / dist * 0.1f; velocity[2] = -dz / dist * 0.1f; break;
            case 2: velocity[0] = -dz / dist * 0.2f; velocity[2] = dx / dist * 0.2f; break;
            case 3: velocity[0] = dz / dist * 0.2f; velocity[2] = -dx / dist * 0.2f; break;
            case 4: velocity[0] = -dz / dist * 0.2f; velocity[2] = dx / dist * 0.2f; velocity[1] = 0.4f; break;
            case 5: velocity[0] = dx / dist * 0.3f; velocity[2] = dz / dist * 0.3f; break;
        }

        if (attackCooldown == 0 && dist < 3.5f && random.nextFloat() < 0.5f) {
            attackCooldown = 10;
        }
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void smartBehavior(float[] agentPos, float dist, float dx, float dz) {
        moveTimer--;
        if (moveTimer <= 0) {
            movePattern = random.nextInt(8);
            moveTimer = 8 + random.nextInt(15);
        }

        float playerHealth = 20f; // Would be passed in real implementation
        
        if (dist > 6f) {
            velocity[0] = dx / dist * 0.22f;
            velocity[2] = dz / dist * 0.22f;
        } else if (dist < 2.5f) {
            velocity[0] = -dx / dist * 0.2f;
            velocity[2] = -dz / dist * 0.2f;
            velocity[1] = 0.35f;
        } else {
            switch (movePattern) {
                case 0: velocity[0] = -dz / dist * 0.18f; velocity[2] = dx / dist * 0.18f; break;
                case 1: velocity[0] = dz / dist * 0.18f; velocity[2] = -dx / dist * 0.18f; break;
                case 2: velocity[0] = dx / dist * 0.15f; velocity[2] = dz / dist * 0.15f; break;
                case 3: velocity[0] = -dx / dist * 0.15f; velocity[2] = -dz / dist * 0.15f; break;
                case 4: velocity[0] = -dz / dist * 0.2f; velocity[2] = dx / dist * 0.2f; velocity[1] = 0.4f; break;
                case 5: velocity[0] = dx / dist * 0.2f; velocity[2] = dz / dist * 0.2f; break;
                case 6: velocity[0] = 0; velocity[2] = 0; break;
                case 7: velocity[1] = 0.42f; velocity[0] = dx / dist * 0.1f; velocity[2] = dz / dist * 0.1f; break;
            }
        }

        if (attackCooldown == 0 && dist < 3.5f && random.nextFloat() < 0.6f) {
            attackCooldown = 8;
        }
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public void takeDamage(float damage) {
        health -= damage;
    }

    public void setPosition(float[] pos) {
        System.arraycopy(pos, 0, position, 0, 3);
    }

    public float[] getPosition() {
        return position;
    }

    public float[] getVelocity() {
        return velocity;
    }

    public float getHealth() {
        return health;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isAttacking() {
        return attackCooldown > 15;
    }

    public int getAttackCooldown() {
        return attackCooldown;
    }

    public OpponentType getType() {
        return type;
    }
}