package com.tungtung.world;

import com.tungtung.InfDimMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ModDimensions {
    public static final RegistryKey<World> MYSTIC_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(com.tungtung.InfDimMod.MOD_ID, "mystic_world")
    );

    public static final RegistryKey<DimensionType> MYSTIC_DIMENSION_TYPE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            Identifier.of(com.tungtung.InfDimMod.MOD_ID, "mystic_world")
    );

    public static void initialize() {
        InfDimMod.LOGGER.info("Registering Mystic Dimension");
    }
}
