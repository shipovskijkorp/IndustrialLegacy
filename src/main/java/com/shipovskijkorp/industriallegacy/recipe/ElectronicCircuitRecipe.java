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
import net.minecraft.world.World;
import net.minecraft.util.collection.DefaultedList;

/**
 * IC2 electronic circuit recipe (two shaped variants).
 *
 * Ingredients:
 *  C = copper cable (insulation=1)
 *  P = iron plate
 *  R = redstone
 *
 * Variants:
 *  1)
 *   CCC
 *   RPR
 *   CCC
 *
 *  2)
 *   CRC
 *   CPC
 *   CRC
 */
public final class ElectronicCircuitRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public ElectronicCircuitRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        // Try both variants.
        return matchesVariant1(inv) || matchesVariant2(inv);
    }

    private static boolean isCopperInsulated(ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof CableItem)) return false;
        return CableItem.getKind(s) == CableKind.COPPER && CableItem.getInsulation(s) == 1;
    }

    private static boolean matchesVariant1(RecipeInputInventory inv) {
        // Row 0: C C C
        if (!isCopperInsulated(inv.getStack(0))) return false;
        if (!isCopperInsulated(inv.getStack(1))) return false;
        if (!isCopperInsulated(inv.getStack(2))) return false;

        // Row 1: R P R
        if (!inv.getStack(3).isOf(net.minecraft.item.Items.REDSTONE)) return false;
        if (!inv.getStack(4).isOf(ModItems.IRON_PLATE)) return false;
        if (!inv.getStack(5).isOf(net.minecraft.item.Items.REDSTONE)) return false;

        // Row 2: C C C
        if (!isCopperInsulated(inv.getStack(6))) return false;
        if (!isCopperInsulated(inv.getStack(7))) return false;
        if (!isCopperInsulated(inv.getStack(8))) return false;

        return true;
    }

    private static boolean matchesVariant2(RecipeInputInventory inv) {
        // C R C
        if (!isCopperInsulated(inv.getStack(0))) return false;
        if (!inv.getStack(1).isOf(net.minecraft.item.Items.REDSTONE)) return false;
        if (!isCopperInsulated(inv.getStack(2))) return false;

        // C P C
        if (!isCopperInsulated(inv.getStack(3))) return false;
        if (!inv.getStack(4).isOf(ModItems.IRON_PLATE)) return false;
        if (!isCopperInsulated(inv.getStack(5))) return false;

        // C R C
        if (!isCopperInsulated(inv.getStack(6))) return false;
        if (!inv.getStack(7).isOf(net.minecraft.item.Items.REDSTONE)) return false;
        if (!isCopperInsulated(inv.getStack(8))) return false;

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
        return ModRecipes.ELECTRONIC_CIRCUIT_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
