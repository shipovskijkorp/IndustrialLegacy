package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
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
 * IC2 Experimental Luminator recipe (shaped_recipes.ini):
 *
 * pattern:
 *  "ICI"
 *  "GTG"
 *  "GGG"
 *
 * where:
 *  I = iron casing
 *  C = copper cable (insulation=1)
 *  T = tin cable (insulation=0)
 *  G = glass
 *
 * output: 8 luminators (flat)
 */
public final class LuminatorRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public LuminatorRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        // row 0: I C I
        if (!inv.getStack(0).isOf(ModItems.IRON_CASING)) return false;

        ItemStack c = inv.getStack(1);
        if (c.isEmpty() || !(c.getItem() instanceof CableItem)) return false;
        if (CableItem.getKind(c) != CableKind.COPPER) return false;
        if (CableItem.getInsulation(c) != 1) return false;

        if (!inv.getStack(2).isOf(ModItems.IRON_CASING)) return false;

        // row 1: G T G
        if (!inv.getStack(3).isOf(net.minecraft.item.Items.GLASS)) return false;

        ItemStack t = inv.getStack(4);
        if (t.isEmpty() || !(t.getItem() instanceof CableItem)) return false;
        if (CableItem.getKind(t) != CableKind.TIN) return false;
        if (CableItem.getInsulation(t) != 0) return false;

        if (!inv.getStack(5).isOf(net.minecraft.item.Items.GLASS)) return false;

        // row 2: G G G
        if (!inv.getStack(6).isOf(net.minecraft.item.Items.GLASS)) return false;
        if (!inv.getStack(7).isOf(net.minecraft.item.Items.GLASS)) return false;
        if (!inv.getStack(8).isOf(net.minecraft.item.Items.GLASS)) return false;

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
        return ModRecipes.LUMINATOR_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
