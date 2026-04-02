package com.infdimmod.Entities.custom;

import com.infdimmod.particle.ModParticles;
import com.infdimmod.util.IEntityTeleportTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.util.List;

public class BackPortal extends Entity {
    private static final TrackedData<Vector3f> DESTINATION_VEC = DataTracker.registerData(BackPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<String> DIMENSION_CODE = DataTracker.registerData(BackPortal.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Long> SPAWN_TICK = DataTracker.registerData(BackPortal.class, TrackedDataHandlerRegistry.LONG);

    private static final int TOTAL_LIFETIME = 160;

    public BackPortal(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        if (!world.isClient) this.setSpawnTick(world.getTime());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DIMENSION_CODE, "¯\\_(ツ)_/¯");
        builder.add(DESTINATION_VEC, new Vector3f());
        builder.add(SPAWN_TICK, 0L);
    }
    public int getAge() { return this.age; }
    public void setSpawnTick(long tick) { this.getDataTracker().set(SPAWN_TICK, tick); }
    public long getSpawnTick() { return this.getDataTracker().get(SPAWN_TICK); }
    public long getPortalAge() { return this.getWorld().getTime() - getSpawnTick(); }

    public void setDimensionCode(String code) { this.getDataTracker().set(DIMENSION_CODE, code); }
    public String getDimensionCode() { return this.getDataTracker().get(DIMENSION_CODE); }

    @Override
    public void tick() {
        super.tick();
        long currentAge = getPortalAge();

        if (!this.getWorld().isClient) {
            if (currentAge == 1) setChunkForceLoaded(true);
            if (currentAge >= TOTAL_LIFETIME) {
                setChunkForceLoaded(false);
                this.discard();
                return;
            }

            List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox());
            for (Entity entity : entities) {
                tryTeleportEntity(entity);
            }
        }

        if (age <= 10 && this.getWorld().isClient) {
            spawnIdleParticles();
        }
    }

