package com.example.rlpvp.rl.minecraft;

import com.example.rlpvp.rl.observation.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class MinecraftIntegration {

    private ClientPlayerEntity player;
    private LivingEntity target;
    private float prevHealth = 20f;
    private float prevTargetHealth = 20f;
    private int hitCooldown = 0;
    private int lastAttackTick = -100;
    private boolean lastSwing = false;
    private float accumulatedYawError = 0f;
    private float accumulatedPitchError = 0f;
    private int aimSamples = 0;

    public void setPlayer(ClientPlayerEntity player) {
        this.player = player;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
        if (target != null) {
            this.prevTargetHealth = target.getHealth() + target.getAbsorptionAmount();
        }
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void clearTarget() {
        this.target = null;
    }

    public float[] collectObservations() {
        if (player == null || target == null || !target.isAlive()) {
            return new float[ObservationEncoder.getObsDim()];
        }

        SelfObservation self = new SelfObservation();
        TargetObservation targetObs = new TargetObservation();
        CombatObservation combat = new CombatObservation();
        AimObservation aim = new AimObservation();
        MovementObservation movement = new MovementObservation();

        fillSelfObservation(self);
        fillTargetObservation(targetObs);
        fillCombatObservation(combat);
        fillAimObservation(aim);
        fillMovementObservation(movement);

        self.normalize();
        targetObs.normalize();
        combat.normalize();
        aim.normalize();
        movement.normalize();

        return ObservationEncoder.encode(self, targetObs, combat, aim, movement);
    }

    private void fillSelfObservation(SelfObservation obs) {
        obs.health = player.getHealth();
        obs.absorption = player.getAbsorptionAmount();
        
        Vec3d pos = player.getPos();
        obs.posX = (float) pos.x;
        obs.posY = (float) pos.y;
        obs.posZ = (float) pos.z;

        Vec3d vel = player.getVelocity();
        obs.velX = (float) vel.x;
        obs.velY = (float) vel.y;
        obs.velZ = (float) vel.z;
        obs.horVel = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        obs.verVel = (float) vel.y;

        obs.yaw = player.getYaw();
        obs.pitch = player.getPitch();
        obs.onGround = player.isOnGround() ? 1f : 0f;
        obs.sprinting = player.isSprinting() ? 1f : 0f;
        obs.sneaking = player.isSneaking() ? 1f : 0f;

        obs.attackCooldown = player.getAttackCooldownProgress(0f) * 20f;
        obs.hurtTime = player.hurtTime;
        
        obs.movementState = getMovementState();
        obs.weaponState = getWeaponState();
    }

    private void fillTargetObservation(TargetObservation obs) {
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = target.getPos();

        obs.relPosX = (float) (targetPos.x - playerPos.x);
        obs.relPosY = (float) (targetPos.y - playerPos.y);
        obs.relPosZ = (float) (targetPos.z - playerPos.z);

        obs.distance = (float) playerPos.distanceTo(targetPos);
        obs.horDistance = (float) Math.sqrt(
            (targetPos.x - playerPos.x) * (targetPos.x - playerPos.x) +
            (targetPos.z - playerPos.z) * (targetPos.z - playerPos.z)
        );
        obs.verDiff = (float) (targetPos.y - playerPos.y);

        Vec3d targetVel = target.getVelocity();
        obs.velX = (float) targetVel.x;
        obs.velY = (float) targetVel.y;
        obs.velZ = (float) targetVel.z;
        obs.horVel = (float) Math.sqrt(targetVel.x * targetVel.x + targetVel.z * targetVel.z);
        obs.verVel = (float) targetVel.y;

        obs.health = target.getHealth() + target.getAbsorptionAmount();
        obs.yaw = target.getYaw();
        obs.pitch = target.getPitch();
        obs.onGround = target.isOnGround() ? 1f : 0f;
        obs.sprinting = target.isSprinting() ? 1f : 0f;
        obs.attackingState = isTargetAttacking() ? 1f : 0f;
        obs.hurtTime = target.hurtTime;
    }

    private void fillCombatObservation(CombatObservation obs) {
        obs.attackCooldown = player.getAttackCooldownProgress(0f) * 20f;
        obs.attackRange = getAttackRange();

        float currentHealth = player.getHealth() + player.getAbsorptionAmount();
        float currentTargetHealth = target.getHealth() + target.getAbsorptionAmount();

        obs.recentDamageDealt = Math.max(0f, prevTargetHealth - currentTargetHealth);
        obs.recentDamageReceived = Math.max(0f, prevHealth - currentHealth);

        obs.timeSinceLastHit = hitCooldown;
        obs.timeSinceReceivedLastHit = player.hurtTime > 0 ? 0f : hitCooldown;

        if (obs.recentDamageDealt > 0.5f) {
            hitCooldown = 0;
        } else {
            hitCooldown++;
        }

        prevHealth = currentHealth;
        prevTargetHealth = currentTargetHealth;

        obs.comboLength = getComboCount();
        obs.recentWhiff = wasLastAttackWhiff() ? 1f : 0f;
        obs.targetVulnerability = getTargetVulnerability();
        obs.combatSpacing = obs.distance;
        obs.canCritical = canCriticalHit() ? 1f : 0f;
        obs.blockState = target.isBlocking() ? 1f : 0f;
    }

    private void fillAimObservation(AimObservation obs) {
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d targetEyePos = target.getEyePos();

        double dx = targetEyePos.x - playerPos.x;
        double dy = targetEyePos.y - playerPos.y;
        double dz = targetEyePos.z - playerPos.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));

        float yawDiff = normalizeAngle(targetYaw - player.getYaw());
        float pitchDiff = targetPitch - player.getPitch();

        obs.yawError = yawDiff;
        obs.pitchError = pitchDiff;
        obs.relativeTargetAngle = yawDiff;
        obs.targetAngularVel = getTargetAngularVelocity();
        
        accumulatedYawError += Math.abs(yawDiff);
        accumulatedPitchError += Math.abs(pitchDiff);
        aimSamples++;
        obs.recentYawError = aimSamples > 0 ? accumulatedYawError / aimSamples : 0f;
        obs.recentPitchError = aimSamples > 0 ? accumulatedPitchError / aimSamples : 0f;

        obs.inAttackCone = (Math.abs(yawDiff) < 15f && Math.abs(pitchDiff) < 15f) ? 1f : 0f;
        obs.aimSmoothness = getAimSmoothness();
    }

    private void fillMovementObservation(MovementObservation obs) {
        Vec3d playerVel = player.getVelocity();
        Vec3d targetVel = target.getVelocity();

        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        double playerSpeed = Math.sqrt(playerVel.x * playerVel.x + playerVel.z * playerVel.z);
        double targetSpeed = Math.sqrt(targetVel.x * targetVel.x + targetVel.z * targetVel.z);

        double closingSpeed = 0;
        if (horizontalDist > 0) {
            closingSpeed = (dx * (playerVel.x - targetVel.x) + dz * (playerVel.z - targetVel.z)) / horizontalDist;
        }

        obs.closingDistance = (float) closingSpeed;
        obs.relativeMovementDirection = (float) Math.toDegrees(Math.atan2(
            targetVel.z - playerVel.z, targetVel.x - playerVel.x
        ));
        obs.strafeRelation = getStrafeRelation();
        obs.velocityDifference = (float) (playerSpeed - targetSpeed);
        obs.predictedTargetMovement = predictTargetMovement();
        obs.spacingTrend = (float) closingSpeed;
        obs.movementEfficiency = getMovementEfficiency();
        obs.sprintEfficiency = player.isSprinting() && playerSpeed > 0.1 ? 1f : 0f;
    }

    private int getMovementState() {
        if (!player.isOnGround()) return 2; // airborne
        if (player.isSprinting()) return 1; // sprinting
        if (player.isSneaking()) return 3; // sneaking
        if (player.getVelocity().horizontalLength() > 0.01) return 4; // walking
        return 0; // idle
    }

    private int getWeaponState() {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof SwordItem) {
            if (mainHand.getItem() == Items.DIAMOND_SWORD) return 4;
            if (mainHand.getItem() == Items.NETHERITE_SWORD) return 5;
            if (mainHand.getItem() == Items.IRON_SWORD) return 3;
            if (mainHand.getItem() == Items.STONE_SWORD) return 2;
            return 1; // other sword
        }
        if (mainHand.getItem() == Items.SHIELD) return -1; // blocking
        return 0; // no weapon
    }

    private float getAttackRange() {
        ItemStack mainHand = player.getMainHandStack();
        float baseRange = 3.0f;
        if (mainHand.getItem() instanceof SwordItem) {
            baseRange = 3.5f;
        }
        return baseRange + player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_RANGE);
    }

    private boolean isTargetAttacking() {
        return target instanceof PlayerEntity && ((PlayerEntity) target).isAttacking();
    }

    private int getComboCount() {
        return 0;
    }

    private boolean wasLastAttackWhiff() {
        return false;
    }

    private float getTargetVulnerability() {
        float vuln = 0f;
        if (!target.isOnGround()) vuln += 0.3f;
        if (target.hurtTime > 0) vuln += 0.5f;
        if (target.isSprinting()) vuln -= 0.1f;
        if (target.getHealth() < 6f) vuln += 0.2f;
        return Math.max(0f, Math.min(1f, vuln));
    }

    private boolean canCriticalHit() {
        return player.fallDistance > 0.0f && !player.isOnGround() && 
               player.getVelocity().y < 0 && !player.isSprinting() && !player.isSneaking();
    }

    private float getTargetAngularVelocity() {
        return 0f;
    }

    private float getAimSmoothness() {
        return 1f;
    }

    private float getStrafeRelation() {
        Vec3d playerVel = player.getVelocity();
        Vec3d targetVel = target.getVelocity();
        
        double playerDir = Math.atan2(playerVel.z, playerVel.x);
        double targetDir = Math.atan2(targetVel.z, targetVel.x);
        double diff = Math.abs(normalizeAngleRad(playerDir - targetDir));
        
        return (float) (1.0 - diff / Math.PI);
    }

    private float predictTargetMovement() {
        return 0f;
    }

    private float getMovementEfficiency() {
        Vec3d vel = player.getVelocity();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed < 0.01) return 0f;
        
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = target.getPos();
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        if (dist < 0.1) return 1f;
        
        double idealDir = Math.atan2(dz, dx);
        double actualDir = Math.atan2(vel.z, vel.x);
        double alignment = Math.cos(idealDir - actualDir);
        
        return (float) Math.max(0, alignment);
    }

    private float normalizeAngle(float angle) {
        while (angle <= -180f) angle += 360f;
        while (angle > 180f) angle -= 360f;
        return angle;
    }

    private double normalizeAngleRad(double angle) {
        while (angle <= -Math.PI) angle += 2 * Math.PI;
        while (angle > Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    public void reset() {
        prevHealth = 20f;
        prevTargetHealth = 20f;
        hitCooldown = 0;
        lastAttackTick = -100;
        lastSwing = false;
        accumulatedYawError = 0f;
        accumulatedPitchError = 0f;
        aimSamples = 0;
    }

    public void onPlayerAttack() {
        lastAttackTick = (int) (System.currentTimeMillis() / 50);
        lastSwing = true;
    }

    public void onTargetDamaged(float damage) {
    }
}