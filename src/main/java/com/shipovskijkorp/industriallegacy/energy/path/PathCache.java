package com.shipovskijkorp.industriallegacy.energy.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Temporary cache for energy paths until a full IL-like EnergyNetLocal is implemented.
 *
 * <p>Cache key is the first cable position adjacent to a source out-side.</p>
 */
public final class PathCache {
    private PathCache() {}

    private static final Map<World, Map<Long, List<EnergyPath>>> CACHE = new WeakHashMap<>();

    public static List<EnergyPath> getOrCompute(World world, BlockPos sourcePos, BlockPos startCablePos) {
        if (world == null) return List.of();
        Map<Long, List<EnergyPath>> byStart = CACHE.computeIfAbsent(world, w -> new HashMap<>());
        long key = startCablePos.asLong();
        List<EnergyPath> cached = byStart.get(key);
        if (cached != null) return cached;

        List<EnergyPath> paths = PathFinder.findPaths(world, sourcePos, startCablePos);
        byStart.put(key, paths);
        return paths;
    }

    public static void invalidate(World world) {
        if (world == null) return;
        Map<Long, List<EnergyPath>> byStart = CACHE.get(world);
        if (byStart != null) byStart.clear();
    }

    public static void invalidate(World world, BlockPos pos) {
        // TODO: targeted invalidation (connected component). For now, clear all for correctness.
        invalidate(world);
    }
}
