package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.MaceratorBlock;
import com.shipovskijkorp.industriallegacy.recipe.MaceratorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.MaceratorScreenHandler;
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

public class MaceratorBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;
    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[]{SLOT_INPUT};
    private static final int[] SIDE_SLOTS = new int[]{SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    private static final int TIER = 1;
    private static final long CAPACITY = 600L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 300;

    public MaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACERATOR, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, MaceratorBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processStandardMachine(world);
        if (state.get(MaceratorBlock.LIT) != active) world.setBlockState(pos, state.with(MaceratorBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    @Override
    protected MachineOperation getOperation(World world) {
        MaceratorRecipe recipe = MachineRecipeManager.findMaceratorRecipe(this).orElse(null);
        if (recipe == null) return null;
        ItemStack result = recipe.getOutput(world.getRegistryManager()).copy();
        int ticks = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        return operation(SLOT_INPUT, recipe.getIngredientCount(), SLOT_OUTPUT, result, ticks, energyConsume);
    }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.macerator"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new MaceratorScreenHandler(syncId, inv, this, getGuiProps()); }
}
