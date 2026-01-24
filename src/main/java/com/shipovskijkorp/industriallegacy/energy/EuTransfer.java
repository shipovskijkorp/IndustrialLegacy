package com.shipovskijkorp.industriallegacy.energy;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Legacy compatibility wrapper.
 *
 * <p>New code should use {@link com.shipovskijkorp.industriallegacy.energy.util.EuTransfer}.</p>
 */
public final class EuTransfer {
    private EuTransfer() {}

    public static IEuEnergyStorage getNeighborStorage(World world, BlockPos pos, Direction dir) {
        return (IEuEnergyStorage) com.shipovskijkorp.industriallegacy.energy.util.EuTransfer.getNeighborStorage(world, pos, dir);
    }

    public static long tryExtract(World world, BlockPos pos, Direction dir, long amount, boolean simulate) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuTransfer.tryExtract(world, pos, dir, amount, simulate);
    }

    public static long tryInsert(World world, BlockPos pos, Direction dir, long amount, boolean simulate) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuTransfer.tryInsert(world, pos, dir, amount, simulate);
    }
}
