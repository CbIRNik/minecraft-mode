package com.infdimmod.items.custom.portalgun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public record PortalGunCodeComponent(String portalcode) {

    public static final Codec<PortalGunCodeComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("pcode", "").forGetter(PortalGunCodeComponent::portalcode)
            ).apply(instance, PortalGunCodeComponent::new)
    );

    public static final ComponentType<PortalGunCodeComponent> PORTALCODETYPE =
            ComponentType.<PortalGunCodeComponent>builder()
                    .codec(CODEC)
                    .build();

    public static void register() {
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_code"),
                PORTALCODETYPE
        );
    }
}
