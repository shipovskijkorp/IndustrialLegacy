package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Rubber wood log with IC2 resin hole states + regeneration.
 *
 * Mechanics source of truth: IC2 1.12.2 BlockRubWood.
 */
public class RubberWoodBlock extends Block {
    public static final EnumProperty<RubberWoodState> STATE = EnumProperty.of("state", RubberWoodState.class);

    public RubberWoodBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(STATE, RubberWoodState.plain_y));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction.Axis axis = ctx.getSide().getAxis();
        return getDefaultState().with(STATE, RubberWoodState.plainForAxis(axis));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        RubberWoodState s = state.get(STATE);
        if (s.isPlain()) {
            Direction.Axis axis = s.axis;
            if (rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90) {
                if (axis == Direction.Axis.X) axis = Direction.Axis.Z;
                else if (axis == Direction.Axis.Z) axis = Direction.Axis.X;
            }
            return state.with(STATE, RubberWoodState.plainForAxis(axis));
        } else {
            Direction rotated = rotation.rotate(s.facing);
            return state.with(STATE, RubberWoodState.withFacing(s.wet, rotated));
        }
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        RubberWoodState s = state.get(STATE);
        if (s.isPlain()) return super.mirror(state, mirror);
        Direction mirrored = mirror.apply(s.facing);
        return state.with(STATE, RubberWoodState.withFacing(s.wet, mirrored));
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        // IC2: 1/7 chance to regenerate dry -> wet
        if (random.nextInt(7) == 0) {
            RubberWoodState s = state.get(STATE);
            if (!s.canRegenerate()) return;
            world.setBlockState(pos, state.with(STATE, s.getWet()), Block.NOTIFY_ALL);
        }
    }

    @Override
    public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.onStacksDropped(state, world, pos, tool, dropExperience);

        // IC2: when breaking a non-plain rubber wood, extra resin drops with 1/6 chance.
        RubberWoodState s = state.get(STATE);
        if (!s.isPlain() && world.random.nextInt(6) == 0) {
            Block.dropStack(world, pos, new ItemStack(ModItems.STICKY_RESIN));
        }
    }
}
