package com.infdimmod.world.collider;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CataclysmManager {
    public static final int MAX_RADIUS = 100;
    private static final int BLOCKS_PER_TICK = 4000;

    private static final List<ActiveCataclysm> activeCataclysms = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CataclysmManager::tick);
    }

    public static void startCataclysm(ServerWorld world, BlockPos center) {
        activeCataclysms.add(new ActiveCataclysm(world, center));
    }

    private static void tick(MinecraftServer server) {
        if (activeCataclysms.isEmpty()) return;

        List<ActiveCataclysm> finished = new ArrayList<>();
        for (ActiveCataclysm cataclysm : activeCataclysms) {
            if (cataclysm.tick()) {
                finished.add(cataclysm);
            }
        }
        activeCataclysms.removeAll(finished);
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
                
                // Only destroy blocks roughly within the spherical shell or sphere volume
                if (pos.getSquaredDistance(center) <= currentRadius * currentRadius) {
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir()) {
                        // Even bedrock is destroyed
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
    }
}
