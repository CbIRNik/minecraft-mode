package com.infdimmod.burmaldeniya;

import com.infdimmod.world.BurmaldeniyaWorldFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MurinoAtmosphereManager {
    private static final Map<Identifier, WeatherCycleState> WEATHER_STATES = new HashMap<>();

    private MurinoAtmosphereManager() {
    }

    public static void tick(MinecraftServer server) {
        Set<Identifier> activeWorldIds = new HashSet<>();
        for (ServerWorld world : server.getWorlds()) {
            if (!isBurmaldeniyaManagedWorld(world)) {
                continue;
            }

            Identifier worldId = world.getRegistryKey().getValue();
            activeWorldIds.add(worldId);

            if (world.getTimeOfDay() != BurmaldeniyaConfig.Murino.FIXED_TIME_OF_DAY) {
                world.setTimeOfDay(BurmaldeniyaConfig.Murino.FIXED_TIME_OF_DAY);
            }

            WeatherCycleState state = WEATHER_STATES.computeIfAbsent(worldId, ignored -> WeatherCycleState.createInitial(world));
            state.update(world);

            if (!state.matches(world)) {
                state.apply(world);
            }
        }
        WEATHER_STATES.keySet().removeIf(worldId -> !activeWorldIds.contains(worldId));
    }

    private static boolean isBurmaldeniyaManagedWorld(ServerWorld world) {
        return world.getRegistryKey().getValue().equals(BurmaldeniyaWorldFactory.burmaldeniyaDimensionId());
    }

    private enum WeatherMode {
        CLEAR,
        RAIN,
        STORM
    }

    private static final class WeatherCycleState {
        private WeatherMode mode;
        private boolean highStormWindow;
        private long nextModeSwitchTick;
        private long nextWindowShiftTick;

        private WeatherCycleState(WeatherMode mode, boolean highStormWindow, long nextModeSwitchTick, long nextWindowShiftTick) {
            this.mode = mode;
            this.highStormWindow = highStormWindow;
            this.nextModeSwitchTick = nextModeSwitchTick;
            this.nextWindowShiftTick = nextWindowShiftTick;
        }

        private static WeatherCycleState createInitial(ServerWorld world) {
            long now = world.getTime();
            WeatherCycleState state = new WeatherCycleState(
                    WeatherMode.STORM,
                    true,
                    now,
                    now + randomBetween(world, 6 * 60 * BurmaldeniyaConstants.TICKS_PER_SECOND, 10 * 60 * BurmaldeniyaConstants.TICKS_PER_SECOND)
            );
            state.update(world);
            state.apply(world);
            return state;
        }

        private void update(ServerWorld world) {
            long now = world.getTime();
            if (now >= nextWindowShiftTick) {
                highStormWindow = !highStormWindow;
                nextWindowShiftTick = now + windowDuration(world, highStormWindow);
            }
            if (now >= nextModeSwitchTick) {
                mode = rollMode(world, highStormWindow);
                nextModeSwitchTick = now + modeDuration(world, mode);
            }
        }

        private boolean matches(ServerWorld world) {
            return switch (mode) {
                case CLEAR -> !world.isRaining() && !world.isThundering();
                case RAIN -> world.isRaining() && !world.isThundering();
                case STORM -> world.isRaining() && world.isThundering();
            };
        }

        private void apply(ServerWorld world) {
            int duration = Math.max(1, (int) Math.min(Integer.MAX_VALUE, nextModeSwitchTick - world.getTime()));
            switch (mode) {
                case CLEAR -> world.setWeather(duration, 0, false, false);
                case RAIN -> world.setWeather(0, duration, true, false);
                case STORM -> world.setWeather(0, duration, true, true);
            }
        }

        private static long windowDuration(ServerWorld world, boolean highStormWindow) {
            if (highStormWindow) {
                return randomBetween(world, 6 * 60 * BurmaldeniyaConstants.TICKS_PER_SECOND, 10 * 60 * BurmaldeniyaConstants.TICKS_PER_SECOND);
            }
            return randomBetween(world, 3 * 60 * BurmaldeniyaConstants.TICKS_PER_SECOND, 6 * 60 * BurmaldeniyaConstants.TICKS_PER_SECOND);
        }

        private static WeatherMode rollMode(ServerWorld world, boolean highStormWindow) {
            int roll = randomBetween(world, 1, 100);
            if (highStormWindow) {
                if (roll <= 75) {
                    return WeatherMode.STORM;
                }
                if (roll <= 97) {
                    return WeatherMode.RAIN;
                }
                return WeatherMode.CLEAR;
            }

            if (roll <= 35) {
                return WeatherMode.STORM;
            }
            if (roll <= 85) {
                return WeatherMode.RAIN;
            }
            return WeatherMode.CLEAR;
        }

        private static long modeDuration(ServerWorld world, WeatherMode mode) {
            return switch (mode) {
                case STORM -> randomBetween(world, 90 * BurmaldeniyaConstants.TICKS_PER_SECOND, 210 * BurmaldeniyaConstants.TICKS_PER_SECOND);
                case RAIN -> randomBetween(world, 60 * BurmaldeniyaConstants.TICKS_PER_SECOND, 180 * BurmaldeniyaConstants.TICKS_PER_SECOND);
                case CLEAR -> randomBetween(world, 30 * BurmaldeniyaConstants.TICKS_PER_SECOND, 90 * BurmaldeniyaConstants.TICKS_PER_SECOND);
            };
        }

        private static int randomBetween(ServerWorld world, int minInclusive, int maxInclusive) {
            return minInclusive + world.getRandom().nextInt(maxInclusive - minInclusive + 1);
        }
    }
}
