package com.shipovskijkorp.industriallegacy.energy.grid;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collections;
import java.util.List;

/**
 * Cached route from a starting cable to a sink.
 *
 * <p>Inspired by IC2's EnergyPath in EnergyCalculatorLeg: we track the maximum packet conducted
 * during a tick and total energy supplied, and apply cable/sink effects at end of tick.</p>
 */
public final class RoutePath {

    private final BlockPos sinkPos;
    private final Direction intoSink;
    private final double loss;
    private final List<BlockPos> cables;

    final double minConductorBreakdownEnergy;
    final double minInsulationBreakdownEnergy;
    final double minInsulationEnergyAbsorption;
    final double minEffectEnergy;

    // Tick-local stats.
    private long lastTick = Long.MIN_VALUE;
    private double energySupplied = 0.0;
    private double maxPacketConducted = 0.0;

    RoutePath(
            BlockPos sinkPos,
            Direction intoSink,
            double loss,
            List<BlockPos> cables,
            double minConductorBreakdownEnergy,
            double minInsulationBreakdownEnergy,
            double minInsulationEnergyAbsorption
    ) {
        this.sinkPos = sinkPos;
        this.intoSink = intoSink;
        this.loss = loss;
        this.cables = Collections.unmodifiableList(cables);
        this.minConductorBreakdownEnergy = minConductorBreakdownEnergy;
        this.minInsulationBreakdownEnergy = minInsulationBreakdownEnergy;
        this.minInsulationEnergyAbsorption = minInsulationEnergyAbsorption;
        this.minEffectEnergy = Math.min(minConductorBreakdownEnergy,
                Math.min(minInsulationBreakdownEnergy, minInsulationEnergyAbsorption));
    }

    public BlockPos sinkPos() {
        return sinkPos;
    }

    public Direction intoSink() {
        return intoSink;
    }

    public double loss() {
        return loss;
    }

    public List<BlockPos> cables() {
        return cables;
    }

    public double energySupplied(long tick) {
        return lastTick == tick ? energySupplied : 0.0;
    }

    public double maxPacketConducted(long tick) {
        return lastTick == tick ? maxPacketConducted : 0.0;
    }

    void record(long tick, double supplied, double packetConducted) {
        if (this.lastTick != tick) {
            this.lastTick = tick;
            this.energySupplied = 0.0;
            this.maxPacketConducted = 0.0;
        }
        if (supplied > 0.0) {
            this.energySupplied += supplied;
        }
        if (packetConducted > this.maxPacketConducted) {
            this.maxPacketConducted = packetConducted;
        }
    }
}
