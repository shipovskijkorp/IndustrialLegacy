package com.shipovskijkorp.industriallegacy.energy.calc;

import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.event.EuTransferRecorder;
import com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal;
import com.shipovskijkorp.industriallegacy.energy.grid.RoutePath;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/**
 * IC2-inspired energy routing: one emission call can feed multiple sinks on the same cable network,
 * accounting for per-cable loss (spent = accepted + loss).
 *
 * <p>This is still a stepping stone until full EnergyNetLocal caching is implemented.</p>
 */
public final class EuEnergyCalculator {
    private EuEnergyCalculator() {}

    /** @return amount spent (extracted) from the source (EU) */
    public static long route(World world, BlockPos sourcePos, IEuEnergyStorage source, Direction outSide, long maxAmount) {
        if (world == null) return 0;
        if (maxAmount <= 0) return 0;
        if (!source.canExtract(outSide)) return 0;

        BlockPos firstPos = sourcePos.offset(outSide);

        final int sourceTier = source.getSourceTier(outSide);
        final long maxPacket = EuUtil.powerFromTier(sourceTier);

        // Direct sink neighbor (no cables, no loss).
        BlockEntity directBe = world.getBlockEntity(firstPos);
        if (directBe instanceof IEuEnergyStorage directSink) {
            Direction intoSink = outSide.getOpposite();
            if (!directSink.canInsert(intoSink)) return 0;

            // IC2: direct neighbor can still over-volt.
            long sinkMaxPacket = EuUtil.powerFromTier(directSink.getSinkTier(intoSink));
            if (maxPacket > sinkMaxPacket) {
                // IC2: even direct neighbors can be over-volted; spend (at most) one packet.
                long want = source.isFullEnergyOutput() ? maxPacket : Math.min(maxAmount, maxPacket);
                if (source.isFullEnergyOutput() && want < maxPacket) return 0;

                long canSim = source.extractEu(want, outSide, true);
                if (source.isFullEnergyOutput() && canSim < maxPacket) return 0;
                if (canSim <= 0) return 0;

                EnergyNetLocal.get(world).scheduleSinkExplosion(firstPos, (double) maxPacket);
                long extracted = source.extractEu(want, outSide, false);
                return Math.max(0L, extracted);
            }

            return moveEnergyDirect(world, source, outSide, directSink, intoSink, maxAmount, 0.0);
        }

        // Cable graph start.
        BlockState firstState = world.getBlockState(firstPos);
        if (!ModBlocks.isCable(firstState.getBlock())) return 0;

        // Total budget we are willing to try to extract this call.
        long canExtractTotal = source.extractEu(maxAmount, outSide, true);
        if (canExtractTotal <= 0) return 0;

        if (source.isFullEnergyOutput() && canExtractTotal < maxPacket) {
            return 0;
        }

        EnergyNetLocal net = EnergyNetLocal.get(world);
        List<RoutePath> paths = net.getOrComputeRoutes(world, sourcePos, firstPos);
        if (paths.isEmpty()) return 0;

        int pathCount = paths.size();
        int startIndex = ((world.getTime() & 3L) != 0L && pathCount > 1) ? world.random.nextInt(pathCount) : 0;

        long spentTotal = 0L;

        int maxPackets = source.sendMultipleEnergyPackets() ? Math.max(1, source.getMaxEnergyPacketCount()) : 1;
        for (int packetIndex = 0; packetIndex < maxPackets; packetIndex++) {
            long remainingTotal = canExtractTotal - spentTotal;
            if (remainingTotal <= 0) break;

            // Packet budget is limited by tier.
            long packetBudget = Math.min(remainingTotal, maxPacket);
            if (source.isFullEnergyOutput() && packetBudget < maxPacket) break;
            if (source.isFullEnergyOutput()) packetBudget = maxPacket;

            long remainingPacketBudget = packetBudget;

            for (int i = 0; i < pathCount && remainingPacketBudget > 0; i++) {
                RoutePath path = paths.get((startIndex + i) % pathCount);
                BlockEntity sinkBe = world.getBlockEntity(path.sinkPos());
                if (!(sinkBe instanceof IEuEnergyStorage sink)) continue;

                Direction intoSink = path.intoSink();
                if (!sink.canInsert(intoSink)) continue;

                // IC2: over-voltage is not ignored; it causes explosions/meltdown.
                int sinkTier = sink.getSinkTier(intoSink);
                long sinkMaxPacket = EuUtil.powerFromTier(sinkTier);
                if (packetBudget > sinkMaxPacket) {
                    // Spend the packet (source emission) and schedule sink explosion.
                    long extracted = source.extractEu(packetBudget, outSide, false);
                    spentTotal += extracted;
                    net.recordPathTransfer(world, path, 0.0, (double) packetBudget);
                    net.scheduleSinkExplosion(path.sinkPos(), (double) packetBudget);
                    remainingPacketBudget = 0;
                    break;
                }

                double loss = applyLossRounding(path.loss());
                long spent = moveEnergy(world, net, path, source, outSide, sink, intoSink, remainingPacketBudget, loss, packetBudget);
                if (spent <= 0) continue;

                spentTotal += spent;
                remainingPacketBudget -= spent;

                if (spentTotal >= canExtractTotal) break;
            }
        }

        return spentTotal;
    }

