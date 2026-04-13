package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FogiApexEntity extends ZombieEntity {
    private static final int LEAP_INTERVAL = 30;
    private static final int DARKNESS_INTERVAL = 60;

    private int leapCooldown = LEAP_INTERVAL;
    private int darknessCooldown = DARKNESS_INTERVAL;

    public FogiApexEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 12;
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, false, false, entity -> entity != this));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (--leapCooldown <= 0) {
            tryLeap(target);
            leapCooldown = LEAP_INTERVAL;
        }

        if (--darknessCooldown <= 0 && this.squaredDistanceTo(target) <= 49.0) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, true, true, true));
            darknessCooldown = DARKNESS_INTERVAL;
        }
    }

    private void tryLeap(LivingEntity target) {
        double horizontalSq = this.squaredDistanceTo(target.getX(), this.getY(), target.getZ());
        if (horizontalSq < 16.0 || horizontalSq > 196.0) {
            return;
        }

        Vec3d delta = target.getPos().subtract(this.getPos());
        Vec3d horizontal = new Vec3d(delta.x, 0.0, delta.z);
        if (horizontal.lengthSquared() < 1.0E-4) {
            return;
        }

        Vec3d impulse = horizontal.normalize().multiply(0.75).add(0.0, 0.35, 0.0);
        this.addVelocity(impulse.x, impulse.y, impulse.z);
        this.velocityModified = true;
    }
}
