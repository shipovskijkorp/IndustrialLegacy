package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * IC2 coil recipe.
 * Pattern:
 *  CCC
 *  CXC
 *  CCC
 * C = copper cable (insulation 0)
 * X = iron ingot
 */
public final class CoilRecipe extends SpecialCraftingRecipe {
    private final ItemStack result;

    public CoilRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    private static boolean isCopperBare(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof CableItem
                && CableItem.getKind(stack) == CableKind.COPPER
                && CableItem.getInsulation(stack) == 0;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                if (!inv.getStack(i).isOf(net.minecraft.item.Items.IRON_INGOT)) return false;
            } else {
                if (!isCopperBare(inv.getStack(i))) return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result.copy();
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        return DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COIL_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
