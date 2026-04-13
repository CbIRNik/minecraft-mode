package com.infdimmod.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

public final class PortalTeleportSafety {
    private static final int MAX_HORIZONTAL_SEARCH_RADIUS = 6;
    private static final int[] Y_OFFSETS = new int[]{0, 1, -1, 2, -2, 3, -3, 4};

    private PortalTeleportSafety() {
    }

    public static Vec3d resolveSafeTarget(ServerWorld world, Vec3d desired) {
        BlockPos basePos = BlockPos.ofFloored(desired);

        for (int radius = 0; radius <= MAX_HORIZONTAL_SEARCH_RADIUS; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }
                    for (int yOffset : Y_OFFSETS) {
                        BlockPos candidate = basePos.add(xOffset, yOffset, zOffset);
                        if (isSafeStandPosition(world, candidate)) {
                            return centerOf(candidate);
                        }
                    }
                }
            }
        }

        for (int radius = 0; radius <= MAX_HORIZONTAL_SEARCH_RADIUS + 2; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    int x = basePos.getX() + xOffset;
                    int z = basePos.getZ() + zOffset;
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (isSafeStandPosition(world, candidate)) {
                        return centerOf(candidate);
                    }
                }
            }
        }

        BlockPos spawnPos = world.getSpawnPos();
        BlockPos fallback = new BlockPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        if (isSafeStandPosition(world, fallback)) {
            return centerOf(fallback);
        }

        int fallbackTopY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawnPos.getX(), spawnPos.getZ()) + 1;
        return new Vec3d(spawnPos.getX() + 0.5, fallbackTopY, spawnPos.getZ() + 0.5);
    }

    private static boolean isSafeStandPosition(ServerWorld world, BlockPos feetPos) {
        int bottom = world.getBottomY() + 1;
        int ceiling = world.getTopY() - 2;
        if (feetPos.getY() < bottom || feetPos.getY() > ceiling) {
            return false;
        }

        BlockPos headPos = feetPos.up();
        BlockPos belowPos = feetPos.down();

        BlockState feetState = world.getBlockState(feetPos);
        BlockState headState = world.getBlockState(headPos);
        BlockState belowState = world.getBlockState(belowPos);

        if (!isPassable(world, feetPos, feetState) || !isPassable(world, headPos, headState)) {
            return false;
        }

        if (belowState.isAir() || belowState.getCollisionShape(world, belowPos).isEmpty()) {
            return false;
        }

        return !isHazard(feetState) && !isHazard(headState) && !isHazard(belowState);
    }

    private static boolean isPassable(ServerWorld world, BlockPos pos, BlockState state) {
        return state.getCollisionShape(world, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static boolean isHazard(BlockState state) {
        return state.getFluidState().isIn(FluidTags.LAVA)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isIn(BlockTags.FIRE);
    }

    private static Vec3d centerOf(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
}
