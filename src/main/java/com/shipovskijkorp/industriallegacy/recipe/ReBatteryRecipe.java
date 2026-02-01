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
 * IL re_battery recipe (shaped_recipes.ini):
 *
 * pattern:
 *  " C "
 *  "TRT"
 *  "TRT"
 *
 * where:
 *  C = tin cable (insulation=1)
 *  T = tin casing
 *  R = redstone
 */
public final class ReBatteryRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public ReBatteryRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        // (1,0) tin cable ins=1
        ItemStack c = inv.getStack(1);
        if (c.isEmpty() || !(c.getItem() instanceof CableItem)) return false;
        if (CableItem.getKind(c) != CableKind.TIN) return false;
        if (CableItem.getInsulation(c) != 1) return false;

        // casings
        if (!inv.getStack(3).isOf(ModItems.TIN_CASING)) return false;
        if (!inv.getStack(5).isOf(ModItems.TIN_CASING)) return false;
        if (!inv.getStack(6).isOf(ModItems.TIN_CASING)) return false;
        if (!inv.getStack(8).isOf(ModItems.TIN_CASING)) return false;

        // redstone
        if (!inv.getStack(4).isOf(net.minecraft.item.Items.REDSTONE)) return false;
        if (!inv.getStack(7).isOf(net.minecraft.item.Items.REDSTONE)) return false;

        // empty slots: 0,2
        if (!inv.getStack(0).isEmpty()) return false;
        if (!inv.getStack(2).isEmpty()) return false;

        return true;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        ItemStack out = result.copy();
        // Newly crafted battery is empty (0 EU).
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
        return ModRecipes.RE_BATTERY_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
