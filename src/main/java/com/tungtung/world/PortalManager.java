package com.tungtung.world;

import com.tungtung.Blocks.TungBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class PortalManager extends PersistentState {
    private final Map<BlockPos, RegistryKey<World>> portalLinks = new HashMap<>();
    
    public PortalManager() {
    }

    public void linkPortal(BlockPos pos, RegistryKey<World> destination) {
        portalLinks.put(pos, destination);
        markDirty();
    }

    public RegistryKey<World> getDestination(BlockPos pos) {
        return portalLinks.get(pos);
    }

    public static PortalManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            new Type<>(PortalManager::new, PortalManager::fromNbt, null),
            "portal_links"
        );
    }

    public static PortalManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        return new PortalManager();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        return nbt;
    }

    public static BlockPos findSafeSpawnLocation(ServerWorld world, BlockPos center) {
        BlockPos.Mutable mutable = center.mutableCopy();
        
        for (int y = world.getTopY() - 1; y > world.getBottomY(); y--) {
            mutable.setY(y);
            if (world.getBlockState(mutable).isAir() && 
                world.getBlockState(mutable.up()).isAir() &&
                !world.getBlockState(mutable.down()).isAir()) {
                return mutable.toImmutable();
            }
        }
        
        return center.withY(65);
    }

    public static void createReturnPortal(ServerWorld world, BlockPos pos) {
        BlockPos corner = pos.add(-2, -1, 0);
        
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                BlockPos framePos = corner.add(x, y, 0);
                boolean isEdge = x == 0 || x == 4 || y == 0 || y == 4;
                
                if (isEdge) {
                    world.setBlockState(framePos, TungBlocks.MYSTIC_PORTAL_FRAME.getDefaultState());
                } else {
                    world.setBlockState(framePos, TungBlocks.MYSTIC_PORTAL.getDefaultState());
                }
            }
        }
        
        for (int x = -3; x <= 5; x++) {
            world.setBlockState(corner.add(x, -2, 0), Blocks.STONE.getDefaultState());
        }
    }
}
