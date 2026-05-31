package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * IL @filler shapeless repair recipe.
 *
 * The original INI syntax uses an implicit damaged output item as one ingredient:
 *   redstone @filler*10000 = rsh_condensator
 * meaning: damaged RSH condensator + redstone -> same condensator with less stored heat.
 */
public final class IniFillerRepairRecipe extends SpecialCraftingRecipe {
    private static final String NBT_REACTOR_HEAT = "il_reactor_heat";

    private final List<IlCraftingIngredient> repairItems;
    private final ItemStack target;
    private final int repairAmount;

    public IniFillerRepairRecipe(Identifier id, CraftingRecipeCategory category, List<IlCraftingIngredient> repairItems, ItemStack target, int repairAmount) {
        super(id, category);
        this.repairItems = List.copyOf(repairItems);
        this.target = target.copy();
        this.repairAmount = Math.max(1, repairAmount);
    }

    public List<IlCraftingIngredient> repairItems() {
        return repairItems;
    }

    public ItemStack targetStack() {
        return target.copy();
    }

    public int repairAmount() {
        return repairAmount;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        List<ItemStack> stacks = nonEmptyStacks(inv);
        if (stacks.size() != repairItems.size() + 1) return false;

        for (int targetIndex = 0; targetIndex < stacks.size(); targetIndex++) {
            ItemStack candidate = stacks.get(targetIndex);
            if (!isRepairableTarget(candidate)) continue;

            List<ItemStack> remaining = new ArrayList<>(stacks);
            remaining.remove(targetIndex);
            boolean[] used = new boolean[repairItems.size()];
            if (matchRepairItems(remaining, 0, used)) return true;
        }
        return false;
    }

    private boolean matchRepairItems(List<ItemStack> stacks, int stackIndex, boolean[] used) {
        if (stackIndex >= stacks.size()) return true;
        ItemStack stack = stacks.get(stackIndex);
        for (int i = 0; i < repairItems.size(); i++) {
            if (used[i]) continue;
            if (!repairItems.get(i).test(stack)) continue;
            used[i] = true;
            if (matchRepairItems(stacks, stackIndex + 1, used)) return true;
            used[i] = false;
        }
        return false;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!isRepairableTarget(stack)) continue;
            ItemStack repaired = stack.copy();
            setReactorHeat(repaired, Math.max(0, getReactorHeat(repaired) - repairAmount));
            repaired.setCount(1);
            return repaired;
        }
        return target.copy();
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        return DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= repairItems.size() + 1;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return target;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INI_FILLER_REPAIR_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    private boolean isRepairableTarget(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(target.getItem()) && getReactorHeat(stack) > 0;
    }

    private static List<ItemStack> nonEmptyStacks(RecipeInputInventory inv) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    private static int getReactorHeat(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt(NBT_REACTOR_HEAT);
    }

    private static void setReactorHeat(ItemStack stack, int heat) {
        if (heat <= 0) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_REACTOR_HEAT);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
        } else {
            stack.getOrCreateNbt().putInt(NBT_REACTOR_HEAT, heat);
        }
    }
}
