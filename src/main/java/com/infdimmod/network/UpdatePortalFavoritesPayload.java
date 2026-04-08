package com.infdimmod.network;

import com.infdimmod.InfDimMod;
import com.infdimmod.items.custom.portalgun.PortalGunComponents;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record UpdatePortalFavoritesPayload(List<PortalGunComponents.PortalEntry> favorites) implements CustomPayload {
    public static final Id<UpdatePortalFavoritesPayload> ID = new Id<>(Identifier.of(InfDimMod.MOD_ID, "update_favorites"));

    public static final PacketCodec<RegistryByteBuf, UpdatePortalFavoritesPayload> CODEC = PacketCodec.tuple(
            PortalGunComponents.PortalEntry.PACKET_CODEC.collect(PacketCodecs.toList()),
            UpdatePortalFavoritesPayload::favorites,
            UpdatePortalFavoritesPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}