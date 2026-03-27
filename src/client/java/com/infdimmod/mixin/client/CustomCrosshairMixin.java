package com.infdimmod.mixin.client;

import com.infdimmod.items.custom.portalgun.PortalGun;import com.infdimmod.items.custom.portalgun.PortalGunCodeComponent;import com.mojang.blaze3d.platform.GlStateManager;import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class CustomCrosshairMixin {

    private static final Identifier MODE_1_CROSSHAIR = Identifier.of("infdimmod", "textures/gui/crosshair_mode_1.png");
    private static final Identifier MODE_2_CROSSHAIR = Identifier.of("infdimmod", "textures/gui/crosshair_mode_2.png");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCustomCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) return;

        if (client.options.hudHidden || !client.options.getPerspective().isFirstPerson()) {
            return;
        }

        ItemStack stack = client.player.getMainHandStack();

        if (stack.getItem() instanceof PortalGun) {
            boolean isMode2 = stack.getOrDefault(PortalGunCodeComponent.PORTAL_GUN_MODE, false);
            Identifier texture = isMode2 ? MODE_2_CROSSHAIR : MODE_1_CROSSHAIR;

            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();

            int x = (width - 16) / 2;
            int y = (height - 16) / 2;

            RenderSystem.enableBlend();


            RenderSystem.blendFunc(
                    GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                    GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR
            );

            context.drawTexture(texture, x, y, 0, 0, 16, 16, 16, 16);

            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            ci.cancel();
        }
    }
}