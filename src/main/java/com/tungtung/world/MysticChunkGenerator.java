package com.tungtung.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
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
import java.util.concurrent.CompletableFuture;

public class MysticChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MysticChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(generator -> generator.biomeSource))
                    .apply(instance, MysticChunkGenerator::new)
    );

    public MysticChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    @Override
    public int getWorldHeight() {
        return 256;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                    StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 64; y++) {
                        mutable.set(x, y, z);
                        chunk.setBlockState(mutable, Blocks.STONE.getDefaultState(), false);
                    }
                    
                    mutable.set(x, 64, z);
                    chunk.setBlockState(mutable, Blocks.GRASS_BLOCK.getDefaultState(), false);
                    
                    int worldX = chunk.getPos().getStartX() + x;
                    int worldZ = chunk.getPos().getStartZ() + z;
                    double noise = Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1) * 5;
                    
                    for (int y = 65; y < 65 + (int)noise; y++) {
                        mutable.set(x, y, z);
                        chunk.setBlockState(mutable, Blocks.STONE.getDefaultState(), false);
                    }
                }
            }
            
            return chunk;
        });
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 64;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[256];
        for (int i = 0; i < 64; i++) {
            states[i] = Blocks.STONE.getDefaultState();
        }
        states[64] = Blocks.GRASS_BLOCK.getDefaultState();
        for (int i = 65; i < 256; i++) {
            states[i] = Blocks.AIR.getDefaultState();
        }
        return new VerticalBlockSample(0, states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Mystic World Generator");
    }
}
