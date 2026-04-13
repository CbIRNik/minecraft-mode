package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StudentEntity extends VillagerEntity {
    private static final VillagerProfession[] STUDENT_PROFESSIONS = new VillagerProfession[]{
            VillagerProfession.LIBRARIAN,
            VillagerProfession.ARMORER,
            VillagerProfession.CLERIC,
            VillagerProfession.FARMER,
            VillagerProfession.CARTOGRAPHER
    };

    public StudentEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        VillagerProfession profession = STUDENT_PROFESSIONS[this.random.nextInt(STUDENT_PROFESSIONS.length)];
        VillagerData villagerData = this.getVillagerData().withProfession(profession).withLevel(2);
        this.setVillagerData(villagerData);
        return data;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.getWorld().isClient || this.age % 40 != 0) {
            return;
        }

        VillagerProfession profession = this.getVillagerData().getProfession();
        if (profession == VillagerProfession.LIBRARIAN) {
            librarianSupport();
        } else if (profession == VillagerProfession.ARMORER) {
            armorerShieldPulse();
        } else if (profession == VillagerProfession.CLERIC) {
            clericHealPulse();
        } else if (profession == VillagerProfession.FARMER) {
            farmerEnergyPulse();
        } else if (profession == VillagerProfession.CARTOGRAPHER) {
            cartographerScanPulse();
        }
    }

    private void librarianSupport() {
        PlayerEntity player = this.getWorld().getClosestPlayer(this, 6.0);
        if (player == null || player.isSpectator()) {
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 200, 0, true, true, true));
    }

    private void armorerShieldPulse() {
        boolean dangerNearby = !this.getWorld().getEntitiesByClass(HostileEntity.class,
                Box.of(this.getPos(), 10.0, 6.0, 10.0),
                entity -> entity.isAlive()).isEmpty();
        if (!dangerNearby) {
            return;
        }

        List<LivingEntity> allies = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                Box.of(this.getPos(), 9.0, 5.0, 9.0),
                entity -> entity.isAlive() && (entity instanceof VillagerEntity || entity instanceof LittleTastyBabyEntity)
        );
        for (LivingEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 140, 0, true, true, true));
        }
    }

    private void clericHealPulse() {
        List<LivingEntity> allies = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                Box.of(this.getPos(), 8.0, 4.0, 8.0),
                entity -> entity.isAlive() && (entity instanceof VillagerEntity || entity instanceof LittleTastyBabyEntity)
        );

        for (LivingEntity ally : allies) {
            if (ally.getHealth() >= ally.getMaxHealth()) {
                continue;
            }
            ally.heal(2.0f);
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, true, true, true));
            break;
        }
    }

    private void farmerEnergyPulse() {
        List<PlayerEntity> players = this.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                Box.of(this.getPos(), 8.0, 4.0, 8.0),
                player -> player.isAlive() && !player.isSpectator()
        );
        for (PlayerEntity player : players) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1, 0, true, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 120, 0, true, true, true));
        }
    }

    private void cartographerScanPulse() {
        List<HostileEntity> hostiles = this.getWorld().getEntitiesByClass(
                HostileEntity.class,
                Box.of(this.getPos(), 14.0, 6.0, 14.0),
                entity -> entity.isAlive()
        );
        for (HostileEntity hostile : hostiles) {
            hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 120, 0, true, true, true));
        }
        if (!hostiles.isEmpty()) {
            this.playSound(SoundEvents.BLOCK_BELL_USE, 0.6f, 1.2f);
        }
    }
}
