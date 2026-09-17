package com.example.rlpvp.rl.network;

public class Layer {
    public final int inputSize;
    public final int outputSize;
    public final float[] weight;
    public final float[] bias;
    public final float[] weightGrad;
    public final float[] biasGrad;
    public final Activation activation;

    private final float[] inputCache;
    private final float[] preActCache;

    public Layer(int inputSize, int outputSize, Activation activation) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.weight = new float[inputSize * outputSize];
        this.bias = new float[outputSize];
        this.weightGrad = new float[inputSize * outputSize];
        this.biasGrad = new float[outputSize];
        this.activation = activation;
        this.inputCache = new float[inputSize];
        this.preActCache = new float[outputSize];
        xavierInit();
    }

    private void xavierInit() {
        float scale = (float) Math.sqrt(2.0 / (inputSize + outputSize));
        for (int i = 0; i < weight.length; i++) {
            weight[i] = (float) (Math.random() * 2 - 1) * scale;
        }
    }

    public void forward(float[] input, float[] output) {
        System.arraycopy(input, 0, inputCache, 0, inputSize);

        for (int j = 0; j < outputSize; j++) {
            float sum = bias[j];
            int base = j * inputSize;
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weight[base + i];
            }
            preActCache[j] = sum;
        }

        activation.forward(preActCache, output);
    }

    public void backward(float[] gradOutput, float[] gradInput) {
        float[] actGrad = new float[outputSize];
        activation.backward(preActCache, gradOutput, actGrad);

        for (int i = 0; i < inputSize; i++) {
            float sum = 0f;
            for (int j = 0; j < outputSize; j++) {
                int idx = j * inputSize + i;
                sum += actGrad[j] * weight[idx];
                weightGrad[idx] += actGrad[j] * inputCache[i];
            }
            gradInput[i] = sum;
        }

        for (int j = 0; j < outputSize; j++) {
            biasGrad[j] += actGrad[j];
        }
    }

    public void zeroGrad() {
        for (int i = 0; i < weightGrad.length; i++) weightGrad[i] = 0f;
        for (int i = 0; i < biasGrad.length; i++) biasGrad[i] = 0f;
    }

    public void update(float lr) {
        for (int i = 0; i < weight.length; i++) weight[i] -= lr * weightGrad[i];
        for (int i = 0; i < bias.length; i++) bias[i] -= lr * biasGrad[i];
    }

    public void clipGrad(float maxNorm) {
        float norm = 0f;
        for (float g : weightGrad) norm += g * g;
        for (float g : biasGrad) norm += g * g;
        norm = (float) Math.sqrt(norm);
        if (norm > maxNorm) {
            float scale = maxNorm / norm;
            for (int i = 0; i < weightGrad.length; i++) weightGrad[i] *= scale;
            for (int i = 0; i < biasGrad.length; i++) biasGrad[i] *= scale;
        }
    }

    public int paramCount() {
        return weight.length + bias.length;
    }
}