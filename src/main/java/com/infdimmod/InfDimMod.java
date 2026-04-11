package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.items.ModItems;
import com.infdimmod.items.custom.portalgun.PortalGun;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import com.infdimmod.network.PortalCodePayload;
import com.infdimmod.network.PortalCoordsPayload;
import com.infdimmod.network.ToggleGunModePayload;
import com.infdimmod.network.UpdatePortalHistoryPayload;
import com.infdimmod.network.UpdatePortalFavoritesPayload;
import com.infdimmod.particle.ModParticles;
import com.infdimmod.recipe.PortalGunRecipe;
import com.infdimmod.util.PortalGunCrafterScreenHandler;
import com.infdimmod.world.ModWorldManager;
import com.infdimmod.world.generator.DeterministicChaosGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.infdimmod.items.custom.portalgun.PortalGunComponents.PORTAL_GUN_MODE;

public class InfDimMod implements ModInitializer {
	public static final String MOD_ID = "infdimmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ScreenHandlerType<PortalGunCrafterScreenHandler> PORTAL_GUN_CRAFTER_SH = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MOD_ID, "portal_gun_crafter"),
            new ScreenHandlerType<>(PortalGunCrafterScreenHandler::new, FeatureSet.empty())
    );

    public static final RecipeType<PortalGunRecipe> PORTAL_RECIPE_TYPE =
            Registry.register(Registries.RECIPE_TYPE, Identifier.of(MOD_ID, "portal_gun_crafting"), PortalGunRecipe.Type.INSTANCE);
    public static final RecipeSerializer<PortalGunRecipe> PORTAL_RECIPE_SERIALIZER =
            Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(MOD_ID, "portal_gun_crafting"), PortalGunRecipe.Serializer.INSTANCE);

	@Override
	public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();

        Registry.register(Registries.CHUNK_GENERATOR,
                Identifier.of("infdimmod", "deterministic_chaos"),
                DeterministicChaosGenerator.CODEC);

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

        // пакет режимов пушки
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

        //пакет координат
        // Регистрируем тип пакета (Client to Server)
        PayloadTypeRegistry.playC2S().register(PortalCoordsPayload.ID, PortalCoordsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PortalCoordsPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                var stack = player.getMainHandStack();

                if (stack.getItem() instanceof PortalGun) {
                    PortalGun.setTargetCoords(stack, payload.x(), payload.y(), payload.z());
                }
            });
        });
        ModParticles.register();

        ModWorldManager.registerLifecycleEvents();

        PayloadTypeRegistry.playC2S().register(UpdatePortalHistoryPayload.ID, UpdatePortalHistoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdatePortalFavoritesPayload.ID, UpdatePortalFavoritesPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UpdatePortalHistoryPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof PortalGun) {
                    stack.set(PortalGunComponents.PORTAL_HISTORY, payload.history());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdatePortalFavoritesPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ItemStack stack = context.player().getMainHandStack();
                if (stack.getItem() instanceof PortalGun) {
                    stack.set(PortalGunComponents.PORTAL_FAVORITES, payload.favorites());
                }
            });
        });
	}
}