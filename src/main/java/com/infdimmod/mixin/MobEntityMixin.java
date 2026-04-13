package com.infdimmod.mixin;

import com.infdimmod.burmaldeniya.MurinoWorldgenHooks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Inject(method = "isAffectedByDaylight", at = @At("HEAD"), cancellable = true)
    private void infdimmod$immuneToSunInMurino(CallbackInfoReturnable<Boolean> cir) {
        MobEntity self = (MobEntity) (Object) this;
        World world = self.getWorld();
        if (world != null && !world.isClient()) {
            if (world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BIOME)
                    .getKey(world.getBiome(self.getBlockPos()).value())
                    .map(k -> k.equals(MurinoWorldgenHooks.MURINO_BIOME_KEY)).orElse(false)) {
                // If in Murino biome, sun doesn't affect them
                cir.setReturnValue(false);
            }
        }
    }
}
