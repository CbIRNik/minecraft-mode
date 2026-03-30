package com.infdimmod.Entities;

import com.infdimmod.Entities.custom.BackPortal;
import com.infdimmod.Entities.custom.GreenPortal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<GreenPortal> GREEN_PORTAL_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("infdimmod", "green_portal"),
            EntityType.Builder.create(GreenPortal::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f) // хитбокс
                    .build()
    );

    public static final EntityType<BackPortal> BACK_PORTAL_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("infdimmod", "back_portal"),
            EntityType.Builder.create(BackPortal::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .build()
    );

    public static void registerModEntities() {}
}