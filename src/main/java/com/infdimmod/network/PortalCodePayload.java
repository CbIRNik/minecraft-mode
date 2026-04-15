package com.infdimmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortalCodePayload(String code) implements CustomPayload {
    public static final Id<PortalCodePayload> ID = new Id<>(Identifier.of("infdimmod", "portal_code_packet"));

    public static final PacketCodec<RegistryByteBuf, PortalCodePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PortalCodePayload::code,
            PortalCodePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}