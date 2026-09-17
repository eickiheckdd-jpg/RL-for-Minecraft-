package com.example.rlpvp.rl.ppo;

import com.example.rlpvp.rl.network.ActorCritic;

public class PPO {
    private final ActorCritic actorCritic;
    private final float lr;
    private final float clipEpsilon;
    private final float entropyCoef;
    private final float valueCoef;
    private final float maxGradNorm;
    private final int ppoEpochs;
    private final int minibatchSize;

    private final float[] obsBatch;
    private final float[] actionBatch;
    private final float[] logProbBatch;
    private final float[] advantageBatch;
    private final float[] returnBatch;

    private final float[] actionLogits;
    private final float[] newLogProbs;
    private final float[] ratio;
    private final float[] clippedRatio;
    private final float[] policyLoss;
    private final float[] valueLoss;
    private final float[] entropy;

    public PPO(ActorCritic actorCritic, float lr, float clipEpsilon, float entropyCoef, 
               float valueCoef, float maxGradNorm, int ppoEpochs, int minibatchSize) {
        this.actorCritic = actorCritic;
        this.lr = lr;
        this.clipEpsilon = clipEpsilon;
        this.entropyCoef = entropyCoef;
        this.valueCoef = valueCoef;
        this.maxGradNorm = maxGradNorm;
        this.ppoEpochs = ppoEpochs;
        this.minibatchSize = minibatchSize;

        int obsDim = actorCritic.getLayer(0).inputSize;
        int actionDim = actorCritic.getActionLogits().length;

        this.obsBatch = new float[minibatchSize * obsDim];
        this.actionBatch = new float[minibatchSize * actionDim];
        this.logProbBatch = new float[minibatchSize];
        this.advantageBatch = new float[minibatchSize];
        this.returnBatch = new float[minibatchSize];

        this.actionLogits = new float[minibatchSize * actionDim];
        this.newLogProbs = new float[minibatchSize];
        this.ratio = new float[minibatchSize];
        this.clippedRatio = new float[minibatchSize];
        this.policyLoss = new float[minibatchSize];
        this.valueLoss = new float[minibatchSize];
        this.entropy = new float[minibatchSize];
    }

    public void update(RolloutBuffer buffer, GAE gae) {
        for (int epoch = 0; epoch < ppoEpochs; epoch++) {
            for (int start = 0; start < buffer.size; start += minibatchSize) {
                int end = Math.min(start + minibatchSize, buffer.size);
                int batchSize = end - start;

                collectMinibatch(buffer, gae, start, batchSize);
                computeLosses(batchSize);
                backward(batchSize);
                actorCritic.clipGrad(maxGradNorm);
                actorCritic.update(lr);
                actorCritic.zeroGrad();
            }
        }
    }

    private void collectMinibatch(RolloutBuffer buffer, GAE gae, int start, int batchSize) {
        int obsDim = actorCritic.getLayer(0).inputSize;
        int actionDim = actorCritic.getActionLogits().length;

        for (int i = 0; i < batchSize; i++) {
            int idx = buffer.get(start + i);
            float[] obs = buffer.observations[idx];
            float[] action = buffer.actions[idx];

            System.arraycopy(obs, 0, obsBatch, i * obsDim, obsDim);
            System.arraycopy(action, 0, actionBatch, i * actionDim, actionDim);
            logProbBatch[i] = buffer.logProbs[idx];
            advantageBatch[i] = gae.getAdvantage(idx);
            returnBatch[i] = gae.getReturn(idx);
        }
    }

    private void computeLosses(int batchSize) {
        int actionDim = actorCritic.getActionLogits().length;

        for (int i = 0; i < batchSize; i++) {
            float[] obs = new float[actorCritic.getLayer(0).inputSize];
            System.arraycopy(obsBatch, i * obs.length, obs, 0, obs.length);

            actorCritic.forward(obs);
            float[] logits = actorCritic.getActionLogits();
            float value = actorCritic.getValue();

            System.arraycopy(logits, 0, actionLogits, i * actionDim, actionDim);

            newLogProbs[i] = computeLogProb(logits, actionBatch, i * actionDim, actionDim);
            float oldLogProb = logProbBatch[i];
            ratio[i] = (float) Math.exp(newLogProbs[i] - oldLogProb);

            clippedRatio[i] = Math.max(1f - clipEpsilon, Math.min(1f + clipEpsilon, ratio[i]));

            float adv = advantageBatch[i];
            float surr1 = ratio[i] * adv;
            float surr2 = clippedRatio[i] * adv;
            policyLoss[i] = -Math.min(surr1, surr2);

            float valDiff = value - returnBatch[i];
            valueLoss[i] = valueCoef * valDiff * valDiff;

            entropy[i] = computeEntropy(logits, actionDim);
        }
    }

