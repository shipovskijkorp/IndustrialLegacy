package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.energy.path.EnergyPath;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * IC2-inspired per-world EnergyNet cache.
 *
 * <p>Key goals:
 * <ul>
 *   <li>Keep a cached cable graph (connected components = grids).</li>
 *   <li>Invalidate incrementally on block changes, rebuilding only affected grids.</li>
 *   <li>Cache Dijkstra results per start cable for fast repeated emissions.</li>
 * </ul>
 *
 * <p>This is intentionally server-side only; callers should avoid using it on the client.</p>
 */
public final class EnergyNetLocal {

    private static final Map<World, EnergyNetLocal> INSTANCES = new WeakHashMap<>();

    public static EnergyNetLocal get(World world) {
        return INSTANCES.computeIfAbsent(world, EnergyNetLocal::new);
    }

    /** Clear all caches for this world (topology unknown). */
    public static void invalidate(@Nullable World world) {
        if (world == null) return;
        EnergyNetLocal net = INSTANCES.get(world);
        if (net != null) net.invalidateAll();
    }

    /** Targeted invalidation around a position (cable placed/removed, splitter toggled, etc.). */
    public static void invalidate(@Nullable World world, @Nullable BlockPos pos) {
        if (world == null || pos == null) return;
        EnergyNetLocal net = INSTANCES.get(world);
        if (net != null) net.invalidateAround(pos);
    }

    // ---- per-world instance ----

    private final World world;

    /** cablePos -> gridId. 0 = unknown. */
    private final Long2IntOpenHashMap cableToGrid = new Long2IntOpenHashMap();

    /** gridId -> grid. */
    private final Int2ObjectOpenHashMap<EnergyGrid> grids = new Int2ObjectOpenHashMap<>();

    /** grid ids that must be rebuilt. */
    private final IntOpenHashSet dirtyGrids = new IntOpenHashSet();

    /** If true, all grids are invalid and caches must be cleared. */
    private boolean dirtyAll = true;

    /** startCablePos -> cached paths. */
    private final Long2ObjectOpenHashMap<List<EnergyPath>> pathCache = new Long2ObjectOpenHashMap<>();

    /** startCablePos -> gridId used when caching; for targeted cache eviction. */
    private final Long2IntOpenHashMap cachedStartToGrid = new Long2IntOpenHashMap();

    /** Seeds (positions) that might require discovering a new grid (new cables). */
    private final LongOpenHashSet dirtySeeds = new LongOpenHashSet();

    private int nextGridId = 1;

    private EnergyNetLocal(World world) {
        this.world = world;
        this.cableToGrid.defaultReturnValue(0);
        this.cachedStartToGrid.defaultReturnValue(0);
    }

    /**
     * Get cached best-loss paths from a start cable to all reachable sinks.
     *
     * <p>The {@code sourcePos} is used only to prevent immediately routing back into the source.</p>
     */
    public List<EnergyPath> getOrComputePaths(BlockPos sourcePos, BlockPos startCablePos) {
        if (world.isClient) return List.of();

        long startKey = startCablePos.asLong();

        if (dirtyAll) {
            clearAll();
        } else {
            processDirtyGrids();
        }

        List<EnergyPath> cached = pathCache.get(startKey);
        if (cached != null) return cached;

        EnergyGrid grid = ensureGridFor(startCablePos);
        if (grid == null) return List.of();

        List<EnergyPath> paths = EnergyGridPathFinder.findPaths(world, grid, sourcePos, startCablePos);
        pathCache.put(startKey, paths);
        cachedStartToGrid.put(startKey, grid.id());
        return paths;
    }

    public void invalidateAll() {
        dirtyAll = true;
    }

    public void invalidateAround(BlockPos pos) {
        // Cable topology affects its own position and immediate neighbors.
        markDirtySeed(pos);
        for (var dir : net.minecraft.util.math.Direction.values()) {
            markDirtySeed(pos.offset(dir));
        }

        // If this position (or its neighbors) belonged to an existing grid, mark that grid dirty.
        markDirtyGridIfKnown(pos);
        for (var dir : net.minecraft.util.math.Direction.values()) {
            markDirtyGridIfKnown(pos.offset(dir));
        }
    }

    private void markDirtySeed(BlockPos pos) {
        dirtySeeds.add(pos.asLong());
    }

    private void markDirtyGridIfKnown(BlockPos pos) {
        int gid = cableToGrid.get(pos.asLong());
        if (gid != 0) dirtyGrids.add(gid);
    }

    private void clearAll() {
        dirtyAll = false;
        dirtyGrids.clear();
        dirtySeeds.clear();
        grids.clear();
        cableToGrid.clear();
        pathCache.clear();
        cachedStartToGrid.clear();
        nextGridId = 1;
    }

    private void processDirtyGrids() {
        if (dirtyGrids.isEmpty() && dirtySeeds.isEmpty()) return;

        // Drop cached paths for any dirty grids.
        if (!dirtyGrids.isEmpty()) {
            evictCachedPathsForDirtyGrids();

            // Remove old grids and cable mappings; they will be rebuilt lazily.
            for (int gid : dirtyGrids) {
                EnergyGrid grid = grids.remove(gid);
                if (grid == null) continue;
                for (long cable : grid.cables()) {
                    cableToGrid.remove(cable);
                }
            }
            dirtyGrids.clear();
        }

        // Dirty seeds are kept; they help discovering new grids (newly placed cables).
        // We don't eagerly build grids here to keep costs on-demand.
    }

    private void evictCachedPathsForDirtyGrids() {
        // Scan the cached start->grid map; size is typically small.
        LongIterator it = cachedStartToGrid.keySet().iterator();
        LongOpenHashSet toRemove = new LongOpenHashSet();
        while (it.hasNext()) {
            long start = it.nextLong();
            int gid = cachedStartToGrid.get(start);
            if (gid != 0 && dirtyGrids.contains(gid)) {
                toRemove.add(start);
            }
        }
        LongIterator rit = toRemove.iterator();
        while (rit.hasNext()) {
            long start = rit.nextLong();
            pathCache.remove(start);
            cachedStartToGrid.remove(start);
        }
    }

    private @Nullable EnergyGrid ensureGridFor(BlockPos anyCablePos) {
        long key = anyCablePos.asLong();
        int gid = cableToGrid.get(key);
        if (gid != 0) {
            EnergyGrid existing = grids.get(gid);
            if (existing != null) return existing;
            // Mapping exists but grid got dropped; treat as unknown.
            cableToGrid.remove(key);
        }

        // Build a new grid starting from this position.
        EnergyGrid grid = EnergyGridBuilder.build(world, anyCablePos, nextGridId);
        if (grid == null) return null;

        grids.put(grid.id(), grid);
        for (long cable : grid.cables()) {
            cableToGrid.put(cable, grid.id());
        }
        nextGridId = Math.max(nextGridId, grid.id() + 1);

        // We have now "resolved" any dirty seeds inside this grid.
        // (Keep it simple: just remove those we know about.)
        if (!dirtySeeds.isEmpty()) {
            LongIterator it = dirtySeeds.iterator();
            while (it.hasNext()) {
                long seed = it.nextLong();
                if (grid.containsCable(seed)) {
                    it.remove();
                }
            }
        }

        return grid;
    }
}
