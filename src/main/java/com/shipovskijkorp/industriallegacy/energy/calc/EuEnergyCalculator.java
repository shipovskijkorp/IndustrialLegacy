package com.shipovskijkorp.industriallegacy.energy.calc;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.event.EuTransferRecorder;
import com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal;
import com.shipovskijkorp.industriallegacy.energy.path.EnergyPath;
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

        // Direct sink neighbor (no cables, no loss).
        BlockEntity directBe = world.getBlockEntity(firstPos);
        if (directBe instanceof IEuEnergyStorage directSink) {
            Direction intoSink = outSide.getOpposite();
            if (directSink.canInsert(intoSink)) {
                return moveEnergy(world, source, outSide, directSink, intoSink, List.of(), maxAmount, 0.0);
            }
            return 0;
        }

        // Cable graph start.
        BlockState firstState = world.getBlockState(firstPos);
        if (!ModBlocks.isCable(firstState.getBlock())) return 0;

        // Determine how much we can (and should) send this tick.
        long maxPacket = EuUtil.powerFromTier(source.getSourceTier());
        long canExtract = source.extractEu(Math.min(maxAmount, maxPacket), outSide, true);
        if (canExtract <= 0) return 0;

        // IC2 "fullEnergy": only offer energy if we can start with a full packet.
        if (source.isFullEnergyOutput() && canExtract < maxPacket) {
            return 0;
        }

        long offered = source.isFullEnergyOutput() ? maxPacket : Math.min(canExtract, maxPacket);
        if (offered <= 0) return 0;

        List<EnergyPath> paths = EnergyNetLocal.get(world).getOrComputePaths(sourcePos, firstPos);
        if (paths.isEmpty()) return 0;

        // IC2-like fairness: 3/4 ticks random path offset, else 0.
        int size = paths.size();
        int startIndex;
        boolean shuffle = (world.getTime() & 3L) != 0L;
        if (shuffle && size > 1) startIndex = world.random.nextInt(size);
        else startIndex = 0;

        long remainingBudget = offered;
        long spentTotal = 0L;

        for (int i = 0; i < size && remainingBudget > 0; i++) {
            EnergyPath path = paths.get((startIndex + i) % size);
            BlockEntity sinkBe = world.getBlockEntity(path.sinkPos());
            if (!(sinkBe instanceof IEuEnergyStorage sink)) continue;

            Direction intoSink = path.intoSink();
            if (!sink.canInsert(intoSink)) continue;

            // Tier sanity: refuse over-voltage for now (later: IC2 explode/melt rules).
            int outTier = source.getSourceTier();
            int inTier = sink.getSinkTier(intoSink);
            if (outTier > inTier) continue;

            // Overload check: if offered packet exceeds any cable capacity on this path, melt and abort.
            if (path.minCapacity() > 0 && offered > path.minCapacity()) {
                meltFirstOverloadedCable(world, path.cables(), offered);
                return 0;
            }

            double loss = applyLossRounding(path.loss());
            long spent = moveEnergy(world, source, outSide, sink, intoSink, path.cables(), remainingBudget, loss);
            if (spent <= 0) continue;

            spentTotal += spent;
            remainingBudget -= spent;
        }

        return spentTotal;
    }

    private static long moveEnergy(
            World world,
            IEuEnergyStorage source,
            Direction outSide,
            IEuEnergyStorage sink,
            Direction intoSink,
            List<BlockPos> pathCables,
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
        if (deliveredActual <= 0) {
            EuTransferRecorder.record(world, pathCables, 0L);
            return extracted;
        }

        long inserted = sink.insertEu(deliveredActual, intoSink, false);
        EuTransferRecorder.record(world, pathCables, inserted);
        return extracted;
    }

    private static double applyLossRounding(double loss) {
        // IC2 default: roundEnetLoss=true and loss is floored.
        boolean round = ILConfig.getBool("misc/roundEnetLoss", true);
        if (!round) return loss;
        return Math.floor(loss);
    }

    private static void meltFirstOverloadedCable(World world, List<BlockPos> pathCables, long packet) {
        for (BlockPos p : pathCables) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                if (packet > cb.getKind().capacity) {
                    world.breakBlock(p, false);
                    return;
                }
            }
        }
    }
}
