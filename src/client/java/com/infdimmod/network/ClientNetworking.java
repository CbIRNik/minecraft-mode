package com.infdimmod.network;

import com.infdimmod.items.custom.PortalGunHudRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworking {
    public static void register() {
        // Регистрируем пакеты в реестре (только клиентские)
        PayloadRegistry.registerClient();

        // Регистрируем обработчик для пакета синхронизации кода
        ClientPlayNetworking.registerGlobalReceiver(
            SyncPortalGunCodePayload.PAYLOAD_ID,
            (payload, context) -> {
                PortalGunHudRenderer.setCurrentCode(payload.getCode());
            }
        );
    }
}

