package com.example.rlpvp.rl.minecraft;

import com.example.rlpvp.rl.action.ActionSpace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;

public class ActionExecutor {

    private final MinecraftClient client;
    private float yawAccumulator = 0f;
    private float pitchAccumulator = 0f;
    private int attackCooldown = 0;
    private boolean wasAttacking = false;

    public ActionExecutor() {
        this.client = MinecraftClient.getInstance();
    }

    public void execute(float[] action) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        executeMovement(action, player);
        executeLook(action, player);
        executeAttack(action, player);
    }

    private void executeMovement(float[] action, ClientPlayerEntity player) {
        KeyBinding.forwardKey.setPressed(action[ActionSpace.IDX_FORWARD] > 0.5f);
        KeyBinding.backKey.setPressed(action[ActionSpace.IDX_BACKWARD] > 0.5f);
        KeyBinding.leftKey.setPressed(action[ActionSpace.IDX_LEFT] > 0.5f);
        KeyBinding.rightKey.setPressed(action[ActionSpace.IDX_RIGHT] > 0.5f);
        KeyBinding.sprintKey.setPressed(action[ActionSpace.IDX_SPRINT] > 0.5f);
        
        if (action[ActionSpace.IDX_JUMP] > 0.5f && player.isOnGround()) {
            player.jump();
        }
    }

    private void executeLook(float[] action, ClientPlayerEntity player) {
        float yawDelta = action[ActionSpace.IDX_YAW_DELTA];
        float pitchDelta = action[ActionSpace.IDX_PITCH_DELTA];

        float maxYawPerTick = 30f;
        float maxPitchPerTick = 30f;

        float scaledYaw = yawDelta * maxYawPerTick;
        float scaledPitch = pitchDelta * maxPitchPerTick;

        yawAccumulator += scaledYaw;
        pitchAccumulator += scaledPitch;

        float appliedYaw = Math.round(yawAccumulator);
        float appliedPitch = Math.round(pitchAccumulator);

        if (appliedYaw != 0 || appliedPitch != 0) {
            player.setYaw(player.getYaw() + appliedYaw);
            player.setPitch(clampPitch(player.getPitch() + appliedPitch));
            
            yawAccumulator -= appliedYaw;
            pitchAccumulator -= appliedPitch;
        }
    }

    private void executeAttack(float[] action, ClientPlayerEntity player) {
        boolean shouldAttack = action[ActionSpace.IDX_ATTACK] > 0.5f;

        if (shouldAttack && !wasAttacking && attackCooldown <= 0) {
            if (player.getAttackCooldownProgress(0f) >= 0.9f) {
                client.interactionManager.attackEntity(player, getTargetEntity());
                attackCooldown = (int) (10 / Math.max(0.1, player.getAttackCooldownProgress(0f)));
            }
        }

        wasAttacking = shouldAttack;
        
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

    private net.minecraft.entity.Entity getTargetEntity() {
        ClientPlayerEntity player = client.player;
        if (player == null) return null;

        var hitResult = player.raycast(5.0, 0.0f, false);
        if (hitResult != null && hitResult.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
            return ((net.minecraft.util.hit.EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    private float clampPitch(float pitch) {
        return Math.max(-90f, Math.min(90f, pitch));
    }

    public void reset() {
        yawAccumulator = 0f;
        pitchAccumulator = 0f;
        attackCooldown = 0;
        wasAttacking = false;
        
        ClientPlayerEntity player = client.player;
        if (player != null) {
            KeyBinding.forwardKey.setPressed(false);
            KeyBinding.backKey.setPressed(false);
            KeyBinding.leftKey.setPressed(false);
            KeyBinding.rightKey.setPressed(false);
            KeyBinding.sprintKey.setPressed(false);
        }
    }
}