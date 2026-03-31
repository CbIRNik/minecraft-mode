package com.infdimmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortalCoordsPayload(double x, double y, double z) implements CustomPayload {
    public static final Id<PortalCoordsPayload> ID = new Id<>(Identifier.of("infdimmod", "portal_coords_packet"));

    public static final PacketCodec<RegistryByteBuf, PortalCoordsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, PortalCoordsPayload::x,
            PacketCodecs.DOUBLE, PortalCoordsPayload::y,
            PacketCodecs.DOUBLE, PortalCoordsPayload::z,
            PortalCoordsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}