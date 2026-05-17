package com.shipovskijkorp.industriallegacy.block.entity.base;

import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
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
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shared IC2-like base for electric machine block entities.
 *
 * <p>This is the Fabric-side replacement for IC2's {@code TileEntityElectricMachine}:
 * one common inventory, one EU sink, one discharge slot, one upgrade slot range and
 * one GUI property delegate. Concrete blocks still keep their own registry ids and
 * their own {@link BlockEntityType}; only the machine behaviour is centralized.</p>
 */
public abstract class AbstractElectricMachineBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;
    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int SIMPLE_INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] SIMPLE_TOP_SLOTS = new int[]{SLOT_INPUT};
    private static final int[] SIMPLE_SIDE_SLOTS = new int[]{SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3};
    private static final int[] SIMPLE_BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    protected final DefaultedList<ItemStack> items;

    protected final int defaultTier;
    protected final long defaultEnergyStorage;
    protected final int defaultEnergyConsume;
    protected final int defaultOperationLength;

    protected int sinkTier;
    protected long energyCapacity;
    protected int energyConsume;
    protected int operationLength;
    protected int operationsPerCycle = 1;

    protected long energy;
    protected int progress;
    protected int maxProgress;

    private final int guiPropertyCount;
    protected final PropertyDelegate props = new PropertyDelegate() {
        @Override
        public int size() {
            return guiPropertyCount;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> progress;
                case 3 -> maxProgress;
                default -> getExtraGuiProperty(index);
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(energyCapacity, (long) value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                default -> setExtraGuiProperty(index, value);
            }
        }
    };

    protected AbstractElectricMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                                 int inventorySize, long defaultEnergyStorage, int defaultTier,
                                                 int defaultEnergyConsume, int defaultOperationLength,
                                                 int guiPropertyCount) {
        super(type, pos, state);
        this.items = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
        this.defaultEnergyStorage = Math.max(0L, defaultEnergyStorage);
        this.defaultTier = Math.max(0, defaultTier);
        this.defaultEnergyConsume = Math.max(0, defaultEnergyConsume);
        this.defaultOperationLength = Math.max(1, defaultOperationLength);
        this.guiPropertyCount = Math.max(4, guiPropertyCount);
        this.sinkTier = this.defaultTier;
        this.energyCapacity = this.defaultEnergyStorage;
        this.energyConsume = this.defaultEnergyConsume;
        this.operationLength = this.defaultOperationLength;
        this.maxProgress = this.defaultOperationLength;
    }

    /**
     * Same idea as IC2's upgradeSlot.onChanged()/setOverclockRates(), but intentionally
     * empty until the actual upgrade items are wired. Future upgrade logic belongs here.
     */
    protected void recalculateUpgrades() {
        double progressRatio = this.maxProgress > 0 ? (double) this.progress / (double) this.maxProgress : 0.0;

        this.operationsPerCycle = 1;
        this.operationLength = this.defaultOperationLength;
        this.energyConsume = this.defaultEnergyConsume;
        this.sinkTier = this.defaultTier;
        this.energyCapacity = this.defaultEnergyStorage;
        this.maxProgress = Math.max(1, this.operationLength);
        this.progress = Math.max(0, (int) Math.floor(progressRatio * (double) this.maxProgress + 0.1));
        if (this.progress >= this.maxProgress) this.progress = this.maxProgress - 1;
        if (this.energy > this.energyCapacity) this.energy = this.energyCapacity;
    }

    protected int getExtraGuiProperty(int index) {
        return 0;
    }

    protected void setExtraGuiProperty(int index, int value) {
    }

    public PropertyDelegate getGuiProps() {
        return props;
    }

    protected boolean tickElectricMachine(World world, BlockState state, BooleanProperty litProperty) {
        if (world.isClient) return false;

        boolean dirty = chargeFromDischargeSlot();
        boolean active = processMachineTick(world);

        if (state.contains(litProperty) && state.get(litProperty) != active) {
            world.setBlockState(pos, state.with(litProperty, active), 3);
        }
        if (dirty || active) markDirty();
        return active;
    }

    protected abstract boolean processMachineTick(World world);

    protected boolean chargeFromDischargeSlot() {
        int slot = getDischargeSlot();
        if (slot < 0 || slot >= items.size()) return false;

        ItemStack discharge = items.get(slot);
        if (discharge.isEmpty() || !ElectricItemManager.isElectric(discharge) || discharge.getCount() != 1) return false;

        long free = energyCapacity - energy;
        if (free <= 0L) return false;

        long maxMove = Math.min(free, EuUtil.powerFromTier(sinkTier));
        long extracted = ElectricItemManager.discharge(discharge, maxMove, false);
        if (extracted <= 0L) return false;

        energy += extracted;
        return true;
    }

    protected int getInputSlot() {
        return SLOT_INPUT;
    }

    protected int getOutputSlot() {
        return SLOT_OUTPUT;
    }

    protected int getDischargeSlot() {
        return SLOT_DISCHARGE;
    }

    protected int getFirstUpgradeSlot() {
        return SLOT_UPGRADE_0;
    }

    protected int getUpgradeSlotCount() {
        return UPGRADE_SLOTS;
    }

    protected boolean isUpgradeSlot(int slot) {
        return slot >= getFirstUpgradeSlot() && slot < getFirstUpgradeSlot() + getUpgradeSlotCount();
    }

    protected boolean isOutputSlot(int slot) {
        return slot == getOutputSlot();
    }

    protected int[] getTopSlots() {
        return SIMPLE_TOP_SLOTS;
    }

    protected int[] getSideSlots() {
        return SIMPLE_SIDE_SLOTS;
    }

    protected int[] getBottomSlots() {
        return SIMPLE_BOTTOM_SLOTS;
    }

    protected boolean canInsertIntoMachineSlot(int slot, ItemStack stack, @Nullable Direction dir) {
        if (isOutputSlot(slot)) return false;
        if (slot == getDischargeSlot()) return ElectricItemManager.isElectric(stack);
        return true;
    }

    protected boolean canExtractFromMachineSlot(int slot, ItemStack stack, Direction dir) {
        return isOutputSlot(slot);
    }

    protected boolean canOutput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        ItemStack out = items.get(getOutputSlot());
        if (out.isEmpty()) return true;
        if (!ItemStack.canCombine(out, stack)) return false;
        return out.getCount() + stack.getCount() <= out.getMaxCount();
    }

    protected void insertOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack out = items.get(getOutputSlot());
        if (out.isEmpty()) items.set(getOutputSlot(), stack.copy());
        else if (ItemStack.canCombine(out, stack)) out.increment(stack.getCount());
    }

    protected void resetProgress() {
        if (progress != 0) progress = 0;
        maxProgress = Math.max(1, operationLength);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
        writeMachineNbt(nbt);
    }

    protected void writeMachineNbt(NbtCompound nbt) {
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        recalculateUpgrades();
        energy = Math.max(0L, Math.min(energyCapacity, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
        readMachineNbt(nbt);
    }

    protected void readMachineNbt(NbtCompound nbt) {
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
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
        if (world == null) return false;
        if (world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return getTopSlots();
        if (side == Direction.DOWN) return getBottomSlots();
        return getSideSlots();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return canInsertIntoMachineSlot(slot, stack, dir);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return canExtractFromMachineSlot(slot, stack, dir);
    }

    @Override
    public long getEuStored() {
        return energy;
    }

    @Override
    public long getEuCapacity() {
        return energyCapacity;
    }

    @Override
    public int getSinkTier() {
        return sinkTier;
    }

    @Override
    public int getSourceTier() {
        return 0;
    }

    @Override
    public boolean canInsert(Direction from) {
        return true;
    }

    @Override
    public boolean canExtract(Direction to) {
        return false;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long free = energyCapacity - energy;
        if (free <= 0L) return 0L;
        long accepted = Math.min(amount, free);
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
}
