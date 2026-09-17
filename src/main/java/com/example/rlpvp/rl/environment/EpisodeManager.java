package com.example.rlpvp.rl.environment;

import com.example.rlpvp.rl.action.ActionSpace;
import com.example.rlpvp.rl.reward.RewardCalculator;
import com.example.rlpvp.rl.reward.RewardConfig;

public class EpisodeManager {
    private final TrainingArena arena;
    private final RewardCalculator rewardCalculator;
    private final RewardConfig rewardConfig;

    private boolean inEpisode = false;
    private float episodeReward = 0f;
    private int episodeSteps = 0;
    private int maxEpisodeSteps = 1200;

    private float agentHealth = 20f;
    private float targetHealth = 20f;
    private float[] agentPos = new float[3];
    private float[] targetPos = new float[3];
    private float[] prevAgentPos = new float[3];
    private float[] prevTargetPos = new float[3];

    private boolean lastHit = false;
    private float lastDamageDealt = 0f;
    private float lastDamageTaken = 0f;
    private boolean lastCritical = false;
    private boolean lastCombo = false;
    private boolean lastWhiff = false;
    private boolean lastCounter = false;
    private int comboCount = 0;

    public EpisodeManager() {
        this.arena = new TrainingArena();
        this.rewardConfig = new RewardConfig();
        this.rewardCalculator = new RewardCalculator(rewardConfig);
    }

    public void startEpisode() {
        inEpisode = true;
        episodeReward = 0f;
        episodeSteps = 0;
        agentHealth = 20f;
        targetHealth = 20f;
        comboCount = 0;

        float[] agentSpawn = arena.getAgentSpawn();
        System.arraycopy(agentSpawn, 0, agentPos, 0, 3);
        System.arraycopy(agentSpawn, 0, prevAgentPos, 0, 3);

        float[] targetSpawn = arena.getTargetSpawn(agentPos);
        System.arraycopy(targetSpawn, 0, targetPos, 0, 3);
        System.arraycopy(targetSpawn, 0, prevTargetPos, 0, 3);

        rewardCalculator.reset();
    }

    public void endEpisode() {
        inEpisode = false;
    }

    public float step(float[] action, float[] obs) {
        if (!inEpisode) return 0f;

        episodeSteps++;
        System.arraycopy(agentPos, 0, prevAgentPos, 0, 3);
        System.arraycopy(targetPos, 0, prevTargetPos, 0, 3);

        boolean hit = false;
        float damageDealt = 0f;
        float damageTaken = 0f;
        boolean critical = false;
        boolean whiff = false;
        boolean counter = false;

        float distance = (float) Math.sqrt(
            (agentPos[0] - targetPos[0]) * (agentPos[0] - targetPos[0]) +
            (agentPos[1] - targetPos[1]) * (agentPos[1] - targetPos[1]) +
            (agentPos[2] - targetPos[2]) * (agentPos[2] - targetPos[2])
        );

        float yawError = 0f;
        float pitchError = 0f;

        if (action[ActionSpace.IDX_ATTACK] > 0.5f && distance < 3.5f) {
            hit = true;
            damageDealt = (float) (Math.random() * 6 + 1);
            if (Math.random() < 0.15) {
                critical = true;
                damageDealt *= 1.5f;
            }
            targetHealth -= damageDealt;
            comboCount++;
            if (comboCount > 1) {
                lastCombo = true;
            }
        } else if (action[ActionSpace.IDX_ATTACK] > 0.5f) {
            whiff = true;
            comboCount = 0;
        } else {
            comboCount = 0;
        }

        if (Math.random() < 0.1f && distance < 4f) {
            damageTaken = (float) (Math.random() * 4 + 1);
            agentHealth -= damageTaken;
            if (hit) {
                counter = true;
            }
        }

        boolean won = targetHealth <= 0;
        boolean died = agentHealth <= 0;
        boolean timeout = episodeSteps >= maxEpisodeSteps;

        float reward = rewardCalculator.compute(
            prevAgentPos, agentPos, action,
            hit, damageDealt, damageTaken,
            critical, lastCombo, whiff, counter, won, died,
            distance, yawError, pitchError, 0f
        );

        episodeReward += reward;

        if (won || died || timeout) {
            endEpisode();
        }

        return reward;
    }

    public boolean isInEpisode() {
        return inEpisode;
    }

    public float getEpisodeReward() {
        return episodeReward;
    }

    public int getEpisodeSteps() {
        return episodeSteps;
    }

    public float getAgentHealth() {
        return agentHealth;
    }

    public float getTargetHealth() {
        return targetHealth;
    }

    public float[] getAgentPos() {
        return agentPos;
    }

    public float[] getTargetPos() {
        return targetPos;
    }

    public TrainingArena getArena() {
        return arena;
    }

    public RewardConfig getRewardConfig() {
        return rewardConfig;
    }

    public void setAgentPos(float[] pos) {
        System.arraycopy(pos, 0, agentPos, 0, 3);
    }

    public void setTargetPos(float[] pos) {
        System.arraycopy(pos, 0, targetPos, 0, 3);
    }
}