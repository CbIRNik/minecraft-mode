package com.infdimmod.util;

import com.infdimmod.world.generator.DeterministicChaosGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class SafeZoneHelper {
    public static boolean isInsideDormitory(ServerWorld world, BlockPos pos) {
        if (world.getChunkManager().getChunkGenerator() instanceof DeterministicChaosGenerator generator) {
            return generator.isInsideDormitory(pos);
        }
        return false;
    }
}
