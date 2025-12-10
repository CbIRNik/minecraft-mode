package com.tungtung.items;


import com.tungtung.Tungtungmod;
import com.tungtung.items.custom.CustomDescriptionItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.tungtung.items.custom.Sosiska.Sosiska;

public class ModItems {

    public static final Item Sausage = register(new CustomDescriptionItem(new Item.Settings().food(Sosiska)), "sausage");

    public static final Item BlockOfSausage = register(new CustomDescriptionItem(new Item.Settings()), "sausageblock");

    public static final Item PortalGun = register(new Item (new Item.Settings().maxCount(1)), "portal_gun");


    public static Item register(Item item, String id) {
        Identifier itemID = Identifier.of(Tungtungmod.MOD_ID, id);
        Item registeredItem = Registry.register(Registries.ITEM, itemID, item);
        return registeredItem;
    }




    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
            .register((itemGroup) -> itemGroup.add(ModItems.Sausage));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register((itemGroup) -> itemGroup.add(ModItems.PortalGun));
    }


}

