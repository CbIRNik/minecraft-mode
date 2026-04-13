package com.infdimmod.burmaldeniya;

public final class BurmaldeniyaConstants {
    public static final String BURMALDOT_NAMESPACE = "burmaldot";
    public static final String BURMALDENIYA_NAMESPACE = "burmaldeniya";
    public static final String BURMALDENIYA_DIMENSION_ID = "burmaldeniya";
    public static final String BURMALDENIYA_ROUTE_CODE = BURMALDENIYA_NAMESPACE + ":" + BURMALDENIYA_DIMENSION_ID;

    public static final String BURMALDUSHKA_ITEM_ID = "burmaldushka";
    public static final String DRUNNY_COLLIDER_BLOCK_ID = "drunny_collider";
    public static final String RESIDENT_ENTITY_ID = "resident";
    public static final String FOGI_ENTITY_ID = "fogi";
    public static final String DORMITORY_STRUCTURE_ID = "dormitory";

    public static final String BURMALDENIYA_AMBIENT_MUSIC_ID = "burmaldeniya_ambient_music";
    public static final String DRUNNY_COLLIDER_AMBIENT_SOUND_ID = "drunny_collider_ambient";

    public static final int TICKS_PER_SECOND = 20;
    public static final int TICKS_PER_DAY = 24_000;
    public static final int BURMALDENIYA_ROUTE_TICK_STEP = 15 * TICKS_PER_SECOND;

    private BurmaldeniyaConstants() {
    }
}