    private float computeLogProb(float[] logits, float[] actions, int actionOffset, int actionDim) {
        float maxLogit = logits[0];
        for (int i = 1; i < actionDim; i++) {
            if (logits[i] > maxLogit) maxLogit = logits[i];
        }

        float sumExp = 0f;
        for (int i = 0; i < actionDim; i++) {
            sumExp += (float) Math.exp(logits[i] - maxLogit);
        }

        float logProb = 0f;
        for (int i = 0; i < actionDim; i++) {
            float prob = (float) Math.exp(logits[i] - maxLogit) / sumExp;
            float actionVal = actions[actionOffset + i];
            if (actionVal > 0.5f) {
                logProb += (float) Math.log(Math.max(prob, 1e-8f));
            }
        }
        return logProb;
    }

    private float computeEntropy(float[] logits, int actionDim) {
        float maxLogit = logits[0];
        for (int i = 1; i < actionDim; i++) {
            if (logits[i] > maxLogit) maxLogit = logits[i];
        }

        float sumExp = 0f;
        for (int i = 0; i < actionDim; i++) {
            sumExp += (float) Math.exp(logits[i] - maxLogit);
        }

        float ent = 0f;
        for (int i = 0; i < actionDim; i++) {
            float prob = (float) Math.exp(logits[i] - maxLogit) / sumExp;
            if (prob > 0f) {
                ent -= prob * (float) Math.log(prob);
            }
        }
        return entropyCoef * ent;
    }

    private void backward(int batchSize) {
        int obsDim = actorCritic.getLayer(0).inputSize;
        int actionDim = actorCritic.getActionLogits().length;

        float[] obsGrad = new float[obsDim];
        float[] actionGrad = new float[actionDim];

        for (int i = 0; i < batchSize; i++) {
            float[] obs = new float[obsDim];
            System.arraycopy(obsBatch, i * obsDim, obs, 0, obsDim);

            float adv = advantageBatch[i];

            for (int j = 0; j < actionDim; j++) {
                int idx = i * actionDim + j;
                float prob = (float) Math.exp(actionLogits[idx] - maxLogit(actionLogits, i * actionDim, actionDim)) 
                           / sumExp(actionLogits, i * actionDim, actionDim);
                float actionVal = actionBatch[idx];
                
                if (actionVal > 0.5f) {
                    float grad = -(adv * (1f - prob) * (ratio[i] > 1f + clipEpsilon || ratio[i] < 1f - clipEpsilon ? 0f : 1f));
                    actionGrad[j] = grad;
                } else {
                    actionGrad[j] = 0f;
                }
            }

            float valGrad = 2f * valueCoef * (actorCritic.getValue() - returnBatch[i]);

            actorCritic.backward(obs, actionGrad, valGrad, obsGrad);
        }
    }

    private float maxLogit(float[] arr, int offset, int len) {
        float max = arr[offset];
        for (int i = 1; i < len; i++) {
            if (arr[offset + i] > max) max = arr[offset + i];
        }
        return max;
    }

    private float sumExp(float[] arr, int offset, int len) {
        float max = maxLogit(arr, offset, len);
        float sum = 0f;
        for (int i = 0; i < len; i++) {
            sum += (float) Math.exp(arr[offset + i] - max);
        }
        return sum;
    }

    public float getAvgPolicyLoss() {
        float sum = 0f;
        for (float v : policyLoss) sum += v;
        return sum / policyLoss.length;
    }

    public float getAvgValueLoss() {
        float sum = 0f;
        for (float v : valueLoss) sum += v;
        return sum / valueLoss.length;
    }

    public float getAvgEntropy() {
        float sum = 0f;
        for (float v : entropy) sum += v;
        return sum / entropy.length;
    }
}