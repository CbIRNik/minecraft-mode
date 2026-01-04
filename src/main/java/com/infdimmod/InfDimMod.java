package com.infdimmod;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.items.ModItems;
import com.infdimmod.world.ModDimensions;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
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

	}
}