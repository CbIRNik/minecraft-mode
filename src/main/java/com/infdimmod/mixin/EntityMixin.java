package com.infdimmod.mixin;

import com.infdimmod.util.IEntityTeleportTracker;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityTeleportTracker {
    @Unique
    private long lastTeleportTick = 0;

    @Override
    public long infdimmod$getLastTeleportTick() {
        return lastTeleportTick;
    }

    @Override
    public void infdimmod$setLastTeleportTick(long tick) {
        this.lastTeleportTick = tick;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeTeleportData(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        nbt.putLong("infdimmod_last_tp", lastTeleportTick);
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readTeleportData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("infdimmod_last_tp")) {
            this.lastTeleportTick = nbt.getLong("infdimmod_last_tp");
        }
    }
}