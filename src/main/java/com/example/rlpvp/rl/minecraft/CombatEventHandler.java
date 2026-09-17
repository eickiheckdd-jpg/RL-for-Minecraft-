package com.example.rlpvp.rl.minecraft;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class CombatEventHandler {

    private final MinecraftClient client;
    
    private float lastPlayerHealth = 20f;
    private float lastTargetHealth = 20f;
    private LivingEntity currentTarget = null;
    
    private int comboCount = 0;
    private int lastHitTick = -100;
    private boolean lastAttackHit = false;
    private boolean lastAttackWasCritical = false;
    private float lastDamageDealt = 0f;
    private float lastDamageTaken = 0f;
    
    private int consecutiveHits = 0;
    private int consecutiveWhiffs = 0;
    private int lastAttackTime = 0;

    public CombatEventHandler() {
        this.client = MinecraftClient.getInstance();
    }

    public void tick() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        updateHealthTracking(player);
        updateTargetTracking();
        updateComboTracking();
    }

    private void updateHealthTracking(ClientPlayerEntity player) {
        float currentPlayerHealth = player.getHealth() + player.getAbsorptionAmount();
        float currentTargetHealth = currentTarget != null ? 
            currentTarget.getHealth() + currentTarget.getAbsorptionAmount() : 20f;

        lastDamageTaken = Math.max(0f, lastPlayerHealth - currentPlayerHealth);
        lastDamageDealt = Math.max(0f, lastTargetHealth - currentTargetHealth);

        lastPlayerHealth = currentPlayerHealth;
        lastTargetHealth = currentTargetHealth;
    }

    private void updateTargetTracking() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        var hitResult = player.raycast(6.0, 0.0f, false);
        if (hitResult != null && hitResult.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
            net.minecraft.entity.Entity entity = ((net.minecraft.util.hit.EntityHitResult) hitResult).getEntity();
            if (entity instanceof LivingEntity && entity != player && entity.isAlive()) {
                currentTarget = (LivingEntity) entity;
            }
        }
    }

    private void updateComboTracking() {
        if (lastAttackHit) {
            consecutiveHits++;
            consecutiveWhiffs = 0;
        } else if (lastDamageDealt == 0f && lastAttackTime > 0) {
            consecutiveWhiffs++;
        }
    }

    public void onPlayerAttack() {
        lastAttackTime = (int) (System.currentTimeMillis() / 50);
        
        if (currentTarget != null) {
            float targetHealthBefore = currentTarget.getHealth() + currentTarget.getAbsorptionAmount();
        }
    }

    public void onAttackHit(LivingEntity target, float damage, boolean critical) {
        lastAttackHit = true;
        lastAttackWasCritical = critical;
        lastHitTick = (int) (System.currentTimeMillis() / 50);
        comboCount++;
        
        if (target == currentTarget) {
            lastDamageDealt = damage;
        }
    }

    public void onAttackMiss() {
        lastAttackHit = false;
        lastAttackWasCritical = false;
        comboCount = 0;
        consecutiveWhiffs++;
    }

    public void onPlayerDamaged(DamageSource source, float damage) {
        lastDamageTaken = damage;
    }

    public void onTargetDeath() {
        if (currentTarget != null && !currentTarget.isAlive()) {
            currentTarget = null;
            comboCount = 0;
        }
    }

    public void reset() {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            lastPlayerHealth = player.getHealth() + player.getAbsorptionAmount();
        }
        lastTargetHealth = 20f;
        currentTarget = null;
        comboCount = 0;
        lastHitTick = -100;
        lastAttackHit = false;
        lastAttackWasCritical = false;
        lastDamageDealt = 0f;
        lastDamageTaken = 0f;
        consecutiveHits = 0;
        consecutiveWhiffs = 0;
        lastAttackTime = 0;
    }

    public float getLastDamageDealt() {
        return lastDamageDealt;
    }

    public float getLastDamageTaken() {
        return lastDamageTaken;
    }

    public int getComboCount() {
        return comboCount;
    }

    public boolean wasLastAttackCritical() {
        return lastAttackWasCritical;
    }

    public boolean wasLastAttackHit() {
        return lastAttackHit;
    }

    public int getTimeSinceLastHit() {
        return (int) (System.currentTimeMillis() / 50) - lastHitTick;
    }

    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(LivingEntity target) {
        this.currentTarget = target;
        if (target != null) {
            lastTargetHealth = target.getHealth() + target.getAbsorptionAmount();
        }
    }

    public boolean isTargetValid() {
        return currentTarget != null && currentTarget.isAlive() && 
               client.player != null &&
               client.player.distanceTo(currentTarget) < 6.0;
    }

    public float getDistanceToTarget() {
        if (currentTarget == null || client.player == null) return 100f;
        return (float) client.player.distanceTo(currentTarget);
    }

    public int getConsecutiveHits() {
        return consecutiveHits;
    }

    public int getConsecutiveWhiffs() {
        return consecutiveWhiffs;
    }
}