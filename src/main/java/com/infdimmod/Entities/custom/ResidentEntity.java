package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;

public class ResidentEntity extends VillagerEntity {
    public ResidentEntity(EntityType<? extends VillagerEntity> entityType, World world) {
        super(entityType, world);
    }
}
