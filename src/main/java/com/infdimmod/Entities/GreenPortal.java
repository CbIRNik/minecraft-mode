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
    private static final TrackedData<Integer> MAX_AGE = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int TOTAL_LIFETIME = 160;

    public GreenPortal(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(START_VEC, new Vector3f());
        builder.add(TARGET_VEC, new Vector3f());
        builder.add(MAX_AGE, 6); // По умолчанию 6
    }

    public void setFlightDuration(int ticks) {
        this.getDataTracker().set(MAX_AGE, ticks);
    }

    public void setAnimationData(Vector3f start, Vector3f target) {
        this.getDataTracker().set(START_VEC, start);
        this.getDataTracker().set(TARGET_VEC, target);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        int duration = this.getDataTracker().get(MAX_AGE);

        if (age <= duration) {
            Vector3f start = this.getDataTracker().get(START_VEC);
            Vector3f target = this.getDataTracker().get(TARGET_VEC);

            float t = (float) age / duration;
            float interp = 1.0f - (1.0f - t) * (1.0f - t);

            this.setPosition(
                    MathHelper.lerp(interp, (double)start.x, (double)target.x),
                    MathHelper.lerp(interp, (double)start.y, (double)target.y),
                    MathHelper.lerp(interp, (double)start.z, (double)target.z)
            );
        }
        if (!this.getWorld().isClient && age >= TOTAL_LIFETIME) {
            this.discard();
        }
    }

    // изменение масштаба
    public float getVisualScale(float tickDelta) {
        float currentAge = age + tickDelta;
        int duration = this.getDataTracker().get(MAX_AGE);
        return MathHelper.clamp(currentAge / duration, 0.0f, 1.0f);
    }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) { this.age = nbt.getInt("Age"); }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) { nbt.putInt("Age", this.age); }
}