package com.infdimmod.world.generator;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import java.util.HashMap;
import java.util.Map;

public class DimTypeRegistry {
    private static final Map<String, DimGeneratorProvider> TYPES = new HashMap<>();

    static {
        register("00", (server, seed, lookup) -> {
            var overworldGenerator = server.getOverworld().getChunkManager().getChunkGenerator();
            var biomeSource = overworldGenerator.getBiomeSource();

            return new DeterministicChaosGenerator(biomeSource, seed);
        });

        register("AA", (server, seed, lookup) -> {
            RegistryWrapper.Impl<ChunkGeneratorSettings> settingsWrapper =
                    server.getRegistryManager().getWrapperOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS);

            RegistryEntry<ChunkGeneratorSettings> settingsEntry =
                    settingsWrapper.getOrThrow(ChunkGeneratorSettings.OVERWORLD);

            var overworldGenerator = server.getOverworld().getChunkManager().getChunkGenerator();
            var biomeSource = overworldGenerator.getBiomeSource();

            return new NoiseChunkGenerator(biomeSource, settingsEntry);
        });
    }

    public static void register(String code, DimGeneratorProvider provider) {
        TYPES.put(code, provider);
    }

    public static DimGeneratorProvider get(String code) {
        return TYPES.getOrDefault(code, TYPES.get("00"));
    }
}