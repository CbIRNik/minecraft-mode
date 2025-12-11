package com.infdimmod.Blocks;

import com.infdimmod.world.ModDimensions;
import com.infdimmod.world.PortalManager;
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
                PortalManager manager = PortalManager.get(serverWorld);
                RegistryKey<World> destination = manager.getDestination(pos);
                
                if (destination == null) {
                    destination = serverWorld.getRegistryKey() == ModDimensions.MYSTIC_WORLD_KEY
                            ? World.OVERWORLD
                            : ModDimensions.MYSTIC_WORLD_KEY;
                }

                ServerWorld destinationWorld = serverWorld.getServer().getWorld(destination);
                if (destinationWorld != null) {
                    BlockPos targetPos = PortalManager.findSafeSpawnLocation(destinationWorld, 
                        new BlockPos((int)entity.getX(), 65, (int)entity.getZ()));
                    
                    if (!hasNearbyPortal(destinationWorld, targetPos, 10)) {
                        PortalManager.createReturnPortal(destinationWorld, targetPos);
                        BlockPos returnPortalCenter = targetPos.add(0, 1, 0);
                        PortalManager.get(destinationWorld).linkPortal(returnPortalCenter, serverWorld.getRegistryKey());
                    }
                    
                    Vec3d teleportPos = new Vec3d(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
                    TeleportTarget target = new TeleportTarget(destinationWorld, teleportPos, Vec3d.ZERO, entity.getYaw(), entity.getPitch(), TeleportTarget.NO_OP);
                    entity.teleportTo(target);
                }
            }
        }
    }

    private static boolean hasNearbyPortal(ServerWorld world, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            if (world.getBlockState(pos).isOf(ModBlocks.MYSTIC_PORTAL)) {
                return true;
            }
        }
        return false;
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
            if (neighbor.isOf(ModBlocks.MYSTIC_PORTAL_FRAME) || neighbor.isOf(this)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryCreatePortal(World world, BlockPos pos) {
        java.util.Set<BlockPos> airBlocks = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
        queue.add(pos);
        airBlocks.add(pos);
        
        Direction.Axis axis = null;
        
        while (!queue.isEmpty() && airBlocks.size() < 200) {
            BlockPos current = queue.poll();
            
            for (Direction dir : Direction.values()) {
                if (axis == null) {
                    if (dir.getAxis() == Direction.Axis.X || dir.getAxis() == Direction.Axis.Z) {
                        BlockPos neighbor = current.offset(dir);
                        if (world.getBlockState(neighbor).isOf(ModBlocks.MYSTIC_PORTAL_FRAME)) {
                            axis = dir.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
                        }
                    }
                }
                
                if (axis != null && dir.getAxis() != axis) {
                    BlockPos neighbor = current.offset(dir);
                    if (world.getBlockState(neighbor).isAir() && !airBlocks.contains(neighbor)) {
                        airBlocks.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        if (axis != null && airBlocks.size() >= 1 && isValidFrame(world, airBlocks, axis)) {
            for (BlockPos airPos : airBlocks) {
                world.setBlockState(airPos, ModBlocks.MYSTIC_PORTAL.getDefaultState());
            }
            return true;
        }
        
        return false;
    }
    
    private static boolean isValidFrame(World world, java.util.Set<BlockPos> airBlocks, Direction.Axis axis) {
        for (BlockPos airPos : airBlocks) {
            boolean hasFrame = false;
            for (Direction dir : Direction.values()) {
                if (dir.getAxis() != axis) {
                    if (world.getBlockState(airPos.offset(dir)).isOf(ModBlocks.MYSTIC_PORTAL_FRAME)) {
                        hasFrame = true;
                        break;
                    }
                }
            }
            if (!hasFrame) return false;
        }
        return true;
    }
}
