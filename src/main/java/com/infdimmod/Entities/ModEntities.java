package com.infdimmod.Entities;

import com.infdimmod.Entities.custom.ArthurEntity;
import com.infdimmod.Entities.custom.BackPortal;
import com.infdimmod.Entities.custom.DrunGuardEntity;
import com.infdimmod.Entities.custom.DrunnyParticleOrbitEntity;
import com.infdimmod.Entities.custom.FatOmayGadnostEntity;
import com.infdimmod.Entities.custom.FogiApexEntity;
import com.infdimmod.Entities.custom.FogiEntity;
import com.infdimmod.Entities.custom.GreenPortal;
import com.infdimmod.Entities.custom.LittleTastyBabyEntity;
import com.infdimmod.Entities.custom.ResidentEntity;
import com.infdimmod.Entities.custom.StudentEntity;
import com.infdimmod.InfDimMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.IronGolemEntity;
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

    public static final EntityType<StudentEntity> STUDENT_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "student"),
            EntityType.Builder.create(StudentEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.95f)
                    .build()
    );

    public static final EntityType<ArthurEntity> ARTHUR_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "arthur"),
            EntityType.Builder.create(ArthurEntity::new, SpawnGroup.CREATURE)
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

    public static final EntityType<FogiApexEntity> FOGI_APEX_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "fogi_apex"),
            EntityType.Builder.create(FogiApexEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.85f, 2.8f)
                    .build()
    );

    public static final EntityType<DrunGuardEntity> DRUN_GUARD_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "drun_guard"),
            EntityType.Builder.create(DrunGuardEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f)
                    .build()
    );

    public static final EntityType<FatOmayGadnostEntity> FAT_OMAY_GADNOST_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "fat_omay_gadnost"),
            EntityType.Builder.create(FatOmayGadnostEntity::new, SpawnGroup.CREATURE)
                    .dimensions(1.8f, 3.1f)
                    .build()
    );

    public static final EntityType<LittleTastyBabyEntity> LITTLE_TASTY_BABY_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(InfDimMod.MOD_ID, "little_tasty_baby"),
            EntityType.Builder.create(LittleTastyBabyEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.45f, 0.75f)
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
        FabricDefaultAttributeRegistry.register(STUDENT_ENTITY_TYPE, VillagerEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(ARTHUR_ENTITY_TYPE, VillagerEntity.createVillagerAttributes());
        FabricDefaultAttributeRegistry.register(FOGI_ENTITY_TYPE, createFogiAttributes());
        FabricDefaultAttributeRegistry.register(FOGI_APEX_ENTITY_TYPE, createFogiApexAttributes());
        FabricDefaultAttributeRegistry.register(DRUN_GUARD_ENTITY_TYPE, createDrunGuardAttributes());
        FabricDefaultAttributeRegistry.register(FAT_OMAY_GADNOST_ENTITY_TYPE, createFatOmayAttributes());
        FabricDefaultAttributeRegistry.register(LITTLE_TASTY_BABY_ENTITY_TYPE, createLittleTastyBabyAttributes());

        registerGroundSpawn(RESIDENT_ENTITY_TYPE, MobEntity::canMobSpawn);
        registerGroundSpawn(STUDENT_ENTITY_TYPE, MobEntity::canMobSpawn);
        registerGroundSpawn(ARTHUR_ENTITY_TYPE, MobEntity::canMobSpawn);
        registerGroundSpawn(LITTLE_TASTY_BABY_ENTITY_TYPE, MobEntity::canMobSpawn);
        registerGroundSpawn(FAT_OMAY_GADNOST_ENTITY_TYPE, MobEntity::canMobSpawn);
        registerGroundSpawn(FOGI_ENTITY_TYPE, HostileEntity::canSpawnInDark);
        registerGroundSpawn(FOGI_APEX_ENTITY_TYPE, HostileEntity::canSpawnInDark);
        registerGroundSpawn(DRUN_GUARD_ENTITY_TYPE, HostileEntity::canSpawnInDark);
    }

    private static DefaultAttributeContainer.Builder createFogiAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.MAX_HEALTH, 30.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.ARMOR, 4.0);
    }

    private static DefaultAttributeContainer.Builder createFogiApexAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.MAX_HEALTH, 90.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.ATTACK_DAMAGE, 12.0)
                .add(EntityAttributes.FOLLOW_RANGE, 56.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.45)
                .add(EntityAttributes.ARMOR, 8.0);
    }

    private static DefaultAttributeContainer.Builder createDrunGuardAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.MAX_HEALTH, 42.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.ARMOR, 10.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    private static DefaultAttributeContainer.Builder createFatOmayAttributes() {
        return IronGolemEntity.createIronGolemAttributes()
                .add(EntityAttributes.MAX_HEALTH, 220.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.2)
                .add(EntityAttributes.ATTACK_DAMAGE, 20.0)
                .add(EntityAttributes.FOLLOW_RANGE, 28.0)
                .add(EntityAttributes.ARMOR, 12.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    private static DefaultAttributeContainer.Builder createLittleTastyBabyAttributes() {
        return ChickenEntity.createChickenAttributes()
                .add(EntityAttributes.MAX_HEALTH, 12.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3);
    }

    private static <T extends MobEntity> void registerGroundSpawn(EntityType<T> entityType, SpawnRestriction.SpawnPredicate<T> predicate) {
        SpawnRestriction.register(
                entityType,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                predicate
        );
    }
}
