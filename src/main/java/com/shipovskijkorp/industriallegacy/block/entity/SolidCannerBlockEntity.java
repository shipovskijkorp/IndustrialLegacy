package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.SolidCannerBlock;
import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.SolidCannerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class SolidCannerBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_CONTAINER = 0;
    public static final int SLOT_FILL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_DISCHARGE = 3;
    public static final int SLOT_UPGRADE_0 = 4;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_CONTAINER, SLOT_FILL };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_CONTAINER, SLOT_FILL, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 400L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 200;

    public SolidCannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLID_CANNER, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, SolidCannerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processStandardMachine(world);
        if (state.get(SolidCannerBlock.LIT) != active) world.setBlockState(pos, state.with(SolidCannerBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    @Override
    protected MachineOperation getOperation(World world) {
        CanningRecipe recipe = MachineRecipeManager.findCanningRecipe(this).orElse(null);
        if (recipe == null) return null;
        ItemStack result = recipe.getResultStack().copy();
        int ticks = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        return operation(
                List.of(new SlotConsumption(SLOT_CONTAINER, recipe.getContainerCount()), new SlotConsumption(SLOT_FILL, recipe.getFillCount())),
                List.of(new SlotOutput(SLOT_OUTPUT, result)),
                ticks,
                energyConsume
        );
    }

    public static boolean isValidContainer(ItemStack stack) { return !stack.isEmpty(); }
    public static boolean isValidFill(ItemStack stack) { return !stack.isEmpty(); }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.solid_canner"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new SolidCannerScreenHandler(syncId, inv, this); }
}
