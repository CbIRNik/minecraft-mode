package com.infdimmod.burmaldeniya;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.InfDimMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;

public final class MurinoWorldgenHooks {
    public static final Identifier MURINO_BIOME_ID = Identifier.of(InfDimMod.MOD_ID, "murino");
    public static final RegistryKey<Biome> MURINO_BIOME_KEY = RegistryKey.of(RegistryKeys.BIOME, MURINO_BIOME_ID);
    public static final TagKey<Biome> BURMALDENIYA_SPAWN_BIOMES = TagKey.of(
            RegistryKeys.BIOME,
            Identifier.of(InfDimMod.MOD_ID, "has_burmaldeniya_spawns")
    );

    private MurinoWorldgenHooks() {
    }

    public static void register() {
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.CREATURE,
                ModEntities.RESIDENT_ENTITY_TYPE,
                30,
                2,
                4
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.MONSTER,
                ModEntities.FOGI_ENTITY_TYPE,
                80,
                1,
                3
        );
    }

    public static BiomeSource createBiomeSource(ServerWorld overworld) {
        RegistryEntry<Biome> murinoBiome = overworld.getRegistryManager()
                .get(RegistryKeys.BIOME)
                .getEntry(MURINO_BIOME_KEY)
                .orElse(null);

        if (murinoBiome != null) {
            return new FixedBiomeSource(murinoBiome);
        }

        return overworld.getChunkManager().getChunkGenerator().getBiomeSource();
    }
}
