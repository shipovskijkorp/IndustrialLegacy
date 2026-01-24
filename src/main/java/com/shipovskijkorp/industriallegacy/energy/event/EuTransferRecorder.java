package com.shipovskijkorp.industriallegacy.energy.event;

import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Records the last transferred EU value for cable visualizers (detector cable, debug tools).
 */
public final class EuTransferRecorder {
    private EuTransferRecorder() {}

    public static void record(World world, List<BlockPos> pathCables, long inserted) {
        if (pathCables == null || pathCables.isEmpty()) return;
        for (BlockPos p : pathCables) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof CableBlockEntity cbe) {
                cbe.setLastTransferredEu(inserted);
            }
        }
    }
}
