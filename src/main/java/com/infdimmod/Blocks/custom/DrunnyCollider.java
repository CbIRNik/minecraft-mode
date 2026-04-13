package com.infdimmod.Blocks.custom;

import com.infdimmod.sound.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

public class DrunnyCollider extends Block {
    public static final MapCodec<DrunnyCollider> CODEC = createCodec(DrunnyCollider::new);
    private static final int SOUND_TICK_INTERVAL = 40;
    private static final double HEAR_RADIUS = 20.0;

    public DrunnyCollider(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (!world.isClient) {
            world.scheduleBlockTick(pos, this, SOUND_TICK_INTERVAL);
        }
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.scheduledTick(state, world, pos, random);

        boolean powered = world.isReceivingRedstonePower(pos);
        PlayerEntity nearestPlayer = world.getClosestPlayer(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                HEAR_RADIUS,
                false
        );
        boolean playerNearby = nearestPlayer != null;

        if (playerNearby && (powered || random.nextInt(5) == 0)) {
            float volume = powered ? 0.8F : 0.35F;
            float pitch = powered ? 1.05F : 0.95F + random.nextFloat() * 0.1F;
            world.playSound(
                    null,
                    pos,
                    ModSounds.DRUNNY_COLLIDER_AMBIENT,
                    SoundCategory.BLOCKS,
                    volume,
                    pitch
            );
        }

        world.scheduleBlockTick(pos, this, powered ? 20 : SOUND_TICK_INTERVAL);
    }
}
