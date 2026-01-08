package com.infdimmod.items.custom;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class PortalGunData extends PersistentState {
    private static final String DATA_NAME = "portal_gun_data";

    // Храним данные по ID мира (например, "minecraft:overworld")
    private final Map<String, Map<BlockPos, Long>> placedBlocksByDimension = new HashMap<>();

    public PortalGunData() {
        super();
    }

    public static PortalGunData get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager()
                .getOrCreate(new PersistentState.Type<>(
                        PortalGunData::new,
                        PortalGunData::fromNbt,
                        null
                ), DATA_NAME);
    }

    public void addBlock(ServerWorld world, BlockPos pos, long removalTime) {
        String dimensionId = world.getRegistryKey().getValue().toString();
        placedBlocksByDimension.computeIfAbsent(dimensionId, k -> new HashMap<>())
                .put(pos.toImmutable(), removalTime);
        markDirty();
    }

    public void removeBlock(ServerWorld world, BlockPos pos) {
        String dimensionId = world.getRegistryKey().getValue().toString();
        Map<BlockPos, Long> worldBlocks = placedBlocksByDimension.get(dimensionId);
        if (worldBlocks != null) {
            worldBlocks.remove(pos);
            if (worldBlocks.isEmpty()) {
                placedBlocksByDimension.remove(dimensionId);
            }
            markDirty();
        }
    }

    public Map<String, Map<BlockPos, Long>> getPlacedBlocksByDimension() {
        return placedBlocksByDimension;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        NbtList worldsList = new NbtList();

        for (Map.Entry<String, Map<BlockPos, Long>> worldEntry : placedBlocksByDimension.entrySet()) {
            NbtCompound worldNbt = new NbtCompound();
            worldNbt.putString("dimension", worldEntry.getKey());

            NbtList blocksList = new NbtList();
            for (Map.Entry<BlockPos, Long> blockEntry : worldEntry.getValue().entrySet()) {
                NbtCompound blockNbt = new NbtCompound();
                blockNbt.putLong("pos", blockEntry.getKey().asLong());
                blockNbt.putLong("removalTime", blockEntry.getValue());
                blocksList.add(blockNbt);
            }

            worldNbt.put("blocks", blocksList);
            worldsList.add(worldNbt);
        }

        nbt.put("worlds", worldsList);
        return nbt;
    }

    public static PortalGunData fromNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup) {
        PortalGunData data = new PortalGunData();

        if (nbt.contains("worlds", NbtElement.LIST_TYPE)) {
            NbtList worldsList = nbt.getList("worlds", NbtElement.COMPOUND_TYPE);

            for (int i = 0; i < worldsList.size(); i++) {
                NbtCompound worldNbt = worldsList.getCompound(i);
                String dimensionId = worldNbt.getString("dimension");

                Map<BlockPos, Long> worldBlocks = new HashMap<>();
                NbtList blocksList = worldNbt.getList("blocks", NbtElement.COMPOUND_TYPE);

                for (int j = 0; j < blocksList.size(); j++) {
                    NbtCompound blockNbt = blocksList.getCompound(j);
                    BlockPos pos = BlockPos.fromLong(blockNbt.getLong("pos"));
                    long removalTime = blockNbt.getLong("removalTime");
                    worldBlocks.put(pos, removalTime);
                }

                data.placedBlocksByDimension.put(dimensionId, worldBlocks);
            }
        }

        return data;
    }
}