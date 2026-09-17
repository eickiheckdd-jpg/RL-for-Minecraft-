package com.example.rlpvp.rl.network;

public final class Tensor {
    public final float[] data;
    public final int[] shape;

    public Tensor(int... shape) {
        this.shape = shape.clone();
        int size = 1;
        for (int dim : shape) size *= dim;
        this.data = new float[size];
    }

    public Tensor(float[] data, int... shape) {
        this.data = data;
        this.shape = shape.clone();
    }

    public static Tensor zeros(int... shape) {
        return new Tensor(shape);
    }

    public static Tensor ones(int... shape) {
        Tensor t = new Tensor(shape);
        for (int i = 0; i < t.data.length; i++) t.data[i] = 1f;
        return t;
    }

    public static Tensor from(float[] data) {
        return new Tensor(data, data.length);
    }

    public int size() {
        return data.length;
    }

    public float get(int i) {
        return data[i];
    }

    public void set(int i, float v) {
        data[i] = v;
    }

    public float get(int... indices) {
        int idx = 0;
        int stride = 1;
        for (int i = indices.length - 1; i >= 0; i--) {
            idx += indices[i] * stride;
            stride *= shape[i];
        }
        return data[idx];
    }

    public void set(int[] indices, float v) {
        int idx = 0;
        int stride = 1;
        for (int i = indices.length - 1; i >= 0; i--) {
            idx += indices[i] * stride;
            stride *= shape[i];
        }
        data[idx] = v;
    }

    public Tensor reshape(int... newShape) {
        int newSize = 1;
        for (int d : newShape) newSize *= d;
        if (newSize != data.length) throw new IllegalArgumentException("Size mismatch");
        return new Tensor(data, newShape);
    }

    public Tensor copy() {
        float[] copy = new float[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return new Tensor(copy, shape);
    }

    public void fill(float v) {
        for (int i = 0; i < data.length; i++) data[i] = v;
    }

    public void add(Tensor other) {
        if (other.data.length != data.length) throw new IllegalArgumentException("Shape mismatch");
        for (int i = 0; i < data.length; i++) data[i] += other.data[i];
    }

    public void mul(float scalar) {
        for (int i = 0; i < data.length; i++) data[i] *= scalar;
    }

    public void addMul(Tensor other, float scalar) {
        if (other.data.length != data.length) throw new IllegalArgumentException("Shape mismatch");
        for (int i = 0; i < data.length; i++) data[i] += other.data[i] * scalar;
    }
}