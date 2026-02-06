package com.shipovskijkorp.industriallegacy.reactor.api;

import net.minecraft.item.ItemStack;

/**
 * Minimal IC2-like reactor component interface.
 * Ported behavior should follow IC2 1.12.2 Experimental semantics 1:1.
 */
public interface IReactorComponent {

    default void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        // default no-op
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
}
