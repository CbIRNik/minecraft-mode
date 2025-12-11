package com.tungtung.items;


import com.tungtung.InfDimMod;
import com.tungtung.items.custom.CustomDescriptionItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.tungtung.items.custom.Sosiska.Sosiska;

public class ModItems {

    public static final RegistryKey<ItemGroup> MODGROUPKEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(InfDimMod.MOD_ID, "item_group"));
    public static final ItemGroup MODGROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.Sausage))
            .displayName(Text.translatable("itemGroup.idm"))
            .build();


    public static final Item Sausage = register(new CustomDescriptionItem(new Item.Settings().food(Sosiska)), "sausage");

    public static final Item BlockOfSausage = register(new CustomDescriptionItem(new Item.Settings()), "sausageblock");

    public static final Item PortalGun = register(new Item (new Item.Settings().maxCount(1)), "portal_gun");


    public static Item register(Item item, String id) {
        Identifier itemID = Identifier.of(InfDimMod.MOD_ID, id);
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);
        return registeredItem;
    }




    public static void initialize() {

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
            .register((itemGroup) -> itemGroup.add(ModItems.Sausage));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register((itemGroup) -> itemGroup.add(ModItems.BlockOfSausage));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register((itemGroup) -> itemGroup.add(ModItems.PortalGun));
    }


}

