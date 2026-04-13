package com.infdimmod.mixin;

import com.infdimmod.burmaldeniya.MurinoBiomeHelper;
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
        if (world != null && !world.isClient() && MurinoBiomeHelper.isMurino(world, self.getBlockPos())) {
            cir.setReturnValue(false);
        }
    }
}
