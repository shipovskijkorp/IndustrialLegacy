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

public final class CanningRecipe implements Recipe<Inventory> {
    private final Identifier id;
    private final Ingredient container;
    private final int containerCount;
    private final Ingredient fill;
    private final int fillCount;
    private final ItemStack result;
    private final int ticks;

    public CanningRecipe(Identifier id, Ingredient container, int containerCount, Ingredient fill, int fillCount, ItemStack result, int ticks) {
        this.id = id;
        this.container = container;
        this.containerCount = Math.max(1, containerCount);
        this.fill = fill;
        this.fillCount = Math.max(1, fillCount);
        this.result = result;
        this.ticks = Math.max(1, ticks);
    }

    public Ingredient getContainer() { return container; }
    public int getContainerCount() { return containerCount; }
    public Ingredient getFill() { return fill; }
    public int getFillCount() { return fillCount; }
    public int getTicks() { return ticks; }
    public ItemStack getResultStack() { return result; }

    @Override
    public boolean matches(Inventory inv, World world) {
        ItemStack containerStack = inv.getStack(0);
        ItemStack fillStack = inv.getStack(1);
        return !containerStack.isEmpty() && !fillStack.isEmpty()
                && containerStack.getCount() >= containerCount
                && fillStack.getCount() >= fillCount
                && container.test(containerStack)
                && fill.test(fillStack);
    }

    @Override
    public ItemStack craft(Inventory inv, DynamicRegistryManager registryManager) {
        return result.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return result;
    }

    @Override public Identifier getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CANNING_SERIALIZER; }
    @Override public RecipeType<?> getType() { return ModRecipes.CANNING_TYPE; }
}
