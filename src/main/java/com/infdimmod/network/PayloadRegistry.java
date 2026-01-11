package com.infdimmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PayloadRegistry {
    public static void registerServer() {
        // Регистрируем пакет C2S (клиент -> сервер) только на сервере
        PayloadTypeRegistry.playC2S().register(
            SetPortalGunCodePayload.PAYLOAD_ID,
            SetPortalGunCodePayload.CODEC
        );
    }

    public static void registerClient() {
        // Регистрируем пакет S2C (сервер -> клиент) только на клиенте
        PayloadTypeRegistry.playS2C().register(
            SyncPortalGunCodePayload.PAYLOAD_ID,
            SyncPortalGunCodePayload.CODEC
        );
    }
}

