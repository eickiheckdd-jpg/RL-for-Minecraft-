package com.example.rlpvp.rl.observation;

public class ObservationHistory {
    private final int historyLength;
    private final int obsDim;
    private final float[][] history;
    private int index = 0;
    private int count = 0;

    public ObservationHistory(int historyLength, int obsDim) {
        this.historyLength = historyLength;
        this.obsDim = obsDim;
        this.history = new float[historyLength][obsDim];
    }

    public void add(float[] obs) {
        System.arraycopy(obs, 0, history[index], 0, obsDim);
        index = (index + 1) % historyLength;
        if (count < historyLength) count++;
    }

    public float[] getFlattened() {
        float[] flat = new float[historyLength * obsDim];
        for (int i = 0; i < historyLength; i++) {
            int histIdx = (index - historyLength + i + historyLength) % historyLength;
            if (histIdx < count || count == historyLength) {
                System.arraycopy(history[histIdx], 0, flat, i * obsDim, obsDim);
            }
        }
        return flat;
    }

    public float[] getLatest() {
        int latestIdx = (index - 1 + historyLength) % historyLength;
        return history[latestIdx];
    }

    public int getCount() {
        return count;
    }

    public boolean isFull() {
        return count == historyLength;
    }

    public void clear() {
        index = 0;
        count = 0;
    }
}