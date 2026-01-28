package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.BatBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.player.PlayerEntity;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;


/**
 * Simple BatBox storage block.
 *
 * Output side = {@link #FACING}.
 */
public class BatBoxBlock extends BlockWithEntity implements BlockEntityProvider {
    // IC2 electric storage blocks are horizontal-facing machines.
    // The "output dot" is on the front face.
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BatBoxBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }
    private static void invalidateAdjacentCables(World world, BlockPos pos) {
        if (world == null || world.isClient) return;

        for (Direction d : Direction.values()) {
            BlockPos p = pos.offset(d);
            if (ModBlocks.isCable(world.getBlockState(p).getBlock())) {
                // targeted: сбрасывает только grid рядом с машиной
                EuNetwork.invalidate(world, p);
            }
        }
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // IC2: when placed, the front/output face points towards the player.
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        invalidateAdjacentCables(world, pos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!world.isClient && state.getBlock() != newState.getBlock()) {
            invalidateAdjacentCables(world, pos);
        }
    }


    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // BlockWithEntity defaults to INVISIBLE in modern MC; IC2 machines must render their block model.
        return BlockRenderType.MODEL;
    }

    // IC2 storage blocks can emit a redstone signal depending on the selected redstone mode.
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BatBoxBlockEntity bat) {
            return bat.getRedstoneOutputLevel();
        }
        return 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }


    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BatBoxBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.BATBOX, BatBoxBlockEntity::tick);
    }
@Override
public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
    if (world.isClient) return ActionResult.SUCCESS;

    BlockEntity be = world.getBlockEntity(pos);
    if (be instanceof BatBoxBlockEntity bat) {
        player.openHandledScreen(bat);
        return ActionResult.CONSUME;
    }
    return ActionResult.PASS;
}

}
