package com.infdimmod.items.custom.portalgun;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.Entities.custom.GreenPortal;
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
        super(settings.maxDamage(42));
    }


    public boolean isItemBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    public static float getBrokenState(ItemStack stack) {
        if (stack.getDamage() >= 42) {
            return 1.0f;
        } else if (stack.getDamage() >= 28) {
            return 0.7f;
        } else if (stack.getDamage() >= 14) {
            return 0.8f;
        } else {
            return 0.0f;
        }
    }

    public static void setPortalCode(ItemStack stack, String code) {
        stack.set(PortalGunComponents.PORTALCODETYPE, new PortalGunComponents(code));
    }

    public static String getPortalCode(ItemStack stack) {
        PortalGunComponents component = stack.get(PortalGunComponents.PORTALCODETYPE);
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
        boolean isMode2 = stack.getOrDefault(PortalGunComponents.PORTAL_GUN_MODE, false);
        if (!world.isClient) {
            if (isItemBroken(stack) || user.getItemCooldownManager().isCoolingDown(this) || hand == Hand.OFF_HAND) {
                return TypedActionResult.pass(stack);
            }
            String code = getPortalCode(stack);
            if (code == null || code.isEmpty()) {
                return TypedActionResult.pass(stack);
            }
            // базовые вектора для обоих режимов
            Vec3d eyePos = user.getEyePos();
            Vec3d lookVec = user.getRotationVec(1.0F);
            Vec3d rightVec = lookVec.crossProduct(new Vec3d(0, 1, 0)).normalize();

            Vec3d startPos = eyePos
                    .add(lookVec.multiply(0.4))
                    .add(rightVec.multiply(0.35))
                    .add(0, -0.25, 0);

            Vec3d targetPos;
            float shotYaw = user.getYaw();
            float shotPitch;
            int flightTicks;

            if (isMode2) {
                // дальний
                shotPitch = user.getPitch();
                double maxDistFar = 64.0;
                Vec3d traceEndFar = eyePos.add(lookVec.multiply(maxDistFar));

                BlockHitResult hitFar = world.raycast(new RaycastContext(
                        eyePos, traceEndFar,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        user
                ));

                if (hitFar.getType() == HitResult.Type.MISS) {
                    return TypedActionResult.fail(stack);
                }

                Vec3d hitPos = hitFar.getPos();
                Vec3d directionToPlayer = eyePos.subtract(hitPos).normalize();
                targetPos = hitPos.add(directionToPlayer); //смещение от стеныdirectionToPlayer.multiply(0.75)

                // расчет времени
                double distance = startPos.distanceTo(targetPos);
                double speedFactor = 4.5;
                flightTicks = (int) Math.max(2, Math.round(distance / speedFactor));

            } else {
                // ближний
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
                    targetPos = hitPos.add(directionToPlayer); //смещение от стеныdirectionToPlayer.multiply(0.75)
                } else {
                    targetPos = traceEndShort;
                }

                flightTicks = 4;
            }

            GreenPortal entity = new GreenPortal(ModEntities.GREEN_PORTAL_ENTITY_TYPE, world);
            String codeFromGun = stack.getOrDefault(PortalGunComponents.PORTALCODETYPE, new PortalGunComponents("")).portalcode();
            entity.setDimensionCode(codeFromGun);

            entity.setAnimationData(startPos.toVector3f(), targetPos.toVector3f());
            entity.setFlightDuration(flightTicks);
            PortalGunComponents.PortalCoords coords = getTargetCoords(stack);
            if (coords != null) {
                entity.setPortalTargetPos(new Vec3d(coords.x(), coords.y(), coords.z()));
            }
            entity.refreshPositionAndAngles(startPos.x, startPos.y, startPos.z, shotYaw, shotPitch);

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

    public static PortalGunComponents.PortalCoords getTargetCoords(ItemStack stack) {
        return stack.get(PortalGunComponents.PORTAL_COORDS);
    }
    public static void setTargetCoords(ItemStack stack, double x, double y, double z) {
        stack.set(PortalGunComponents.PORTAL_COORDS, new PortalGunComponents.PortalCoords(x, y, z));
    }
}