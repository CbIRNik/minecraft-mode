package com.infdimmod.Blocks.custom;

import com.infdimmod.world.collider.DrunnyColliderSystemManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DrunnyAtomBlock extends Block {
    public DrunnyAtomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            DrunnyColliderSystemManager.forceMeltdown(serverWorld, pos);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient && world.isReceivingRedstonePower(pos) && world instanceof ServerWorld serverWorld) {
            DrunnyColliderSystemManager.forceMeltdown(serverWorld, pos);
        }
    }
}
