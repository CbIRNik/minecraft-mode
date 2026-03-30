package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.BackPortalRenderer;
import com.infdimmod.Entities.GreenPortalRenderer;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.items.ModItems;
import com.infdimmod.Hud.PortalGunScreen;
import com.infdimmod.items.custom.portalgun.PortalGun;
import com.infdimmod.network.ToggleGunModePayload;
import com.infdimmod.particle.ModParticlesClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class InfDimModClient implements ClientModInitializer {

    private static KeyBinding openPortalGuiKey;
    private static KeyBinding toggleModeKey;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GREEN_PORTAL_ENTITY_TYPE, GreenPortalRenderer::new);
        EntityRendererRegistry.register(ModEntities.BACK_PORTAL_ENTITY_TYPE, BackPortalRenderer::new);

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
                // Если в руках пушка - отправляем пакет на сервер
                if (client.player != null && client.player.getMainHandStack().getItem() instanceof PortalGun) {
                    ClientPlayNetworking.send(new ToggleGunModePayload());
                }
            }
        });



        toggleModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.infdimmod.toggle_mode",
                InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_V,
                "category.infdimmod.controls"
        ));

        ModParticlesClient.registerParticleFactories();
    }

    private void handleOpenPortalGui(MinecraftClient client) {
        if (client.player == null) return;
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        if (main.getItem() == ModItems.PortalGun || off.getItem() == ModItems.PortalGun) {
            client.execute(() -> client.setScreen(new PortalGunScreen()));
        }
    }
}