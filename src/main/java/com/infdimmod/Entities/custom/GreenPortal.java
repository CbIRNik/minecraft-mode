package com.infdimmod.Entities.custom;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import com.infdimmod.particle.ModParticles;
import com.infdimmod.util.IEntityTeleportTracker;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.util.List;

public class GreenPortal extends Entity {
    private static final TrackedData<Vector3f> PORTAL_TARGET_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Vector3f> START_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Vector3f> TARGET_VEC = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<Integer> MAX_AGE = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> DIMENSION_CODE = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Long> SPAWN_TICK = DataTracker.registerData(GreenPortal.class, TrackedDataHandlerRegistry.LONG);


    private static final int TOTAL_LIFETIME = 160;
    private Vec3d startPos;

    public GreenPortal(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        if (!world.isClient) {
            this.setSpawnTick(world.getTime());
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SPAWN_TICK, 0L);
        builder.add(START_VEC, new Vector3f());
        builder.add(TARGET_VEC, new Vector3f());
        builder.add(MAX_AGE, 6);
        builder.add(DIMENSION_CODE, "¯\\_(ツ)_/¯");
        builder.add(PORTAL_TARGET_VEC, new Vector3f());
    }

    public void setSpawnTick(long tick) { this.getDataTracker().set(SPAWN_TICK, tick); }
    public long getSpawnTick() { return this.getDataTracker().get(SPAWN_TICK); }

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

    public long getPortalAge() {
        return this.getWorld().getTime() - getSpawnTick();
    }

    @Override
    public void tick() {
        super.tick();
        long currentAge = getPortalAge();
        int duration = this.getDataTracker().get(MAX_AGE);

        if (!this.getWorld().isClient) {
            if (currentAge == 1) setChunkForceLoaded(true);
            if (currentAge >= TOTAL_LIFETIME) {
                setChunkForceLoaded(false);
                this.discard();
                return;
            }
        }

        if (currentAge <= duration) {
            Vector3f start = this.getDataTracker().get(START_VEC);
            Vector3f target = this.getDataTracker().get(TARGET_VEC);
            float t = (float) currentAge / duration;

            double posX = MathHelper.lerp(t, (double)start.x, (double)target.x);
            double posY = MathHelper.lerp(t, (double)start.y, (double)target.y);
            double posZ = MathHelper.lerp(t, (double)start.z, (double)target.z);
            this.setPosition(posX, posY, posZ);

            if (this.getWorld().isClient) {
                spawnParticles(start);
            }
        }

        if (!this.getWorld().isClient) {
            List<Entity> entities = this.getWorld().getOtherEntities(this, this.getBoundingBox());

            for (Entity entity : entities) {
                tryTeleportEntity(entity);
            }
        }
    }

    private void spawnParticles(Vector3f start) {
        if (this.startPos == null) this.startPos = new Vec3d(start.x, start.y, start.z);
        double distance = this.getPos().distanceTo(startPos);
        int particleCount = (int) Math.min(3, 1 + (distance / 1.0));
        double spread = Math.min(1.0, 0.2 + (distance / 40.0));
        for (int i = 0; i < particleCount; i++) {
            this.getWorld().addParticle(ModParticles.GREEN_LIGHTNING,
                    this.getX() + (random.nextDouble() - 0.5) * spread,
                    this.getY() + (random.nextDouble() - 0.5) * spread,
                    this.getZ() + (random.nextDouble() - 0.5) * spread,
                    0, 0, 0);
        }
    }

