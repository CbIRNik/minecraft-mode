package com.infdimmod.world.generator.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public abstract class BaseCustomGenerator extends ChunkGenerator {
    protected final long worldSeed;
    protected final List<BlockState> palette = new ArrayList<>();
    protected final Random random;

    public BaseCustomGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.worldSeed = seed;
        this.random = new Random(seed);
        this.generatePalette();
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = cp.getStartX() + x;
                int worldZ = cp.getStartZ() + z;

                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                    BlockState state = getBlockAt(worldX, y, worldZ);
                    if (state != null && !state.isAir()) {
                        chunk.setBlockState(mutablePos.set(x, y, z), state, false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    protected BlockState getBlockAt(int x, int y, int z) {
        if (shouldGenerate(x, y, z)) {
            return getPaletteBlock(x, y, z);
        }
        return Blocks.AIR.getDefaultState();
    }

    protected abstract boolean shouldGenerate(int x, int y, int z);

    protected BlockState getPaletteBlock(int x, int y, int z) {
        int index = Math.abs(x ^ y ^ z ^ (int)worldSeed) % palette.size();
        return palette.get(index);
    }

    protected void generatePalette() {
        int size = 3 + random.nextInt(4);
        for (int i = 0; i < size; i++) {
            palette.add(pickStableBlock(random));
        }
    }

    protected BlockState pickStableBlock(Random r) {
        int registrySize = Registries.BLOCK.size();
        for (int i = 0; i < 500; i++) {
            var block = Registries.BLOCK.get(r.nextInt(registrySize));
            if (block != null) {
                BlockState state = block.getDefaultState();

                if (!state.isAir() &&
                        state.isFullCube(null, null) &&
                        !state.hasBlockEntity() &&
                        block != Blocks.BEDROCK &&
                        block != Blocks.BARRIER &&
                        state.getFluidState().isEmpty()) {
                    return state;
                }
            }
        }
        return Blocks.STONE.getDefaultState();
    }

    @Override public int getWorldHeight() { return 384; }
    @Override public int getMinimumY() { return -64; }
    @Override public int getSeaLevel() { return -64; }

    @Override protected MapCodec<? extends ChunkGenerator> getCodec() { return null; }
    @Override public void buildSurface(ChunkRegion r, StructureAccessor s, NoiseConfig n, Chunk c) {}
    @Override public void carve(ChunkRegion cr, long s, NoiseConfig n, BiomeAccess ba, StructureAccessor sa, Chunk c, GenerationStep.Carver cs) {}
    @Override public void populateEntities(ChunkRegion r) {}
    @Override public int getHeight(int x, int z, Heightmap.Type h, HeightLimitView w, NoiseConfig n) { return 0; }
    @Override public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView w, NoiseConfig n) {
        return new VerticalBlockSample(w.getBottomY(), new BlockState[0]);
    }
    @Override public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Current Dim Generator: " + this.getClass().getSimpleName());
    }
}