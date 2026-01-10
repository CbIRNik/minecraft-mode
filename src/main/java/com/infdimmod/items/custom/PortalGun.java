package com.infdimmod.items.custom;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Blocks.custom.Gunportal;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Map;

public class PortalGun extends Item {
    public PortalGun(Settings settings) {
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
        if (!world.isClient() && player != null) {
            if (isItemBroken(stack)) {
                return ActionResult.FAIL;
            }
            if (player.getItemCooldownManager().isCoolingDown(this)) {
                return ActionResult.PASS;
            }
            BlockPos posAbove = clickedPos.up();
            if (state.isReplaceable()) {
                posAbove = clickedPos;
            }

            if (canPlaceBlock(world, posAbove) && canPlaceBlock(world, posAbove.up())) {
                int newDamage = stack.getDamage();
                if (!player.isCreative()) {
                    newDamage = stack.getDamage() + 1;
                }

                if (newDamage >= stack.getMaxDamage()) {
                    stack.setDamage(stack.getMaxDamage());
                } else {
                    stack.setDamage(newDamage);
                }

                Direction playerFacing = player.getHorizontalFacing();
                Gunportal.PortalDirection portalDir;
                if (playerFacing == Direction.NORTH || playerFacing == Direction.SOUTH) {
                    portalDir = Gunportal.PortalDirection.NORTH_SOUTH;
                } else {
                    portalDir = Gunportal.PortalDirection.EAST_WEST;
                }

                world.setBlockState(posAbove, ModBlocks.GUNPORTAL_BOTTOM.getDefaultState()
                        .with(Gunportal.PORTAL_DIRECTION, portalDir));
                world.setBlockState(posAbove.up(), ModBlocks.GUNPORTAL_TOP.getDefaultState()
                        .with(Gunportal.PORTAL_DIRECTION, portalDir));

                // Сохраняем в PersistentState
                savePlacedBlocks((ServerWorld) world, posAbove, posAbove.up());

                player.getItemCooldownManager().set(this, 10);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    private void savePlacedBlocks(ServerWorld world, BlockPos... positions) {
        PortalGunData data = PortalGunData.get(world.getServer());
        long removalTime = world.getTime() + 80;

        for (BlockPos pos : positions) {
            data.addBlock(world, pos.toImmutable(), removalTime);
        }
    }

    public static void checkAndRemoveBlocks(MinecraftServer server) {
        PortalGunData data = PortalGunData.get(server);
        Map<String, Map<BlockPos, Long>> placedBlocksByDimension = data.getPlacedBlocksByDimension();

        Iterator<Map.Entry<String, Map<BlockPos, Long>>> dimensionIterator = placedBlocksByDimension.entrySet().iterator();

        while (dimensionIterator.hasNext()) {
            Map.Entry<String, Map<BlockPos, Long>> dimensionEntry = dimensionIterator.next();
            String dimensionId = dimensionEntry.getKey();
            Map<BlockPos, Long> blocks = dimensionEntry.getValue();

            // Получаем мир по ID измерения
            ServerWorld world = null;
            for (ServerWorld serverWorld : server.getWorlds()) {
                if (serverWorld.getRegistryKey().getValue().toString().equals(dimensionId)) {
                    world = serverWorld;
                    break;
                }
            }

            if (world == null) {
                // Если мир не найден, удаляем все блоки для этого измерения
                dimensionIterator.remove();
                data.markDirty();
                continue;
            }

            Iterator<Map.Entry<BlockPos, Long>> blockIterator = blocks.entrySet().iterator();
            boolean dimensionChanged = false;

            while (blockIterator.hasNext()) {
                Map.Entry<BlockPos, Long> blockEntry = blockIterator.next();
                BlockPos pos = blockEntry.getKey();
                Long removalTime = blockEntry.getValue();

                if (world.getTime() >= removalTime) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    blockIterator.remove();
                    dimensionChanged = true;
                }
            }

            if (dimensionChanged) {
                if (blocks.isEmpty()) {
                    dimensionIterator.remove();
                }
                data.markDirty();
            }
        }
    }

    public boolean isItemBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    public static float getBrokenState(ItemStack stack) {
        if (stack.getDamage() >= 420) {
            return 1.0f;
        } else if (stack.getDamage() >= 280) {
            return 0.7f;
        } else if (stack.getDamage() >= 140) {
            return 0.8f;
        } else {
            return 0.0f;
        }
    }

    private boolean canPlaceBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return (state.isAir() ||
                state.isReplaceable()) &&
                !state.isFullCube(world, pos);
    }
}