package com.infdimmod.Blocks;

import com.infdimmod.Blocks.custom.PortalFluid;
import com.infdimmod.Blocks.custom.PortalFluidBlock;
import com.infdimmod.InfDimMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final FlowableFluid STILL_PORTAL_FLUID = register("portal_fluid", new PortalFluid.Still());

    public static final FlowableFluid FLOWING_PORTAL_FLUID = register("portal_fluid_flowing", new PortalFluid.Flowing());

    public static final Block PORTAL_FLUID_BLOCK = register(
            new PortalFluidBlock(STILL_PORTAL_FLUID, AbstractBlock.Settings.create()
                    .replaceable()
                    .noCollision()
                    .strength(100.0f)
                    .dropsNothing()
                    .nonOpaque()
                    .luminance(state -> 8)
                    .mapColor(MapColor.LIME)),
            "portal_fluid_block",
            false
    );

    public static final Block SausageBlock = register(
            new Block(AbstractBlock.Settings.create().sounds(BlockSoundGroup.NETHERRACK).strength(1.5f)),
            "sausage_block",
            true
    );
    public static final Block PolytechBlock = register(
            new Block(AbstractBlock.Settings.create().sounds(BlockSoundGroup.NETHERRACK).strength(1.5f)
                    .mapColor(MapColor.LIME)),
            "polytech_block",
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
        Identifier id = Identifier.of(InfDimMod.MOD_ID, name);
        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, new Item.Settings());
            Registry.register(Registries.ITEM, id, blockItem);
        }
        return Registry.register(Registries.BLOCK, id, block);
    }

    private static FlowableFluid register(String name, FlowableFluid fluid) {
        return Registry.register(Registries.FLUID, Identifier.of(InfDimMod.MOD_ID, name), fluid);
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
