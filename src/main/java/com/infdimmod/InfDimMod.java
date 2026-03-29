package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.items.ModItems;
import com.infdimmod.items.custom.portalgun.PortalGun;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import com.infdimmod.network.PortalCodePayload;
import com.infdimmod.network.ToggleGunModePayload;
import com.infdimmod.particle.ModParticles;
import com.infdimmod.world.ModDimensions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.infdimmod.items.custom.portalgun.PortalGunComponents.PORTAL_GUN_MODE;

public class InfDimMod implements ModInitializer {
	public static final String MOD_ID = "infdimmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
        ModDimensions.initialize();

        // Регистрируем пакетик
        PayloadTypeRegistry.playC2S().register(PortalCodePayload.ID, PortalCodePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PortalCodePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();


                stack.set(PortalGunComponents.PORTALCODETYPE, new PortalGunComponents(payload.code()));

            });
        });

        PortalGunComponents.register();

        ModEntities.registerModEntities();

        // пакеты режимов пушки
        PayloadTypeRegistry.playC2S().register(ToggleGunModePayload.ID, ToggleGunModePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ToggleGunModePayload.ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof PortalGun) {
                    boolean currentMode = stack.getOrDefault(PORTAL_GUN_MODE, false);
                    stack.set(PORTAL_GUN_MODE, !currentMode);
                }
            });
        });

        ModParticles.register();
	}
}