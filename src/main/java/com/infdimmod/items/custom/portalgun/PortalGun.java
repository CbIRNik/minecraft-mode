package com.infdimmod.items.custom.portalgun;

import com.infdimmod.Entities.GreenPortal;
import com.infdimmod.Entities.ModEntities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public class PortalGun extends Item {
    public PortalGun(Settings settings) {
        super(settings.maxDamage(420));
    }


    public boolean isItemBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    public static float getBrokenState(ItemStack stack) {
        if (stack.getDamage() >= 420) {
            return 1.0f;
        } else if (stack.getDamage() >= 280) {
            return 0.7f;
        } else if (stack.getDamage() >= 140) {
            return 0.8f;
        } else {
            return 0.0f;
        }
    }

    public static void setPortalCode(ItemStack stack, String code) {
        stack.set(PortalGunCodeComponent.PORTALCODETYPE, new PortalGunCodeComponent(code));
    }

    public static String getPortalCode(ItemStack stack) {
        PortalGunCodeComponent component = stack.get(PortalGunCodeComponent.PORTALCODETYPE);
        return component != null ? component.portalcode() : "";
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("itemTooltip.infdimmod.portal_gun").formatted(Formatting.GRAY));
        tooltip.add(Text.of(getPortalCode(stack)));
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean isMode2 = stack.getOrDefault(PortalGunCodeComponent.PORTAL_GUN_MODE, false);

        if (!world.isClient) {
            if (isItemBroken(stack) || user.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.pass(stack);
            }

            Vec3d eyePos = user.getEyePos();
            Vec3d targetPos;

            float shotYaw = user.getYaw();
            float shotPitch;

            if (isMode2) {
                // второй (дальний) режим
                shotPitch = user.getPitch();
                double maxDistFar = 64.0;
                Vec3d lookVec = user.getRotationVec(1.0F);
                Vec3d traceEndFar = eyePos.add(lookVec.multiply(maxDistFar));

                BlockHitResult hitFar = world.raycast(new RaycastContext(
                        eyePos, traceEndFar,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        user
                ));

                // не попал - не ПЭПЭ
                if (hitFar.getType() == HitResult.Type.MISS) {
                    return TypedActionResult.fail(stack);
                }

                Vec3d hitPos = hitFar.getPos();
                Vec3d directionToPlayer = eyePos.subtract(hitPos).normalize();
                targetPos = hitPos.add(directionToPlayer.multiply(0.5));

            } else {
                // первый (ближний) режим
                shotPitch = 0.0f;
                float clampedPitch = MathHelper.clamp(user.getPitch(), -20.0F, 20.0F);

                Vec3d limitedLookVec = Vec3d.fromPolar(clampedPitch, user.getYaw());
                double maxDistShort = 2.5;

                Vec3d traceEndShort = eyePos.add(limitedLookVec.multiply(maxDistShort));
                BlockHitResult hitShort = world.raycast(new RaycastContext(
                        eyePos, traceEndShort,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        user
                ));

                if (hitShort.getType() != HitResult.Type.MISS) {
                    Vec3d hitPos = hitShort.getPos();
                    Vec3d directionToPlayer = eyePos.subtract(hitPos).normalize();
                    targetPos = hitPos.add(directionToPlayer.multiply(0.5));
                } else {
                    targetPos = traceEndShort;
                }
            }


            Vec3d sideOffset = user.getRotationVec(1.0F).crossProduct(new Vec3d(0, 1, 0)).multiply(0.3);
            Vec3d startPos = eyePos.add(sideOffset).add(0, -0.2, 0);

            GreenPortal entity = new GreenPortal(ModEntities.GREEN_PORTAL_ENTITY_TYPE, world);
            entity.setAnimationData(startPos.toVector3f(), targetPos.toVector3f());

            entity.refreshPositionAndAngles(startPos.x, startPos.y, startPos.z, shotYaw, shotPitch);

            if (isMode2) {
                double distance = startPos.distanceTo(targetPos);
                double speedFactor = 6.0;

                int dynamicTicks = (int) Math.max(2, Math.round(distance / speedFactor));

                entity.setFlightDuration(dynamicTicks);
            } else {
                entity.setFlightDuration(2);  // 4 тиков для ближнего режима
            }
            world.spawnEntity(entity);

            // звук и кд
            float soundPitch = isMode2 ? 1.2F : 0.6F;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0F, soundPitch);

            if (!user.isCreative()) {
                stack.setDamage(Math.min(stack.getMaxDamage(), stack.getDamage() + 1));
            }
            user.getItemCooldownManager().set(this, 10);
        }
        return TypedActionResult.success(stack);
    }
}