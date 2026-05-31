package com.shipovskijkorp.industriallegacy.block.entity.upgrade;

import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.item.MachineUpgradeItem;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Shared IL upgrade math and tick-side transfer logic.
 * Source truth: IL 2.8.222-ex112 ItemUpgradeModule + InvSlotUpgrade.
 */
public final class MachineUpgradeSupport {
    private MachineUpgradeSupport() {}

    public record UpgradeRates(int operationsPerTick, int operationLength, int energyDemand, long energyStorage, int tier) { }

    public static UpgradeRates calculateRates(Inventory inventory, int firstUpgradeSlot, int upgradeSlotCount,
                                              Set<UpgradableProperty> properties,
                                              int defaultOperationLength, int defaultEnergyDemand,
                                              long defaultEnergyStorage, int defaultTier) {
        int extraProcessTime = 0;
        double processTimeMultiplier = 1.0;
        int extraEnergyDemand = 0;
        double energyDemandMultiplier = 1.0;
        long extraEnergyStorage = 0L;
        double energyStorageMultiplier = 1.0;
        int extraTier = 0;

        for (int slot = firstUpgradeSlot; slot >= 0 && slot < firstUpgradeSlot + upgradeSlotCount; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!isValidUpgrade(stack, properties)) continue;
            MachineUpgradeItem item = (MachineUpgradeItem) stack.getItem();
            int size = stack.getCount();
            switch (item.getUpgradeType()) {
                case OVERCLOCKER -> {
                    processTimeMultiplier *= Math.pow(0.7, size);
                    energyDemandMultiplier *= Math.pow(1.6, size);
                }
                case ENERGY_STORAGE -> extraEnergyStorage += 10_000L * size;
                case TRANSFORMER -> extraTier += size;
                default -> { }
            }
        }

