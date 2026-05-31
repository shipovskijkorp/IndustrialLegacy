package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.LuminatorBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * IL-style luminator.
 *
 * <p>Wall/ceiling/floor mounted 1-pixel-thick light that:</p>
 * <ul>
 *   <li>attaches to any solid face or adjacent EU emitter face</li>
 *   <li>consumes 0.25 EU/t while lit</li>
 *   <li>toggles redstone inversion on right click if not manually charged from an electric item</li>
 *   <li>sets hostile mobs on fire while active</li>
 * </ul>
 */
public class LuminatorBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private static final Map<Direction, VoxelShape> SHAPES = createShapes();

    public LuminatorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(ACTIVE, false));
    }

    private static Map<Direction, VoxelShape> createShapes() {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, VoxelShapes.cuboid(0.0, 0.0, 15.0 / 16.0, 1.0, 1.0, 1.0));
        map.put(Direction.SOUTH, VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0, 1.0 / 16.0));
        map.put(Direction.EAST, VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0 / 16.0, 1.0, 1.0));
        map.put(Direction.WEST, VoxelShapes.cuboid(15.0 / 16.0, 0.0, 0.0, 1.0, 1.0, 1.0));
        map.put(Direction.UP, VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 1.0 / 16.0, 1.0));
        map.put(Direction.DOWN, VoxelShapes.cuboid(0.0, 15.0 / 16.0, 0.0, 1.0, 1.0, 1.0));
        return map;
    }

    private static void invalidateAround(World world, BlockPos pos) {
        if (world == null || world.isClient) return;
        EuNetwork.invalidate(world, pos);
        for (Direction direction : Direction.values()) {
            EuNetwork.invalidate(world, pos.offset(direction));
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = this.getDefaultState()
                .with(FACING, ctx.getSide())
                .with(ACTIVE, false);
        return state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos()) ? state : null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return LuminatorBlockEntity.isValidSupport(world, pos, state.get(FACING));
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient && !canPlaceAt(state, world, pos)) {
            world.breakBlock(pos, true);
        }
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
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof LuminatorBlockEntity luminator) {
            return luminator.getComparatorOutput();
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LuminatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.LUMINATOR, LuminatorBlockEntity::serverTick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof LuminatorBlockEntity luminator)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        long freeEu = luminator.getManualChargeFreeEu();
        if (!stack.isEmpty() && freeEu > 0L && ElectricItemManager.isElectric(stack)) {
            long moved = ElectricItemManager.discharge(stack, freeEu, false);
            if (moved > 0L) {
                luminator.addManualCharge(moved);
                return ActionResult.CONSUME;
            }
        }

        luminator.toggleInvertRedstone();
        return ActionResult.CONSUME;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        if (world.isClient || !state.get(ACTIVE) || !(entity instanceof Monster)) {
            return;
        }

        int seconds = 10;
        if (entity instanceof LivingEntity living && living.getGroup() == EntityGroup.UNDEAD) {
            seconds = 20;
        }
        entity.setOnFireFor(seconds);
    }
}
