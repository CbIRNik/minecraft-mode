package com.infdimmod.burmaldeniya;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MurinoBiomeHelper {
    private MurinoBiomeHelper() {
    }

    public static boolean isMurino(World world, BlockPos pos) {
        return world != null && pos != null && world.getBiome(pos).matchesKey(MurinoWorldgenHooks.MURINO_BIOME_KEY);
    }
}
