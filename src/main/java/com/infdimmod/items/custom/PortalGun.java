package com.infdimmod.items.custom;

import com.infdimmod.items.ModItems;
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

                // Наносим урон предмету
                int newDamage = 0;
                if (!player.isCreative()){
                newDamage = stack.getDamage() + 200;}


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


                // Устанавливаем перезарядку на 0.75 секунды
                player.getItemCooldownManager().set(this, 15);

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        // Можно чинить жидкостью
        return ingredient.isOf(ModItems.PortalFluid);
    }

    // Метод для проверки, сломан ли предмет
    public boolean isItemBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false; // Как элитры, нельзя зачаровывать
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    private boolean canPlaceBlock(World world, BlockPos pos) {
        // Проверяем, что блок можно заменить
        BlockState state = world.getBlockState(pos);
        return state.isAir() ||
                state.isReplaceable() ||
                !state.isFullCube(world, pos);
    }
}

