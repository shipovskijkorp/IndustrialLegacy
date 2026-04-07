package com.shipovskijkorp.industriallegacy.block.entity;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Common contract for electric storage blocks that expose IL-style redstone mode cycling.
 */
public interface RedstoneModeCycleTarget {
    void cycleRedstoneMode(@Nullable ServerPlayerEntity player);
}
