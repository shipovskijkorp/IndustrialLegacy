package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.path.EnergyPath;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Dijkstra over a pre-built {@link EnergyGrid}, producing best-loss paths to reachable sinks.
 *
 * <p>Weight model matches the previous stepping-stone implementation:
 * distance(start) = loss(startCable), and traversing into a neighbor cable adds loss(neighborCable).</p>
 */
final class EnergyGridPathFinder {

    private EnergyGridPathFinder() {}

    private static final int MAX_NODES = 4096;
    private static final double INF = 1e100;

    private record QNode(long pos, double loss) {}

    public static List<EnergyPath> findPaths(World world, EnergyGrid grid, BlockPos sourcePos, BlockPos startCablePos) {
        if (world == null) return List.of();
        if (grid == null) return List.of();

        long start = startCablePos.asLong();
        if (!grid.containsCable(start)) return List.of();

        long sourceKey = sourcePos == null ? 0L : sourcePos.asLong();

        PriorityQueue<QNode> pq = new PriorityQueue<>(Comparator.comparingDouble(QNode::loss));
        Long2DoubleOpenHashMap dist = new Long2DoubleOpenHashMap();
        dist.defaultReturnValue(INF);
        Long2LongOpenHashMap prev = new Long2LongOpenHashMap();
        prev.defaultReturnValue(Long.MIN_VALUE);

        double startLoss = grid.loss(start);
        dist.put(start, startLoss);
        pq.add(new QNode(start, startLoss));

        Long2ObjectOpenHashMap<EnergyPath> bestPerSink = new Long2ObjectOpenHashMap<>();

        int visited = 0;
        BlockPos.Mutable curPos = new BlockPos.Mutable();
        BlockPos.Mutable nbPos = new BlockPos.Mutable();

        while (!pq.isEmpty() && visited++ < MAX_NODES) {
            QNode cur = pq.poll();
            long curKey = cur.pos;
            double curLoss = cur.loss;
            if (curLoss > dist.get(curKey)) continue;

            curPos.set(BlockPos.unpackLongX(curKey), BlockPos.unpackLongY(curKey), BlockPos.unpackLongZ(curKey));

            // Collect sinks adjacent to this cable.
            for (Direction dir : Direction.values()) {
                nbPos.set(curPos).move(dir);
                long nbKey = nbPos.asLong();
                if (nbKey == sourceKey) continue;

                BlockEntity be = world.getBlockEntity(nbPos);
                if (!(be instanceof IEuEnergyStorage)) continue;

                Direction intoSink = dir.getOpposite();

                LongArrayList cableKeys = reconstruct(start, curKey, prev);
                long minCap = minCapacityAlong(grid, cableKeys);
                List<BlockPos> cables = toBlockPosList(cableKeys);

                long sinkKey = mixSinkKey(nbKey, intoSink.getId());
                EnergyPath existing = bestPerSink.get(sinkKey);
                if (existing == null || curLoss < existing.loss()) {
                    bestPerSink.put(sinkKey, new EnergyPath(BlockPos.fromLong(nbKey), intoSink, curLoss, minCap, cables));
                }
            }

            // Relax neighbor cables.
            for (long nkey : grid.neighbors(curKey)) {
                double nextLoss = curLoss + grid.loss(nkey);
                if (nextLoss < dist.get(nkey)) {
                    dist.put(nkey, nextLoss);
                    prev.put(nkey, curKey);
                    pq.add(new QNode(nkey, nextLoss));
                }
            }
        }

        ArrayList<EnergyPath> out = new ArrayList<>(bestPerSink.values());
        out.sort(Comparator.comparingDouble(EnergyPath::loss));
        return out;
    }

    private static LongArrayList reconstruct(long start, long end, Long2LongOpenHashMap prev) {
        LongArrayList out = new LongArrayList();
        long cur = end;

        while (true) {
            out.add(cur);
            if (cur == start) break;
            long p = prev.get(cur);
            if (p == Long.MIN_VALUE) break;
            cur = p;
        }

        // manual reverse (fastutil-safe)
        for (int i = 0, j = out.size() - 1; i < j; i++, j--) {
            long tmp = out.getLong(i);
            out.set(i, out.getLong(j));
            out.set(j, tmp);
        }

        return out;
    }


    private static long minCapacityAlong(EnergyGrid grid, LongArrayList cableKeys) {
        long min = Long.MAX_VALUE;
        for (int i = 0; i < cableKeys.size(); i++) {
            long key = cableKeys.getLong(i);
            long cap = grid.capacity(key);
            if (cap > 0) min = Math.min(min, cap);
        }
        return min == Long.MAX_VALUE ? 0L : min;
    }

    private static List<BlockPos> toBlockPosList(LongArrayList cableKeys) {
        if (cableKeys.isEmpty()) return List.of();
        ArrayList<BlockPos> out = new ArrayList<>(cableKeys.size());
        for (int i = 0; i < cableKeys.size(); i++) {
            out.add(BlockPos.fromLong(cableKeys.getLong(i)));
        }
        return out;
    }

    private static long mixSinkKey(long posLong, int sideId) {
        return (posLong * 31L) ^ (long) sideId;
    }
}
