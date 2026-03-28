package com.infdimmod.Entities;

import com.infdimmod.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class GreenPortal extends Entity {

    private static final TrackedData<Vector3f> START_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Vector3f> TARGET_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> MAX_AGE = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> DIMENSION_CODE = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.STRING);

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
        builder.add(DIMENSION_CODE, "¯\\_(ツ)_/¯");
    }

    public void setDimensionCode(String code) {
        this.getDataTracker().set(DIMENSION_CODE, code);
    }

    public String getDimensionCode() {
        return this.getDataTracker().get(DIMENSION_CODE);
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

            float t = (float) age / duration;

            double posX = MathHelper.lerp(t, (double)start.x, (double)target.x);
            double posY = MathHelper.lerp(t, (double)start.y, (double)target.y);
            double posZ = MathHelper.lerp(t, (double)start.z, (double)target.z);

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
        float t = ((float)this.age + tickDelta) / ((float)this.getMaxAge()+2);
        if (t >= 1.0f) return 1.0f;
        if (t <= 0.0f) return 0.0f;
        return (float) Math.pow(2, 2*(t-1))-0.25f;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
        if (nbt.contains("DimensionCode")) {
            setDimensionCode(nbt.getString("DimensionCode"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
        nbt.putString("DimensionCode", getDimensionCode());
    }
    //ОТЛАДКА-----------------------------
    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (!this.getWorld().isClient) {
            int flightDuration = this.getDataTracker().get(MAX_AGE);
            if (this.age < 5) {
                return;
            }//проверка что портал долетел

            String code = this.getDimensionCode();
            Text message = Text.literal("Код портала: ")
                    .append(Text.literal(code != null ? code : "NULL").formatted(Formatting.GREEN));
            player.sendMessage(message, false);
        }
    }
    //------------------------------------


}