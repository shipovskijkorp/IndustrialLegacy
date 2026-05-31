package com.shipovskijkorp.industriallegacy.reactor.api;

import net.minecraft.item.ItemStack;

/**
 * Minimal-but-useful IL-like reactor component interface.
 */
public interface IReactorComponent {

    default void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        // default no-op
    }

    default boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                       int youX, int youY, int pulseX, int pulseY, boolean heatRun) {
        return false;
    }

    default boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return false;
    }

    default int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return 0;
    }

    default int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return 0;
    }

    /**
     * Alters heat stored in the component. Returns leftover heat that couldn't be stored.
     */
    default int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        return heat;
    }

    default float influenceExplosion(ItemStack stack, IReactor reactor) {
        return 0.0f;
    }

    default boolean canBePlacedIn(ItemStack stack, IReactor reactor) {
        return true;
    }
}
