package com.infdimmod.items;


import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.InfDimMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.infdimmod.items.custom.Sosiska.Sosiska;

public class ModItems {

    public static final RegistryKey<ItemGroup> MODGROUPKEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(InfDimMod.MOD_ID, "item_group"));
    public static final ItemGroup MODGROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.Sausage))
            .displayName(Text.translatable("itemGroup.idm"))
            .build();


    public static final Item Sausage = register(new Item(new Item.Settings().food(Sosiska)), "sausage");

    public static final Item PortalGun = register(new Item (new Item.Settings().maxCount(1)), "portal_gun");


    public static Item register(Item item, String id) {
        Identifier itemID = Identifier.of(InfDimMod.MOD_ID, id);
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);
        return registeredItem;
    }


    public static void initialize() {

        Registry.register(Registries.ITEM_GROUP, MODGROUPKEY, MODGROUP);
        ItemGroupEvents.modifyEntriesEvent(MODGROUPKEY).register(itemGroup -> {
            itemGroup.add(ModItems.Sausage);
            itemGroup.add(ModBlocks.SausageBlock);
            itemGroup.add(ModItems.PortalGun);
        });
    }


}

