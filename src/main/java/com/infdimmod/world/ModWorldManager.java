package com.infdimmod.world;

import com.infdimmod.InfDimMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ModWorldManager {

    public static void registerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.getGameRules().get(GameRules.SPAWN_CHUNK_RADIUS).set(0, server);

            Path dimensionsDir = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve("infdimmod");
            if (!Files.exists(dimensionsDir)) return;

            try (Stream<Path> paths = Files.list(dimensionsDir)) {
                paths.filter(Files::isDirectory).forEach(path -> {
                    String folderName = path.getFileName().toString();
                    if (folderName.startsWith("dim_")) {
                        try {
                            long seed = Long.parseLong(folderName.substring(4));
                            BurmaldeniyaWorldFactory.getOrCreateWorld(server, seed);
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
