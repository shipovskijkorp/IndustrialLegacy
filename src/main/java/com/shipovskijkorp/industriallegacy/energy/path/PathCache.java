package com.shipovskijkorp.industriallegacy.energy.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, intermediate cache for energy paths.
 *
 * <p>IC2 uses persistent grids with path caches; we start with a short TTL cache and explicit
 * invalidation, then migrate to true grids later.</p>
 */
public final class PathCache {
    private PathCache() {}

    private static final long PATH_CACHE_TTL_TICKS = 20L;

    private static final Map<World, Map<Long, Entry>> CACHE = new HashMap<>();

    private record Entry(long builtAtTick, List<EnergyPath> paths) {}

    public static List<EnergyPath> getOrBuild(World world, BlockPos sourcePos, BlockPos startCablePos) {
        long now = world.getTime();
        Map<Long, Entry> wc = CACHE.computeIfAbsent(world, w -> new HashMap<>());
        long key = startCablePos.asLong();
        Entry entry = wc.get(key);
        if (entry != null && (now - entry.builtAtTick) <= PATH_CACHE_TTL_TICKS) {
            return entry.paths;
        }

        List<EnergyPath> built = PathFinder.buildPaths(world, sourcePos, startCablePos);
        wc.put(key, new Entry(now, built));
        return built;
    }

    public static void invalidate(World world, BlockPos anyCablePos) {
        Map<Long, Entry> wc = CACHE.get(world);
        if (wc == null) return;
        wc.remove(anyCablePos.asLong());
    }
}
