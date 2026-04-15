package com.infdimmod.Blocks.custom;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.recipe.PortalGunRecipe;
import com.infdimmod.util.ImplementedInventory;
import com.infdimmod.util.PortalGunCrafterScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class PortalGunCrafterEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(7, ItemStack.EMPTY);

    public PortalGunCrafterEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PORTAL_GUN_CRAFTER_BE, pos, state);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PortalGunCrafterScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.portal_gun_crafter");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, inventory, registries);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, inventory, registries);
    }

    public void updateResult() {
        if (this.world == null || this.world.isClient) return;
        RecipeInput input = new RecipeInput() {
            @Override
            public ItemStack getStackInSlot(int slot) { return getStack(slot); }
            @Override
            public int getSize() { return 6; }
        };
        var recipe = world.getRecipeManager().getFirstMatch(PortalGunRecipe.Type.INSTANCE, input, world);

        if (recipe.isPresent()) {
            this.inventory.set(6, recipe.get().value().getResult(world.getRegistryManager()).copy());
        } else {
            this.inventory.set(6, ItemStack.EMPTY);
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (slot < 6) {
            updateResult();
        }
        markDirty();
    }
}