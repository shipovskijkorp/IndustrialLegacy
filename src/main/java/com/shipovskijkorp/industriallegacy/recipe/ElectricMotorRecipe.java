package com.shipovskijkorp.industriallegacy.recipe;

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
 * IC2 electric motor recipe, supports both mirrored variants.
 */
public final class ElectricMotorRecipe extends SpecialCraftingRecipe {
    private final ItemStack result;

    public ElectricMotorRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    private static boolean isCoil(ItemStack stack) {
        return stack.isOf(ModItems.COIL);
    }

    private static boolean isTinCasing(ItemStack stack) {
        return stack.isOf(ModItems.TIN_CASING);
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;
        return matchesVariantA(inv) || matchesVariantB(inv);
    }

    private static boolean matchesVariantA(RecipeInputInventory inv) {
        return inv.getStack(0).isEmpty()
                && isTinCasing(inv.getStack(1))
                && inv.getStack(2).isEmpty()
                && isCoil(inv.getStack(3))
                && inv.getStack(4).isOf(net.minecraft.item.Items.IRON_INGOT)
                && isCoil(inv.getStack(5))
                && inv.getStack(6).isEmpty()
                && isTinCasing(inv.getStack(7))
                && inv.getStack(8).isEmpty();
    }

    private static boolean matchesVariantB(RecipeInputInventory inv) {
        return inv.getStack(0).isEmpty()
                && isCoil(inv.getStack(1))
                && inv.getStack(2).isEmpty()
                && isTinCasing(inv.getStack(3))
                && inv.getStack(4).isOf(net.minecraft.item.Items.IRON_INGOT)
                && isTinCasing(inv.getStack(5))
                && inv.getStack(6).isEmpty()
                && isCoil(inv.getStack(7))
                && inv.getStack(8).isEmpty();
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
        return ModRecipes.ELECTRIC_MOTOR_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
