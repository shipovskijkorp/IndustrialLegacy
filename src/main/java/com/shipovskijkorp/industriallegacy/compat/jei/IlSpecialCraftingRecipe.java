package com.shipovskijkorp.industriallegacy.compat.jei;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class IlSpecialCraftingRecipe {
    private final Identifier id;
    private final List<List<ItemStack>> inputs;
    private final ItemStack output;
    private final boolean shapeless;

    public IlSpecialCraftingRecipe(Identifier id, List<List<ItemStack>> inputs, ItemStack output, boolean shapeless) {
        this.id = id;
        this.inputs = normalize(inputs);
        this.output = output.copy();
        this.shapeless = shapeless;
    }

    public Identifier id() {
        return id;
    }

    public List<List<ItemStack>> inputs() {
        return inputs;
    }

    public ItemStack output() {
        return output.copy();
    }

    public boolean shapeless() {
        return shapeless;
    }

    private static List<List<ItemStack>> normalize(List<List<ItemStack>> in) {
        List<List<ItemStack>> out = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            if (i < in.size()) {
                out.add(copyList(in.get(i)));
            } else {
                out.add(List.of());
            }
        }
        return List.copyOf(out);
    }

    private static List<ItemStack> copyList(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> out = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                out.add(stack.copy());
            }
        }
        return List.copyOf(out);
    }
}
