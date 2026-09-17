package com.example.rlpvp.rl.network;

public class MLP {
    private final Layer[] layers;
    private final int[] layerSizes;
    private final float[] buffer1;
    private final float[] buffer2;

    public MLP(int inputSize, int[] hiddenSizes, int outputSize, Activation hiddenAct, Activation outputAct) {
        this.layerSizes = new int[hiddenSizes.length + 2];
        layerSizes[0] = inputSize;
        System.arraycopy(hiddenSizes, 0, layerSizes, 1, hiddenSizes.length);
        layerSizes[layerSizes.length - 1] = outputSize;

        this.layers = new Layer[layerSizes.length - 1];
        for (int i = 0; i < layers.length; i++) {
            Activation act = (i == layers.length - 1) ? outputAct : hiddenAct;
            layers[i] = new Layer(layerSizes[i], layerSizes[i + 1], act);
        }

        int maxHidden = inputSize;
        for (int s : hiddenSizes) if (s > maxHidden) maxHidden = s;
        maxHidden = Math.max(maxHidden, outputSize);
        this.buffer1 = new float[maxHidden];
        this.buffer2 = new float[maxHidden];
    }

    public float[] forward(float[] input, float[] output) {
        float[] currentInput = input;
        float[] currentOutput = buffer1;

        for (int i = 0; i < layers.length; i++) {
            layers[i].forward(currentInput, currentOutput);
            float[] temp = currentInput;
            currentInput = currentOutput;
            currentOutput = temp;
        }

        if (currentInput != output) {
            System.arraycopy(currentInput, 0, output, 0, output.length);
        }
        return output;
    }

    public void backward(float[] input, float[] gradOutput, float[] gradInput) {
        float[][] layerInputs = new float[layers.length][];
        float[][] layerOutputs = new float[layers.length][];

        float[] currentInput = input;
        for (int i = 0; i < layers.length; i++) {
            layerInputs[i] = currentInput;
            float[] out = (i == layers.length - 1) ? new float[layerSizes[i + 1]] : buffer1;
            layers[i].forward(currentInput, out);
            layerOutputs[i] = out;
            currentInput = out;
        }

        float[] currentGrad = gradOutput;
        for (int i = layers.length - 1; i >= 0; i--) {
            float[] nextGrad = (i > 0) ? buffer2 : gradInput;
            layers[i].backward(currentGrad, nextGrad);
            currentGrad = nextGrad;
        }
    }

    public void zeroGrad() {
        for (Layer l : layers) l.zeroGrad();
    }

    public void update(float lr) {
        for (Layer l : layers) l.update(lr);
    }

    public void clipGrad(float maxNorm) {
        for (Layer l : layers) l.clipGrad(maxNorm);
    }

    public int paramCount() {
        int sum = 0;
        for (Layer l : layers) sum += l.paramCount();
        return sum;
    }

    public Layer getLayer(int index) {
        return layers[index];
    }

    public int getLayerCount() {
        return layers.length;
    }
}