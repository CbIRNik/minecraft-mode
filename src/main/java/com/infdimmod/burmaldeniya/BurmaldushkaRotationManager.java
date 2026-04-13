package com.infdimmod.burmaldeniya;

import com.infdimmod.InfDimMod;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BurmaldushkaRotationManager {
    private static final String STATE_ID = "burmaldushka_rotation_state";
    private static final long ROTATION_INTERVAL_MS = 20L * 60L * 1000L;
    private static final int CRAFTER_INPUT_SLOTS = 6;

    private static final List<List<Identifier>> ROTATIONS = List.of(
            List.of(
                    Identifier.of(InfDimMod.MOD_ID, "sausage"),
                    Identifier.of("minecraft", "slime_ball"),
                    Identifier.of("minecraft", "amethyst_shard"),
                    Identifier.of("minecraft", "blaze_powder"),
                    Identifier.of("minecraft", "ender_pearl"),
                    Identifier.of("minecraft", "ghast_tear")
            ),
            List.of(
                    Identifier.of(InfDimMod.MOD_ID, "sausage"),
                    Identifier.of("minecraft", "slime_ball"),
                    Identifier.of("minecraft", "amethyst_shard"),
                    Identifier.of("minecraft", "blaze_powder"),
                    Identifier.of("minecraft", "echo_shard"),
                    Identifier.of("minecraft", "golden_carrot")
            ),
            List.of(
                    Identifier.of(InfDimMod.MOD_ID, "sausage"),
                    Identifier.of("minecraft", "ender_pearl"),
                    Identifier.of("minecraft", "echo_shard"),
                    Identifier.of("minecraft", "ghast_tear"),
                    Identifier.of("minecraft", "blaze_powder"),
                    Identifier.of("minecraft", "amethyst_shard")
            ),
            List.of(
                    Identifier.of(InfDimMod.MOD_ID, "sausage"),
                    Identifier.of("minecraft", "golden_carrot"),
                    Identifier.of("minecraft", "ghast_tear"),
                    Identifier.of("minecraft", "slime_ball"),
                    Identifier.of("minecraft", "echo_shard"),
                    Identifier.of("minecraft", "ender_pearl")
            )
    );

    private static final Set<Identifier> ALLOWED_ROTATION_ITEMS = ROTATIONS.stream()
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private BurmaldushkaRotationManager() {
    }

    public static void tick(MinecraftServer server) {
        getState(server).rotateIfNeeded();
    }

    public static BurmaldushkaRotationSnapshot getSnapshot(World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return new BurmaldushkaRotationSnapshot(0, 1, 0L);
        }

        RotationState state = getState(serverWorld.getServer());
        return new BurmaldushkaRotationSnapshot(state.rotationIndex, state.rotationVersion, state.nextRotationEpochMs);
    }

    public static boolean matchesCurrentRotation(RecipeInput input, World world) {
        if (world.isClient || !(world instanceof ServerWorld)) {
            return false;
        }

        BurmaldushkaRotationSnapshot snapshot = getSnapshot(world);
        return matchesRotation(input, snapshot.rotationIndex());
    }

    public static boolean isAllowedRotationItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return ALLOWED_ROTATION_ITEMS.contains(Registries.ITEM.getId(stack.getItem()));
    }

    public static long getSecondsUntilNextRotation(World world) {
        if (world == null || world.isClient) {
            return 0L;
        }
        BurmaldushkaRotationSnapshot snapshot = getSnapshot(world);
        long deltaMillis = Math.max(0L, snapshot.nextRotationEpochMs() - System.currentTimeMillis());
        return deltaMillis / 1000L;
    }

    private static boolean matchesRotation(RecipeInput input, int rotationIndex) {
        List<Identifier> requiredItems = ROTATIONS.get(Math.floorMod(rotationIndex, ROTATIONS.size()));
        Map<Identifier, Integer> requiredCounts = new HashMap<>();
        for (Identifier id : requiredItems) {
            requiredCounts.merge(id, 1, Integer::sum);
        }

        int nonEmptyStacks = 0;
        for (int slot = 0; slot < CRAFTER_INPUT_SLOTS; slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            nonEmptyStacks++;
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            if (!ALLOWED_ROTATION_ITEMS.contains(itemId)) {
                return false;
            }

            Integer remaining = requiredCounts.get(itemId);
            if (remaining == null || remaining <= 0) {
                return false;
            }

            requiredCounts.put(itemId, remaining - 1);
        }

        if (nonEmptyStacks != requiredItems.size()) {
            return false;
        }

        return requiredCounts.values().stream().allMatch(count -> count == 0);
    }

    private static RotationState getState(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(RotationState.TYPE, STATE_ID);
    }

    public record BurmaldushkaRotationSnapshot(int rotationIndex, int rotationVersion, long nextRotationEpochMs) {
    }

    private static final class RotationState extends PersistentState {
        private static final String ROTATION_INDEX_KEY = "rotation_index";
        private static final String ROTATION_VERSION_KEY = "rotation_version";
        private static final String NEXT_ROTATION_EPOCH_MS_KEY = "next_rotation_epoch_ms";

        private static final Type<RotationState> TYPE = new Type<>(
                RotationState::create,
                RotationState::fromNbt,
                DataFixTypes.LEVEL
        );

        private int rotationIndex;
        private int rotationVersion;
        private long nextRotationEpochMs;

        private RotationState(int rotationIndex, int rotationVersion, long nextRotationEpochMs) {
            this.rotationIndex = Math.floorMod(rotationIndex, ROTATIONS.size());
            this.rotationVersion = Math.max(1, rotationVersion);
            this.nextRotationEpochMs = nextRotationEpochMs;
        }

        private static RotationState create() {
            long now = System.currentTimeMillis();
            return new RotationState(0, 1, now + ROTATION_INTERVAL_MS);
        }

        private static RotationState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            int rotationIndex = nbt.contains(ROTATION_INDEX_KEY) ? nbt.getInt(ROTATION_INDEX_KEY) : 0;
            int rotationVersion = nbt.contains(ROTATION_VERSION_KEY) ? nbt.getInt(ROTATION_VERSION_KEY) : 1;
            long nextRotationAt = nbt.contains(NEXT_ROTATION_EPOCH_MS_KEY)
                    ? nbt.getLong(NEXT_ROTATION_EPOCH_MS_KEY)
                    : System.currentTimeMillis() + ROTATION_INTERVAL_MS;
            return new RotationState(rotationIndex, rotationVersion, nextRotationAt);
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            nbt.putInt(ROTATION_INDEX_KEY, rotationIndex);
            nbt.putInt(ROTATION_VERSION_KEY, rotationVersion);
            nbt.putLong(NEXT_ROTATION_EPOCH_MS_KEY, nextRotationEpochMs);
            return nbt;
        }

        private void rotateIfNeeded() {
            long now = System.currentTimeMillis();
            if (nextRotationEpochMs <= 0L) {
                nextRotationEpochMs = now + ROTATION_INTERVAL_MS;
                markDirty();
                return;
            }

            boolean changed = false;
            while (now >= nextRotationEpochMs) {
                rotationIndex = (rotationIndex + 1) % ROTATIONS.size();
                rotationVersion++;
                nextRotationEpochMs += ROTATION_INTERVAL_MS;
                changed = true;
            }

            if (changed) {
                markDirty();
            }
        }
    }
}
