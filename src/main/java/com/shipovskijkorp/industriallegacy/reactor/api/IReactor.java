package com.shipovskijkorp.industriallegacy.reactor.api;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal IC2-like reactor interface used by reactor components.
 *
 * NOTE: This is intentionally small; it will be expanded when the reactor is ported.
 */
public interface IReactor {
    @Nullable ItemStack getItemAt(int x, int y);

    void setItemAt(int x, int y, @Nullable ItemStack stack);

    int getHeat();

    void setHeat(int heat);

    int getMaxHeat();
}
