package com.infdimmod.world;

import com.infdimmod.world.generator.DimTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ModWorldManager {

    public static long getSeedFromCode(String code) {
        if (code == null || code.isEmpty()) return 1_000_000_000_000_000_000L;
        if (code.length() < 12) {
            code = String.format("%12s", code).replace(' ', '0');
        } else if (code.length() > 12) {
            code = code.substring(0, 12);
        }
        long hash = 0;
        for (int i = 0; i < code.length(); i++) {
            hash = 63L * hash + code.charAt(i);
        }
        long base = 1_000_000_000_000_000_000L;
        long range = 8_223_372_036_854_775_807L;
        return base + (Math.abs(hash) % range);
    }

    public static void registerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path dimensionsDir = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve("infdimmod");
            if (!Files.exists(dimensionsDir)) return;

            Fantasy fantasy = Fantasy.get(server);
            var registryManager = server.getRegistryManager();
            var biomeRegistry = registryManager.getWrapperOrThrow(RegistryKeys.BIOME);

            try (Stream<Path> paths = Files.list(dimensionsDir)) {
                paths.filter(Files::isDirectory).forEach(path -> {
                    String folderName = path.getFileName().toString();
                    if (!folderName.startsWith("dim_")) return;

                    String fullCode = folderName.replace("dim_", "");

                    long seed = getSeedFromCode(fullCode);

                    Identifier dimId = Identifier.of("infdimmod", folderName);

                    var generatorProvider = DimTypeRegistry.get(fullCode);
                    var chunkGenerator = generatorProvider.createGenerator(server, seed, biomeRegistry);

                    RuntimeWorldConfig config = new RuntimeWorldConfig()
                            .setDimensionType(DimensionTypes.OVERWORLD)
                            .setSeed(seed)
                            .setGenerator(chunkGenerator)
                            .setShouldTickTime(true);

                    fantasy.getOrOpenPersistentWorld(dimId, config);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}