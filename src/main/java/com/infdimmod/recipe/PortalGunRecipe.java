package com.infdimmod.recipe;

import com.infdimmod.burmaldeniya.BurmaldushkaRotationManager;
import com.infdimmod.items.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public record PortalGunRecipe(DefaultedList<Ingredient> ingredients, ItemStack result, boolean rotatingBurmaldushka) implements Recipe<RecipeInput> {

    @Override
    public boolean matches(RecipeInput input, World world) {
        if (world.isClient) return false;
        if (this.rotatingBurmaldushka && result.isOf(ModItems.Burmaldushka)) {
            return BurmaldushkaRotationManager.matchesCurrentRotation(input, world);
        }
        java.util.List<ItemStack> inputStacks = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inputStacks.add(stack);
            }
        }
        if (ingredients.isEmpty() || inputStacks.isEmpty()) {
            return false;
        }
        if (inputStacks.size() != ingredients.size()) {
            return false;
        }
        java.util.List<Ingredient> recipeIngredients = new java.util.ArrayList<>(this.ingredients);

        for (ItemStack inputStack : inputStacks) {
            boolean matched = false;
            for (int i = 0; i < recipeIngredients.size(); i++) {
                if (recipeIngredients.get(i).test(inputStack)) {
                    recipeIngredients.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return recipeIngredients.isEmpty();
    }

    @Override
    public ItemStack craft(RecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return result.copy();
    }

    @Override
    public boolean fits(int width, int height) { return true; }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registries) { return result; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<PortalGunRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "portal_gun_crafting";
    }

    public static class Serializer implements RecipeSerializer<PortalGunRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<PortalGunRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.DISALLOW_EMPTY_CODEC.listOf()
                        .fieldOf("ingredients")
                        .xmap(ingredients -> {
                            DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(ingredients.size(), Ingredient.EMPTY);
                            for (int i = 0; i < ingredients.size(); i++) {
                                defaultedList.set(i, ingredients.get(i));
                            }
                            return defaultedList;
                        }, ingredients -> ingredients)
                        .forGetter(PortalGunRecipe::ingredients),
                ItemStack.CODEC.fieldOf("result").forGetter(PortalGunRecipe::result)
                ,
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("rotating_burmaldushka", false).forGetter(PortalGunRecipe::rotatingBurmaldushka)
        ).apply(inst, PortalGunRecipe::new));

        public static final PacketCodec<RegistryByteBuf, PortalGunRecipe> PACKET_CODEC = PacketCodec.tuple(
                Ingredient.PACKET_CODEC.collect(PacketCodecs.toList())
                        .xmap(list -> {
                            DefaultedList<Ingredient> defaultedList = DefaultedList.ofSize(list.size(), Ingredient.EMPTY);
                            for (int i = 0; i < list.size(); i++) {
                                defaultedList.set(i, list.get(i));
                            }
                            return defaultedList;
                        }, list -> list),
                PortalGunRecipe::ingredients,
                ItemStack.PACKET_CODEC, PortalGunRecipe::result,
                PacketCodecs.BOOL, PortalGunRecipe::rotatingBurmaldushka,
                PortalGunRecipe::new
        );

        @Override
        public MapCodec<PortalGunRecipe> codec() { return CODEC; }

        @Override
        public PacketCodec<RegistryByteBuf, PortalGunRecipe> packetCodec() { return PACKET_CODEC; }
    }
}
