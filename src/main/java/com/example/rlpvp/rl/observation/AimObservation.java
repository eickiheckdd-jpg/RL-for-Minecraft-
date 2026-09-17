package com.example.rlpvp.rl.observation;

public class AimObservation {
    public float yawError;
    public float pitchError;
    public float relativeTargetAngle;
    public float targetAngularVel;
    public float recentYawError;
    public float recentPitchError;
    public float inAttackCone;
    public float aimSmoothness;

    public void normalize() {
        yawError = Math.min(Math.abs(yawError) / 180f, 1f);
        pitchError = Math.min(Math.abs(pitchError) / 90f, 1f);
        relativeTargetAngle = (relativeTargetAngle % 360f) / 180f - 1f;
        targetAngularVel = Math.min(Math.abs(targetAngularVel) / 100f, 1f);
        recentYawError = Math.min(Math.abs(recentYawError) / 180f, 1f);
        recentPitchError = Math.min(Math.abs(recentPitchError) / 90f, 1f);
    }

    public void toArray(float[] out, int offset) {
        out[offset++] = yawError;
        out[offset++] = pitchError;
        out[offset++] = relativeTargetAngle;
        out[offset++] = targetAngularVel;
        out[offset++] = recentYawError;
        out[offset++] = recentPitchError;
        out[offset++] = inAttackCone;
        out[offset++] = aimSmoothness;
    }

    public static int dim() {
        return 8;
    }
}