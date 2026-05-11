package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.block.entity.RecyclerBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;

public final class RecyclerJeiRecipe {
    private final List<ItemStack> inputs;
    private final ItemStack output;

    private RecyclerJeiRecipe(List<ItemStack> inputs, ItemStack output) {
        this.inputs = List.copyOf(inputs);
        this.output = output.copy();
    }

    public static RecyclerJeiRecipe create() {
        List<ItemStack> inputs = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && !RecyclerBlockEntity.isRecyclerBlacklisted(stack)) {
                inputs.add(stack);
            }
        }
        return new RecyclerJeiRecipe(inputs, new ItemStack(ModItems.SCRAP));
    }

    public List<ItemStack> inputs() {
        return inputs;
    }

    public ItemStack output() {
        return output.copy();
    }
}
