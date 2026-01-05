package com.infdimmod.items.custom;

import com.infdimmod.items.ModItems;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;

public class PortalGunClient {
    public static void registerModelPredicates() {
        ModelPredicateProviderRegistry.register(
                ModItems.PortalGun,
                Identifier.of("infdimmod", "broken_state"),
                (stack, world, entity, seed) -> PortalGun.getBrokenState(stack)
        );
    }
}