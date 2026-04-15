package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class ShatteredSavannaPlusGenerator extends BaseCustomGenerator {
    public ShatteredSavannaPlusGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        // Усиленный шум Перлина (упрощенная имитация синусами)
        double noise = Math.sin(x * 0.02) * Math.sin(z * 0.02) * 120.0;
        double detail = Math.cos(x * 0.1) * Math.sin(z * 0.1) * 20.0;
        return y < (100 + noise + detail);
    }
}