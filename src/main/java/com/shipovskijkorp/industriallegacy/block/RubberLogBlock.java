package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class RubberLogBlock extends PillarBlock {
    public static final BooleanProperty RESIN = BooleanProperty.of("resin");

    public RubberLogBlock(Settings settings) {
        super(settings.ticksRandomly());
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(AXIS, Direction.Axis.Y)
                .with(RESIN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS, RESIN);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.randomTick(state, world, pos, random);
        // маленький шанс “вырастить” смолу
        if (!state.get(RESIN) && random.nextFloat() < 0.02f) {
            world.setBlockState(pos, state.with(RESIN, true), 2);
        }
    }
}
