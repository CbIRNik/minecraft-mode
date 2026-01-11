package com.infdimmod.items.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

public class PortalFluid extends Item {
    public PortalFluid(Settings settings) {
        super(settings);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // Текущий стек, которым игрок пытается воспользоваться
        ItemStack stack = user.getStackInHand(hand);
        // Требуется именно левая рука (offhand): действие срабатывает при клике правой кнопкой,
        // держа portal_fluid в offhand. Если это не offhand, ничего не делаем.
        if (hand != Hand.OFF_HAND) {
            return TypedActionResult.pass(stack);
        }
        // Изменения предметов выполняем только на сервере. Клиенту возвращаем pass,
        // чтобы сервер позже отправил актуальное состояние предметов.
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }
        // Берём предмет из правой руки — ожидаем portal_gun
        ItemStack mainHand = user.getMainHandStack();
        if (mainHand.isEmpty()) {
            // Правой руки нет -> ничего восстанавливать
            return TypedActionResult.pass(stack);
        }
        // Получаем идентификатор предмета в правой руке для проверки типа предмета
        Identifier id = Registries.ITEM.getId(mainHand.getItem());
        // Если идентификатор не найден или путь не совпадает с portal_gun -> пропускаем
        if (id == null || !"portal_gun".equals(id.getPath())) {
            return TypedActionResult.pass(stack);
        }
        // Если portal_gun действительно повреждён, восстанавливаем его прочность
        if (mainHand.isDamaged()) {
            mainHand.setDamage(0); // Сброс повреждения -> максимальная прочность

            // В творчестве предметы не тратятся; проверяем через abilities.creativeMode
            if (!user.getAbilities().creativeMode) {
                stack.decrement(1); // уменьшаем количество portal_fluid в offhand на 1
            }
            if (!stack.isEmpty()) {
                stack.setDamage(0);
            }
        }
        // Если нет повреждений — нечего делать
        return TypedActionResult.pass(stack);
    }
}