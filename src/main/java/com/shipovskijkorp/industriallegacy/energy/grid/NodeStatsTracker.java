package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Tracks per-cable statistics for the current server tick and exposes the previous-tick snapshot.
 *
 * <p>We snapshot at {@code END_WORLD_TICK} to avoid ordering issues (cable block entities may tick
 * before or after energy emission sources).</p>
 */
final class NodeStatsTracker {

    private static final class Mutable {
        double in;
        double out;
        double maxPacket;

        void add(double amount, double packetSize) {
            // In IL, cables are both conductors and nodes; treat conduction as both in and out.
            this.in += amount;
            this.out += amount;
            if (packetSize > this.maxPacket) this.maxPacket = packetSize;
        }
    }

    private final Long2ObjectOpenHashMap<Mutable> current = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<NodeStats> previous = new Long2ObjectOpenHashMap<>();

    void recordConduction(long cablePosLong, double supplied, double packetConducted) {
        if (supplied <= 0.0 && packetConducted <= 0.0) return;
        Mutable m = current.get(cablePosLong);
        if (m == null) {
            m = new Mutable();
            current.put(cablePosLong, m);
        }
        // supplied can be 0 for over-voltage attempts, but we still want maxPacket.
        if (supplied > 0.0) {
            m.add(supplied, packetConducted);
        } else {
            if (packetConducted > m.maxPacket) m.maxPacket = packetConducted;
        }
    }

    NodeStats getPrevious(long cablePosLong) {
        NodeStats s = previous.get(cablePosLong);
        return s == null ? NodeStats.ZERO : s;
    }

    void endTick() {
        previous.clear();
        for (Long2ObjectMap.Entry<Mutable> e : current.long2ObjectEntrySet()) {
            Mutable m = e.getValue();
            int tier = EuUtil.tierFromPower(m.maxPacket);
            previous.put(e.getLongKey(), new NodeStats(m.in, m.out, tier));
        }
        current.clear();
    }
}
