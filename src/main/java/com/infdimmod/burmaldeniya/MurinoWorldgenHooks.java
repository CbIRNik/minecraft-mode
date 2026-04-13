package com.infdimmod.burmaldeniya;

import com.infdimmod.InfDimMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;

public final class MurinoWorldgenHooks {
    public static final Identifier MURINO_BIOME_ID = Identifier.of(InfDimMod.MOD_ID, "murino");
    public static final RegistryKey<Biome> MURINO_BIOME_KEY = RegistryKey.of(RegistryKeys.BIOME, MURINO_BIOME_ID);

    private MurinoWorldgenHooks() {
    }

    public static BiomeSource createBiomeSource(ServerWorld overworld) {
        RegistryEntry<Biome> murinoBiome = overworld.getRegistryManager()
                .getOrThrow(RegistryKeys.BIOME)
                .getEntry(MURINO_BIOME_KEY)
                .orElse(null);

        if (murinoBiome != null) {
            return new FixedBiomeSource(murinoBiome);
        }

        return overworld.getChunkManager().getChunkGenerator().getBiomeSource();
    }
}
