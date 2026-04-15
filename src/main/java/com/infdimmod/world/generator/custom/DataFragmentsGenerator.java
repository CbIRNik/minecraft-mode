package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;
import java.util.Random;

public class DataFragmentsGenerator extends BaseCustomGenerator {
    public DataFragmentsGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        Random r = new Random((x >> 3) * 7431L + (y >> 3) * 1231L + (z >> 3) * 917L + worldSeed);
        if (r.nextFloat() > 0.05f) return false;

        return new Random(x * 31L + y * 17L + z + worldSeed).nextFloat() > 0.3f;
    }
}