package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * IL-like thin cable block.
 *
 * <p>The block has no baked JSON model: {@link BlockRenderType#INVISIBLE} is used and the cable is drawn
 * by {@code CableBlockEntityRenderer} with the original IL textures.</p>
 */
public class CableBlock extends BlockWithEntity {

    private final CableKind kind;
    private final int insulation;
    private final String texturePath; // block atlas path without extension

    public CableBlock(Settings settings, CableKind kind, int insulation, String texturePath) {
        super(settings);
        this.kind = kind;
        this.insulation = insulation;
        this.texturePath = texturePath;
    }

    public CableKind getKind() {
        return kind;
    }

    public int getInsulation() {
        return insulation;
    }

    /** @return texture id path relative to namespace, e.g. "block/wiring/cable/copper_cable_0". */
    public String getTexturePath() {
        return texturePath;
    }

    /**
     * IL-ish visual thickness: base thickness + insulation * (2/16).
     */
    public float getVisualWidth() {
        float w = kind.thickness;
        if (insulation > 0) {
            w += insulation * (2.0f / 16.0f);
        }
        return Math.max(2.0f / 16.0f, Math.min(1.0f, w));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.CABLE, CableBlockEntity::tick);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            // Network topology changed.
            EuNetwork.invalidate(world);
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cableBe) {
                cableBe.refreshDerivedState();
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!world.isClient && state.getBlock() != newState.getBlock()) {
            // Network topology changed.
            EuNetwork.invalidate(world);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (world.isClient) return;

        // Splitter enable/disable affects routes.
        if (kind == CableKind.SPLITTER) {
            EuNetwork.invalidate(world);
        }

        // Splitter cable toggles active state based on redstone input (matches IL load/unload).
        if (kind == CableKind.SPLITTER) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cableBe) {
                cableBe.refreshDerivedState();
            }
        }
    }

    // ---- Shapes (thin cable + arms) ----

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return buildShape(world, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return buildShape(world, pos);
    }

    private VoxelShape buildShape(BlockView world, BlockPos pos) {
        float w = getVisualWidth();
        float min = 0.5f - w / 2.0f;
        float max = 0.5f + w / 2.0f;

        VoxelShape shape = box(min, min, min, max, max, max);

        for (Direction dir : Direction.values()) {
            if (connectsTo(world, pos, dir)) {
                shape = VoxelShapes.union(shape, arm(min, max, dir));
            }
        }

        return shape;
    }

    private boolean connectsTo(BlockView world, BlockPos pos, Direction dir) {
        BlockPos np = pos.offset(dir);
        BlockState ns = world.getBlockState(np);
        Block nb = ns.getBlock();

        if (nb instanceof CableBlock) {
            return true;
        }

        BlockEntity be = world.getBlockEntity(np);
        if (be instanceof IEuEnergyStorage storage) {
            Direction face = dir.getOpposite();
            return storage.canInsert(face) || storage.canExtract(face);
        }

        return false;
    }

    private VoxelShape arm(float min, float max, Direction dir) {
        return switch (dir) {
            case NORTH -> box(min, min, 0.0f, max, max, min);
            case SOUTH -> box(min, min, max, max, max, 1.0f);
            case WEST -> box(0.0f, min, min, min, max, max);
            case EAST -> box(max, min, min, 1.0f, max, max);
            case DOWN -> box(min, 0.0f, min, max, min, max);
            case UP -> box(min, max, min, max, 1.0f, max);
        };
    }

    private static VoxelShape box(float x1, float y1, float z1, float x2, float y2, float z2) {
        return Block.createCuboidShape(x1 * 16.0, y1 * 16.0, z1 * 16.0, x2 * 16.0, y2 * 16.0, z2 * 16.0);
    }

    // ---- Detector cable redstone/comparator ----

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return kind == CableKind.DETECTOR;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (kind != CableKind.DETECTOR) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof CableBlockEntity cableBe) {
            return cableBe.getRedstoneLevel();
        }
        return 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return getWeakRedstonePower(state, world, pos, direction);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return kind == CableKind.DETECTOR;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (kind != CableKind.DETECTOR) return 0;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof CableBlockEntity cableBe) {
            return cableBe.getComparatorLevel();
        }
        return 0;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Cables don't have directional state, but this keeps vanilla placement flow consistent.
        return getDefaultState();
    }
}
