package com.shipovskijkorp.industriallegacy.energy;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * EU routing through IC2-style cable blocks.
 *
 * <p>This is an intermediate implementation designed to be mechanically stable and consistent
 * with IC2 concepts (packets, tiers, per-cable loss, capacity burn). The long-term goal is
 * still a full 1:1 EnergyNet (non-classic) with proper caching, events, explosions, and
 * advanced blocks.</p>
 */
public final class EuNetwork {
    private EuNetwork() {}

    private static final int MAX_NODES = 4096;
    private static final double INF = 1e100;

    /** How long (in ticks) we keep cached path lists per world. */
    private static final long PATH_CACHE_TTL_TICKS = 20;

    /** World-scoped path cache keyed by start cable position. */
    private static final Map<World, Map<Long, PathCacheEntry>> PATH_CACHE = new HashMap<>();

    /**
     * Route EU from a source into sinks reachable from {@code sourcePos} via {@code outSide}.
     *
     * <p><b>IC2 semantics:</b> the network is packet/tier based; however the <i>energy spent</i>
     * by the source is {@code accepted + loss} (IC2's effectiveAmount). This allows storages
     * to "top off" the last few EU without wasting a full packet, while still requiring
     * {@link IEuEnergyStorage#isFullEnergyOutput()} sources to have at least one full packet
     * available before they can begin emitting.</p>
     *
     * @return EU <b>spent</b> from the source (integer EU), i.e. what was extracted from {@code source}
     */
    public static long route(World world, BlockPos sourcePos, IEuEnergyStorage source, Direction outSide, long maxAmount) {
        if (world == null) return 0;
        if (maxAmount <= 0) return 0;
        if (!source.canExtract(outSide)) return 0;

        BlockPos firstPos = sourcePos.offset(outSide);

        // Voltage / packet tier (IC2 passes tier separately from amount).
        final int sourceTier = source.getSourceTier(outSide);
        final long voltage = EuUtil.powerFromTier(sourceTier);

        // Direct sink neighbor.
        BlockEntity directBe = world.getBlockEntity(firstPos);
        if (directBe instanceof IEuEnergyStorage directSink) {
            Direction intoSink = outSide.getOpposite();
            if (directSink.canInsert(intoSink)) {
                return moveAlongPath(world, source, outSide, directSink, intoSink, /*path=*/List.of(), voltage, maxAmount, /*loss=*/0.0);
            }
            return 0;
        }

        // Cable graph.
        BlockState firstState = world.getBlockState(firstPos);
        if (!ModBlocks.isCable(firstState.getBlock())) {
            return 0;
        }
        if (!(firstState.getBlock() instanceof CableBlock startCable)) {
            return 0;
        }
        if (isSplitterDisabled(world, firstPos, startCable)) {
            return 0;
        }

        // Determine how much we can (and should) spend from the source this tick on this side.
        // This is NOT the energy that arrives at sinks; it's the source-side budget (accepted + loss).
        long budget = Math.min(maxAmount, voltage);
        long canExtractSim = source.extractEu(budget, outSide, true);
        if (canExtractSim <= 0) return 0;

        // IC2 "fullEnergy" start condition: don't begin emitting unless at least one full packet exists.
        if (source.isFullEnergyOutput()) {
            long full = source.extractEu(voltage, outSide, true);
            if (full < voltage) return 0;
        }

        long offerBudget = Math.min(canExtractSim, budget);
        if (offerBudget <= 0) return 0;

        // Build (or fetch) paths to sinks.
        List<Path> paths = getOrBuildPaths(world, sourcePos, firstPos);
        if (paths.isEmpty()) return 0;

        // IC2 fairness: 3/4 ticks choose a random path offset, otherwise start from 0.
        // This prevents deterministic bias when multiple equal-loss sinks exist.
        final boolean shuffle = (world.getTime() & 3L) != 0L;
        final int startIndex = (shuffle && paths.size() > 1) ? world.random.nextInt(paths.size()) : 0;

        long spentTotal = 0L;
        long remainingBudget = offerBudget;

        for (int i = 0; i < paths.size(); i++) {
            if (remainingBudget <= 0) break;
            int idx = (startIndex + i) % paths.size();
            Path p = paths.get(idx);

            // Fetch sink each time (chunk unload safety).
            BlockEntity be = world.getBlockEntity(p.sinkPos);
            if (!(be instanceof IEuEnergyStorage sink)) continue;
            Direction intoSink = p.intoSink;
            if (!sink.canInsert(intoSink)) continue;

            // Tier sanity (IC2: voltageTier is sourceTier).
            int sinkTier = sink.getSinkTier(intoSink);
            if (sinkTier > 0 && sourceTier > sinkTier) continue;

            // Over-voltage melts any cable on path (IC2 conductor breakdown). Voltage is tier-based.
            if (voltage > p.minCapacity) {
                meltFirstOverloadedCable(world, p.cables, voltage);
                continue;
            }

            double loss = applyLossRounding(p.loss);
            if (loss >= (double) remainingBudget) continue;

            // How much could arrive at the sink from the remaining source budget?
            long maxArrive = (long) Math.floor((double) remainingBudget - loss);
            if (maxArrive <= 0) continue;

            long demand = (long) Math.floor(sink.getDemandedEnergy(intoSink));
            if (demand <= 0) continue;

            long offerToSink = Math.min(maxArrive, demand);
            if (offerToSink <= 0) continue;

            long acceptedSim = sink.insertEu(offerToSink, intoSink, true);
            if (acceptedSim <= 0) continue;

            // Clamp accepted to what can be funded after loss.
            long maxAccepted = (long) Math.floor(Math.max(0.0, (double) remainingBudget - loss));
            if (acceptedSim > maxAccepted) {
                acceptedSim = maxAccepted;
                if (acceptedSim <= 0) continue;
            }

            // Source-side spend = accepted + loss (IC2 effectiveAmount).
            long spend = (long) Math.ceil((double) acceptedSim + loss);
            spend = Math.max(1L, Math.min(spend, remainingBudget));

            // FullEnergy sources may only emit if they had >=1 full packet; but the spend may be smaller.
            long extracted = source.extractEu(spend, outSide, false);
            if (extracted <= 0) continue;

            // Compute what arrives given the actually extracted energy.
            long arrive = (long) Math.floor(Math.max(0.0, (double) extracted - loss));
            if (arrive <= 0) {
                recordTransfer(world, p.cables, 0L);
                spentTotal += extracted;
                remainingBudget -= extracted;
                continue;
            }

            long inserted = sink.insertEu(Math.min(arrive, acceptedSim), intoSink, false);
            recordTransfer(world, p.cables, inserted);

            spentTotal += extracted;
            remainingBudget -= extracted;
        }

        return spentTotal;
    }

