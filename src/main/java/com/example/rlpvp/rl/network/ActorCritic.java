package com.example.rlpvp.rl.network;

public class ActorCritic {
    private final MLP shared;
    private final Layer actorHead;
    private final Layer criticHead;

    private final float[] sharedOut;
    private final float[] actionLogits;
    private final float[] valueOut;

    public ActorCritic(int obsDim, int[] hiddenSizes, int actionDim) {
        int lastHidden = hiddenSizes[hiddenSizes.length - 1];
        this.shared = new MLP(obsDim, hiddenSizes, lastHidden, Activations.RELU, Activations.RELU);
        this.actorHead = new Layer(lastHidden, actionDim, Activations.LINEAR);
        this.criticHead = new Layer(lastHidden, 1, Activations.LINEAR);

        this.sharedOut = new float[lastHidden];
        this.actionLogits = new float[actionDim];
        this.valueOut = new float[1];
    }

    public void forward(float[] obs, float[] actionLogitsOut, float[] valueOut) {
        shared.forward(obs, sharedOut);
        actorHead.forward(sharedOut, actionLogitsOut);
        criticHead.forward(sharedOut, valueOut);
    }

    public void forward(float[] obs) {
        forward(obs, actionLogits, valueOut);
    }

    public float[] getActionLogits() {
        return actionLogits;
    }

    public float getValue() {
        return valueOut[0];
    }

    public void backward(float[] obs, float[] actionGrad, float valueGrad, float[] obsGrad) {
        float[] sharedGrad = new float[sharedOut.length];
        float[] valueGradArr = new float[] { valueGrad };

        criticHead.backward(valueGradArr, sharedGrad);
        actorHead.backward(actionGrad, sharedGrad);

        shared.backward(obs, sharedGrad, obsGrad);
    }

    public void zeroGrad() {
        shared.zeroGrad();
        actorHead.zeroGrad();
        criticHead.zeroGrad();
    }

    public void update(float lr) {
        shared.update(lr);
        actorHead.update(lr);
        criticHead.update(lr);
    }

    public void clipGrad(float maxNorm) {
        shared.clipGrad(maxNorm);
        actorHead.clipGrad(maxNorm);
        criticHead.clipGrad(maxNorm);
    }

    public int paramCount() {
        return shared.paramCount() + actorHead.paramCount() + criticHead.paramCount();
    }

    public void copyFrom(ActorCritic other) {
        copyLayerParams(shared.getLayer(0), other.shared.getLayer(0));
        for (int i = 1; i < shared.getLayerCount(); i++) {
            copyLayerParams(shared.getLayer(i), other.shared.getLayer(i));
        }
        copyLayerParams(actorHead, other.actorHead);
        copyLayerParams(criticHead, other.criticHead);
    }

    public Layer getLayer(int index) {
        return shared.getLayer(index);
    }

    public int getLayerCount() {
        return shared.getLayerCount();
    }

    private void copyLayerParams(Layer dst, Layer src) {
        System.arraycopy(src.weight, 0, dst.weight, 0, src.weight.length);
        System.arraycopy(src.bias, 0, dst.bias, 0, src.bias.length);
    }
}