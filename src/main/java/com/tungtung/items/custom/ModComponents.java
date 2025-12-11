package com.tungtung.items.custom;

import com.tungtung.InfDimMod;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {

    public static final ComponentType<Integer> MY_COMPONENT_TYPE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "component"),
            ComponentType.<Integer>builder().codec(null).build()
    );




    protected static void initialize() {
    }
}