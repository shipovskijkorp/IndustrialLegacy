package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.StorageBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.item.WrenchItem;
import com.shipovskijkorp.industriallegacy.item.tool.ElectricWrenchItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2 Experimental storage boxes.
 *
 * <p>Source truth: TileEntityStorageBox and subclasses in IC2 2.8.222-ex112.
 * These blocks keep their inventory in the dropped block item.</p>
 */
public final class StorageBoxBlock extends BlockWithEntity {
    private final Type type;

    public StorageBoxBlock(Settings settings, Type type) {
        super(settings);
        this.type = type;
    }

    public Type getStorageBoxType() {
        return type;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBoxBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack held = player.getStackInHand(hand);
        if (held.getItem() instanceof WrenchItem || held.getItem() instanceof ElectricWrenchItem) {
            return ActionResult.PASS;
        }

        if (world.isClient) return ActionResult.SUCCESS;

        NamedScreenHandlerFactory factory = state.createScreenHandlerFactory(world, pos);
        if (factory != null) {
            player.openHandledScreen(factory);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    @Override
    public @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof StorageBoxBlockEntity box ? box : null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && world.getBlockEntity(pos) instanceof StorageBoxBlockEntity box) {
            box.readInventoryFromStack(itemStack);
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!world.isClient && player.getAbilities().creativeMode && be instanceof StorageBoxBlockEntity box && !box.isEmpty()) {
            ItemStack drop = box.createDroppedStack();
            ItemEntity itemEntity = new ItemEntity(world,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    drop);
            itemEntity.setToDefaultPickupDelay();
            world.spawnEntity(itemEntity);
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity be = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (be instanceof StorageBoxBlockEntity box) {
            return List.of(box.createDroppedStack());
        }
        return List.of(new ItemStack(state.getBlock().asItem()));
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof StorageBoxBlockEntity box) {
            return box.createDroppedStack();
        }
        return super.getPickStack(world, pos, state);
    }


    public enum Type {
        WOODEN(27, 9, 3, 176, 166, 7, 16, 7, 83, BlockSoundGroup.WOOD),
        IRON(45, 9, 5, 176, 202, 7, 16, 7, 119, BlockSoundGroup.METAL),
        BRONZE(45, 9, 5, 176, 202, 7, 16, 7, 119, BlockSoundGroup.METAL),
        STEEL(63, 9, 7, 176, 238, 7, 16, 7, 155, BlockSoundGroup.METAL),
        IRIDIUM(126, 18, 7, 338, 238, 7, 16, 88, 155, BlockSoundGroup.METAL);

        private final int slots;
        private final int columns;
        private final int rows;
        private final int guiWidth;
        private final int guiHeight;
        private final int inventoryX;
        private final int inventoryY;
        private final int playerInventoryX;
        private final int playerInventoryY;
        private final BlockSoundGroup soundGroup;

        Type(int slots, int columns, int rows, int guiWidth, int guiHeight, int inventoryX, int inventoryY,
             int playerInventoryX, int playerInventoryY, BlockSoundGroup soundGroup) {
            this.slots = slots;
            this.columns = columns;
            this.rows = rows;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            this.inventoryX = inventoryX;
            this.inventoryY = inventoryY;
            this.playerInventoryX = playerInventoryX;
            this.playerInventoryY = playerInventoryY;
            this.soundGroup = soundGroup;
        }

        public int slots() { return slots; }
        public int columns() { return columns; }
        public int rows() { return rows; }
        public int guiWidth() { return guiWidth; }
        public int guiHeight() { return guiHeight; }
        public int inventoryX() { return inventoryX; }
        public int inventoryY() { return inventoryY; }
        public int playerInventoryX() { return playerInventoryX; }
        public int playerInventoryY() { return playerInventoryY; }
        public BlockSoundGroup soundGroup() { return soundGroup; }
    }

    public static Type getType(BlockState state) {
        if (state.getBlock() instanceof StorageBoxBlock box) {
            return box.getStorageBoxType();
        }
        return Type.WOODEN;
    }
}
