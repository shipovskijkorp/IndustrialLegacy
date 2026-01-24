package com.shipovskijkorp.industriallegacy.energy;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Small helpers for moving EU between adjacent tiles.
 *
 * <p>Higher level routing is handled by {@link EuNetwork}.</p>
 */
public final class EuTransfer {
    private EuTransfer() {}

    /**
     * Get an {@link IEuEnergyStorage} at the position, if any.
     */
    public static IEuEnergyStorage getStorage(World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        return (be instanceof IEuEnergyStorage s) ? s : null;
    }

    /**
     * Try to inject energy into a neighbor tile.
     *
     * @param fromPos origin block position
     * @param dir     direction towards the neighbor
     * @param amount  amount to inject (EU)
     * @param voltageTier voltage tier used for overvoltage checks
     * @param simulate if true, don't change state
     * @return amount that was accepted (EU)
     */
    public static double tryInjectNeighbor(World world, BlockPos fromPos, Direction dir, double amount, int voltageTier, boolean simulate) {
        if (amount <= 0.0) return 0.0;

        BlockPos toPos = fromPos.offset(dir);
        IEuEnergyStorage storage = getStorage(world, toPos);
        if (storage == null) return 0.0;

        Direction fromSide = dir.getOpposite();
        if (!storage.canInsert(fromSide)) return 0.0;

        double rejected = storage.injectEnergy(fromSide, amount, voltageTier, simulate);
        return Math.max(0.0, amount - rejected);
    }

    /**
     * Try to extract energy from a neighbor tile.
     *
     * @return amount extracted (EU)
     */
    public static double tryExtractNeighbor(World world, BlockPos fromPos, Direction dir, double amount, boolean simulate) {
        if (amount <= 0.0) return 0.0;

        BlockPos toPos = fromPos.offset(dir);
        IEuEnergyStorage storage = getStorage(world, toPos);
        if (storage == null) return 0.0;

        Direction toSide = dir.getOpposite();
        if (!storage.canExtract(toSide)) return 0.0;

        return storage.drawEnergy(amount, simulate);
    }
}
