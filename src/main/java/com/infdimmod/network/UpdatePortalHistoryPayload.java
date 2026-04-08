package com.infdimmod.network;

import com.infdimmod.InfDimMod;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record UpdatePortalHistoryPayload(List<PortalGunComponents.PortalEntry> history) implements CustomPayload {
    public static final Id<UpdatePortalHistoryPayload> ID = new Id<>(Identifier.of(InfDimMod.MOD_ID, "update_history"));

    public static final PacketCodec<RegistryByteBuf, UpdatePortalHistoryPayload> CODEC = PacketCodec.tuple(
            PortalGunComponents.PortalEntry.PACKET_CODEC.collect(PacketCodecs.toList()),
            UpdatePortalHistoryPayload::history,
            UpdatePortalHistoryPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}