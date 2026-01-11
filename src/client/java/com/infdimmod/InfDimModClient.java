package com.infdimmod;

import com.infdimmod.items.ModItems;
import com.infdimmod.items.custom.PortalGunClient;
import com.infdimmod.items.custom.PortalGunScreen;
import com.infdimmod.items.custom.PortalGunHudRenderer;
import com.infdimmod.network.ClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class InfDimModClient implements ClientModInitializer {

    private static KeyBinding openPortalGuiKey;

    @Override
    public void onInitializeClient() {
        PortalGunClient.registerModelPredicates();
        PortalGunHudRenderer.register();
        ClientNetworking.register();

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