package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.HvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
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

/**
 * IC2-like LV Transformer.
 *
 * <p>The block facing marks the transformer special side. In step-down mode the facing side is the
 * high-voltage input. In step-up mode the facing side is the high-voltage output.</p>
 */
public class HvTransformerBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty DOT = Properties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public HvTransformerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(DOT, Direction.NORTH)
                .with(ACTIVE, false));
    }

    public static void invalidateAround(World world, BlockPos pos) {
        if (world == null || world.isClient) return;

        EuNetwork.invalidate(world, pos);
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            if (ModBlocks.isCable(world.getBlockState(neighbor).getBlock())) {
                EuNetwork.invalidate(world, neighbor);
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(DOT, ACTIVE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(DOT, ctx.getPlayerLookDirection().getOpposite())
                .with(ACTIVE, false);
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
        return new HvTransformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.HV_TRANSFORMER, HvTransformerBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);

        if (held.isOf(ModItems.WRENCH)) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(DOT, hit.getSide()), Block.NOTIFY_ALL);
                if (world.getBlockEntity(pos) instanceof HvTransformerBlockEntity transformer) {
                    transformer.onFacingChanged();
                }
                invalidateAround(world, pos);
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof HvTransformerBlockEntity transformer) {
                player.openHandledScreen(transformer);
                return ActionResult.CONSUME;
            }
        }

        return ActionResult.SUCCESS;
    }
}
