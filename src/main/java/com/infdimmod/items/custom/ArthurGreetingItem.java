package com.infdimmod.items.custom;

import com.infdimmod.Entities.custom.ArthurEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class ArthurGreetingItem extends Item {
    public ArthurGreetingItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            boolean greeted = ArthurEntity.greetNearest(serverPlayer);
            if (!greeted) {
                serverPlayer.sendMessage(Text.translatable("itemTooltip.infdimmod.arthur_greeting"), true);
            }
            ActionResult result = greeted ? ActionResult.SUCCESS : ActionResult.PASS;
            return new TypedActionResult<>(result, stack);
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("itemTooltip.infdimmod.arthur_greeting"));
    }
}
