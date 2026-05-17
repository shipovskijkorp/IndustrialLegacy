package com.shipovskijkorp.industriallegacy.block.entity;

import java.util.Set;
import java.util.EnumSet;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradeableFluidMachine;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.OreWashingPlantBlock;
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
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OreWashingPlantBlockEntity extends AbstractStandardMachineBlockEntity implements UpgradeableFluidMachine {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_0 = 1;
    public static final int SLOT_OUTPUT_1 = 2;
    public static final int SLOT_OUTPUT_2 = 3;
    public static final int SLOT_WATER = 4;
    public static final int SLOT_CELL_OUTPUT = 5;
    public static final int SLOT_DISCHARGE = 6;
    public static final int SLOT_UPGRADE_0 = 7;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;
    public static final int WATER_CAPACITY = 8000;

    private static final int[] TOP_SLOTS = new int[] { SLOT_INPUT, SLOT_WATER };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_INPUT, SLOT_WATER, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_CELL_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 8000L;
    private static final int EU_PER_TICK = 16;
    private static final int BASE_TICKS = 500;
    private static final int WATER_CELL_AMOUNT = 1000;

    private int waterAmount = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 5; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int)Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int)Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> waterAmount;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> energy = clampEnergy(value);
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> waterAmount = Math.max(0, Math.min(WATER_CAPACITY, value));
                default -> { }
            }
        }
    };

    public OreWashingPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_WASHING_PLANT, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS,
                new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2, SLOT_CELL_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, OreWashingPlantBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        dirty |= be.tickUpgrades();
        be.gainWater();
        boolean active = be.processTick(world);
        if (state.get(OreWashingPlantBlock.LIT) != active) world.setBlockState(pos, state.with(OreWashingPlantBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    private boolean processTick(World world) {
        OreWashingRecipe recipe = MachineRecipeManager.findOreWashingRecipe(this).orElse(null);
        if (recipe == null) { progress = 0; return false; }
        if (waterAmount < recipe.getWaterAmount()) { progress = 0; return false; }
        List<ItemStack> outputs = recipe.getResults();
        if (!canOutputStacks(outputs) || energy < energyConsume) return false;
        energy -= energyConsume;
        maxProgress = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_INPUT).decrement(recipe.getInputCount());
            waterAmount = Math.max(0, waterAmount - recipe.getWaterAmount());
            insertOutputStacks(outputs);
            progress = 0;
        }
        return true;
    }

    private void gainWater() {
        if (waterAmount > WATER_CAPACITY - WATER_CELL_AMOUNT) return;
        ItemStack stack = items.get(SLOT_WATER);
        if (stack.isEmpty()) return;
        ItemStack empty = ItemStack.EMPTY;
        boolean valid = false;
        if (stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.WATER) {
            empty = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
            valid = true;
        } else if (stack.isOf(Items.WATER_BUCKET)) {
            empty = new ItemStack(Items.BUCKET);
            valid = true;
        }
        if (!valid || !canOutput(SLOT_CELL_OUTPUT, empty)) return;
        stack.decrement(1);
        insertOutput(SLOT_CELL_OUTPUT, empty);
        waterAmount += WATER_CELL_AMOUNT;
        markDirty();
    }

    private boolean canOutputStacks(List<ItemStack> outputs) {
        List<ItemStack> temp = new ArrayList<>();
        temp.add(items.get(SLOT_OUTPUT_0).copy());
        temp.add(items.get(SLOT_OUTPUT_1).copy());
        temp.add(items.get(SLOT_OUTPUT_2).copy());
        return canMergeToTempOutputs(outputs, temp);
    }

    private void insertOutputStacks(List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            ItemStack remaining = out.copy();
            for (int slot : new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2}) {
                if (remaining.isEmpty()) break;
                ItemStack current = items.get(slot);
                if (!current.isEmpty() && ItemStack.canCombine(current, remaining)) {
                    int move = Math.min(remaining.getCount(), current.getMaxCount() - current.getCount());
                    if (move > 0) { current.increment(move); remaining.decrement(move); }
                }
            }
            for (int slot : new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2}) {
                if (remaining.isEmpty()) break;
                ItemStack current = items.get(slot);
                if (current.isEmpty()) {
                    int move = Math.min(remaining.getCount(), remaining.getMaxCount());
                    ItemStack placed = remaining.copy();
                    placed.setCount(move);
                    items.set(slot, placed);
                    remaining.decrement(move);
                }
            }
        }
    }

    private static boolean canMergeToTempOutputs(List<ItemStack> outputs, List<ItemStack> temp) {
        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (ItemStack slot : temp) {
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) { slot.increment(add); remaining -= add; }
                }
            }
            for (int i = 0; i < temp.size() && remaining > 0; i++) {
                ItemStack slot = temp.get(i);
                if (slot.isEmpty()) {
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

    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT_0 || slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2 || slot == SLOT_CELL_OUTPUT) return false;
        if (slot == SLOT_WATER) return stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.WATER || stack.isOf(Items.WATER_BUCKET);
        return super.canInsert(slot, stack, dir);
    }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT_0 || slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2 || slot == SLOT_CELL_OUTPUT; }

    @Override
    protected Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
                UpgradableProperty.Processing,
                UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming
        );
    }

    @Override
    public int fillFromUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate) {
        if (fluid != UniversalFluidCellItem.CellFluid.WATER || amountMb <= 0) return 0;
        int accepted = Math.min(amountMb, WATER_CAPACITY - waterAmount);
        if (!simulate && accepted > 0) {
            waterAmount += accepted;
            markDirty();
        }
        return accepted;
    }

    @Override
    public int drainForUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate) {
        return 0;
    }

    @Override
    public UniversalFluidCellItem.CellFluid getPreferredDrainFluidForUpgrade() {
        return UniversalFluidCellItem.CellFluid.EMPTY;
    }

    @Override public PropertyDelegate getGuiProps() { return props; }
    public int getWaterAmount() { return waterAmount; }
    public int getWaterCapacity() { return WATER_CAPACITY; }

    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); nbt.putInt("waterAmount", waterAmount); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); waterAmount = Math.max(0, Math.min(WATER_CAPACITY, nbt.getInt("waterAmount"))); }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.ore_washing_plant"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new OreWashingPlantScreenHandler(syncId, inv, this); }
}
