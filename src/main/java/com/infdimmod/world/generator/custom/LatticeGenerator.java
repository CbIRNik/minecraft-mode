package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class LatticeGenerator extends BaseCustomGenerator {
    public LatticeGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        int grid = 14;
        int thickness = 2;

        boolean xB = Math.abs(x % grid) < thickness;
        boolean yB = Math.abs(y % grid) < thickness;
        boolean zB = Math.abs(z % grid) < thickness;

        return (xB && yB) || (yB && zB) || (xB && zB);
    }
}