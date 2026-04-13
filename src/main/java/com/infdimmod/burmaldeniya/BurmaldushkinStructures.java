package com.infdimmod.burmaldeniya;

import com.infdimmod.InfDimMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

public class BurmaldushkinStructures {
    public static final RegistryKey<Structure> DORMITORY_5 = RegistryKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(InfDimMod.MOD_ID, "dormitory_5")
    );

    public static final RegistryKey<Structure> DORMITORY_9 = RegistryKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(InfDimMod.MOD_ID, "dormitory_9")
    );
}
