package com.shipovskijkorp.industriallegacy.energy.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Cached energy route from the first cable node to a sink.
 *
 * <p>loss and minCapacity are cable-only (the first cable on the path is included).</p>
 */
public record EnergyPath(
        BlockPos sinkPos,
        Direction intoSink,
        double loss,
        long minCapacity,
        List<BlockPos> cables
) {}
