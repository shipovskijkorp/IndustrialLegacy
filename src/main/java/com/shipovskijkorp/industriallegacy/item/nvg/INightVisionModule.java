package com.shipovskijkorp.industriallegacy.item.nvg;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Marker + helpers for items that provide Night Vision when enabled.
 * Toggle state is stored in item NBT ("active") to match IC2 semantics.
 */
public interface INightVisionModule {

    String NBT_ACTIVE = "active";

    default boolean isNightVisionActive(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(NBT_ACTIVE);
    }

    default void setNightVisionActive(ItemStack stack, boolean active) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(NBT_ACTIVE, active);
    }
}
