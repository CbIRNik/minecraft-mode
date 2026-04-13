package com.infdimmod.world.generator;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.Entities.custom.DrunGuardEntity;
import com.infdimmod.Entities.custom.FogiEntity;
import com.infdimmod.Entities.custom.ResidentEntity;
import com.infdimmod.world.collider.DrunnyColliderLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class DeterministicChaosGenerator extends ChunkGenerator {
    public static final MapCodec<DeterministicChaosGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    Codec.LONG.fieldOf("seed").forGetter(generator -> generator.worldSeed)
            ).apply(instance, instance.stable(DeterministicChaosGenerator::new))
    );

    private static final int COLLIDER_RADIUS_CHUNKS = 5;
    private static final int COLLIDER_HALF_HEIGHT = 22;

    private final long worldSeed;
    private final int surfaceBase;
    private final int lowerLayerCeiling;
    private final int lavaLevel;
    private final int separatorThickness;
    private final int upperCavernDepth;
    private final double terrainFrequencyA;
    private final double terrainFrequencyB;
    private final double caveFrequency;

    public DeterministicChaosGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.worldSeed = seed;
        Random random = new Random(seed ^ 0xC00F_FEEEL);
        this.surfaceBase = 58 + random.nextInt(8);
        this.lowerLayerCeiling = 18 + random.nextInt(10);
        this.lavaLevel = this.lowerLayerCeiling - 10;
        this.separatorThickness = 9 + random.nextInt(4);
        this.upperCavernDepth = 20 + random.nextInt(7);
        this.terrainFrequencyA = 0.012 + random.nextDouble() * 0.006;
        this.terrainFrequencyB = 0.02 + random.nextDouble() * 0.01;
        this.caveFrequency = 0.05 + random.nextDouble() * 0.02;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getStartX() + x;
                int worldZ = chunkPos.getStartZ() + z;

                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                    BlockState blockState = sampleBlockState(worldX, y, worldZ);
                    if (!blockState.isAir()) {
                        chunk.setBlockState(mutable.set(x, y, z), blockState, false);
                    }
                }
            }
        }

        placeColliderComplex(chunk, chunkPos);
        placeDormitoryScaffolding(chunk, chunkPos);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {}
    @Override protected MapCodec<? extends ChunkGenerator> getCodec() { return CODEC; }
    @Override public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}
    @Override public int getWorldHeight() { return 384; }
    @Override public int getSeaLevel() { return 63; }
    @Override public int getMinimumY() { return -64; }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottomY = world.getBottomY();
        int topY = world.getTopY();
        for (int y = topY - 1; y >= bottomY; y--) {
            BlockState state = sampleBlockState(x, y, z);
            if (state != null && heightmap.getBlockPredicate().test(state)) {
                return y + 1;
            }
        }
        return bottomY;
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        ServerWorld world = region.toServerWorld();
        ChunkPos chunkPos = region.getCenterPos();

        if (isDormChunk(chunkPos)) {
            spawnResident(world, chunkPos, 6, 6, 0.0f);
            spawnResident(world, chunkPos, 10, 9, 180.0f);
            spawnFogi(world, chunkPos, 12, 4, 90.0f);
            return;
        }

        if (isColliderCoreChunk(chunkPos)) {
            spawnDrunGuard(world, chunkPos, 8, 8, 0.0f);
            spawnDrunGuard(world, chunkPos, 4, 11, 90.0f);
            spawnDrunGuard(world, chunkPos, 11, 4, 180.0f);
            return;
        }

        double patrolChance = hash2d(chunkPos.x, chunkPos.z, 0xD0A4_5001L);
        if (patrolChance > 0.86) {
            spawnFogi(world, chunkPos, 8, 8, 0.0f);
        }
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottomY = world.getBottomY();
        int topY = world.getTopY();
        BlockState[] states = new BlockState[topY - bottomY];
        for (int y = bottomY; y < topY; y++) {
            states[y - bottomY] = sampleBlockState(x, y, z);
        }
        return new VerticalBlockSample(bottomY, states);
    }

    @Override
    public void getDebugHudText(java.util.List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Burmaldeniya layered cavern generator");
        text.add("Dorm chunk: " + (isDormChunk(new ChunkPos(pos)) ? "yes" : "no"));
        text.add("Collider chunk: " + (isColliderCoreChunk(new ChunkPos(pos)) ? "yes" : "no"));
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public int getColliderCoreY() {
        return Math.max(getMinimumY() + 18, this.lowerLayerCeiling - 2);
    }

    private void placeColliderComplex(Chunk chunk, ChunkPos chunkPos) {
        List<ChunkPos> nearbyCores = DrunnyColliderLayout.findNearbyCoreChunks(chunkPos, worldSeed, COLLIDER_RADIUS_CHUNKS + 2);
        int coreY = getColliderCoreY();

        for (ChunkPos coreChunk : nearbyCores) {
            if (Math.abs(coreChunk.x - chunkPos.x) > COLLIDER_RADIUS_CHUNKS || Math.abs(coreChunk.z - chunkPos.z) > COLLIDER_RADIUS_CHUNKS) {
                continue;
            }

            int coreX = coreChunk.getStartX() + 8;
            int coreZ = coreChunk.getStartZ() + 8;
            int minY = Math.max(chunk.getBottomY(), coreY - COLLIDER_HALF_HEIGHT);
            int maxY = Math.min(chunk.getTopY() - 1, coreY + COLLIDER_HALF_HEIGHT);

            for (int worldX = chunkPos.getStartX(); worldX <= chunkPos.getStartX() + 15; worldX++) {
                int dx = worldX - coreX;
                for (int worldZ = chunkPos.getStartZ(); worldZ <= chunkPos.getStartZ() + 15; worldZ++) {
                    int dz = worldZ - coreZ;
                    double horizontalSq = dx * dx + dz * dz;
                    if (horizontalSq > DrunnyColliderLayout.COMPLEX_RADIUS_BLOCKS * DrunnyColliderLayout.COMPLEX_RADIUS_BLOCKS) {
                        continue;
                    }

                    for (int y = minY; y <= maxY; y++) {
                        int dy = y - coreY;
                        BlockState state = colliderStateAt(dx, dy, dz, horizontalSq);
                        if (state != null) {
                            setBlockInChunk(chunk, worldX, y, worldZ, state);
                        }
                    }
                }
            }
        }
    }

    private BlockState colliderStateAt(int dx, int dy, int dz, double horizontalSq) {
        double shellNorm = horizontalSq / (double) (DrunnyColliderLayout.COMPLEX_RADIUS_BLOCKS * DrunnyColliderLayout.COMPLEX_RADIUS_BLOCKS)
                + (dy * dy) / (double) (COLLIDER_HALF_HEIGHT * COLLIDER_HALF_HEIGHT);
        if (shellNorm > 1.0) {
            return null;
        }

        int radial = (int) Math.round(Math.sqrt(horizontalSq));
        if (radial <= 3 && Math.abs(dy) <= 3) {
            return ModBlocks.DRUNNY_ATOM.getDefaultState();
        }

        if (radial <= 5 && Math.abs(dy) <= 4) {
            return Blocks.CRYING_OBSIDIAN.getDefaultState();
        }

        if (radial <= 11 && Math.abs(dy) <= 7) {
            if (dy <= -3) {
                return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        if ((Math.abs(dx) <= 2 || Math.abs(dz) <= 2) && Math.abs(dy) <= 3) {
            if (dy <= -2) {
                return Blocks.POLISHED_DEEPSLATE.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }

        if (Math.abs(dx) == 9 && dz == 0 && dy == 1) {
            return ModBlocks.DrunnyCollider.getDefaultState();
        }
        if (Math.abs(dz) == 9 && dx == 0 && dy == 1) {
            return ModBlocks.DrunnyCollider.getDefaultState();
        }

        if (Math.floorMod(Math.abs(dx), 12) == 0 && Math.floorMod(Math.abs(dz), 12) == 0 && dy >= -14 && dy <= 9) {
            return Blocks.DEEPSLATE_BRICKS.getDefaultState();
        }

        if (shellNorm >= 0.88) {
            return Blocks.DEEPSLATE_TILES.getDefaultState();
        }

        if (dy <= -15 && radial < 46) {
            return Blocks.MAGMA_BLOCK.getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }

    private void placeDormitoryScaffolding(Chunk chunk, ChunkPos chunkPos) {
        if (!isDormChunk(chunkPos)) {
            return;
        }

        int centerX = chunkPos.getStartX() + 8;
        int centerZ = chunkPos.getStartZ() + 8;
        int floorY = sampleSurfaceHeight(centerX, centerZ) + 1;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int worldX = centerX + dx;
                int worldZ = centerZ + dz;

                setBlockInChunk(chunk, worldX, floorY - 1, worldZ, Blocks.STONE_BRICKS.getDefaultState());
                setBlockInChunk(chunk, worldX, floorY, worldZ, Blocks.OAK_PLANKS.getDefaultState());

                boolean wall = Math.abs(dx) == 4 || Math.abs(dz) == 3;
                for (int dy = 1; dy <= 3; dy++) {
                    setBlockInChunk(chunk, worldX, floorY + dy, worldZ,
                            wall ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.AIR.getDefaultState());
                }

                setBlockInChunk(chunk, worldX, floorY + 4, worldZ, Blocks.DARK_OAK_PLANKS.getDefaultState());
            }
        }

        setBlockInChunk(chunk, centerX, floorY + 1, centerZ + 3, Blocks.AIR.getDefaultState());
        setBlockInChunk(chunk, centerX, floorY + 2, centerZ + 3, Blocks.AIR.getDefaultState());
        setBlockInChunk(chunk, centerX - 3, floorY + 1, centerZ, Blocks.TORCH.getDefaultState());
        setBlockInChunk(chunk, centerX + 3, floorY + 1, centerZ, Blocks.TORCH.getDefaultState());
    }

    private void setBlockInChunk(Chunk chunk, int worldX, int worldY, int worldZ, BlockState state) {
        int localX = worldX - chunk.getPos().getStartX();
        int localZ = worldZ - chunk.getPos().getStartZ();

        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            return;
        }
        if (worldY < chunk.getBottomY() || worldY >= chunk.getTopY()) {
            return;
        }

        chunk.setBlockState(new BlockPos(localX, worldY, localZ), state, false);
    }

    private void spawnResident(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = sampleSurfaceHeight(worldX, worldZ) + 1;

        ResidentEntity resident = new ResidentEntity(ModEntities.RESIDENT_ENTITY_TYPE, world);
        resident.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        resident.setPersistent();
        world.spawnEntity(resident);
    }

    private void spawnFogi(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = sampleSurfaceHeight(worldX, worldZ) + 1;

        FogiEntity fogi = new FogiEntity(ModEntities.FOGI_ENTITY_TYPE, world);
        fogi.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        fogi.setPersistent();
        world.spawnEntity(fogi);
    }

    private void spawnDrunGuard(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = getColliderCoreY();

        DrunGuardEntity guard = new DrunGuardEntity(ModEntities.DRUN_GUARD_ENTITY_TYPE, world);
        guard.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        guard.setPersistent();
        world.spawnEntity(guard);
    }

    private boolean isDormChunk(ChunkPos chunkPos) {
        if (Math.floorMod(chunkPos.x, 12) != 0 || Math.floorMod(chunkPos.z, 12) != 0) {
            return false;
        }
        return hash2d(chunkPos.x, chunkPos.z, 0x0D05_4510L) > -0.25;
    }

    private boolean isColliderCoreChunk(ChunkPos chunkPos) {
        ChunkPos core = DrunnyColliderLayout.coreChunkForGrid(
                Math.floorDiv(chunkPos.x, DrunnyColliderLayout.GRID_SPACING_CHUNKS),
                Math.floorDiv(chunkPos.z, DrunnyColliderLayout.GRID_SPACING_CHUNKS),
                worldSeed
        );
        return core.x == chunkPos.x && core.z == chunkPos.z;
    }

    private BlockState sampleBlockState(int x, int y, int z) {
        if (y <= getMinimumY() + 1) {
            return Blocks.BEDROCK.getDefaultState();
        }

        int upperFloor = sampleSurfaceHeight(x, z);
        int upperCeiling = sampleUpperCeilingHeight(x, z, upperFloor);
        int separatorBottom = lowerLayerCeiling + 1;
        int separatorTop = upperFloor - 1;

        if (y <= lowerLayerCeiling) {
            if (isLowerCave(x, y, z)) {
                return y <= lavaLevel ? Blocks.LAVA.getDefaultState() : Blocks.AIR.getDefaultState();
            }
            return sampleLowerLayerMaterial(x, y, z);
        }

        if (y <= separatorTop) {
            if (isLayerConnector(x, y, z, separatorBottom, separatorTop)) {
                return Blocks.AIR.getDefaultState();
            }
            return sampleSeparatorMaterial(x, y, z);
        }

        if (y > upperCeiling) {
            return Blocks.STONE.getDefaultState();
        }

        if (y == upperFloor) {
            return Blocks.BASALT.getDefaultState();
        }

        if (y >= upperCeiling - 2) {
            return Blocks.STONE.getDefaultState();
        }

        if (y <= upperFloor + 2) {
            return sampleUpperLayerMaterial(x, y, z);
        }

        if (isUpperPillar(x, y, z, upperFloor, upperCeiling)) {
            return sampleUpperLayerMaterial(x, y, z);
        }

        return Blocks.AIR.getDefaultState();
    }

    private BlockState sampleLowerLayerMaterial(int x, int y, int z) {
        double heat = hash3d(x, y, z, 0x5F37_59DFL);
        if (y <= lavaLevel - 2 && heat > 0.45) {
            return Blocks.MAGMA_BLOCK.getDefaultState();
        }
        if (heat > 0.65) {
            return Blocks.BASALT.getDefaultState();
        }
        if (heat < -0.55) {
            return Blocks.BLACKSTONE.getDefaultState();
        }
        if (y >= lowerLayerCeiling - 2 && heat < -0.2) {
            return Blocks.SOUL_SAND.getDefaultState();
        }
        return Blocks.NETHERRACK.getDefaultState();
    }

    private BlockState sampleUpperLayerMaterial(int x, int y, int z) {
        double crust = hash3d(x, y, z, 0xF00D_2401L);
        if (crust > 0.62) {
            return Blocks.BLACKSTONE.getDefaultState();
        }
        if (crust < -0.55) {
            return Blocks.BASALT.getDefaultState();
        }
        return Blocks.STONE.getDefaultState();
    }

    private BlockState sampleSeparatorMaterial(int x, int y, int z) {
        double blend = hash3d(x >> 1, y, z >> 1, 0xD1F1_5EEPL);
        if (blend > 0.3) {
            return Blocks.DEEPSLATE.getDefaultState();
        }
        if (blend < -0.45) {
            return Blocks.BLACKSTONE.getDefaultState();
        }
        return Blocks.STONE.getDefaultState();
    }

    private int sampleSurfaceHeight(int x, int z) {
        double rolling = Math.sin((x + worldSeed * 0.03125) * terrainFrequencyA) * 10.0
                + Math.cos((z - worldSeed * 0.015625) * terrainFrequencyA) * 9.0;
        double ridges = Math.sin((x + z) * terrainFrequencyB) * 4.0
                + Math.cos((x - z) * terrainFrequencyB * 0.8) * 3.0;
        double seeded = hash2d(x >> 2, z >> 2, 0xB529_7A4DL) * 6.0;
        int surface = surfaceBase + (int) Math.round(rolling + ridges + seeded);
        return Math.max(lowerLayerCeiling + separatorThickness + 8, surface);
    }

    private int sampleUpperCeilingHeight(int x, int z, int upperFloor) {
        double rolling = Math.cos((x + worldSeed * 0.0078125) * terrainFrequencyA * 0.75) * 3.5
                + Math.sin((z - worldSeed * 0.00390625) * terrainFrequencyA * 0.75) * 3.0;
        int ceiling = upperFloor + upperCavernDepth + (int) Math.round(rolling);
        return Math.max(upperFloor + 12, ceiling);
    }

    private boolean isUpperPillar(int x, int y, int z, int upperFloor, int upperCeiling) {
        int centerY = (upperFloor + upperCeiling) / 2;
        double verticalBias = 1.0 - (Math.abs(y - centerY) / (double) Math.max(2, (upperCeiling - upperFloor) / 2));
        double mass = hash3d(
                (int) Math.floor(x * caveFrequency * 1.4),
                (int) Math.floor(y * caveFrequency * 1.1),
                (int) Math.floor(z * caveFrequency * 1.4),
                0x41C6_CE57L
        );
        return mass > (0.6 + (1.0 - verticalBias) * 0.25);
    }

    private boolean isLayerConnector(int x, int y, int z, int separatorBottom, int separatorTop) {
        int regionX = Math.floorDiv(x, 24);
        int regionZ = Math.floorDiv(z, 24);
        if (hash2d(regionX, regionZ, 0xAA77_30F1L) < 0.08) {
            return false;
        }

        int localX = Math.floorMod(x, 24) - 12;
        int localZ = Math.floorMod(z, 24) - 12;
        if (localX * localX + localZ * localZ <= 4) {
            return true;
        }

        int midY = (separatorBottom + separatorTop) / 2;
        if (Math.abs(y - midY) <= 1 && (Math.floorMod(x, 12) == 0 || Math.floorMod(z, 12) == 0)) {
            return hash3d(x >> 1, y, z >> 1, 0x8BAD_F00DL) > -0.15;
        }
        return false;
    }

    private boolean isLowerCave(int x, int y, int z) {
        if (y <= lavaLevel - 2) {
            return false;
        }
        if (y >= lowerLayerCeiling - 1) {
            return true;
        }

        double band = 1.0 - (Math.abs(y - (lavaLevel + lowerLayerCeiling) * 0.5) / Math.max(1.0, (lowerLayerCeiling - lavaLevel) * 0.5));
        double base = Math.abs(hash3d(
                (int) Math.floor(x * caveFrequency),
                (int) Math.floor(y * caveFrequency * 0.9),
                (int) Math.floor(z * caveFrequency),
                0xC2B2_AE3DL
        ));
        double detail = Math.abs(hash3d(
                (int) Math.floor(x * caveFrequency * 1.8),
                (int) Math.floor(y * caveFrequency * 1.3),
                (int) Math.floor(z * caveFrequency * 1.8),
                0x1656_67B1L
        ));
        return base + detail * 0.55 < 0.66 + band * 0.12;
    }

    private double hash2d(int x, int z, long salt) {
        return hash3d(x, 0, z, salt);
    }

    private double hash3d(int x, int y, int z, long salt) {
        long mixed = worldSeed ^ salt;
        mixed ^= (long) x * 0x9E37_79B9_7F4A_7C15L;
        mixed ^= (long) y * 0xC2B2_AE3D_27D4_EB4FL;
        mixed ^= (long) z * 0x1656_67B1_9E37_79F9L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D0_49BB_1331_11EBL;
        mixed = mixed ^ (mixed >>> 31);
        double normalized = (mixed >>> 11) * 0x1.0p-53;
        return normalized * 2.0 - 1.0;
    }
}
