package com.infdimmod.items.custom.portalgun;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

    public static void register() {
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_code"),
                PORTALCODETYPE
        );
    }

    public void saveCodeToItem(PlayerEntity player, String newCode) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() instanceof PortalGun) {
            stack.set(PortalGunComponents.PORTALCODETYPE, new PortalGunComponents(newCode));
        }
    }


    //режим пушки
    public static final ComponentType<Boolean> PORTAL_GUN_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("infdimmod", "portal_gun_mode"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );
}
