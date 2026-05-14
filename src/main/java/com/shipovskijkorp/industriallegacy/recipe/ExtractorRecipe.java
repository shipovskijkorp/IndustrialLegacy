package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
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
 * IC2 Extractor machine recipe: one input stack -> one output stack.
 *
 * <p>The optional requiredFluid field is used for IC2 fluid-cell recipes such as
 * compressed air cell -> empty cell while keeping normal item ingredients simple.</p>
 */
public class ExtractorRecipe implements Recipe<Inventory> {
    private final Identifier id;
    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemStack output;
    private final int ticks;
    private final String requiredFluid;

    public ExtractorRecipe(Identifier id, Ingredient ingredient, int ingredientCount, ItemStack output, int ticks, String requiredFluid) {
        this.id = id;
        this.ingredient = ingredient;
        this.ingredientCount = Math.max(1, ingredientCount);
        this.output = output;
        this.ticks = ticks;
        this.requiredFluid = requiredFluid;
    }

    public Ingredient getIngredient() { return ingredient; }
    public int getIngredientCount() { return ingredientCount; }
    public int getTicks() { return ticks; }
    public String getRequiredFluid() { return requiredFluid; }

    @Override
    public boolean matches(Inventory inv, World world) {
        ItemStack in = inv.getStack(0);
        if (in.isEmpty() || in.getCount() < ingredientCount || !ingredient.test(in)) return false;
        if (requiredFluid != null) {
            if (!(in.getItem() instanceof UniversalFluidCellItem)) return false;
            return UniversalFluidCellItem.matchesRequiredFluid(in, requiredFluid);
        }
        return true;
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
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.EXTRACTOR_SERIALIZER; }
    @Override public RecipeType<?> getType() { return ModRecipes.EXTRACTOR_TYPE; }
}
