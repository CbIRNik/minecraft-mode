package com.infdimmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class SyncPortalGunCodePayload implements CustomPayload {
    public static final Identifier ID = Identifier.of("infdimmod", "sync_portal_gun_code");

    public static final net.minecraft.network.packet.CustomPayload.Id<SyncPortalGunCodePayload> PAYLOAD_ID =
        new net.minecraft.network.packet.CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, SyncPortalGunCodePayload> CODEC =
        PacketCodecs.STRING.xmap(SyncPortalGunCodePayload::new, p -> p.code).cast();

    private final String code;

    public SyncPortalGunCodePayload(String code) {
        this.code = code;
    }

    @Override
    public net.minecraft.network.packet.CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }

    public String getCode() {
        return code;
    }
}

