package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FatOmayGadnostEntity extends IronGolemEntity {
    private static final int PROVOKED_DURATION = 20 * 45;
    private static final int ROAR_INTERVAL = 70;

    private long provokedUntil = 0L;
    private int roarCooldown = ROAR_INTERVAL;

    public FatOmayGadnostEntity(EntityType<? extends IronGolemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (damaged && !this.getWorld().isClient && source.getAttacker() instanceof PlayerEntity player) {
            this.provokedUntil = this.getWorld().getTime() + PROVOKED_DURATION;
            this.setTarget(player);
        }
        return damaged;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }

        long now = this.getWorld().getTime();
        if (now > provokedUntil && this.getTarget() instanceof PlayerEntity) {
            this.setTarget(null);
        }

        if (now <= provokedUntil && --roarCooldown <= 0) {
            performRoar();
            roarCooldown = ROAR_INTERVAL;
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof PlayerEntity && this.getWorld() != null && this.getWorld().getTime() > this.provokedUntil) {
            return;
        }
        super.setTarget(target);
    }

    private void performRoar() {
        List<HostileEntity> hostiles = this.getWorld().getEntitiesByClass(
                HostileEntity.class,
                Box.of(this.getPos(), 14.0, 6.0, 14.0),
                LivingEntity::isAlive
        );
        for (HostileEntity hostile : hostiles) {
            hostile.takeKnockback(1.1f, this.getX() - hostile.getX(), this.getZ() - hostile.getZ());
            hostile.damage(this.getDamageSources().mobAttack(this), 8.0f);
        }
        this.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 0.85f);
    }
}
