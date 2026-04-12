package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public final class ThermalCentrifugeRecipe implements Recipe<Inventory> {
    private final Identifier id;
    private final Ingredient ingredient;
    private final int inputCount;
    private final List<ItemStack> results;
    private final int ticks;
    private final int heat;

    public ThermalCentrifugeRecipe(Identifier id, Ingredient ingredient, int inputCount, List<ItemStack> results, int ticks, int heat) {
        this.id = id;
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.results = results.stream().map(ItemStack::copy).toList();
        this.ticks = Math.max(1, ticks);
        this.heat = Math.max(0, heat);
    }

    public Ingredient getIngredient() { return ingredient; }
    public int getInputCount() { return inputCount; }
    public List<ItemStack> getResults() { return results.stream().map(ItemStack::copy).toList(); }
    public int getTicks() { return ticks; }
    public int getHeat() { return heat; }

    @Override
    public boolean matches(Inventory inv, World world) {
        ItemStack stack = inv.getStack(0);
        return !stack.isEmpty() && stack.getCount() >= inputCount && ingredient.test(stack);
    }

    @Override
    public ItemStack craft(Inventory inv, DynamicRegistryManager registryManager) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    @Override public Identifier getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.THERMAL_CENTRIFUGE_SERIALIZER; }
    @Override public RecipeType<?> getType() { return ModRecipes.THERMAL_CENTRIFUGE_TYPE; }
}
