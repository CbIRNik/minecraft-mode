package com.infdimmod.Entities.custom;

import com.infdimmod.Blocks.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class DrunnyParticleOrbitEntity extends Entity {
    private static final String CORE_X_KEY = "core_x";
    private static final String CORE_Y_KEY = "core_y";
    private static final String CORE_Z_KEY = "core_z";
    private static final String ORBIT_ANGLE_KEY = "orbit_angle";
    private static final String ORBIT_RADIUS_KEY = "orbit_radius";

    private BlockPos corePos = BlockPos.ORIGIN;
    private float orbitAngle;
    private float orbitRadius = 3.8F;

    public DrunnyParticleOrbitEntity(EntityType<? extends DrunnyParticleOrbitEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.toImmutable();
        this.setPosition(corePos.getX() + 0.5, corePos.getY() + 1.2, corePos.getZ() + 0.5);
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }

        if (this.age > 20 * 60 * 12) {
            this.discard();
            return;
        }

        if (this.corePos == null || !this.getWorld().getBlockState(this.corePos).isOf(ModBlocks.DRUNNY_ATOM)) {
            this.discard();
            return;
        }

        orbitAngle += 0.11F;
        double x = corePos.getX() + 0.5 + MathHelper.cos(orbitAngle) * orbitRadius;
        double z = corePos.getZ() + 0.5 + MathHelper.sin(orbitAngle) * orbitRadius;
        double y = corePos.getY() + 1.45 + MathHelper.sin((this.age + orbitAngle * 11.0F) * 0.12F) * 0.4;

        this.setPosition(x, y, z);
        this.setVelocity(0.0, 0.0, 0.0);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        int coreX = nbt.getInt(CORE_X_KEY);
        int coreY = nbt.getInt(CORE_Y_KEY);
        int coreZ = nbt.getInt(CORE_Z_KEY);
        this.corePos = new BlockPos(coreX, coreY, coreZ);
        this.orbitAngle = nbt.getFloat(ORBIT_ANGLE_KEY);
        this.orbitRadius = Math.max(1.5F, nbt.getFloat(ORBIT_RADIUS_KEY));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt(CORE_X_KEY, this.corePos.getX());
        nbt.putInt(CORE_Y_KEY, this.corePos.getY());
        nbt.putInt(CORE_Z_KEY, this.corePos.getZ());
        nbt.putFloat(ORBIT_ANGLE_KEY, this.orbitAngle);
        nbt.putFloat(ORBIT_RADIUS_KEY, this.orbitRadius);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }
}
