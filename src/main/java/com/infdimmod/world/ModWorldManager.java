package com.infdimmod.world;

import com.infdimmod.InfDimMod;
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
                            long seed = Long.parseLong(folderName.substring(4));
                            Identifier dimId = Identifier.of("infdimmod", folderName);

                            RuntimeWorldConfig config = new RuntimeWorldConfig()
                                    .setDimensionType(DimensionTypes.OVERWORLD)
                                    .setSeed(seed)
                                    .setGenerator(overworld.getChunkManager().getChunkGenerator());

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

                            Fantasy.get(server).getOrOpenPersistentWorld(dimId, config);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
            } catch (Exception e) {
                InfDimMod.LOGGER.error("Failed to restore saved dimensions from {}", dimensionsDir, e);
            }
        });
    }
}
