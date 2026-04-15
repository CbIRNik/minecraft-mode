package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class AshWastelandGenerator extends BaseCustomGenerator {
    public AshWastelandGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        double noise = Math.sin(x * 0.05) * Math.cos(z * 0.05) * 5.0;
        double dunes = Math.sin(x * 0.01) * 15.0;
        return y < (64 + noise + dunes);
    }
}