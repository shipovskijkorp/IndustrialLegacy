package com.shipovskijkorp.industriallegacy.block.entity.base;

import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricSlotHelper;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.MachineUpgradeSupport;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Common IC2-like base for electric machines.
 *
 * Keeps every machine on its own BlockEntityType/registry id, but centralizes the shared
 * TileEntityElectricMachine-style state: inventory, EU buffer, discharge slot, sided IO
 * and GUI sync. Special machines can still own their own processing logic.
 */
public abstract class AbstractElectricMachineBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    protected final DefaultedList<ItemStack> items;

    protected final int dischargeSlot;
    protected final int firstUpgradeSlot;
    protected final int upgradeSlotCount;
    protected final int[] topSlots;
    protected final int[] sideSlots;
    protected final int[] bottomSlots;
    protected final int[] outputSlots;

    protected final long baseEnergyCapacity;
    protected final int baseSinkTier;

    protected long energy = 0L;
    protected long energyCapacity;
    protected int sinkTier;
    protected int progress = 0;
    protected int maxProgress;

    private final PropertyDelegate defaultGuiProps = new PropertyDelegate() {
        @Override public int size() { return 4; }

        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> progress;
                case 3 -> maxProgress;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = clampEnergy(value);
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                default -> { }
            }
        }
    };

    protected AbstractElectricMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inventorySize,
            long baseEnergyCapacity,
            int baseSinkTier,
            int baseMaxProgress,
            int dischargeSlot,
            int firstUpgradeSlot,
            int upgradeSlotCount,
            int[] topSlots,
            int[] sideSlots,
            int[] bottomSlots,
            int[] outputSlots
    ) {
        super(type, pos, state);
        this.items = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
        this.baseEnergyCapacity = baseEnergyCapacity;
        this.energyCapacity = baseEnergyCapacity;
        this.baseSinkTier = baseSinkTier;
        this.sinkTier = baseSinkTier;
        this.maxProgress = Math.max(1, baseMaxProgress);
        this.dischargeSlot = dischargeSlot;
        this.firstUpgradeSlot = firstUpgradeSlot;
        this.upgradeSlotCount = upgradeSlotCount;
        this.topSlots = topSlots;
        this.sideSlots = sideSlots;
        this.bottomSlots = bottomSlots;
        this.outputSlots = outputSlots;
    }

    protected Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.noneOf(UpgradableProperty.class);
    }

    /** Public read-only bridge for shared upgrade helpers. */
    public final Set<UpgradableProperty> getUpgradablePropertiesView() {
        return getUpgradableProperties();
    }

    public final int getFirstUpgradeSlot() {
        return firstUpgradeSlot;
    }

    public final int getUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    protected void recalculateUpgrades() {
        MachineUpgradeSupport.UpgradeRates rates = MachineUpgradeSupport.calculateRates(
                this, firstUpgradeSlot, upgradeSlotCount, getUpgradableProperties(),
                maxProgress, 0, baseEnergyCapacity, baseSinkTier
        );
        this.energyCapacity = rates.energyStorage();
        this.sinkTier = rates.tier();
        if (energy > energyCapacity) energy = energyCapacity;
    }

    protected final boolean tickUpgrades() {
        return MachineUpgradeSupport.tickUpgrades(this, this, firstUpgradeSlot, upgradeSlotCount, getUpgradableProperties());
    }

    protected final boolean hasEffectiveRedstoneInput() {
        return MachineUpgradeSupport.hasRedstoneInput(this);
    }

    protected final boolean chargeFromDischargeSlot() {
        if (dischargeSlot < 0 || dischargeSlot >= items.size()) return false;
        ItemStack discharge = items.get(dischargeSlot);
        long free = energyCapacity - energy;
        if (free <= 0L) return false;
        long extracted = ElectricSlotHelper.dischargeIntoStorage(discharge, free, sinkTier, true, false);
        if (extracted > 0L) {
            energy = Math.min(energyCapacity, energy + extracted);
            return true;
        }
        return false;
    }

    protected final boolean useEnergy(long amount) {
        if (amount <= 0L) return true;
        if (energy < amount) return false;
        energy -= amount;
        return true;
    }

    protected boolean isOutputSlot(int slot) {
        for (int outputSlot : outputSlots) {
            if (outputSlot == slot) return true;
        }
        return false;
    }

    protected boolean isUpgradeSlot(int slot) {
        return firstUpgradeSlot >= 0 && slot >= firstUpgradeSlot && slot < firstUpgradeSlot + upgradeSlotCount;
    }

    protected boolean canOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack out = items.get(slot);
        if (out.isEmpty()) return true;
        if (!ItemStack.canCombine(out, stack)) return false;
        return out.getCount() + stack.getCount() <= out.getMaxCount();
    }

    protected void insertOutput(int slot, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack out = items.get(slot);
        if (out.isEmpty()) {
            items.set(slot, stack.copy());
        } else {
            out.increment(stack.getCount());
        }
    }

    protected long clampEnergy(long value) {
        return Math.max(0L, Math.min(energyCapacity, value));
    }

    public PropertyDelegate getGuiProps() {
        return defaultGuiProps;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = nbt.getLong("energy");
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.contains("maxProgress") ? nbt.getInt("maxProgress") : maxProgress);
        recalculateUpgrades();
        energy = clampEnergy(energy);
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            if (isUpgradeSlot(slot)) recalculateUpgrades();
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        if (isUpgradeSlot(slot)) recalculateUpgrades();
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount());
        if (isUpgradeSlot(slot)) recalculateUpgrades();
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        recalculateUpgrades();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return topSlots;
        if (side == Direction.DOWN) return bottomSlots;
        return sideSlots;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (isOutputSlot(slot)) return false;
        if (isUpgradeSlot(slot)) return MachineUpgradeSupport.isValidUpgrade(stack, getUpgradableProperties());
        if (slot == dischargeSlot) return ElectricSlotHelper.canDischarge(stack, sinkTier, true);
        return true;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return isOutputSlot(slot);
    }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return energyCapacity; }
    @Override public int getSinkTier() { return sinkTier; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long accepted = Math.min(amount, Math.max(0L, energyCapacity - energy));
        if (!simulate && accepted > 0L) {
            energy += accepted;
            markDirty();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        return 0L;
    }

    protected static int[] concat(int[] first, int[] second) {
        int[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
