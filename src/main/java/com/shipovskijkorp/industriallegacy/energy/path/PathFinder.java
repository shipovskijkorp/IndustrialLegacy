package com.shipovskijkorp.industriallegacy.energy.path;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
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
 * Builds (loss-minimizing) paths from the first cable node to all reachable sinks.
 *
 * <p>Intermediate implementation: Dijkstra by total conduction loss.</p>
 */
public final class PathFinder {
    private PathFinder() {}

    private static final int MAX_NODES = 4096;
    private static final double INF = 1.0e30;

    private record Node(BlockPos pos, double loss) {}

    public static List<EnergyPath> buildPaths(World world, BlockPos sourcePos, BlockPos startCablePos) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(Node::loss));
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();

        BlockState startState = world.getBlockState(startCablePos);
        if (!(startState.getBlock() instanceof CableBlock startCable)) return List.of();
        if (isSplitterDisabled(world, startCablePos, startCable)) return List.of();

        double startLoss = startCable.getKind().loss;
        dist.put(startCablePos.asLong(), startLoss);
        pq.add(new Node(startCablePos, startLoss));

        Map<Long, EnergyPath> bestBySink = new HashMap<>();

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

                    long sinkKey = np.asLong() ^ ((long) intoSink.getId() << 60);
                    EnergyPath existing = bestBySink.get(sinkKey);
                    if (existing == null || curLoss < existing.loss()) {
                        List<BlockPos> cables = reconstructPath(prev, startCablePos, cur.pos);
                        long minCap = minCapacityAlongPath(world, cables);
                        bestBySink.put(sinkKey, new EnergyPath(np, intoSink, curLoss, minCap, cables));
                    }
                    continue;
                }

                BlockState ns = world.getBlockState(np);
                if (!ModBlocks.isCable(ns.getBlock())) continue;
                if (!(ns.getBlock() instanceof CableBlock nextCable)) continue;
                if (isSplitterDisabled(world, np, nextCable)) continue;

                double nextLoss = curLoss + nextCable.getKind().loss;
                long nk = np.asLong();
                double best = dist.getOrDefault(nk, INF);
                if (nextLoss < best) {
                    dist.put(nk, nextLoss);
                    prev.put(nk, curKey);
                    pq.add(new Node(np, nextLoss));
                }
            }
        }

        ArrayList<EnergyPath> out = new ArrayList<>(bestBySink.values());
        out.sort(Comparator.comparingDouble(EnergyPath::loss));
        return out;
    }

    private static List<BlockPos> reconstructPath(Map<Long, Long> prev, BlockPos start, BlockPos end) {
        ArrayList<BlockPos> rev = new ArrayList<>();
        long cur = end.asLong();
        long startKey = start.asLong();
        while (true) {
            rev.add(BlockPos.fromLong(cur));
            if (cur == startKey) break;
            Long p = prev.get(cur);
            if (p == null) break;
            cur = p;
        }
        ArrayList<BlockPos> path = new ArrayList<>(rev.size());
        for (int i = rev.size() - 1; i >= 0; i--) path.add(rev.get(i));
        return path;
    }

    private static long minCapacityAlongPath(World world, List<BlockPos> cables) {
        long min = Long.MAX_VALUE;
        for (BlockPos p : cables) {
            BlockState st = world.getBlockState(p);
            if (st.getBlock() instanceof CableBlock cb) {
                min = Math.min(min, cb.getKind().capacity);
            }
        }
        return min;
    }

    private static boolean isSplitterDisabled(World world, BlockPos pos, CableBlock cable) {
        if (cable.getKind() != CableKind.SPLITTER) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof CableBlockEntity cbe)) return false;
        // Splitter disconnects from the net when powered by redstone (IC2 behavior).
        return !cbe.isActive();
    }
}
