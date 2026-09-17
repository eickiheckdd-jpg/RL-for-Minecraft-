package com.example.rlpvp.rl.environment;

public class TrainingArena {
    public static final float ARENA_SIZE = 30f;
    public static final float ARENA_HEIGHT = 10f;
    public static final float SPAWN_RADIUS = 10f;
    public static final float MIN_SPAWN_DIST = 5f;
    public static final float MAX_EPISODE_TIME = 60f;

    private float centerX = 0f;
    private float centerZ = 0f;
    private float centerY = 64f;

    public float getCenterX() { return centerX; }
    public float getCenterZ() { return centerZ; }
    public float getCenterY() { return centerY; }

    public float[] getAgentSpawn() {
        float angle = (float) (Math.random() * 2 * Math.PI);
        float radius = MIN_SPAWN_DIST + (float) Math.random() * (SPAWN_RADIUS - MIN_SPAWN_DIST);
        return new float[] {
            centerX + radius * (float) Math.cos(angle),
            centerY,
            centerZ + radius * (float) Math.sin(angle)
        };
    }

    public float[] getTargetSpawn(float[] agentPos) {
        float angle = (float) (Math.random() * 2 * Math.PI);
        float radius = MIN_SPAWN_DIST + (float) Math.random() * (SPAWN_RADIUS - MIN_SPAWN_DIST);
        return new float[] {
            agentPos[0] + radius * (float) Math.cos(angle),
            centerY,
            agentPos[2] + radius * (float) Math.sin(angle)
        };
    }

    public boolean isInArena(float x, float y, float z) {
        return Math.abs(x - centerX) < ARENA_SIZE / 2 &&
               Math.abs(z - centerZ) < ARENA_SIZE / 2 &&
               y > centerY - 5 && y < centerY + ARENA_HEIGHT;
    }

    public void reset() {
    }
}