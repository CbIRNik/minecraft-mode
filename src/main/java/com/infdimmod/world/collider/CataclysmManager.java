package com.infdimmod.world.collider;

import com.infdimmod.burmaldeniya.BurmaldeniyaConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public class CataclysmManager {
    public static final int MAX_RADIUS = BurmaldeniyaConfig.Collider.MELTDOWN_RADIUS_BLOCKS;
    private static final int BLOCKS_PER_TICK = BurmaldeniyaConfig.Collider.MELTDOWN_BLOCKS_PER_TICK;

    private static final Map<CataclysmKey, ActiveCataclysm> activeCataclysms = new HashMap<>();
    private static final Queue<ActiveCataclysm> pendingCataclysms = new ArrayDeque<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CataclysmManager::tick);
    }

    public static void startCataclysm(ServerWorld world, BlockPos center) {
        ActiveCataclysm cataclysm = new ActiveCataclysm(world, center.toImmutable());
        CataclysmKey key = cataclysm.key();
        if (activeCataclysms.containsKey(key) || pendingCataclysms.stream().anyMatch(existing -> existing.key().equals(key))) {
            return;
        }
        pendingCataclysms.add(cataclysm);
    }

    private static void tick(MinecraftServer server) {
        while (!pendingCataclysms.isEmpty()) {
            ActiveCataclysm cataclysm = pendingCataclysms.remove();
            activeCataclysms.putIfAbsent(cataclysm.key(), cataclysm);
        }

        if (activeCataclysms.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<CataclysmKey, ActiveCataclysm>> iterator = activeCataclysms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CataclysmKey, ActiveCataclysm> entry = iterator.next();
            if (entry.getValue().tick()) {
                iterator.remove();
            }
        }
    }

    private static class ActiveCataclysm {
        private final ServerWorld world;
        private final BlockPos center;
        private int currentRadius;
        private int currentY;
        private int currentX;
        private int currentZ;

        public ActiveCataclysm(ServerWorld world, BlockPos center) {
            this.world = world;
            this.center = center;
            this.currentRadius = 1;
            this.currentY = -currentRadius;
            this.currentX = -currentRadius;
            this.currentZ = -currentRadius;
        }

        /**
         * Returns true if finished.
         */
        public boolean tick() {
            int blocksProcessed = 0;

            while (blocksProcessed < BLOCKS_PER_TICK && currentRadius <= MAX_RADIUS) {
                BlockPos pos = center.add(currentX, currentY, currentZ);
                if (isWithinBuildHeight(pos) && isChunkLoaded(pos) && pos.getSquaredDistance(center) <= currentRadius * currentRadius) {
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                        blocksProcessed++;
                    }
                }

                // Advance coordinates
                currentZ++;
                if (currentZ > currentRadius) {
                    currentZ = -currentRadius;
                    currentX++;
                    if (currentX > currentRadius) {
                        currentX = -currentRadius;
                        currentY++;
                        if (currentY > currentRadius) {
                            currentY = -(currentRadius + 1);
                            currentX = -(currentRadius + 1);
                            currentZ = -(currentRadius + 1);
                            currentRadius++;
                        }
                    }
                }
            }

            return currentRadius > MAX_RADIUS;
        }

        private boolean isWithinBuildHeight(BlockPos pos) {
            return pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY();
        }

        private boolean isChunkLoaded(BlockPos pos) {
            return world.isChunkLoaded(new ChunkPos(pos));
        }

        private CataclysmKey key() {
            return new CataclysmKey(world.getRegistryKey().getValue().toString(), center.asLong());
        }
    }

    private record CataclysmKey(String worldId, long centerPos) {
        private CataclysmKey {
            Objects.requireNonNull(worldId, "worldId");
        }
    }
}
