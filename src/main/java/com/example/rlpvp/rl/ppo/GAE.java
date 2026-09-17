package com.example.rlpvp.rl.ppo;

public class GAE {
    private final float gamma;
    private final float lambda;

    private final float[] advantages;
    private final float[] returns;

    public GAE(int capacity, float gamma, float lambda) {
        this.gamma = gamma;
        this.lambda = lambda;
        this.advantages = new float[capacity];
        this.returns = new float[capacity];
    }

    public void compute(RolloutBuffer buffer, float lastValue, boolean lastDone) {
        float gae = 0f;

        for (int i = buffer.size - 1; i >= 0; i--) {
            int idx = buffer.get(i);
            float reward = buffer.rewards[idx];
            float value = buffer.values[idx];
            boolean done = buffer.dones[idx];

            float nextValue = (i == buffer.size - 1) ? lastValue : buffer.values[buffer.get(i + 1)];
            float nextDone = (i == buffer.size - 1) ? (lastDone ? 1f : 0f) : (buffer.dones[buffer.get(i + 1)] ? 1f : 0f);

            float delta = reward + gamma * nextValue * (1f - nextDone) - value;
            gae = delta + gamma * lambda * (1f - nextDone) * gae;

            advantages[idx] = gae;
            returns[idx] = gae + value;
        }
    }

    public void normalizeAdvantages(RolloutBuffer buffer) {
        float sum = 0f;
        for (int i = 0; i < buffer.size; i++) {
            sum += advantages[buffer.get(i)];
        }
        float mean = sum / buffer.size;

        float varSum = 0f;
        for (int i = 0; i < buffer.size; i++) {
            float diff = advantages[buffer.get(i)] - mean;
            varSum += diff * diff;
        }
        float std = (float) Math.sqrt(varSum / buffer.size + 1e-8f);

        for (int i = 0; i < buffer.size; i++) {
            int idx = buffer.get(i);
            advantages[idx] = (advantages[idx] - mean) / std;
        }
    }

    public float getAdvantage(int idx) {
        return advantages[idx];
    }

    public float getReturn(int idx) {
        return returns[idx];
    }

    public float[] getAdvantagesArray() {
        return advantages;
    }

    public float[] getReturnsArray() {
        return returns;
    }
}