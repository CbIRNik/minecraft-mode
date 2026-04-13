package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class DrunGuardEntity extends ZombieEntity {
    private static final int GUARD_RADIUS = 18;

    private BlockPos guardPost;

    public DrunGuardEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }

        if (guardPost == null) {
            guardPost = this.getBlockPos();
        }

        if (this.age % 20 == 0) {
            pullTargetToGuardZone();
        }

        if (this.getTarget() == null && guardPost != null && this.squaredDistanceTo(guardPost.toCenterPos()) > GUARD_RADIUS * GUARD_RADIUS) {
            this.getNavigation().startMovingTo(guardPost.getX() + 0.5, guardPost.getY(), guardPost.getZ() + 0.5, 1.15);
        }
    }

    private void pullTargetToGuardZone() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || guardPost == null) {
            return;
        }

        Box guardArea = Box.of(guardPost.toCenterPos(), GUARD_RADIUS * 2.0, 10.0, GUARD_RADIUS * 2.0);
        List<PlayerEntity> intruders = serverWorld.getEntitiesByClass(
                PlayerEntity.class,
                guardArea,
                player -> player.isAlive() && !player.isSpectator()
        );

        if (intruders.isEmpty()) {
            return;
        }

        PlayerEntity nearest = intruders.stream()
                .min(java.util.Comparator.comparingDouble(player -> player.squaredDistanceTo(this)))
                .orElse(null);

        if (nearest != null) {
            this.setTarget(nearest);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (guardPost != null) {
            nbt.putLong("GuardPost", guardPost.asLong());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("GuardPost")) {
            guardPost = BlockPos.fromLong(nbt.getLong("GuardPost"));
        }
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    public void setGuardPost(BlockPos guardPost) {
        this.guardPost = guardPost;
    }

    public BlockPos getGuardPost() {
        return guardPost;
    }
}
