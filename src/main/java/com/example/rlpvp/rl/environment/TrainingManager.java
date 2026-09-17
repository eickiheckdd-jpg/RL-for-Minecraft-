package com.example.rlpvp.rl.environment;

import com.example.rlpvp.RLMain;
import com.example.rlpvp.config.RLConfig;
import com.example.rlpvp.rl.minecraft.CarpetIntegration;
import com.example.rlpvp.rl.minecraft.MinecraftIntegration;
import com.example.rlpvp.rl.minecraft.ActionExecutor;
import com.example.rlpvp.rl.minecraft.CombatEventHandler;
import com.example.rlpvp.rl.minecraft.TrainingArena;
import net.minecraft.util.math.Vec3d;
import com.example.rlpvp.rl.network.ActorCritic;
import com.example.rlpvp.rl.ppo.RolloutBuffer;
import com.example.rlpvp.rl.ppo.GAE;
import com.example.rlpvp.rl.ppo.PPO;
import com.example.rlpvp.rl.observation.ObservationEncoder;
import com.example.rlpvp.rl.observation.ObservationHistory;
import com.example.rlpvp.rl.curriculum.CurriculumManager;
import com.example.rlpvp.rl.curriculum.CurriculumStage;
import com.example.rlpvp.rl.checkpoint.CheckpointManager;
import com.example.rlpvp.rl.evaluation.Evaluator;
import com.example.rlpvp.rl.evaluation.EvaluationMetrics;
import com.example.rlpvp.rl.reward.RewardCalculator;
import com.example.rlpvp.rl.reward.RewardConfig;
import com.example.rlpvp.rl.action.ActionProcessor;
import com.example.rlpvp.rl.action.ActionSpace;

public class TrainingManager {

    private boolean enabled = false;
    private int currentStage = 0;
    private long trainingStep = 0;
    private int episodeCount = 0;
    private float episodeReward = 0f;
    private float winRate = 0f;
    private float hitRate = 0f;

    private final MinecraftIntegration minecraftIntegration;
    private final ActionExecutor actionExecutor;
    private final CombatEventHandler combatEventHandler;
    private final TrainingArena trainingArena;
    private final CarpetIntegration carpetIntegration;

    private ActorCritic policy;
    private RolloutBuffer rolloutBuffer;
    private GAE gae;
    private PPO ppo;
    private CurriculumManager curriculum;
    private Evaluator evaluator;

    private ObservationEncoder encoder;
    private ObservationHistory history;
    private ActionProcessor actionProcessor;
    private RewardCalculator rewardCalculator;

    private float[] currentObs;
    private float[] currentAction;
    private float[] flatObs;

    private int obsDim;
    private int actionDim;
    private int historyLength;
    private int rolloutLength;
    private int stepsSinceUpdate = 0;

    private int episodeHits = 0;
    private int episodeAttacks = 0;
    private int episodeWhiffs = 0;
    private int episodeCombo = 0;
    private float episodeDamageDealt = 0f;
    private float episodeDamageTaken = 0f;

    private boolean evaluating = false;

