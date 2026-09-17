package com.example.rlpvp.rl.observation;

public class TargetObservation {
    public float relPosX, relPosY, relPosZ;
    public float distance;
    public float horDistance;
    public float verDiff;
    public float velX, velY, velZ;
    public float horVel;
    public float verVel;
    public float health;
    public float yaw;
    public float pitch;
    public float onGround;
    public float sprinting;
    public float attackingState;
    public float hurtTime;

    public void normalize() {
        float maxDist = 50f;
        distance = Math.min(distance / maxDist, 1f);
        horDistance = Math.min(horDistance / maxDist, 1f);
        verDiff = Math.min(Math.abs(verDiff) / 10f, 1f);
        horVel = Math.min(Math.abs(horVel) / 10f, 1f);
        verVel = Math.min(Math.abs(verVel) / 10f, 1f);
        health = Math.min(health / 20f, 1f);
        yaw = (yaw % 360f) / 180f - 1f;
        pitch = pitch / 90f;
        hurtTime = Math.min(hurtTime / 10f, 1f);
    }

    public void toArray(float[] out, int offset) {
        out[offset++] = relPosX;
        out[offset++] = relPosY;
        out[offset++] = relPosZ;
        out[offset++] = distance;
        out[offset++] = horDistance;
        out[offset++] = verDiff;
        out[offset++] = velX;
        out[offset++] = velY;
        out[offset++] = velZ;
        out[offset++] = horVel;
        out[offset++] = verVel;
        out[offset++] = health;
        out[offset++] = yaw;
        out[offset++] = pitch;
        out[offset++] = onGround;
        out[offset++] = sprinting;
        out[offset++] = attackingState;
        out[offset++] = hurtTime;
    }

    public static int dim() {
        return 18;
    }
}