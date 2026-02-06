package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
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
 * Advanced RE-Battery recipe.
 *
 * pattern:
 *  "CBC"
 *  "BSB"
 *  "BLB"
 *
 * where:
 *  C = copper cable (insulation=1)
 *  B = bronze casing
 *  S = sulfur
 *  L = lead dust
 *
 * Result: advanced_re_battery (empty)
 */
public final class AdvancedReBatteryRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public AdvancedReBatteryRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        // Cables at (0,0) and (2,0)
        if (!isCopperCableIns1(inv.getStack(0))) return false;
        if (!isCopperCableIns1(inv.getStack(2))) return false;

        // Bronze casings at B positions: (1,0) (0,1) (2,1) (0,2) (2,2)
        if (!inv.getStack(1).isOf(ModItems.BRONZE_CASING)) return false;
        if (!inv.getStack(3).isOf(ModItems.BRONZE_CASING)) return false;
        if (!inv.getStack(5).isOf(ModItems.BRONZE_CASING)) return false;
        if (!inv.getStack(6).isOf(ModItems.BRONZE_CASING)) return false;
        if (!inv.getStack(8).isOf(ModItems.BRONZE_CASING)) return false;

        // Sulfur at center (1,1)
        if (!inv.getStack(4).isOf(ModItems.SULFUR)) return false;

        // Lead dust at (1,2)
        if (!inv.getStack(7).isOf(ModItems.LEAD_DUST)) return false;

        return true;
    }

    private static boolean isCopperCableIns1(ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof CableItem)) return false;
        if (CableItem.getKind(s) != CableKind.COPPER) return false;
        return CableItem.getInsulation(s) == 1;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        ItemStack out = result.copy();
        if (out.getItem() instanceof IElectricItem ei) {
            ei.setEnergy(out, 0L);
        }
        return out;
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
        return ModRecipes.ADVANCED_RE_BATTERY_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
