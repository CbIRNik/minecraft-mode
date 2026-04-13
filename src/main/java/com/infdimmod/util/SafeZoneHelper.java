package com.infdimmod.util;

import com.infdimmod.burmaldeniya.BurmaldushkinStructures;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class SafeZoneHelper {
    public static boolean isInsideDormitory(ServerWorld world, BlockPos pos) {
        var strAccessor = world.getStructureAccessor();
        var registry = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE);
        
        var d5 = registry.getEntry(BurmaldushkinStructures.DORMITORY_5);
        if (d5.isPresent() && strAccessor.getStructureAt(pos, d5.get().value()).hasChildren()) {
            return true;
        }

        var d9 = registry.getEntry(BurmaldushkinStructures.DORMITORY_9);
        if (d9.isPresent() && strAccessor.getStructureAt(pos, d9.get().value()).hasChildren()) {
            return true;
        }

        return false;
    }
}
