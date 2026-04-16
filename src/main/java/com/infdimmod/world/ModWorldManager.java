package com.infdimmod.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ModWorldManager {

    public static void registerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.getGameRules().get(GameRules.SPAWN_CHUNK_RADIUS).set(0, server);

            Path dimensionsDir = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve("infdimmod");
            if (!Files.exists(dimensionsDir)) return;

            ServerWorld overworld = server.getOverworld();
            GameRules overworldRules = overworld.getGameRules();

            try (Stream<Path> paths = Files.list(dimensionsDir)) {
                paths.filter(Files::isDirectory).forEach(path -> {
                    String folderName = path.getFileName().toString();
                    if (folderName.startsWith("dim_")) {
                        try {
                            String seedStr = folderName.substring(4);
                            long seed = Long.parseLong(seedStr);
                            Identifier dimId = Identifier.of("infdimmod", folderName);

                            // ИЗВЛЕКАЕМ КОД ИЗ СИДА (2-й и 3-й символы)
                            String code = "00";
                            if (seedStr.length() >= 3) {
                                code = seedStr.substring(1, 3);
                            }

                            // ПОЛУЧАЕМ ПРАВИЛЬНЫЙ ГЕНЕРАТОР (кастомный или ванильный через наш Registry)
                            var generatorProvider = com.infdimmod.world.generator.DimTypeRegistry.get(code);
                            var chunkGenerator = generatorProvider.createGenerator(server, seed, server.getRegistryManager().getWrapperOrThrow(net.minecraft.registry.RegistryKeys.BIOME));

                            RuntimeWorldConfig config = new RuntimeWorldConfig()
                                    .setDimensionType(DimensionTypes.OVERWORLD)
                                    .setSeed(seed)
                                    .setGenerator(chunkGenerator); // Устанавливаем правильный генератор

                            // Копирование правил игры
                            copyGameRules(overworldRules, config);

                            // Fantasy откроет мир с ПРАВИЛЬНЫМ генератором, и позиция игрока сохранится
                            Fantasy.get(server).getOrOpenPersistentWorld(dimId, config);

                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    private static void copyGameRules(GameRules overworldRules, RuntimeWorldConfig config) {
        overworldRules.accept(new GameRules.Visitor() {
            @Override
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                if (key == GameRules.SPAWN_CHUNK_RADIUS) {
                    config.setGameRule(GameRules.SPAWN_CHUNK_RADIUS, 0);
                    return;
                }
                T rule = overworldRules.get(key);
                if (rule instanceof GameRules.BooleanRule boolRule) {
                    config.setGameRule((GameRules.Key<GameRules.BooleanRule>) key, boolRule.get());
                } else if (rule instanceof GameRules.IntRule intRule) {
                    config.setGameRule((GameRules.Key<GameRules.IntRule>) key, intRule.get());
                }
            }
        });
    }
}