package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.IronFurnaceBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Iron Furnace: vanilla furnace logic, but faster smelting (8s instead of 10s).
 *
 * Implementation detail:
 * - speed tweak is applied via a mixin to AbstractFurnaceBlockEntity#getCookTime for IronFurnaceBlockEntity instances
 * - fuel burn time remains vanilla
 */
public final class IronFurnaceBlock extends FurnaceBlock implements BlockEntityProvider {
    public IronFurnaceBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new IronFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // server-side ticker only, like vanilla furnace
        return checkType(world, type, ModBlockEntities.IRON_FURNACE);
    }

    @Override
    protected void openScreen(World world, BlockPos pos, PlayerEntity player) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IronFurnaceBlockEntity) {
            player.openHandledScreen((NamedScreenHandlerFactory) be);
            player.incrementStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
}
