package com.infdimmod.Blocks;

import com.infdimmod.Blocks.custom.PortalFluid;
import com.infdimmod.Blocks.custom.PortalFluidBlock;
import com.infdimmod.Blocks.custom.PortalGunCrafter;
import com.infdimmod.Blocks.custom.PortalGunCrafterEntity;
import com.infdimmod.Blocks.custom.DrunnyCollider;
import com.infdimmod.InfDimMod;
import com.infdimmod.burmaldeniya.BurmaldeniyaConstants;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
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

    public static final Block PortalGunCrafter = register(
            new PortalGunCrafter(AbstractBlock.Settings.create().sounds(BlockSoundGroup.STONE).strength(1.5f)),
            "portal_gun_crafter",
            true
    );
    public static final BlockEntityType<PortalGunCrafterEntity> PORTAL_GUN_CRAFTER_BE =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(InfDimMod.MOD_ID, "portal_gun_crafter_be"),
                    BlockEntityType.Builder.create(PortalGunCrafterEntity::new, ModBlocks.PortalGunCrafter).build());
    public static final Block PolytechBlock = register(
            new Block(AbstractBlock.Settings.create().sounds(BlockSoundGroup.NETHERRACK).strength(1.5f)
                    .mapColor(MapColor.LIME)),
            "polytech_block",
            true
    );
    public static final Block DrunnyCollider = register(
            new DrunnyCollider(AbstractBlock.Settings.create().sounds(BlockSoundGroup.AMETHYST_BLOCK).strength(1.8f)
                    .mapColor(MapColor.PURPLE)),
            BurmaldeniyaConstants.DRUNNY_COLLIDER_BLOCK_ID,
            true
    );

    public static final Block DRUNNY_ATOM = register(
            new com.infdimmod.Blocks.custom.DrunnyAtomBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.AMETHYST_CLUSTER)
                    .strength(4.0f, 12.0f)
                    .requiresTool()
                    .luminance(state -> 13)
                    .mapColor(MapColor.CYAN)),
            BurmaldeniyaConstants.DRUNNY_ATOM_BLOCK_ID,
            true
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
    }
}
