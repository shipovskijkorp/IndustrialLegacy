package com.shipovskijkorp.industriallegacy.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class MetalFormerRecipe implements Recipe<Inventory> {
    private final Identifier id;
    private final Ingredient ingredient;
    private final ItemStack output;
    private final int ticks;
    private final int inputCount;
    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;

    public MetalFormerRecipe(Identifier id, Ingredient ingredient, ItemStack output, int ticks, int inputCount,
                             RecipeType<?> type, RecipeSerializer<?> serializer) {
        this.id = id;
        this.ingredient = ingredient;
        this.output = output;
        this.ticks = ticks;
        this.inputCount = inputCount;
        this.type = type;
        this.serializer = serializer;
    }

    public Ingredient getIngredient() { return ingredient; }
    public int getTicks() { return ticks; }
    public int getInputCount() { return inputCount; }

    @Override
    public boolean matches(Inventory inv, World world) {
        ItemStack stack = inv.getStack(0);
        return !stack.isEmpty() && stack.getCount() >= inputCount && ingredient.test(stack);
    }

    @Override
    public ItemStack craft(Inventory inv, DynamicRegistryManager registryManager) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return output;
    }

    public ItemStack getOutputStack() {
        return output;
    }

    @Override public Identifier getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return serializer; }
    @Override public RecipeType<?> getType() { return type; }
}