    private void tryTeleportEntity(Entity entity) {
        if (this.getWorld().isClient || entity == null || !entity.isAlive()) return;

        MinecraftServer server = this.getServer();
        if (server == null) return;
        if (entity instanceof GreenPortal || entity instanceof BackPortal) {return;}

        if (age < 30) return;

        long currentTime = server.getOverworld().getTime();

        if (entity instanceof IEntityTeleportTracker tracker) {
            if (currentTime - tracker.infdimmod$getLastTeleportTick() < 30) return;
        } else {
            return;
        }

        String targetCode = getDimensionCode();
        long targetSeed = this.getSeedFromCode(targetCode);
        ServerWorld targetWorld = null;
        Identifier potentialId = Identifier.tryParse(targetCode);

        if (potentialId != null && targetCode.contains(":")) {
            targetWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, potentialId));
        }

        if (targetWorld == null) {
            Identifier targetDimId = Identifier.of("infdimmod", "dim_" + targetSeed);
            Fantasy fantasy = Fantasy.get(server);

            ServerWorld overworld = server.getOverworld();
            GameRules overworldRules = overworld.getGameRules();

            RuntimeWorldConfig config = new RuntimeWorldConfig()
                    .setDimensionType(DimensionTypes.OVERWORLD)
                    .setSeed(targetSeed)
                    .setGenerator(overworld.getChunkManager().getChunkGenerator());

            overworldRules.accept(new GameRules.Visitor() {
                @Override
                public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    if (key == GameRules.SPAWN_CHUNK_RADIUS) {
                        config.setGameRule(GameRules.SPAWN_CHUNK_RADIUS, 0);
                        return;
                    }

                    T rule = overworldRules.get(key);
                    if (rule instanceof GameRules.BooleanRule boolRule) {
                        config.setGameRule((GameRules.Key<GameRules.BooleanRule>) key, boolRule.get());
                    } else if (rule instanceof GameRules.IntRule intRule) {
                        config.setGameRule((GameRules.Key<GameRules.IntRule>) key, intRule.get());
                    }
                }
            });
            targetWorld = fantasy.getOrOpenPersistentWorld(targetDimId, config).asWorld();
        }

        if (targetWorld != null) {
            Vec3d targetPos = getDestinationPos();

            ((IEntityTeleportTracker) entity).infdimmod$setLastTeleportTick(currentTime);

            TeleportTarget teleportTarget = new TeleportTarget(
                    targetWorld,
                    targetPos,
                    entity.getVelocity(),
                    entity.getYaw(),
                    entity.getPitch(),
                    TeleportTarget.NO_OP
            );

            Entity teleportedEntity = entity.teleportTo(teleportTarget);

            if (teleportedEntity != null && teleportedEntity != entity) {
                ((IEntityTeleportTracker) teleportedEntity).infdimmod$setLastTeleportTick(currentTime);
            }
        }
    }

    private void spawnIdleParticles() {
        for (int i = 0; i < 2; i++) {
            this.getWorld().addParticle(ModParticles.GREEN_LIGHTNING,
                    this.getX() + (random.nextDouble() - 0.5),
                    this.getY() + (random.nextDouble() - 0.5),
                    this.getZ() + (random.nextDouble() - 0.5),
                    0, 0, 0);
        }
    }

    public float getVisualScale(float tickDelta) {
        float exactAge = (float)getPortalAge() + tickDelta;
        if (exactAge > (TOTAL_LIFETIME - 10f)) {
            return MathHelper.sin(((TOTAL_LIFETIME - exactAge) / 10f) * (float)Math.PI / 2f);
        }
        if (age + tickDelta < 10f) return age / 10f;
        return 1.0f;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("DimensionCode")) setDimensionCode(nbt.getString("DimensionCode"));
        if (nbt.contains("destX")) {
            setDestinationPos(new Vec3d(nbt.getDouble("destX"), nbt.getDouble("destY"), nbt.getDouble("destZ")));
        }
        if (nbt.contains("SpawnTick")) setSpawnTick(nbt.getLong("SpawnTick"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("DimensionCode", getDimensionCode());
        Vec3d dest = getDestinationPos();
        nbt.putDouble("destX", dest.x);
        nbt.putDouble("destY", dest.y);
        nbt.putDouble("destZ", dest.z);
        nbt.putLong("SpawnTick", getSpawnTick());
    }

    public void setDestinationPos(Vec3d pos) {
        this.getDataTracker().set(DESTINATION_VEC, new Vector3f((float)pos.x, (float)pos.y, (float)pos.z));
    }

    public Vec3d getDestinationPos() {
        Vector3f vec = this.getDataTracker().get(DESTINATION_VEC);
        return new Vec3d(vec.x, vec.y, vec.z);
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
            serverWorld.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, forced);
        }
    }

    @Override
    public void onRemoved() {
        if (!this.getWorld().isClient) setChunkForceLoaded(false);
        super.onRemoved();
    }

    @Override
    protected net.minecraft.util.math.Box calculateBoundingBox() {
        double halfW = (18.0 / 16.0) / 2.0; // Половина ширины (0.5625)
        double halfH = (30.0 / 16.0) / 2.0; // Половина высоты (0.9375)
        double halfT = 0.05;                // Половина толщины (5 см)

        float yawRad = -this.getYaw() * ((float)Math.PI / 180F);
        float pitchRad = -this.getPitch() * ((float)Math.PI / 180F);

        Vec3d right = new Vec3d(Math.cos(yawRad), 0, Math.sin(yawRad)).multiply(halfW);

        Vec3d up = new Vec3d(
                -Math.sin(yawRad) * Math.sin(pitchRad),
                Math.cos(pitchRad),
                Math.cos(yawRad) * Math.sin(pitchRad)
        ).multiply(halfH);

        Vec3d forward = right.crossProduct(up).normalize().multiply(halfT);

        double maxDeltaX = Math.abs(right.x) + Math.abs(up.x) + Math.abs(forward.x);
        double maxDeltaY = Math.abs(right.y) + Math.abs(up.y) + Math.abs(forward.y);
        double maxDeltaZ = Math.abs(right.z) + Math.abs(up.z) + Math.abs(forward.z);

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ();

        return new net.minecraft.util.math.Box(
                centerX - maxDeltaX, centerY - maxDeltaY, centerZ - maxDeltaZ,
                centerX + maxDeltaX, centerY + maxDeltaY, centerZ + maxDeltaZ
        );
    }
}