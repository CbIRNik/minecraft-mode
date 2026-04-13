package com.infdimmod.Entities.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;

public class DrunGuardEntity extends ZombieEntity {
    public DrunGuardEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }
}
