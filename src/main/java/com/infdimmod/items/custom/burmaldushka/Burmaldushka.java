package com.infdimmod.items.custom.burmaldushka;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.Entities.custom.GreenPortal;
import com.infdimmod.burmaldeniya.BurmaldeniyaConstants;
import com.infdimmod.world.BurmaldeniyaWorldFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public class Burmaldushka extends Item {
    public Burmaldushka(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("itemTooltip.infdimmod.burmaldushka_route").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(BurmaldeniyaConstants.BURMALDENIYA_ROUTE_CODE).formatted(Formatting.DARK_AQUA));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hand == Hand.OFF_HAND || user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient) {
            Vec3d eyePos = user.getEyePos();
            Vec3d lookVec = user.getRotationVec(1.0F);
            Vec3d rightVec = lookVec.crossProduct(new Vec3d(0, 1, 0)).normalize();

            Vec3d startPos = eyePos
                    .add(lookVec.multiply(0.4))
                    .add(rightVec.multiply(0.35))
                    .add(0, -0.25, 0);

            double maxDist = 64.0;
            Vec3d traceEnd = eyePos.add(lookVec.multiply(maxDist));
            BlockHitResult hit = world.raycast(new RaycastContext(
                    eyePos, traceEnd,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    user
            ));

            if (hit.getType() == HitResult.Type.MISS) {
                return TypedActionResult.fail(stack);
            }

            Vec3d hitPos = hit.getPos();
            Vec3d directionToPlayer = eyePos.subtract(hitPos).normalize();
            Vec3d targetPos = hitPos.add(directionToPlayer);

            int flightTicks = (int) Math.max(2, Math.round(startPos.distanceTo(targetPos) / 4.5));

            GreenPortal entity = new GreenPortal(ModEntities.GREEN_PORTAL_ENTITY_TYPE, world);
            entity.setDimensionCode(BurmaldeniyaConstants.BURMALDENIYA_ROUTE_CODE);
            entity.setAnimationData(startPos.toVector3f(), targetPos.toVector3f());
            entity.setFlightDuration(flightTicks);
            entity.setPortalTargetPos(resolveBurmaldeniyaTarget(world, user));
            entity.refreshPositionAndAngles(startPos.x, startPos.y, startPos.z, user.getYaw(), user.getPitch());
            world.spawnEntity(entity);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_ENDER_PEARL_THROW, SoundCategory.PLAYERS, 1.0F, 0.75F);

            user.getItemCooldownManager().set(this, 20);
        }
        return TypedActionResult.success(stack);
    }

    private Vec3d resolveBurmaldeniyaTarget(World world, PlayerEntity player) {
        if (world.getServer() == null) {
            return player.getPos();
        }

        ServerWorld burmaldeniyaWorld = BurmaldeniyaWorldFactory.getOrCreateBurmaldeniyaWorld(world.getServer());
        int topY = burmaldeniyaWorld.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                player.getBlockX(),
                player.getBlockZ());
        return new Vec3d(player.getX(), topY + 1, player.getZ());
    }
}
