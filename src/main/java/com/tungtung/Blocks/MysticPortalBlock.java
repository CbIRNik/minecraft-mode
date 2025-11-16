package com.tungtung.Blocks;

import com.tungtung.world.ModDimensions;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class MysticPortalBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

    public MysticPortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(100) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.5f,
                    random.nextFloat() * 0.4f + 0.8f, false);
        }

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            world.addParticle(ParticleTypes.PORTAL, x, y, z,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5);
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && !entity.hasVehicle() && !entity.hasPassengers() && entity.canUsePortals(false)) {
            if (world instanceof ServerWorld serverWorld) {
                RegistryKey<World> destination = serverWorld.getRegistryKey() == ModDimensions.MYSTIC_WORLD_KEY
                        ? World.OVERWORLD
                        : ModDimensions.MYSTIC_WORLD_KEY;

                ServerWorld destinationWorld = serverWorld.getServer().getWorld(destination);
                if (destinationWorld != null) {
                    FabricDimensions.teleport(entity, destinationWorld, 
                        new TeleportTarget(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), 
                        entity.getVelocity(), entity.getYaw(), entity.getPitch()));
                }
            }
        }
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                     WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!isValidPortal(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return state;
    }

    private boolean isValidPortal(WorldAccess world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = world.getBlockState(pos.offset(direction));
            if (neighbor.isOf(TungBlocks.MYSTIC_PORTAL_FRAME) || neighbor.isOf(this)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryCreatePortal(World world, BlockPos pos) {
        // Проверяем вертикальный портал (3x3)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos checkPos = pos.add(dx, dy, 0);
                if (isValidPortalFrame(world, checkPos, dx, dy)) {
                    createPortal(world, checkPos);
                    return true;
                }
                
                checkPos = pos.add(0, dy, dx);
                if (isValidPortalFrame(world, checkPos, dx, dy)) {
                    createPortalZ(world, checkPos);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidPortalFrame(World world, BlockPos corner, int dx, int dy) {
        // Проверяем рамку 3x3
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                BlockPos checkPos = corner.add(x, y, 0);
                boolean isEdge = x == 0 || x == 2 || y == 0 || y == 2;
                boolean isCorner = (x == 0 || x == 2) && (y == 0 || y == 2);
                
                if (isEdge) {
                    if (!world.getBlockState(checkPos).isOf(TungBlocks.MYSTIC_PORTAL_FRAME)) {
                        return false;
                    }
                } else if (!world.getBlockState(checkPos).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void createPortal(World world, BlockPos corner) {
        for (int x = 1; x < 2; x++) {
            for (int y = 1; y < 2; y++) {
                world.setBlockState(corner.add(x, y, 0), TungBlocks.MYSTIC_PORTAL.getDefaultState());
            }
        }
    }

    private static void createPortalZ(World world, BlockPos corner) {
        for (int z = 1; z < 2; z++) {
            for (int y = 1; y < 2; y++) {
                world.setBlockState(corner.add(0, y, z), TungBlocks.MYSTIC_PORTAL.getDefaultState());
            }
        }
    }
}
