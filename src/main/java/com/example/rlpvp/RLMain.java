package com.example.rlpvp;

import com.example.rlpvp.debug.DebugOverlay;
import com.example.rlpvp.rl.environment.TrainingManager;

public class RLMain {

    public static final String MOD_ID = "rlpvp";

    public static Object toggleHudKey;
    public static Object toggleTrainingKey;

    public static boolean hudEnabled = false;
    public static boolean trainingEnabled = false;

    private static DebugOverlay debugOverlay;
    private static TrainingManager trainingManager;

    public void onInitializeClient() {
        debugOverlay = new DebugOverlay();
        trainingManager = new TrainingManager();

        hudEnabled = false;
        trainingEnabled = false;
    }

    public static void toggleHud() {
        hudEnabled = !hudEnabled;
    }

    public static void toggleTraining() {
        trainingEnabled = !trainingEnabled;
        trainingManager.setEnabled(trainingEnabled);
    }

    public static void tick() {
        if (hudEnabled) {
            debugOverlay.tick();
        }
        if (trainingEnabled) {
            trainingManager.tick();
        }
    }

    public static DebugOverlay getDebugOverlay() {
        return debugOverlay;
    }

    public static TrainingManager getTrainingManager() {
        return trainingManager;
    }
}