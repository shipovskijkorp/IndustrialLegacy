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

import java.util.List;

public final class IniShapedCraftingRecipe extends SpecialCraftingRecipe {
    private final int width;
    private final int height;
    private final List<IlCraftingIngredient> inputs;
    private final ItemStack result;

    public IniShapedCraftingRecipe(Identifier id, CraftingRecipeCategory category, int width, int height, List<IlCraftingIngredient> inputs, ItemStack result) {
        super(id, category);
        this.width = width;
        this.height = height;
        this.inputs = List.copyOf(inputs);
        this.result = result.copy();
    }

    public int patternWidth() {
        return width;
    }

    public int patternHeight() {
        return height;
    }

    public List<IlCraftingIngredient> inputs() {
        return inputs;
    }

    public ItemStack resultStack() {
        return result.copy();
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        for (int x = 0; x <= inv.getWidth() - width; x++) {
            for (int y = 0; y <= inv.getHeight() - height; y++) {
                if (matchesAt(inv, x, y, false) || matchesAt(inv, x, y, true)) return true;
            }
        }
        return false;
    }

    private boolean matchesAt(RecipeInputInventory inv, int offsetX, int offsetY, boolean mirrored) {
        for (int y = 0; y < inv.getHeight(); y++) {
            for (int x = 0; x < inv.getWidth(); x++) {
                int patternX = x - offsetX;
                int patternY = y - offsetY;
                IlCraftingIngredient expected = IlCraftingIngredient.empty();
                if (patternX >= 0 && patternY >= 0 && patternX < width && patternY < height) {
                    int realX = mirrored ? width - patternX - 1 : patternX;
                    expected = inputs.get(realX + patternY * width);
                }
                ItemStack stack = inv.getStack(x + y * inv.getWidth());
                if (expected.isEmpty()) {
                    if (!stack.isEmpty()) return false;
                } else if (!expected.test(stack)) {
                    return false;
                }
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
        return width >= this.width && height >= this.height;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INI_SHAPED_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
