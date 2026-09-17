package com.example.rlpvp.rl.curriculum;

import com.example.rlpvp.rl.environment.OpponentController;

public class CurriculumStage {
    public static final int STAGE_MOVEMENT = 0;
    public static final int STAGE_AIM = 1;
    public static final int STAGE_BASIC_COMBAT = 2;
    public static final int STAGE_STRAFING = 3;
    public static final int STAGE_COMBOS = 4;
    public static final int STAGE_ADV_MOVEMENT = 5;
    public static final int STAGE_DEFENSE = 6;
    public static final int STAGE_FULL_PVP = 7;
    public static final int STAGE_SELF_PLAY = 8;

    public static final int NUM_STAGES = 9;

    private final int stageId;
    private final String name;
    private final OpponentController.OpponentType opponentType;
    private final float[] rewardMultipliers;
    private final int minEpisodes;
    private final float winRateThreshold;
    private final float hitRateThreshold;

    public CurriculumStage(int stageId, String name, OpponentController.OpponentType opponentType,
                           float[] rewardMultipliers, int minEpisodes, float winRateThreshold, float hitRateThreshold) {
        this.stageId = stageId;
        this.name = name;
        this.opponentType = opponentType;
        this.rewardMultipliers = rewardMultipliers;
        this.minEpisodes = minEpisodes;
        this.winRateThreshold = winRateThreshold;
        this.hitRateThreshold = hitRateThreshold;
    }

    public int getStageId() { return stageId; }
    public String getName() { return name; }
    public OpponentController.OpponentType getOpponentType() { return opponentType; }
    public float[] getRewardMultipliers() { return rewardMultipliers; }
    public int getMinEpisodes() { return minEpisodes; }
    public float getWinRateThreshold() { return winRateThreshold; }
    public float getHitRateThreshold() { return hitRateThreshold; }

    public static CurriculumStage[] createDefaultStages() {
        return new CurriculumStage[] {
            new CurriculumStage(STAGE_MOVEMENT, "Movement", OpponentController.OpponentType.SCRIPTED_WEAK,
                new float[]{1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                100, 0.6f, 0.0f),
            new CurriculumStage(STAGE_AIM, "Aim", OpponentController.OpponentType.SCRIPTED_WEAK,
                new float[]{0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                100, 0.5f, 0.3f),
            new CurriculumStage(STAGE_BASIC_COMBAT, "Basic Combat", OpponentController.OpponentType.SCRIPTED_MEDIUM,
                new float[]{0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                150, 0.5f, 0.4f),
            new CurriculumStage(STAGE_STRAFING, "Strafing", OpponentController.OpponentType.SCRIPTED_MEDIUM,
                new float[]{0.3f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                150, 0.5f, 0.45f),
            new CurriculumStage(STAGE_COMBOS, "Combos", OpponentController.OpponentType.SCRIPTED_STRONG,
                new float[]{0.2f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                200, 0.5f, 0.5f),
            new CurriculumStage(STAGE_ADV_MOVEMENT, "Advanced Movement", OpponentController.OpponentType.SCRIPTED_STRONG,
                new float[]{0.2f, 0.2f, 0.3f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f},
                200, 0.55f, 0.5f),
            new CurriculumStage(STAGE_DEFENSE, "Defense", OpponentController.OpponentType.SCRIPTED_STRONG,
                new float[]{0.2f, 0.2f, 0.2f, 0.3f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f, 0.0f},
                200, 0.55f, 0.55f),
            new CurriculumStage(STAGE_FULL_PVP, "Full PvP", OpponentController.OpponentType.SCRIPTED_STRONG,
                new float[]{0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.3f, 0.5f, 1.0f, 0.0f, 0.0f},
                300, 0.6f, 0.6f),
            new CurriculumStage(STAGE_SELF_PLAY, "Self-Play", OpponentController.OpponentType.SELF_PLAY,
                new float[]{0.1f, 0.1f, 0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.5f, 1.0f, 0.0f},
                500, 0.5f, 0.5f)
        };
    }
}