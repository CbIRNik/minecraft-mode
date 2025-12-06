package com.tungtung.Blocks;

import com.tungtung.Tungtungmod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block SausageBlock = register(
            new Block(AbstractBlock.Settings.create().sounds(BlockSoundGroup.NETHERRACK).strength(1.5f)),
            "sausage_block",
            true
    );

    public static final Block MYSTIC_PORTAL_FRAME = register(
            new MysticPortalFrameBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.PURPLE)
                    .strength(50.0f, 1200.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .luminance(state -> 3)),
            "mystic_portal_frame",
            true
    );

    public static final Block MYSTIC_PORTAL = register(
            new MysticPortalBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.PURPLE)
                    .noCollision()
                    .strength(-1.0f)
                    .sounds(BlockSoundGroup.GLASS)
                    .luminance(state -> 11)),
            "mystic_portal",
            false
    );



    public static Block register(Block block, String name, boolean shouldRegisterItem) {

        Identifier id = Identifier.of(Tungtungmod.MOD_ID, name);


        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, new Item.Settings());
            Registry.register(Registries.ITEM, id, blockItem);
        }

        return Registry.register(Registries.BLOCK, id, block);
    }



    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register((itemGroup) -> {
            itemGroup.add(ModBlocks.SausageBlock.asItem());
        });
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register((itemGroup) -> {
            itemGroup.add(ModBlocks.MYSTIC_PORTAL_FRAME.asItem());
        });
    }
}
