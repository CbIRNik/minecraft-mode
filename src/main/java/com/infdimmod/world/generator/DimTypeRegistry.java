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
        register("антон", (s, seed, l) -> new SpongeGenerator(getVanillaBiomes(s), seed));
        register("чигур", (s, seed, l) -> new SphericalClustersGenerator(getVanillaBiomes(s), seed));
        register("никого", (s, seed, l) -> new TorusRingGenerator(getVanillaBiomes(s), seed));
        register("не", (s, seed, l) -> new LatticeGenerator(getVanillaBiomes(s), seed));
        register("убивал", (s, seed, l) -> new DataFragmentsGenerator(getVanillaBiomes(s), seed));
        register("всем", (s, seed, l) -> new AshWastelandGenerator(getVanillaBiomes(s), seed));
        register("людям", (s, seed, l) -> new ShatteredSavannaPlusGenerator(getVanillaBiomes(s), seed));
        register("помогал", (s, seed, l) -> new EndStyleOverworldGenerator(getVanillaBiomes(s), seed));
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

        double vScale = 0.1 + (getDigit(seed, 0) * 0.4);
        double erosionOffset = (getDigit(seed, 1) * 0.6) - 3.0;
        double depthOffset = (getDigit(seed, 2) * 0.4) - 1.5;

        double veinFrequency = (getDigit(seed, 7) * 0.2) - 1.0;
        double veinThickness = 0.5 + (getDigit(seed, 8) * 0.5);
        double veinGap = (getDigit(seed, 9) * 0.1);

        int seaLevel = 58 + (getDigit(seed, 4));

        NoiseRouter originalRouter = originalSettings.noiseRouter();

        NoiseRouter modifiedRouter = new NoiseRouter(
                originalRouter.barrierNoise(),
                originalRouter.fluidLevelFloodednessNoise(),
                originalRouter.fluidLevelSpreadNoise(),
                originalRouter.lavaNoise(),
                originalRouter.temperature(),
                originalRouter.vegetation(),
                originalRouter.continents(),
                DensityFunctionTypes.add(originalRouter.erosion(), DensityFunctionTypes.constant(erosionOffset)),
                DensityFunctionTypes.add(originalRouter.depth(), DensityFunctionTypes.constant(depthOffset)),
                originalRouter.ridges(),
                originalRouter.initialDensityWithoutJaggedness(),
                DensityFunctionTypes.mul(originalRouter.finalDensity(), DensityFunctionTypes.constant(vScale)),
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
                seaLevel,
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