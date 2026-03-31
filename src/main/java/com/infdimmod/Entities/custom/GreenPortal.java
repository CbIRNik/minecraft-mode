package com.infdimmod.Entities.custom;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import com.infdimmod.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public class GreenPortal extends Entity {
    private static final TrackedData<Vector3f> PORTAL_TARGET_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
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
        builder.add(PORTAL_TARGET_VEC, new Vector3f());
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

        if (!this.getWorld().isClient && this.age == 1) {
            setChunkForceLoaded(true);
        }

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
                int particleCount = (int) Math.min(3, 1 + (distance / 1.0));
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
            setChunkForceLoaded(false);
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
        this.lastTeleportTick = nbt.getLong("LastTeleportTick");
        if (nbt.contains("DimensionCode")) {
            setDimensionCode(nbt.getString("DimensionCode"));
        }
        if (nbt.contains("targetX")) {
            setPortalTargetPos(new Vec3d(nbt.getDouble("targetX"), nbt.getDouble("targetY"), nbt.getDouble("targetZ")));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
        nbt.putLong("LastTeleportTick", this.lastTeleportTick);
        nbt.putString("DimensionCode", getDimensionCode());
        Vec3d target = getPortalTargetPos();
        nbt.putDouble("targetX", target.x);
        nbt.putDouble("targetY", target.y);
        nbt.putDouble("targetZ", target.z);
    }

    public void setPortalTargetPos(Vec3d pos) {
        this.getDataTracker().set(PORTAL_TARGET_VEC, new Vector3f((float)pos.x, (float)pos.y, (float)pos.z));
    }

    public Vec3d getPortalTargetPos() {
        Vector3f vec = this.getDataTracker().get(PORTAL_TARGET_VEC);
        return new Vec3d(vec.x, vec.y, vec.z);
    }

    private long lastTeleportTick;
    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (this.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        MinecraftServer server = this.getServer();
        if (server == null) return;

        long globalTime = server.getOverworld().getTime();
        if (globalTime - lastTeleportTick < 60 || this.age < 10) return;//кулдаун портала в 3секунд

        // код мира направления
        String targetCode = getDimensionCode();

        long targetSeed = this.getSeedFromCode(targetCode);

        // мир
        ServerWorld targetWorld = null;
        Identifier potentialId = Identifier.tryParse(targetCode);

        if (potentialId != null && targetCode.contains(":")) {
            targetWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, potentialId));
        }

        if (targetWorld == null) {
            Identifier targetDimId = Identifier.of("infdimmod", "dim_" + targetSeed);
            Fantasy fantasy = Fantasy.get(server);
            RuntimeWorldConfig config = new RuntimeWorldConfig()
                    .setDimensionType(DimensionTypes.OVERWORLD)
                    .setSeed(targetSeed)
                    .setGenerator(server.getOverworld().getChunkManager().getChunkGenerator());
            targetWorld = fantasy.getOrOpenPersistentWorld(targetDimId, config).asWorld();
        }

        // телепортация
        this.lastTeleportTick = globalTime;
        Vec3d targetPos = getPortalTargetPos();
        serverPlayer.teleport(targetWorld, targetPos.x, targetPos.y, targetPos.z, serverPlayer.getYaw(), serverPlayer.getPitch());
        //обратный портал
        BackPortal backentity = new BackPortal(ModEntities.BACK_PORTAL_ENTITY_TYPE, targetWorld);
        backentity.setDimensionCode(this.getWorld().getRegistryKey().getValue().toString());
        backentity.setDestinationPos(this.getPos());
        backentity.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, this.getYaw(), this.getPitch());


        targetWorld.spawnEntity(backentity);
    }

    public long getSeedFromCode(String code) {
        if (code == null || code.isEmpty()) return 0L;
        try {
            return Math.abs(Long.parseLong(code.toLowerCase(), 36));
        } catch (NumberFormatException e) {
            return (long) Math.abs(code.hashCode());
        }
    }

    private void setChunkForceLoaded(boolean forced) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = this.getBlockPos();
            //координаты чанка
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            serverWorld.setChunkForced(chunkX, chunkZ, forced);
        }
    }

    @Override
    public void onRemoved() {
        if (!this.getWorld().isClient) {
            setChunkForceLoaded(false);
        }
        super.onRemoved();
    }
}