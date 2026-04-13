package com.infdimmod.Entities.custom;

import com.infdimmod.items.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class LittleTastyBabyEntity extends ChickenEntity {
    public LittleTastyBabyEntity(EntityType<? extends ChickenEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(2, new TemptGoal(this, 1.1, Ingredient.ofItems(ModItems.Sausage), false));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.05));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(9, new LookAroundGoal(this));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient || this.age % 60 != 0) {
            return;
        }

        PlayerEntity player = this.getWorld().getClosestPlayer(this, 4.0);
        if (player == null || player.isSpectator()) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, true, true, true));
        this.playSound(SoundEvents.ENTITY_CHICKEN_AMBIENT, 0.8f, 1.3f);
    }
}
