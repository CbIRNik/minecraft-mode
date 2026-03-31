package com.infdimmod.items.custom.portalgun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public record PortalGunComponents(String portalcode) {
    // код измерения
    public static final Codec<PortalGunComponents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("pcode", "").forGetter(PortalGunComponents::portalcode)
            ).apply(instance, PortalGunComponents::new)
    );

    public static final ComponentType<PortalGunComponents> PORTALCODETYPE =
            ComponentType.<PortalGunComponents>builder()
                    .codec(CODEC)
                    .build();
    //координаты
    public record PortalCoords(double x, double y, double z) {
        public static final Codec<PortalCoords> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.fieldOf("x").forGetter(PortalCoords::x),
                        Codec.DOUBLE.fieldOf("y").forGetter(PortalCoords::y),
                        Codec.DOUBLE.fieldOf("z").forGetter(PortalCoords::z)
                ).apply(instance, PortalCoords::new)
        );
    }
    public static final ComponentType<PortalCoords> PORTAL_COORDS =
            ComponentType.<PortalCoords>builder()
                    .codec(PortalCoords.CODEC)
                    .build();



    public static void register() {
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_code"),
                PORTALCODETYPE
        );
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_coords"),
                PORTAL_COORDS
        );
    }

    //режим пушки
    public static final ComponentType<Boolean> PORTAL_GUN_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("infdimmod", "portal_gun_mode"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );
}
