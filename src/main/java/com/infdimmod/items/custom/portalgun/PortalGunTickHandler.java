package com.infdimmod.items.custom.portalgun;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class PortalGunTickHandler {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            PortalGun.checkAndRemoveBlocks(server);
        });
    }
}