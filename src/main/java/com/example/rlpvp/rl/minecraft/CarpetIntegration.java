package com.example.rlpvp.rl.minecraft;

import com.example.rlpvp.rl.network.ActorCritic;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CarpetIntegration {

    private static final String CARPET_MOD_ID = "carpet";
    private static boolean carpetAvailable = false;
    private static Class<?> fakePlayerClass = null;
    private static Class<?> carpetServerClass = null;

    private final List<FakePlayerWrapper> fakePlayers = new ArrayList<>();
    private final Random random = new Random();
    private ActorCritic currentPolicy = null;

    static {
        try {
            carpetServerClass = Class.forName("carpet.CarpetServer");
            fakePlayerClass = Class.forName("carpet.fakes.FakePlayerEntity");
            carpetAvailable = true;
            System.out.println("[RL-PvP] Carpet mod detected - self-play enabled");
        } catch (ClassNotFoundException e) {
            System.out.println("[RL-PvP] Carpet mod not found - self-play will use scripted opponents");
        }
    }

    public static boolean isCarpetAvailable() {
        return carpetAvailable;
    }

    public void setPolicy(ActorCritic policy) {
        this.currentPolicy = policy;
    }

    public LivingEntity createFakePlayer(Vec3d spawnPos) {
        if (!carpetAvailable) return null;

        try {
            var server = getServer();
            if (server == null) return null;

            Object fakePlayer = fakePlayerClass
                .getConstructor(net.minecraft.server.world.ServerWorld.class, 
                               net.minecraft.util.math.BlockPos.class, 
                               float.class, float.class, 
                               net.minecraft.text.Text.class)
                .newInstance(server.getOverworld(), 
                           new net.minecraft.util.math.BlockPos(spawnPos.x, spawnPos.y, spawnPos.z),
                           0f, 0f, Text.literal("RL-Bot"));

            var entity = (LivingEntity) fakePlayer;
            entity.setHealth(20f);
            entity.setAbsorptionAmount(0f);
            
            FakePlayerWrapper wrapper = new FakePlayerWrapper(entity);
            fakePlayers.add(wrapper);
            
            server.getOverworld().spawnEntity(entity);
            return entity;
        } catch (Exception e) {
            System.err.println("[RL-PvP] Failed to create fake player: " + e.getMessage());
            return null;
        }
    }

    public void removeAllFakePlayers() {
        for (FakePlayerWrapper wrapper : fakePlayers) {
            wrapper.discard();
        }
        fakePlayers.clear();
    }

    public void tickFakePlayers(Vec3d agentPos) {
        if (currentPolicy == null) return;

        for (FakePlayerWrapper wrapper : fakePlayers) {
            if (!wrapper.entity.isAlive()) continue;
            
            float[] obs = collectFakePlayerObs(wrapper.entity, agentPos);
            if (obs == null) continue;

            currentPolicy.forward(obs);
            float[] logits = currentPolicy.getActionLogits();
            float[] action = new float[logits.length];
            
            for (int i = 0; i < logits.length; i++) {
                action[i] = logits[i] > 0 ? 1f : 0f;
            }

            applyActionToFakePlayer(wrapper, action);
        }
    }

    private float[] collectFakePlayerObs(LivingEntity fakePlayer, Vec3d agentPos) {
        float[] obs = new float[66]; // Same as ObservationEncoder.OBS_DIM
        
        Vec3d fakePos = fakePlayer.getPos();
        Vec3d fakeVel = fakePlayer.getVelocity();
        
        // Self (fake player) - 20 dims
        obs[0] = fakePlayer.getHealth() / 20f;
        obs[1] = fakePlayer.getAbsorptionAmount() / 20f;
        obs[2] = (float) fakePos.x;
        obs[3] = (float) fakePos.y;
        obs[4] = (float) fakePos.z;
        obs[5] = (float) fakeVel.x;
        obs[6] = (float) fakeVel.y;
        obs[7] = (float) fakeVel.z;
        obs[8] = (float) Math.sqrt(fakeVel.x * fakeVel.x + fakeVel.z * fakeVel.z);
        obs[9] = (float) fakeVel.y;
        obs[10] = fakePlayer.getYaw() / 180f - 1f;
        obs[11] = fakePlayer.getPitch() / 90f;
        obs[12] = fakePlayer.isOnGround() ? 1f : 0f;
        obs[13] = fakePlayer.isSprinting() ? 1f : 0f;
        obs[14] = fakePlayer.isSneaking() ? 1f : 0f;
        obs[15] = fakePlayer.getAttackCooldownProgress(0f) * 20f;
        obs[16] = fakePlayer.hurtTime;
        obs[17] = getMovementState(fakePlayer);
        obs[18] = getWeaponState(fakePlayer);
        obs[19] = 0f;

        // Target (agent) - 18 dims
        double dx = agentPos.x - fakePos.x;
        double dy = agentPos.y - fakePos.y;
        double dz = agentPos.z - fakePos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        obs[20] = (float) dx;
        obs[21] = (float) dy;
        obs[22] = (float) dz;
        obs[23] = (float) dist;
        obs[24] = (float) Math.sqrt(dx * dx + dz * dz);
        obs[25] = (float) dy;
        
        Vec3d agentVel = getAgentVelocity();
        obs[26] = (float) agentVel.x;
        obs[27] = (float) agentVel.y;
        obs[28] = (float) agentVel.z;
        obs[29] = (float) Math.sqrt(agentVel.x * agentVel.x + agentVel.z * agentVel.z);
        obs[30] = (float) agentVel.y;
        
        obs[31] = 20f / 20f; // agent health (would need to track)
        obs[32] = 0f; // agent yaw
        obs[33] = 0f; // agent pitch
        obs[34] = 1f; // agent onGround
        obs[35] = 0f; // agent sprinting
        obs[36] = 0f; // agent attacking
        obs[37] = 0f; // agent hurtTime

        // Combat - 12 dims
        obs[38] = fakePlayer.getAttackCooldownProgress(0f) * 20f;
        obs[39] = 3.5f;
        obs[40] = 0f; // recentDamageDealt
        obs[41] = 0f; // recentDamageReceived
        obs[42] = 0f; // timeSinceLastHit
        obs[43] = 0f; // timeSinceReceivedLastHit
        obs[44] = 0f; // comboLength
        obs[45] = 0f; // recentWhiff
        obs[46] = 0.5f; // targetVulnerability
        obs[47] = (float) dist;
        obs[48] = canCritical(fakePlayer) ? 1f : 0f;
        obs[49] = 0f; // blockState

        // Aim - 8 dims
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, hDist));
        float yawDiff = normalizeAngle(targetYaw - fakePlayer.getYaw());
        float pitchDiff = targetPitch - fakePlayer.getPitch();
        
        obs[50] = yawDiff;
        obs[51] = pitchDiff;
        obs[52] = yawDiff;
        obs[53] = 0f;
        obs[54] = Math.abs(yawDiff);
        obs[55] = Math.abs(pitchDiff);
        obs[56] = (Math.abs(yawDiff) < 15 && Math.abs(pitchDiff) < 15) ? 1f : 0f;
        obs[57] = 1f;

        // Movement - 8 dims
        obs[58] = (float) (-(fakeVel.x * dx + fakeVel.z * dz) / Math.max(0.1, dist));
        obs[59] = 0f;
        obs[60] = 0f;
        obs[61] = 0f;
        obs[62] = 0f;
        obs[63] = 0f;
        obs[64] = 0f;
        obs[65] = fakePlayer.isSprinting() ? 1f : 0f;

        return obs;
    }

    private void applyActionToFakePlayer(FakePlayerWrapper wrapper, float[] action) {
        LivingEntity entity = wrapper.entity;
        
        boolean forward = action.length > 0 && action[0] > 0.5f;
        boolean backward = action.length > 1 && action[1] > 0.5f;
        boolean left = action.length > 2 && action[2] > 0.5f;
        boolean right = action.length > 3 && action[3] > 0.5f;
        boolean sprint = action.length > 4 && action[4] > 0.5f;
        boolean jump = action.length > 5 && action[5] > 0.5f;
        float yawDelta = action.length > 6 ? action[6] * 30f : 0f;
        float pitchDelta = action.length > 7 ? action[7] * 30f : 0f;
        boolean attack = action.length > 8 && action[8] > 0.5f;

        // Movement
        if (forward) entity.moveForward(0.1f);
        if (backward) entity.moveForward(-0.1f);
        if (left) entity.moveSideways(-0.1f);
        if (right) entity.moveSideways(0.1f);
        
        if (sprint && entity.isOnGround()) {
            entity.setSprinting(true);
        }

        if (jump && entity.isOnGround()) {
            entity.jump();
        }

        // Look
        if (yawDelta != 0 || pitchDelta != 0) {
            entity.setYaw(entity.getYaw() + yawDelta);
            entity.setPitch(clampPitch(entity.getPitch() + pitchDelta));
        }

        // Attack
        if (attack && entity.getAttackCooldownProgress(0f) >= 0.9f) {
            // Would need to trigger attack on server
        }
    }

    private Vec3d getAgentVelocity() {
        return Vec3d.ZERO;
    }

    private int getMovementState(LivingEntity entity) {
        if (!entity.isOnGround()) return 2;
        if (entity.isSprinting()) return 1;
        if (entity.isSneaking()) return 3;
        if (entity.getVelocity().horizontalLength() > 0.01) return 4;
        return 0;
    }

    private int getWeaponState(LivingEntity entity) {
        return 0;
    }

    private boolean canCritical(LivingEntity entity) {
        return entity.fallDistance > 0 && !entity.isOnGround() && entity.getVelocity().y < 0;
    }

    private float normalizeAngle(float angle) {
        while (angle <= -180f) angle += 360f;
        while (angle > 180f) angle -= 360f;
        return angle;
    }

    private float clampPitch(float pitch) {
        return Math.max(-90f, Math.min(90f, pitch));
    }

    private net.minecraft.server.MinecraftServer getServer() {
        try {
            return (net.minecraft.server.MinecraftServer) carpetServerClass.getMethod("getServer").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static class FakePlayerWrapper {
        final LivingEntity entity;
        int lastAttackTick = 0;

        FakePlayerWrapper(LivingEntity entity) {
            this.entity = entity;
        }

        void discard() {
            entity.discard();
        }
    }
}