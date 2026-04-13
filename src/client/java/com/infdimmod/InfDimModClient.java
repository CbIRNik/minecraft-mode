package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.BackPortalRenderer;
import com.infdimmod.Entities.DrunnyParticleOrbitRenderer;
import com.infdimmod.Entities.GreenPortalRenderer;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.Hud.PortalGunCrafterScreen;
import com.infdimmod.items.ModItems;
import com.infdimmod.Hud.PortalGunScreen;
import com.infdimmod.burmaldeniya.BurmaldeniyaConstants;
import com.infdimmod.items.custom.portalgun.PortalGun;
import com.infdimmod.network.ToggleGunModePayload;
import com.infdimmod.particle.ModParticlesClient;
import com.infdimmod.sound.ModSounds;
import com.infdimmod.util.PortalGunCrafterScreenHandler;
import com.infdimmod.world.BurmaldeniyaWorldFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static com.infdimmod.items.custom.portalgun.PortalGun.getBrokenState;

public class InfDimModClient implements ClientModInitializer {

    private static KeyBinding openPortalGuiKey;
    private static KeyBinding toggleModeKey;
    private static int burmaldeniyaMusicCooldownTicks = 0;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GREEN_PORTAL_ENTITY_TYPE, GreenPortalRenderer::new);
        EntityRendererRegistry.register(ModEntities.BACK_PORTAL_ENTITY_TYPE, BackPortalRenderer::new);
        EntityRendererRegistry.register(ModEntities.RESIDENT_ENTITY_TYPE, VillagerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FOGI_ENTITY_TYPE, ZombieEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.DRUN_GUARD_ENTITY_TYPE, ZombieEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.DRUNNY_PARTICLE_ORBIT_ENTITY_TYPE, DrunnyParticleOrbitRenderer::new);

        ModelPredicateProviderRegistry.register(
                ModItems.PortalGun,
                Identifier.of("infdimmod", "broken_state"),
                (stack, world, entity, seed) -> {
                    return getBrokenState(stack);
                }
        );

        Identifier stillTextureId = Identifier.of(InfDimMod.MOD_ID, "block/portal_fluid");
        Identifier flowingTextureId = Identifier.of(InfDimMod.MOD_ID, "block/portal_fluid_flowing");
        FluidRenderHandlerRegistry.INSTANCE.register(
                ModBlocks.STILL_PORTAL_FLUID,
                ModBlocks.FLOWING_PORTAL_FLUID,
                new SimpleFluidRenderHandler(
                        stillTextureId,
                        flowingTextureId
                )
        );

        openPortalGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.infdimmod.open_portal_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.infdimmod.controls"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPortalGuiKey.wasPressed()) {
                if (client.player != null && client.player.getMainHandStack().getItem() instanceof PortalGun) {
                    handleOpenPortalGui(client);
                }
            }

            while (toggleModeKey.wasPressed()) {
                if (client.player != null && client.player.getMainHandStack().getItem() instanceof PortalGun) {
                    ClientPlayNetworking.send(new ToggleGunModePayload());
                }
            }

            tickBurmaldeniyaMusic(client);
        });

        toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.infdimmod.toggle_mode",
                InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_V,
                "category.infdimmod.controls"
        ));

        ModParticlesClient.registerParticleFactories();

        HandledScreens.register(InfDimMod.PORTAL_GUN_CRAFTER_SH, PortalGunCrafterScreen::new);
    }

    private void handleOpenPortalGui(MinecraftClient client) {
        if (client.player == null) return;
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        if (main.getItem() == ModItems.PortalGun || off.getItem() == ModItems.PortalGun) {
            client.execute(() -> client.setScreen(new PortalGunScreen()));
        }
    }

    private static void tickBurmaldeniyaMusic(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            burmaldeniyaMusicCooldownTicks = 0;
            return;
        }

        Identifier burmaldeniyaId = BurmaldeniyaWorldFactory.burmaldeniyaDimensionId();
        if (!client.world.getRegistryKey().getValue().equals(burmaldeniyaId)) {
            burmaldeniyaMusicCooldownTicks = 0;
            return;
        }

        if (burmaldeniyaMusicCooldownTicks > 0) {
            burmaldeniyaMusicCooldownTicks--;
            return;
        }

        client.world.playSound(
                client.player,
                client.player.getBlockPos(),
                ModSounds.BURMALDENIYA_AMBIENT_MUSIC,
                net.minecraft.sound.SoundCategory.MUSIC,
                0.5F,
                1.0F
        );
        burmaldeniyaMusicCooldownTicks = BurmaldeniyaConstants.TICKS_PER_SECOND * 90;
    }
}
