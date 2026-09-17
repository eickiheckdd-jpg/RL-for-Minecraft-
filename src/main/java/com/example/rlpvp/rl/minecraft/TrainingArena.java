package com.example.rlpvp.rl.minecraft;

import com.example.rlpvp.config.RLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Random;

public class TrainingArena {

    private static final float ARENA_RADIUS = 15f;
    private static final float MIN_SPAWN_DIST = 4f;
    private static final float MAX_SPAWN_DIST = 12f;
    private static final int MAX_EPISODE_TICKS = 1200;

    private final MinecraftClient client;
    private final Random random = new Random();
    
    private Vec3d arenaCenter = Vec3d.ZERO;
    private LivingEntity trainingTarget = null;
    private int episodeTicks = 0;
    private boolean episodeActive = false;

    public TrainingArena() {
        this.client = MinecraftClient.getInstance();
    }

    public void initialize(Vec3d center) {
        this.arenaCenter = center;
    }

    public void startEpisode() {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        episodeActive = true;
        episodeTicks = 0;

        Vec3d playerSpawn = getRandomSpawnPos();
        teleportPlayer(player, playerSpawn);

        resetPlayerState(player);
        givePlayerLoadout(player);

        spawnTarget(playerSpawn);
        
        if (trainingTarget != null) {
            Vec3d targetSpawn = getTargetSpawnPos(playerSpawn);
            teleportEntity(trainingTarget, targetSpawn);
            resetTargetState(trainingTarget);
        }
    }

    public void endEpisode() {
        episodeActive = false;
        removeTarget();
    }

    public void tick() {
        if (!episodeActive) return;
        
        episodeTicks++;
        
        ClientPlayerEntity player = client.player;
        if (player == null || trainingTarget == null || !trainingTarget.isAlive()) {
            endEpisode();
            return;
        }

        if (episodeTicks >= MAX_EPISODE_TICKS) {
            endEpisode();
            return;
        }

        if (player.getHealth() <= 0 || trainingTarget.getHealth() <= 0) {
            endEpisode();
            return;
        }

        if (!isInArena(player.getPos()) || !isInArena(trainingTarget.getPos())) {
            endEpisode();
            return;
        }
    }

