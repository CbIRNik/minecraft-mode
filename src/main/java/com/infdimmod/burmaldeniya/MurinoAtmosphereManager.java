package com.infdimmod.burmaldeniya;

import com.infdimmod.InfDimMod;
import com.infdimmod.world.BurmaldeniyaWorldFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class MurinoAtmosphereManager {
    private static final long DUSK_TIME = 12_600L;

    private MurinoAtmosphereManager() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (!isBurmaldeniyaManagedWorld(world)) {
                continue;
            }

            if (world.getTimeOfDay() != DUSK_TIME) {
                world.setTimeOfDay(DUSK_TIME);
            }

            if (!world.isRaining() || !world.isThundering()) {
                world.setWeather(0, Integer.MAX_VALUE, true, true);
            }
        }
    }

    private static boolean isBurmaldeniyaManagedWorld(ServerWorld world) {
        Identifier worldId = world.getRegistryKey().getValue();
        if (worldId.equals(BurmaldeniyaWorldFactory.burmaldeniyaDimensionId())) {
            return true;
        }
        return worldId.getNamespace().equals(InfDimMod.MOD_ID) && worldId.getPath().startsWith("dim_");
    }
}
