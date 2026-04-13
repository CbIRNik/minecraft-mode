package com.infdimmod.burmaldeniya;

public final class BurmaldeniyaConfig {
    private BurmaldeniyaConfig() {
    }

    public static final class Murino {
        public static final double BIOME_CHANCE = 0.24D;
        public static final long FIXED_TIME_OF_DAY = 12_600L;
        public static final float FOG_START_FRACTION = 0.05F;
        public static final float FOG_END_FRACTION = 0.45F;

        private Murino() {
        }
    }

    public static final class Dormitory {
        public static final int FLOOR_HEIGHT = 3;
        public static final int MURINO_GRID_SPACING_CHUNKS = 4;
        public static final int OTHER_GRID_SPACING_CHUNKS = 10;
        public static final double MURINO_THRESHOLD = -0.55D;
        public static final double OTHER_THRESHOLD = 0.2D;

        private Dormitory() {
        }
    }

    public static final class Collider {
        public static final int GRID_SPACING_CHUNKS = 40;
        public static final int COMPLEX_RADIUS_BLOCKS = 52;
        public static final int COMPLEX_HALF_HEIGHT = 22;
        public static final int ACTIVE_SEARCH_RANGE_CHUNKS = 9;
        public static final int RADIATION_RADIUS = 24;
        public static final int GUARD_RADIUS = 20;
        public static final int TARGET_GUARD_COUNT = 4;
        public static final int OVERHEAT_THRESHOLD = 1_600;
        public static final int OVERHEAT_PER_NEARBY_TARGET = 4;
        public static final int PASSIVE_COOLDOWN_PER_TICK = 3;
        public static final int REDSTONE_OVERHEAT_BONUS = 12;
        public static final int STUDENT_ABSORB_COOLDOWN = 400;
        public static final int MELTDOWN_RADIUS_BLOCKS = 100;
        public static final int MELTDOWN_BLOCKS_PER_TICK = 1_024;

        private Collider() {
        }
    }
}
