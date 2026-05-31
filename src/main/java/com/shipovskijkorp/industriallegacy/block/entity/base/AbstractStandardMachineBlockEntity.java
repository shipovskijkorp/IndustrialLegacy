package com.shipovskijkorp.industriallegacy.block.entity.base;

import com.shipovskijkorp.industriallegacy.block.entity.upgrade.MachineUpgradeSupport;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * IL-like standard machine base, matching the TileEntityStandardMachine idea:
 * one central progress/energy loop, with per-machine recipe lookup and finish effects.
 */
public abstract class AbstractStandardMachineBlockEntity extends AbstractElectricMachineBlockEntity {
    protected final int baseEnergyConsume;
    protected final int baseOperationLength;

    protected int energyConsume;
    protected int operationLength;
    protected int operationsPerCycle = 1;

    protected AbstractStandardMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inventorySize,
            long baseEnergyCapacity,
            int baseSinkTier,
            int baseEnergyConsume,
            int baseOperationLength,
            int dischargeSlot,
            int firstUpgradeSlot,
            int upgradeSlotCount,
            int[] topSlots,
            int[] sideSlots,
            int[] bottomSlots,
            int[] outputSlots
    ) {
        super(type, pos, state, inventorySize, baseEnergyCapacity, baseSinkTier, baseOperationLength,
                dischargeSlot, firstUpgradeSlot, upgradeSlotCount, topSlots, sideSlots, bottomSlots, outputSlots);
        this.baseEnergyConsume = baseEnergyConsume;
        this.baseOperationLength = Math.max(1, baseOperationLength);
        this.energyConsume = baseEnergyConsume;
        this.operationLength = this.baseOperationLength;
        this.maxProgress = this.operationLength;
    }

    @Override
    protected Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
                UpgradableProperty.Processing,
                UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing
        );
    }

    @Override
    protected void recalculateUpgrades() {
        int oldMaxProgress = Math.max(1, this.maxProgress);
        int oldProgress = this.progress;
        MachineUpgradeSupport.UpgradeRates rates = MachineUpgradeSupport.calculateRates(
                this, firstUpgradeSlot, upgradeSlotCount, getUpgradableProperties(),
                baseOperationLength, baseEnergyConsume, baseEnergyCapacity, baseSinkTier
        );
        this.energyCapacity = rates.energyStorage();
        this.sinkTier = rates.tier();
        this.energyConsume = rates.energyDemand();
        this.operationLength = rates.operationLength();
        this.operationsPerCycle = rates.operationsPerTick();
        this.maxProgress = this.operationLength;
        this.progress = Math.max(0, (int) Math.floor((double) oldProgress / (double) oldMaxProgress * (double) this.maxProgress + 0.1));
        if (energy > energyCapacity) energy = energyCapacity;
    }

    protected MachineOperation getOperation(World world) {
        return null;
    }

    protected boolean processStandardMachine(World world) {
        MachineOperation operation = getOperation(world);
        if (operation == null) {
            if (progress != 0) progress = 0;
            return false;
        }

        if (!canOutput(operation.outputs)) return false;
        if (energy < operation.euPerTick) return false;

        energy -= operation.euPerTick;
        maxProgress = operation.ticks;
        progress++;

        if (progress >= maxProgress) {
            for (int i = 0; i < operationsPerCycle; i++) {
                if (!canConsume(operation.inputs) || !canOutput(operation.outputs)) break;
                consume(operation.inputs);
                insertOutputs(operation.outputs);
                if (operation.onFinish != null) operation.onFinish.run();
            }
            progress = 0;
        }
        return true;
    }

    protected boolean canConsume(List<SlotConsumption> inputs) {
        for (SlotConsumption input : inputs) {
            if (input.amount <= 0) continue;
            ItemStack stack = items.get(input.slot);
            if (stack.isEmpty() || stack.getCount() < input.amount) return false;
        }
        return true;
    }

    protected void consume(List<SlotConsumption> inputs) {
        for (SlotConsumption input : inputs) {
            if (input.amount > 0) items.get(input.slot).decrement(input.amount);
        }
    }

    protected boolean canOutput(List<SlotOutput> outputs) {
        List<SlotSnapshot> snapshots = new ArrayList<>();
        for (SlotOutput output : outputs) {
            snapshots.add(new SlotSnapshot(output.slot, items.get(output.slot).copy()));
        }

        for (SlotOutput output : outputs) {
            if (output.stack.isEmpty()) return false;
            int remaining = output.stack.getCount();
            for (SlotSnapshot snapshot : snapshots) {
                if (snapshot.slot != output.slot) continue;
                ItemStack existing = snapshot.stack;
                if (existing.isEmpty()) {
                    ItemStack placed = output.stack.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    snapshot.stack = placed;
                    remaining -= add;
                } else if (ItemStack.canCombine(existing, output.stack)) {
                    int add = Math.min(remaining, existing.getMaxCount() - existing.getCount());
                    if (add > 0) {
                        existing.increment(add);
                        remaining -= add;
                    }
                }
                if (remaining <= 0) break;
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    protected void insertOutputs(List<SlotOutput> outputs) {
        for (SlotOutput output : outputs) {
            insertOutput(output.slot, output.stack);
        }
    }

    protected static MachineOperation operation(int inputSlot, int inputCount, int outputSlot, ItemStack output, int ticks, long euPerTick) {
        return new MachineOperation(
                List.of(new SlotConsumption(inputSlot, Math.max(1, inputCount))),
                List.of(new SlotOutput(outputSlot, output.copy())),
                Math.max(1, ticks),
                Math.max(0L, euPerTick),
                null
        );
    }

    protected static MachineOperation operation(List<SlotConsumption> inputs, List<SlotOutput> outputs, int ticks, long euPerTick) {
        return new MachineOperation(inputs, outputs, Math.max(1, ticks), Math.max(0L, euPerTick), null);
    }

    protected static MachineOperation operation(List<SlotConsumption> inputs, List<SlotOutput> outputs, int ticks, long euPerTick, Runnable onFinish) {
        return new MachineOperation(inputs, outputs, Math.max(1, ticks), Math.max(0L, euPerTick), onFinish);
    }

    public record SlotConsumption(int slot, int amount) { }
    public record SlotOutput(int slot, ItemStack stack) { }

    protected static final class MachineOperation {
        private final List<SlotConsumption> inputs;
        private final List<SlotOutput> outputs;
        private final int ticks;
        private final long euPerTick;
        private final Runnable onFinish;

        private MachineOperation(List<SlotConsumption> inputs, List<SlotOutput> outputs, int ticks, long euPerTick, Runnable onFinish) {
            this.inputs = inputs;
            this.outputs = outputs;
            this.ticks = ticks;
            this.euPerTick = euPerTick;
            this.onFinish = onFinish;
        }
    }

    private static final class SlotSnapshot {
        private final int slot;
        private ItemStack stack;

        private SlotSnapshot(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }
}
