package com.example.rlpvp.rl.observation;

public class MovementObservation {
    public float closingDistance;
    public float relativeMoveDir;
    public float strafeRelation;
    public float velDiff;
    public float predictedTargetMove;
    public float spacingTrend;
    public float movementEfficiency;
    public float sprintEfficiency;

    public void normalize() {
        closingDistance = Math.min(Math.abs(closingDistance) / 10f, 1f);
        relativeMoveDir = (relativeMoveDir % 360f) / 180f - 1f;
        strafeRelation = Math.min(Math.abs(strafeRelation), 1f);
        velDiff = Math.min(Math.abs(velDiff) / 10f, 1f);
        spacingTrend = Math.min(Math.abs(spacingTrend) / 5f, 1f);
    }

    public void toArray(float[] out, int offset) {
        out[offset++] = closingDistance;
        out[offset++] = relativeMoveDir;
        out[offset++] = strafeRelation;
        out[offset++] = velDiff;
        out[offset++] = predictedTargetMove;
        out[offset++] = spacingTrend;
        out[offset++] = movementEfficiency;
        out[offset++] = sprintEfficiency;
    }

    public static int dim() {
        return 8;
    }
}