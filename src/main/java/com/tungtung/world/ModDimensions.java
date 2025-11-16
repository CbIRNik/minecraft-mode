package com.tungtung.world;

import com.tungtung.Tungtungmod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ModDimensions {
    public static final RegistryKey<World> MYSTIC_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(Tungtungmod.MOD_ID, "mystic_world")
    );

    public static final RegistryKey<DimensionType> MYSTIC_DIMENSION_TYPE = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            Identifier.of(Tungtungmod.MOD_ID, "mystic_world")
    );

    public static void initialize() {
        Tungtungmod.LOGGER.info("Registering Mystic Dimension");
    }
}
