package com.infdimmod.items.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortalGun extends Item {
    public PortalGun(Settings settings){
        super(settings.maxDamage(420));
    }
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        ItemStack stack = player.getStackInHand(hand);
        BlockState state = world.getBlockState(clickedPos);
        // Проверяем, что действие происходит на сервере
        if (!world.isClient() && player != null) {
            if (isItemBroken(stack)) {
                return ActionResult.FAIL;
            }
            // Проверяем перезарядку
            if (player.getItemCooldownManager().isCoolingDown(this)) {
                return ActionResult.PASS;
            }
            // Получаем позицию над кликнутым блоком
            BlockPos posAbove = clickedPos.up();
            if (state.isReplaceable()){posAbove = clickedPos;}
            // Проверяем, можно ли разместить блоки
            if (canPlaceBlock(world, posAbove) && canPlaceBlock(world, posAbove.up())) {
                // Наносим урон предмету если игрок не в креативе
                int newDamage = stack.getDamage();
                if (!player.isCreative()){
                newDamage = stack.getDamage() + 1;}
                if (newDamage >= stack.getMaxDamage()) {
                    // Предмет полностью сломался
                    stack.setDamage(stack.getMaxDamage());
                } else {
                    // Устанавливаем новое значение повреждения
                    stack.setDamage(newDamage);
                }
                // Ставим блоки
                world.setBlockState(posAbove, Blocks.STONE.getDefaultState());
                world.setBlockState(posAbove.up(), Blocks.STONE.getDefaultState());
                // Устанавливаем перезарядку на 0.5 секунды
                player.getItemCooldownManager().set(this, 10);

                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }
    // Метод для проверки, сломан ли предмет
    public boolean isItemBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }
    // Метод для определения состояния предмета
    public static float getBrokenState(ItemStack stack) {
        if (stack.getDamage() >= 420) {
            return 1.0f;
        }
        else if (stack.getDamage() >= 280 && stack.getDamage() < 420) {
            return 0.7f;
        }
        else if (stack.getDamage() >= 140 && stack.getDamage() < 280) {
            return 0.8f;
        }
        else {
            return 0.0f;
        }
    }
    private boolean canPlaceBlock(World world, BlockPos pos) {
        // Проверяем, что блок можно заменить
        BlockState state = world.getBlockState(pos);
        return state.isAir() ||
                state.isReplaceable() ||
                !state.isFullCube(world, pos);
    }
}

