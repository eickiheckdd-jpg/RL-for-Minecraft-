package com.example.rlpvp.rl.curriculum;

import com.example.rlpvp.config.RLConfig;
import com.example.rlpvp.rl.environment.OpponentController;

public class CurriculumManager {
    private final CurriculumStage[] stages;
    private int currentStage = 0;
    private int episodesInStage = 0;
    private int totalEpisodes = 0;
    private float winRate = 0f;
    private float hitRate = 0f;
    private int wins = 0;
    private int hits = 0;
    private int attacks = 0;

    public CurriculumManager(RLConfig.CurriculumConfig config) {
        this.stages = createStages(config);
    }

    private CurriculumStage[] createStages(RLConfig.CurriculumConfig config) {
        return new CurriculumStage[] {
            new CurriculumStage(0, "Movement", OpponentController.OpponentType.STATIONARY,
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                config.movementStageEpisodes, config.movementWinThreshold, 0.0f),
            new CurriculumStage(1, "Aim", OpponentController.OpponentType.STRAFE_SLOW,
                new float[]{0.3f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                config.aimStageEpisodes, 0.4f, config.aimHitRateThreshold),
            new CurriculumStage(2, "Basic Combat", OpponentController.OpponentType.STRAFE_MEDIUM,
                new float[]{0.2f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                config.basicCombatStageEpisodes, config.combatWinThreshold, 0.4f),
            new CurriculumStage(3, "Strafing", OpponentController.OpponentType.STRAFE_FAST,
                new float[]{0.2f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                config.strafingStageEpisodes, 0.45f, config.strafingHitRateThreshold),
            new CurriculumStage(4, "Combos", OpponentController.OpponentType.AGGRESSIVE,
                new float[]{0.1f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                config.combosStageEpisodes, config.comboWinThreshold, 0.5f),
            new CurriculumStage(5, "Advanced Movement", OpponentController.OpponentType.SMART,
                new float[]{0.1f, 0.2f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                config.advMovementStageEpisodes, config.advMovementWinThreshold, 0.5f),
            new CurriculumStage(6, "Defense", OpponentController.OpponentType.SMART,
                new float[]{0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f},
                config.defenseStageEpisodes, config.defenseWinThreshold, 0.55f),
            new CurriculumStage(7, "Full PvP", OpponentController.OpponentType.SMART,
                new float[]{0.1f, 0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f},
                config.fullPvpStageEpisodes, config.fullPvpWinThreshold, 0.6f),
            new CurriculumStage(8, "Self-Play", OpponentController.OpponentType.SELF_PLAY,
                new float[]{0.1f, 0.1f, 0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f},
                10000, 0.5f, 0.5f)
        };
    }

    public void onEpisodeEnd(boolean won, int hits, int attacks) {
        totalEpisodes++;
        episodesInStage++;

        if (won) wins++;
        this.hits += hits;
        this.attacks += attacks;

        winRate = (float) wins / totalEpisodes;
        hitRate = attacks > 0 ? (float) this.hits / attacks : 0f;

        checkProgression();
    }

    private void checkProgression() {
        if (currentStage >= stages.length - 1) return;

        CurriculumStage stage = stages[currentStage];
        
        if (episodesInStage >= stage.getMinEpisodes()) {
            boolean shouldProgress = winRate >= stage.getWinRateThreshold() 
                && hitRate >= stage.getHitRateThreshold();

            if (shouldProgress) {
                advanceStage();
            }
        }
    }

    private void advanceStage() {
        currentStage++;
        episodesInStage = 0;
        wins = 0;
        hits = 0;
        attacks = 0;
        winRate = 0f;
        hitRate = 0f;
    }

    public CurriculumStage getCurrentStage() {
        return stages[currentStage];
    }

    public int getCurrentStageId() {
        return currentStage;
    }

    public void setStage(int stage) {
        if (stage >= 0 && stage < stages.length) {
            currentStage = stage;
            episodesInStage = 0;
            wins = 0;
            hits = 0;
            attacks = 0;
            winRate = 0f;
            hitRate = 0f;
        }
    }

    public float getWinRate() {
        return winRate;
    }

    public float getHitRate() {
        return hitRate;
    }

    public int getEpisodesInStage() {
        return episodesInStage;
    }

    public int getTotalEpisodes() {
        return totalEpisodes;
    }

    public float getProgress() {
        return (float) currentStage / (stages.length - 1);
    }

    public CurriculumStage[] getStages() {
        return stages;
    }
}