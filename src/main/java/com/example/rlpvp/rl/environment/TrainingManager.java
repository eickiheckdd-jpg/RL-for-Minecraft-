package com.example.rlpvp.rl.environment;

import com.example.rlpvp.rl.network.ActorCritic;
import com.example.rlpvp.rl.ppo.RolloutBuffer;
import com.example.rlpvp.rl.ppo.GAE;
import com.example.rlpvp.rl.ppo.PPO;
import com.example.rlpvp.rl.observation.ObservationEncoder;
import com.example.rlpvp.rl.observation.ObservationHistory;
import com.example.rlpvp.rl.observation.SelfObservation;
import com.example.rlpvp.rl.observation.TargetObservation;
import com.example.rlpvp.rl.observation.CombatObservation;
import com.example.rlpvp.rl.observation.AimObservation;
import com.example.rlpvp.rl.observation.MovementObservation;
import com.example.rlpvp.rl.action.ActionProcessor;
import com.example.rlpvp.rl.action.ActionSpace;
import com.example.rlpvp.rl.reward.RewardCalculator;
import com.example.rlpvp.rl.reward.RewardConfig;
import com.example.rlpvp.rl.curriculum.CurriculumManager;
import com.example.rlpvp.rl.curriculum.CurriculumStage;
import com.example.rlpvp.rl.checkpoint.CheckpointManager;
import com.example.rlpvp.rl.evaluation.Evaluator;
import com.example.rlpvp.rl.evaluation.EvaluationMetrics;

public class TrainingManager {

    private boolean enabled = false;
    private int currentStage = 0;
    private long trainingStep = 0;
    private int episodeCount = 0;
    private float episodeReward = 0f;
    private float winRate = 0f;
    private float hitRate = 0f;

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
    private EpisodeManager episodeManager;
    private OpponentController opponent;

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

    public TrainingManager() {
        this.obsDim = ObservationEncoder.getObsDim();
        this.actionDim = ActionSpace.TOTAL_DIM;
        this.historyLength = 8;
        this.rolloutLength = 2048;

        int[] hiddenSizes = {256, 128, 64};
        
        this.policy = new ActorCritic(obsDim * historyLength, hiddenSizes, actionDim);
        this.rolloutBuffer = new RolloutBuffer(rolloutLength, obsDim * historyLength, actionDim);
        this.gae = new GAE(rolloutLength, 0.99f, 0.95f);
        this.ppo = new PPO(policy, 3e-4f, 0.2f, 0.01f, 0.5f, 0.5f, 4, 256);
        this.curriculum = new CurriculumManager();
        this.evaluator = new Evaluator(policy, obsDim, historyLength, actionDim);

        this.encoder = new ObservationEncoder();
        this.history = new ObservationHistory(historyLength, obsDim);
        this.actionProcessor = new ActionProcessor();
        this.rewardCalculator = new RewardCalculator(new RewardConfig());
        this.episodeManager = new EpisodeManager();
        this.opponent = new OpponentController();

        this.currentObs = new float[obsDim];
        this.currentAction = new float[actionDim];
        this.flatObs = new float[obsDim * historyLength];

        CurriculumStage stage = curriculum.getCurrentStage();
        opponent.setType(stage.getOpponentType());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            startNewEpisode();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void tick() {
        if (!enabled) return;

        trainingStep++;
        stepsSinceUpdate++;

        if (episodeManager.isInEpisode()) {
            step();
        } else {
            startNewEpisode();
        }

        if (stepsSinceUpdate >= rolloutLength && rolloutBuffer.isFull()) {
            updatePolicy();
            stepsSinceUpdate = 0;
        }

        currentStage = curriculum.getCurrentStageId();
        winRate = curriculum.getWinRate();
        hitRate = curriculum.getHitRate();
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

        episodeManager.startEpisode();
        history.clear();

        float[] agentSpawn = episodeManager.getArena().getAgentSpawn();
        episodeManager.setAgentPos(agentSpawn);

        float[] targetSpawn = episodeManager.getArena().getTargetSpawn(agentSpawn);
        episodeManager.setTargetPos(targetSpawn);

        CurriculumStage stage = curriculum.getCurrentStage();
        opponent.setType(stage.getOpponentType());
        opponent.reset(targetSpawn);
    }

    private void step() {
        fillAndEncodeObservations();

        history.add(encoder.getEncoded());

        if (history.isFull()) {
            System.arraycopy(history.getFlattened(), 0, flatObs, 0, flatObs.length);

            policy.forward(flatObs);
            float[] logits = policy.getActionLogits();
            float value = policy.getValue();

            actionProcessor.process(logits, currentAction);

            float reward = episodeManager.step(currentAction, flatObs);
            episodeReward += reward;

            rolloutBuffer.add(flatObs, currentAction, reward, value, 0f, !episodeManager.isInEpisode());

            if (currentAction[ActionSpace.IDX_ATTACK] > 0.5f) {
                episodeAttacks++;
                float dist = distance(episodeManager.getAgentPos(), episodeManager.getTargetPos());
                if (dist < 3.5f) {
                    episodeHits++;
                    episodeDamageDealt += 5f;
                    episodeCombo++;
                } else {
                    episodeWhiffs++;
                    episodeCombo = 0;
                }
            }
        }

        opponent.tick(episodeManager.getAgentPos());

        if (!episodeManager.isInEpisode()) {
            boolean won = episodeManager.getTargetHealth() <= 0;
            curriculum.onEpisodeEnd(won, episodeHits, episodeAttacks);

            CurriculumStage newStage = curriculum.getCurrentStage();
            if (newStage.getStageId() != opponent.getType().ordinal()) {
                opponent.setType(newStage.getOpponentType());
            }
        }
    }

    private void fillAndEncodeObservations() {
        SelfObservation self = new SelfObservation();
        TargetObservation target = new TargetObservation();
        CombatObservation combat = new CombatObservation();
        AimObservation aim = new AimObservation();
        MovementObservation movement = new MovementObservation();

        float[] agentPos = episodeManager.getAgentPos();
        float[] targetPos = episodeManager.getTargetPos();

        self.health = episodeManager.getAgentHealth();
        self.posX = agentPos[0];
        self.posY = agentPos[1];
        self.posZ = agentPos[2];

        target.relPosX = targetPos[0] - agentPos[0];
        target.relPosY = targetPos[1] - agentPos[1];
        target.relPosZ = targetPos[2] - agentPos[2];
        target.distance = distance(agentPos, targetPos);
        target.health = episodeManager.getTargetHealth();

        self.normalize();
        target.normalize();
        combat.normalize();
        aim.normalize();
        movement.normalize();

        encoder.encode(self, target, combat, aim, movement);
    }

    private float distance(float[] a, float[] b) {
        float dx = a[0] - b[0];
        float dy = a[1] - b[1];
        float dz = a[2] - b[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void updatePolicy() {
        float lastValue = 0f;
        boolean lastDone = !episodeManager.isInEpisode();
        
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

    public void saveCheckpoint(String filename) throws Exception {
        CheckpointManager.save(filename, policy, curriculum, trainingStep, null, null, 
            3e-4f, historyLength, new int[]{256, 128, 64});
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
        evaluator.runEvaluation(numEpisodes, OpponentController.OpponentType.SCRIPTED_MEDIUM);
        return evaluator.getMetrics();
    }

    public int getCurrentStage() { return currentStage; }
    public long getTrainingStep() { return trainingStep; }
    public int getEpisodeCount() { return episodeCount; }
    public float getEpisodeReward() { return episodeReward; }
    public float getWinRate() { return winRate; }
    public float getHitRate() { return hitRate; }
}