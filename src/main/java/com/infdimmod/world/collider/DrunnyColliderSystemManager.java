package com.infdimmod.world.collider;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.Entities.ModEntities;
import com.infdimmod.Entities.custom.DrunGuardEntity;
import com.infdimmod.Entities.custom.DrunnyParticleOrbitEntity;
import com.infdimmod.world.generator.DeterministicChaosGenerator;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DrunnyColliderSystemManager {
    private static final String STATE_ID = "drunny_collider_core_state";
    private static final int RADIATION_RADIUS = 24;
    private static final int GUARD_RADIUS = 20;
    private static final int OVERHEAT_THRESHOLD = 1600;
    private static final float MELTDOWN_EXPLOSION_POWER = 36.0F;

    private DrunnyColliderSystemManager() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            if (!(world.getChunkManager().getChunkGenerator() instanceof DeterministicChaosGenerator generator)) {
                continue;
            }
            tickWorld(world, generator);
        }
    }

    private static void tickWorld(ServerWorld world, DeterministicChaosGenerator generator) {
        if (world.getPlayers().isEmpty()) {
            return;
        }

        long time = world.getTime();
        Set<BlockPos> activeCores = new HashSet<>();

        for (var player : world.getPlayers()) {
            ChunkPos chunkPos = new ChunkPos(player.getBlockPos());
            List<ChunkPos> nearbyCoreChunks = DrunnyColliderLayout.findNearbyCoreChunks(chunkPos, generator.getWorldSeed(), 9);
            for (ChunkPos coreChunk : nearbyCoreChunks) {
                activeCores.add(DrunnyColliderLayout.coreBlockPos(coreChunk, generator.getColliderCoreY()));
            }
        }

        if (activeCores.isEmpty()) {
            return;
        }

        ColliderCoreState state = getState(world);

        for (BlockPos corePos : activeCores) {
            if (!world.isChunkLoaded(corePos)) {
                continue;
            }

            if (state.isExploded(corePos)) {
                continue;
            }

            if (!world.getBlockState(corePos).isOf(ModBlocks.DRUNNY_ATOM)) {
                continue;
            }

            if (time % 20 == 0) {
                applyRadiation(world, corePos);
                spawnOrbitParticleIfMissing(world, corePos);
                spawnGuards(world, corePos);
            }

            if (time % 10 == 0) {
                processOverheat(world, corePos, state);
            }
        }
    }

    private static void applyRadiation(ServerWorld world, BlockPos corePos) {
        Box auraBox = Box.of(corePos.toCenterPos(), RADIATION_RADIUS * 2.0, 26.0, RADIATION_RADIUS * 2.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, auraBox, entity -> entity.isAlive() && !entity.isSpectator());
        for (LivingEntity entity : targets) {
            if (entity instanceof DrunGuardEntity) {
                continue;
            }
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0, true, true, true));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, true, false, true));
        }
    }

    private static void spawnOrbitParticleIfMissing(ServerWorld world, BlockPos corePos) {
        Box search = Box.of(corePos.toCenterPos(), 12.0, 8.0, 12.0);
        boolean present = !world.getEntitiesByClass(DrunnyParticleOrbitEntity.class, search, entity -> entity.isAlive()).isEmpty();
        if (present) {
            return;
        }

        DrunnyParticleOrbitEntity particle = new DrunnyParticleOrbitEntity(ModEntities.DRUNNY_PARTICLE_ORBIT_ENTITY_TYPE, world);
        particle.setCorePos(corePos);
        world.spawnEntity(particle);
    }

    private static void spawnGuards(ServerWorld world, BlockPos corePos) {
        Box guardArea = Box.of(corePos.toCenterPos(), GUARD_RADIUS * 2.0, 12.0, GUARD_RADIUS * 2.0);
        List<DrunGuardEntity> guards = world.getEntitiesByClass(DrunGuardEntity.class, guardArea, entity -> entity.isAlive());
        int guardTargetCount = 4;
        if (guards.size() >= guardTargetCount) {
            return;
        }

        for (int i = guards.size(); i < guardTargetCount; i++) {
            int offsetX = (i % 2 == 0 ? 1 : -1) * (8 + world.random.nextBetween(0, 4));
            int offsetZ = (i < 2 ? 1 : -1) * (8 + world.random.nextBetween(0, 4));
            BlockPos spawnPos = corePos.add(offsetX, 0, offsetZ);

            DrunGuardEntity guard = new DrunGuardEntity(ModEntities.DRUN_GUARD_ENTITY_TYPE, world);
            guard.refreshPositionAndAngles(spawnPos.getX() + 0.5, corePos.getY(), spawnPos.getZ() + 0.5, world.random.nextFloat() * 360.0F, 0.0F);
            guard.setGuardPost(spawnPos.withY(corePos.getY()));
            guard.setPersistent();

            if (world.isSpaceEmpty(guard)) {
                guard.getAttributeInstance(EntityAttributes.FOLLOW_RANGE).setBaseValue(48.0);
                guard.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(30.0);
                guard.setHealth(30.0F);
                world.spawnEntity(guard);
            }
        }
    }

    private static void processOverheat(ServerWorld world, BlockPos corePos, ColliderCoreState state) {
        int heat = state.getOverheat(corePos);

        Box chamber = Box.of(corePos.toCenterPos(), 20.0, 16.0, 20.0);
        int nearbyTargets = world.getEntitiesByClass(LivingEntity.class, chamber, entity -> entity.isAlive() && !entity.isSpectator() && !(entity instanceof DrunGuardEntity)).size();

        if (nearbyTargets > 0) {
            heat += nearbyTargets * 4;
        } else {
            heat = Math.max(0, heat - 3);
        }

        if (isCoreControlPowered(world, corePos)) {
            heat += 12;
        }

        state.setOverheat(corePos, heat);

        if (heat >= OVERHEAT_THRESHOLD) {
            triggerMeltdown(world, corePos, state);
        }
    }

    private static boolean isCoreControlPowered(ServerWorld world, BlockPos corePos) {
        return world.isReceivingRedstonePower(corePos.add(9, 1, 0))
                || world.isReceivingRedstonePower(corePos.add(-9, 1, 0))
                || world.isReceivingRedstonePower(corePos.add(0, 1, 9))
                || world.isReceivingRedstonePower(corePos.add(0, 1, -9));
    }

    private static void triggerMeltdown(ServerWorld world, BlockPos corePos, ColliderCoreState state) {
        state.markExploded(corePos);
        state.setOverheat(corePos, 0);
        world.createExplosion(
                null,
                corePos.getX() + 0.5,
                corePos.getY() + 0.5,
                corePos.getZ() + 0.5,
                MELTDOWN_EXPLOSION_POWER,
                World.ExplosionSourceType.BLOCK
        );
        world.removeBlock(corePos, false);
    }

    private static ColliderCoreState getState(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(ColliderCoreState.TYPE, STATE_ID);
    }

    private static final class ColliderCoreState extends PersistentState {
        private static final String ENTRIES_KEY = "entries";
        private static final String CORE_POS_KEY = "core_pos";
        private static final String OVERHEAT_KEY = "overheat";
        private static final String EXPLODED_KEY = "exploded";

        private static final Type<ColliderCoreState> TYPE = new Type<>(
                ColliderCoreState::new,
                ColliderCoreState::fromNbt,
                DataFixTypes.LEVEL
        );

        private final Map<Long, Integer> overheatByCore = new ConcurrentHashMap<>();
        private final Set<Long> explodedCores = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

        private ColliderCoreState() {
        }

        private static ColliderCoreState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            ColliderCoreState state = new ColliderCoreState();
            NbtList entries = nbt.getList(ENTRIES_KEY, net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < entries.size(); i++) {
                NbtCompound entry = entries.getCompound(i);
                long corePos = entry.getLong(CORE_POS_KEY);
                int heat = Math.max(0, entry.getInt(OVERHEAT_KEY));
                boolean exploded = entry.getBoolean(EXPLODED_KEY);
                if (heat > 0) {
                    state.overheatByCore.put(corePos, heat);
                }
                if (exploded) {
                    state.explodedCores.add(corePos);
                }
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            NbtList entries = new NbtList();
            Set<Long> allKeys = new HashSet<>();
            allKeys.addAll(overheatByCore.keySet());
            allKeys.addAll(explodedCores);

            for (long key : allKeys) {
                NbtCompound entry = new NbtCompound();
                entry.putLong(CORE_POS_KEY, key);
                entry.putInt(OVERHEAT_KEY, overheatByCore.getOrDefault(key, 0));
                entry.putBoolean(EXPLODED_KEY, explodedCores.contains(key));
                entries.add(entry);
            }

            nbt.put(ENTRIES_KEY, entries);
            return nbt;
        }

        private int getOverheat(BlockPos corePos) {
            return overheatByCore.getOrDefault(corePos.asLong(), 0);
        }

        private void setOverheat(BlockPos corePos, int heat) {
            long key = corePos.asLong();
            int clamped = Math.max(0, heat);
            if (clamped == 0) {
                if (overheatByCore.remove(key) != null) {
                    markDirty();
                }
                return;
            }
            Integer previous = overheatByCore.put(key, clamped);
            if (!Objects.equals(previous, clamped)) {
                markDirty();
            }
        }

        private boolean isExploded(BlockPos corePos) {
            return explodedCores.contains(corePos.asLong());
        }

        private void markExploded(BlockPos corePos) {
            if (explodedCores.add(corePos.asLong())) {
                markDirty();
            }
        }
    }
}
