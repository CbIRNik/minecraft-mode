package com.infdimmod.world.generator;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.Entities.custom.ArthurEntity;
import com.infdimmod.Entities.custom.DrunGuardEntity;
import com.infdimmod.Entities.custom.FatOmayGadnostEntity;
import com.infdimmod.Entities.custom.FogiApexEntity;
import com.infdimmod.Entities.custom.FogiEntity;
import com.infdimmod.Entities.custom.LittleTastyBabyEntity;
import com.infdimmod.Entities.custom.ResidentEntity;
import com.infdimmod.Entities.custom.StudentEntity;
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
import java.util.function.Consumer;

public class DeterministicChaosGenerator extends ChunkGenerator {
    public static final MapCodec<DeterministicChaosGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    Codec.LONG.fieldOf("seed").forGetter(generator -> generator.worldSeed)
            ).apply(instance, instance.stable(DeterministicChaosGenerator::new))
    );

    private static final int COLLIDER_RADIUS_CHUNKS = 5;
    private static final int COLLIDER_HALF_HEIGHT = 22;
    private static final int DORM_FLOOR_HEIGHT = 3;
    private static final int MURINO_DORM_GRID_SPACING_CHUNKS = 4;
    private static final int OTHER_DORM_GRID_SPACING_CHUNKS = 10;
    private static final double MURINO_DORM_THRESHOLD = -0.55D;
    private static final double OTHER_DORM_THRESHOLD = 0.2D;
    private static final double MURINO_BIOME_CHANCE = 0.24D;

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
        placeDormitories(chunk, chunkPos);
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

        DormitoryPlan dormitoryPlan = getDormitoryPlanForAnchor(chunkPos);
        if (dormitoryPlan != null) {
            spawnDormitoryPopulation(world, dormitoryPlan);
            return;
        }

        if (isColliderCoreChunk(chunkPos)) {
            spawnDrunGuard(world, chunkPos, 8, 8, 0.0f);
            spawnDrunGuard(world, chunkPos, 4, 11, 90.0f);
            spawnDrunGuard(world, chunkPos, 11, 4, 180.0f);
            if (hash2d(chunkPos.x, chunkPos.z, 0x8A70_1134L) > 0.42) {
                spawnFatOmayGadnost(world, chunkPos, 13, 13, 90.0f);
            }
            return;
        }

        double patrolChance = hash2d(chunkPos.x, chunkPos.z, 0xD0A4_5001L);
        if (patrolChance > 0.88) {
            spawnFogiApex(world, chunkPos, 8, 8, 0.0f);
        } else if (patrolChance > 0.73) {
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

    private void placeDormitories(Chunk chunk, ChunkPos chunkPos) {
        forEachNearbyDormitoryPlan(chunkPos, 2, plan -> placeDormitoryInChunk(chunk, plan));
    }

    private void placeDormitoryInChunk(Chunk chunk, DormitoryPlan plan) {
        int xHalf = plan.xHalf();
        int zHalf = plan.zHalf();
        int roofY = plan.baseY + plan.floors * DORM_FLOOR_HEIGHT;

        for (int dx = -xHalf - 1; dx <= xHalf + 1; dx++) {
            for (int dz = -zHalf - 1; dz <= zHalf + 1; dz++) {
                int worldX = plan.centerX + dx;
                int worldZ = plan.centerZ + dz;
                boolean inFootprint = Math.abs(dx) <= xHalf && Math.abs(dz) <= zHalf;

                if (!inFootprint) {
                    if (Math.abs(dx) <= xHalf + 1 && Math.abs(dz) <= zHalf + 1
                            && Math.floorMod(dx + dz + plan.anchorX + plan.anchorZ, 2) == 0) {
                        setBlockInChunk(chunk, worldX, plan.baseY - 1, worldZ, Blocks.CRACKED_STONE_BRICKS.getDefaultState());
                    }
                    continue;
                }

                setBlockInChunk(chunk, worldX, plan.baseY - 2, worldZ, Blocks.COBBLED_DEEPSLATE.getDefaultState());
                setBlockInChunk(chunk, worldX, plan.baseY - 1, worldZ, Blocks.STONE_BRICKS.getDefaultState());

                for (int floor = 0; floor < plan.floors; floor++) {
                    int floorBase = plan.baseY + floor * DORM_FLOOR_HEIGHT;
                    setBlockInChunk(chunk, worldX, floorBase, worldZ, Blocks.STONE.getDefaultState());

                    boolean wall = Math.abs(dx) == xHalf || Math.abs(dz) == zHalf;
                    boolean corner = Math.abs(dx) == xHalf && Math.abs(dz) == zHalf;
                    boolean longSide = (plan.longAxisX && Math.abs(dz) == zHalf) || (!plan.longAxisX && Math.abs(dx) == xHalf);
                    boolean windowColumn = Math.floorMod((plan.longAxisX ? dx : dz) + floor, 4) == 0;
                    boolean hasWindow = wall && !corner && longSide && windowColumn;

                    for (int localY = 1; localY <= 2; localY++) {
                        BlockState state;
                        if (!wall) {
                            state = Blocks.AIR.getDefaultState();
                        } else if (hasWindow) {
                            state = Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultState();
                        } else if (localY == 1 && Math.floorMod(dx + dz + floor, 6) == 0) {
                            state = Blocks.POLISHED_ANDESITE.getDefaultState();
                        } else {
                            state = Blocks.STONE_BRICKS.getDefaultState();
                        }
                        setBlockInChunk(chunk, worldX, floorBase + localY, worldZ, state);
                    }
                }

                setBlockInChunk(chunk, worldX, roofY, worldZ, Blocks.DEEPSLATE_TILES.getDefaultState());
                if (Math.abs(dx) == xHalf || Math.abs(dz) == zHalf) {
                    setBlockInChunk(chunk, worldX, roofY + 1, worldZ, Blocks.DEEPSLATE_BRICKS.getDefaultState());
                }
            }
        }

        carveDormitoryEntrance(chunk, plan);
        carveDormitoryShaft(chunk, plan, roofY);
    }

    private void carveDormitoryEntrance(Chunk chunk, DormitoryPlan plan) {
        int entranceX = plan.centerX + (plan.longAxisX ? 0 : plan.xHalf());
        int entranceZ = plan.centerZ + (plan.longAxisX ? plan.zHalf() : 0);
        int outsideX = entranceX + (plan.longAxisX ? 0 : 1);
        int outsideZ = entranceZ + (plan.longAxisX ? 1 : 0);

        for (int lateral = -1; lateral <= 1; lateral++) {
            int doorX = entranceX + (plan.longAxisX ? lateral : 0);
            int doorZ = entranceZ + (plan.longAxisX ? 0 : lateral);
            setBlockInChunk(chunk, doorX, plan.baseY + 1, doorZ, Blocks.AIR.getDefaultState());
            setBlockInChunk(chunk, doorX, plan.baseY + 2, doorZ, Blocks.AIR.getDefaultState());
            setBlockInChunk(chunk, doorX, plan.baseY, doorZ, Blocks.POLISHED_ANDESITE.getDefaultState());
        }

        for (int lateral = -2; lateral <= 2; lateral++) {
            int pathX = outsideX + (plan.longAxisX ? lateral : 0);
            int pathZ = outsideZ + (plan.longAxisX ? 0 : lateral);
            setBlockInChunk(chunk, pathX, plan.baseY - 1, pathZ, Blocks.STONE_BRICKS.getDefaultState());
            setBlockInChunk(chunk, pathX, plan.baseY, pathZ, Blocks.STONE.getDefaultState());
        }
    }

    private void carveDormitoryShaft(Chunk chunk, DormitoryPlan plan, int roofY) {
        int shaftCenterX = plan.centerX + (plan.longAxisX ? -2 : 0);
        int shaftCenterZ = plan.centerZ + (plan.longAxisX ? 0 : -2);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int worldX = shaftCenterX + dx;
                int worldZ = shaftCenterZ + dz;
                for (int y = plan.baseY + 1; y < roofY; y++) {
                    setBlockInChunk(chunk, worldX, y, worldZ, Blocks.AIR.getDefaultState());
                }
            }
        }
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

    private void spawnResident(ServerWorld world, int worldX, int worldZ, float yaw) {
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }
        ResidentEntity resident = new ResidentEntity(ModEntities.RESIDENT_ENTITY_TYPE, world);
        resident.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        resident.setPersistent();
        world.spawnEntity(resident);
    }

    private void spawnStudent(ServerWorld world, int worldX, int worldZ, float yaw) {
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }
        StudentEntity student = new StudentEntity(ModEntities.STUDENT_ENTITY_TYPE, world);
        student.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        student.setPersistent();
        world.spawnEntity(student);
    }

    private void spawnDormitoryPopulation(ServerWorld world, DormitoryPlan plan) {
        int spawnX = plan.centerX + (plan.longAxisX ? 0 : plan.xHalf() + 2);
        int spawnZ = plan.centerZ + (plan.longAxisX ? plan.zHalf() + 2 : 0);

        spawnResident(world, spawnX, spawnZ, 180.0f);
        spawnResident(world, spawnX + (plan.longAxisX ? 3 : 0), spawnZ + (plan.longAxisX ? 0 : 3), 90.0f);
        spawnStudent(world, spawnX - (plan.longAxisX ? 3 : 0), spawnZ - (plan.longAxisX ? 0 : 3), 0.0f);
        spawnStudent(world, spawnX + (plan.longAxisX ? -2 : 2), spawnZ + (plan.longAxisX ? 2 : -2), 45.0f);

        if (plan.floors >= 9 || plan.murinoDistrict) {
            spawnResident(world, spawnX + (plan.longAxisX ? 5 : 0), spawnZ + (plan.longAxisX ? 0 : 5), 270.0f);
            spawnStudent(world, spawnX + (plan.longAxisX ? -5 : 0), spawnZ + (plan.longAxisX ? 0 : -5), 225.0f);
        }
    }

    private int findSpawnY(ServerWorld world, int worldX, int worldZ) {
        int minY = world.getBottomY() + 1;
        int maxY = world.getTopY() - 2;
        int worldY = Math.max(minY, Math.min(maxY, sampleSurfaceHeight(worldX, worldZ) + 1));
        BlockPos.Mutable mutable = new BlockPos.Mutable(worldX, worldY, worldZ);

        while (worldY <= maxY && !world.getBlockState(mutable).isAir()) {
            worldY++;
            mutable.setY(worldY);
        }
        if (worldY > maxY) {
            return world.getBottomY() - 1;
        }
        return worldY;
    }

    private void spawnArthur(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }

        ArthurEntity arthur = new ArthurEntity(ModEntities.ARTHUR_ENTITY_TYPE, world);
        arthur.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        arthur.setPersistent();
        world.spawnEntity(arthur);
    }

    private void spawnLittleTastyBaby(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }

        LittleTastyBabyEntity baby = new LittleTastyBabyEntity(ModEntities.LITTLE_TASTY_BABY_ENTITY_TYPE, world);
        baby.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        baby.setPersistent();
        baby.setBreedingAge(-24_000);
        world.spawnEntity(baby);
    }

    private void spawnFogi(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }

        FogiEntity fogi = new FogiEntity(ModEntities.FOGI_ENTITY_TYPE, world);
        fogi.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        fogi.setPersistent();
        world.spawnEntity(fogi);
    }

    private void spawnFogiApex(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }

        FogiApexEntity fogiApex = new FogiApexEntity(ModEntities.FOGI_APEX_ENTITY_TYPE, world);
        fogiApex.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        fogiApex.setPersistent();
        world.spawnEntity(fogiApex);
    }

    private void spawnDrunGuard(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = getColliderCoreY();

        DrunGuardEntity guard = new DrunGuardEntity(ModEntities.DRUN_GUARD_ENTITY_TYPE, world);
        guard.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        guard.setGuardPost(new BlockPos(worldX, worldY, worldZ));
        guard.setPersistent();
        world.spawnEntity(guard);
    }

    private void spawnFatOmayGadnost(ServerWorld world, ChunkPos chunkPos, int offsetX, int offsetZ, float yaw) {
        int worldX = chunkPos.getStartX() + offsetX;
        int worldZ = chunkPos.getStartZ() + offsetZ;
        int worldY = findSpawnY(world, worldX, worldZ);
        if (worldY < world.getBottomY()) {
            return;
        }

        FatOmayGadnostEntity gadnost = new FatOmayGadnostEntity(ModEntities.FAT_OMAY_GADNOST_ENTITY_TYPE, world);
        gadnost.refreshPositionAndAngles(worldX + 0.5, worldY, worldZ + 0.5, yaw, 0.0f);
        gadnost.setPersistent();
        world.spawnEntity(gadnost);
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
