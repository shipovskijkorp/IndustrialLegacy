package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.NuclearReactorBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ReactorChamberBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public ReactorChamberBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        validate(world, pos);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        validate(world, pos);
    }

    private void validate(World world, BlockPos pos) {
        if (world.isClient) return;
        int count = 0;
        Direction face = Direction.NORTH;
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof NuclearReactorBlockEntity) {
                count++;
                face = dir;
            }
        }
        if (count != 1) {
            world.breakBlock(pos, true);
        } else {
            world.setBlockState(pos, world.getBlockState(pos).with(FACING, face), 3);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof NuclearReactorBlockEntity reactor) {
                if (!world.isClient) {
                    player.openHandledScreen(reactor);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }
}
