package com.infdimmod.Entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class GreenPortal extends Entity {
    private int ticksExisted = 0;
    private static final int MAX_LIFETIME = 80;

    private float roll = 0.0f;

    public GreenPortal(EntityType<?> type, World world) {
        super(type, world);
    }

    public float getRoll() { return this.roll; }
    public void setRoll(float roll) { this.roll = roll; }

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

    @Override protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ticksExisted = nbt.getInt("TicksExisted");
        this.setYaw(nbt.getFloat("Yaw"));
        this.setPitch(nbt.getFloat("Pitch"));
        this.roll = nbt.getFloat("Roll"); // Читаем Roll
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("TicksExisted", this.ticksExisted);
        nbt.putFloat("Yaw", this.getYaw());
        nbt.putFloat("Pitch", this.getPitch());
        nbt.putFloat("Roll", this.roll); // Сохраняем Roll
    }
}