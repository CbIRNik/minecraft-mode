package com.infdimmod.world.generator;

import com.infdimmod.burmaldeniya.BurmaldeniyaConfig;
import com.infdimmod.burmaldeniya.MurinoWorldgenHooks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.stream.Stream;

public final class MurinoBlendBiomeSource extends BiomeSource {
    private static final double DEFAULT_MURINO_CHANCE = BurmaldeniyaConfig.Murino.BIOME_CHANCE;

    public static final MapCodec<MurinoBlendBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("delegate").forGetter(source -> source.delegate),
                    Biome.REGISTRY_CODEC.fieldOf("murino").forGetter(source -> source.murinoBiome),
                    Codec.LONG.fieldOf("seed").forGetter(source -> source.seed),
                    Codec.DOUBLE.optionalFieldOf("murino_chance", DEFAULT_MURINO_CHANCE).forGetter(source -> source.murinoChance)
            ).apply(instance, MurinoBlendBiomeSource::new)
    );

    private final BiomeSource delegate;
    private final RegistryEntry<Biome> murinoBiome;
    private final long seed;
    private final double murinoChance;

    public MurinoBlendBiomeSource(BiomeSource delegate, RegistryEntry<Biome> murinoBiome, long seed) {
        this(delegate, murinoBiome, seed, DEFAULT_MURINO_CHANCE);
    }

    private MurinoBlendBiomeSource(BiomeSource delegate, RegistryEntry<Biome> murinoBiome, long seed, double murinoChance) {
        this.delegate = delegate;
        this.murinoBiome = murinoBiome;
        this.seed = seed;
        this.murinoChance = Math.max(0.0D, Math.min(0.45D, murinoChance));
    }

    @Override
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return Stream.concat(delegate.getBiomes().stream(), Stream.of(murinoBiome)).distinct();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        RegistryEntry<Biome> fallback = delegate.getBiome(x, y, z, noise);
        if (!shouldInjectMurino(fallback, x, z)) {
            return fallback;
        }
        return murinoBiome;
    }

    private boolean shouldInjectMurino(RegistryEntry<Biome> fallback, int biomeX, int biomeZ) {
        if (fallback.matchesKey(MurinoWorldgenHooks.MURINO_BIOME_KEY)) {
            return true;
        }
        if (fallback.isIn(BiomeTags.IS_OCEAN)
                || fallback.isIn(BiomeTags.IS_DEEP_OCEAN)
                || fallback.isIn(BiomeTags.IS_RIVER)
                || fallback.isIn(BiomeTags.IS_BEACH)) {
            return false;
        }
        return sampleMurinoDensity(biomeX, biomeZ) < murinoChance;
    }

    private double sampleMurinoDensity(int biomeX, int biomeZ) {
        long continentalX = Math.floorDiv(biomeX, 6);
        long continentalZ = Math.floorDiv(biomeZ, 6);
        long regionalX = Math.floorDiv(biomeX, 2);
        long regionalZ = Math.floorDiv(biomeZ, 2);

        double continental = normalizedHash(continentalX, continentalZ, 0xA6C8_7E11_D7E9_194DL);
        double regional = normalizedHash(regionalX, regionalZ, 0x56B2_1B75_3CF5_6E4BL);
        return continental * 0.72D + regional * 0.28D;
    }

    private double normalizedHash(long x, long z, long salt) {
        long mixed = seed ^ salt;
        mixed ^= x * 0x9E37_79B9_7F4A_7C15L;
        mixed ^= z * 0xC2B2_AE3D_27D4_EB4FL;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D0_49BB_1331_11EBL;
        mixed = mixed ^ (mixed >>> 31);
        return (mixed >>> 11) * 0x1.0p-53;
    }
}
