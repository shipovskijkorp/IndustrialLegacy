package com.shipovskijkorp.industriallegacy.energy.grid;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * IL-style per-world EnergyNetLocal.
 *
 * <p>This implementation focuses on the core behavior needed for 1:1 gameplay:
 * <ul>
 *   <li>Cache cable connected components (grids) and best-loss routes to sinks.</li>
 *   <li>Incremental invalidation: clear only the affected grid when a cable changes.</li>
 *   <li>Per-node statistics (NodeStats) for detector cables.</li>
 *   <li>Over-voltage side effects applied at end of tick (meltdown/insulation/shock/explosion).</li>
 * </ul></p>
 */
public final class EnergyNetLocal {

    private static final Map<World, EnergyNetLocal> INSTANCES = new WeakHashMap<>();

    public static EnergyNetLocal get(World world) {
        if (world == null) throw new IllegalArgumentException("world");
        synchronized (INSTANCES) {
            return INSTANCES.computeIfAbsent(world, w -> new EnergyNetLocal());
        }
    }

    private final Map<Long, EnergyGrid> gridsById = new HashMap<>();
    private final Map<Long, Long> cableToGridId = new HashMap<>();

    private final NodeStatsTracker statsTracker = new NodeStatsTracker();

    // End-of-tick effects.
    private final Set<RoutePath> touchedPaths = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Long, Double> pendingSinkExplosions = new HashMap<>(); // sinkPosLong -> maxPacket

    private EnergyNetLocal() {}

    /**
     * Get routes to sinks for a given start cable. Cached per grid.
     */
    public List<RoutePath> getOrComputeRoutes(World world, BlockPos sourcePos, BlockPos startCablePos) {
        if (world == null) return List.of();
        if (startCablePos == null) return List.of();

        EnergyGrid grid = getOrBuildGrid(world, startCablePos);
        if (grid == null) return List.of();

        long key = mixStartSource(startCablePos.asLong(), sourcePos == null ? 0L : sourcePos.asLong());
        List<RoutePath> cached = grid.routesByStartCableWithSource.get(key);
        if (cached != null) return cached;

        List<RoutePath> routes = EnergyGridPathFinder.findRoutes(world, sourcePos, startCablePos);
        grid.routesByStartCableWithSource.put(key, routes);
        return routes;
    }

    /** Record conduction through the path's cable nodes for node-stats and later over-voltage effects. */
    public void recordPathTransfer(World world, RoutePath path, double supplied, double packetConducted) {
        if (world == null || path == null) return;
        long tick = world.getTime();
        path.record(tick, supplied, packetConducted);
        touchedPaths.add(path);

        // Per-cable stats.
        for (BlockPos p : path.cables()) {
            statsTracker.recordConduction(p.asLong(), supplied, packetConducted);
        }
    }

    /** Schedule a sink explosion (over-voltage). Applied at end of tick. */
    public void scheduleSinkExplosion(BlockPos sinkPos, double packet) {
        if (sinkPos == null) return;
        long key = sinkPos.asLong();
        Double prev = pendingSinkExplosions.get(key);
        if (prev == null || packet > prev) {
            pendingSinkExplosions.put(key, packet);
        }
    }

    /** Previous-tick node stats for a cable position. */
    public NodeStats getNodeStats(BlockPos cablePos) {
        if (cablePos == null) return NodeStats.ZERO;
        return statsTracker.getPrevious(cablePos.asLong());
    }

    /** Clear all caches for this world. */
    public void invalidateAll() {
        gridsById.clear();
        cableToGridId.clear();
    }

    /**
     * Targeted invalidation: clears only the grid that contains the given position if known.
     */
    public void invalidateAt(BlockPos pos) {
        if (pos == null) return;
        Long gid = cableToGridId.get(pos.asLong());
        if (gid == null) {
            // Fallback to full clear for correctness.
            invalidateAll();
            return;
        }
        EnergyGrid grid = gridsById.remove(gid);
        if (grid != null) {
            for (long p : grid.cables) {
                cableToGridId.remove(p);
            }
        } else {
            invalidateAll();
        }
    }

    /** Called at END_WORLD_TICK. Applies over-voltage effects and advances NodeStats snapshot. */
    public void onWorldTickEnd(World world) {
        if (world == null) return;
        long tick = world.getTime();

        // Cable effects (meltdown/insulation/shock) for paths that actually conducted something.
        if (!touchedPaths.isEmpty()) {
            IdentityHashMap<LivingEntity, Double> shockEnergyMap = new IdentityHashMap<>();
            for (RoutePath p : touchedPaths) {
                double packet = p.maxPacketConducted(tick);
                if (packet <= 0.0) continue;
                if (packet > p.minEffectEnergy) {
                    OverVoltageProcessor.applyCableEffects(world, p.cables(), packet, shockEnergyMap);
                }
            }
            if (world instanceof ServerWorld serverWorld) {
                OverVoltageProcessor.applyAccumulatedShockDamage(serverWorld, shockEnergyMap);
            }
            touchedPaths.clear();
        }

        // Sink explosions (over-voltage to machines).
        if (!pendingSinkExplosions.isEmpty()) {
            for (Map.Entry<Long, Double> e : pendingSinkExplosions.entrySet()) {
                BlockPos sinkPos = BlockPos.fromLong(e.getKey());
                OverVoltageProcessor.explodeSink(world, sinkPos, e.getValue());
            }
            pendingSinkExplosions.clear();
        }

        // Snapshot stats.
        statsTracker.endTick();
    }

    private EnergyGrid getOrBuildGrid(World world, BlockPos startCablePos) {
        Long gid = cableToGridId.get(startCablePos.asLong());
        if (gid != null) {
            EnergyGrid existing = gridsById.get(gid);
            if (existing != null) return existing;
            // stale mapping
            cableToGridId.remove(startCablePos.asLong());
        }

        // Build component and register all cable positions.
        EnergyGrid grid = EnergyGridBuilder.build(world, startCablePos, p -> cableToGridId.put(p, 0L));

        // Now that we know the id, fix all positions mapping.
        for (long p : grid.cables) {
            cableToGridId.put(p, grid.id);
        }
        gridsById.put(grid.id, grid);
        return grid;
    }

    private static long mixStartSource(long startCableLong, long sourceLong) {
        return (startCableLong * 31L) ^ sourceLong;
    }
}
