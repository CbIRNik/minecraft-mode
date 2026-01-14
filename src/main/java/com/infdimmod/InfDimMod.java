package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.items.ModItems;
import com.infdimmod.items.custom.PortalGunTickHandler;
import com.infdimmod.world.ModDimensions;
import com.infdimmod.network.Networking;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfDimMod implements ModInitializer {
	public static final String MOD_ID = "infdimmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@Override
	public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
        ModDimensions.initialize();
        PortalGunTickHandler.register();
        Networking.registerServer();
	}
}