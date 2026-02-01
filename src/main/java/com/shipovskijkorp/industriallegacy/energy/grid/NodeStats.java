package com.shipovskijkorp.industriallegacy.energy.grid;

/**
 * Per-node energy statistics (IL-style).
 *
 * <p>IL exposes energy-in/out and the maximum voltage tier observed during the last network
 * calculation. We keep the same concept for detector cables and debugging.</p>
 */
public record NodeStats(double energyIn, double energyOut, int voltageTier) {
    public static final NodeStats ZERO = new NodeStats(0.0, 0.0, 0);
}
