package com.infdimmod.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

public class DeterministicChaosGenerator extends ChunkGenerator {
    public static final MapCodec<DeterministicChaosGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    MapCodec.unit(0L).fieldOf("seed").forGetter(generator -> generator.worldSeed)
            ).apply(instance, instance.stable(DeterministicChaosGenerator::new))
    );

    private final long worldSeed;
    private final List<BlockState> palette = new ArrayList<>();
    private final double[] mathConstants = new double[10];

    private final double xSpread, ySpread, zSpread;
    private final double noiseThreshold;
    private final double distortion;

    public DeterministicChaosGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.worldSeed = seed;
        Random r = new Random(seed);

        int paletteSize = 5 + r.nextInt(11);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(pickStableBlock(r));
        }

        this.xSpread = 0.01 + r.nextDouble() * 0.08;
        this.ySpread = 0.01 + r.nextDouble() * 0.08;
        this.zSpread = 0.01 + r.nextDouble() * 0.08;

        this.noiseThreshold = 0.1 + r.nextDouble() * 0.7;

        this.distortion = r.nextDouble() * 7.0;

        for (int i = 0; i < mathConstants.length; i++) {
            mathConstants[i] = (r.nextDouble() - 0.5);
        }
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double rx = (chunkPos.getStartX() + x) * xSpread;
                double rz = (chunkPos.getStartZ() + z) * zSpread;

                for (int y = chunk.getBottomY(); y < chunk.getTopY(); y++) {
                    double ry = y * ySpread;

                    double dx = Math.sin(ry * mathConstants[0] + rz * mathConstants[1]) * distortion;
                    double dy = Math.cos(rx * mathConstants[2] + rz * mathConstants[3]) * distortion;
                    double dz = Math.sin(rx * mathConstants[4] + ry * mathConstants[5]) * distortion;

                    double noise = Math.sin(rx + dx) + Math.cos(ry + dy) + Math.sin(rz + dz);

                    noise += (Math.sin(rx * 3) * Math.cos(rz * 3) * Math.sin(ry * 3)) * 0.5;

                    if (noise > noiseThreshold) {
                        int blockIndex = (int) (Math.abs(noise * 5 + (y * 0.1)) % palette.size());
                        chunk.setBlockState(mutable.set(x, y, z), palette.get(blockIndex), false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private BlockState pickStableBlock(Random r) {
        int size = Registries.BLOCK.size();
        for (int i = 0; i < 500; i++) {
            var block = Registries.BLOCK.get(r.nextInt(size));
            if (block != null) {
                BlockState state = block.getDefaultState();
                if (state.isFullCube(null, null)
                        && !state.hasBlockEntity()
                        && !state.isAir()
                        && block != Blocks.BEDROCK) {
                    return state;
                }
            }
        }
        return Blocks.STONE.getDefaultState();
    }

    @Override public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {}
    @Override protected MapCodec<? extends ChunkGenerator> getCodec() { return CODEC; }
    @Override public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}
    @Override public int getWorldHeight() { return 384; }
    @Override public int getSeaLevel() { return -64; }
    @Override public int getMinimumY() { return -64; }
    @Override public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) { return 0; }
    @Override public void populateEntities(ChunkRegion region) {}
    @Override public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) { return new VerticalBlockSample(world.getBottomY(), new BlockState[0]); }
    @Override public void getDebugHudText(java.util.List<String> text, NoiseConfig noiseConfig, BlockPos pos) { text.add("Rick & Morty Infinite Variety Generator"); }
}