package com.infdimmod.world.generator.custom;

import net.minecraft.world.biome.source.BiomeSource;

public class TorusRingGenerator extends BaseCustomGenerator {
    public TorusRingGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, seed);
    }

    @Override
    protected boolean shouldGenerate(int x, int y, int z) {
        double R = 18.0;
        double r = 4.5;
        int spacing = 48;

        double lx = (x % spacing + spacing) % spacing - spacing / 2.0;
        double ly = (y % spacing + spacing) % spacing - spacing / 2.0;
        double lz = (z % spacing + spacing) % spacing - spacing / 2.0;

        double distToAxis = Math.sqrt(lx * lx + lz * lz);
        return Math.pow(R - distToAxis, 2) + ly * ly < r * r;
    }
}