package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/** IL-style 1/8-block sheets: resin/latex, rubber and wool. */
public class SheetBlock extends Block {
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    private static final Direction[] SUPPORT_AXES = {Direction.EAST, Direction.SOUTH};

    private final SheetType type;

    public SheetBlock(Settings settings, SheetType type) {
        super(settings);
        this.type = type;
    }

    public SheetType getSheetType() {
        return type;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return type == SheetType.RESIN ? VoxelShapes.empty() : SHAPE;
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return switch (type) {
            case RESIN -> hasSolidTop(world, pos.down());
            case RUBBER -> hasSolidTop(world, pos.down()) || hasRubberOrSolidSide(world, pos);
            case WOOL -> true;
        };
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        switch (type) {
            case RESIN -> {
                entity.fallDistance *= 0.75f;
                entity.setVelocity(entity.getVelocity().multiply(0.6D, 0.85D, 0.6D));
            }
            case RUBBER -> {
                if (hasSolidTop(world, pos.down())) {
                    return;
                }
                if (entity instanceof LivingEntity && !canSupportWeight(world, pos)) {
                    if (!world.isClient) {
                        world.breakBlock(pos, true);
                    }
                    return;
                }
                if (entity.getVelocity().y <= -0.4D) {
                    entity.fallDistance = 0.0f;
                    double bounce = -0.8D;
                    if (entity instanceof PlayerEntity player && player.isSneaking()) {
                        bounce = -0.1D;
                    }
                    entity.setVelocity(entity.getVelocity().multiply(1.1D, bounce, 1.1D));
                }
            }
            case WOOL -> entity.fallDistance *= 0.95f;
        }
    }

    private static boolean hasRubberOrSolidSide(WorldView world, BlockPos pos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighbor = world.getBlockState(neighborPos);
            if (neighbor.isOf(ModBlocks.RUBBER_SHEET) || isFullCube(world, neighborPos, neighbor)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSolidTop(WorldView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    private static boolean isFullCube(WorldView world, BlockPos pos, BlockState state) {
        return state.isSideSolidFullSquare(world, pos, Direction.UP)
                && state.isSideSolidFullSquare(world, pos, Direction.NORTH)
                && state.isSideSolidFullSquare(world, pos, Direction.SOUTH)
                && state.isSideSolidFullSquare(world, pos, Direction.WEST)
                && state.isSideSolidFullSquare(world, pos, Direction.EAST)
                && state.isSideSolidFullSquare(world, pos, Direction.DOWN);
    }

    private static boolean canSupportWeight(WorldView world, BlockPos pos) {
        for (Direction axis : SUPPORT_AXES) {
            boolean negativeSupported = isSupportedInDirection(world, pos, axis.getOpposite());
            boolean positiveSupported = isSupportedInDirection(world, pos, axis);
            if (negativeSupported && positiveSupported) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSupportedInDirection(WorldView world, BlockPos start, Direction direction) {
        BlockPos.Mutable mutable = start.mutableCopy();
        for (int i = 0; i < 16; i++) {
            mutable.move(direction);
            BlockState state = world.getBlockState(mutable);
            if (isFullCube(world, mutable, state) || hasSolidTop(world, mutable.down())) {
                return true;
            }
            if (!state.isOf(ModBlocks.RUBBER_SHEET)) {
                return false;
            }
        }
        return false;
    }

    public enum SheetType {
        RESIN,
        RUBBER,
        WOOL
    }
}
