package com.infdimmod.world.generator;

import com.infdimmod.world.generator.custom.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.source.BiomeSource;
import java.util.HashMap;
import java.util.Map;

public class DimTypeRegistry {
    private static final Map<String, DimGeneratorProvider> TYPES = new HashMap<>();

    static {
        register("03", (s, seed, l) -> new SpongeGenerator(getVanillaBiomes(s), seed));
        register("05", (s, seed, l) -> new SphericalClustersGenerator(getVanillaBiomes(s), seed));
        register("08", (s, seed, l) -> new TorusRingGenerator(getVanillaBiomes(s), seed));
        register("09", (s, seed, l) -> new LatticeGenerator(getVanillaBiomes(s), seed));
        register("23", (s, seed, l) -> new DataFragmentsGenerator(getVanillaBiomes(s), seed));
        register("39", (s, seed, l) -> new AshWastelandGenerator(getVanillaBiomes(s), seed));
        register("43", (s, seed, l) -> new ShatteredSavannaPlusGenerator(getVanillaBiomes(s), seed));
        register("46", (s, seed, l) -> new EndStyleOverworldGenerator(getVanillaBiomes(s), seed));
    }

    private static BiomeSource getVanillaBiomes(MinecraftServer server) {
        var overworldGenerator = server.getOverworld().getChunkManager().getChunkGenerator();
        var biomeSource = overworldGenerator.getBiomeSource();
        return biomeSource;
    }

    public static void register(String code, DimGeneratorProvider provider) {
        TYPES.put(code, provider);
    }

    public static DimGeneratorProvider get(String code) {
        if (TYPES.containsKey(code)) {
            return TYPES.get(code);
        }

        return (server, seed, lookup) -> {
            var noiseSettings = server.getRegistryManager()
                    .getWrapperOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS)
                    .getOrThrow(net.minecraft.world.gen.chunk.ChunkGeneratorSettings.OVERWORLD);
            return new net.minecraft.world.gen.chunk.NoiseChunkGenerator(
                    getVanillaBiomes(server),
                    noiseSettings
            );
        };
    }
}