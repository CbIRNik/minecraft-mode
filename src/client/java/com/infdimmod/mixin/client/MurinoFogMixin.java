package com.infdimmod.mixin.client;

import com.infdimmod.burmaldeniya.BurmaldeniyaConfig;
import com.infdimmod.burmaldeniya.MurinoBiomeHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class MurinoFogMixin {
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void applyMurinoFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        Entity entity = camera.getFocusedEntity();
        if (entity != null && entity.getWorld() != null && MurinoBiomeHelper.isMurino(entity.getWorld(), entity.getBlockPos())) {
            float start = viewDistance * BurmaldeniyaConfig.Murino.FOG_START_FRACTION;
            float end = viewDistance * BurmaldeniyaConfig.Murino.FOG_END_FRACTION;
            RenderSystem.setShaderFogStart(start);
            RenderSystem.setShaderFogEnd(end);
        }
    }
}
