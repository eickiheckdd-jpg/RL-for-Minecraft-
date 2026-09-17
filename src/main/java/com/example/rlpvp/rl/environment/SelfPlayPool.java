package com.example.rlpvp.rl.environment;

import com.example.rlpvp.rl.network.ActorCritic;

public class SelfPlayPool {
    private final ActorCritic[] opponentPolicies;
    private final int[] opponentVersions;
    private int currentOpponent = 0;
    private int poolSize;

    public SelfPlayPool(int poolSize, int obsDim, int[] hiddenSizes, int actionDim) {
        this.poolSize = poolSize;
        this.opponentPolicies = new ActorCritic[poolSize];
        this.opponentVersions = new int[poolSize];

        for (int i = 0; i < poolSize; i++) {
            opponentPolicies[i] = new ActorCritic(obsDim, hiddenSizes, actionDim);
            opponentVersions[i] = 0;
        }
    }

    public void addPolicy(ActorCritic policy, int version) {
        int idx = currentOpponent % poolSize;
        opponentPolicies[idx].copyFrom(policy);
        opponentVersions[idx] = version;
        currentOpponent = (currentOpponent + 1) % poolSize;
    }

    public ActorCritic getRandomOpponent() {
        int idx = (int) (Math.random() * poolSize);
        return opponentPolicies[idx];
    }

    public ActorCritic getBestOpponent() {
        int bestIdx = 0;
        for (int i = 1; i < poolSize; i++) {
            if (opponentVersions[i] > opponentVersions[bestIdx]) {
                bestIdx = i;
            }
        }
        return opponentPolicies[bestIdx];
    }

    public ActorCritic getWorstOpponent() {
        int worstIdx = 0;
        for (int i = 1; i < poolSize; i++) {
            if (opponentVersions[i] < opponentVersions[worstIdx]) {
                worstIdx = i;
            }
        }
        return opponentPolicies[worstIdx];
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int[] getVersions() {
        return opponentVersions;
    }
}