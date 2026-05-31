package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.MfsuBlockEntity;
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
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.item.EnergyMachineBlockItem;
import net.minecraft.entity.player.PlayerEntity;

/**
 * MFSU (МФСУ) storage block (IL Exp semantics).
 *
 * Output side = {@link #FACING}.
 */
public class MfsuBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.FACING;

    public MfsuBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    private static void invalidateAdjacentCables(World world, BlockPos pos) {
        if (world == null || world.isClient) return;

        for (Direction d : Direction.values()) {
            BlockPos p = pos.offset(d);
            if (ModBlocks.isCable(world.getBlockState(p).getBlock())) {
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
        return getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && world.getBlockEntity(pos) instanceof MfsuBlockEntity storage && itemStack.getItem() instanceof EnergyMachineBlockItem energyItem) {
            storage.setStoredEnergyFromItem(energyItem.getStoredEnergy(itemStack));
        }
        invalidateAdjacentCables(world, pos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
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
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MfsuBlockEntity mfsu) {
            return mfsu.getRedstoneOutputLevel();
        }
        return 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }


    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity be = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (be != null) {
            return List.of(EnergyMachineBlockItem.createEnergyStack(state, be, true));
        }
        return super.getDroppedStacks(state, builder);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null) {
            return EnergyMachineBlockItem.createEnergyStack(state, be, false);
        }
        return super.getPickStack(world, pos, state);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MfsuBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.MFSU, MfsuBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof MfsuBlockEntity mfsu) {
            player.openHandledScreen(mfsu);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }
}
