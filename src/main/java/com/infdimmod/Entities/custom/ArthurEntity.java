package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArthurEntity extends VillagerEntity {
    private static final double GREETING_RANGE = 8.0;
    private static final int GREETING_TIMEOUT_TICKS = 80;
    private static final int GREETING_GRACE_TICKS = 20 * 45;

    private final Map<UUID, Long> pendingGreetingSince = new HashMap<>();
    private final Map<UUID, Long> greetedUntil = new HashMap<>();

    public ArthurEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }

        long now = this.getWorld().getTime();
        List<ServerPlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                ServerPlayerEntity.class,
                Box.of(this.getPos(), GREETING_RANGE * 2.0, 4.0, GREETING_RANGE * 2.0),
                player -> player.isAlive() && !player.isSpectator() && player.squaredDistanceTo(this) <= GREETING_RANGE * GREETING_RANGE
        );

        pendingGreetingSince.keySet().removeIf(uuid -> nearbyPlayers.stream().noneMatch(player -> player.getUuid().equals(uuid)));
        greetedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (ServerPlayerEntity player : nearbyPlayers) {
            UUID uuid = player.getUuid();
            long graceUntil = greetedUntil.getOrDefault(uuid, 0L);
            if (graceUntil > now) {
                pendingGreetingSince.remove(uuid);
                continue;
            }

            long seenSince = pendingGreetingSince.computeIfAbsent(uuid, ignored -> now);
            if (now - seenSince >= GREETING_TIMEOUT_TICKS) {
                oneShot(player);
                pendingGreetingSince.put(uuid, now + 20);
            }
        }
    }

    private void oneShot(ServerPlayerEntity player) {
        DamageSource source = this.getDamageSources().mobAttack(this);
        player.damage(source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.damage(source, 1000.0f);
        }
        this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
    }

    public void onGreeting(ServerPlayerEntity player) {
        long now = this.getWorld().getTime();
        pendingGreetingSince.remove(player.getUuid());
        greetedUntil.put(player.getUuid(), now + GREETING_GRACE_TICKS);
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
    }

    public static boolean greetNearest(ServerPlayerEntity player) {
        World world = player.getWorld();
        List<ArthurEntity> nearby = world.getEntitiesByClass(
                ArthurEntity.class,
                player.getBoundingBox().expand(GREETING_RANGE),
                entity -> entity.isAlive()
        );

        if (nearby.isEmpty()) {
            return false;
        }

        ArthurEntity nearest = nearby.stream()
                .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)))
                .orElse(null);

        if (nearest == null || nearest.squaredDistanceTo(player) > GREETING_RANGE * GREETING_RANGE) {
            return false;
        }

        nearest.onGreeting(player);
        return true;
    }
}
