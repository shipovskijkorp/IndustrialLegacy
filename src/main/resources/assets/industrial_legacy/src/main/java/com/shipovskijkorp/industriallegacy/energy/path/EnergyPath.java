package com.shipovskijkorp.industriallegacy.energy.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * IC2-like energy path description.
 *
 * @param sinkPos   sink block position
 * @param intoSink  direction pointing from cable into sink
 * @param loss      summed conduction loss along cable blocks on this path (EU)
 * @param minCapacity minimum cable capacity along the path (EU/packet)
 * @param cables    ordered cable positions from source-adjacent to sink-adjacent
 */
public record EnergyPath(
        BlockPos sinkPos,
        Direction intoSink,
        double loss,
        long minCapacity,
        List<BlockPos> cables
) {}
