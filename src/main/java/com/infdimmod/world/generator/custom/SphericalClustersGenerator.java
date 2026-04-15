package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class SphericalClustersGenerator extends BaseCustomGenerator {
    public SphericalClustersGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        int grid = 32;
        int cx = Math.floorDiv(x, grid) * grid + grid / 2;
        int cy = Math.floorDiv(y, grid) * grid + grid / 2;
        int cz = Math.floorDiv(z, grid) * grid + grid / 2;

        double radius = 10.0 + Math.sin(cx * 0.5 + cz * 0.5) * 4.0;

        double distSq = Math.pow(x - cx, 2) + Math.pow(y - cy, 2) + Math.pow(z - cz, 2);
        return distSq < radius * radius;
    }
}