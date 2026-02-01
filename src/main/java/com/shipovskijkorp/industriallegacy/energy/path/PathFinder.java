package com.shipovskijkorp.industriallegacy.energy.path;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

/**
 * Dijkstra path-finder over cable blocks, collecting best-loss paths to reachable sinks.
 *
 * <p>This is a stepping stone towards IL's EnergyNetLocal grid caching.</p>
 */
public final class PathFinder {
    private PathFinder() {}

    private static final int MAX_NODES = 4096;
    private static final double INF = 1e100;

    private record Node(BlockPos pos, double loss) {}

    public static List<EnergyPath> findPaths(World world, BlockPos sourcePos, BlockPos startCablePos) {
        if (world == null) return List.of();

        BlockState startState = world.getBlockState(startCablePos);
        if (!(startState.getBlock() instanceof CableBlock startCable)) return List.of();
        if (isSplitterDisabled(world, startCablePos, startCable)) return List.of();

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(Node::loss));
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();

        double startLoss = startCable.getKind().loss;
        dist.put(startCablePos.asLong(), startLoss);
        pq.add(new Node(startCablePos, startLoss));

        // best path per sink-pos + into-side
        Map<Long, EnergyPath> bestPerSink = new HashMap<>();

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
                if (nbe instanceof IEuEnergyStorage) {
                    Direction intoSink = dir.getOpposite();
                    List<BlockPos> cables = reconstructPath(startCablePos, cur.pos, prev);
                    long minCap = minCapacityAlong(world, cables);

                    long sinkKey = mixSinkKey(np.asLong(), intoSink.getId());
                    EnergyPath existing = bestPerSink.get(sinkKey);
                    if (existing == null || curLoss < existing.loss()) {
                        bestPerSink.put(sinkKey, new EnergyPath(np, intoSink, curLoss, minCap, cables));
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

        ArrayList<EnergyPath> out = new ArrayList<>(bestPerSink.values());
        out.sort(Comparator.comparingDouble(EnergyPath::loss));
        return out;
    }

    private static boolean isSplitterDisabled(World world, BlockPos pos, CableBlock cable) {
        return cable.getKind() == CableKind.SPLITTER && world.isReceivingRedstonePower(pos);
    }

    private static List<BlockPos> reconstructPath(BlockPos start, BlockPos end, Map<Long, Long> prev) {
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

    private static long minCapacityAlong(World world, List<BlockPos> cables) {
        long min = Long.MAX_VALUE;
        for (BlockPos p : cables) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                min = Math.min(min, cb.getKind().capacity);
            }
        }
        return min == Long.MAX_VALUE ? 0L : min;
    }

    private static long mixSinkKey(long posLong, int sideId) {
        return (posLong * 31L) ^ (long) sideId;
    }
}
