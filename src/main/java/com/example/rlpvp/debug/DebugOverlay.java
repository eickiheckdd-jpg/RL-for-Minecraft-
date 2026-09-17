package com.example.rlpvp.debug;

import com.example.rlpvp.RLMain;
import com.example.rlpvp.rl.environment.TrainingManager;
import com.example.rlpvp.rl.minecraft.MinecraftIntegration;
import com.example.rlpvp.rl.minecraft.CombatEventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class DebugOverlay {

    public void tick() {
    }

    public void render(DrawContext context) {
        if (!RLMain.hudEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        TrainingManager tm = RLMain.getTrainingManager();
        if (tm == null) return;

        MinecraftIntegration mi = RLMain.getMinecraftIntegration();
        CombatEventHandler ceh = RLMain.getCombatEventHandler();

        int x = 10;
        int y = 10;
        int lineHeight = 12;
        int color = 0xFFFFFFFF;

        context.drawTextWithShadow(client.textRenderer, Text.literal("=== RL-PvP Debug ==="), x, y, color);
        y += lineHeight;

        context.drawTextWithShadow(client.textRenderer, Text.literal("Training: " + (RLMain.trainingEnabled ? "§aON" : "§cOFF")), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("HUD: " + (RLMain.hudEnabled ? "§aON" : "§cOFF")), x, y, color);
        y += lineHeight;

        context.drawTextWithShadow(client.textRenderer, Text.literal("Step: " + tm.getTrainingStep()), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Episode: " + tm.getEpisodeCount()), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Ep. Reward: " + String.format("%.2f", tm.getEpisodeReward())), x, y, color);
        y += lineHeight;

        var cm = tm.getCurriculumManager();
        context.drawTextWithShadow(client.textRenderer, Text.literal("Stage: " + cm.getCurrentStage().getName() + " (" + cm.getCurrentStageId() + ")"), x, y, 0xFFFFFF00);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Ep in Stage: " + cm.getEpisodesInStage() + "/" + cm.getCurrentStage().getMinEpisodes()), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Win Rate: " + String.format("%.1f%%", tm.getWinRate() * 100)), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Hit Rate: " + String.format("%.1f%%", tm.getHitRate() * 100)), x, y, color);
        y += lineHeight;

        y += 4;
        context.drawTextWithShadow(client.textRenderer, Text.literal("--- Combat ---"), x, y, 0xFFFFFF00);
        y += lineHeight;

        if (mi.getTarget() != null) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Target: " + mi.getTarget().getName().getString()), x, y, color);
            y += lineHeight;
            context.drawTextWithShadow(client.textRenderer, Text.literal("Target HP: " + String.format("%.1f", mi.getTarget().getHealth())), x, y, color);
            y += lineHeight;
            context.drawTextWithShadow(client.textRenderer, Text.literal("Dist: " + String.format("%.1f", ceh.getDistanceToTarget())), x, y, color);
            y += lineHeight;
        } else {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Target: None"), x, y, 0xFFFF5555);
            y += lineHeight;
        }

        context.drawTextWithShadow(client.textRenderer, Text.literal("Combo: " + ceh.getComboCount()), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Last Hit: " + (ceh.wasLastAttackHit() ? "§aYES" : "§cNO") + (ceh.wasLastAttackCritical() ? " §e(CRIT)" : "")), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Dmg Dealt: " + String.format("%.1f", ceh.getLastDamageDealt())), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Dmg Taken: " + String.format("%.1f", ceh.getLastDamageTaken())), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Consecutive Hits: " + ceh.getConsecutiveHits()), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Consecutive Whiffs: " + ceh.getConsecutiveWhiffs()), x, y, color);
        y += lineHeight;

        y += 4;
        context.drawTextWithShadow(client.textRenderer, Text.literal("--- Player ---"), x, y, 0xFFFFFF00);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("HP: " + String.format("%.1f", client.player.getHealth())), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Yaw: " + String.format("%.1f", client.player.getYaw())), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Pitch: " + String.format("%.1f", client.player.getPitch())), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Sprinting: " + (client.player.isSprinting() ? "YES" : "NO")), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("On Ground: " + (client.player.isOnGround() ? "YES" : "NO")), x, y, color);
        y += lineHeight;
        context.drawTextWithShadow(client.textRenderer, Text.literal("Attack CD: " + String.format("%.2f", client.player.getAttackCooldownProgress(0f))), x, y, color);
    }
}