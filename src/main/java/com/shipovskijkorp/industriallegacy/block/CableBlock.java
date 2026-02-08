package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IL-like thin cable block.
 *
 * <p>The block has no baked JSON model: {@link BlockRenderType#INVISIBLE} is used and the cable is drawn
 * by {@code CableBlockEntityRenderer} with the original IL textures.</p>
 */
public class CableBlock extends BlockWithEntity {

    private static void invalidateAround(World world, BlockPos pos) {
        EuNetwork.invalidate(world, pos);
        for (Direction d : Direction.values()) {
            EuNetwork.invalidate(world, pos.offset(d));
        }
    }

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
        if (world.isClient) return;

        // ✅ 2) When placed: apply stack NBT oxidation into BE (only copper, only uninsulated)
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof CableBlockEntity cableBe) {
            if (this.kind == CableKind.COPPER && this.insulation == 0) {
                cableBe.setOxidationLevel(CableItem.getOxidation(itemStack));
            }
            cableBe.refreshDerivedState();
        }

        invalidateAround(world, pos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (world.isClient) return;

        if (state.getBlock() != newState.getBlock()) {
            invalidateAround(world, pos);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (world.isClient) return;

        invalidateAround(world, pos);

        // Splitter toggles active state based on redstone input.
        if (kind == CableKind.SPLITTER) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cableBe) {
                cableBe.refreshDerivedState();
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);

        // Only copper uninsulated participates.
        if (this.kind == CableKind.COPPER && this.insulation == 0) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cableBe) {

                // Axe scrape: oxidized -> weathered -> exposed -> clean
                if (held.getItem() instanceof AxeItem) {
                    int lvl = cableBe.getOxidationLevel();
                    if (lvl > 0) {
                        if (!world.isClient) {
                            cableBe.setOxidationLevel(lvl - 1);
                            held.damage(1, player, p -> p.sendToolBreakStatus(hand));
                            world.syncWorldEvent(player, WorldEvents.BLOCK_SCRAPED, pos, 0);
                            world.playSound(null, pos, SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                            invalidateAround(world, pos);
                        }
                        return ActionResult.SUCCESS;
                    }
                }

                // Rubber insulation: only if cable is clean (lvl=0)
                if (held.isOf(ModItems.RUBBER)) {
                    if (cableBe.getOxidationLevel() != 0) {
                        if (!world.isClient) {
                            player.sendMessage(net.minecraft.text.Text.translatable("msg.industrial_legacy.cable_needs_cleaning"), true);
                        }
                        return ActionResult.SUCCESS;
                    }

                    if (!world.isClient) {
                        // Replace with insulated copper cable block (lvl 1 insulation)
                        world.setBlockState(pos, ModBlocks.COPPER_CABLE_1.getDefaultState(), Block.NOTIFY_ALL);
                        if (!player.getAbilities().creativeMode) held.decrement(1);
                        invalidateAround(world, pos);
                    }
                    return ActionResult.SUCCESS;
                }
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    // ✅ 3) Drops preserve oxidation (only copper uninsulated)
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity be = builder.getOptional(LootContextParameters.BLOCK_ENTITY);

        // Always drop exactly "the cable item stack for this block variant"
        ItemStack drop = CableItem.createStack(ModItems.CABLE, this.kind, this.insulation);

        if (this.kind == CableKind.COPPER && this.insulation == 0 && be instanceof CableBlockEntity cableBe) {
            drop.getOrCreateNbt().putInt(CableItem.NBT_OXIDATION, cableBe.getOxidationLevel());
        }

        return List.of(drop);
    }

    // ✅ 4) Middle-click gives current oxidation stage (only copper uninsulated)
    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack pick = CableItem.createStack(ModItems.CABLE, this.kind, this.insulation);

        if (this.kind == CableKind.COPPER && this.insulation == 0) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cableBe) {
                pick.getOrCreateNbt().putInt(CableItem.NBT_OXIDATION, cableBe.getOxidationLevel());
            }
        }

        return pick;
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
