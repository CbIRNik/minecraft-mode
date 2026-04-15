package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class SpongeGenerator extends BaseCustomGenerator {
    public SpongeGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        if (y < 0 || y > 128) return false;

        int xx = Math.abs(x), yy = Math.abs(y), zz = Math.abs(z);
        for (int size = 1; size <= 81; size *= 3) {
            int count = 0;
            if ((xx / size) % 3 == 1) count++;
            if ((yy / size) % 3 == 1) count++;
            if ((zz / size) % 3 == 1) count++;
            if (count >= 2) return false;
        }
        return true;
    }
}