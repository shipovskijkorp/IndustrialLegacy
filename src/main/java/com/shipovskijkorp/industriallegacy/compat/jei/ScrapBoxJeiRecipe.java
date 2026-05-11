package com.shipovskijkorp.industriallegacy.compat.jei;

import net.minecraft.item.ItemStack;

public record ScrapBoxJeiRecipe(ItemStack output, float chance) {
    public ItemStack output() {
        return output.copy();
    }
}
