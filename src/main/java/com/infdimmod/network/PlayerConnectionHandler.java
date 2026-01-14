package com.infdimmod.network;

import com.infdimmod.items.custom.PortalGunData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PlayerConnectionHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Отправляем текущий код при подключении игрока
            PortalGunData data = PortalGunData.get(server);
            SyncPortalGunCodePayload payload = new SyncPortalGunCodePayload(data.getPortalGunCode());
            ServerPlayNetworking.send(handler.getPlayer(), payload);
        });
    }
}

