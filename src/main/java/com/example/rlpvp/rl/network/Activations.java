package com.example.rlpvp.rl.network;

public final class Activations {
    public static final Activation RELU = new ReLU();
    public static final Activation TANH = new Tanh();
    public static final Activation LINEAR = new Linear();

    private Activations() {}
}