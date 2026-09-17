package com.example.rlpvp.rl.evaluation;

import com.example.rlpvp.RLMain;
import com.example.rlpvp.rl.network.ActorCritic;
import com.example.rlpvp.rl.minecraft.MinecraftIntegration;
import com.example.rlpvp.rl.minecraft.CombatEventHandler;
import com.example.rlpvp.rl.minecraft.TrainingArena;
import com.example.rlpvp.rl.observation.ObservationEncoder;
import com.example.rlpvp.rl.observation.ObservationHistory;
import com.example.rlpvp.rl.action.ActionProcessor;
import com.example.rlpvp.rl.action.ActionSpace;

public class Evaluator {
    private final ActorCritic policy;
    private final MinecraftIntegration minecraftIntegration;
    private final CombatEventHandler combatEventHandler;
    private final TrainingArena trainingArena;
    private final ObservationEncoder encoder;
    private final ObservationHistory history;
    private final ActionProcessor actionProcessor;
    private final EvaluationMetrics metrics;

    private float[] currentObs;
    private float[] currentAction;
    private int episodeHits = 0;
    private int episodeAttacks = 0;
    private int episodeWhiffs = 0;
    private int episodeCombo = 0;
    private float episodeDamageDealt = 0f;
    private float episodeDamageTaken = 0f;
    private float episodeStartTime = 0f;

    public Evaluator(ActorCritic policy, int obsDim, int historyLength, int actionDim,
                     MinecraftIntegration minecraftIntegration, CombatEventHandler combatEventHandler,
                     TrainingArena trainingArena) {
        this.policy = policy;
        this.minecraftIntegration = minecraftIntegration;
        this.combatEventHandler = combatEventHandler;
        this.trainingArena = trainingArena;
        this.encoder = new ObservationEncoder();
        this.history = new ObservationHistory(historyLength, obsDim);
        this.actionProcessor = new ActionProcessor();
        this.metrics = new EvaluationMetrics();

        this.currentObs = new float[obsDim];
        this.currentAction = new float[actionDim];
    }

    public EvaluationMetrics runEvaluation(int numEpisodes) {
        metrics.reset();

        for (int ep = 0; ep < numEpisodes; ep++) {
            runEpisode();
        }

        return metrics;
    }

    private void runEpisode() {
        history.clear();
        combatEventHandler.reset();

        trainingArena.startEpisode();

        episodeHits = 0;
        episodeAttacks = 0;
        episodeWhiffs = 0;
        episodeCombo = 0;
        episodeDamageDealt = 0f;
        episodeDamageTaken = 0f;
        episodeStartTime = System.currentTimeMillis();

        while (trainingArena.isEpisodeActive()) {
            step();
        }

        float duration = (System.currentTimeMillis() - episodeStartTime) / 1000f;
        float avgDistance = combatEventHandler.getDistanceToTarget();

        metrics.recordEpisode(
            trainingArena.getTrainingTarget() != null && trainingArena.getTrainingTarget().getHealth() <= 0,
            episodeDamageDealt, episodeDamageTaken,
            episodeHits, episodeAttacks, episodeWhiffs, episodeCombo,
            duration, avgDistance, 0f, 0f,
            0f, 0f, duration, 0f
        );

        trainingArena.endEpisode();
    }

    private void step() {
        float[] obs = minecraftIntegration.collectObservations();
        if (obs == null) return;

        history.add(obs);

        if (history.isFull()) {
            float[] flatObs = history.getFlattened();
            policy.forward(flatObs);
            float[] logits = policy.getActionLogits();

            actionProcessor.process(logits, currentAction);

            RLMain.getActionExecutor().execute(currentAction);

            if (currentAction[ActionSpace.IDX_ATTACK] > 0.5f) {
                episodeAttacks++;
                combatEventHandler.onPlayerAttack();
            }

            updateCombatStats();
        }
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

    public EvaluationMetrics getMetrics() {
        return metrics;
    }
}