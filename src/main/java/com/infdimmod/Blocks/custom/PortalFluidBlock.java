package com.infdimmod.Blocks.custom;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class PortalFluidBlock extends FlowableFluid {

    @Override
    public Fluid getFlowing() {
        return ModBlocks.FLOWING_CUSTOM_FLUID;
    }

    @Override
    public Fluid getStill() {
        return ModBlocks.STILL_CUSTOM_FLUID;
    }

    @Override
    protected boolean isInfinite(World world) {
        return false;
    }

    @Override
    protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        // выпадение предметов вроде редстоуна при смывании их жидкостью
        Block.dropStacks(state, (World) world, pos);
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        // Разрешаем замену, если текущая жидкость находится выше или если это та же жидкость
        return direction == Direction.DOWN && !fluid.matchesType(this);
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return 1;
    }

    @Override
    public int getTickRate(WorldView world) {
        return 5;
    }

    @Override
    protected float getBlastResistance() {
        return 100.0F;
    }

    @Override
    protected int getMaxFlowDistance(WorldView world) {
        return 4;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        // level 0 = полный блок, level 8 = пустота
        int level = Math.max(0, 8 - state.getLevel() * 8 / 8);
        return ModBlocks.CUSTOM_FLUID_BLOCK.getDefaultState()
                .with(FluidBlock.LEVEL, level);
    }

    @Override
    public boolean matchesType(Fluid fluid) {
        return fluid == getStill() || fluid == getFlowing();
    }

    @Override
    public Item getBucketItem() {
        return ModItems.PortalFluidBucket;
    }

    @Override
    protected boolean hasRandomTicks() {
        return true;
    }



    // Исправленный метод для получения уровня жидкости
    public static int getBlockStateLevel(FluidState state) {
        if (state.isStill()) {
            return 8;
        } else {
            return state.get(LEVEL);
        }
    }

    public static class Still extends PortalFluidBlock {

        @Override
        public int getLevel(FluidState state) {
            return 8;
        }

        @Override
        public boolean isStill(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends PortalFluidBlock {
        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }

        @Override
        public boolean isStill(FluidState state) {
            return false;
        }
    }
}