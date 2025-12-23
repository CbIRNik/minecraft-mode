package com.infdimmod.items.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortalGun extends Item {
    public PortalGun(Settings settings){
        super(settings);
    }
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();





        // Проверяем, что действие происходит на сервере
        if (!world.isClient() && player != null) {
            // Получаем позицию над кликнутым блоком
            BlockPos posAbove = clickedPos.up();

            // Проверяем, можно ли разместить блоки
            if (canPlaceBlock(world, posAbove) && canPlaceBlock(world, posAbove.up())) {

                // Ставим первый блок камня
                world.setBlockState(posAbove, Blocks.STONE.getDefaultState());

                // Ставим второй блок камня сверху
                world.setBlockState(posAbove.up(), Blocks.STONE.getDefaultState());

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    private boolean canPlaceBlock(World world, BlockPos pos) {
        // Проверяем, что блок можно заменить
        BlockState state = world.getBlockState(pos);
        return state.isAir() ||
                state.isReplaceable() ||
                !state.isFullCube(world, pos);
    }
}

