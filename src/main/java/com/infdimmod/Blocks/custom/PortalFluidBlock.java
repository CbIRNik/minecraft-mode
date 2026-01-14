package com.infdimmod.Blocks.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortalFluidBlock extends FluidBlock {
    public PortalFluidBlock(FlowableFluid fluid, Settings settings){
        super(fluid, settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity livingEntity) {
            // Накладываем эффект отравления при касании
            livingEntity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.POISON,
                    40, // 2 секунд
                    9,   // Уровень X
                    false, // не от амбьена
                    false,  // не показывает частицы
                    false   // не показывает иконку
            ));
        }
        super.onEntityCollision(state, world, pos, entity);
    }
}
