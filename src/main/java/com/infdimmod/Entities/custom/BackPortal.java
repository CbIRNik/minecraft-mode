package com.infdimmod.Entities.custom;

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
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.joml.Vector3f;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

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
        ServerWorld targetWorld = resolveTargetWorld(server, targetCode);

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

    private ServerWorld resolveTargetWorld(MinecraftServer server, String targetCode) {
        Identifier potentialId = Identifier.tryParse(targetCode);
        if (potentialId != null && targetCode.contains(":")) {
            ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, potentialId));
            if (world != null) return world;
        }

        String fullCode = targetCode.length() < 12 ? (targetCode + "000000000000").substring(0, 12) : targetCode;
        String typeCode = fullCode.substring(1, 3);
        long targetSeed = this.getSeedFromCode(fullCode);

        Identifier targetDimId = Identifier.of("infdimmod", "dim_" + fullCode.toLowerCase());
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