    private Vec3d getRandomSpawnPos() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = MIN_SPAWN_DIST + random.nextDouble() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
        double x = arenaCenter.x + radius * Math.cos(angle);
        double z = arenaCenter.z + radius * Math.sin(angle);
        double y = findSafeY((int) x, (int) z);
        return new Vec3d(x, y, z);
    }

    private Vec3d getTargetSpawnPos(Vec3d playerPos) {
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = MIN_SPAWN_DIST + random.nextDouble() * (MAX_SPAWN_DIST - MIN_SPAWN_DIST);
            double x = playerPos.x + radius * Math.cos(angle);
            double z = playerPos.z + radius * Math.sin(angle);
            double y = findSafeY((int) x, (int) z);
            
            Vec3d candidate = new Vec3d(x, y, z);
            if (candidate.distanceTo(playerPos) >= MIN_SPAWN_DIST) {
                return candidate;
            }
        }
        return getRandomSpawnPos();
    }

    private double findSafeY(int x, int z) {
        if (client.world == null) return 64;
        for (int y = 255; y >= 0; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (client.world.getBlockState(pos).isAir() && 
                client.world.getBlockState(pos.up()).isAir() &&
                !client.world.getBlockState(pos.down()).isAir()) {
                return y + 1;
            }
        }
        return 64;
    }

    private void teleportPlayer(ClientPlayerEntity player, Vec3d pos) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendTeleportPacket(pos.x, pos.y, pos.z, 
                player.getYaw(), player.getPitch(), new net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket(0));
        }
        player.teleport(pos.x, pos.y, pos.z, true);
    }

    private void teleportEntity(LivingEntity entity, Vec3d pos) {
        entity.teleport(pos.x, pos.y, pos.z, true);
    }

    private void resetPlayerState(ClientPlayerEntity player) {
        player.setHealth(20f);
        player.setAbsorptionAmount(0f);
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(20f);
        player.clearStatusEffects();
        player.extinguish();
        player.fallDistance = 0f;
        player.velocity = Vec3d.ZERO;
    }

    private void givePlayerLoadout(ClientPlayerEntity player) {
        player.getInventory().clear();
        
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.getInventory().setStack(0, sword);
        
        ItemStack shield = new ItemStack(Items.SHIELD);
        player.getInventory().setStack(1, shield);
        
        for (int i = 2; i < 9; i++) {
            player.getInventory().setStack(i, ItemStack.EMPTY);
        }
        
        player.getInventory().armor.set(0, new ItemStack(Items.DIAMOND_BOOTS));
        player.getInventory().armor.set(1, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.getInventory().armor.set(2, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.getInventory().armor.set(3, new ItemStack(Items.DIAMOND_HELMET));
        
        player.getInventory().selectedSlot = 0;
    }

    private void spawnTarget(Vec3d playerPos) {
        if (client.world == null) return;
        
        if (client.getServer() != null) {
            ServerWorld serverWorld = client.getServer().getOverworld();
            if (serverWorld != null) {
                ZombieEntity zombie = EntityType.ZOMBIE.create(serverWorld);
                if (zombie != null) {
                    zombie.setCustomName(Text.literal("Training Target"));
                    zombie.setCustomNameVisible(true);
                    zombie.setCanPickUpLoot(false);
                    zombie.setPersistent();
                    zombie.getAttributes().getBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).ifPresent(v -> zombie.setHealth(20f));
                    zombie.getAttributes().getBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).ifPresent(v -> zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.23f));
                    zombie.getAttributes().getBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).ifPresent(v -> zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0));
                    
                    trainingTarget = zombie;
                    
                    Vec3d targetPos = getTargetSpawnPos(playerPos);
                    teleportEntity(zombie, targetPos);
                    serverWorld.spawnEntity(zombie);
                }
            }
        } else {
            ZombieEntity zombie = EntityType.ZOMBIE.create(client.world);
            if (zombie != null) {
                zombie.setCustomName(Text.literal("Training Target"));
                zombie.setCustomNameVisible(true);
                zombie.setCanPickUpLoot(false);
                zombie.setPersistent();
                zombie.getAttributes().getBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).ifPresent(v -> zombie.setHealth(20f));
                zombie.getAttributes().getBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).ifPresent(v -> zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.23f));
                zombie.getAttributes().getBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).ifPresent(v -> zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(3.0));
                
                trainingTarget = zombie;
                Vec3d targetPos = getTargetSpawnPos(playerPos);
                teleportEntity(zombie, targetPos);
                client.world.spawnEntity(zombie);
            }
        }
    }

    private void resetTargetState(LivingEntity target) {
        target.setHealth(20f);
        target.setAbsorptionAmount(0f);
        target.clearStatusEffects();
        target.extinguish();
        target.fallDistance = 0f;
        target.velocity = Vec3d.ZERO;
    }

    private void removeTarget() {
        if (trainingTarget != null) {
            trainingTarget.discard();
            trainingTarget = null;
        }
    }

    private boolean isInArena(Vec3d pos) {
        return pos.distanceTo(arenaCenter) <= ARENA_RADIUS + 5;
    }

    public Vec3d getArenaCenter() {
        return arenaCenter;
    }

    public LivingEntity getTrainingTarget() {
        return trainingTarget;
    }

    public boolean isEpisodeActive() {
        return episodeActive;
    }

    public int getEpisodeTicks() {
        return episodeTicks;
    }

    public int getMaxEpisodeTicks() {
        return MAX_EPISODE_TICKS;
    }

    public void setArenaCenter(Vec3d center) {
        this.arenaCenter = center;
    }
}