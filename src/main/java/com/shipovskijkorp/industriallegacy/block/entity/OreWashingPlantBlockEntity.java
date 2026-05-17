package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.OreWashingPlantBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.recipe.OreWashingRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.OreWashingPlantScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OreWashingPlantBlockEntity extends AbstractElectricMachineBlockEntity {
    public static final int SLOT_WATER = 0;
    public static final int SLOT_CELL_OUTPUT = 1;
    public static final int SLOT_INPUT = 2;
    public static final int SLOT_OUTPUT_0 = 3;
    public static final int SLOT_OUTPUT_1 = 4;
    public static final int SLOT_OUTPUT_2 = 5;
    public static final int SLOT_DISCHARGE = 6;
    public static final int SLOT_UPGRADE_0 = 7;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_INPUT, SLOT_WATER };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_INPUT, SLOT_WATER, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_CELL_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 8000L;
    private static final int EU_PER_TICK = 16;
    private static final int BASE_TICKS = 500;
    public static final int WATER_CAPACITY = 8000;
    public static final int WATER_CELL_AMOUNT = 1000;

    private int waterAmount = 0;

    public OreWashingPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_WASHING_PLANT, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS, 5);
    }

    public static void tick(World world, BlockPos pos, BlockState state, OreWashingPlantBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        dirty |= be.gainWater();
        boolean active = be.processMachineTick(world);
        if (state.get(OreWashingPlantBlock.LIT) != active) {
            world.setBlockState(pos, state.with(OreWashingPlantBlock.LIT, active), 3);
        }
        if (dirty || active) be.markDirty();
    }

    @Override
    protected boolean processMachineTick(World world) {
        OreWashingRecipe recipe = findRecipe(world).orElse(null);
        if (recipe == null) {
            resetProgress();
            return false;
        }
        if (waterAmount < recipe.getWaterAmount()) {
            resetProgress();
            return false;
        }

        List<ItemStack> outputs = recipe.getResults();
        if (!canOutput(outputs) || energy < energyConsume) return false;

        energy -= energyConsume;
        maxProgress = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        progress++;

        if (progress >= maxProgress) {
            items.get(SLOT_INPUT).decrement(Math.max(1, recipe.getInputCount()));
            waterAmount = Math.max(0, waterAmount - recipe.getWaterAmount());
            insertOutputs(outputs);
            progress = 0;
        }
        return true;
    }

    private Optional<OreWashingRecipe> findRecipe(World world) {
        return MachineRecipeManager.findOreWashingRecipe(this);
    }

    private boolean gainWater() {
        if (waterAmount > WATER_CAPACITY - WATER_CELL_AMOUNT) return false;
        ItemStack stack = items.get(SLOT_WATER);
        if (stack.isEmpty()) return false;

        ItemStack empty = ItemStack.EMPTY;
        boolean valid = false;
        if (stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.WATER) {
            empty = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
            valid = true;
        } else if (stack.isOf(Items.WATER_BUCKET)) {
            empty = new ItemStack(Items.BUCKET);
            valid = true;
        }

        if (!valid || !canCellOutput(empty)) return false;
        stack.decrement(1);
        insertCellOutput(empty);
        waterAmount += WATER_CELL_AMOUNT;
        return true;
    }

    private boolean canCellOutput(ItemStack stack) {
        ItemStack cur = items.get(SLOT_CELL_OUTPUT);
        return cur.isEmpty() || (ItemStack.canCombine(cur, stack) && cur.getCount() + stack.getCount() <= cur.getMaxCount());
    }

    private void insertCellOutput(ItemStack stack) {
        ItemStack cur = items.get(SLOT_CELL_OUTPUT);
        if (cur.isEmpty()) items.set(SLOT_CELL_OUTPUT, stack);
        else cur.increment(stack.getCount());
    }

    private boolean canOutput(List<ItemStack> outputs) {
        List<ItemStack> temp = new ArrayList<>();
        temp.add(items.get(SLOT_OUTPUT_0).copy());
        temp.add(items.get(SLOT_OUTPUT_1).copy());
        temp.add(items.get(SLOT_OUTPUT_2).copy());

        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (ItemStack slot : temp) {
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) {
                        slot.increment(add);
                        remaining -= add;
                    }
                }
            }
            for (int i = 0; i < temp.size() && remaining > 0; i++) {
                if (temp.get(i).isEmpty()) {
                    ItemStack placed = out.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    temp.set(i, placed);
                    remaining -= add;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private void insertOutputs(List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (int slotId : new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2}) {
                ItemStack slot = items.get(slotId);
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) {
                        slot.increment(add);
                        remaining -= add;
                    }
                }
            }
            for (int slotId : new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2}) {
                if (remaining <= 0) break;
                if (items.get(slotId).isEmpty()) {
                    ItemStack placed = out.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    items.set(slotId, placed);
                    remaining -= add;
                }
            }
        }
    }

    @Override protected int getInputSlot() { return SLOT_INPUT; }
    @Override protected int getOutputSlot() { return SLOT_OUTPUT_0; }
    @Override protected int getDischargeSlot() { return SLOT_DISCHARGE; }
    @Override protected int getFirstUpgradeSlot() { return SLOT_UPGRADE_0; }
    @Override protected int getUpgradeSlotCount() { return UPGRADE_SLOTS; }
    @Override protected int[] getTopSlots() { return TOP_SLOTS; }
    @Override protected int[] getSideSlots() { return SIDE_SLOTS; }
    @Override protected int[] getBottomSlots() { return BOTTOM_SLOTS; }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT_0 || slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2 || slot == SLOT_CELL_OUTPUT;
    }

    @Override
    protected boolean canInsertIntoMachineSlot(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == SLOT_INPUT || slot == SLOT_WATER || slot == SLOT_DISCHARGE || isUpgradeSlot(slot);
    }

    @Override
    protected boolean canExtractFromMachineSlot(int slot, ItemStack stack, Direction dir) {
        return isOutputSlot(slot);
    }

    @Override
    protected int getExtraGuiProperty(int index) {
        return index == 4 ? waterAmount : 0;
    }

    @Override
    protected void setExtraGuiProperty(int index, int value) {
        if (index == 4) waterAmount = Math.max(0, Math.min(WATER_CAPACITY, value));
    }

    @Override
    protected void writeMachineNbt(NbtCompound nbt) {
        nbt.putInt("waterAmount", waterAmount);
    }

    @Override
    protected void readMachineNbt(NbtCompound nbt) {
        waterAmount = Math.max(0, Math.min(WATER_CAPACITY, nbt.getInt("waterAmount")));
    }

    public int getWaterAmount() { return waterAmount; }
    @Override public Text getDisplayName(){ return Text.translatable("container.industrial_legacy.ore_washing_plant"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf){ buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player){ return new OreWashingPlantScreenHandler(syncId, inv, this); }
}
