package com.example.rlpvp.rl.action;

public class ActionProcessor {
    private float[] lastAction = new float[ActionSpace.TOTAL_DIM];
    private float yawAccumulator = 0f;
    private float pitchAccumulator = 0f;

    public void process(float[] actionLogits, float[] outAction) {
        int idx = 0;
        
        for (int i = 0; i < ActionSpace.MOVEMENT_DIM; i++) {
            float prob = sigmoid(actionLogits[idx++]);
            outAction[i] = prob > 0.5f ? 1f : 0f;
        }

        float yawDelta = Math.max(-1f, Math.min(1f, actionLogits[idx++]));
        float pitchDelta = Math.max(-1f, Math.min(1f, actionLogits[idx++]));
        
        outAction[ActionSpace.IDX_YAW_DELTA] = yawDelta;
        outAction[ActionSpace.IDX_PITCH_DELTA] = pitchDelta;

        float attackProb = sigmoid(actionLogits[idx]);
        outAction[ActionSpace.IDX_ATTACK] = attackProb > 0.5f ? 1f : 0f;

        System.arraycopy(outAction, 0, lastAction, 0, ActionSpace.TOTAL_DIM);
    }

    public void applyToMinecraft(Object player, float[] action) {
    }

    public float[] getLastAction() {
        return lastAction;
    }

    private float sigmoid(float x) {
        return 1f / (1f + (float) Math.exp(-x));
    }
}