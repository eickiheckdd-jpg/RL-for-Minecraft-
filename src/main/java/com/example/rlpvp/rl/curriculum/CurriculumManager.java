package com.example.rlpvp.rl.curriculum;

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

    public CurriculumManager() {
        this.stages = CurriculumStage.createDefaultStages();
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