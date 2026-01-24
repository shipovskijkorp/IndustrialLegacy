package com.shipovskijkorp.industriallegacy.energy.event;

import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Records last transferred EU on cable block entities.
 */
public final class EuTransferRecorder {
    private EuTransferRecorder() {}

    public static void record(World world, List<BlockPos> pathCables, long transferred) {
        if (world == null) return;
        if (pathCables == null || pathCables.isEmpty()) return;
        for (BlockPos p : pathCables) {
            BlockEntity be = world.getBlockEntity(p);
            if (be instanceof CableBlockEntity cableBe) {
                cableBe.setLastTransferredEu(transferred);
            }
        }
    }
}