    public TrainingManager(MinecraftIntegration minecraftIntegration, ActionExecutor actionExecutor,
                           CombatEventHandler combatEventHandler, TrainingArena trainingArena) {
        this.minecraftIntegration = minecraftIntegration;
        this.actionExecutor = actionExecutor;
        this.combatEventHandler = combatEventHandler;
        this.trainingArena = trainingArena;
        this.carpetIntegration = new CarpetIntegration();

        this.obsDim = ObservationEncoder.getObsDim();
        this.actionDim = ActionSpace.TOTAL_DIM;
        this.historyLength = RLConfig.INSTANCE.training.historyLength;
        this.rolloutLength = RLConfig.INSTANCE.ppo.rolloutLength;

        int[] hiddenSizes = RLConfig.INSTANCE.network.hiddenSizes;
        
        this.policy = new ActorCritic(obsDim * historyLength, hiddenSizes, actionDim);
        this.rolloutBuffer = new RolloutBuffer(rolloutLength, obsDim * historyLength, actionDim);
        this.gae = new GAE(rolloutLength, RLConfig.INSTANCE.ppo.gamma, RLConfig.INSTANCE.ppo.gaeLambda);
        this.ppo = new PPO(policy, RLConfig.INSTANCE.ppo.learningRate, RLConfig.INSTANCE.ppo.clipEpsilon, 
                          RLConfig.INSTANCE.ppo.entropyCoef, RLConfig.INSTANCE.ppo.valueCoef, 
                          RLConfig.INSTANCE.ppo.maxGradNorm, RLConfig.INSTANCE.ppo.ppoEpochs, RLConfig.INSTANCE.ppo.minibatchSize);
        this.curriculum = new CurriculumManager(RLConfig.INSTANCE.curriculum);
        this.evaluator = new Evaluator(policy, obsDim, historyLength, actionDim, minecraftIntegration, combatEventHandler, trainingArena);

        this.encoder = new ObservationEncoder();
        this.history = new ObservationHistory(historyLength, obsDim);
        this.actionProcessor = new ActionProcessor();
        
        RewardConfig rewardConfig = new RewardConfig();
        copyRewardConfig(rewardConfig);
        this.rewardCalculator = new RewardCalculator(rewardConfig);

        this.currentObs = new float[obsDim];
        this.currentAction = new float[actionDim];
        this.flatObs = new float[obsDim * historyLength];

        CurriculumStage stage = curriculum.getCurrentStage();
        configureStage(stage);
    }

