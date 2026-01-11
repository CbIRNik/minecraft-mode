package com.infdimmod.network;

import com.infdimmod.items.custom.PortalGunData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class SetPortalGunCodePayload implements CustomPayload {
    public static final Identifier ID = Identifier.of("infdimmod", "set_portal_gun_code");

    public static final net.minecraft.network.packet.CustomPayload.Id<SetPortalGunCodePayload> PAYLOAD_ID =
        new net.minecraft.network.packet.CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, SetPortalGunCodePayload> CODEC =
        PacketCodecs.STRING.xmap(SetPortalGunCodePayload::new, p -> p.code).cast();

    private final String code;

    public SetPortalGunCodePayload(String code) {
        this.code = code;
    }

    @Override
    public net.minecraft.network.packet.CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }

    public String getCode() {
        return code;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PAYLOAD_ID, (payload, context) -> {
            context.server().execute(() -> {
                MinecraftServer server = context.server();
                PortalGunData data = PortalGunData.get(server);

                // Проверяем, что код содержит только цифры и буквы
                if (payload.code.matches("^[a-zA-Z0-9]*$")) {
                    data.setPortalGunCode(payload.code);

                    // Отправляем обновление всем игрокам
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(player, new SyncPortalGunCodePayload(payload.code));
                    }
                }
            });
        });
    }
}



