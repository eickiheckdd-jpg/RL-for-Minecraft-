package com.example.rlpvp.rl.environment;

public class OpponentController {
    public enum OpponentType {
        SCRIPTED_WEAK,
        SCRIPTED_MEDIUM,
        SCRIPTED_STRONG,
        SELF_PLAY
    }

    private OpponentType type = OpponentType.SCRIPTED_WEAK;
    private float[] position = new float[3];
    private float[] velocity = new float[3];
    private float health = 20f;
    private float yaw = 0f;
    private float pitch = 0f;
    private int attackCooldown = 0;
    private int moveTimer = 0;
    private int movePattern = 0;

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
        movePattern = (int) (Math.random() * 4);
    }

    public void tick(float[] agentPos) {
        float dx = agentPos[0] - position[0];
        float dz = agentPos[2] - position[2];
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        if (attackCooldown > 0) attackCooldown--;

        switch (type) {
            case SCRIPTED_WEAK:
                weakBehavior(agentPos, dist, dx, dz);
                break;
            case SCRIPTED_MEDIUM:
                mediumBehavior(agentPos, dist, dx, dz);
                break;
            case SCRIPTED_STRONG:
                strongBehavior(agentPos, dist, dx, dz);
                break;
            case SELF_PLAY:
                break;
        }
    }

    private void weakBehavior(float[] agentPos, float dist, float dx, float dz) {
        if (dist > 4f) {
            velocity[0] = dx / dist * 0.1f;
            velocity[2] = dz / dist * 0.1f;
        } else {
            velocity[0] = velocity[2] = 0f;
            if (attackCooldown == 0 && dist < 3.5f) {
                attackCooldown = 20;
            }
        }

        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void mediumBehavior(float[] agentPos, float dist, float dx, float dz) {
        if (dist > 5f) {
            velocity[0] = dx / dist * 0.15f;
            velocity[2] = dz / dist * 0.15f;
        } else if (dist < 3f) {
            velocity[0] = -dx / dist * 0.1f;
            velocity[2] = -dz / dist * 0.1f;
        } else {
            velocity[0] = -dz / dist * 0.1f;
            velocity[2] = dx / dist * 0.1f;
        }

        if (attackCooldown == 0 && dist < 3.5f) {
            attackCooldown = 15;
        }

        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void strongBehavior(float[] agentPos, float dist, float dx, float dz) {
        moveTimer--;
        if (moveTimer <= 0) {
            movePattern = (int) (Math.random() * 6);
            moveTimer = 20 + (int) (Math.random() * 40);
        }

        switch (movePattern) {
            case 0: // Approach
                velocity[0] = dx / dist * 0.2f;
                velocity[2] = dz / dist * 0.2f;
                break;
            case 1: // Retreat
                velocity[0] = -dx / dist * 0.15f;
                velocity[2] = -dz / dist * 0.15f;
                break;
            case 2: // Strafe left
                velocity[0] = -dz / dist * 0.15f;
                velocity[2] = dx / dist * 0.15f;
                break;
            case 3: // Strafe right
                velocity[0] = dz / dist * 0.15f;
                velocity[2] = -dx / dist * 0.15f;
                break;
            case 4: // Circle
                velocity[0] = -dz / dist * 0.2f;
                velocity[2] = dx / dist * 0.2f;
                break;
            case 5: // Jump attack
                velocity[1] = 0.4f;
                velocity[0] = dx / dist * 0.1f;
                velocity[2] = dz / dist * 0.1f;
                break;
        }

        if (attackCooldown == 0 && dist < 3.5f && Math.random() < 0.3) {
            attackCooldown = 10;
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