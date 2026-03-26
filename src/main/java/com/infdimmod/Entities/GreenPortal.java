package com.infdimmod.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class GreenPortal extends Entity {
    private static final TrackedData<Vector3f> START_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Vector3f> TARGET_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);

    private int age = 0;
    private static final int FLIGHT_DURATION = 3; // вылет
    private static final int TOTAL_LIFETIME = 80;

    public GreenPortal(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(START_VEC, new Vector3f());
        builder.add(TARGET_VEC, new Vector3f());
    }

    public void setAnimationData(Vector3f start, Vector3f target) {
        this.getDataTracker().set(START_VEC, start);
        this.getDataTracker().set(TARGET_VEC, target);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (age <= FLIGHT_DURATION) {
            Vector3f start = this.getDataTracker().get(START_VEC);
            Vector3f target = this.getDataTracker().get(TARGET_VEC);

            // анимация
            float t = (float) age / FLIGHT_DURATION;
            float interp = 1.0f - (float)Math.pow(1.0f - t, 2);

            this.setPosition(
                    MathHelper.lerp(interp, start.x, target.x),
                    MathHelper.lerp(interp, start.y, target.y),
                    MathHelper.lerp(interp, start.z, target.z)
            );
        }

        if (!this.getWorld().isClient && age >= TOTAL_LIFETIME) {
            this.discard();
        }
    }

    // изменение масштаба
    public float getVisualScale(float tickDelta) {
        float currentAge = age + tickDelta;
        return MathHelper.clamp(currentAge / FLIGHT_DURATION, 0.0f, 1.0f);
    }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { this.age = nbt.getInt("Age"); }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { nbt.putInt("Age", this.age); }
}