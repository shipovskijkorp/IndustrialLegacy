package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
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
 *
 * IL-accurate loss model:
 * - Each node has an "inner loss": cables use CableKind.loss, endpoints (source/sink) use 0.002
 * - Each edge/link loss = average(innerLossA, innerLossB)
 * - Path loss = sum(link losses)
 */
final class EnergyGridPathFinder {
    private EnergyGridPathFinder() {}

    private static final int MAX_NODES = 4096;
    private static final double INF = 1e100;

    // IL: sources/sinks inner loss = 0.002
    private static final double ENDPOINT_INNER_LOSS = 0.002;

    private static double dynamicInnerLoss(World world, BlockPos pos, CableBlock cable) {
        double base = cable.getKind().loss;
        if (cable.getKind() == com.shipovskijkorp.industriallegacy.item.CableKind.COPPER && cable.getInsulation() == 0) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cbe) {
                base *= CableBlockEntity.oxidationLossMultiplier(cbe.getOxidationLevel());
            }
        }
        return base;
    }

    /**
     * @param pos current cable position
     * @param loss accumulated path loss up to this cable (IL link-loss model)
     * @param innerLoss inner loss of THIS node (for cables = cableKind.loss)
     */
    private record Node(BlockPos pos, double loss, double innerLoss) {}

    static boolean isCableDisabledByRedstone(World world, BlockPos pos, CableBlock cable) {
        // Matches current behavior: splitter disabled when powered.
        return cable.getKind() == com.shipovskijkorp.industriallegacy.item.CableKind.SPLITTER
                && world.isReceivingRedstonePower(pos);
    }

    static List<RoutePath> findRoutes(World world, BlockPos sourcePos, BlockPos startCablePos) {
        if (world == null) return List.of();

        BlockState startState = world.getBlockState(startCablePos);
        if (!(startState.getBlock() instanceof CableBlock startCable)) return List.of();
        if (isCableDisabledByRedstone(world, startCablePos, startCable)) return List.of();

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(Node::loss));
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();

        // IL: first link is (source endpoint innerLoss + startCable innerLoss)/2
        double startInnerLoss = dynamicInnerLoss(world, startCablePos, startCable);
        double startLoss = (ENDPOINT_INNER_LOSS + startInnerLoss) / 2.0;

        dist.put(startCablePos.asLong(), startLoss);
        pq.add(new Node(startCablePos, startLoss, startInnerLoss));

        // best path per sink-pos + into-side
        Map<Long, RoutePath> bestPerSink = new HashMap<>();

        int visited = 0;
        while (!pq.isEmpty() && visited++ < MAX_NODES) {
            Node cur = pq.poll();
            double curLoss = cur.loss;
            double curInnerLoss = cur.innerLoss;
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
                    // IL: last link is (lastCable innerLoss + sink endpoint innerLoss)/2
                    double endLinkLoss = (curInnerLoss + ENDPOINT_INNER_LOSS) / 2.0;
                    double totalLossToSink = curLoss + endLinkLoss;

                    Direction intoSink = dir.getOpposite();
                    List<BlockPos> cables = reconstruct(startCablePos, cur.pos, prev);
                    RoutePath path = buildPath(world, np, intoSink, totalLossToSink, cables);

                    long sinkKey = mixSinkKey(np.asLong(), intoSink.getId());
                    RoutePath existing = bestPerSink.get(sinkKey);
                    if (existing == null || totalLossToSink < existing.loss()) {
                        bestPerSink.put(sinkKey, path);
                    }
                    continue;
                }

                BlockState ns = world.getBlockState(np);
                if (!ModBlocks.isCable(ns.getBlock())) continue;
                if (!(ns.getBlock() instanceof CableBlock nextCable)) continue;
                if (isCableDisabledByRedstone(world, np, nextCable)) continue;
                if (!CableBlock.canCablesInteract(world, cur.pos, dir)) continue;

                // IL: link loss between two cables is average(innerLossA, innerLossB)
                double nextInnerLoss = dynamicInnerLoss(world, np, nextCable);
                double linkLoss = (curInnerLoss + nextInnerLoss) / 2.0;
                double nextLoss = curLoss + linkLoss;

                long nkey = np.asLong();
                if (nextLoss < dist.getOrDefault(nkey, INF)) {
                    dist.put(nkey, nextLoss);
                    prev.put(nkey, curKey);
                    pq.add(new Node(np, nextLoss, nextInnerLoss));
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
