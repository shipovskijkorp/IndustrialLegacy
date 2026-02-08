package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.LvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.player.PlayerEntity;

/**
 * LV Transformer (НН): LV <-> MV.
 *
 * DOT side = high side (MV).
 * Other sides = low side (LV).
 */
public class LvTransformerBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty DOT = Properties.FACING;

    public LvTransformerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(DOT, Direction.NORTH));
    }

    private static void invalidateAround(World world, BlockPos pos) {
        if (world == null || world.isClient) return;
        EuNetwork.invalidate(world, pos);
        for (Direction d : Direction.values()) {
            BlockPos p = pos.offset(d);
            if (ModBlocks.isCable(world.getBlockState(p).getBlock())) {
                EuNetwork.invalidate(world, p);
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DOT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Make DOT point to the face the player clicked (IC2-feel: dot is a "specific side")
        Direction dot = ctx.getSide();
        return getDefaultState().with(DOT, dot);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        invalidateAround(world, pos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!world.isClient && state.getBlock() != newState.getBlock()) {
            invalidateAround(world, pos);
        }
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(DOT, rotation.rotate(state.get(DOT)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(DOT)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LvTransformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.LV_TRANSFORMER, LvTransformerBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);

        // DOT-set by wrench (у тебя пока есть DebugWrench — используем его)
        if (held.isOf(ModItems.DEBUG_WRENCH)) {
            if (!world.isClient) {
                Direction newDot = hit.getSide();
                world.setBlockState(pos, state.with(DOT, newDot), Block.NOTIFY_ALL);
                invalidateAround(world, pos);
            }
            return ActionResult.SUCCESS;
        }

        // GUI (интерфейс)
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof LvTransformerBlockEntity tr) {
                player.openHandledScreen(tr);
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.SUCCESS;
    }
}
