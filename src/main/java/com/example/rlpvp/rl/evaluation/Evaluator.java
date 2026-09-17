package com.example.rlpvp.rl.evaluation;

import com.example.rlpvp.rl.network.ActorCritic;
import com.example.rlpvp.rl.environment.EpisodeManager;
import com.example.rlpvp.rl.environment.OpponentController;
import com.example.rlpvp.rl.observation.ObservationEncoder;
import com.example.rlpvp.rl.observation.ObservationHistory;
import com.example.rlpvp.rl.observation.SelfObservation;
import com.example.rlpvp.rl.observation.TargetObservation;
import com.example.rlpvp.rl.observation.CombatObservation;
import com.example.rlpvp.rl.observation.AimObservation;
import com.example.rlpvp.rl.observation.MovementObservation;
import com.example.rlpvp.rl.action.ActionProcessor;
import com.example.rlpvp.rl.action.ActionSpace;

public class Evaluator {
    private final ActorCritic policy;
    private final EpisodeManager episodeManager;
    private final ObservationEncoder encoder;
    private final ObservationHistory history;
    private final ActionProcessor actionProcessor;
    private final OpponentController opponent;
    private final EvaluationMetrics metrics;

    private float[] currentObs;
    private float[] currentAction;
    private int episodeHits = 0;
    private int episodeAttacks = 0;
    private int episodeWhiffs = 0;
    private int episodeCombo = 0;
    private float episodeDamageDealt = 0f;
    private float episodeDamageTaken = 0f;
    private float episodeYawError = 0f;
    private float episodePitchError = 0f;
    private float episodeMovementEff = 0f;
    private int episodeCrits = 0;
    private float episodeStartTime = 0f;

    public Evaluator(ActorCritic policy, int obsDim, int historyLength, int actionDim) {
        this.policy = policy;
        this.episodeManager = new EpisodeManager();
        this.encoder = new ObservationEncoder();
        this.history = new ObservationHistory(historyLength, obsDim);
        this.actionProcessor = new ActionProcessor();
        this.opponent = new OpponentController();
        this.metrics = new EvaluationMetrics();

        this.currentObs = new float[obsDim];
        this.currentAction = new float[actionDim];
    }

    public void runEvaluation(int numEpisodes, OpponentController.OpponentType opponentType) {
        metrics.reset();
        opponent.setType(opponentType);

        for (int ep = 0; ep < numEpisodes; ep++) {
            runEpisode();
        }
    }

    private void runEpisode() {
        episodeManager.startEpisode();
        history.clear();

        float[] agentSpawn = episodeManager.getArena().getAgentSpawn();
        episodeManager.setAgentPos(agentSpawn);

        float[] targetSpawn = episodeManager.getArena().getTargetSpawn(agentSpawn);
        episodeManager.setTargetPos(targetSpawn);
        opponent.reset(targetSpawn);

        episodeHits = 0;
        episodeAttacks = 0;
        episodeWhiffs = 0;
        episodeCombo = 0;
        episodeDamageDealt = 0f;
        episodeDamageTaken = 0f;
        episodeYawError = 0f;
        episodePitchError = 0f;
        episodeMovementEff = 0f;
        episodeCrits = 0;
        episodeStartTime = System.currentTimeMillis();

        while (episodeManager.isInEpisode()) {
            step();
        }

        float duration = (System.currentTimeMillis() - episodeStartTime) / 1000f;
        float avgDistance = episodeManager.getArena().isInArena(
            episodeManager.getAgentPos()[0], episodeManager.getAgentPos()[1], episodeManager.getAgentPos()[2]) ? 3.5f : 10f;

        metrics.recordEpisode(
            episodeManager.getTargetHealth() <= 0,
            episodeDamageDealt, episodeDamageTaken,
            episodeHits, episodeAttacks, episodeWhiffs, episodeCombo,
            duration, avgDistance, episodeYawError, episodePitchError,
            episodeMovementEff, episodeCrits, duration, episodeManager.getEpisodeReward()
        );
    }

    private void step() {
        SelfObservation self = new SelfObservation();
        TargetObservation target = new TargetObservation();
        CombatObservation combat = new CombatObservation();
        AimObservation aim = new AimObservation();
        MovementObservation movement = new MovementObservation();

        float[] agentPos = episodeManager.getAgentPos();
        float[] targetPos = episodeManager.getTargetPos();

        fillObservations(self, target, combat, aim, movement, agentPos, targetPos);

        self.normalize();
        target.normalize();
        combat.normalize();
        aim.normalize();
        movement.normalize();

        float[] encoded = encoder.encode(self, target, combat, aim, movement);
        history.add(encoded);

        if (history.isFull()) {
            float[] flatObs = history.getFlattened();
            policy.forward(flatObs);
            float[] logits = policy.getActionLogits();

            actionProcessor.process(logits, currentAction);

            float reward = episodeManager.step(currentAction, flatObs);

            if (currentAction[ActionSpace.IDX_ATTACK] > 0.5f) {
                episodeAttacks++;
                float dist = (float) Math.sqrt(
                    (agentPos[0] - targetPos[0]) * (agentPos[0] - targetPos[0]) +
                    (agentPos[1] - targetPos[1]) * (agentPos[1] - targetPos[1]) +
                    (agentPos[2] - targetPos[2]) * (agentPos[2] - targetPos[2])
                );
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

        opponent.tick(agentPos);
    }

    private void fillObservations(SelfObservation self, TargetObservation target, 
                                  CombatObservation combat, AimObservation aim, 
                                  MovementObservation movement, float[] agentPos, float[] targetPos) {
        self.health = episodeManager.getAgentHealth();
        self.posX = agentPos[0];
        self.posY = agentPos[1];
        self.posZ = agentPos[2];

        target.relPosX = targetPos[0] - agentPos[0];
        target.relPosY = targetPos[1] - agentPos[1];
        target.relPosZ = targetPos[2] - agentPos[2];
        target.distance = (float) Math.sqrt(
            target.relPosX * target.relPosX + 
            target.relPosY * target.relPosY + 
            target.relPosZ * target.relPosZ
        );
        target.health = episodeManager.getTargetHealth();
    }

    public EvaluationMetrics getMetrics() {
        return metrics;
    }
}