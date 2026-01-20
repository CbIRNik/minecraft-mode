package com.infdimmod.items.custom;

import com.infdimmod.items.custom.portalgun.PortalGun;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class PortalGunHudRenderer {
    private static String currentCode = "";

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.getMainHandStack().getItem() instanceof PortalGun) {
                renderCodeIndicator(drawContext, client);
            }
        });
    }

    private static void renderCodeIndicator(DrawContext context, MinecraftClient client) {
        // Отображаем код в левом верхнем углу экрана
        String displayText = "Portal Code: " + (currentCode.isEmpty() ? "None" : currentCode);
        context.drawText(client.textRenderer, displayText, 10, 10, 0xFFFFFF, false);
    }

    public static void setCurrentCode(String code) {
        currentCode = code;
    }

    public static String getCurrentCode() {
        return currentCode;
    }
}

