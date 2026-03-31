package com.infdimmod.Entities.custom;

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

public class BackPortal extends Entity {
    private static final TrackedData<Vector3f> DESTINATION_VEC = DataTracker.registerData(BackPortal.class, TrackedDataHandlerRegistry.VECTOR3F);
    private static final TrackedData<String> DIMENSION_CODE = DataTracker.registerData(BackPortal.class, TrackedDataHandlerRegistry.STRING);

    private int age = 0;
    private static final int TOTAL_LIFETIME = 160;

    public BackPortal(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DIMENSION_CODE, "¯\\_(ツ)_/¯");
        builder.add(DESTINATION_VEC, new Vector3f());
    }

    public void setDimensionCode(String code) {
        this.getDataTracker().set(DIMENSION_CODE, code);
    }
    public String getDimensionCode() {
        return this.getDataTracker().get(DIMENSION_CODE);
    }

    public int getAge() { return this.age; }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (!this.getWorld().isClient && this.age == 1) {
            setChunkForceLoaded(true);
        }

        if (age <= 10) {
            for (int i = 0; i < 2; i++) {
                double offsetX = (random.nextDouble() - 0.5);
                double offsetY = (random.nextDouble() - 0.5);
                double offsetZ = (random.nextDouble() - 0.5);

                this.getWorld().addParticle(
                        ModParticles.GREEN_LIGHTNING,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ,
                        0, 0, 0
                );
            }
        }

        if (!this.getWorld().isClient && age >= TOTAL_LIFETIME) {
            setChunkForceLoaded(false);
            this.discard();
        }
    }

    public float getVisualScale(float tickDelta) {
        float exactAge = (float)this.age + tickDelta;
        int totalLifetime = TOTAL_LIFETIME;
        if (exactAge < 10f) {
            return exactAge / 10f;
        }
        if (exactAge > (totalLifetime - 10f)) {
            return MathHelper.sin((exactAge / 10f) * (float)Math.PI / 2f);
        }
        return 1.0f;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.age = nbt.getInt("Age");
        if (nbt.contains("DimensionCode")) {
            setDimensionCode(nbt.getString("DimensionCode"));
        }
        if (nbt.contains("destX")) {
            setDestinationPos(new Vec3d(nbt.getDouble("destX"), nbt.getDouble("destY"), nbt.getDouble("destZ")));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Age", this.age);
        nbt.putString("DimensionCode", getDimensionCode());
        Vec3d dest = getDestinationPos();
        nbt.putDouble("destX", dest.x);
        nbt.putDouble("destY", dest.y);
        nbt.putDouble("destZ", dest.z);
    }

    public void setDestinationPos(Vec3d pos) {
        this.getDataTracker().set(DESTINATION_VEC, new Vector3f((float)pos.x, (float)pos.y, (float)pos.z));
    }

    public Vec3d getDestinationPos() {
        Vector3f vec = this.getDataTracker().get(DESTINATION_VEC);
        return new Vec3d(vec.x, vec.y, vec.z);
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (this.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        MinecraftServer server = this.getServer();
        if (server == null) return;

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
        Vec3d targetPos = getDestinationPos();
        if (this.age >= 60) {
        serverPlayer.teleport(targetWorld, targetPos.x, targetPos.y, targetPos.z, serverPlayer.getYaw(), serverPlayer.getPitch());

            //!!!!!!!!!!!!!!ЧЕТ ПОЛОМАНО НЕ ВЛЕЗАЙ УБЬЕТ!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //ТЕПАЕТ НАЗАД ПРИ 2Й ТЕЛЕПОРТАЦИИ НА ТЕ ЖЕ КОРДЫ!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        }
    }

    public long getSeedFromCode(String code) {
        if (code == null || code.isEmpty()) return 0L;
        try {
            // Long.parseLong с радиалом 36 идеально подходит для 0-9 и a-z
            // Используем Math.abs, так как сиды в Minecraft обычно положительные
            // или обрабатываются как unsigned
            return Math.abs(Long.parseLong(code.toLowerCase(), 36));
        } catch (NumberFormatException e) {
            // Если в коде есть спецсимволы, не входящие в 0-9, a-z
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