package com.example.rlpvp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class RLConfig {

    public static RLConfig INSTANCE;

    public PPOConfig ppo = new PPOConfig();
    public RewardConfig reward = new RewardConfig();
    public CurriculumConfig curriculum = new CurriculumConfig();
    public TrainingConfig training = new TrainingConfig();
    public NetworkConfig network = new NetworkConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of(MinecraftClient.getInstance().runDirectory.getAbsolutePath(), "config", "rlpvp.json");

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, RLConfig.class);
                if (INSTANCE == null) INSTANCE = new RLConfig();
            } else {
                INSTANCE = new RLConfig();
                save();
            }
        } catch (Exception e) {
            System.err.println("[RL-PvP] Failed to load config: " + e.getMessage());
            INSTANCE = new RLConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(INSTANCE);
            Files.writeString(CONFIG_PATH, json);
        } catch (Exception e) {
            System.err.println("[RL-PvP] Failed to save config: " + e.getMessage());
        }
    }

    public static class PPOConfig {
        public float learningRate = 3e-4f;
        public float clipEpsilon = 0.2f;
        public float entropyCoef = 0.01f;
        public float valueCoef = 0.5f;
        public float maxGradNorm = 0.5f;
        public int ppoEpochs = 4;
        public int minibatchSize = 256;
        public int rolloutLength = 2048;
        public float gamma = 0.99f;
        public float gaeLambda = 0.95f;
    }

    public static class RewardConfig {
        public float hitReward = 2.0f;
        public float damageDealtReward = 0.5f;
        public float criticalHitBonus = 1.0f;
        public float comboReward = 0.5f;
        public float goodTrackingReward = 0.2f;
        public float spacingReward = 0.3f;
        public float counterHitReward = 1.0f;
        public float whiffPunishReward = 0.5f;
        public float survivalReward = 0.01f;
        public float winReward = 10.0f;

        public float damageTakenPenalty = -2.0f;
        public float whiffPenalty = -0.5f;
        public float badSpacingPenalty = -0.2f;
        public float poorTimingPenalty = -0.2f;
        public float randomMovementPenalty = -0.02f;
        public float excessiveJumpPenalty = -0.05f;
        public float loseTargetPenalty = -0.5f;
        public float deathPenalty = -15.0f;
        public float missedOpportunityPenalty = -0.2f;

        public float exploitationSpinPenalty = -1.0f;
        public float exploitationStrafePenalty = -0.5f;
        public float exploitationAttackPenalty = -0.3f;
        public float exploitationDamagePenalty = -1.0f;

        public boolean enableExploitationPrevention = true;
    }

    public static class CurriculumConfig {
        public int movementStageEpisodes = 200;
        public int aimStageEpisodes = 200;
        public int basicCombatStageEpisodes = 300;
        public int strafingStageEpisodes = 300;
        public int combosStageEpisodes = 400;
        public int advMovementStageEpisodes = 400;
        public int defenseStageEpisodes = 400;
        public int fullPvpStageEpisodes = 600;
        public int selfPlayStageEpisodes = 1000;

        public float movementWinThreshold = 0.6f;
        public float aimHitRateThreshold = 0.35f;
        public float combatWinThreshold = 0.5f;
        public float strafingHitRateThreshold = 0.45f;
        public float comboWinThreshold = 0.5f;
        public float advMovementWinThreshold = 0.55f;
        public float defenseWinThreshold = 0.55f;
        public float fullPvpWinThreshold = 0.6f;
    }

    public static class TrainingConfig {
        public int historyLength = 8;
        public int evalEpisodes = 20;
        public int evalInterval = 500;
        public int checkpointInterval = 1000;
        public boolean autoSave = true;
        public boolean renderDebug = false;
    }

    public static class NetworkConfig {
        public int[] hiddenSizes = {256, 128, 64};
        public String activation = "relu";
    }

    public RLConfig getInstance() {
        return INSTANCE;
    }
}