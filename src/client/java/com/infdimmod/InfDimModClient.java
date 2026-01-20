package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.items.ModItems;
import com.infdimmod.items.custom.PortalGunScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class InfDimModClient implements ClientModInitializer {

    private static KeyBinding openPortalGuiKey;

    @Override
    public void onInitializeClient() {

        Identifier stillTextureId = Identifier.of(InfDimMod.MOD_ID, "block/portal_fluid");
        Identifier flowingTextureId = Identifier.of(InfDimMod.MOD_ID, "block/portal_fluid_flowing");
        // Регистрация рендера жидкости
        FluidRenderHandlerRegistry.INSTANCE.register(
                ModBlocks.STILL_PORTAL_FLUID,
                ModBlocks.FLOWING_PORTAL_FLUID,
                new SimpleFluidRenderHandler(
                        stillTextureId,
                        flowingTextureId
                )
        );


        // Register keybinding (default B)
        openPortalGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.infdimmod.open_portal_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.infdimmod.controls"
        ));

        // Listen for client ticks to check key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPortalGuiKey.wasPressed()) {
                handleOpenPortalGui(client);
            }
        });
    }

    private void handleOpenPortalGui(MinecraftClient client) {
        if (client.player == null) return;
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        // Open only if player holds PortalGun in either hand
        if (main.getItem() == ModItems.PortalGun || off.getItem() == ModItems.PortalGun) {
            client.execute(() -> client.setScreen(new PortalGunScreen()));
        }
    }
}