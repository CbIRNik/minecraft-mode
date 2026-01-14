package com.infdimmod.items.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

public class PortalFluidItem extends Item {
    public PortalFluidItem(Settings settings) {
        super(settings);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }
        ItemStack otherHand = user.getMainHandStack();
        if (hand == Hand.MAIN_HAND) {
            otherHand = user.getOffHandStack();
        }
        if (otherHand.isEmpty()) {
            return TypedActionResult.pass(stack);
        }
        if (("portal_gun".equals(Registries.ITEM.getId(otherHand.getItem()).getPath()))&&(otherHand.getDamage()>0)) {
            otherHand.setDamage(0);
            if (!user.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return TypedActionResult.pass(stack);
    }
}
