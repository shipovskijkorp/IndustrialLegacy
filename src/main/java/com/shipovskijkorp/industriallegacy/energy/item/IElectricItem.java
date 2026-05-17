package com.shipovskijkorp.industriallegacy.energy.item;

import net.minecraft.item.ItemStack;

/**
 * Minimal electric-item API (IL-style).
 *
 * Implementations store energy however they want (usually NBT).
 * All methods are INSTANCE methods (do NOT make them static).
 */
public interface IElectricItem {

    long getEnergy(ItemStack stack);

    void setEnergy(ItemStack stack, long energy);

    long getCapacity(ItemStack stack);

    long getTransferLimit(ItemStack stack);

    int getTier(ItemStack stack);

    /**
     * IC2 IElectricItem.canProvideEnergy equivalent.
     * Batteries and energy packs return true; tools/armor normally return false.
     */
    default boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    default float getChargeRatio(ItemStack stack) {
        long cap = getCapacity(stack);
        if (cap <= 0L) return 0.0f;
        return (float) ((double) getEnergy(stack) / (double) cap);
    }
}
