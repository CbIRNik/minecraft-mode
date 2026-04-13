package com.infdimmod.world.generator;

import com.infdimmod.Entities.ModEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
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

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class DeterministicChaosGenerator extends ChunkGenerator {
    public static final MapCodec<DeterministicChaosGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    Codec.LONG.fieldOf("seed").forGetter(generator -> generator.worldSeed)
            ).apply(instance, instance.stable(DeterministicChaosGenerator::new))
    );

    private final long worldSeed;
    private final int surfaceBase;
    private final int lowerLayerCeiling;
    private final int lavaLevel;
    private final double terrainFrequencyA;
    private final double terrainFrequencyB;
    private final double caveFrequency;

    public DeterministicChaosGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.worldSeed = seed;
        Random random = new Random(seed ^ 0xC00F_FEEEL);
        this.surfaceBase = 68 + random.nextInt(6);
        this.lowerLayerCeiling = 18 + random.nextInt(10);
        this.lavaLevel = this.lowerLayerCeiling - 8;
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
        text.add("Burmaldeniya two-layer generator");
        text.add("Dorm chunk: " + (isDormChunk(new ChunkPos(pos)) ? "yes" : "no"));
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

        VillagerEntity resident = new VillagerEntity(ModEntities.RESIDENT_ENTITY_TYPE, world);
        resident.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        resident.setPersistent();
        world.spawnEntity(resident);
    }

    private void spawnFogi(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = sampleSurfaceHeight(worldX, worldZ) + 1;

        ZombieEntity fogi = new ZombieEntity(ModEntities.FOGI_ENTITY_TYPE, world);
        fogi.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        fogi.setPersistent();
        world.spawnEntity(fogi);
    }

    private boolean isDormChunk(ChunkPos chunkPos) {
        if (Math.floorMod(chunkPos.x, 12) != 0 || Math.floorMod(chunkPos.z, 12) != 0) {
            return false;
        }
        return hash2d(chunkPos.x, chunkPos.z, 0x0D05_4510L) > -0.25;
    }

    private BlockState sampleBlockState(int x, int y, int z) {
        if (y <= getMinimumY() + 1) {
            return Blocks.BEDROCK.getDefaultState();
        }

        int surfaceHeight = sampleSurfaceHeight(x, z);
        if (y > surfaceHeight) {
            return Blocks.AIR.getDefaultState();
        }

        if (y > lowerLayerCeiling && isUpperCave(x, y, z)) {
            return Blocks.AIR.getDefaultState();
        }

        if (y <= lowerLayerCeiling) {
            if (isLowerCave(x, y, z)) {
                return y <= lavaLevel ? Blocks.LAVA.getDefaultState() : Blocks.AIR.getDefaultState();
            }
            return sampleLowerLayerMaterial(x, y, z);
        }

        if (y == surfaceHeight) {
            return Blocks.GRASS_BLOCK.getDefaultState();
        }

        if (y >= surfaceHeight - 3) {
            return Blocks.DIRT.getDefaultState();
        }

        if (y < 16) {
            return Blocks.DEEPSLATE.getDefaultState();
        }

        return Blocks.STONE.getDefaultState();
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

    private int sampleSurfaceHeight(int x, int z) {
        double rolling = Math.sin((x + worldSeed * 0.03125) * terrainFrequencyA) * 10.0
                + Math.cos((z - worldSeed * 0.015625) * terrainFrequencyA) * 9.0;
        double ridges = Math.sin((x + z) * terrainFrequencyB) * 4.0
                + Math.cos((x - z) * terrainFrequencyB * 0.8) * 3.0;
        double seeded = hash2d(x >> 2, z >> 2, 0xB529_7A4DL) * 6.0;
        int surface = surfaceBase + (int) Math.round(rolling + ridges + seeded);
        return Math.max(lowerLayerCeiling + 8, surface);
    }

    private boolean isUpperCave(int x, int y, int z) {
        if (y >= 56) {
            return false;
        }
        double cave = Math.abs(hash3d(x, y, z, 0x9E37_79B9L))
                + Math.abs(hash3d(x * 2, y, z * 2, 0x7F4A_7C15L)) * 0.5;
        return cave < 0.23 && y < sampleSurfaceHeight(x, z) - 2;
    }

    private boolean isLowerCave(int x, int y, int z) {
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
        return base + detail * 0.55 < 0.62;
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
