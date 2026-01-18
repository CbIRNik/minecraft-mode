package com.infdimmod.mixin;

import com.infdimmod.Blocks.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PortalFluidMixin extends Entity {

    Entity self = (Entity)(Object)this;
    World world = self.getWorld();
    BlockPos pos = self.getBlockPos();

    @Shadow
    protected boolean jumping;

    protected PortalFluidMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3d movementInput, CallbackInfo ci) {
        if (this.isSubmergedInCustomFluid()) {
            if (self.isPlayer()) {
                PlayerEntity player = (PlayerEntity) self;
                if (!player.getAbilities().flying) {
                    handleCustomFluidMovement(movementInput);
                }
            }
            else {
            handleCustomFluidMovement(movementInput);
            }
        }
    }

    @Unique
    private boolean isSubmergedInCustomFluid() {
        BlockPos feetPos = BlockPos.ofFloored(this.getX(), this.getY(), this.getZ());
        BlockPos headPos = BlockPos.ofFloored(this.getX(), this.getEyeY(), this.getZ());
        BlockState feetState = this.getWorld().getBlockState(feetPos);
        BlockState headState = this.getWorld().getBlockState(headPos);
        return feetState.getFluidState().isOf(ModBlocks.STILL_CUSTOM_FLUID) ||
                feetState.getFluidState().isOf(ModBlocks.FLOWING_CUSTOM_FLUID) ||
                headState.getFluidState().isOf(ModBlocks.STILL_CUSTOM_FLUID) ||
                headState.getFluidState().isOf(ModBlocks.FLOWING_CUSTOM_FLUID);
    }

    @Unique
    private void handleCustomFluidMovement(Vec3d movementInput) {
        LivingEntity entity = (LivingEntity)(Object)this;
        float dragCoefficient = 0.7f;
        float farch = 0.062f;
        Vec3d velocity = entity.getVelocity();

        //Обработка вертикального движения (всплытие/погружение)
        if (this.jumping) {
            // При прыжке - всплываем
            velocity = velocity.add(0, 0.1f, 0);
            this.setVelocity(velocity);
        } else {
            // Без прыжка - медленно тонем или остаемся на плаву
            velocity = velocity.add(0, farch, 0);
            this.setVelocity(velocity);
        }
        //Применяем сопротивление жидкости (замедление движения)
        velocity = velocity.multiply(dragCoefficient, 0.7, dragCoefficient);
        //Ограничиваем максимальную скорость в жидкости
        double maxSpeed = 0.2;
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }
        //Устанавливаем финальную скорость и перемещаем сущность
        this.setVelocity(velocity);
        this.move(MovementType.SELF, this.getVelocity());
        //Сбрасываем прыжок после обработки
        this.jumping = false;
    }
}