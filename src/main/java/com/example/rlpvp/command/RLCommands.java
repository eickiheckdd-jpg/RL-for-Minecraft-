package com.example.rlpvp.command;

import com.example.rlpvp.RLMain;
import com.example.rlpvp.config.RLConfig;
import com.example.rlpvp.rl.environment.TrainingManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class RLCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            net.minecraft.command.argument.CommandSyntaxException.class.getSimpleName().isEmpty() ?
                net.minecraft.command.CommandManager.literal("rlpvp")
                    .then(net.minecraft.command.CommandManager.literal("train")
                        .executes(RLCommands::cmdTrain)
                        .then(net.minecraft.command.CommandManager.literal("on").executes(RLCommands::cmdTrainOn))
                        .then(net.minecraft.command.CommandManager.literal("off").executes(RLCommands::cmdTrainOff))
                    )
                    .then(net.minecraft.command.CommandManager.literal("eval")
                        .executes(RLCommands::cmdEval)
                        .then(net.minecraft.command.CommandManager.argument("episodes", IntegerArgumentType.integer(1, 100))
                            .executes(RLCommands::cmdEvalEpisodes))
                    )
                    .then(net.minecraft.command.CommandManager.literal("checkpoint")
                        .then(net.minecraft.command.CommandManager.literal("save")
                            .executes(RLCommands::cmdCheckpointSave)
                            .then(net.minecraft.command.CommandManager.argument("name", StringArgumentType.string())
                                .executes(RLCommands::cmdCheckpointSaveName))
                        )
                        .then(net.minecraft.command.CommandManager.literal("load")
                            .executes(RLCommands::cmdCheckpointLoad)
                            .then(net.minecraft.command.CommandManager.argument("name", StringArgumentType.string())
                                .executes(RLCommands::cmdCheckpointLoadName))
                        )
                    )
                    .then(net.minecraft.command.CommandManager.literal("curriculum")
                        .then(net.minecraft.command.CommandManager.literal("stage")
                            .then(net.minecraft.command.CommandManager.argument("stage", IntegerArgumentType.integer(0, 8))
                                .executes(RLCommands::cmdCurriculumStage))
                        )
                        .then(net.minecraft.command.CommandManager.literal("info").executes(RLCommands::cmdCurriculumInfo))
                    )
                    .then(net.minecraft.command.CommandManager.literal("config")
                        .then(net.minecraft.command.CommandManager.literal("lr")
                            .then(net.minecraft.command.CommandManager.argument("value", FloatArgumentType.floatArg(0.00001f, 0.1f))
                                .executes(RLCommands::cmdConfigLr))
                        )
                        .then(net.minecraft.command.CommandManager.literal("reset").executes(RLCommands::cmdConfigReset))
                    )
                    .then(net.minecraft.command.CommandManager.literal("hud")
                        .executes(RLCommands::cmdHud)
                        .then(net.minecraft.command.CommandManager.literal("on").executes(RLCommands::cmdHudOn))
                        .then(net.minecraft.command.CommandManager.literal("off").executes(RLCommands::cmdHudOff))
                    )
                    .then(net.minecraft.command.CommandManager.literal("arena")
                        .then(net.minecraft.command.CommandManager.literal("set").executes(RLCommands::cmdArenaSet))
                        .then(net.minecraft.command.CommandManager.literal("reset").executes(RLCommands::cmdArenaReset))
                    )
                    .then(net.minecraft.command.CommandManager.literal("status").executes(RLCommands::cmdStatus))
                : net.minecraft.command.CommandManager.literal("rlpvp")
        );
    }

    private static int cmdTrain(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] Training: " + (tm.isEnabled() ? "§aON" : "§cOFF") + " §7- use 'on' or 'off'"), false);
        return 1;
    }

    private static int cmdTrainOn(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        tm.setEnabled(true);
        RLMain.trainingEnabled = true;
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Training started"), false);
        return 1;
    }

    private static int cmdTrainOff(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        tm.setEnabled(false);
        RLMain.trainingEnabled = false;
        ctx.getSource().sendFeedback(() -> Text.literal("§c[RL-PvP] Training stopped"), false);
        return 1;
    }

    private static int cmdEval(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] Starting evaluation..."), false);
        var metrics = tm.evaluate(RLConfig.INSTANCE.training.evalEpisodes);
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Evaluation complete: " + formatMetrics(metrics)), false);
        return 1;
    }

    private static int cmdEvalEpisodes(CommandContext<ServerCommandSource> ctx) {
        int episodes = IntegerArgumentType.getInteger(ctx, "episodes");
        TrainingManager tm = RLMain.getTrainingManager();
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] Starting evaluation (" + episodes + " episodes)..."), false);
        var metrics = tm.evaluate(episodes);
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Evaluation complete: " + formatMetrics(metrics)), false);
        return 1;
    }

    private static int cmdCheckpointSave(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        try {
            tm.saveCheckpoint("checkpoint_latest.bin");
            ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Checkpoint saved"), false);
        } catch (Exception e) {
            ctx.getSource().sendFeedback(() -> Text.literal("§c[RL-PvP] Failed to save: " + e.getMessage()), false);
        }
        return 1;
    }

    private static int cmdCheckpointSaveName(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (!name.endsWith(".bin")) name += ".bin";
        TrainingManager tm = RLMain.getTrainingManager();
        try {
            tm.saveCheckpoint(name);
            ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Checkpoint saved as " + name), false);
        } catch (Exception e) {
            ctx.getSource().sendFeedback(() -> Text.literal("§c[RL-PvP] Failed to save: " + e.getMessage()), false);
        }
        return 1;
    }

    private static int cmdCheckpointLoad(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        try {
            tm.loadCheckpoint("checkpoint_latest.bin");
            ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Checkpoint loaded"), false);
        } catch (Exception e) {
            ctx.getSource().sendFeedback(() -> Text.literal("§c[RL-PvP] Failed to load: " + e.getMessage()), false);
        }
        return 1;
    }

    private static int cmdCheckpointLoadName(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        if (!name.endsWith(".bin")) name += ".bin";
        TrainingManager tm = RLMain.getTrainingManager();
        try {
            tm.loadCheckpoint(name);
            ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Checkpoint loaded from " + name), false);
        } catch (Exception e) {
            ctx.getSource().sendFeedback(() -> Text.literal("§c[RL-PvP] Failed to load: " + e.getMessage()), false);
        }
        return 1;
    }

    private static int cmdCurriculumStage(CommandContext<ServerCommandSource> ctx) {
        int stage = IntegerArgumentType.getInteger(ctx, "stage");
        TrainingManager tm = RLMain.getTrainingManager();
        tm.getCurriculumManager().setStage(stage);
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Curriculum stage set to " + stage), false);
        return 1;
    }

    private static int cmdCurriculumInfo(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        var cm = tm.getCurriculumManager();
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] Stage: " + cm.getCurrentStage().getName() + " (" + cm.getCurrentStageId() + ")"), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] Episodes in stage: " + cm.getEpisodesInStage()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] Win rate: " + String.format("%.1f%%", cm.getWinRate() * 100) + ", Hit rate: " + String.format("%.1f%%", cm.getHitRate() * 100)), false);
        return 1;
    }

    private static int cmdConfigLr(CommandContext<ServerCommandSource> ctx) {
        float lr = FloatArgumentType.getFloat(ctx, "value");
        RLConfig.INSTANCE.ppo.learningRate = lr;
        RLConfig.save();
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Learning rate set to " + lr), false);
        return 1;
    }

    private static int cmdConfigReset(CommandContext<ServerCommandSource> ctx) {
        RLConfig.INSTANCE = new RLConfig();
        RLConfig.save();
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Config reset to defaults"), false);
        return 1;
    }

    private static int cmdHud(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("§e[RL-PvP] HUD: " + (RLMain.hudEnabled ? "§aON" : "§cOFF") + " §7- use 'on' or 'off'"), false);
        return 1;
    }

    private static int cmdHudOn(CommandContext<ServerCommandSource> ctx) {
        RLMain.hudEnabled = true;
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] HUD enabled"), false);
        return 1;
    }

    private static int cmdHudOff(CommandContext<ServerCommandSource> ctx) {
        RLMain.hudEnabled = false;
        ctx.getSource().sendFeedback(() -> Text.literal("§c[RL-PvP] HUD disabled"), false);
        return 1;
    }

    private static int cmdArenaSet(CommandContext<ServerCommandSource> ctx) {
        var arena = RLMain.getTrainingArena();
        var player = ctx.getSource().getEntity();
        if (player != null) {
            arena.setArenaCenter(player.getPos());
            ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Arena center set to current position"), false);
        }
        return 1;
    }

    private static int cmdArenaReset(CommandContext<ServerCommandSource> ctx) {
        var arena = RLMain.getTrainingArena();
        arena.setArenaCenter(net.minecraft.util.math.Vec3d.ZERO);
        ctx.getSource().sendFeedback(() -> Text.literal("§a[RL-PvP] Arena reset"), false);
        return 1;
    }

    private static int cmdStatus(CommandContext<ServerCommandSource> ctx) {
        TrainingManager tm = RLMain.getTrainingManager();
        ctx.getSource().sendFeedback(() -> Text.literal("§e=== RL-PvP Status ==="), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Training: " + (RLMain.trainingEnabled ? "§aON" : "§cOFF")), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Step: " + tm.getTrainingStep()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Episodes: " + tm.getEpisodeCount()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Episode Reward: " + String.format("%.2f", tm.getEpisodeReward())), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Curriculum: " + tm.getCurriculumManager().getCurrentStage().getName()), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Win Rate: " + String.format("%.1f%%", tm.getWinRate() * 100)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("§7Hit Rate: " + String.format("%.1f%%", tm.getHitRate() * 100)), false);
        return 1;
    }

    private static String formatMetrics(com.example.rlpvp.rl.evaluation.EvaluationMetrics m) {
        return String.format("Win: %.1f%% | Dmg Dealt: %.1f | Dmg Taken: %.1f | Hit: %.1f%% | Whiff: %.1f%% | Combo: %d | Survive: %.1fs",
            m.getWinRate() * 100, m.getAvgDamageDealt(), m.getAvgDamageTaken(),
            m.getHitRate() * 100, m.getWhiffRate() * 100,
            m.getMaxCombo(), m.getAvgSurvivalTime());
    }
}