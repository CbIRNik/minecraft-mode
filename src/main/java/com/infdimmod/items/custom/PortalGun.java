package com.infdimmod.items.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class PortalGun extends Item {
    // Храним информацию о поставленных блоках: мир -> список позиций с временем
    private static final Map<RegistryKey<World>, Map<BlockPos, Long>> placedBlocks = new HashMap<>();

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
                // Сохраняем информацию о поставленных блоках
                savePlacedBlocks(world, posAbove, posAbove.up());
                // Устанавливаем перезарядку на 0.5 секунды
                player.getItemCooldownManager().set(this, 10);
                // Запускаем проверку удаления блоков (асинхронно через планировщик)
                scheduleBlockRemoval(world);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }
    // Метод для сохранения информации о поставленных блоках
    private void savePlacedBlocks(World world, BlockPos... positions) {
        RegistryKey<World> worldKey = world.getRegistryKey();
        long removalTime = world.getTime() + 80; // 4 секунды = 80 тиков
        // Получаем или создаем мапу для этого мира
        Map<BlockPos, Long> worldBlocks = placedBlocks.getOrDefault(worldKey, new HashMap<>());
        // Добавляем блоки с временем удаления
        for (BlockPos pos : positions) {
            worldBlocks.put(pos.toImmutable(), removalTime);
        }
        placedBlocks.put(worldKey, worldBlocks);
    }
    // Метод для планирования удаления блоков
    private void scheduleBlockRemoval(World world) {
        if (world instanceof ServerWorld serverWorld) {
            // Используем планировщик мира для проверки через 4 секунды
            serverWorld.getServer().execute(() -> {
                checkAndRemoveBlocks(serverWorld.getServer());
            });
        }
    }
    // Метод для проверки и удаления блоков (должен вызываться на сервере)
    public static void checkAndRemoveBlocks(MinecraftServer server) {
        for (Map.Entry<RegistryKey<World>, Map<BlockPos, Long>> worldEntry : placedBlocks.entrySet()) {
            RegistryKey<World> worldKey = worldEntry.getKey();
            Map<BlockPos, Long> blocks = worldEntry.getValue();
            // Получаем мир по ключу
            ServerWorld world = server.getWorld(worldKey);
            if (world == null) continue;
            // Проверяем каждый блок в этом мире
            Map<BlockPos, Long> blocksToKeep = new HashMap<>();
            for (Map.Entry<BlockPos, Long> blockEntry : blocks.entrySet()) {
                BlockPos pos = blockEntry.getKey();
                Long removalTime = blockEntry.getValue();
                // Если время удаления наступило
                if (world.getTime() >= removalTime) {
                    // Удаляем блок, если он камень (чтобы не удалить случайно поставленные игроком блоки)
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.STONE)) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                } else {
                    // Сохраняем блок для будущей проверки
                    blocksToKeep.put(pos, removalTime);
                }
            }
            // Обновляем список блоков для этого мира
            if (blocksToKeep.isEmpty()) {
                placedBlocks.remove(worldKey);
            } else {
                placedBlocks.put(worldKey, blocksToKeep);
            }
        }
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

