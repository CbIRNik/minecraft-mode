package com.infdimmod;

import com.infdimmod.items.custom.PortalGunClient;
import net.fabricmc.api.ClientModInitializer;

public class InfDimModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        PortalGunClient.registerModelPredicates();
	}
}