package com.infdimmod.world.generator;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public interface DimGeneratorProvider {
    ChunkGenerator createGenerator(MinecraftServer server, long seed, net.minecraft.registry.RegistryWrapper<net.minecraft.world.biome.Biome> biomeRegistry);
}