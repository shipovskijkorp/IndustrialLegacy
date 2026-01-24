package com.shipovskijkorp.industriallegacy.energy;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Legacy compatibility wrapper.
 *
 * <p>New code should use {@link com.shipovskijkorp.industriallegacy.energy.net.EuNetwork}.</p>
 */
public final class EuNetwork {
    private EuNetwork() {}

    public static long route(World world, BlockPos sourcePos, IEuEnergyStorage source, Direction outSide, long maxAmount) {
        return com.shipovskijkorp.industriallegacy.energy.net.EuNetwork.route(world, sourcePos, source, outSide, maxAmount);
    }

    public static void invalidate(World world, BlockPos anyCablePos) {
        com.shipovskijkorp.industriallegacy.energy.net.EuNetwork.invalidate(world, anyCablePos);
    }
}
