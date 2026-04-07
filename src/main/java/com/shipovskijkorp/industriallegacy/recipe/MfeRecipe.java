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
 * MFE recipe:
 *  G E G
 *  E M E
 *  G E G
 *
 * G = gold cable (insulation = 2)
 * E = energy crystal
 * M = machine casing
 *
 * Result: MFE
 */
public final class MfeRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public MfeRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        if (!isGoldCableIns2(inv.getStack(0))) return false;
        if (!inv.getStack(1).isOf(ModItems.ENERGY_CRYSTAL)) return false;
        if (!isGoldCableIns2(inv.getStack(2))) return false;

        if (!inv.getStack(3).isOf(ModItems.ENERGY_CRYSTAL)) return false;
        if (!inv.getStack(4).isOf(ModBlocks.MACHINE_CASING.asItem())) return false;
        if (!inv.getStack(5).isOf(ModItems.ENERGY_CRYSTAL)) return false;

        if (!isGoldCableIns2(inv.getStack(6))) return false;
        if (!inv.getStack(7).isOf(ModItems.ENERGY_CRYSTAL)) return false;
        if (!isGoldCableIns2(inv.getStack(8))) return false;

        return true;
    }

    private static boolean isGoldCableIns2(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CableItem)) return false;
        if (CableItem.getKind(stack) != CableKind.GOLD) return false;
        return CableItem.getInsulation(stack) == 2;
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
        return ModRecipes.MFE_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
