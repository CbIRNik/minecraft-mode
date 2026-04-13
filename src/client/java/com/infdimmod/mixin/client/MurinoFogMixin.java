package com.infdimmod.mixin.client;

import com.infdimmod.burmaldeniya.MurinoWorldgenHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class MurinoFogMixin {
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void applyMurinoFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        Entity entity = camera.getFocusedEntity();
        if (entity != null && entity.getWorld() != null) {
            Biome biome = entity.getWorld().getBiome(entity.getBlockPos()).value();
            if (entity.getWorld().getRegistryManager()
                    .get(net.minecraft.registry.RegistryKeys.BIOME)
                    .getKey(biome).map(k -> k.equals(MurinoWorldgenHooks.MURINO_BIOME_KEY)).orElse(false)) {
                
                // Set dense fog for Murino biome
                float start = viewDistance * 0.05F;
                float end = viewDistance * 0.45F;
                RenderSystem.setShaderFogStart(start);
                RenderSystem.setShaderFogEnd(end);
            }
        }
    }
}
