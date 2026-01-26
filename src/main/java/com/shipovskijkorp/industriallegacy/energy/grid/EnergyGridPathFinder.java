package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * Dijkstra over cables, producing best-loss paths to reachable sinks.
 */
final class EnergyGridPathFinder {
    private EnergyGridPathFinder() {}

    private static final int MAX_NODES = 4096;
    private static final double INF = 1e100;

    private record Node(BlockPos pos, double loss) {}

    static boolean isCableDisabledByRedstone(World world, BlockPos pos, CableBlock cable) {
        // Matches current behavior: splitter disabled when powered.
        return cable.getKind() == com.shipovskijkorp.industriallegacy.item.CableKind.SPLITTER && world.isReceivingRedstonePower(pos);
    }

    static List<RoutePath> findRoutes(World world, BlockPos sourcePos, BlockPos startCablePos) {
        if (world == null) return List.of();

        BlockState startState = world.getBlockState(startCablePos);
        if (!(startState.getBlock() instanceof CableBlock startCable)) return List.of();
        if (isCableDisabledByRedstone(world, startCablePos, startCable)) return List.of();

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(Node::loss));
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();

        double startLoss = startCable.getKind().loss;
        dist.put(startCablePos.asLong(), startLoss);
        pq.add(new Node(startCablePos, startLoss));

        // best path per sink-pos + into-side
        Map<Long, RoutePath> bestPerSink = new HashMap<>();

        int visited = 0;
        while (!pq.isEmpty() && visited++ < MAX_NODES) {
            Node cur = pq.poll();
            double curLoss = cur.loss;
            long curKey = cur.pos.asLong();

            double known = dist.getOrDefault(curKey, INF);
            if (curLoss > known) continue;

            BlockState curState = world.getBlockState(cur.pos);
            if (!(curState.getBlock() instanceof CableBlock curCable)) continue;
            if (isCableDisabledByRedstone(world, cur.pos, curCable)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos np = cur.pos.offset(dir);
                if (np.equals(sourcePos)) continue;

                BlockEntity nbe = world.getBlockEntity(np);
                if (nbe instanceof IEuEnergyStorage) {
                    Direction intoSink = dir.getOpposite();
                    List<BlockPos> cables = reconstruct(startCablePos, cur.pos, prev);
                    RoutePath path = buildPath(world, np, intoSink, curLoss, cables);
                    long sinkKey = mixSinkKey(np.asLong(), intoSink.getId());
                    RoutePath existing = bestPerSink.get(sinkKey);
                    if (existing == null || curLoss < existing.loss()) {
                        bestPerSink.put(sinkKey, path);
                    }
                    continue;
                }

                BlockState ns = world.getBlockState(np);
                if (!ModBlocks.isCable(ns.getBlock())) continue;
                if (!(ns.getBlock() instanceof CableBlock nextCable)) continue;
                if (isCableDisabledByRedstone(world, np, nextCable)) continue;

                double nextLoss = curLoss + nextCable.getKind().loss;
                long nkey = np.asLong();
                if (nextLoss < dist.getOrDefault(nkey, INF)) {
                    dist.put(nkey, nextLoss);
                    prev.put(nkey, curKey);
                    pq.add(new Node(np, nextLoss));
                }
            }
        }

        ArrayList<RoutePath> out = new ArrayList<>(bestPerSink.values());
        out.sort(Comparator.comparingDouble(RoutePath::loss));
        return out;
    }

    private static RoutePath buildPath(World world, BlockPos sinkPos, Direction intoSink, double loss, List<BlockPos> cables) {
        double minConductor = Double.POSITIVE_INFINITY;
        double minInsulationBreak = Double.POSITIVE_INFINITY;
        double minAbsorb = Double.POSITIVE_INFINITY;

        for (BlockPos p : cables) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                minConductor = Math.min(minConductor, cb.getKind().getConductorBreakdownEnergy());
                minInsulationBreak = Math.min(minInsulationBreak, cb.getKind().getInsulationBreakdownEnergy());
                minAbsorb = Math.min(minAbsorb, cb.getKind().getInsulationEnergyAbsorption(cb.getInsulation()));
            }
        }
        if (!Double.isFinite(minConductor)) minConductor = Double.POSITIVE_INFINITY;
        if (!Double.isFinite(minInsulationBreak)) minInsulationBreak = Double.POSITIVE_INFINITY;
        if (!Double.isFinite(minAbsorb)) minAbsorb = Double.POSITIVE_INFINITY;

        return new RoutePath(sinkPos, intoSink, loss, cables, minConductor, minInsulationBreak, minAbsorb);
    }

    private static List<BlockPos> reconstruct(BlockPos start, BlockPos end, Map<Long, Long> prev) {
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

        Collections.reverse(out);
        return out;
    }

    private static long mixSinkKey(long posLong, int sideId) {
        return (posLong * 31L) ^ (long) sideId;
    }
}
