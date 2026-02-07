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
 * CESU recipe:
 *  P C P
 *  A A A
 *  P P P
 *
 * P = bronze plate
 * A = advanced battery
 * C = copper cable (insulation = 1)
 *
 * Result: cesu
 */
public final class CesuRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public CesuRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        // Row 0: P C P
        if (!inv.getStack(0).isOf(ModItems.BRONZE_PLATE)) return false;
        if (!isCopperCableIns1(inv.getStack(1))) return false;
        if (!inv.getStack(2).isOf(ModItems.BRONZE_PLATE)) return false;

        // Row 1: A A A
        if (!inv.getStack(3).isOf(ModItems.ADVANCED_RE_BATTERY)) return false;
        if (!inv.getStack(4).isOf(ModItems.ADVANCED_RE_BATTERY)) return false;
        if (!inv.getStack(5).isOf(ModItems.ADVANCED_RE_BATTERY)) return false;

        // Row 2: P P P
        if (!inv.getStack(6).isOf(ModItems.BRONZE_PLATE)) return false;
        if (!inv.getStack(7).isOf(ModItems.BRONZE_PLATE)) return false;
        if (!inv.getStack(8).isOf(ModItems.BRONZE_PLATE)) return false;

        return true;
    }

    private static boolean isCopperCableIns1(ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof CableItem)) return false;
        if (CableItem.getKind(s) != CableKind.COPPER) return false;
        return CableItem.getInsulation(s) == 1;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        return DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CESU_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