    // Kept for direct-neighbor moves and as a helper for potential future refactors.
    private static long moveAlongPath(
            World world,
            IEuEnergyStorage source,
            Direction outSide,
            IEuEnergyStorage sink,
            Direction intoSink,
            List<BlockPos> pathCables,
            long voltage,
            long maxBudget,
            double loss
    ) {
        if (!source.canExtract(outSide)) return 0;
        if (!sink.canInsert(intoSink)) return 0;

        long budget = Math.min(maxBudget, voltage);
        long canExtractSim = source.extractEu(budget, outSide, true);
        if (canExtractSim <= 0) return 0;
        if (source.isFullEnergyOutput()) {
            long full = source.extractEu(voltage, outSide, true);
            if (full < voltage) return 0;
        }

        // Voltage-based overload (IC2 conductor breakdown is based on tier, not the accepted amount).
        if (!pathCables.isEmpty() && wouldOverloadAnyCable(world, pathCables, voltage)) {
            meltFirstOverloadedCable(world, pathCables, voltage);
            return 0;
        }

        loss = applyLossRounding(loss);
        if (loss >= (double) canExtractSim) return 0;

        long arriveMax = (long) Math.floor((double) canExtractSim - loss);
        if (arriveMax <= 0) return 0;

        long demand = (long) Math.floor(sink.getDemandedEnergy(intoSink));
        if (demand <= 0) return 0;

        long offerToSink = Math.min(arriveMax, demand);
        if (offerToSink <= 0) return 0;

        long acceptedSim = sink.insertEu(offerToSink, intoSink, true);
        if (acceptedSim <= 0) return 0;

        long spend = (long) Math.ceil((double) acceptedSim + loss);
        spend = Math.max(1L, Math.min(spend, canExtractSim));

        long extracted = source.extractEu(spend, outSide, false);
        if (extracted <= 0) return 0;

        long arrive = (long) Math.floor(Math.max(0.0, (double) extracted - loss));
        if (arrive <= 0) {
            recordTransfer(world, pathCables, 0L);
            return extracted;
        }

        long inserted = sink.insertEu(Math.min(arrive, acceptedSim), intoSink, false);
        recordTransfer(world, pathCables, inserted);
        return extracted;
    }

