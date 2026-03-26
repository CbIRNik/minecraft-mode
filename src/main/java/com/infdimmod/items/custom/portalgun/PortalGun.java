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

        if (!world.isClient) {
            if (isItemBroken(stack) || user.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.pass(stack);
            }

            // 1. ограничение разлета в +-30 градусов
            float clampedPitch = MathHelper.clamp(user.getPitch(), -20.0F, 20.0F);

            // 2. вектор направления на основе ограниченного угла
            Vec3d limitedLookVec = Vec3d.fromPolar(clampedPitch, user.getYaw());

            Vec3d eyePos = user.getEyePos();
            double maxDist = 2.5;

            // 3. проверяем путь по ограниченному вектору
            Vec3d traceEnd = eyePos.add(limitedLookVec.multiply(maxDist));
            BlockHitResult hit = world.raycast(new RaycastContext(
                    eyePos,
                    traceEnd,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    user
            ));

            // 4. финальная точка прилета
            Vec3d targetPos;
            if (hit.getType() != HitResult.Type.MISS) {
                //попадание в блок
                Vec3d hitPos = hit.getPos();
                Vec3d directionToPlayer = eyePos.subtract(hitPos).normalize();
                targetPos = hitPos.add(directionToPlayer.multiply(0.1));
            } else {
                targetPos = traceEnd;
            }

            // 5. точка старта
            Vec3d sideOffset = user.getRotationVec(1.0F).crossProduct(new Vec3d(0, 1, 0)).multiply(0.3);
            Vec3d startPos = eyePos.add(sideOffset).add(0, -0.2, 0);

            // 6. сущность
            GreenPortal entity = new GreenPortal(ModEntities.GREEN_PORTAL_ENTITY_TYPE, world);
            entity.setAnimationData(startPos.toVector3f(), targetPos.toVector3f());
            entity.refreshPositionAndAngles(startPos.x, startPos.y, startPos.z, user.getYaw(), 0);

            world.spawnEntity(entity);

            // звук
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0F, 0.6F);
            // поломка и кулдаун
            if (!user.isCreative()) {
                stack.setDamage(Math.min(stack.getMaxDamage(), stack.getDamage() + 1));
            }
            user.getItemCooldownManager().set(this, 10);
        }
        return TypedActionResult.success(stack);
    }
}