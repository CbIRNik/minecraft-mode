package com.tungtung.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MysticPortalFrameBlock extends Block {
    public MysticPortalFrameBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack stack = player.getMainHandStack();
        
        if (stack.isOf(Items.FLINT_AND_STEEL)) {
            if (!world.isClient) {
                if (MysticPortalBlock.tryCreatePortal(world, pos)) {
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    stack.damage(1, player, p -> p.sendToolBreakStatus(player.getActiveHand()));
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.success(world.isClient);
        }
        
        return ActionResult.PASS;
    }
}