    private void copyRewardConfig(RewardConfig config) {
        RLConfig.RewardConfig src = RLConfig.INSTANCE.reward;
        config.hitReward = src.hitReward;
        config.damageDealtReward = src.damageDealtReward;
        config.criticalHitBonus = src.criticalHitBonus;
        config.comboReward = src.comboReward;
        config.goodTrackingReward = src.goodTrackingReward;
        config.spacingReward = src.spacingReward;
        config.counterHitReward = src.counterHitReward;
        config.whiffPunishReward = src.whiffPunishReward;
        config.survivalReward = src.survivalReward;
        config.winReward = src.winReward;
        config.damageTakenPenalty = src.damageTakenPenalty;
        config.whiffPenalty = src.whiffPenalty;
        config.badSpacingPenalty = src.badSpacingPenalty;
        config.poorTimingPenalty = src.poorTimingPenalty;
        config.randomMovementPenalty = src.randomMovementPenalty;
        config.excessiveJumpPenalty = src.excessiveJumpPenalty;
        config.loseTargetPenalty = src.loseTargetPenalty;
        config.deathPenalty = src.deathPenalty;
        config.missedOpportunityPenalty = src.missedOpportunityPenalty;
        config.exploitationSpinPenalty = src.exploitationSpinPenalty;
        config.exploitationStrafePenalty = src.exploitationStrafePenalty;
        config.exploitationAttackPenalty = src.exploitationAttackPenalty;
        config.exploitationDamagePenalty = src.exploitationDamagePenalty;
        config.enableExploitationPrevention = src.enableExploitationPrevention;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            startNewEpisode();
        } else {
            actionExecutor.reset();
            trainingArena.endEpisode();
            combatEventHandler.reset();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleEvaluation() {
        if (!enabled && !evaluating) {
            evaluating = true;
            new Thread(() -> {
                EvaluationMetrics metrics = evaluator.runEvaluation(RLConfig.INSTANCE.training.evalEpisodes);
                System.out.println("[RL-PvP] Evaluation: " + formatMetrics(metrics));
                evaluating = false;
            }).start();
        }
    }

    public void tick() {
        if (!enabled) return;

        trainingStep++;
        stepsSinceUpdate++;

        if (!trainingArena.isEpisodeActive()) {
            startNewEpisode();
        } else {
            step();
            trainingArena.tick();
        }

        // Handle carpet self-play opponents
        CurriculumStage currentStageObj = curriculum.getCurrentStage();
        if (currentStageObj.getOpponentType() == OpponentController.OpponentType.SELF_PLAY) {
            if (CarpetIntegration.isCarpetAvailable()) {
                carpetIntegration.setPolicy(policy);
                Vec3d agentPos = RLMain.getMinecraftIntegration().client.player != null ? 
                    RLMain.getMinecraftIntegration().client.player.getPos() : Vec3d.ZERO;
                carpetIntegration.tickFakePlayers(agentPos);
            } else {
                // Fallback to SMART opponent if carpet not available
                // This is handled by the training arena spawning a smart zombie
            }
        }

        combatEventHandler.tick();

        if (stepsSinceUpdate >= rolloutLength && rolloutBuffer.isFull()) {
            updatePolicy();
            stepsSinceUpdate = 0;
        }

        currentStage = curriculum.getCurrentStageId();
        winRate = curriculum.getWinRate();
        hitRate = curriculum.getHitRate();

        if (RLConfig.INSTANCE.training.autoSave && trainingStep % RLConfig.INSTANCE.training.checkpointInterval == 0) {
            try {
                saveCheckpoint("checkpoint_auto_" + trainingStep + ".bin");
            } catch (Exception e) {
                System.err.println("[RL-PvP] Auto-save failed: " + e.getMessage());
            }
        }
    }

    private void startNewEpisode() {
        episodeCount++;
        episodeReward = 0f;
        episodeHits = 0;
        episodeAttacks = 0;
        episodeWhiffs = 0;
        episodeCombo = 0;
        episodeDamageDealt = 0f;
        episodeDamageTaken = 0f;

        history.clear();
        combatEventHandler.reset();
        minecraftIntegration.reset();
        actionExecutor.reset();

        trainingArena.startEpisode();
        
        minecraftIntegration.setPlayer(RLMain.getMinecraftIntegration().client.player);
        if (trainingArena.getTrainingTarget() != null) {
            minecraftIntegration.setTarget(trainingArena.getTrainingTarget());
            combatEventHandler.setCurrentTarget(trainingArena.getTrainingTarget());
        }

        CurriculumStage stage = curriculum.getCurrentStage();
        configureStage(stage);
    }

    private void step() {
        float[] obs = minecraftIntegration.collectObservations();
        
        if (obs == null || obs.length != obsDim) {
            return;
        }

        history.add(obs);

        if (history.isFull()) {
            System.arraycopy(history.getFlattened(), 0, flatObs, 0, flatObs.length);

            policy.forward(flatObs);
            float[] logits = policy.getActionLogits();
            float value = policy.getValue();

            actionProcessor.process(logits, currentAction);
            actionExecutor.execute(currentAction);

            float reward = computeReward();
            episodeReward += reward;

            boolean done = !trainingArena.isEpisodeActive();
            rolloutBuffer.add(flatObs, currentAction, reward, value, 0f, done);

            if (currentAction[ActionSpace.IDX_ATTACK] > 0.5f) {
                episodeAttacks++;
                combatEventHandler.onPlayerAttack();
            }

            updateCombatStats();
        }
    }

    private float computeReward() {
        boolean hit = combatEventHandler.wasLastAttackHit();
        boolean critical = combatEventHandler.wasLastAttackCritical();
        int combo = combatEventHandler.getComboCount();
        float damageDealt = combatEventHandler.getLastDamageDealt();
        float damageTaken = combatEventHandler.getLastDamageTaken();
        float distance = combatEventHandler.getDistanceToTarget();
        
        float yawError = 0f;
        float pitchError = 0f;
        if (minecraftIntegration.getTarget() != null) {
            yawError = Math.abs(minecraftIntegration.getTarget().getYaw() - RLMain.getMinecraftIntegration().client.player.getYaw());
            pitchError = Math.abs(minecraftIntegration.getTarget().getPitch() - RLMain.getMinecraftIntegration().client.player.getPitch());
        }

        boolean won = trainingArena.getTrainingTarget() != null && trainingArena.getTrainingTarget().getHealth() <= 0;
        boolean died = RLMain.getMinecraftIntegration().client.player != null && RLMain.getMinecraftIntegration().client.player.getHealth() <= 0;

        return rewardCalculator.compute(
            null, null, currentAction,
            hit, damageDealt, damageTaken,
            critical, combo > 1, !hit && currentAction[ActionSpace.IDX_ATTACK] > 0.5f,
            false, won, died,
            distance, yawError, pitchError,
            combatEventHandler.getTimeSinceLastHit()
        );
    }

    private void updateCombatStats() {
        if (combatEventHandler.wasLastAttackHit()) {
            episodeHits++;
            episodeDamageDealt += combatEventHandler.getLastDamageDealt();
            episodeCombo = combatEventHandler.getComboCount();
        } else if (currentAction[ActionSpace.IDX_ATTACK] > 0.5f) {
            episodeWhiffs++;
            episodeCombo = 0;
        }
        
        episodeDamageTaken += combatEventHandler.getLastDamageTaken();
    }

    private void updatePolicy() {
        float lastValue = 0f;
        boolean lastDone = !trainingArena.isEpisodeActive();
        
        if (rolloutBuffer.size > 0) {
            int lastIdx = rolloutBuffer.get(rolloutBuffer.size - 1);
            lastValue = rolloutBuffer.values[lastIdx];
            lastDone = rolloutBuffer.dones[lastIdx];
        }

        gae.compute(rolloutBuffer, lastValue, lastDone);
        gae.normalizeAdvantages(rolloutBuffer);
        ppo.update(rolloutBuffer, gae);
        rolloutBuffer.clear();
    }

    private void configureStage(CurriculumStage stage) {
        OpponentController.OpponentType type = stage.getOpponentType();
        
        if (type == OpponentController.OpponentType.SELF_PLAY) {
            if (CarpetIntegration.isCarpetAvailable()) {
                // Clear any existing target, carpet will create fake players
                trainingArena.removeTarget();
                carpetIntegration.removeAllFakePlayers();
                
                // Create fake players after arena starts
                Vec3d arenaCenter = trainingArena.getArenaCenter();
                if (arenaCenter != null && !arenaCenter.equals(Vec3d.ZERO)) {
                    for (int i = 0; i < 3; i++) {
                        Vec3d spawnPos = arenaCenter.add(
                            (random.nextDouble() - 0.5) * 10,
                            0,
                            (random.nextDouble() - 0.5) * 10
                        );
                        carpetIntegration.createFakePlayer(spawnPos);
                    }
                }
            }
        }
    }
    
    private final Random random = new Random();

    public void saveCheckpoint(String filename) throws Exception {
        CheckpointManager.save(filename, policy, curriculum, trainingStep, null, null, 
            RLConfig.INSTANCE.ppo.learningRate, historyLength, RLConfig.INSTANCE.network.hiddenSizes);
    }

    public void loadCheckpoint(String filename) throws Exception {
        CheckpointManager.CheckpointData data = CheckpointManager.load(filename, obsDim * historyLength, actionDim);
        if (data != null) {
            policy.copyFrom(data.model);
            trainingStep = data.trainingStep;
            curriculum.setStage(data.curriculumStage);
            episodeCount = data.totalEpisodes;
        }
    }

    public EvaluationMetrics evaluate(int numEpisodes) {
        return evaluator.runEvaluation(numEpisodes);
    }

    public CurriculumManager getCurriculumManager() {
        return curriculum;
    }

    public int getCurrentStage() { return currentStage; }
    public long getTrainingStep() { return trainingStep; }
    public int getEpisodeCount() { return episodeCount; }
    public float getEpisodeReward() { return episodeReward; }
    public float getWinRate() { return winRate; }
    public float getHitRate() { return hitRate; }

    private String formatMetrics(EvaluationMetrics m) {
        return String.format("Win: %.1f%% | Dmg Dealt: %.1f | Dmg Taken: %.1f | Hit: %.1f%% | Whiff: %.1f%% | Combo: %d | Survive: %.1fs",
            m.getWinRate() * 100, m.getAvgDamageDealt(), m.getAvgDamageTaken(),
            m.getHitRate() * 100, m.getWhiffRate() * 100,
            m.getMaxCombo(), m.getAvgSurvivalTime());
    }
}