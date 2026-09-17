package com.example.rlpvp.rl.reward;

import com.example.rlpvp.rl.action.ActionSpace;

public class RewardCalculator {
    private final RewardConfig config;
    private float lastYaw = 0f;
    private int spinCounter = 0;
    private int strafeCounter = 0;
    private int attackSpamCounter = 0;
    private float lastHealth = 20f;

    public RewardCalculator(RewardConfig config) {
        this.config = config;
    }

    public float compute(float[] prevObs, float[] currObs, float[] action, 
                         boolean hit, float damageDealt, float damageTaken,
                         boolean critical, boolean combo, boolean whiff,
                         boolean counter, boolean won, boolean died,
                         float distance, float yawError, float pitchError,
                         float timeSinceLastHit) {
        float reward = 0f;

        if (hit) {
            reward += config.hitReward;
            reward += config.damageDealtReward * Math.min(damageDealt / 10f, 1f);
            if (critical) reward += config.criticalHitBonus;
            if (combo) reward += config.comboReward;
        }

        if (damageTaken > 0) {
            reward += config.damageTakenPenalty * Math.min(damageTaken / 10f, 1f);
        }

        if (whiff) {
            reward += config.whiffPenalty;
        }

        if (counter) {
            reward += config.counterHitReward;
        }

        if (distance > 2f && distance < 5f) {
            reward += config.spacingReward * (1f - Math.abs(distance - 3.5f) / 1.5f);
        } else {
            reward += config.badSpacingPenalty;
        }

        float aimError = (float) Math.sqrt(yawError * yawError + pitchError * pitchError);
        if (aimError < 0.2f) {
            reward += config.goodTrackingReward;
        } else {
            reward += config.poorTimingPenalty;
        }

        if (won) reward += config.winReward;
        if (died) reward += config.deathPenalty;

        reward += config.survivalReward;

        if (config.enableExploitationPrevention) {
            reward += computeExploitationPenalties(action, currObs);
        }

        return reward;
    }

    private float computeExploitationPenalties(float[] action, float[] obs) {
        float penalty = 0f;

        float yawDelta = action[ActionSpace.IDX_YAW_DELTA];
        if (Math.abs(yawDelta) > 0.9f) {
            spinCounter++;
            if (spinCounter > 10) {
                penalty += config.exploitationSpinPenalty;
            }
        } else {
            spinCounter = 0;
        }

        float forward = action[ActionSpace.IDX_FORWARD];
        float backward = action[ActionSpace.IDX_BACKWARD];
        float left = action[ActionSpace.IDX_LEFT];
        float right = action[ActionSpace.IDX_RIGHT];
        
        if ((left > 0.5f && right > 0.5f) || (forward > 0.5f && backward > 0.5f)) {
            strafeCounter++;
            if (strafeCounter > 20) {
                penalty += config.exploitationStrafePenalty;
            }
        } else {
            strafeCounter = 0;
        }

        float attack = action[ActionSpace.IDX_ATTACK];
        if (attack > 0.5f) {
            attackSpamCounter++;
            if (attackSpamCounter > 5) {
                penalty += config.exploitationAttackPenalty;
            }
        } else {
            attackSpamCounter = 0;
        }

        return penalty;
    }

    public void reset() {
        spinCounter = 0;
        strafeCounter = 0;
        attackSpamCounter = 0;
        lastHealth = 20f;
    }
}