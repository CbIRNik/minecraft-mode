package com.infdimmod.Entities;

import com.infdimmod.Entities.custom.BackPortal;
import com.infdimmod.Entities.custom.DrunGuardEntity;
import com.infdimmod.Entities.custom.DrunnyParticleOrbitEntity;
import com.infdimmod.Entities.custom.FogiEntity;
import com.infdimmod.Entities.custom.GreenPortal;
import com.infdimmod.Entities.custom.ResidentEntity;
import com.infdimmod.InfDimMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

public class ModEntities {

    public static final EntityType<GreenPortal> GREEN_PORTAL_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "green_portal"),
            EntityType.Builder.create(GreenPortal::new, SpawnGroup.MISC)
                    .dimensions(0.8f, 1.6f)
                    .build()
    );

    public static final EntityType<BackPortal> BACK_PORTAL_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "back_portal"),
            EntityType.Builder.create(BackPortal::new, SpawnGroup.MISC)
                    .dimensions(0.8f, 1.6f)
                    .build()
    );

    public static final EntityType<ResidentEntity> RESIDENT_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "resident"),
            EntityType.Builder.create(ResidentEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.95f)
                    .build()
    );

    public static final EntityType<FogiEntity> FOGI_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "fogi"),
            EntityType.Builder.create(FogiEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f)
                    .build()
    );

    public static final EntityType<DrunGuardEntity> DRUN_GUARD_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "drun_guard"),
            EntityType.Builder.create(DrunGuardEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f)
                    .build()
    );

    public static final EntityType<DrunnyParticleOrbitEntity> DRUNNY_PARTICLE_ORBIT_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "drunny_particle_orbit"),
            EntityType.Builder.<DrunnyParticleOrbitEntity>create(DrunnyParticleOrbitEntity::new, SpawnGroup.MISC)
                    .dimensions(0.2f, 0.2f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build()
    );

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(RESIDENT_ENTITY_TYPE, VillagerEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(FOGI_ENTITY_TYPE, ZombieEntity.createZombieAttributes());
        FabricDefaultAttributeRegistry.register(DRUN_GUARD_ENTITY_TYPE, ZombieEntity.createZombieAttributes());

        SpawnRestriction.register(
                RESIDENT_ENTITY_TYPE,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                MobEntity::canMobSpawn
        );

        SpawnRestriction.register(
                FOGI_ENTITY_TYPE,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                HostileEntity::canSpawnInDark
        );

        SpawnRestriction.register(
                DRUN_GUARD_ENTITY_TYPE,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                HostileEntity::canSpawnInDark
        );
    }
}
