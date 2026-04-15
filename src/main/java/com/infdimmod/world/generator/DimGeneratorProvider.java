package com.infdimmod.world.generator;

import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public interface DimGeneratorProvider {
    ChunkGenerator createGenerator(MinecraftServer server, long seed, RegistryWrapper.Impl<Biome> biomeLookup);
}