package com.infdimmod.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class GreenPortal extends Entity {
    private int ticksExisted = 0;
    private static final int MAX_LIFETIME = 80;

    public GreenPortal(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            ticksExisted++;
            if (ticksExisted >= MAX_LIFETIME) {
                this.discard();
            }
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ticksExisted = nbt.getInt("TicksExisted");
        // Загружаем углы поворота
        if (nbt.contains("Yaw")) this.setYaw(nbt.getFloat("Yaw"));
        if (nbt.contains("Pitch")) this.setPitch(nbt.getFloat("Pitch"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("TicksExisted", this.ticksExisted);
        // Сохраняем углы поворота
        nbt.putFloat("Yaw", this.getYaw());
        nbt.putFloat("Pitch", this.getPitch());
    }
}