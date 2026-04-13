package com.infdimmod.util;

import com.infdimmod.Blocks.custom.PortalGunCrafterEntity;
import com.infdimmod.InfDimMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class PortalGunCrafterScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    public PortalGunCrafterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(7), new ArrayPropertyDelegate(3));
    }

    public PortalGunCrafterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        this(syncId, playerInventory, inventory,
                inventory instanceof PortalGunCrafterEntity entity ? entity.getPropertyDelegate() : new ArrayPropertyDelegate(3));
    }

    public PortalGunCrafterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(InfDimMod.PORTAL_GUN_CRAFTER_SH, syncId);
        checkSize(inventory, 7);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        checkDataCount(this.propertyDelegate, 3);
        addProperties(this.propertyDelegate);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(inventory, col + row * 3, 30 + col * 18, 26 + row * 18));
            }
        }

        this.addSlot(new Slot(inventory, 6, 124, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity player) {
                if (inventory instanceof PortalGunCrafterEntity entity) {
                    return entity.canTakeCraftResult(player);
                }
                return super.canTakeItems(player);
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                if (inventory instanceof PortalGunCrafterEntity entity) {
                    if (!entity.tryConsumeIngredientsForCraft(player, stack)) {
                        return;
                    }
                } else {
                    for (int i = 0; i < 6; i++) {
                        inventory.removeStack(i, 1);
                    }
                }
                super.onTakeItem(player, stack);
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < 7) {
                if (!this.insertItem(originalStack, 7, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(originalStack, 0, 6, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public int getRotationIndexHint() {
        return propertyDelegate.get(0);
    }

    public int getRotationVersionHint() {
        return propertyDelegate.get(1);
    }

    public int getSecondsUntilNextRotationHint() {
        return propertyDelegate.get(2);
    }

    private void addPlayerInventory(PlayerInventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        if (inventory == this.inventory) {
            if (this.inventory instanceof PortalGunCrafterEntity entity) {
                entity.updateResult();
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
