package com.infdimmod.Entities.custom;

import com.infdimmod.Entities.ModEntities;
import com.infdimmod.particle.ModParticles;
import com.infdimmod.util.IEntityTeleportTracker;
import com.infdimmod.world.generator.DimTypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
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
    private boolean backPortalCreated = false;

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
            if (!backPortalCreated && currentAge >= 20) {
                prepareTargetLocation();
                backPortalCreated = true;
            }

            if (currentAge == 1) setChunkForceLoaded(true);
            if (currentAge >= TOTAL_LIFETIME) {
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

    private void prepareTargetLocation() {
        MinecraftServer server = this.getServer();
        if (server == null) return;

        ServerWorld targetWorld = resolveTargetWorld(server, getDimensionCode());
        if (targetWorld != null) {
            Vec3d targetPos = getPortalTargetPos();

            setTargetChunkForceLoaded(targetWorld, targetPos, true);

            net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(
                    targetPos.x - 2, targetPos.y - 2, targetPos.z - 2,
                    targetPos.x + 2, targetPos.y + 2, targetPos.z + 2
            );

            if (targetWorld.getEntitiesByClass(BackPortal.class, searchBox, e -> true).isEmpty()) {
                BackPortal backentity = new BackPortal(ModEntities.BACK_PORTAL_ENTITY_TYPE, targetWorld);
                backentity.setSpawnTick(this.getSpawnTick());
                backentity.setDimensionCode(this.getWorld().getRegistryKey().getValue().toString());
                backentity.setDestinationPos(this.getPos());
                backentity.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, this.getYaw(), this.getPitch());
                targetWorld.spawnEntity(backentity);
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
        if (entity instanceof GreenPortal || entity instanceof BackPortal) return;
        if (getPortalAge() < 10) return;

        MinecraftServer server = this.getServer();
        if (server == null) return;

        if (!(entity instanceof IEntityTeleportTracker tracker)) return;
        if (server.getOverworld().getTime() - tracker.infdimmod$getLastTeleportTick() < 30) return;

        ServerWorld targetWorld = resolveTargetWorld(server, getDimensionCode());
        if (targetWorld != null) {
            Vec3d targetPos = getPortalTargetPos();

            List<BackPortal> nearby = targetWorld.getEntitiesByClass(BackPortal.class,
                    new net.minecraft.util.math.Box(targetPos.add(-2,-2,-2), targetPos.add(2,2,2)), e -> true);

            if (!nearby.isEmpty()) targetPos = nearby.get(0).getPos();

            long currentTime = server.getOverworld().getTime();
            tracker.infdimmod$setLastTeleportTick(currentTime);

            net.minecraft.world.TeleportTarget teleportTarget = new net.minecraft.world.TeleportTarget(
                    targetWorld, targetPos, entity.getVelocity(), entity.getYaw(), entity.getPitch(),
                    net.minecraft.world.TeleportTarget.NO_OP
            );

            Entity result = entity.teleportTo(teleportTarget);
            if (result != null) {
                ((IEntityTeleportTracker) result).infdimmod$setLastTeleportTick(currentTime);
            }
        }
    }

    private ServerWorld resolveTargetWorld(MinecraftServer server, String targetCode) {
        RegistryKey<World> vanillaKey = switch (targetCode.toLowerCase()) {
            case "overworld" -> World.OVERWORLD;
            case "nether" -> World.NETHER;
            case "end" -> World.END;
            default -> null;
        };
        if (vanillaKey != null) return server.getWorld(vanillaKey);

        String typeCode = targetCode.substring(1, 3);
        long targetSeed = this.getSeedFromCode(targetCode);

        Identifier targetDimId = Identifier.of("infdimmod", "dim_" + targetCode.toLowerCase());
        Fantasy fantasy = Fantasy.get(server);

        RegistryWrapper.Impl<Biome> biomeLookup = server.getRegistryManager().getWrapperOrThrow(RegistryKeys.BIOME);

        ChunkGenerator generator = DimTypeRegistry.get(typeCode).createGenerator(server, targetSeed, biomeLookup);

        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setDimensionType(DimensionTypes.OVERWORLD)
                .setSeed(targetSeed)
                .setGenerator(generator);

        copyGameRules(server.getOverworld().getGameRules(), config);

        return fantasy.getOrOpenPersistentWorld(targetDimId, config).asWorld();
    }

    private void copyGameRules(GameRules source, RuntimeWorldConfig targetConfig) {
        source.accept(new GameRules.Visitor() {
            @Override
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                if (key == GameRules.SPAWN_CHUNK_RADIUS) {
                    targetConfig.setGameRule(GameRules.SPAWN_CHUNK_RADIUS, 0);
                    return;
                }

                T rule = source.get(key);
                if (rule instanceof GameRules.BooleanRule boolRule) {
                    targetConfig.setGameRule((GameRules.Key<GameRules.BooleanRule>) key, boolRule.get());
                } else if (rule instanceof GameRules.IntRule intRule) {
                    targetConfig.setGameRule((GameRules.Key<GameRules.IntRule>) key, intRule.get());
                }
            }
        });
    }

    public long getSeedFromCode(String code) {
        if (code == null || code.isEmpty()) return 1_000_000_000_000_000_000L;
        if (code.length() < 12) {
            code = String.format("%12s", code).replace(' ', '0');
        } else if (code.length() > 12) {
            code = code.substring(0, 12);
        }
        long hash = 0;
        for (int i = 0; i < code.length(); i++) {
            hash = 63L * hash + code.charAt(i);
        }
        long base = 1_000_000_000_000_000_000L;
        long range = 8_223_372_036_854_775_807L;
        return base + (Math.abs(hash) % range);
    }

    private void setChunkForceLoaded(boolean forced) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = this.getBlockPos();
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            serverWorld.setChunkForced(chunkX, chunkZ, forced);
        }
    }

    private void setTargetChunkForceLoaded(ServerWorld targetWorld, Vec3d pos, boolean forced) {
        int chunkX = (int)pos.x >> 4;
        int chunkZ = (int)pos.z >> 4;
        targetWorld.setChunkForced(chunkX, chunkZ, forced);
    }

    @Override
    public void onRemoved() {
        if (!this.getWorld().isClient) {
            setChunkForceLoaded(false);
            MinecraftServer server = this.getServer();
            if (server != null) {
                ServerWorld targetWorld = resolveTargetWorld(server, getDimensionCode());
                if (targetWorld != null) {
                    setTargetChunkForceLoaded(targetWorld, getPortalTargetPos(), false);
                }
            }
        }
        super.onRemoved();
    }

    @Override
    protected net.minecraft.util.math.Box calculateBoundingBox() {
        double halfW = (18.0 / 16.0) / 2.0;
        double halfH = (30.0 / 16.0) / 2.0;
        double halfT = 0.05;

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

    @Override
    public boolean isFireImmune() {
        return true;
    }
}