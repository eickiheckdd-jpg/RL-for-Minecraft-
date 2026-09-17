package com.example.rlpvp.rl.observation;

public class SelfObservation {
    public float health;
    public float absorption;
    public float posX, posY, posZ;
    public float velX, velY, velZ;
    public float horVel;
    public float verVel;
    public float yaw;
    public float pitch;
    public float onGround;
    public float sprinting;
    public float sneaking;
    public float attackCooldown;
    public float hurtTime;
    public float movementState;
    public float weaponState;

    public void normalize() {
        health = Math.min(health / 20f, 1f);
        absorption = Math.min(absorption / 20f, 1f);
        horVel = Math.min(Math.abs(horVel) / 10f, 1f);
        verVel = Math.min(Math.abs(verVel) / 10f, 1f);
        yaw = (yaw % 360f) / 180f - 1f;
        pitch = pitch / 90f;
        attackCooldown = Math.min(attackCooldown / 20f, 1f);
        hurtTime = Math.min(hurtTime / 10f, 1f);
    }

    public void toArray(float[] out, int offset) {
        out[offset++] = health;
        out[offset++] = absorption;
        out[offset++] = posX;
        out[offset++] = posY;
        out[offset++] = posZ;
        out[offset++] = velX;
        out[offset++] = velY;
        out[offset++] = velZ;
        out[offset++] = horVel;
        out[offset++] = verVel;
        out[offset++] = yaw;
        out[offset++] = pitch;
        out[offset++] = onGround;
        out[offset++] = sprinting;
        out[offset++] = sneaking;
        out[offset++] = attackCooldown;
        out[offset++] = hurtTime;
        out[offset++] = movementState;
        out[offset++] = weaponState;
    }

    public static int dim() {
        return 20;
    }
}