package com.infdimmod.Entities;

import com.infdimmod.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class GreenPortal extends Entity {

    private static final TrackedData<Vector3f> START_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Vector3f> TARGET_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> MAX_AGE = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.INTEGER);

    private int age = 0;
    private static final int TOTAL_LIFETIME = 160;
    private Vec3d startPos;

    public GreenPortal(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(START_VEC, new Vector3f());
        builder.add(TARGET_VEC, new Vector3f());
        builder.add(MAX_AGE, 6);
    }

    public int getAge() { return this.age; }
    public int getMaxAge() { return this.getDataTracker().get(MAX_AGE); }
    public Vector3f getStartVec() { return this.getDataTracker().get(START_VEC); }


    public void setFlightDuration(int ticks) {
        this.getDataTracker().set(MAX_AGE, ticks);
    }

    public void setAnimationData(Vector3f start, Vector3f target) {
        this.getDataTracker().set(START_VEC, start);
        this.getDataTracker().set(TARGET_VEC, target);
        this.startPos = new Vec3d(start.x, start.y, start.z);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        int duration = this.getDataTracker().get(MAX_AGE);

        if (age <= duration) {
            Vector3f start = this.getDataTracker().get(START_VEC);
            Vector3f target = this.getDataTracker().get(TARGET_VEC);

            // Квадратичная интерполяция (плавный старт, быстрый конец)
            float t = (float) age / duration;
            float interp = 1.0f - (1.0f - t) * (1.0f - t);

            double posX = MathHelper.lerp(interp, (double)start.x, (double)target.x);
            double posY = MathHelper.lerp(interp, (double)start.y, (double)target.y);
            double posZ = MathHelper.lerp(interp, (double)start.z, (double)target.z);

            this.setPosition(posX, posY, posZ);


            if (this.getWorld().isClient) {
                if (this.startPos == null) {
                    this.startPos = new Vec3d(start.x, start.y, start.z);
                }

                double distance = this.getPos().distanceTo(startPos);

                // количество
                int particleCount = (int) Math.min(7, 1 + (distance / 3.5));
                // разброс
                double spread = Math.min(1.0, 0.2 + (distance / 40.0));

                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * spread;
                    double offsetY = (random.nextDouble() - 0.5) * spread;
                    double offsetZ = (random.nextDouble() - 0.5) * spread;

                    this.getWorld().addParticle(
                            ModParticles.GREEN_LIGHTNING,
                            this.getX() + offsetX,
                            this.getY() + offsetY,
                            this.getZ() + offsetZ,
                            0, 0, 0
                    );
                }
            }
        }

        if (!this.getWorld().isClient && age >= TOTAL_LIFETIME) {
            this.discard();
        }
    }

    public float getVisualScale(float tickDelta) {
        float currentAge = age + tickDelta;
        int duration = this.getDataTracker().get(MAX_AGE);
        return MathHelper.clamp(currentAge / duration, 0.0f, 1.0f);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
    }
}