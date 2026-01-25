package com.shipovskijkorp.industriallegacy.energy;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Legacy compatibility wrapper. Prefer {@link com.shipovskijkorp.industriallegacy.energy.util.EuTransfer}.
 */
@Deprecated
public final class EuTransfer {
    private EuTransfer() {}

    public static long tryExtract(World world, BlockPos pos, Direction side, long maxAmount, boolean simulate) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuTransfer.tryExtract(world, pos, side, maxAmount, simulate);
    }

    public static long tryInsert(World world, BlockPos pos, Direction side, long amount, boolean simulate) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuTransfer.tryInsert(world, pos, side, amount, simulate);
    }
}
