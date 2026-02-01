package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Crafting recipe: 1x plate + 1x cutter -> Nx cable (variant encoded in NBT).
 *
 * <p>The cutter remains in the grid and loses 1 durability per craft.</p>
 */
public final class CutterCableRecipe extends SpecialCraftingRecipe {

    private final Ingredient tool;
    private final Ingredient material;
    private final ItemStack result;

    public CutterCableRecipe(Identifier id,
                             CraftingRecipeCategory category,
                             Ingredient tool,
                             Ingredient material,
                             ItemStack result) {
        super(id, category);
        this.tool = tool;
        this.material = material;
        this.result = result;
    }

    public Ingredient tool() {
        return tool;
    }

    public Ingredient material() {
        return material;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        int toolCount = 0;
        int materialCount = 0;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            if (tool.test(stack)) {
                toolCount++;
                if (toolCount > 1) return false;
            } else if (material.test(stack)) {
                materialCount++;
                if (materialCount > 1) return false;
            } else {
                return false;
            }
        }

        return toolCount == 1 && materialCount == 1;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result.copy();
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        DefaultedList<ItemStack> remainder = DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            if (tool.test(stack)) {
                ItemStack copy = stack.copy();
                if (copy.isDamageable()) {
                    copy.setDamage(copy.getDamage() + 1);
                    // maxDamage=60 => valid damage range is 0..60 (61 uses). Break on 61st craft.
                    if (copy.getDamage() > copy.getMaxDamage()) {
                        copy = ItemStack.EMPTY;
                    }
                }
                remainder.set(i, copy);
                break;
            }
        }

        return remainder;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CUTTER_CABLE_SERIALIZER;
    }

    /**
     * IMPORTANT:
     * Crafting tables only search RecipeType.CRAFTING.
     * If we return a custom RecipeType, the recipe will never match in a crafting grid.
     */
    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
