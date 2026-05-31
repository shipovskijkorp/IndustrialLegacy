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

/**
 * Simple machine recipe: one input -> one output, plus per-recipe tick cost.
 *
 * <p>IL macerator recipes sometimes require multiple input items (for example
 * plant matter or tin cans). We store the required input count explicitly so
 * both datapack formats and the runtime machine logic stay in sync.</p>
 */
public class MaceratorRecipe implements Recipe<Inventory> {
    private final Identifier id;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemStack output;
    private final int ticks;

    public MaceratorRecipe(Identifier id, Ingredient ingredient, int ingredientCount, ItemStack output, int ticks) {
        this.id = id;
        this.ingredient = ingredient;
        this.ingredientCount = Math.max(1, ingredientCount);
        this.output = output;
        this.ticks = ticks;
    }

    public Ingredient getIngredient() { return ingredient; }
    public int getIngredientCount() { return ingredientCount; }
    public int getTicks() { return ticks; }

    @Override
    public boolean matches(Inventory inv, World world) {
        ItemStack in = inv.getStack(0);
        return !in.isEmpty() && in.getCount() >= ingredientCount && ingredient.test(in);
    }

    @Override
    public ItemStack craft(Inventory inv, DynamicRegistryManager registryManager) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    /**
     * Yarn/MC 1.20.x uses getOutput(registryManager) as the canonical output getter.
     */
    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return output;
    }

    // Convenience for other call-sites (do NOT annotate @Override; mappings differ).
    public ItemStack getOutputStack() {
        return output;
    }

    @Override public Identifier getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.MACERATOR_SERIALIZER; }
    @Override public RecipeType<?> getType() { return ModRecipes.MACERATOR_TYPE; }
}
