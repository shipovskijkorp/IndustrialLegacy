package com.shipovskijkorp.industriallegacy.block.entity.base;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared IC2-like implementation of standard electric processing machines.
 *
 * <p>Mirrors the important part of IC2's {@code TileEntityStandardMachine}:
 * find a valid output, consume EU every tick, advance progress, perform one or
 * more operations at the end of the cycle, then reset progress. Upgrade maths is
 * intentionally centralized here so overclockers/transformer/storage upgrades can
 * be added once instead of per machine.</p>
 */
public abstract class AbstractStandardMachineBlockEntity extends AbstractElectricMachineBlockEntity {
    protected AbstractStandardMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                                 int inventorySize, long defaultEnergyStorage, int defaultTier,
                                                 int defaultEnergyConsume, int defaultOperationLength,
                                                 int guiPropertyCount) {
        super(type, pos, state, inventorySize, defaultEnergyStorage, defaultTier, defaultEnergyConsume, defaultOperationLength, guiPropertyCount);
    }

    @Override
    protected boolean processMachineTick(World world) {
        MachineOperation operation = findOperation(world);
        if (operation == null) {
            resetProgress();
            return false;
        }

        if (!canOutput(operation.outputs())) return false;
        if (energy < energyConsume) return false;

        energy -= energyConsume;
        maxProgress = Math.max(1, operation.ticks() <= 0 ? operationLength : operation.ticks());
        progress++;

        if (progress >= maxProgress) {
            completeOperations(world, operation);
            progress = 0;
        }

        return true;
    }

    protected void completeOperations(World world, MachineOperation firstOperation) {
        MachineOperation operation = firstOperation;
        int count = Math.max(1, operationsPerCycle);
        for (int i = 0; i < count && operation != null; i++) {
            if (!canOutput(operation.outputs())) break;
            if (!beforeCompleteOperation(world, operation)) break;
            consumeOperationInput(operation);
            insertOutputs(operation.outputs());
            afterCompleteOperation(world, operation);
            operation = findOperation(world);
        }
    }

    @Nullable
    protected abstract MachineOperation findOperation(World world);

    protected boolean beforeCompleteOperation(World world, MachineOperation operation) {
        return true;
    }

    protected void afterCompleteOperation(World world, MachineOperation operation) {
    }

    protected void consumeOperationInput(MachineOperation operation) {
        int count = Math.max(0, operation.inputCount());
        if (count > 0) items.get(getInputSlot()).decrement(count);
    }

    protected boolean canOutput(List<ItemStack> outputs) {
        if (outputs == null || outputs.isEmpty()) return false;
        if (outputs.size() == 1) return canOutput(outputs.get(0));

        // Conservative multi-output fallback: subclasses with real multiple output
        // slots should override this. Returning false is safer than voiding output.
        return false;
    }

    protected void insertOutputs(List<ItemStack> outputs) {
        if (outputs == null || outputs.isEmpty()) return;
        insertOutput(outputs.get(0));
    }

    protected MachineOperation operation(ItemStack output, int inputCount, int ticks) {
        return operation(output, inputCount, ticks, null);
    }

    protected MachineOperation operation(ItemStack output, int inputCount, int ticks, Object context) {
        return new MachineOperation(List.of(output.copy()), inputCount, ticks, context);
    }

    protected MachineOperation operation(List<ItemStack> outputs, int inputCount, int ticks, Object context) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) copy.add(stack.copy());
        }
        return new MachineOperation(List.copyOf(copy), inputCount, ticks, context);
    }

    protected record MachineOperation(List<ItemStack> outputs, int inputCount, int ticks, Object context) {
    }
}
