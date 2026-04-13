package com.infdimmod.world.collider;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public final class DrunnyColliderLayout {
    public static final int GRID_SPACING_CHUNKS = 40;
    public static final int COMPLEX_RADIUS_BLOCKS = 52;

    private DrunnyColliderLayout() {
    }

    public static List<ChunkPos> findNearbyCoreChunks(ChunkPos centerChunk, long worldSeed, int searchRangeChunks) {
        int gridRadius = Math.max(1, searchRangeChunks / GRID_SPACING_CHUNKS + 2);
        int centerGridX = Math.floorDiv(centerChunk.x, GRID_SPACING_CHUNKS);
        int centerGridZ = Math.floorDiv(centerChunk.z, GRID_SPACING_CHUNKS);

        List<ChunkPos> cores = new ArrayList<>();
        for (int gridX = centerGridX - gridRadius; gridX <= centerGridX + gridRadius; gridX++) {
            for (int gridZ = centerGridZ - gridRadius; gridZ <= centerGridZ + gridRadius; gridZ++) {
                ChunkPos core = coreChunkForGrid(gridX, gridZ, worldSeed);
                if (Math.abs(core.x - centerChunk.x) <= searchRangeChunks && Math.abs(core.z - centerChunk.z) <= searchRangeChunks) {
                    cores.add(core);
                }
            }
        }
        return cores;
    }

    public static ChunkPos coreChunkForGrid(int gridX, int gridZ, long worldSeed) {
        int baseX = gridX * GRID_SPACING_CHUNKS;
        int baseZ = gridZ * GRID_SPACING_CHUNKS;

        int offsetX = (int) Math.round(hash2d(gridX, gridZ, worldSeed, 0x6A09_E667L) * 8.0);
        int offsetZ = (int) Math.round(hash2d(gridX, gridZ, worldSeed, 0xBB67_AE85L) * 8.0);

        int chunkX = baseX + GRID_SPACING_CHUNKS / 2 + offsetX;
        int chunkZ = baseZ + GRID_SPACING_CHUNKS / 2 + offsetZ;
        return new ChunkPos(chunkX, chunkZ);
    }

    public static BlockPos coreBlockPos(ChunkPos coreChunk, int coreY) {
        return new BlockPos(coreChunk.getStartX() + 8, coreY, coreChunk.getStartZ() + 8);
    }

    private static double hash2d(int x, int z, long seed, long salt) {
        long mixed = seed ^ salt;
        mixed ^= (long) x * 0x9E37_79B9_7F4A_7C15L;
        mixed ^= (long) z * 0xC2B2_AE3D_27D4_EB4FL;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D0_49BB_1331_11EBL;
        mixed = mixed ^ (mixed >>> 31);
        double normalized = (mixed >>> 11) * 0x1.0p-53;
        return normalized * 2.0 - 1.0;
    }
}
