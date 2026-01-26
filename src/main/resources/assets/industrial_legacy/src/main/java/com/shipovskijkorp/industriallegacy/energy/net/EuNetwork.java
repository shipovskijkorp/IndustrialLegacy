package com.shipovskijkorp.industriallegacy.energy.net;

import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.calc.EuEnergyCalculator;
import com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Public facade for energy routing and invalidation.
 */
public final class EuNetwork {
    private EuNetwork() {}

    /** @return amount spent (extracted) from the source (EU). */
    public static long route(World world, BlockPos sourcePos, IEuEnergyStorage source, Direction outSide, long maxAmount) {
        return EuEnergyCalculator.route(world, sourcePos, source, outSide, maxAmount);
    }

    public static void invalidate(World world) {
        if (world == null) return;
        EnergyNetLocal.get(world).invalidateAll();
    }

    public static void invalidate(World world, BlockPos pos) {
        if (world == null) return;
        EnergyNetLocal.get(world).invalidateAt(pos);
    }
}
