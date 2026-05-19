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

import java.util.ArrayList;
import java.util.List;

public final class IniShapelessCraftingRecipe extends SpecialCraftingRecipe {
    private final List<IlCraftingIngredient> inputs;
    private final ItemStack result;

    public IniShapelessCraftingRecipe(Identifier id, CraftingRecipeCategory category, List<IlCraftingIngredient> inputs, ItemStack result) {
        super(id, category);
        this.inputs = List.copyOf(inputs);
        this.result = result.copy();
    }

    public List<IlCraftingIngredient> inputs() {
        return inputs;
    }

    public ItemStack resultStack() {
        return result.copy();
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        if (stacks.size() != inputs.size()) return false;

        boolean[] used = new boolean[inputs.size()];
        return matchShapeless(stacks, 0, used);
    }

    private boolean matchShapeless(List<ItemStack> stacks, int stackIndex, boolean[] used) {
        if (stackIndex >= stacks.size()) return true;
        ItemStack stack = stacks.get(stackIndex);
        for (int i = 0; i < inputs.size(); i++) {
            if (used[i]) continue;
            if (!inputs.get(i).test(stack)) continue;
            used[i] = true;
            if (matchShapeless(stacks, stackIndex + 1, used)) return true;
            used[i] = false;
        }
        return false;
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
            ItemStack remaining = toolRemainder(stack);
            if (remaining.isEmpty()) remaining = stack.getRecipeRemainder();
            remainder.set(i, remaining);
        }
        return remainder;
    }

    private static ItemStack toolRemainder(ItemStack stack) {
        if (!stack.isOf(ModItems.FORGE_HAMMER) && !stack.isOf(ModItems.CUTTER) && !stack.isOf(ModItems.PAINTER)) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        if (!copy.isDamageable()) return copy;
        copy.setDamage(copy.getDamage() + 1);
        return copy.getDamage() > copy.getMaxDamage() ? ItemStack.EMPTY : copy;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= inputs.size();
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INI_SHAPELESS_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
