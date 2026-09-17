package com.example.rlpvp;

import com.example.rlpvp.debug.DebugOverlay;
import com.example.rlpvp.rl.environment.TrainingManager;
import com.example.rlpvp.rl.minecraft.MinecraftIntegration;
import com.example.rlpvp.rl.minecraft.ActionExecutor;
import com.example.rlpvp.rl.minecraft.CombatEventHandler;
import com.example.rlpvp.rl.minecraft.TrainingArena;
import com.example.rlpvp.config.RLConfig;
import com.example.rlpvp.command.RLCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class RLMain implements ClientModInitializer {

    public static final String MOD_ID = "rlpvp";

    public static KeyBinding toggleHudKey;
    public static KeyBinding toggleTrainingKey;
    public static KeyBinding toggleEvaluationKey;
    public static KeyBinding saveCheckpointKey;
    public static KeyBinding loadCheckpointKey;

    public static boolean hudEnabled = false;
    public static boolean trainingEnabled = false;

    private static DebugOverlay debugOverlay;
    private static TrainingManager trainingManager;
    private static MinecraftIntegration minecraftIntegration;
    private static ActionExecutor actionExecutor;
    private static CombatEventHandler combatEventHandler;
    private static TrainingArena trainingArena;

    @Override
    public void onInitializeClient() {
        RLConfig.load();

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.rlpvp.toggle_hud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "category.rlpvp.controls"
        ));

        toggleTrainingKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.rlpvp.toggle_training",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.rlpvp.controls"
        ));

        toggleEvaluationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.rlpvp.toggle_evaluation",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.rlpvp.controls"
        ));

        saveCheckpointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.rlpvp.save_checkpoint",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F5,
            "category.rlpvp.controls"
        ));

        loadCheckpointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.rlpvp.load_checkpoint",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.rlpvp.controls"
        ));

        debugOverlay = new DebugOverlay();
        minecraftIntegration = new MinecraftIntegration();
        actionExecutor = new ActionExecutor();
        combatEventHandler = new CombatEventHandler();
        trainingArena = new TrainingArena();
        trainingManager = new TrainingManager(minecraftIntegration, actionExecutor, combatEventHandler, trainingArena);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds();
            
            if (hudEnabled) {
                debugOverlay.tick();
            }
            
            if (trainingEnabled) {
                trainingManager.tick();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            RLCommands.register(dispatcher);
        });

        System.out.println("[RL-PvP] Mod initialized. X=HUD, C=Training, V=Eval, F5=Save, F9=Load");
    }

    private static void handleKeybinds() {
        while (toggleHudKey.wasPressed()) {
            hudEnabled = !hudEnabled;
        }
        while (toggleTrainingKey.wasPressed()) {
            trainingEnabled = !trainingEnabled;
            trainingManager.setEnabled(trainingEnabled);
        }
        while (toggleEvaluationKey.wasPressed()) {
            trainingManager.toggleEvaluation();
        }
        while (saveCheckpointKey.wasPressed()) {
            trainingManager.saveCheckpoint("checkpoint_latest.bin");
        }
        while (loadCheckpointKey.wasPressed()) {
            trainingManager.loadCheckpoint("checkpoint_latest.bin");
        }
    }

    public static DebugOverlay getDebugOverlay() {
        return debugOverlay;
    }

    public static TrainingManager getTrainingManager() {
        return trainingManager;
    }

    public static MinecraftIntegration getMinecraftIntegration() {
        return minecraftIntegration;
    }

    public static ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public static CombatEventHandler getCombatEventHandler() {
        return combatEventHandler;
    }

    public static TrainingArena getTrainingArena() {
        return trainingArena;
    }
}