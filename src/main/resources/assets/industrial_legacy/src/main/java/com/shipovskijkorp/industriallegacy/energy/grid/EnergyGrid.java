package com.shipovskijkorp.industriallegacy.energy.grid;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A connected component (grid) of cable blocks.
 */
final class EnergyGrid {

    final long id;
    final LongSet cables; // positions of cable blocks
    final Map<Long, List<RoutePath>> routesByStartCable = new HashMap<>();
    final Map<Long, List<RoutePath>> routesByStartCableWithSource = new HashMap<>();

    EnergyGrid(long id, LongSet cables) {
        this.id = id;
        this.cables = cables;
    }

    static LongSet newCableSet() {
        return new LongOpenHashSet();
    }
}