    private static long moveEnergy(
            World world,
            EnergyNetLocal net,
            RoutePath path,
            IEuEnergyStorage source,
            Direction outSide,
            IEuEnergyStorage sink,
            Direction intoSink,
            long budget,
            double loss,
            long packetConducted
    ) {
        if (budget <= 0) return 0;
        if (!source.canExtract(outSide)) return 0;
        if (!sink.canInsert(intoSink)) return 0;

        double deliveredMaxD = (double) budget - loss;
        if (deliveredMaxD <= 0.0) return 0;
        long deliveredMax = (long) Math.floor(deliveredMaxD);
        if (deliveredMax <= 0) return 0;

        double demandedD = sink.getDemandedEnergy(intoSink);
        long demanded = demandedD <= 0.0 ? 0L : (long) Math.floor(demandedD);
        if (demanded <= 0) return 0;

        long offer = Math.min(deliveredMax, demanded);

        long acceptedSim = sink.insertEu(offer, intoSink, true);
        if (acceptedSim <= 0) return 0;

        long wantSpent = (long) Math.ceil((double) acceptedSim + loss);
        if (wantSpent <= 0) return 0;
        if (wantSpent > budget) wantSpent = budget;

        long extracted = source.extractEu(wantSpent, outSide, false);
        if (extracted <= 0) return 0;

        long deliveredActual = (long) Math.floor(Math.max(0.0, (double) extracted - loss));
        deliveredActual = Math.min(deliveredActual, acceptedSim);
        if (deliveredActual <= 0) {
            EuTransferRecorder.record(world, path.cables(), 0L);
            if (net != null) {
                net.recordPathTransfer(world, path, 0.0, (double) packetConducted);
            }
            return extracted;
        }

        long inserted = sink.insertEu(deliveredActual, intoSink, false);
        EuTransferRecorder.record(world, path.cables(), inserted);
        if (net != null) {
            net.recordPathTransfer(world, path, (double) inserted, (double) packetConducted);
        }
        return extracted;
    }

    private static long moveEnergyDirect(
            World world,
            IEuEnergyStorage source,
            Direction outSide,
            IEuEnergyStorage sink,
            Direction intoSink,
            long budget,
            double loss
    ) {
        if (budget <= 0) return 0;
        if (!source.canExtract(outSide)) return 0;
        if (!sink.canInsert(intoSink)) return 0;

        double deliveredMaxD = (double) budget - loss;
        if (deliveredMaxD <= 0.0) return 0;
        long deliveredMax = (long) Math.floor(deliveredMaxD);
        if (deliveredMax <= 0) return 0;

        double demandedD = sink.getDemandedEnergy(intoSink);
        long demanded = demandedD <= 0.0 ? 0L : (long) Math.floor(demandedD);
        if (demanded <= 0) return 0;

        long offer = Math.min(deliveredMax, demanded);
        long acceptedSim = sink.insertEu(offer, intoSink, true);
        if (acceptedSim <= 0) return 0;

        long wantSpent = (long) Math.ceil((double) acceptedSim + loss);
        if (wantSpent <= 0) return 0;
        if (wantSpent > budget) wantSpent = budget;

        long extracted = source.extractEu(wantSpent, outSide, false);
        if (extracted <= 0) return 0;

        long deliveredActual = (long) Math.floor(Math.max(0.0, (double) extracted - loss));
        deliveredActual = Math.min(deliveredActual, acceptedSim);
        if (deliveredActual <= 0) return extracted;

        sink.insertEu(deliveredActual, intoSink, false);
        return extracted;
    }

    private static double applyLossRounding(double loss) {
        // IC2 default: roundEnetLoss=true and loss is floored.
        boolean round = ILConfig.getBool("misc/roundEnetLoss", true);
        if (!round) return loss;
        return Math.floor(loss);
    }

    // Cable overload is handled by EnergyNetLocal end-of-tick effects (IC2-style).
}
