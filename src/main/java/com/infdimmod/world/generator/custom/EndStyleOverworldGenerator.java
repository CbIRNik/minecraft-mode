package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class EndStyleOverworldGenerator extends BaseCustomGenerator {
    public EndStyleOverworldGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        int islandSize = 30;
        int grid = 100;
        int cx = Math.floorDiv(x, grid) * grid + 50;
        int cz = Math.floorDiv(z, grid) * grid + 50;

        double dist = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(z - cz, 2));
        double surface = Math.sin(x * 0.2) * 3.0;
        return dist < islandSize && y < (64 + surface) && y > (64 - (islandSize - dist));
    }
}