    public float getVisualScale(float tickDelta) {
        float exactAge = (float)getPortalAge() + tickDelta;
        float t = exactAge / ((float)this.getMaxAge() + 2);
        if (exactAge > (TOTAL_LIFETIME - 10f)) {
            return MathHelper.sin(((TOTAL_LIFETIME - exactAge) / 10f) * (float)Math.PI / 2f);
        }
        return MathHelper.clamp((float) Math.pow(2, 2 * (t - 1)) - 0.25f, 0f, 1f);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
        if (nbt.contains("DimensionCode")) {
            setDimensionCode(nbt.getString("DimensionCode"));
        }
        if (nbt.contains("targetX")) {
            setPortalTargetPos(new Vec3d(nbt.getDouble("targetX"), nbt.getDouble("targetY"), nbt.getDouble("targetZ")));
        }
        if (nbt.contains("SpawnTick")) setSpawnTick(nbt.getLong("SpawnTick"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
        nbt.putString("DimensionCode", getDimensionCode());
        Vec3d target = getPortalTargetPos();
        nbt.putDouble("targetX", target.x);
        nbt.putDouble("targetY", target.y);
        nbt.putDouble("targetZ", target.z);
        nbt.putLong("SpawnTick", getSpawnTick());
    }

    public void setPortalTargetPos(Vec3d pos) {
        this.getDataTracker().set(PORTAL_TARGET_VEC, new Vector3f((float)pos.x, (float)pos.y, (float)pos.z));
    }

    public Vec3d getPortalTargetPos() {
        Vector3f vec = this.getDataTracker().get(PORTAL_TARGET_VEC);
        return new Vec3d(vec.x, vec.y, vec.z);
    }

    private void tryTeleportEntity(Entity entity) {
        if (this.getWorld().isClient || entity == null || !entity.isAlive()) return;

        MinecraftServer server = this.getServer();
        if (server == null) return;
        if (entity instanceof GreenPortal || entity instanceof BackPortal) {return;}

        long currentTime = server.getOverworld().getTime();

        if (getPortalAge() < 10) return;

        if (entity instanceof IEntityTeleportTracker tracker) {
            long lastEntityTP = tracker.infdimmod$getLastTeleportTick();
            if (currentTime - lastEntityTP < 30) return;
        } else {
            return;
        }

        String targetCode = getDimensionCode();
        if (targetCode == "¯\\_(ツ)_/¯"){return;}
        ServerWorld targetWorld = null;
        RegistryKey<World> vanillaKey = switch (targetCode) {
            case "overworld" -> World.OVERWORLD;
            case "nether", "the_nether" -> World.NETHER;
            case "end", "the_end" -> World.END;
            default -> null;
        };

        if (vanillaKey != null) {
            targetWorld = server.getWorld(vanillaKey);
        }

        long targetSeed = this.getSeedFromCode(targetCode);
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
            Vec3d targetPos = getPortalTargetPos();

            net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(
                    targetPos.x - 4, targetPos.y - 4, targetPos.z - 4,
                    targetPos.x + 4, targetPos.y + 4, targetPos.z + 4
            );

            List<BackPortal> nearbyPortals = targetWorld.getEntitiesByClass(
                    BackPortal.class,
                    searchBox,
                    e -> true
            );

            BackPortal existingPortal = nearbyPortals.isEmpty() ? null : nearbyPortals.get(0);
            boolean shouldSpawnNew = (existingPortal == null);

            if (!shouldSpawnNew) {
                targetPos = existingPortal.getPos();
            }

            ((IEntityTeleportTracker) entity).infdimmod$setLastTeleportTick(currentTime);

            net.minecraft.world.TeleportTarget teleportTarget = new net.minecraft.world.TeleportTarget(
                    targetWorld,
                    targetPos,
                    entity.getVelocity(),
                    entity.getYaw(),
                    entity.getPitch(),
                    net.minecraft.world.TeleportTarget.NO_OP
            );

            Entity teleportedEntity = entity.teleportTo(teleportTarget);

            if (teleportedEntity != null && teleportedEntity != entity) {
                ((IEntityTeleportTracker) teleportedEntity).infdimmod$setLastTeleportTick(currentTime);
            }

            if (shouldSpawnNew) {
                BackPortal backentity = new BackPortal(ModEntities.BACK_PORTAL_ENTITY_TYPE, targetWorld);
                backentity.setSpawnTick(this.getSpawnTick());
                backentity.setDimensionCode(this.getWorld().getRegistryKey().getValue().toString());
                backentity.setDestinationPos(this.getPos());
                backentity.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, this.getYaw(), this.getPitch());

                targetWorld.spawnEntity(backentity);
            }
        }
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