package com.tungtung;

import com.tungtung.Blocks.TungBlocks;
import com.tungtung.items.TungItems;
import com.tungtung.world.ModDimensions;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tungtungmod implements ModInitializer {
	public static final String MOD_ID = "tungtungmod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        TungItems.initialize();
        TungBlocks.initialize();
        ModDimensions.initialize();
	}
}