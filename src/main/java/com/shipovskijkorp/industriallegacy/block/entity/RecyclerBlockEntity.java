package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.RecyclerBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.RecyclerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RecyclerBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;
    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_INPUT };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 45L;
    private static final int EU_PER_TICK = 1;
    private static final int BASE_TICKS = 45;
    private static final int RECYCLE_CHANCE = 8;

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECYCLER, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, RecyclerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processTick(world);
        if (state.get(RecyclerBlock.LIT) != active) world.setBlockState(pos, state.with(RecyclerBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    private boolean processTick(World world) {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty() || isRecyclerBlacklisted(input)) {
            if (progress != 0) progress = 0;
            return false;
        }
        if (energy < energyConsume) return false;
        if (!canPotentiallyOutputScrap()) return false;

        energy -= energyConsume;
        maxProgress = operationLength;
        progress++;

        if (progress >= maxProgress) {
            input.decrement(1);
            if (world.random.nextInt(RECYCLE_CHANCE) == 0) {
                insertOutput(SLOT_OUTPUT, new ItemStack(ModItems.SCRAP));
            }
            progress = 0;
        }
        return true;
    }

    public static boolean isRecyclerBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.isOf(ModItems.SCRAP) || stack.isOf(ModItems.SCRAP_BOX)) return true;
        if (stack.isOf(Items.GLASS_PANE) || stack.isOf(Items.STICK) || stack.isOf(Items.SNOWBALL)) return true;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath();
        return "snow".equals(path)
                || "snow_layer".equals(path)
                || "scaffold".equals(path)
                || path.endsWith("_scaffold");
    }

    private boolean canPotentiallyOutputScrap() {
        ItemStack out = items.get(SLOT_OUTPUT);
        return out.isEmpty() || (out.isOf(ModItems.SCRAP) && out.getCount() < out.getMaxCount());
    }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.recycler"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new RecyclerScreenHandler(syncId, playerInventory, this); }
}
