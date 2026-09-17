package com.example.rlpvp.rl.ppo;

public class RolloutBuffer {
    public final int capacity;
    public final int obsDim;
    public final int actionDim;

    public final float[][] observations;
    public final float[][] actions;
    public final float[] rewards;
    public final float[] values;
    public final float[] logProbs;
    public final boolean[] dones;

    public int size = 0;
    public int pointer = 0;

    public RolloutBuffer(int capacity, int obsDim, int actionDim) {
        this.capacity = capacity;
        this.obsDim = obsDim;
        this.actionDim = actionDim;

        this.observations = new float[capacity][obsDim];
        this.actions = new float[capacity][actionDim];
        this.rewards = new float[capacity];
        this.values = new float[capacity];
        this.logProbs = new float[capacity];
        this.dones = new boolean[capacity];
    }

    public void add(float[] obs, float[] action, float reward, float value, float logProb, boolean done) {
        System.arraycopy(obs, 0, observations[pointer], 0, obsDim);
        System.arraycopy(action, 0, actions[pointer], 0, actionDim);
        rewards[pointer] = reward;
        values[pointer] = value;
        logProbs[pointer] = logProb;
        dones[pointer] = done;

        pointer = (pointer + 1) % capacity;
        if (size < capacity) size++;
    }

    public void clear() {
        size = 0;
        pointer = 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int get(int idx) {
        return (pointer - size + idx + capacity) % capacity;
    }

    public float[] getObs(int idx) {
        return observations[get(idx)];
    }

    public float[] getAction(int idx) {
        return actions[get(idx)];
    }

    public float getReward(int idx) {
        return rewards[get(idx)];
    }

    public float getValue(int idx) {
        return values[get(idx)];
    }

    public float getLogProb(int idx) {
        return logProbs[get(idx)];
    }

    public boolean getDone(int idx) {
        return dones[get(idx)];
    }
}