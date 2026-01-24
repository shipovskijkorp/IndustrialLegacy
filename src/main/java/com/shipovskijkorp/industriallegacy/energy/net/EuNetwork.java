package com.shipovskijkorp.industriallegacy.energy.net;

import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.calc.EuEnergyCalculator;
import com.shipovskijkorp.industriallegacy.energy.path.PathCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Public facade for EU routing through cables.
 *
 * <p>This class is the new home for energy-net logic (IC2-like). The legacy
 * {@code com.shipovskijkorp.industriallegacy.energy.EuNetwork} remains as a thin wrapper.</p>
 */
public final class EuNetwork {
    private EuNetwork() {}

    public static long route(World world, BlockPos sourcePos, IEuEnergyStorage source, Direction outSide, long maxAmount) {
        return EuEnergyCalculator.route(world, sourcePos, source, outSide, maxAmount);
    }

    public static void invalidate(World world, BlockPos anyCablePos) {
        PathCache.invalidate(world, anyCablePos);
    }
}