    private static double applyLossRounding(double loss) {
        boolean round = ILConfig.getBool("misc/roundEnetLoss", false);
        if (!round) return loss;
        return (double) Math.round(loss);
    }

    private static boolean isSplitterDisabled(World world, BlockPos pos, CableBlock cable) {
        return cable.getKind() == CableKind.SPLITTER && world.isReceivingRedstonePower(pos);
    }

    private static boolean wouldOverloadAnyCable(World world, List<BlockPos> pathCables, long voltage) {
        for (BlockPos p : pathCables) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                if (voltage > cb.getKind().capacity) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void meltFirstOverloadedCable(World world, List<BlockPos> pathCables, long voltage) {
        for (BlockPos p : pathCables) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                if (voltage > cb.getKind().capacity) {
                    // Phase3: these blocks are registered without a BlockItem, so drops are deferred.
                    world.breakBlock(p, false);
                    return;
                }
            }
        }
    }

    private static void recordTransfer(World world, List<BlockPos> pathCables, long transferred) {
        if (pathCables.isEmpty()) return;
        for (BlockPos p : pathCables) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof CableBlockEntity cableBe) {
                cableBe.setLastTransferredEu(transferred);
            }
        }
    }

    /** A cached path from the start cable to a specific sink. */
    private record Path(BlockPos sinkPos, Direction intoSink, double loss, List<BlockPos> cables, int minCapacity) {}

    private record Node(BlockPos pos, double loss) {}

    private record PathCacheEntry(long builtAtTick, List<Path> paths) {}

    /**
     * Hard invalidate the whole world's path cache. This is conservative but correct.
     * Called by cable placement/removal and splitter redstone changes.
     */
    public static void invalidate(World world) {
        if (world == null) return;
        PATH_CACHE.remove(world);
    }

    /** Convenience overload for callers that have a position. */
    public static void invalidate(World world, BlockPos pos) {
        invalidate(world);
    }

    private static List<Path> getOrBuildPaths(World world, BlockPos sourcePos, BlockPos startCablePos) {
        long now = world.getTime();
        Map<Long, PathCacheEntry> wc = PATH_CACHE.computeIfAbsent(world, w -> new HashMap<>());
        long key = startCablePos.asLong();
        PathCacheEntry entry = wc.get(key);
        if (entry != null && (now - entry.builtAtTick) <= PATH_CACHE_TTL_TICKS) {
            return entry.paths;
        }

        List<Path> built = buildPaths(world, sourcePos, startCablePos);
        wc.put(key, new PathCacheEntry(now, built));
        return built;
    }

    private static List<Path> buildPaths(World world, BlockPos sourcePos, BlockPos startCablePos) {
        // Dijkstra by total conduction loss to each cable node.
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(Node::loss));
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();

        BlockState startState = world.getBlockState(startCablePos);
        if (!(startState.getBlock() instanceof CableBlock startCable)) return List.of();
        if (isSplitterDisabled(world, startCablePos, startCable)) return List.of();

        double startLoss = startCable.getKind().loss;
        dist.put(startCablePos.asLong(), startLoss);
        pq.add(new Node(startCablePos, startLoss));

        // For each sink, keep the best (lowest-loss) path found.
        Map<Long, Path> bestBySink = new HashMap<>();

        int visited = 0;
        while (!pq.isEmpty() && visited++ < MAX_NODES) {
            Node cur = pq.poll();
            double curLoss = cur.loss;
            long curKey = cur.pos.asLong();
            double known = dist.getOrDefault(curKey, INF);
            if (curLoss > known) continue;

            BlockState curState = world.getBlockState(cur.pos);
            if (!(curState.getBlock() instanceof CableBlock curCable)) continue;
            if (isSplitterDisabled(world, cur.pos, curCable)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos np = cur.pos.offset(dir);
                if (np.equals(sourcePos)) continue;

                BlockEntity nbe = world.getBlockEntity(np);
                if (nbe instanceof IEuEnergyStorage sink) {
                    Direction intoSink = dir.getOpposite();
                    if (!sink.canInsert(intoSink)) continue;

                    // Record best path to this sink (based on loss to the adjacent cable).
                    long sinkKey = np.asLong() ^ ((long) intoSink.getId() << 60);
                    Path existing = bestBySink.get(sinkKey);
                    if (existing == null || curLoss < existing.loss) {
                        List<BlockPos> cables = reconstructPath(startCablePos, cur.pos, prev);
                        int minCap = minCapacityAlong(world, cables);
                        bestBySink.put(sinkKey, new Path(np, intoSink, curLoss, cables, minCap));
                    }
                    continue;
                }

                BlockState ns = world.getBlockState(np);
                if (!ModBlocks.isCable(ns.getBlock())) continue;
                if (!(ns.getBlock() instanceof CableBlock nextCable)) continue;
                if (isSplitterDisabled(world, np, nextCable)) continue;

                double nextLoss = curLoss + nextCable.getKind().loss;
                long nkey = np.asLong();
                if (nextLoss < dist.getOrDefault(nkey, INF)) {
                    dist.put(nkey, nextLoss);
                    prev.put(nkey, curKey);
                    pq.add(new Node(np, nextLoss));
                }
            }
        }

        if (bestBySink.isEmpty()) return List.of();

        ArrayList<Path> out = new ArrayList<>(bestBySink.values());
        out.sort(Comparator.comparingDouble(Path::loss));
        return out;
    }

    private static int minCapacityAlong(World world, List<BlockPos> cables) {
        int min = Integer.MAX_VALUE;
        for (BlockPos p : cables) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                min = Math.min(min, cb.getKind().capacity);
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    // Deterministic-ish hash to choose the path offset. Not perfect, but stable.
    private static int posHashCode(BlockPos pos, Direction side) {
        int h = (int) (pos.asLong() ^ (pos.asLong() >>> 32));
        h = 31 * h + side.getId();
        return h;
    }

    private static List<BlockPos> reconstructPath(BlockPos start, BlockPos end, Map<Long, Long> prev) {
        // Reconstruct cable positions from start .. end (inclusive).
        ArrayList<BlockPos> out = new ArrayList<>();
        long cur = end.asLong();
        long startKey = start.asLong();

        while (true) {
            out.add(BlockPos.fromLong(cur));
            if (cur == startKey) break;
            Long p = prev.get(cur);
            if (p == null) break;
            cur = p;
        }

        // Reverse in-place.
        for (int i = 0, j = out.size() - 1; i < j; i++, j--) {
            BlockPos tmp = out.get(i);
            out.set(i, out.get(j));
            out.set(j, tmp);
        }

        return out;
    }
}
