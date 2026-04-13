package com.infdimmod.burmaldeniya;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.InfDimMod;
import com.infdimmod.world.generator.MurinoBlendBiomeSource;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;

public final class MurinoWorldgenHooks {
    public static final Identifier MURINO_BIOME_ID = Identifier.of(InfDimMod.MOD_ID, "murino");
    public static final RegistryKey<Biome> MURINO_BIOME_KEY = RegistryKey.of(RegistryKeys.BIOME, MURINO_BIOME_ID);
    public static final TagKey<Biome> BURMALDENIYA_SPAWN_BIOMES = TagKey.of(
            RegistryKeys.BIOME,
            Identifier.of(InfDimMod.MOD_ID, "has_burmaldeniya_spawns")
    );
    private static final RegistryKey<Biome>[] MURINO_FALLBACK_BIOME_KEYS = new RegistryKey[]{
            BiomeKeys.SWAMP,
            BiomeKeys.DARK_FOREST,
            BiomeKeys.TAIGA,
            BiomeKeys.PLAINS
    };

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
                SpawnGroup.CREATURE,
                ModEntities.STUDENT_ENTITY_TYPE,
                45,
                2,
                5
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.CREATURE,
                ModEntities.LITTLE_TASTY_BABY_ENTITY_TYPE,
                26,
                1,
                2
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.CREATURE,
                ModEntities.ARTHUR_ENTITY_TYPE,
                9,
                1,
                1
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.CREATURE,
                ModEntities.FAT_OMAY_GADNOST_ENTITY_TYPE,
                4,
                1,
                1
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.MONSTER,
                ModEntities.FOGI_ENTITY_TYPE,
                95,
                2,
                4
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.MONSTER,
                ModEntities.DRUN_GUARD_ENTITY_TYPE,
                68,
                1,
                2
        );

        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BURMALDENIYA_SPAWN_BIOMES),
                SpawnGroup.MONSTER,
                ModEntities.FOGI_APEX_ENTITY_TYPE,
                24,
                1,
                1
        );
    }

    public static BiomeSource createBiomeSource(ServerWorld overworld, long seed) {
        BiomeSource fallbackSource = overworld.getChunkManager().getChunkGenerator().getBiomeSource();
        Registry<Biome> biomeRegistry = overworld.getRegistryManager().get(RegistryKeys.BIOME);
        RegistryEntry.Reference<Biome> murinoBiome = biomeRegistry.getEntry(MURINO_BIOME_KEY).orElse(null);
        if (murinoBiome == null) {
            murinoBiome = resolveFallbackMurinoBiome(biomeRegistry);
        }

        if (murinoBiome == null) {
            return fallbackSource;
        }

        if (!murinoBiome.matchesKey(MURINO_BIOME_KEY)) {
            InfDimMod.LOGGER.warn("Murino biome datapack entry missing; using {} as fallback biome for Murino blending",
                    murinoBiome.getKey().map(key -> key.getValue().toString()).orElse("unknown"));
        }
        return new MurinoBlendBiomeSource(fallbackSource, murinoBiome, seed);
    }

    private static RegistryEntry.Reference<Biome> resolveFallbackMurinoBiome(Registry<Biome> biomeRegistry) {
        for (RegistryKey<Biome> fallbackKey : MURINO_FALLBACK_BIOME_KEYS) {
            RegistryEntry.Reference<Biome> fallbackBiome = biomeRegistry.getEntry(fallbackKey).orElse(null);
            if (fallbackBiome != null) {
                return fallbackBiome;
            }
        }
        return null;
    }
}
