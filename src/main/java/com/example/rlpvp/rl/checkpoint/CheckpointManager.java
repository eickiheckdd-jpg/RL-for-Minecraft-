package com.example.rlpvp.rl.checkpoint;

import com.example.rlpvp.rl.network.ActorCritic;
import com.example.rlpvp.rl.curriculum.CurriculumManager;

import java.io.*;

public class CheckpointManager {
    private static final String CHECKPOINT_DIR = "rlpvp_checkpoints";

    public static void save(String filename, ActorCritic model, CurriculumManager curriculum,
                            long trainingStep, float[] obsMean, float[] obsStd,
                            float lr, int historyLength, int[] hiddenSizes) throws IOException {
        File dir = new File(CHECKPOINT_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, filename);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeInt(1);

            out.writeLong(trainingStep);
            out.writeInt(curriculum.getCurrentStageId());
            out.writeInt(curriculum.getTotalEpisodes());
            out.writeFloat(lr);
            out.writeInt(historyLength);
            out.writeInt(hiddenSizes.length);
            for (int s : hiddenSizes) out.writeInt(s);

            if (obsMean != null) {
                out.writeInt(obsMean.length);
                for (float v : obsMean) out.writeFloat(v);
            } else {
                out.writeInt(0);
            }

            if (obsStd != null) {
                out.writeInt(obsStd.length);
                for (float v : obsStd) out.writeFloat(v);
            } else {
                out.writeInt(0);
            }

            saveModel(out, model);
        }
    }

    private static void saveModel(DataOutputStream out, ActorCritic model) throws IOException {
        saveLayer(out, model.getLayer(0));
        for (int i = 1; i < model.getLayerCount(); i++) {
            saveLayer(out, model.getLayer(i));
        }

        out.writeInt(model.getLayerCount() + 2);
        saveLayer(out, model.getLayer(0));
        saveLayer(out, model.getLayer(model.getLayerCount() - 1));
    }

    private static void saveLayer(DataOutputStream out, com.example.rlpvp.rl.network.Layer layer) throws IOException {
        out.writeInt(layer.inputSize);
        out.writeInt(layer.outputSize);

        for (float v : layer.weight) out.writeFloat(v);
        for (float v : layer.bias) out.writeFloat(v);
    }

    public static CheckpointData load(String filename, int obsDim, int actionDim) throws IOException {
        File file = new File(CHECKPOINT_DIR, filename);
        if (!file.exists()) return null;

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            int version = in.readInt();

            long trainingStep = in.readLong();
            int curriculumStage = in.readInt();
            int totalEpisodes = in.readInt();
            float lr = in.readFloat();
            int historyLength = in.readInt();
            int hiddenCount = in.readInt();
            int[] hiddenSizes = new int[hiddenCount];
            for (int i = 0; i < hiddenCount; i++) hiddenSizes[i] = in.readInt();

            int meanLen = in.readInt();
            float[] obsMean = meanLen > 0 ? new float[meanLen] : null;
            for (int i = 0; i < meanLen; i++) obsMean[i] = in.readFloat();

            int stdLen = in.readInt();
            float[] obsStd = stdLen > 0 ? new float[stdLen] : null;
            for (int i = 0; i < stdLen; i++) obsStd[i] = in.readFloat();

            ActorCritic model = new ActorCritic(obsDim, hiddenSizes, actionDim);
            loadModel(in, model);

            return new CheckpointData(model, trainingStep, curriculumStage, totalEpisodes, 
                lr, historyLength, hiddenSizes, obsMean, obsStd);
        }
    }

    private static void loadModel(DataInputStream in, ActorCritic model) throws IOException {
        int layerCount = in.readInt();
        for (int i = 0; i < layerCount; i++) {
            if (i < model.getLayerCount()) {
                loadLayer(in, model.getLayer(i));
            } else {
                skipLayer(in);
            }
        }
    }

    private static void loadLayer(DataInputStream in, com.example.rlpvp.rl.network.Layer layer) throws IOException {
        int inputSize = in.readInt();
        int outputSize = in.readInt();

        for (int i = 0; i < layer.weight.length; i++) {
            layer.weight[i] = in.readFloat();
        }
        for (int i = 0; i < layer.bias.length; i++) {
            layer.bias[i] = in.readFloat();
        }
    }

    private static void skipLayer(DataInputStream in) throws IOException {
        int inputSize = in.readInt();
        int outputSize = in.readInt();
        int weightSize = inputSize * outputSize;
        in.skipBytes(weightSize * 4 + outputSize * 4);
    }

    public static class CheckpointData {
        public final ActorCritic model;
        public final long trainingStep;
        public final int curriculumStage;
        public final int totalEpisodes;
        public final float lr;
        public final int historyLength;
        public final int[] hiddenSizes;
        public final float[] obsMean;
        public final float[] obsStd;

        public CheckpointData(ActorCritic model, long trainingStep, int curriculumStage,
                              int totalEpisodes, float lr, int historyLength, int[] hiddenSizes,
                              float[] obsMean, float[] obsStd) {
            this.model = model;
            this.trainingStep = trainingStep;
            this.curriculumStage = curriculumStage;
            this.totalEpisodes = totalEpisodes;
            this.lr = lr;
            this.historyLength = historyLength;
            this.hiddenSizes = hiddenSizes;
            this.obsMean = obsMean;
            this.obsStd = obsStd;
        }
    }
}