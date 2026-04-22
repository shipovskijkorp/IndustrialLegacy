package com.shipovskijkorp.industriallegacy.item.tool;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Generic item-in-hand mode switch API used by the universal mode-switch key.
 */
public interface IModeSwitchableItem {
    /** Cycle to the next mode and return the new mode index. */
    int cycleMode(ItemStack stack, ServerPlayerEntity player);

    /** Human-readable mode name for HUD/chat feedback. */
    Text getModeName(ItemStack stack);
}