        int opLen = getOperationLength(defaultOperationLength, extraProcessTime, processTimeMultiplier);
        int ops = getOperationsPerTick(defaultOperationLength, extraProcessTime, processTimeMultiplier);
        int demand = applyModifier(defaultEnergyDemand, extraEnergyDemand, energyDemandMultiplier);
        long storageExtra = extraEnergyStorage + (long) opLen * Math.max(0, demand);
        long storage = applyModifier(defaultEnergyStorage, storageExtra, energyStorageMultiplier);
        int tier = applyModifier(defaultTier, extraTier, 1.0);
        return new UpgradeRates(Math.max(1, ops), Math.max(1, opLen), Math.max(0, demand), Math.max(0L, storage), Math.max(0, tier));
    }

    public static int getOperationsPerTick(int defaultOperationLength, int extraProcessTime, double processTimeMultiplier) {
        if (defaultOperationLength == 0) return 64;
        return getOpsPerTick(getStackOpLen(defaultOperationLength, extraProcessTime, processTimeMultiplier));
    }

    public static int getOperationLength(int defaultOperationLength, int extraProcessTime, double processTimeMultiplier) {
        if (defaultOperationLength == 0) return 1;
        double stackOpLen = getStackOpLen(defaultOperationLength, extraProcessTime, processTimeMultiplier);
        int opsPerTick = getOpsPerTick(stackOpLen);
        return Math.max(1, (int) Math.round(stackOpLen * (double) opsPerTick / 64.0));
    }

    private static double getStackOpLen(int defaultOperationLength, int extraProcessTime, double processTimeMultiplier) {
        return ((double) defaultOperationLength + (double) extraProcessTime) * 64.0 * processTimeMultiplier;
    }

    private static int getOpsPerTick(double stackOpLen) {
        return (int) Math.min(Math.ceil(64.0 / stackOpLen), Integer.MAX_VALUE);
    }

    private static int applyModifier(int base, int extra, double multiplier) {
        double ret = Math.round(((double) base + (double) extra) * multiplier);
        return ret > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ret;
    }

    private static long applyModifier(long base, long extra, double multiplier) {
        double ret = Math.round(((double) base + (double) extra) * multiplier);
        return ret > Long.MAX_VALUE ? Long.MAX_VALUE : (long) ret;
    }

    public static boolean isValidUpgrade(ItemStack stack, Set<UpgradableProperty> properties) {
        return !stack.isEmpty()
                && stack.getItem() instanceof MachineUpgradeItem upgrade
                && upgrade.isSuitableFor(stack, properties);
    }

    public static boolean tickUpgrades(BlockEntity machine, Inventory inventory, int firstUpgradeSlot, int upgradeSlotCount, Set<UpgradableProperty> properties) {
        boolean changed = false;
        for (int slot = firstUpgradeSlot; slot >= 0 && slot < firstUpgradeSlot + upgradeSlotCount; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!isValidUpgrade(stack, properties)) continue;
            MachineUpgradeItem upgrade = (MachineUpgradeItem) stack.getItem();
            changed |= switch (upgrade.getUpgradeType()) {
                case EJECTOR, ADVANCED_EJECTOR -> tickItemEjector(machine, inventory, stack);
                case PULLING, ADVANCED_PULLING -> tickItemPulling(machine, inventory, stack);
                case FLUID_EJECTOR -> tickFluidEjector(machine, stack);
                case FLUID_PULLING -> tickFluidPulling(machine, stack);
                default -> false;
            };
        }
        return changed;
    }

    private static boolean tickItemEjector(BlockEntity machine, Inventory source, ItemStack upgradeStack) {
        World world = machine.getWorld();
        if (world == null) return false;
        boolean changed = false;
        for (Direction dir : targetDirections(upgradeStack)) {
            BlockEntity targetBe = world.getBlockEntity(machine.getPos().offset(dir));
            if (!(targetBe instanceof Inventory target)) continue;
            changed |= transfer(source, target, dir, transferAmount(upgradeStack), stack -> true);
        }
        return changed;
    }

    private static boolean tickItemPulling(BlockEntity machine, Inventory target, ItemStack upgradeStack) {
        World world = machine.getWorld();
        if (world == null) return false;
        boolean changed = false;
        for (Direction dir : targetDirections(upgradeStack)) {
            BlockEntity sourceBe = world.getBlockEntity(machine.getPos().offset(dir));
            if (!(sourceBe instanceof Inventory source)) continue;
            changed |= transfer(source, target, dir.getOpposite(), transferAmount(upgradeStack), stack -> true);
        }
        return changed;
    }

    private static boolean tickFluidEjector(BlockEntity machine, ItemStack upgradeStack) {
        if (!(machine instanceof UpgradeableFluidMachine source)) return false;
        World world = machine.getWorld();
        if (world == null) return false;
        boolean changed = false;
        int amount = fluidTransferAmount(upgradeStack);
        for (Direction dir : targetDirections(upgradeStack)) {
            BlockEntity targetBe = world.getBlockEntity(machine.getPos().offset(dir));
            if (!(targetBe instanceof UpgradeableFluidMachine target)) continue;
            UniversalFluidCellItem.CellFluid fluid = source.getPreferredDrainFluidForUpgrade();
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) continue;
            int drained = source.drainForUpgrade(fluid, amount, true);
            if (drained <= 0) continue;
            int accepted = target.fillFromUpgrade(fluid, drained, true);
            int moved = Math.min(drained, accepted);
            if (moved <= 0) continue;
            source.drainForUpgrade(fluid, moved, false);
            target.fillFromUpgrade(fluid, moved, false);
            changed = true;
        }
        return changed;
    }

    private static boolean tickFluidPulling(BlockEntity machine, ItemStack upgradeStack) {
        if (!(machine instanceof UpgradeableFluidMachine target)) return false;
        World world = machine.getWorld();
        if (world == null) return false;
        boolean changed = false;
        int amount = fluidTransferAmount(upgradeStack);
        for (Direction dir : targetDirections(upgradeStack)) {
            BlockEntity sourceBe = world.getBlockEntity(machine.getPos().offset(dir));
            if (!(sourceBe instanceof UpgradeableFluidMachine source)) continue;
            UniversalFluidCellItem.CellFluid fluid = source.getPreferredDrainFluidForUpgrade();
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) continue;
            int drained = source.drainForUpgrade(fluid, amount, true);
            if (drained <= 0) continue;
            int accepted = target.fillFromUpgrade(fluid, drained, true);
            int moved = Math.min(drained, accepted);
            if (moved <= 0) continue;
            source.drainForUpgrade(fluid, moved, false);
            target.fillFromUpgrade(fluid, moved, false);
            changed = true;
        }
        return changed;
    }

    private static Direction[] targetDirections(ItemStack upgradeStack) {
        Direction dir = MachineUpgradeItem.getDirection(upgradeStack);
        return dir == null ? Direction.values() : new Direction[]{dir};
    }

    private static int transferAmount(ItemStack upgradeStack) {
        return (int) Math.pow(4.0, Math.min(4, Math.max(0, upgradeStack.getCount() - 1)));
    }

    private static int fluidTransferAmount(ItemStack upgradeStack) {
        return (int) (50.0 * Math.pow(4.0, Math.min(4, Math.max(0, upgradeStack.getCount() - 1))));
    }

    private static boolean transfer(Inventory source, Inventory target, Direction sourceToTarget, int amount, Predicate<ItemStack> filter) {
        if (amount <= 0) return false;
        boolean changed = false;
        int remaining = amount;
        for (int sourceSlot : allSlots(source)) {
            if (remaining <= 0) break;
            ItemStack sourceStack = source.getStack(sourceSlot);
            if (sourceStack.isEmpty() || !filter.test(sourceStack)) continue;
            if (!canExtract(source, sourceSlot, sourceStack, sourceToTarget)) continue;
            int toMove = Math.min(remaining, sourceStack.getCount());
            ItemStack moving = sourceStack.copy();
            moving.setCount(toMove);
            int inserted = insertInto(target, moving, sourceToTarget.getOpposite());
            if (inserted <= 0) continue;
            sourceStack.decrement(inserted);
            source.markDirty();
            target.markDirty();
            changed = true;
            remaining -= inserted;
        }
        return changed;
    }

    private static int insertInto(Inventory target, ItemStack stack, Direction side) {
        if (stack.isEmpty()) return 0;
        int original = stack.getCount();
        for (int targetSlot : allSlots(target)) {
            if (stack.isEmpty()) break;
            if (!canInsert(target, targetSlot, stack, side)) continue;
            ItemStack existing = target.getStack(targetSlot);
            if (!existing.isEmpty() && !ItemStack.canCombine(existing, stack)) continue;
            int limit = Math.min(target.getMaxCountPerStack(), stack.getMaxCount());
            if (!existing.isEmpty()) limit = Math.min(limit, existing.getMaxCount());
            int room = existing.isEmpty() ? limit : limit - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, stack.getCount());
            if (existing.isEmpty()) {
                ItemStack placed = stack.copy();
                placed.setCount(moved);
                target.setStack(targetSlot, placed);
            } else {
                existing.increment(moved);
            }
            stack.decrement(moved);
        }
        return original - stack.getCount();
    }


    private static int[] allSlots(Inventory inventory) {
        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    private static int[] availableSlots(Inventory inventory, Direction side) {
        if (inventory instanceof SidedInventory sided) return sided.getAvailableSlots(side);
        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    private static boolean canInsert(Inventory inventory, int slot, ItemStack stack, Direction side) {
        if (inventory instanceof SidedInventory sided) return sided.canInsert(slot, stack, side);
        return inventory.isValid(slot, stack);
    }

    private static boolean canExtract(Inventory inventory, int slot, ItemStack stack, Direction side) {
        if (inventory instanceof SidedInventory sided) return sided.canExtract(slot, stack, side);
        return true;
    }

    public static boolean hasRedstoneInput(AbstractElectricMachineBlockEntity machine) {
        World world = machine.getWorld();
        if (world == null) return false;
        boolean powered = world.isReceivingRedstonePower(machine.getPos());
        if (countAccepted(machine, MachineUpgradeItem.UpgradeType.REDSTONE_INVERTER) > 0) {
            powered = !powered;
        }
        return powered;
    }

    private static int countAccepted(AbstractElectricMachineBlockEntity machine, MachineUpgradeItem.UpgradeType type) {
        int count = 0;
        for (int slot = machine.getFirstUpgradeSlot(); slot >= 0 && slot < machine.getFirstUpgradeSlot() + machine.getUpgradeSlotCount(); slot++) {
            ItemStack stack = machine.getStack(slot);
            if (!isValidUpgrade(stack, machine.getUpgradablePropertiesView())) continue;
            MachineUpgradeItem item = (MachineUpgradeItem) stack.getItem();
            if (item.getUpgradeType() == type) count += stack.getCount();
        }
        return count;
    }
}
