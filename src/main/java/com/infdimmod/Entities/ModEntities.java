package com.infdimmod.Entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    // Регистрация самой сущности
    public static final EntityType<GreenPortal> GREEN_PORTAL_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("infdimmod", "green_portal"),
            EntityType.Builder.create(GreenPortal::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f) // Размеры хитбокса (даже если модель плоская)
                    .build()
    );

    public static void registerModEntities() {}
}