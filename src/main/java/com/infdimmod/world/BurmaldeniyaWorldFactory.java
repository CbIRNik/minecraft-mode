package com.infdimmod.world;

import com.infdimmod.burmaldeniya.MurinoWorldgenHooks;
import com.infdimmod.world.generator.DeterministicChaosGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public final class BurmaldeniyaWorldFactory {
    private BurmaldeniyaWorldFactory() {
    }

    public static Identifier createDimensionId(long seed) {
        return Identifier.of("infdimmod", "dim_" + seed);
    }

    public static ServerWorld getOrCreateWorld(MinecraftServer server, long seed) {
        return Fantasy.get(server).getOrOpenPersistentWorld(createDimensionId(seed), createConfig(server, seed)).asWorld();
    }

    public static RuntimeWorldConfig createConfig(MinecraftServer server, long seed) {
        ServerWorld overworld = server.getOverworld();
        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setSeed(seed)
                .setGenerator(new DeterministicChaosGenerator(MurinoWorldgenHooks.createBiomeSource(overworld), seed));
        copyGameRules(overworld.getGameRules(), config);
        return config;
    }

    private static void copyGameRules(GameRules source, RuntimeWorldConfig config) {
        source.accept(new GameRules.Visitor() {
            @Override
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                if (key == GameRules.SPAWN_CHUNK_RADIUS) {
                    config.setGameRule(GameRules.SPAWN_CHUNK_RADIUS, 0);
                    return;
                }

                T rule = source.get(key);
                if (rule instanceof GameRules.BooleanRule boolRule) {
                    config.setGameRule((GameRules.Key<GameRules.BooleanRule>) key, boolRule.get());
                } else if (rule instanceof GameRules.IntRule intRule) {
                    config.setGameRule((GameRules.Key<GameRules.IntRule>) key, intRule.get());
                }
            }
        });
    }
}
