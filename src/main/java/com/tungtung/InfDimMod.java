package com.tungtung;

import com.tungtung.Blocks.ModBlocks;
import com.tungtung.items.ModItems;
import com.tungtung.world.ModDimensions;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfDimMod implements ModInitializer {
	public static final String MOD_ID = "tungtungmod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
        ModDimensions.initialize();
	}
}