package com.example.rlpvp.rl.evaluation;

public class EvaluationMetrics {
    public int episodes = 0;
    public int wins = 0;
    public int losses = 0;
    public float totalDamageDealt = 0f;
    public float totalDamageTaken = 0f;
    public int totalHits = 0;
    public int totalAttacks = 0;
    public int totalWhiffs = 0;
    public int maxCombo = 0;
    public float totalFightDuration = 0f;
    public float totalCombatDistance = 0f;
    public int combatDistanceSamples = 0;
    public float totalYawError = 0f;
    public float totalPitchError = 0f;
    public int aimSamples = 0;
    public float totalMovementEfficiency = 0f;
    public int movementSamples = 0;
    public int criticalHits = 0;
    public float totalSurvivalTime = 0f;
    public float totalEpisodeReward = 0f;

    public void recordEpisode(boolean won, float damageDealt, float damageTaken,
                              int hits, int attacks, int whiffs, int combo,
                              float duration, float avgDistance, float yawError, float pitchError,
                              float movementEff, int crits, float survivalTime, float episodeReward) {
        episodes++;
        if (won) wins++; else losses++;
        totalDamageDealt += damageDealt;
        totalDamageTaken += damageTaken;
        totalHits += hits;
        totalAttacks += attacks;
        totalWhiffs += whiffs;
        maxCombo = Math.max(maxCombo, combo);
        totalFightDuration += duration;
        totalCombatDistance += avgDistance;
        combatDistanceSamples++;
        totalYawError += yawError;
        totalPitchError += pitchError;
        aimSamples++;
        totalMovementEfficiency += movementEff;
        movementSamples++;
        criticalHits += crits;
        totalSurvivalTime += survivalTime;
        totalEpisodeReward += episodeReward;
    }

    public float getWinRate() {
        return episodes > 0 ? (float) wins / episodes : 0f;
    }

    public float getAvgDamageDealt() {
        return episodes > 0 ? totalDamageDealt / episodes : 0f;
    }

    public float getAvgDamageTaken() {
        return episodes > 0 ? totalDamageTaken / episodes : 0f;
    }

    public float getHitRate() {
        return totalAttacks > 0 ? (float) totalHits / totalAttacks : 0f;
    }

    public float getWhiffRate() {
        return totalAttacks > 0 ? (float) totalWhiffs / totalAttacks : 0f;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public float getAvgFightDuration() {
        return episodes > 0 ? totalFightDuration / episodes : 0f;
    }

    public float getAvgCombatDistance() {
        return combatDistanceSamples > 0 ? totalCombatDistance / combatDistanceSamples : 0f;
    }

    public float getAvgYawError() {
        return aimSamples > 0 ? totalYawError / aimSamples : 0f;
    }

    public float getAvgPitchError() {
        return aimSamples > 0 ? totalPitchError / aimSamples : 0f;
    }

    public float getAvgMovementEfficiency() {
        return movementSamples > 0 ? totalMovementEfficiency / movementSamples : 0f;
    }

    public float getCriticalHitRate() {
        return totalHits > 0 ? (float) criticalHits / totalHits : 0f;
    }

    public float getAvgSurvivalTime() {
        return episodes > 0 ? totalSurvivalTime / episodes : 0f;
    }

    public float getAvgEpisodeReward() {
        return episodes > 0 ? totalEpisodeReward / episodes : 0f;
    }

    public void reset() {
        episodes = 0;
        wins = 0;
        losses = 0;
        totalDamageDealt = 0f;
        totalDamageTaken = 0f;
        totalHits = 0;
        totalAttacks = 0;
        totalWhiffs = 0;
        maxCombo = 0;
        totalFightDuration = 0f;
        totalCombatDistance = 0f;
        combatDistanceSamples = 0;
        totalYawError = 0f;
        totalPitchError = 0f;
        aimSamples = 0;
        totalMovementEfficiency = 0f;
        movementSamples = 0;
        criticalHits = 0;
        totalSurvivalTime = 0f;
        totalEpisodeReward = 0f;
    }
}