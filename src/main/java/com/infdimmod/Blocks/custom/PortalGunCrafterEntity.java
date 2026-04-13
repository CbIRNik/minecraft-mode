package com.infdimmod.Blocks.custom;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.burmaldeniya.BurmaldushkaRotationManager;
import com.infdimmod.items.ModItems;
import com.infdimmod.items.custom.burmaldushka.BurmaldushkaComponents;
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
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PortalGunCrafterEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(7, ItemStack.EMPTY);
    private static final long CRAFT_RATE_LIMIT_TICKS = 4L;
    private final Map<UUID, Long> lastCraftAttemptByPlayer = new HashMap<>();
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (world == null || world.isClient) {
                return 0;
            }

            BurmaldushkaRotationManager.BurmaldushkaRotationSnapshot snapshot = BurmaldushkaRotationManager.getSnapshot(world);
            return switch (index) {
                case 0 -> snapshot.rotationIndex();
                case 1 -> snapshot.rotationVersion();
                case 2 -> (int) Math.min(Integer.MAX_VALUE, BurmaldushkaRotationManager.getSecondsUntilNextRotation(world));
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int size() {
            return 3;
        }
    };

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

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
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
        Optional<RecipeEntry<PortalGunRecipe>> recipe = getMatchingRecipe();

        if (recipe.isPresent()) {
            PortalGunRecipe matchedRecipe = recipe.get().value();
            ItemStack resultStack = matchedRecipe.getResult(world.getRegistryManager()).copy();
            if (matchedRecipe.rotatingBurmaldushka() && resultStack.isOf(ModItems.Burmaldushka)) {
                BurmaldushkaRotationManager.BurmaldushkaRotationSnapshot snapshot = BurmaldushkaRotationManager.getSnapshot(world);
                resultStack.set(
                        BurmaldushkaComponents.BURMALDUSHKA_STATE,
                        new BurmaldushkaComponents.BurmaldushkaState(snapshot.rotationIndex(), snapshot.rotationVersion())
                );
            }
            this.inventory.set(6, resultStack);
        } else {
            this.inventory.set(6, ItemStack.EMPTY);
        }
    }

    public boolean canTakeCraftResult(PlayerEntity player) {
        return canCraftResult(player, getStack(6), false);
    }

    public boolean tryConsumeIngredientsForCraft(PlayerEntity player, ItemStack craftedStack) {
        if (!canCraftResult(player, craftedStack, true)) {
            updateResult();
            return false;
        }

        for (int i = 0; i < 6; i++) {
            if (!getStack(i).isEmpty()) {
                removeStack(i, 1);
            }
        }

        updateResult();
        markDirty();
        return true;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (slot < 6) {
            updateResult();
        }
        markDirty();
    }

    private Optional<RecipeEntry<PortalGunRecipe>> getMatchingRecipe() {
        if (world == null) {
            return Optional.empty();
        }
        RecipeInput input = createInput();
        return world.getRecipeManager().getFirstMatch(PortalGunRecipe.Type.INSTANCE, input, world);
    }

    private RecipeInput createInput() {
        return new RecipeInput() {
            @Override
            public ItemStack getStackInSlot(int slot) {
                return getStack(slot);
            }

            @Override
            public int getSize() {
                return 6;
            }
        };
    }

    private boolean canCraftResult(PlayerEntity player, ItemStack craftedStack, boolean consumeRateLimit) {
        if (world == null || world.isClient || craftedStack.isEmpty()) {
            return false;
        }

        Optional<RecipeEntry<PortalGunRecipe>> recipeEntry = getMatchingRecipe();
        if (recipeEntry.isEmpty()) {
            return false;
        }

        PortalGunRecipe recipe = recipeEntry.get().value();
        if (recipe.rotatingBurmaldushka() && craftedStack.isOf(ModItems.Burmaldushka)) {
            if (!matchesBurmaldushkaMetadata(craftedStack)) {
                return false;
            }

            if (!BurmaldushkaRotationManager.matchesCurrentRotation(createInput(), world)) {
                return false;
            }

            for (int slot = 0; slot < 6; slot++) {
                if (!BurmaldushkaRotationManager.isAllowedRotationItem(getStack(slot))) {
                    return false;
                }
            }
        }

        return checkCraftRateLimit(player, consumeRateLimit);
    }

    private boolean matchesBurmaldushkaMetadata(ItemStack craftedStack) {
        BurmaldushkaRotationManager.BurmaldushkaRotationSnapshot snapshot = BurmaldushkaRotationManager.getSnapshot(world);
        BurmaldushkaComponents.BurmaldushkaState state = craftedStack.getOrDefault(
                BurmaldushkaComponents.BURMALDUSHKA_STATE,
                new BurmaldushkaComponents.BurmaldushkaState(-1, -1)
        );
        return state.rotationVersion() == snapshot.rotationVersion() && state.rotation() == snapshot.rotationIndex();
    }

    private boolean checkCraftRateLimit(PlayerEntity player, boolean consume) {
        UUID playerId = player.getUuid();
        long nowTick = world.getTime();
        Long previous = lastCraftAttemptByPlayer.get(playerId);
        if (previous != null && nowTick - previous < CRAFT_RATE_LIMIT_TICKS) {
            return false;
        }

        if (consume) {
            lastCraftAttemptByPlayer.put(playerId, nowTick);
        }
        return true;
    }
}
