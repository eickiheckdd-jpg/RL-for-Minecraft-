package com.example.rlpvp.rl.network;

public interface Activation {
    void forward(float[] input, float[] output);
    void backward(float[] input, float[] gradOutput, float[] gradInput);
}

class ReLU implements Activation {
    @Override
    public void forward(float[] input, float[] output) {
        for (int i = 0; i < input.length; i++) {
            output[i] = Math.max(0f, input[i]);
        }
    }

    @Override
    public void backward(float[] input, float[] gradOutput, float[] gradInput) {
        for (int i = 0; i < input.length; i++) {
            gradInput[i] = input[i] > 0f ? gradOutput[i] : 0f;
        }
    }
}

class Tanh implements Activation {
    @Override
    public void forward(float[] input, float[] output) {
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) Math.tanh(input[i]);
        }
    }

    @Override
    public void backward(float[] input, float[] gradOutput, float[] gradInput) {
        for (int i = 0; i < input.length; i++) {
            float t = (float) Math.tanh(input[i]);
            gradInput[i] = gradOutput[i] * (1f - t * t);
        }
    }
}

class Linear implements Activation {
    @Override
    public void forward(float[] input, float[] output) {
        System.arraycopy(input, 0, output, 0, input.length);
    }

    @Override
    public void backward(float[] input, float[] gradOutput, float[] gradInput) {
        System.arraycopy(gradOutput, 0, gradInput, 0, gradOutput.length);
    }
}