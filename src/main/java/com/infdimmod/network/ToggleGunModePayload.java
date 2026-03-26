package com.infdimmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ToggleGunModePayload() implements CustomPayload {
    public static final CustomPayload.Id<ToggleGunModePayload> ID = new CustomPayload.Id<>(Identifier.of("infdimmod", "toggle_gun_mode"));
    public static final PacketCodec<RegistryByteBuf, ToggleGunModePayload> CODEC = PacketCodec.unit(new ToggleGunModePayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}