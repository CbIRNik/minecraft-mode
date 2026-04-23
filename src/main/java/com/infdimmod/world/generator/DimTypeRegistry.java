package com.infdimmod.world.generator;

import com.infdimmod.world.generator.custom.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseRouter;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

import java.util.HashMap;
import java.util.Map;

public class DimTypeRegistry {
    private static final Map<String, DimGeneratorProvider> TYPES = new HashMap<>();

    static {
        register("тихо", (s, seed, l) -> new SpongeGenerator(getVanillaBiomes(s), seed));
        register("не спеша", (s, seed, l) -> new SphericalClustersGenerator(getVanillaBiomes(s), seed));
        register("не дыша", (s, seed, l) -> new TorusRingGenerator(getVanillaBiomes(s), seed));
        register("ни шиша", (s, seed, l) -> new LatticeGenerator(getVanillaBiomes(s), seed));
        register("4 карандаша", (s, seed, l) -> new DataFragmentsGenerator(getVanillaBiomes(s), seed));
        register("черемша", (s, seed, l) -> new AshWastelandGenerator(getVanillaBiomes(s), seed));
        register("с некой иронией", (s, seed, l) -> new ShatteredSavannaPlusGenerator(getVanillaBiomes(s), seed));
        register("с чувством что день прошел не зря", (s, seed, l) -> new EndStyleOverworldGenerator(getVanillaBiomes(s), seed));
    }

    private static BiomeSource getVanillaBiomes(MinecraftServer server) {
        var overworldGenerator = server.getOverworld().getChunkManager().getChunkGenerator();
        return overworldGenerator.getBiomeSource();
    }

    private static int getDigit(long seed, int pos) {
        long absoluteSeed = Math.abs(seed);
        return (int) ((absoluteSeed / (long) Math.pow(10, pos)) % 10);
    }

    private static ChunkGenerator createParameterizedVanilla(MinecraftServer server, long seed) {
        var registryManager = server.getRegistryManager();
        var settingsRegistry = registryManager.getWrapperOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
        var originalSettings = settingsRegistry.getOrThrow(net.minecraft.world.gen.chunk.ChunkGeneratorSettings.OVERWORLD).value();

        NoiseRouter originalRouter = originalSettings.noiseRouter();

        double erosionOffset = (getDigit(seed, 1) * 0.6) - 3.0;
        double tempOffset = (getDigit(seed, 3) * 0.05) - 0.2;
        double vegOffset = (getDigit(seed, 6) * 0.1) - 0.5;
        double barrierScale = (getDigit(seed, 11) * 0.5);
        double jaggedScale = (getDigit(seed, 15) * 0.2);
        double floodOffset = (getDigit(seed, 12) * 0.1);
        double fluidSpreadScale = 1.0 + (getDigit(seed, 13) * 0.2);
        double lavaScale = 1.0 + (getDigit(seed, 14) * 0.5);
        double ridgesScale = 1.0 + (getDigit(seed, 10) * 0.5);

        double veinFrequency = (getDigit(seed, 7) * 0.2) - 1.0;
        double veinThickness = 0.5 + (getDigit(seed, 8) * 0.5);
        double veinGap = (getDigit(seed, 9) * 0.1);



        NoiseRouter modifiedRouter = new NoiseRouter(
                DensityFunctionTypes.mul(originalRouter.barrierNoise(), DensityFunctionTypes.constant(barrierScale)),
                DensityFunctionTypes.add(originalRouter.fluidLevelFloodednessNoise(), DensityFunctionTypes.constant(floodOffset)),
                DensityFunctionTypes.mul(originalRouter.fluidLevelSpreadNoise(), DensityFunctionTypes.constant(fluidSpreadScale)),
                DensityFunctionTypes.mul(originalRouter.lavaNoise(), DensityFunctionTypes.constant(lavaScale)),
                DensityFunctionTypes.add(originalRouter.temperature(), DensityFunctionTypes.constant(tempOffset)),
                DensityFunctionTypes.add(originalRouter.vegetation(), DensityFunctionTypes.constant(vegOffset)),
                originalRouter.continents(),
                DensityFunctionTypes.add(originalRouter.erosion(), DensityFunctionTypes.constant(erosionOffset)),
                originalRouter.depth(),
                DensityFunctionTypes.mul(originalRouter.ridges(), DensityFunctionTypes.constant(ridgesScale)),
                DensityFunctionTypes.mul(originalRouter.initialDensityWithoutJaggedness(), DensityFunctionTypes.constant(jaggedScale)),
                originalRouter.finalDensity(),
                DensityFunctionTypes.add(originalRouter.veinToggle(), DensityFunctionTypes.constant(veinFrequency)),
                DensityFunctionTypes.mul(originalRouter.veinRidged(), DensityFunctionTypes.constant(veinThickness)),
                DensityFunctionTypes.add(originalRouter.veinGap(), DensityFunctionTypes.constant(veinGap))
        );

        var modifiedSettings = new net.minecraft.world.gen.chunk.ChunkGeneratorSettings(
                originalSettings.generationShapeConfig(),
                originalSettings.defaultBlock(),
                originalSettings.defaultFluid(),
                modifiedRouter,
                originalSettings.surfaceRule(),
                originalSettings.spawnTarget(),
                originalSettings.seaLevel(),
                originalSettings.mobGenerationDisabled(),
                originalSettings.hasAquifers(),
                true,
                originalSettings.usesLegacyRandom()
        );

        return new net.minecraft.world.gen.chunk.NoiseChunkGenerator(
                getVanillaBiomes(server),
                net.minecraft.registry.entry.RegistryEntry.of(modifiedSettings)
        );
    }

    public static void register(String code, DimGeneratorProvider provider) {
        TYPES.put(code, provider);
    }

    public static DimGeneratorProvider get(String code) {
        if (TYPES.containsKey(code)) {
            return TYPES.get(code);
        }

        return (server, seed, lookup) -> createParameterizedVanilla(server, seed);
    }
}