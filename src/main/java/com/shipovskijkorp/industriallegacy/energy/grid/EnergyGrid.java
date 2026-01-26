package com.shipovskijkorp.industriallegacy.energy.grid;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Immutable cached cable grid (connected component).
 */
public final class EnergyGrid {

    private final int id;
    private final LongOpenHashSet cables;
    private final Long2ObjectOpenHashMap<long[]> neighbors;
    private final Long2DoubleOpenHashMap loss;
    private final Long2LongOpenHashMap capacity;

    EnergyGrid(
            int id,
            LongOpenHashSet cables,
            Long2ObjectOpenHashMap<long[]> neighbors,
            Long2DoubleOpenHashMap loss,
            Long2LongOpenHashMap capacity
    ) {
        this.id = id;
        this.cables = cables;
        this.neighbors = neighbors;
        this.loss = loss;
        this.capacity = capacity;
        this.loss.defaultReturnValue(0.0);
        this.capacity.defaultReturnValue(0L);
    }

    public int id() {
        return id;
    }

    /** Set of cable positions (packed long) in this grid. */
    public LongOpenHashSet cables() {
        return cables;
    }

    public boolean containsCable(long posLong) {
        return cables.contains(posLong);
    }

    /** Neighbor cable positions for a given cable (packed long). */
    public long[] neighbors(long posLong) {
        long[] n = neighbors.get(posLong);
        return n == null ? new long[0] : n;
    }

    /** Per-cable loss contribution (added when the packet traverses that cable). */
    public double loss(long posLong) {
        return loss.get(posLong);
    }

    /** Conductor capacity for over-voltage checks (packet size). */
    public long capacity(long posLong) {
        return capacity.get(posLong);
    }
}
