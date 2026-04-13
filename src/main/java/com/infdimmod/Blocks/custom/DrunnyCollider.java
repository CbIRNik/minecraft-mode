package com.infdimmod.Blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;

public class DrunnyCollider extends Block {
    public static final MapCodec<DrunnyCollider> CODEC = createCodec(DrunnyCollider::new);

    public DrunnyCollider(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }
}
