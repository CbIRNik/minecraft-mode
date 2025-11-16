package com.tungtung.Blocks;

import com.tungtung.Tungtungmod;
import com.tungtung.items.TungItems;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class TungBlocks {
    public static final Block SausageBlock = register(
            new Block(AbstractBlock.Settings.create().sounds(BlockSoundGroup.NETHERRACK).strength(1.5f)),
            "sausage_block",
            true
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
            itemGroup.add(TungBlocks.SausageBlock.asItem());
        });
    }
}
