package com.shipovskijkorp.industriallegacy.energy.calc;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.event.EuTransferRecorder;
import com.shipovskijkorp.industriallegacy.energy.path.EnergyPath;
import com.shipovskijkorp.industriallegacy.energy.path.PathCache;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/**
 * IC2-like energy routing over cable blocks.
 *
 * <p>Intermediate implementation: uses PathCache+PathFinder (Dijkstra by loss), then performs
 * a calculator loop that spends source energy as {@code accepted + loss} (IC2 semantics),
 * with fairness via randomized path offset 3/4 ticks.</p>
 */
public final class EuEnergyCalculator {
    private EuEnergyCalculator() {}

    public static long route(World world, BlockPos sourcePos, IEuEnergyStorage source, Direction outSide, long maxAmount) {
        if (world == null) return 0;
        if (maxAmount <= 0) return 0;
        if (!source.canExtract(outSide)) return 0;

        BlockPos firstPos = sourcePos.offset(outSide);

        final int sourceTier = source.getSourceTier(outSide);
        final long voltage = EuUtil.powerFromTier(sourceTier);

        // Direct sink neighbor.
        BlockEntity directBe = world.getBlockEntity(firstPos);
        if (directBe instanceof IEuEnergyStorage directSink) {
            Direction intoSink = outSide.getOpposite();
            if (directSink.canInsert(intoSink)) {
                return moveAlongPath(world, source, outSide, directSink, intoSink, List.of(), voltage, maxAmount, 0.0);
            }
            return 0;
        }

        BlockState firstState = world.getBlockState(firstPos);
        if (!ModBlocks.isCable(firstState.getBlock())) return 0;
        if (!(firstState.getBlock() instanceof CableBlock startCable)) return 0;
        if (isSplitterDisabled(world, firstPos, startCable)) return 0;

        long budget = Math.min(maxAmount, voltage);
        long canExtractSim = source.extractEu(budget, outSide, true);
        if (canExtractSim <= 0) return 0;

        if (source.isFullEnergyOutput()) {
            long full = source.extractEu(voltage, outSide, true);
            if (full < voltage) return 0;
        }

        long offerBudget = Math.min(canExtractSim, budget);
        if (offerBudget <= 0) return 0;

        List<EnergyPath> paths = PathCache.getOrBuild(world, sourcePos, firstPos);
        if (paths.isEmpty()) return 0;

        final boolean shuffle = (world.getTime() & 3L) != 0L;
        final int startIndex = (shuffle && paths.size() > 1) ? world.random.nextInt(paths.size()) : 0;

        long spentTotal = 0L;
        long remainingBudget = offerBudget;

        for (int i = 0; i < paths.size(); i++) {
            if (remainingBudget <= 0) break;
            int idx = (startIndex + i) % paths.size();
            EnergyPath p = paths.get(idx);

            BlockEntity be = world.getBlockEntity(p.sinkPos());
            if (!(be instanceof IEuEnergyStorage sink)) continue;
            Direction intoSink = p.intoSink();
            if (!sink.canInsert(intoSink)) continue;

            int sinkTier = sink.getSinkTier(intoSink);
            if (sourceTier > sinkTier) continue;

            if (voltage > p.minCapacity()) {
                meltFirstOverloadedCable(world, p.cables(), voltage);
                continue;
            }

            double loss = applyLossRounding(p.loss());
            if (loss >= (double) remainingBudget) continue;

            long maxArrive = (long) Math.floor((double) remainingBudget - loss);
            if (maxArrive <= 0) continue;

            long demand = (long) Math.floor(sink.getDemandedEnergy(intoSink));
            if (demand <= 0) continue;

            long offerToSink = Math.min(maxArrive, demand);
            if (offerToSink <= 0) continue;

            long acceptedSim = sink.insertEu(offerToSink, intoSink, true);
            if (acceptedSim <= 0) continue;

            long maxAccepted = (long) Math.floor(Math.max(0.0, (double) remainingBudget - loss));
            if (acceptedSim > maxAccepted) {
                acceptedSim = maxAccepted;
                if (acceptedSim <= 0) continue;
            }

            long spend = (long) Math.ceil((double) acceptedSim + loss);
            spend = Math.max(1L, Math.min(spend, remainingBudget));

            long extracted = source.extractEu(spend, outSide, false);
            if (extracted <= 0) continue;

            long arrive = (long) Math.floor(Math.max(0.0, (double) extracted - loss));
            if (arrive <= 0) {
                EuTransferRecorder.record(world, p.cables(), 0L);
                spentTotal += extracted;
                remainingBudget -= extracted;
                continue;
            }

            long inserted = sink.insertEu(Math.min(arrive, acceptedSim), intoSink, false);
            EuTransferRecorder.record(world, p.cables(), inserted);

            spentTotal += extracted;
            remainingBudget -= extracted;
        }

        return spentTotal;
    }

    private static long moveAlongPath(
            World world,
            IEuEnergyStorage source,
            Direction outSide,
            IEuEnergyStorage sink,
            Direction intoSink,
            List<BlockPos> pathCables,
            long voltage,
            long maxAmount,
            double loss
    ) {
        long budget = Math.min(Math.min(maxAmount, voltage), source.extractEu(maxAmount, outSide, true));
        if (budget <= 0) return 0;

        if (source.isFullEnergyOutput()) {
            long full = source.extractEu(voltage, outSide, true);
            if (full < voltage) return 0;
        }

        loss = applyLossRounding(loss);
        if (loss >= (double) budget) return 0;

        long arriveMax = (long) Math.floor((double) budget - loss);
        if (arriveMax <= 0) return 0;

        long demand = (long) Math.floor(sink.getDemandedEnergy(intoSink));
        if (demand <= 0) return 0;

        long offer = Math.min(arriveMax, demand);
        if (offer <= 0) return 0;

        long acceptedSim = sink.insertEu(offer, intoSink, true);
        if (acceptedSim <= 0) return 0;

        long spend = (long) Math.ceil((double) acceptedSim + loss);
        spend = Math.max(1L, Math.min(spend, budget));

        long extracted = source.extractEu(spend, outSide, false);
        if (extracted <= 0) return 0;

        long arrive = (long) Math.floor(Math.max(0.0, (double) extracted - loss));
        if (arrive <= 0) {
            EuTransferRecorder.record(world, pathCables, 0L);
            return extracted;
        }

        long inserted = sink.insertEu(Math.min(arrive, acceptedSim), intoSink, false);
        EuTransferRecorder.record(world, pathCables, inserted);
        return extracted;
    }

    private static double applyLossRounding(double loss) {
        boolean roundEnetLoss = ILConfig.getBool("misc/roundEnetLoss", true);
        if (!roundEnetLoss) return loss;
        return Math.floor(loss);
    }

    private static boolean isSplitterDisabled(World world, BlockPos pos, CableBlock cable) {
        if (cable.getKind() != CableKind.SPLITTER) return false;
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof CableBlockEntity cbe)) return false;
        return !cbe.isActive();
    }

    private static void meltFirstOverloadedCable(World world, List<BlockPos> cables, long voltage) {
        if (cables == null || cables.isEmpty()) return;
        for (BlockPos p : cables) {
            BlockState st = world.getBlockState(p);
            if (!(st.getBlock() instanceof CableBlock cb)) continue;

            long breakdown = cb.getKind().capacity + 1L;
            if (voltage >= breakdown) {
                // Break the cable block (IC2-like burn). Keep simple for now.
                world.breakBlock(p, true);
                return;
            }
        }
    }
}
