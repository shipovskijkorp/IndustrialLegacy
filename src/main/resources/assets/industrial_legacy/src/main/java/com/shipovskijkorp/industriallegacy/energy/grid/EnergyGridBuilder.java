package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Builds a cable connected component (grid) starting from a cable position.
 */
final class EnergyGridBuilder {
    private EnergyGridBuilder() {}

    static EnergyGrid build(World world, BlockPos startCablePos, java.util.function.LongConsumer cableConsumer) {
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        LongOpenHashSet visited = new LongOpenHashSet();

        long startLong = startCablePos.asLong();
        q.enqueue(startLong);
        visited.add(startLong);

        long min = startLong;

        while (!q.isEmpty()) {
            long curLong = q.dequeueLong();
            if (curLong < min) min = curLong;
            cableConsumer.accept(curLong);

            BlockPos cur = BlockPos.fromLong(curLong);
            BlockState state = world.getBlockState(cur);
            if (!(state.getBlock() instanceof CableBlock cb)) continue;
            if (EnergyGridPathFinder.isCableDisabledByRedstone(world, cur, cb)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos np = cur.offset(dir);
                BlockState ns = world.getBlockState(np);
                if (!ModBlocks.isCable(ns.getBlock())) continue;
                if (!(ns.getBlock() instanceof CableBlock ncb)) continue;
                if (EnergyGridPathFinder.isCableDisabledByRedstone(world, np, ncb)) continue;
                long nLong = np.asLong();
                if (visited.add(nLong)) {
                    q.enqueue(nLong);
                }
            }
        }

        LongSet set = new LongOpenHashSet(visited);
        return new EnergyGrid(min, set);
    }
}
